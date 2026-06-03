package upch.mluque.final_project.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import upch.mluque.final_project.data.DataRepository
import upch.mluque.final_project.data.local.AppDatabase
import upch.mluque.final_project.data.local.AppSettings
import upch.mluque.final_project.data.local.Visit
import upch.mluque.final_project.data.model.CountryFeature
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.io.ByteArrayOutputStream

// Data ViewModel handling app-wide state
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: DataRepository
    val appSettings: StateFlow<AppSettings?>
    val allVisits: StateFlow<List<Visit>>
    
    private val _countryFeatures = MutableStateFlow<List<CountryFeature>>(emptyList())
    val countryFeatures: StateFlow<List<CountryFeature>> = _countryFeatures.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = DataRepository(database.appSettingsDao(), database.visitDao())
        
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

        loadCountryFeatures()

        viewModelScope.launch {
            if (repository.getSettingsOnce() == null) {
                repository.saveSettings(AppSettings())
            }
        }
    }

    fun addVisit(nationality: String, flag: String, price: String, services: String) {
        viewModelScope.launch {
            val visit = Visit(
                nationality = nationality,
                nationalityFlag = flag,
                priceApprox = price,
                services = services
            )
            repository.insertVisit(visit)
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
            repository.saveSettings(current.copy(
                businessName = name,
                businessCategory = category,
                profilePicture = profilePicture ?: current.profilePicture,
                isOnboardingCompleted = true,
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