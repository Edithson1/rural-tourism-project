package yupay.turismo.data.repository

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import yupay.turismo.data.local.AppSettings
import yupay.turismo.data.local.AppSettingsDao
import yupay.turismo.data.local.PendingOp
import yupay.turismo.data.local.PendingOpDao
import yupay.turismo.data.local.Product
import yupay.turismo.data.local.ProductDao
import yupay.turismo.data.local.Visit
import yupay.turismo.data.local.VisitDao
import yupay.turismo.data.remote.ApiResult
import yupay.turismo.data.remote.DeletePayload
import yupay.turismo.data.remote.YupayApiService
import yupay.turismo.data.remote.applyContentToSettings
import yupay.turismo.data.remote.dto.MigrateRequest
import yupay.turismo.data.remote.dto.PullResponse
import yupay.turismo.data.remote.fromBase64
import yupay.turismo.data.remote.millisToIso
import yupay.turismo.data.remote.parseIsoToMillis
import yupay.turismo.data.remote.toContentRequestDtos
import yupay.turismo.data.remote.toEntity
import yupay.turismo.data.remote.toProfileRequestDto
import yupay.turismo.data.remote.toRequestDto
import yupay.turismo.data.remote.yupayJson
import yupay.turismo.data.session.SessionManager

/** Resultado de una operación de sincronización. */
sealed interface SyncOutcome {
    object Success : SyncOutcome
    object NotLoggedIn : SyncOutcome
    data class Error(val message: String, val offline: Boolean = false) : SyncOutcome
}

/**
 * Sincronización con la nube (offline-first).
 *
 * - **Encolado**: cada cambio local de producto/visita/perfil se registra en el outbox
 *   ([PendingOp]) mediante los métodos `enqueue*`.
 * - **Primer enlace** ([firstLinkSmart]): tras login/registro/Google, decide subir lo local
 *   (`/sync/migrate`) si el servidor está vacío, o adoptar lo del servidor (`/sync/pull`).
 * - **Sync incremental** ([syncNow]): drena el outbox y baja cambios desde `lastSyncAt`.
 */
