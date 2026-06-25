# Text-to-Speech (Sherpa-ONNX) — Yupay Turismo

Motor TTS **único** basado en [Sherpa-ONNX](https://github.com/k2-fsa/sherpa-onnx) (NO se usa el TTS
nativo de Android). 4 idiomas: español (es), inglés (en), portugués (pt) y quechua Cusco‑Collao (quz).

## ⚠️ Único paso manual: copiar las librerías nativas (.so)

La API Kotlin (`com.k2fsa.sherpa.onnx.*`) va **vendorizada** en `java/com/k2fsa/sherpa/onnx/Tts.kt`.
Falta solo la parte nativa. Descarga el release de Android de Sherpa-ONNX
(`sherpa-onnx-vX.Y.Z-android.tar.bz2`) de
<https://github.com/k2-fsa/sherpa-onnx/releases> y copia los `.so` en:

```
app/src/main/jniLibs/arm64-v8a/   libsherpa-onnx-jni.so   libonnxruntime.so
app/src/main/jniLibs/armeabi-v7a/ libsherpa-onnx-jni.so   libonnxruntime.so
app/src/main/jniLibs/x86_64/      libsherpa-onnx-jni.so   libonnxruntime.so
app/src/main/jniLibs/x86/         libsherpa-onnx-jni.so   libonnxruntime.so
```

- **NO** añadas además el AAR oficial: duplicaría la clase `OfflineTts` (ya vendorizada).
- Sin los `.so` la app **compila y arranca igual**; el TTS solo falla (de forma controlada, con log)
  al activar/reproducir una voz. El resto de la app no se ve afectada.

## Arquitectura (encaja con el `ServiceLocator` del proyecto, sin Hilt)

| Pieza | Archivo | Rol |
|---|---|---|
| Idiomas | `SupportedLanguage.kt` | enum + mapeo desde `AppSettings.language` ("Español"…). |
| Modelos | `TtsModel.kt`, `TtsModelCatalog.kt` | metadatos REALES (URL, bytes, SHA‑256) por idioma. |
| Estado | `TtsModelState.kt` | `StateFlow<TtsModelState>` por modelo. |
| Motor | `TtsEngine.kt`, `engine/SherpaOnnxTtsEngine.kt` | síntesis VITS (Piper/MMS) → PCM. |
| Descarga | `download/TtsDownloadWorker.kt` | WorkManager: descarga + SHA‑256 + extracción tar.bz2 + cancelar. |
| Repositorio | `download/TtsDownloadRepository.kt` | estados, descargar/eliminar/activar (singleton). |
| Manager | `TtsManager.kt` | preview con AudioTrack (`speak`/`speakWithModel`), parar (singleton). |
| Reproductor | `tts/audio/AudioPlaybackController.kt` + `WavFileWriter.kt` + `AudioCache.kt` | audio "ligado a página" (tips/mapas/dashboard): síntesis→WAV cacheado→MediaPlayer con seek + velocidad (singleton). |
| ViewModel | `TtsViewModel.kt`, `tts/audio/AudioPlaybackViewModel.kt` | API para la UI (`AndroidViewModel`). |
| UI | `ui/features/profile/VoiceModelsScreen.kt` + `VoiceModelComponents.kt`, `ui/components/AudioButton.kt`, `ui/components/AudioPlayerUI.kt` | pantalla "Modelos de voz" + tarjetas/preview + reproductor con seek. |
| Persistencia | `data/local/TtsPreference*.kt` | tabla `tts_preferences` (idioma → modelo activo), Room v12. |
| Caché de audio | `filesDir/tts/audio_cache/<sha256>.wav` | audio generado; se borra al cerrar sesión (`AppReset`). |

Los modelos se guardan en `filesDir/tts/models/<lang>/<id>/` (no van en el APK).

## Catálogo (datos reales, junio 2026)

`LIGHT` = variante cuantizada `-int8` (ahorra datos) · `HIGH` = modelo completo fp32.

| Idioma | Voz | Género | LIGHT | HIGH |
|---|---|---|---|---|
| es | es_ES-davefx (España) | ♂ | 20.2 MB | 64.1 MB |
| es | es_AR-daniela (Argentina) | ♀ | 33.4 MB | 110.2 MB |
| en | en_US-hfc_female | ♀ | 20.0 MB | 64.1 MB |
| en | en_US-hfc_male | ♂ | 20.0 MB | 64.1 MB |
| pt | pt_BR-faber (Brasil) | ♂ | 20.3 MB | 64.1 MB |
| quz | MMS quz (Cusco) | ◯ neutral | — | 108.7 MB |

### Disponibilidad de género (requisito)
- **Portugués**: Sherpa‑ONNX **no tiene voz femenina** (ni pt_BR ni pt_PT son femeninas en Piper).
  Solo se ofrece la masculina (faber) en LIGHT/HIGH.
- **Quechua (quz)**: **no existe** en el release oficial de Sherpa‑ONNX. Se usa la conversión ONNX
  comunitaria de `facebook/mms-tts-quz` (un solo locutor, **género neutral**, sin variantes de
  calidad/género; ~109 MB).
- Español e inglés sí tienen ambos géneros.

### Sobre el SHA‑256
Los hashes del catálogo son los **oficiales** (campo `digest` de la API de releases de GitHub y
`lfs.oid` de HuggingFace). `tokens.txt` del modelo quechua no tiene hash publicado (no es LFS):
la verificación es "blanda" y el worker registra el hash calculado en Logcat por si se quiere fijar.
