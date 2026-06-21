package yupay.turismo.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import yupay.turismo.data.DataRepository
import yupay.turismo.data.local.AppDatabase
import yupay.turismo.data.local.AppSettings
import yupay.turismo.data.local.Visit
import yupay.turismo.data.local.Product
import yupay.turismo.data.local.SelectedProduct
import yupay.turismo.data.local.DiscountType
import yupay.turismo.data.model.CountryFeature
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.io.ByteArrayOutputStream

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: DataRepository
    val appSettings: StateFlow<AppSettings?>
    val allVisits: StateFlow<List<Visit>>
    val allProducts: StateFlow<List<Product>>

    private val _countryFeatures = MutableStateFlow<List<CountryFeature>>(emptyList())
    val countryFeatures: StateFlow<List<CountryFeature>> = _countryFeatures.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = DataRepository(database.appSettingsDao(), database.visitDao(), database.productDao())
        
        appSettings = repository.appSettings.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null,
        )

        allVisits = repository.allVisits.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allProducts = repository.allProducts.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        loadCountryFeatures()

        viewModelScope.launch {
            val androidId = Settings.Secure.getString(
                application.contentResolver,
                Settings.Secure.ANDROID_ID
            ) ?: "unknown"
            
            val currentSettings = repository.getSettingsOnce()
            if (currentSettings == null) {
                repository.saveSettings(AppSettings(
                    deviceId = androidId,
                    hardwareDeviceId = androidId,
                    entrepreneurTips = defaultTips,
                    mapSummary = defaultSummaries
                ))
            } else {
                var updated = currentSettings
                var changed = false
                
                if (currentSettings.hardwareDeviceId.isEmpty()) {
                    updated = updated.copy(hardwareDeviceId = androidId)
                    changed = true
                }
                
                // Si el deviceId está vacío (por ser nuevo campo) y no es servidor
                if (currentSettings.deviceId.isEmpty()) {
                    updated = updated.copy(deviceId = androidId)
                    changed = true
                }

                if (currentSettings.entrepreneurTips.isEmpty()) {
                    updated = updated.copy(
                        entrepreneurTips = defaultTips,
                        mapSummary = defaultSummaries
                    )
                    changed = true
                }
                
                if (changed) {
                    repository.saveSettings(updated)
                }
            }
        }
    }

    private val defaultTips = mapOf(
        "Español" to "La hospitalidad es la clave.\nSiempre recibe a tus turistas con una sonrisa.\nConoce bien tu historia local para compartirla.\nManten tus espacios limpios y ordenados.\nOfrece productos locales de calidad.",
        "Quechua" to "Allin chaskiymi ancha allin.\nTuristaykikunataqa sapa kutin p'isñuywan chaskiy.\nLlaqtaykiq kawsayninta allinta yachay willanaykipaq.\nKuyuchiy wasiykikunata ch'uya hinaspa allichasqa.\nAllin llaqtaykiq rurunkunata quy.",
        "Inglés" to "Hospitality is the key.\nAlways welcome your tourists with a smile.\nKnow your local history well to share it.\nKeep your spaces clean and organized.\nOffer quality local products.",
        "Portugués" to "A hospitalidade é a chave.\nSempre receba seus turistas com um sorriso.\nConheça bem sua história local para compartilhá-la.\nMantenha seus espaços limpos e organizados.\nOfereça produtos locais de qualidade."
    )

    private val defaultSummaries = mapOf(
        "Español" to "Este mapa muestra la distribución de tus visitas.\nLos puntos azules representan hospedaje.\nLos puntos verdes son de alimentación.\nLos puntos rojos indican artesanía.\nUsa el zoom para ver más detalles.",
        "Quechua" to "Kay saywitipim watukuyniykikuna rakisqa kachkan.\nAnqas unanchakunaqa puñuy wasim.\nQ'umir unanchakunaqa mikhuy wasim.\nPuka unanchakunaqa makipi rurasqakuna.\nHatunyachiy aswan allinta qhawanaykipaq.",
        "Inglés" to "This map shows the distribution of your visits.\nBlue points represent lodging.\nGreen points are for food services.\nRed points indicate handicrafts.\nUse zoom to see more details.",
        "Portugués" to "Este mapa mostra a distribuição das suas visitas.\nOs pontos azuis representam hospedagem.\nOs pontos verdes são de alimentação.\nOs pontos vermelhos indicam artesanato.\nUse o zoom para ver mais detalhes."
    )

    fun addVisit(
        nationality: String,
        flag: String,
        selectedProducts: List<SelectedProduct>,
        subtotal: Double,
        discountValue: Double,
        discountType: DiscountType,
        totalAmount: Double
    ) {
        viewModelScope.launch {
            val currentSettings = repository.getSettingsOnce()
            val visit = Visit(
                deviceId = currentSettings?.deviceId ?: "",
                nationality = nationality,
                nationalityFlag = flag,
                selectedProducts = selectedProducts,
                subtotal = subtotal,
                discountValue = discountValue,
                discountType = discountType,
                totalAmount = totalAmount,
                currency = currentSettings?.preferredCurrency ?: "S/"
            )
            repository.insertVisit(visit)
        }
    }

    fun addProduct(product: Product) {
        viewModelScope.launch { repository.insertProduct(product) }
    }

    fun updateProduct(product: Product) {
        viewModelScope.launch { repository.updateProduct(product) }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch { repository.deleteProduct(product) }
    }

    suspend fun getProductById(id: Int): Product? {
        return repository.getProductById(id)
    }

    fun preloadProducts(category: String) {
        viewModelScope.launch {
            // Eliminar productos previos que sean 'isDefault' para evitar duplicados al cambiar de opinión en el setup
            val all = repository.allProducts.first()
            val defaultsToDelete = all.filter { it.isDefault }
            defaultsToDelete.forEach { repository.deleteProduct(it) }

            val products = when (category) {
                "Hospedaje" -> listOf(
                    Product(name = "Habitación Simple", basePrice = 50.0, category = "Hospedaje", isDefault = true)
                )
                "Alimentación" -> listOf(
                    Product(name = "Almuerzo del día", basePrice = 15.0, category = "Alimentación", isDefault = true)
                )
                "Artesanía" -> listOf(
                    Product(name = "Artesanía de la zona", basePrice = 25.0, category = "Artesanía", isDefault = true)
                )
                "Varios" -> listOf(
                    Product(name = "Habitación", basePrice = 50.0, category = "Hospedaje", isDefault = true),
                    Product(name = "Almuerzo", basePrice = 15.0, category = "Alimentación", isDefault = true),
                    Product(name = "Recuerdo / Artesanía", basePrice = 20.0, category = "Artesanía", isDefault = true)
                )
                else -> emptyList()
            }
            if (products.isNotEmpty()) {
                repository.insertProducts(products)
            }
        }
    }

    suspend fun getVisitDetail(id: Int): Visit? {
        return repository.getVisitById(id)
    }

    fun saveLanguage(language: String) {
        viewModelScope.launch {
            val current = repository.getSettingsOnce() ?: AppSettings()
            repository.saveSettings(current.copy(
                language = language,
                lastModified = System.currentTimeMillis()
            ))
        }
    }

    fun saveProfile(name: String, category: String, profilePicture: ByteArray? = null) {
        viewModelScope.launch {
            val current = repository.getSettingsOnce() ?: AppSettings()
            val oldCategory = current.businessCategory
            
            // Si cambia la categoría y no es "Varios", borrar productos que no pertenezcan
            if (category != oldCategory && category != "Varios") {
                val allProducts = repository.allProducts.first()
                val productsToDelete = allProducts.filter { it.category != category }
                productsToDelete.forEach { repository.deleteProduct(it) }
            }

            repository.saveSettings(current.copy(
                businessName = name,
                businessCategory = category,
                profilePicture = profilePicture ?: current.profilePicture,
                isOnboardingCompleted = true,
                lastModified = System.currentTimeMillis()
            ))
        }
    }

    fun updatePreferredCurrency(currency: String) {
        viewModelScope.launch {
            val current = repository.getSettingsOnce() ?: AppSettings()
            repository.saveSettings(current.copy(
                preferredCurrency = currency,
                lastModified = System.currentTimeMillis()
            ))
        }
    }

    fun updateExchangeRates(usd: Double, eur: Double) {
        viewModelScope.launch {
            val current = repository.getSettingsOnce() ?: AppSettings()
            repository.saveSettings(current.copy(
                usdExchangeRate = usd,
                eurExchangeRate = eur,
                lastModified = System.currentTimeMillis()
            ))
        }
    }

    fun updateVoiceSpeed(speed: Float) {
        viewModelScope.launch {
            val current = repository.getSettingsOnce() ?: AppSettings()
            repository.saveSettings(current.copy(
                voiceSpeed = speed,
                lastModified = System.currentTimeMillis()
            ))
        }
    }

    fun linkAccount(email: String, password: String) {
        viewModelScope.launch {
            val current = repository.getSettingsOnce() ?: AppSettings()
            repository.saveSettings(current.copy(
                isLinked = true,
                accountEmail = email,
                accountPassword = password,
                lastModified = System.currentTimeMillis()
            ))
        }
    }

    fun clearAllAppData() {
        viewModelScope.launch {
            repository.clearAllData()
        }
    }

    fun updateProfilePicture(bitmap: Bitmap, saveImmediately: Boolean = true) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val finalWidth = if (bitmap.width > 128) 128 else bitmap.width
                val finalHeight = if (bitmap.height > 128) 128 else bitmap.height
                
                val resizedBitmap = if (bitmap.width > 128 || bitmap.height > 128) {
                    Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true)
                } else {
                    bitmap
                }

                val outputStream = ByteArrayOutputStream()
                resizedBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                val byteArray = outputStream.toByteArray()

                if (saveImmediately) {
                    val current = repository.getSettingsOnce() ?: AppSettings()
                    repository.saveSettings(current.copy(
                        profilePicture = byteArray,
                        lastModified = System.currentTimeMillis()
                    ))
                }
                
                if (resizedBitmap != bitmap) resizedBitmap.recycle()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateProfilePictureFromUri(uri: Uri) {
        val contentResolver = getApplication<Application>().contentResolver
        viewModelScope.launch(Dispatchers.IO) {
            try {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val originalBitmap = BitmapFactory.decodeStream(inputStream)
                    originalBitmap?.let { updateProfilePicture(it) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadCountryFeatures() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val geoJsonStr = context.assets.open("countries5.json").bufferedReader().use { it.readText() }
                val geoJson = JSONObject(geoJsonStr)
                val features = geoJson.optJSONArray("features") ?: return@launch
                val parsedFeatures = mutableListOf<CountryFeature>()

                for (i in 0 until features.length()) {
                    val feature = features.optJSONObject(i) ?: continue
                    val properties = feature.optJSONObject("properties") ?: continue
                    val countryName = properties.optString("name", "Unknown")
                    val geometry = feature.optJSONObject("geometry") ?: continue
                    val type = geometry.optString("type")
                    val polygons = mutableListOf<List<GeoPoint>>()

                    if (type == "Polygon") {
                        val coordinates = geometry.optJSONArray("coordinates") ?: continue
                        val ring = coordinates.optJSONArray(0) ?: continue
                        polygons.add(parseRing(ring))
                    } else if (type == "MultiPolygon") {
                        val multiCoords = geometry.optJSONArray("coordinates") ?: continue
                        for (j in 0 until multiCoords.length()) {
                            val coords = multiCoords.optJSONArray(j) ?: continue
                            val ring = coords.optJSONArray(0) ?: continue
                            polygons.add(parseRing(ring))
                        }
                    }
                    
                    val centroid = if (polygons.isNotEmpty()) {
                        val mainRing = polygons.maxByOrNull { it.size } ?: polygons.first()
                        var sumLat = 0.0
                        var sumLon = 0.0
                        mainRing.forEach { 
                            sumLat += it.latitude
                            sumLon += it.longitude
                        }
                        GeoPoint(sumLat / mainRing.size, sumLon / mainRing.size)
                    } else null

                    val displayPoint = if (centroid != null && polygons.isNotEmpty()) {
                        val mainRing = polygons.maxByOrNull { it.size } ?: polygons.first()
                        val vertex = mainRing[mainRing.size / 3]
                        GeoPoint(
                            (centroid.latitude + vertex.latitude) / 2.0,
                            (centroid.longitude + vertex.longitude) / 2.0
                        )
                    } else centroid
                    
                    parsedFeatures.add(CountryFeature(countryName, polygons, centroid, displayPoint))
                }
                _countryFeatures.value = parsedFeatures
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun parseRing(coordinates: org.json.JSONArray): List<GeoPoint> {
        val points = mutableListOf<GeoPoint>()
        for (i in 0 until coordinates.length()) {
            val coord = coordinates.optJSONArray(i) ?: continue
            if (coord.length() >= 2) {
                points.add(GeoPoint(coord.optDouble(1), coord.optDouble(0)))
            }
        }
        return points
    }
}