class CloudSyncRepository(
    private val api: YupayApiService,
    private val session: SessionManager,
    private val appSettingsDao: AppSettingsDao,
    private val productDao: ProductDao,
    private val visitDao: VisitDao,
    private val pendingOpDao: PendingOpDao
) {
    val pendingCountFlow = pendingOpDao.countFlow()

    // ───────────────── Encolado (offline) ─────────────────
    suspend fun enqueueProductUpsert(localId: Int) =
        enqueue(PendingOp.ENTITY_PRODUCT, PendingOp.OP_UPDATE, localId, "")

    suspend fun enqueueProductDelete(product: Product) =
        enqueue(
            PendingOp.ENTITY_PRODUCT, PendingOp.OP_DELETE, product.id,
            yupayJson.encodeToString(DeletePayload(product.remoteId))
        )

    suspend fun enqueueVisitUpsert(localId: Int) =
        enqueue(PendingOp.ENTITY_VISIT, PendingOp.OP_UPDATE, localId, "")

    suspend fun enqueueVisitDelete(visit: Visit) =
        enqueue(
            PendingOp.ENTITY_VISIT, PendingOp.OP_DELETE, visit.id,
            yupayJson.encodeToString(DeletePayload(visit.remoteId))
        )

    suspend fun enqueueProfileUpdate() =
        enqueue(PendingOp.ENTITY_PROFILE, PendingOp.OP_UPDATE, PendingOp.PROFILE_LOCAL_ID, "")

    private suspend fun enqueue(entityType: String, opType: String, localId: Int, payload: String) {
        // Coalescencia: sólo la op más reciente por entidad.
        pendingOpDao.deleteForEntity(entityType, localId)
        pendingOpDao.insert(
            PendingOp(entityType = entityType, opType = opType, localId = localId, payloadJson = payload)
        )
    }

    /** Vacía el outbox (al cerrar sesión o borrar la cuenta). */
    suspend fun clearOutbox() = pendingOpDao.clear()

    // ───────────────── Primer enlace ─────────────────
    suspend fun firstLinkSmart(): SyncOutcome {
        if (!session.isLoggedIn()) return SyncOutcome.NotLoggedIn
        val startedAt = System.currentTimeMillis()

        val pull = when (val r = api.pull(null)) {
            is ApiResult.Ok -> r.data
            is ApiResult.Fail -> return r.toOutcome()
        }

        val settings = appSettingsDao.getSettingsOnce() ?: AppSettings()
        val localProducts = productDao.getAllOnce()
        val localVisits = visitDao.getAllOnce()
        // "Servidor vacío" = sin productos ni visitas. NO se mira businessName: el registro por
        // OTP (verify-signup-code) ya crea la fila de perfil, pero los datos del emprendedor
        // (productos/visitas) siguen siendo locales y hay que subirlos con migrate, no perderlos.
        val serverEmpty = pull.products.isEmpty() && pull.visits.isEmpty()
        val localHasData = localProducts.isNotEmpty() || localVisits.isNotEmpty() ||
            settings.businessName.isNotBlank()

        if (serverEmpty && localHasData) {
            // El servidor está vacío y la app tiene datos → subirlos y recuperar ids.
            val migrateRes = api.migrate(buildMigrateRequest(settings, localProducts, localVisits))
            if (migrateRes is ApiResult.Fail) return migrateRes.toOutcome()
            when (val r2 = api.pull(null)) {
                is ApiResult.Ok -> finishSync(applyPull(r2.data, replace = true), startedAt)
                is ApiResult.Fail -> return r2.toOutcome()
            }
        } else {
            // El servidor manda (login a cuenta existente, o nada que subir).
            finishSync(applyPull(pull, replace = true), startedAt)
        }
        pendingOpDao.clear()
        return SyncOutcome.Success
    }

    // ───────────────── Sync incremental ─────────────────
    suspend fun syncNow(): SyncOutcome {
        if (!session.isLoggedIn()) return SyncOutcome.NotLoggedIn
        val startedAt = System.currentTimeMillis()

        if (!drainOutbox()) {
            return SyncOutcome.Error("Cambios locales pendientes; se reintentará al reconectar.", offline = true)
        }

        val settings = appSettingsDao.getSettingsOnce() ?: AppSettings()
        val since = if (settings.lastSyncAt > 0) millisToIso(settings.lastSyncAt) else null
        val pull = when (val r = api.pull(since)) {
            is ApiResult.Ok -> r.data
            is ApiResult.Fail -> return r.toOutcome()
        }
        finishSync(applyPull(pull, replace = false), startedAt)
        return SyncOutcome.Success
    }

    // ───────────────── Outbox ─────────────────
    /** @return true si se drenó por completo; false si hubo que parar (reintentar luego). */
    private suspend fun drainOutbox(): Boolean {
        for (op in pendingOpDao.getAll()) {
            when (handleOp(op)) {
                OpResult.SUCCESS, OpResult.DROP -> pendingOpDao.deleteById(op.id)
                OpResult.RETRY -> return false
            }
        }
        return true
    }

    private enum class OpResult { SUCCESS, RETRY, DROP }

    private suspend fun handleOp(op: PendingOp): OpResult = when (op.entityType) {
        PendingOp.ENTITY_PROFILE -> {
            val s = appSettingsDao.getSettingsOnce()
            if (s == null) OpResult.DROP else classify(api.updateMe(s.toProfileRequestDto()))
        }

        PendingOp.ENTITY_PRODUCT -> if (op.opType == PendingOp.OP_DELETE) {
            val remoteId = decodeRemoteId(op.payloadJson)
            if (remoteId == null) OpResult.SUCCESS else classifyDelete(api.deleteProduct(remoteId))
        } else {
            val p = productDao.getProductById(op.localId) ?: return OpResult.SUCCESS
            if (p.remoteId == null) {
                when (val r = api.createProduct(p.toRequestDto())) {
                    is ApiResult.Ok -> { productDao.updateProduct(p.copy(remoteId = r.data.id)); OpResult.SUCCESS }
                    is ApiResult.Fail -> classifyFail(r)
                }
            } else {
                when (val r = api.updateProduct(p.remoteId, p.toRequestDto())) {
                    is ApiResult.Ok -> OpResult.SUCCESS
                    is ApiResult.Fail -> if (r.code == 404) recreateProduct(p) else classifyFail(r)
                }
            }
        }

        PendingOp.ENTITY_VISIT -> if (op.opType == PendingOp.OP_DELETE) {
            val remoteId = decodeRemoteId(op.payloadJson)
            if (remoteId == null) OpResult.SUCCESS else classifyDelete(api.deleteVisit(remoteId))
        } else {
            val v = visitDao.getVisitById(op.localId) ?: return OpResult.SUCCESS
            if (v.remoteId == null) {
                when (val r = api.createVisit(v.toRequestDto())) {
                    is ApiResult.Ok -> { visitDao.updateVisit(v.copy(remoteId = r.data.id)); OpResult.SUCCESS }
                    is ApiResult.Fail -> classifyFail(r)
                }
            } else {
                when (val r = api.updateVisit(v.remoteId, v.toRequestDto())) {
                    is ApiResult.Ok -> OpResult.SUCCESS
                    is ApiResult.Fail -> if (r.code == 404) recreateVisit(v) else classifyFail(r)
                }
            }
        }

        else -> OpResult.DROP
    }

    private suspend fun recreateProduct(p: Product): OpResult =
        when (val c = api.createProduct(p.toRequestDto())) {
            is ApiResult.Ok -> { productDao.updateProduct(p.copy(remoteId = c.data.id)); OpResult.SUCCESS }
            is ApiResult.Fail -> classifyFail(c)
        }

    private suspend fun recreateVisit(v: Visit): OpResult =
        when (val c = api.createVisit(v.toRequestDto())) {
            is ApiResult.Ok -> { visitDao.updateVisit(v.copy(remoteId = c.data.id)); OpResult.SUCCESS }
            is ApiResult.Fail -> classifyFail(c)
        }

    private fun classify(res: ApiResult<*>): OpResult =
        if (res is ApiResult.Fail) classifyFail(res) else OpResult.SUCCESS

    private fun classifyDelete(res: ApiResult<*>): OpResult = when (res) {
        is ApiResult.Ok -> OpResult.SUCCESS
        is ApiResult.Fail -> if (res.code == 404) OpResult.SUCCESS else classifyFail(res)
    }

    private fun classifyFail(res: ApiResult.Fail): OpResult = when {
        res.offline -> OpResult.RETRY
        res.code in 500..599 -> OpResult.RETRY
        res.code == 401 -> OpResult.RETRY
        else -> OpResult.DROP // 4xx de cliente: descartar para no atascar la cola
    }

    private fun decodeRemoteId(payload: String): Long? =
        if (payload.isBlank()) null
        else runCatching { yupayJson.decodeFromString<DeletePayload>(payload).remoteId }.getOrNull()

    // ───────────────── Aplicar /sync/pull a Room ─────────────────
    /** Vuelca el pull a Room y devuelve el [AppSettings] fusionado (sin guardar todavía). */
    private suspend fun applyPull(pull: PullResponse, replace: Boolean): AppSettings {
        val settings = appSettingsDao.getSettingsOnce() ?: AppSettings()
        var merged = settings
        pull.user?.let { u ->
            merged = merged.copy(
                businessName = u.businessName ?: merged.businessName,
                businessCategory = u.businessCategory ?: merged.businessCategory,
                profilePicture = u.profilePicture.fromBase64() ?: merged.profilePicture
            )
        }
        merged = applyContentToSettings(merged, pull.content, settings.language)

        if (replace) {
            productDao.replaceProducts(pull.products.map { it.toEntity() })
        } else {
            pull.products.forEach { dto ->
                val existing = productDao.getByRemoteId(dto.id)
                if (existing != null) productDao.updateProduct(dto.toEntity(localId = existing.id))
                else productDao.insertProduct(dto.toEntity())
            }
        }

        if (replace) {
            visitDao.replaceVisits(pull.visits.map { it.toEntity() })
        } else {
            pull.visits.forEach { dto ->
                val ms = parseIsoToMillis(dto.registrationDate)
                val existing = visitDao.getByRemoteId(dto.id) ?: ms?.let { visitDao.getByRegistrationDate(it) }
                if (existing != null) visitDao.updateVisit(dto.toEntity(localId = existing.id))
                else visitDao.insertVisit(dto.toEntity())
            }
        }
        return merged
    }

    private suspend fun finishSync(merged: AppSettings, startedAt: Long) {
        appSettingsDao.saveSettings(
            merged.copy(lastSyncAt = startedAt, lastModified = System.currentTimeMillis())
        )
    }

    private fun buildMigrateRequest(
        settings: AppSettings,
        products: List<Product>,
        visits: List<Visit>
    ): MigrateRequest = MigrateRequest(
        profile = settings.toProfileRequestDto(),
        products = products.map { it.toRequestDto() },
        visits = visits.map { it.toRequestDto() },
        content = settings.toContentRequestDtos()
    )

    private fun ApiResult.Fail.toOutcome(): SyncOutcome = SyncOutcome.Error(message, offline)
}
