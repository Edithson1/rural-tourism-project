package upch.mluque.final_project.data.model

import org.osmdroid.util.GeoPoint

/**
 * Data class to cache parsed country data from GeoJSON.
 */
data class CountryFeature(
    val name: String,
    val polygons: List<List<GeoPoint>>,
    val centroid: GeoPoint?,
    val displayPoint: GeoPoint? = null
)