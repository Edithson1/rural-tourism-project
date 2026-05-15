package upch.mluque.final_project.ui.components

import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polygon
import upch.mluque.final_project.data.local.Visit
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sqrt

enum class MapViewMode {
    POINTS, BUBBLES
}

// Data class to cache parsed country data
data class CountryFeature(
    val name: String,
    val polygons: List<List<GeoPoint>>,
    val centroid: GeoPoint?,
    val displayPoint: GeoPoint? = null
)

@Composable
fun OsmMapView(
    modifier: Modifier = Modifier,
    visits: List<Visit>,
    isInteractive: Boolean = true,
    zoomLevel: Double = 2.0,
    center: GeoPoint = GeoPoint(0.0, 0.0),
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val configuration = LocalConfiguration.current
    
    // Colors based on theme - More bluish in dark mode
    val waterColor = if (isDark) 0xFF0A1120.toInt() else 0xFFE0F7FA.toInt()
    
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    val countryFillColorDefault = if (isDark) {
        if (isPortrait) 0xFF354B7A.toInt() else 0xFF1B253D.toInt() // Brighter blue in portrait for contrast
    } else 0xFFF0F0F0.toInt() // Slightly brighter land in light mode
    
    val countryStrokeColor = if (isDark) 0xFF2A3A5A.toInt() else 0xFFE0E0E0.toInt()
    val textColor = if (isDark) 0xFF8BA4D0.toInt() else 0xFF000000.toInt()
    
    val colorPrimary = MaterialTheme.colorScheme.primary.toArgb()
    val colorSecondary = MaterialTheme.colorScheme.secondary.toArgb()
    val colorTertiary = MaterialTheme.colorScheme.tertiary.toArgb()
    
    val globalStats = remember(visits) {
        val mappingMap = mapOf(
            "Perú" to "Peru",
            "México" to "Mexico",
            "España" to "Spain",
            "Estados Unidos" to "United States",
            "Brasil" to "Brazil",
            "Francia" to "France",
            "Alemania" to "Germany",
            "Reino Unido" to "United Kingdom"
        )
        val visitsByCountry = visits.groupBy { mappingMap[it.nationality] ?: it.nationality }
        val allCounts = mutableListOf<Int>()
        visitsByCountry.forEach { (_, countryVisits) ->
            val counts = mutableMapOf("Hospedaje" to 0, "Alimentación" to 0, "Artesanía" to 0)
            countryVisits.forEach { v ->
                v.services.split(", ").forEach { s ->
                    if (counts.containsKey(s)) counts[s] = counts[s]!! + 1
                }
            }
            allCounts.addAll(counts.values.filter { it > 0 })
        }
        
        val gMin = allCounts.minOrNull()?.toFloat() ?: 1f
        val gMax = allCounts.maxOrNull()?.toFloat() ?: 1f
        
        val maxVisualRadius = when {
            gMax <= 10f -> 25f
            gMax <= 100f -> 50f
            else -> 80f
        }
        
        Triple(gMin, gMax, maxVisualRadius)
    }
    
    var baseZoom by remember { mutableStateOf(1.0) }
    var hasZoomedToFit by remember { mutableStateOf(false) }
    
    var viewMode by remember { mutableStateOf(MapViewMode.POINTS) }
    var showModeMessage by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(showModeMessage) {
        if (showModeMessage != null) {
            delay(2000)
            showModeMessage = null
        }
    }

    // Cache for GeoJSON data to avoid re-parsing on every recomposition or theme change
    var countryFeatures by remember { mutableStateOf<List<CountryFeature>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val geoJsonStr = context.assets.open("countries5.json").bufferedReader().use { it.readText() }
                val geoJson = JSONObject(geoJsonStr)
                val features = geoJson.optJSONArray("features") ?: return@withContext
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
                    
                    // Calculate centroid for name placement and a slightly offset display point
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
                        // Pick a point between centroid and a vertex to make it "random" but likely inside
                        val vertex = mainRing[mainRing.size / 3]
                        GeoPoint(
                            (centroid.latitude + vertex.latitude) / 2.0,
                            (centroid.longitude + vertex.longitude) / 2.0
                        )
                    } else centroid
                    
                    parsedFeatures.add(CountryFeature(countryName, polygons, centroid, displayPoint))
                }
                countryFeatures = parsedFeatures
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Service color mapping
    val serviceColors = remember(colorPrimary, colorSecondary, colorTertiary) {
        mapOf(
            "Hospedaje" to colorPrimary,
            "Alimentación" to colorSecondary,
            "Artesanía" to colorTertiary
        )
    }

    val mapping = remember {
        mapOf(
            "Perú" to "Peru",
            "México" to "Mexico",
            "España" to "Spain",
            "Estados Unidos" to "United States",
            "Brasil" to "Brazil",
            "Francia" to "France",
            "Alemania" to "Germany",
            "Reino Unido" to "United Kingdom"
        )
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setUseDataConnection(false)
                    setMultiTouchControls(isInteractive)
                    zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
                    
                    // Infinite horizontal scroll, finite vertical
                    isHorizontalMapRepetitionEnabled = true
                    isVerticalMapRepetitionEnabled = false
                    setScrollableAreaLimitDouble(BoundingBox(85.0, 180.0, -85.0, -180.0))

                    // Disable background tiles to only show polygons
                    overlayManager.tilesOverlay.isEnabled = false
                    
                    controller.setZoom(zoomLevel)
                    controller.setCenter(center)

                    if (!isInteractive) {
                        @Suppress("ClickableViewAccessibility")
                        setOnTouchListener { _, _ -> true }
                    }
                }
            },
            update = { mapView ->
                // Force recomposition tracking by reading state variables here
                val currentMode = viewMode
                val currentConfig = configuration
                val currentWaterColor = waterColor
                val currentFillColor = countryFillColorDefault
                val currentStrokeColor = countryStrokeColor
                val currentTextColor = textColor
                val stats = globalStats

                mapView.setBackgroundColor(currentWaterColor)
                
                // Calculate base zoom when dimensions are available
                if (!hasZoomedToFit && mapView.height > 0) {
                    // Requirement: Map width (360 deg) = screen height (short axis in landscape)
                    val targetPixelWidth = if (currentConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        mapView.height.toDouble()
                    } else {
                        mapView.width.toDouble()
                    }
                    
                    // Zoom level Z is defined as: Width = 256 * 2^Z
                    // So Z = log2(Width / 256)
                    val calculatedBaseZoom = ln(targetPixelWidth / 256.0) / ln(2.0)
                    baseZoom = max(1.0, calculatedBaseZoom)
                    
                    mapView.minZoomLevel = baseZoom
                    mapView.controller.setZoom(baseZoom)
                    mapView.controller.setCenter(GeoPoint(0.0, 0.0))
                    hasZoomedToFit = true
                }

                // Use overlayManager to ensure clean state
                mapView.overlayManager.clear()

                countryFeatures.forEach { feature ->
                    // All countries use the default fill color
                    val fillColor = currentFillColor
                    feature.polygons.forEach { points ->
                        if (points.isNotEmpty()) {
                            val polygon = Polygon(mapView)
                            polygon.points = points
                            polygon.fillPaint.color = fillColor
                            polygon.fillPaint.style = Paint.Style.FILL
                            polygon.outlinePaint.color = currentStrokeColor
                            polygon.outlinePaint.strokeWidth = 1f
                            mapView.overlays.add(polygon)
                        }
                    }
                }
                
                // Add service points and names overlay
                val namesOverlay = object : Overlay() {
                    private val textPaint = Paint().apply {
                        color = currentTextColor
                        textSize = 24f
                        textAlign = Paint.Align.CENTER
                        isAntiAlias = true
                        typeface = Typeface.DEFAULT_BOLD
                    }
                    
                    private val pointPaint = Paint().apply {
                        style = Paint.Style.FILL
                        isAntiAlias = true
                    }

                    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
                        if (shadow) return
                        
                        val isLandscape = currentConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
                        val zoomLevelThreshold = baseZoom + 1.0
                        
                        if (currentMode == MapViewMode.POINTS) {
                            // Draw service points for ALL visits individually
                            visits.forEach { visit ->
                                val jsonName = mapping[visit.nationality] ?: visit.nationality
                                val feature = countryFeatures.find { it.name == jsonName }
                                val serviceName = visit.services.split(", ").firstOrNull()
                                val serviceColor = serviceColors[serviceName] ?: colorPrimary
                                
                                feature?.let { feat ->
                                    // Generate a stable point for this specific visit
                                    val gp = getStablePointForVisit(visit, feat)
                                    val point = mapView.projection.toPixels(gp, null)
                                    
                                    if (point.x in 0..mapView.width && point.y in 0..mapView.height) {
                                        pointPaint.color = serviceColor
                                        canvas.drawCircle(point.x.toFloat(), point.y.toFloat(), 10f, pointPaint)
                                        // White border for contrast
                                        pointPaint.style = Paint.Style.STROKE
                                        pointPaint.color = 0xFFFFFFFF.toInt()
                                        pointPaint.strokeWidth = 2f
                                        canvas.drawCircle(point.x.toFloat(), point.y.toFloat(), 10f, pointPaint)
                                        pointPaint.style = Paint.Style.FILL
                                    }
                                }
                            }
                        } else {
                            // Bubbles Mode: 3 bubbles per country with visits
                            val visitsByCountry = visits.groupBy { mapping[it.nationality] ?: it.nationality }
                            
                            visitsByCountry.forEach { (countryName, countryVisits) ->
                                val feature = countryFeatures.find { it.name == countryName } ?: return@forEach
                                
                                // Service counts for this country
                                val counts = mutableMapOf("Hospedaje" to 0, "Alimentación" to 0, "Artesanía" to 0)
                                countryVisits.forEach { v ->
                                    v.services.split(", ").forEach { s ->
                                        if (counts.containsKey(s)) counts[s] = counts[s]!! + 1
                                    }
                                }
                                
                                // Draw 3 bubbles (one for each service type)
                                val services = listOf("Hospedaje", "Alimentación", "Artesanía")
                                services.forEachIndexed { index, service ->
                                    val count = counts[service] ?: 0
                                    if (count <= 0) return@forEachIndexed
                                    
                                    val color = serviceColors[service] ?: colorPrimary
                                    
                                    // Stable random point based on country and service index
                                    val gp = getStablePointForService(feature, index)
                                    val point = mapView.projection.toPixels(gp, null)
                                    
                                    if (point.x in 0..mapView.width && point.y in 0..mapView.height) {
                                        // Global scale calculation
                                        val minVal = stats.first
                                        val maxVal = stats.second
                                        val visualMax = stats.third
                                        val visualMin = 10f
                                        
                                        val radius = if (maxVal <= minVal) {
                                            visualMax
                                        } else {
                                            val logV = ln(count.toFloat())
                                            val logMin = ln(minVal)
                                            val logMax = ln(maxVal)
                                            val ratio = (logV - logMin) / (logMax - logMin)
                                            visualMin + ratio.toFloat() * (visualMax - visualMin)
                                        }
                                        
                                        pointPaint.color = color
                                        pointPaint.alpha = 160 // Semi-transparent
                                        canvas.drawCircle(point.x.toFloat(), point.y.toFloat(), radius, pointPaint)
                                        
                                        // Border for bubble
                                        pointPaint.style = Paint.Style.STROKE
                                        pointPaint.color = color
                                        pointPaint.alpha = 255
                                        pointPaint.strokeWidth = 2f
                                        canvas.drawCircle(point.x.toFloat(), point.y.toFloat(), radius, pointPaint)
                                        pointPaint.style = Paint.Style.FILL
                                    }
                                }
                            }
                        }

                        // Show names based on logic
                        if (isLandscape && mapView.zoomLevelDouble >= zoomLevelThreshold) {
                            countryFeatures.forEach { feature ->
                                feature.centroid?.let { centroid ->
                                    val point = mapView.projection.toPixels(centroid, null)
                                    if (point.x in 0..mapView.width && point.y in 0..mapView.height) {
                                        canvas.drawText(feature.name, point.x.toFloat(), point.y.toFloat(), textPaint)
                                    }
                                }
                            }
                        }
                    }
                }
                mapView.overlays.add(namesOverlay)

                mapView.postInvalidate()
            },
            onRelease = { mapView ->
                mapView.onDetach()
            }
        )

        // View Mode Toggle Button
        Box(
            modifier = Modifier
                .padding(16.dp)
                .align(if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) Alignment.BottomEnd else Alignment.BottomStart)
        ) {
            val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
            
            FloatingActionButton(
                onClick = {
                    viewMode = if (viewMode == MapViewMode.POINTS) MapViewMode.BUBBLES else MapViewMode.POINTS
                    showModeMessage = if (viewMode == MapViewMode.POINTS) "Modo Puntos" else "Modo Burbujas"
                },
                modifier = if (isPortrait) Modifier.size(40.dp) else Modifier.size(48.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
            ) {
                Icon(
                    imageVector = if (viewMode == MapViewMode.POINTS) Icons.Default.LocationOn else Icons.Default.BubbleChart,
                    contentDescription = "Cambiar modo de vista",
                    modifier = Modifier.size(if (isPortrait) 20.dp else 24.dp)
                )
            }
        }
        
        // Brief mode indicator text
        AnimatedVisibility(
            visible = showModeMessage != null,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 64.dp)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = showModeMessage ?: "",
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun getStablePointForService(feature: CountryFeature, index: Int): GeoPoint {
    val polygons = feature.polygons
    if (polygons.isEmpty()) return feature.centroid ?: GeoPoint(0.0, 0.0)
    
    val seed = feature.name.hashCode().toLong() + index * 12345L
    val random = java.util.Random(seed)
    
    val polygon = polygons.maxByOrNull { it.size } ?: polygons.first()
    if (polygon.size < 3) return feature.centroid ?: GeoPoint(0.0, 0.0)
    
    // To maximize distance between the 3 bubbles:
    // We can use the index to pick different "sectors" or vertices far apart
    val vertexIndex = (index * (polygon.size / 3) + random.nextInt(max(1, polygon.size / 6))) % polygon.size
    val v = polygon[vertexIndex]
    val c = feature.centroid ?: v
    
    // Disperse more than before
    val weightCentroid = 0.5 + random.nextDouble() * 0.3
    val weightVertex = 1.0 - weightCentroid
    
    return GeoPoint(
        v.latitude * weightVertex + c.latitude * weightCentroid,
        v.longitude * weightVertex + c.longitude * weightCentroid
    )
}

private fun getStablePointForVisit(visit: Visit, feature: CountryFeature): GeoPoint {
    val polygons = feature.polygons
    if (polygons.isEmpty()) return feature.centroid ?: GeoPoint(0.0, 0.0)
    
    val seed = visit.id.toLong() * 31 + visit.registrationDate
    val random = java.util.Random(seed)
    
    val polygon = polygons.maxByOrNull { it.size } ?: polygons.first()
    if (polygon.size < 3) return feature.centroid ?: GeoPoint(0.0, 0.0)
    
    // Use the visit ID to pick a vertex in a way that spreads points
    val vertexIndex = (visit.id * 7) % polygon.size
    val v = polygon[vertexIndex]
    val c = feature.centroid ?: v
    
    // Spread points within a generous area around centroid but biased by the chosen vertex
    val weightCentroid = 0.4 + random.nextDouble() * 0.5
    val weightVertex = 1.0 - weightCentroid
    
    return GeoPoint(
        v.latitude * weightVertex + c.latitude * weightCentroid,
        v.longitude * weightVertex + c.longitude * weightCentroid
    )
}

private fun parseRing(coordinates: JSONArray): List<GeoPoint> {
    val points = mutableListOf<GeoPoint>()
    for (i in 0 until coordinates.length()) {
        val coord = coordinates.optJSONArray(i) ?: continue
        if (coord.length() >= 2) {
            points.add(GeoPoint(coord.optDouble(1), coord.optDouble(0)))
        }
    }
    return points
}
