# Plan de integración App Android ↔ API en la nube

> **Fecha:** 2026-06-21 · **App:** `02_app` (Kotlin + Jetpack Compose + Room) ·
> **API:** `https://yupay-turismo-api.onrender.com` (Node + Express + Supabase)
>
> Este documento acompaña a `api/INTEGRACION_API_APP.md` (análisis de brechas) y registra
> **lo que YA quedó implementado (capa funcional)** y **lo que falta (UI + lógica suelta)**.
> El modo **P2P por LAN** (`sync/`) es independiente y **no se tocó**.

---

## 0. Alcance de esta entrega

Se construyó **toda la capa funcional** de consumo de la API (red, sesión, repos, sync
offline-first) y se dejó **cableada al `MainViewModel`** lista para que la UI la consuma. **No**
se rediseñaron las pantallas de Compose (eso es la siguiente fase, §3). Concretamente:

- ✅ Dependencias de red en Gradle (OkHttp, DataStore, desugaring java.time, Credential Manager).
- ✅ Capa `data/remote/` (cliente HTTP tipado, DTOs, refresh automático de token, mappers).
- ✅ Sesión segura en DataStore (`SessionManager`), sin contraseña en claro.
- ✅ Autenticación completa: registro, login, Google nativo (hook), reset por código OTP,
  logout, borrado de cuenta, check-email, reenvío de verificación.
- ✅ **Offline-first**: outbox (`PendingOp`) + `remoteId` en entidades; al volver internet se
  sube todo lo pendiente "de golpe" y se baja lo nuevo (sync incremental).
- ✅ Disparo automático de sync al reconectar (nuevo `isOnline` real, incluye datos móviles).
- ✅ Métodos en `MainViewModel` (`register/login/forgotPassword/…/syncNow`) + estado `authState`.

---

## 1. Mapa de archivos nuevos / modificados

### Nuevos
```
app/src/main/java/yupay/turismo/
├─ YupayApp.kt                         # Application: arranca ServiceLocator + sync auto
├─ di/ServiceLocator.kt               # Singletons (sin framework DI)
├─ data/session/SessionManager.kt     # Tokens en DataStore (no Room, no contraseña)
├─ data/remote/
│  ├─ ApiConfig.kt                    # baseUrl
│  ├─ Net.kt                          # ApiEnvelope, ApiResult, yupayJson, JSON_MEDIA
│  ├─ HttpModule.kt                   # arma OkHttp (interceptor + authenticator)
│  ├─ AuthInterceptor.kt              # header Bearer
│  ├─ TokenAuthenticator.kt           # 401 → /auth/refresh → reintento
│  ├─ YupayApiService.kt              # TODOS los endpoints, devuelve ApiResult
│  ├─ Mappers.kt                      # Room↔DTO (base64, fechas ms↔ISO, content)
│  └─ dto/AuthDtos.kt, dto/SyncDtos.kt
├─ data/repository/
│  ├─ AuthRepository.kt               # flujos de auth + guardado de sesión
│  └─ CloudSyncRepository.kt          # migrate/pull/push + outbox + merge
├─ data/sync/CloudSyncEngine.kt       # mutex + disparadores automáticos
└─ data/local/PendingOp.kt, PendingOpDao.kt   # outbox
```

### Modificados
```
gradle/libs.versions.toml             # versiones+libs de red
app/build.gradle.kts                  # deps + coreLibraryDesugaring
app/src/main/AndroidManifest.xml      # android:name=".YupayApp"
data/local/AppDatabase.kt             # +PendingOp, version 10→11
data/local/AppSettings.kt             # +lastSyncAt (accountPassword marcado DEPRECADO)
data/local/Product.kt / Visit.kt      # +remoteId: Long?
data/local/ProductDao.kt / VisitDao.kt# +getByRemoteId, insert→Long, replaceVisits, etc.
data/DataRepository.kt                # insertProduct/insertVisit devuelven el id (Long)
utils/NetworkMonitor.kt               # +isOnline (internet validado, cualquier transporte)
ui/MainViewModel.kt                   # métodos auth/sync + enqueue al outbox en mutaciones
```

