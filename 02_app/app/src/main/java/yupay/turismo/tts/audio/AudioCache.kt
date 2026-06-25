package yupay.turismo.tts.audio

import android.content.Context
import yupay.turismo.tts.SupportedLanguage
import java.io.File
import java.security.MessageDigest

/**
 * Caché en disco del audio TTS generado (WAV) para tips, mapas y resúmenes del dashboard.
 *
 * Vive en `filesDir/tts/audio_cache/` y se borra completa al cerrar sesión
 * ([yupay.turismo.data.AppReset]). La clave incluye idioma + modelo + texto, de modo que al
 * cambiar el texto o la voz activa se genera otro fichero (sin mezclas ni audio desactualizado).
 */
object AudioCache {

    /** Raíz de la caché de audio generado: `filesDir/tts/audio_cache`. */
    fun cacheRoot(context: Context): File = File(context.filesDir, "tts/audio_cache")

    /** Clave de caché = sha256(idioma | modelo | texto). */
    fun keyFor(text: String, language: SupportedLanguage, modelId: String): String =
        sha256("${language.code}|$modelId|$text")

    /** Fichero WAV asociado a una clave. */
    fun fileFor(context: Context, key: String): File = File(cacheRoot(context), "$key.wav")

    /** Borra TODO el audio cacheado (logout / reset). No toca los modelos descargados. */
    fun deleteAll(context: Context) {
        cacheRoot(context).deleteRecursively()
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
