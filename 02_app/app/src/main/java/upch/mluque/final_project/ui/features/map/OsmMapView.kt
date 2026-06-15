package upch.mluque.final_project.ui.features.map

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
import kotlinx.coroutines.delay
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polygon
import upch.mluque.final_project.data.local.Visit
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
    showLabels: Boolean = false,
    viewMode: MapViewMode = MapViewMode.POINTS
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val configuration = LocalConfiguration.current
    
    val countryFeatures by viewModel.countryFeatures.collectAsState()

    val waterColor = if (isDark) 0xFF0A1120.toInt() else 0xFFE0F7FA.toInt()
    val countryFillColor = if (isDark) 0xFF1B253D.toInt() else 0xFFF0F0F0.toInt()
    val countryStrokeColor = if (isDark) 0xFF2A3A5A.toInt() else 0xFFE0E0E0.toInt()
    val textColor = if (isDark) 0xFF8BA4D0.toInt() else 0xFF000000.toInt()

    val colorPrimary = MaterialTheme.colorScheme.primary.toArgb()
    val colorSecondary = MaterialTheme.colorScheme.secondary.toArgb()
    val colorTertiary = MaterialTheme.colorScheme.tertiary.toArgb()
    val palette = listOf(colorPrimary, colorSecondary, colorTertiary)

    val stats = remember(visits) {
        val allCounts = mutableListOf<Int>()
        visits.forEach { v ->
            v.selectedProducts.forEach { allCounts.add(it.quantity) }
        }
        val gMin = allCounts.minOrNull()?.toFloat() ?: 1f
        val gMax = allCounts.maxOfOrNull { it }?.toFloat() ?: 1f
        Triple(gMin, gMax, 50f)
    }

    var baseZoom by remember { mutableDoubleStateOf(1.0) }
    var hasZoomedToFit by remember { mutableStateOf(false) }

    val countryOverlays = remember(countryFeatures, countryFillColor, countryStrokeColor) {
        countryFeatures.flatMap { feature ->
            feature.polygons.filter { it.isNotEmpty() }.map { points ->
                Polygon().apply {
                    this.points = points
                    fillPaint.color = countryFillColor
                    outlinePaint.color = countryStrokeColor
                    outlinePaint.strokeWidth = 1f
                }
            }
        }
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
                    overlayManager.tilesOverlay.isEnabled = false
                    controller.setZoom(zoomLevel)
                    controller.setCenter(center)
                }
            },
            update = { mapView ->
                mapView.setBackgroundColor(waterColor)
                
                if (mapView.height > 0 && !hasZoomedToFit) {
                    val targetPixelWidth = if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) mapView.height.toDouble() else mapView.width.toDouble()
                    baseZoom = max(1.0, ln(targetPixelWidth / 256.0) / ln(2.0))
                    mapView.minZoomLevel = baseZoom
                    mapView.controller.setZoom(if (isInteractive) zoomLevel else baseZoom)
                    hasZoomedToFit = true
                }

                val currentOverlays = mapView.overlays
                if (currentOverlays.isEmpty()) {
                    currentOverlays.addAll(countryOverlays)
                }

                val dynamicOverlay = object : Overlay() {
                    private val pointPaint = Paint().apply { isAntiAlias = true }
                    private val textPaint = Paint().apply {
                        color = textColor
                        textSize = 24f
                        textAlign = Paint.Align.CENTER
                        isAntiAlias = true
                        typeface = Typeface.DEFAULT_BOLD
                    }

                    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
                        if (shadow) return
                        
                        if (viewMode == MapViewMode.POINTS) {
                            visits.forEach { visit ->
                                val jsonName = mapping[visit.nationality] ?: visit.nationality
                                countryFeatures.find { it.name == jsonName }?.let { feat ->
                                    val gp = getStablePointForVisit(visit, feat)
                                    val point = mapView.projection.toPixels(gp, null)
                                    pointPaint.color = colorPrimary
                                    pointPaint.style = Paint.Style.FILL
                                    canvas.drawCircle(point.x.toFloat(), point.y.toFloat(), 10f, pointPaint)
                                }
                            }
                        } else {
                            visits.groupBy { mapping[it.nationality] ?: it.nationality }.forEach { (countryName, countryVisits) ->
                                val feature = countryFeatures.find { it.name == countryName } ?: return@forEach
                                val prodCounts = mutableMapOf<String, Int>()
                                countryVisits.forEach { v -> v.selectedProducts.forEach { prodCounts[it.name] = prodCounts.getOrDefault(it.name, 0) + it.quantity } }
                                
                                prodCounts.toList().sortedByDescending { it.second }.take(3).forEachIndexed { index, (name, count) ->
                                    val gp = getStablePointForService(feature, index)
                                    val point = mapView.projection.toPixels(gp, null)
                                    val radius = 15f + (count.toFloat() / stats.second) * 30f
                                    pointPaint.color = palette.getOrElse(index) { colorPrimary }
                                    pointPaint.alpha = 160
                                    canvas.drawCircle(point.x.toFloat(), point.y.toFloat(), radius, pointPaint)
                                }
                            }
                        }

                        if (showLabels && mapView.zoomLevelDouble > baseZoom + 1) {
                            countryFeatures.forEach { feature ->
                                feature.centroid?.let { centroid ->
                                    val point = mapView.projection.toPixels(centroid, null)
                                    canvas.drawText(feature.name, point.x.toFloat(), point.y.toFloat(), textPaint)
                                }
                            }
                        }
                    }
                }
                
                if (currentOverlays.isNotEmpty() && currentOverlays.last() !is Polygon) {
                    currentOverlays.removeAt(currentOverlays.size - 1)
                }
                currentOverlays.add(dynamicOverlay)
                mapView.postInvalidate()
            }
        )
    }
}

private fun getStablePointForService(feature: upch.mluque.final_project.data.model.CountryFeature, index: Int): GeoPoint {
    val polygon = feature.polygons.maxByOrNull { it.size } ?: return feature.centroid ?: GeoPoint(0.0, 0.0)
    val v = polygon[(index * (polygon.size / 3)) % polygon.size]
    val c = feature.centroid ?: v
    return GeoPoint((v.latitude + c.latitude) / 2.0, (v.longitude + c.longitude) / 2.0)
}

private fun getStablePointForVisit(visit: Visit, feature: upch.mluque.final_project.data.model.CountryFeature): GeoPoint {
    val polygon = feature.polygons.maxByOrNull { it.size } ?: return feature.centroid ?: GeoPoint(0.0, 0.0)
    val v = polygon[(visit.id * 7) % polygon.size]
    val c = feature.centroid ?: v
    return GeoPoint((v.latitude * 0.4 + c.latitude * 0.6), (v.longitude * 0.4 + c.longitude * 0.6))
}