---

## 2. Cómo funciona (resumen de arquitectura)

- **Cliente HTTP**: `YupayApiService` (OkHttp + kotlinx.serialization). Cada método devuelve
  `ApiResult.Ok<T>` o `ApiResult.Fail(code, message, offline)`. Maneja la envoltura
  `{ success, data, error }` y los cold-starts de Render (timeouts amplios).
- **Sesión**: `SessionManager` (DataStore) guarda `accessToken/refreshToken/expiresAt/userId/email`.
  `TokenAuthenticator` refresca solo ante 401 (no en endpoints públicos de auth).
- **Offline-first**:
  - Cada cambio local (producto/visita/perfil) se escribe en Room **y** se encola un `PendingOp`
    (sólo si la cuenta está vinculada). Coalescencia por entidad.
  - `CloudSyncEngine` observa `NetworkMonitor.isOnline` + sesión y dispara `syncNow()` al
    reconectar o al crecer la cola. `syncNow` **drena el outbox** (POST/PUT/DELETE por entidad,
    capturando `remoteId`) y luego hace **`GET /sync/pull?since=lastSyncAt`** y fusiona.
  - **Primer enlace** (`firstLinkSmart`): tras login/registro/Google decide:
    - servidor vacío + datos locales → **`/sync/migrate`** (sube todo) y luego pull para ids;
    - servidor con datos → adopta lo del servidor (pull, reemplazo).
- **IDs**: local `Int` (Room) ↔ `remoteId: Long?` (servidor). Visitas además se deduplican por
  `registrationDate` (lo hace la API).

---

## 3. ⬜ PENDIENTE — Cableado de UI (siguiente fase)

> Todo esto es **UI/navegación**; la lógica ya existe en `MainViewModel`. Observar
> `viewModel.authState` (`AuthUiState`: `loading/error/info/event`) y reaccionar a `AuthEvent`.

### 3.1 `RegisterScreen.kt`
- Reemplazar `viewModel.linkAccount(...)` por **`viewModel.register(email, password)`**.
- (Opcional) Antes de registrar, `viewModel.checkEmail(email){ exists, confirmed -> }` para avisar
  "correo ya registrado".
- Mostrar `loading` en el botón; ante `error` mostrar Snackbar.
- Diálogo de verificación: reaccionar a `AuthEvent.NeedsEmailConfirmation` (cuenta creada, falta
  confirmar correo) vs `AuthEvent.LoggedIn` (confirmación de correo desactivada → ya entró).
  Botón "Ya verifiqué" → `viewModel.login(email, password)`. Botón "Reenviar" →
  `viewModel.resendVerification(email)`.
- Google: ver §4.

### 3.2 `LoginScreen.kt`
- Botón **Entrar** → `viewModel.login(email, password)` (quitar `linkAccount`).
- **"¿Olvidaste tu contraseña?"** (hoy `/* Simulated */`) → navegar a una pantalla nueva de
  recuperación (§3.4).
- Reaccionar a `AuthEvent.LoggedIn` → `onSuccess()`. Mostrar `error`/`loading`.
- Google "Continuar con Google" → §4.

### 3.3 `AccountInfoScreen.kt`
- **Quitar** la tarjeta que muestra la contraseña (`settings.accountPassword`) — ya no se guarda.
- Mostrar `viewModel.cloudSession` (email real) y estado de sync (`viewModel.syncState`,
  `viewModel.pendingSyncCount`).
- Añadir botones: **Cerrar sesión** → `viewModel.logout()`; **Eliminar cuenta** →
  diálogo de confirmación → `viewModel.deleteAccount()` (reusar `CountdownConfirmationDialog`).
- Opcional: botón **"Sincronizar ahora"** → `viewModel.syncNow()`.

### 3.4 Pantalla nueva: recuperación de contraseña (OTP)
Flujo en 2–3 pasos (la lógica ya está):
1. Pedir correo → `viewModel.forgotPassword(email)`. Reaccionar a `AuthEvent.CodeSent`
   (o `error` si `NOT_REGISTERED`/`OAUTH_ONLY`).
