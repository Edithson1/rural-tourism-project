package upch.mluque.final_project.data.local

import kotlinx.serialization.json.Json

object SerializationHelper {
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }
}
