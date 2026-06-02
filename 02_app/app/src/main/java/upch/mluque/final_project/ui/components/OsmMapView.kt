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
import upch.mluque.final_project.ui.CountryFeature
import upch.mluque.final_project.ui.MainViewModel
import kotlin.math.ln
import kotlin.math.max

enum class MapViewMode {
    POINTS, BUBBLES
}

@Composable
fun OsmMapView(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel,
    visits: List<Visit>,
    isInteractive: Boolean = true,
    zoomLevel: Double = 2.0,
    center: GeoPoint = GeoPoint(0.0, 0.0),
    showLabels: Boolean = false
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val configuration = LocalConfiguration.current
    
    val countryFeatures by viewModel.countryFeatures.collectAsState()

    // Colors and Paints
    val waterColor = if (isDark) 0xFF0A1120.toInt() else 0xFFE0F7FA.toInt()
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    val countryFillColor = if (isDark) {
        if (isPortrait) 0xFF354B7A.toInt() else 0xFF1B253D.toInt()
    } else 0xFFF0F0F0.toInt()
    val countryStrokeColor = if (isDark) 0xFF2A3A5A.toInt() else 0xFFE0E0E0.toInt()
    val textColor = if (isDark) 0xFF8BA4D0.toInt() else 0xFF000000.toInt()

    val colorPrimary = MaterialTheme.colorScheme.primary.toArgb()
    val colorSecondary = MaterialTheme.colorScheme.secondary.toArgb()
    val colorTertiary = MaterialTheme.colorScheme.tertiary.toArgb()

    val stats = remember(visits) {
        val mappingMap = mapOf(
            "Perú" to "Peru", "México" to "Mexico", "España" to "Spain",
            "Estados Unidos" to "United States", "Brasil" to "Brazil",
            "Francia" to "France", "Alemania" to "Germany", "Reino Unido" to "United Kingdom"
        )
        val visitsByCountry = visits.groupBy { mappingMap[it.nationality] ?: it.nationality }
        val allCounts = mutableListOf<Int>()
        visitsByCountry.forEach { (_, countryVisits) ->
            val counts = mutableMapOf("Hospedaje" to 0, "Alimentación" to 0, "Artesanía" to 0)
            countryVisits.forEach { v ->
                v.services.split(", ").forEach { s -> if (counts.containsKey(s)) counts[s] = counts[s]!! + 1 }
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

    var baseZoom by remember { mutableDoubleStateOf(1.0) }
    var hasZoomedToFit by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf(MapViewMode.POINTS) }
    var showModeMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(showModeMessage) {
        if (showModeMessage != null) {
            delay(2000)
            showModeMessage = null
        }
    }

    // Reuse overlays across recompositions
    val countryOverlays = remember(countryFeatures, countryFillColor, countryStrokeColor) {
        countryFeatures.flatMap { feature ->
            feature.polygons.filter { it.isNotEmpty() }.map { points ->
                Polygon().apply {
                    this.points = points
                    fillPaint.color = countryFillColor
                    fillPaint.style = Paint.Style.FILL
                    outlinePaint.color = countryStrokeColor
                    outlinePaint.strokeWidth = 1f
                }
            }
        }
    }

    val serviceColors = remember(colorPrimary, colorSecondary, colorTertiary) {
        mapOf("Hospedaje" to colorPrimary, "Alimentación" to colorSecondary, "Artesanía" to colorTertiary)
    }

    val mapping = remember {
        mapOf(
            "Perú" to "Peru", "México" to "Mexico", "España" to "Spain",
            "Estados Unidos" to "United States", "Brasil" to "Brazil",
            "Francia" to "France", "Alemania" to "Germany", "Reino Unido" to "United Kingdom"
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
                    isHorizontalMapRepetitionEnabled = true
                    isVerticalMapRepetitionEnabled = false
                    setScrollableAreaLimitDouble(BoundingBox(85.0, 180.0, -85.0, -180.0))
                    overlayManager.tilesOverlay.isEnabled = false
                    controller.setZoom(zoomLevel)
                    controller.setCenter(center)
                }
            },
            update = { mapView ->
                mapView.setBackgroundColor(waterColor)
                
                // Re-calculate zoom on orientation change or initial load
                if (mapView.height > 0 && (!hasZoomedToFit || mapView.tag != configuration.orientation)) {
                    val isLandscapeOrientation = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                    val targetPixelWidth = if (isLandscapeOrientation) {
                        mapView.height.toDouble()
                    } else {
                        mapView.width.toDouble()
                    }
                    val calculatedBaseZoom = ln(targetPixelWidth / 256.0) / ln(2.0)
                    baseZoom = max(1.0, calculatedBaseZoom)
                    mapView.minZoomLevel = baseZoom
                    
                    // Set zoom based on context (interactive fullscreen vs small preview)
                    val finalZoom = if (isInteractive) zoomLevel else baseZoom
                    mapView.controller.setZoom(finalZoom)
                    mapView.controller.setCenter(if (isInteractive) center else GeoPoint(0.0, 0.0))
                    
                    mapView.tag = configuration.orientation
                    hasZoomedToFit = true
                }

                // Efficiently update overlays
                val currentOverlays = mapView.overlays
                if (currentOverlays.isEmpty() || currentOverlays.size < countryOverlays.size) {
                    currentOverlays.clear()
                    currentOverlays.addAll(countryOverlays)
                }

                // Add or update the dynamic overlay (points/bubbles)
                val dynamicOverlay = object : Overlay() {
                    private val textPaint = Paint().apply {
                        color = textColor
                        textSize = 24f
                        textAlign = Paint.Align.CENTER
                        isAntiAlias = true
                        typeface = Typeface.DEFAULT_BOLD
                    }
                    private val pointPaint = Paint().apply { isAntiAlias = true }

                    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
                        if (shadow) return
                        val zoomLevelThreshold = baseZoom + 1.0
                        
                        if (viewMode == MapViewMode.POINTS) {
                            visits.forEach { visit ->
                                val jsonName = mapping[visit.nationality] ?: visit.nationality
                                countryFeatures.find { it.name == jsonName }?.let { feat ->
                                    val gp = getStablePointForVisit(visit, feat)
                                    val point = mapView.projection.toPixels(gp, null)
                                    if (point.x in 0..mapView.width && point.y in 0..mapView.height) {
                                        pointPaint.color = serviceColors[visit.services.split(", ").firstOrNull()] ?: colorPrimary
                                        pointPaint.style = Paint.Style.FILL
                                        canvas.drawCircle(point.x.toFloat(), point.y.toFloat(), 10f, pointPaint)
                                        pointPaint.style = Paint.Style.STROKE
                                        pointPaint.color = 0xFFFFFFFF.toInt()
                                        pointPaint.strokeWidth = 2f
                                        canvas.drawCircle(point.x.toFloat(), point.y.toFloat(), 10f, pointPaint)
                                    }
                                }
                            }
                        } else {
                            visits.groupBy { mapping[it.nationality] ?: it.nationality }.forEach { (countryName, countryVisits) ->
                                val feature = countryFeatures.find { it.name == countryName } ?: return@forEach
                                val counts = mutableMapOf("Hospedaje" to 0, "Alimentación" to 0, "Artesanía" to 0)
                                countryVisits.forEach { v -> v.services.split(", ").forEach { s -> if (counts.containsKey(s)) counts[s] = counts[s]!! + 1 } }
                                
                                listOf("Hospedaje", "Alimentación", "Artesanía").forEachIndexed { index, service ->
                                    val count = counts[service] ?: 0
                                    if (count <= 0) return@forEachIndexed
                                    val gp = getStablePointForService(feature, index)
                                    val point = mapView.projection.toPixels(gp, null)
                                    if (point.x in 0..mapView.width && point.y in 0..mapView.height) {
                                        val minVal = stats.first
                                        val maxVal = stats.second
                                        val visualMax = stats.third
                                        val visualMin = 10f
                                        
                                        val radius = if (maxVal <= minVal) {
                                            visualMax
                                        } else {
                                            val logV = ln(count.toFloat())
                                            val logMin = ln(max(1f, minVal))
                                            val logMax = ln(max(2f, maxVal))
                                            val ratio = (logV - logMin) / (logMax - logMin)
                                            visualMin + ratio.toFloat() * (visualMax - visualMin)
                                        }

                                        val color = serviceColors[service] ?: colorPrimary
                                        pointPaint.style = Paint.Style.FILL
                                        pointPaint.color = color
                                        pointPaint.alpha = 160
                                        canvas.drawCircle(point.x.toFloat(), point.y.toFloat(), radius, pointPaint)
                                        pointPaint.style = Paint.Style.STROKE
                                        pointPaint.alpha = 255
                                        pointPaint.strokeWidth = 2f
                                        canvas.drawCircle(point.x.toFloat(), point.y.toFloat(), radius, pointPaint)
                                    }
                                }
                            }
                        }

                        if (showLabels && configuration.orientation == Configuration.ORIENTATION_LANDSCAPE && mapView.zoomLevelDouble >= zoomLevelThreshold) {
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
                
                // Keep exactly one dynamic overlay at the end
                if (currentOverlays.isNotEmpty() && currentOverlays.last() !is Polygon) {
                    currentOverlays.removeAt(currentOverlays.size - 1)
                }
                currentOverlays.add(dynamicOverlay)

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
    
    val vertexIndex = (index * (polygon.size / 3) + random.nextInt(max(1, polygon.size / 6))) % polygon.size
    val v = polygon[vertexIndex]
    val c = feature.centroid ?: v
    
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
    
    val vertexIndex = (visit.id * 7) % polygon.size
    val v = polygon[vertexIndex]
    val c = feature.centroid ?: v
    
    val weightCentroid = 0.4 + random.nextDouble() * 0.5
    val weightVertex = 1.0 - weightCentroid
    
    return GeoPoint(
        v.latitude * weightVertex + c.latitude * weightCentroid,
        v.longitude * weightVertex + c.longitude * weightCentroid
    )
}

