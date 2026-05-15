package upch.mluque.final_project.data.local

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    @TypeConverter
    fun fromMap(value: Map<String, String>): String {
        return Json.encodeToString(value)
    }

    @TypeConverter
    fun toMap(value: String): Map<String, String> {
        return try {
            Json.decodeFromString(value)
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