2. Pedir **código** → `viewModel.verifyResetCode(email, code)` → `AuthEvent.CodeValid`.
3. Pedir **nueva contraseña** → `viewModel.resetPassword(newPassword)` → `AuthEvent.PasswordReset`.
   - Alternativa de un solo paso: `viewModel.resetPassword(email, code, newPassword)`.
- ⚠️ **El código NO es de 6 dígitos fijo.** El Supabase de este proyecto emite **OTP de 8 dígitos**;
  el endpoint acepta cualquier longitud. El `OutlinedTextField` del código debe aceptar longitud
  variable (no fijar `maxLength = 6`).

### 3.5 Splash / arranque
- En `SplashScreen`/navegación inicial, decidir destino según sesión:
  `viewModel.cloudSession` ≠ null → ya logueado. El sync automático ya corre solo al haber red.

### 3.6 Indicadores globales de sync (opcional, recomendado)
- Badge/aviso con `pendingSyncCount > 0` ("N cambios sin subir") y `syncState`
  (`Syncing`/`Success`/`Error`). Reusar `GlobalLoadingScreen`/`LoadingOverlay`.

---

## 4. ⬜ PENDIENTE — Google nativo (Credential Manager)

La parte de servidor y `MainViewModel.loginWithGoogle(idToken, nonce)` **ya están**. Falta el
**helper de UI** que obtiene el `idToken` (necesita un `Activity` y el **Client ID Web**). Las
dependencias (`androidx.credentials`, `googleid`) ya están en Gradle. Snippet listo para pegar:

```kotlin
// ui/features/auth/GoogleAuthHelper.kt  (crear en la fase de UI)
suspend fun fetchGoogleIdToken(activity: Activity, webClientId: String): Pair<String, String>? {
    val rawNonce = java.util.UUID.randomUUID().toString()
    val hashedNonce = java.security.MessageDigest.getInstance("SHA-256")
        .digest(rawNonce.toByteArray()).joinToString("") { "%02x".format(it) }
    val option = com.google.android.libraries.identity.googleid.GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(webClientId)   // ← Client ID *Web* (no el de Android)
        .setNonce(hashedNonce)            // a Google va el hash; a la API el nonce en claro
        .build()
    val request = androidx.credentials.GetCredentialRequest.Builder()
        .addCredentialOption(option).build()
    return try {
        val result = androidx.credentials.CredentialManager.create(activity)
            .getCredential(activity, request)
        val cred = com.google.android.libraries.identity.googleid
            .GoogleIdTokenCredential.createFrom(result.credential.data)
        cred.idToken to rawNonce
    } catch (e: Exception) { null }
}
// Uso: val (idToken, nonce) = fetchGoogleIdToken(activity, webClientId) ?: return
//      viewModel.loginWithGoogle(idToken, nonce)
```

- El `webClientId` puede obtenerse de **`GET /auth/config`** (`googleWebClientId`) — el
  `YupayApiService.getConfig()` ya existe; sólo falta exponerlo si se quiere evitar hardcodearlo.
- **Configuración Supabase (una vez):** añadir el Client ID **Web** y el de **Android** en
  *Authentication → Providers → Google → "Authorized Client IDs"*.
- **Nota (de sesiones previas):** el endpoint `idtoken` está listo pero **no se pudo probar** sin
  los Client IDs reales y un dispositivo con Google Play.

---

## 5. ⬜ PENDIENTE — Desajustes de modelo de datos (§5 del análisis)

- **Audio de content**: la app tiene **un solo** `entrepreneurTipsAudio`/`mapSummaryAudio` (no por
  idioma); la API guarda `audioBase64` **por fila** `(language, type)`. Decisión v1:
  - Al **subir** (migrate) **no** se manda audio (lo genera la IA del servidor).
  - Al **bajar**, se rellena el audio del **idioma actual** (best-effort) en el campo único.
  - **Pendiente**: decidir si la app pasa a audio por idioma (recomendado, calza con la API) y
    ajustar `AppSettings` + reproductor (`AudioPlayerUI`).
- **Generación IA**: el content (tips/maps ×4 idiomas) lo genera el servidor con Gemini tras
  migrate/push (en segundo plano, con gating de costos). La app sólo lo **lee** vía pull. Verificar
  que la UI de Home/Tips refresque al llegar el content nuevo (ya viene en `appSettings`).

---

## 6. ⬜ PENDIENTE — Casos borde de sincronización

- **Reconciliación de IDs de productos**: v1 trata el servidor como fuente de verdad tras el primer
  pull (reconstruye productos/visitas con su `remoteId`). Edge: si el usuario tenía datos locales y
  **inicia sesión en una cuenta existente con datos**, **gana el servidor** (se reemplazan los
  locales). Si esto no es deseable, añadir UI de "fusionar/elegir" antes del pull.
- **Borrados en el servidor**: `/sync/pull?since=` **no** devuelve tombstones; un borrado hecho en
  otro dispositivo no se refleja por pull incremental. Mitigación futura: pull completo periódico,
  o endpoint de tombstones.
- **Reloj device vs servidor**: `lastSyncAt` usa la hora del dispositivo. Un desfase grande podría
  re-traer o saltarse filas. Aceptable en v1 (upserts idempotentes); revisar si causa problemas.
- **Poison-pill**: una op del outbox que falle con 4xx de cliente se **descarta** para no atascar la
  cola (se registra en log). Considerar exponerlo en UI si hace falta.

---

## 7. ⬜ PENDIENTE — Configuración externa (no es código de la app)

- **Supabase**: activar **Confirm email**; plantilla *Reset Password* con `{{ .Token }}`;
  **SMTP propio** (el correo integrado no entrega fiable); `UNIQUE(user_id,language,type)` en
  `content`; ejecutar `api/sql/content_generation.sql`; Client IDs de Google.
- **Render**: rellenar secretos (`SUPABASE_*`, `GEMINI_API_KEY`, opc. `GOOGLE_*_CLIENT_ID`). La
  `baseUrl` ya apunta a `https://yupay-turismo-api.onrender.com` en `ApiConfig.kt`.

---

## 8. ⬜ PENDIENTE — Endurecimiento y pruebas

- **Seguridad**: eliminar definitivamente `accountPassword` de `AppSettings` (tras quitar su uso en
  `AccountInfoScreen`); valorar cifrar el DataStore de sesión (EncryptedSharedPreferences/Tink).
- **Logging**: `HttpLoggingInterceptor` está en `BASIC` (no expone tokens). Subir a `BODY` sólo en
  debug si hace falta depurar.
- **Pruebas manuales** (con la API ya desplegada):
  1. Registro nuevo → migrate sube datos → pull devuelve ids.
  2. Login en otro dispositivo → baja todo.
  3. Crear visita/producto **sin internet** → activar internet → se suben solos.
  4. Editar/eliminar producto offline → reconectar → se refleja en servidor.
  5. Forgot password con OTP de 8 dígitos.
  6. Logout / eliminar cuenta.
- **Verificación de build**: este código se validó por compilación de Kotlin/KSP en local; hacer
  **Gradle sync + Run** en Android Studio para una verificación end-to-end en dispositivo.

---

## 9. Resumen de endpoints consumidos (referencia rápida)

| App (método ViewModel/Repo) | Endpoint |
|---|---|
| `register` | `POST /auth/register` |
| `login` | `POST /auth/login` |
| `loginWithGoogle` | `POST /auth/google/idtoken` |
| `forgotPassword` | `POST /auth/forgot-password` |
| `verifyResetCode` | `POST /auth/verify-reset-code` |
| `resetPassword` | `POST /auth/reset-password` |
| `checkEmail` | `POST /auth/check-email` |
| `resendVerification` | `POST /auth/resend-verification` |
| `logout` | `POST /auth/logout` |
| `deleteAccount` | `DELETE /auth/account` |
| refresh automático | `POST /auth/refresh` |
| primer enlace (subida) | `POST /sync/migrate` |
| sync (bajada) | `GET /sync/pull?since=` |
| outbox productos/visitas | `POST/PUT/DELETE /products`, `/visits` |
| outbox perfil | `PATCH /users/me` |
