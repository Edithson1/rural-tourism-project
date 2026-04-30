# 4. Tecnologías Utilizadas

La aplicación se ha desarrollado íntegramente sobre el ecosistema nativo de Android, priorizando el rendimiento, la eficiencia energética y la compatibilidad con dispositivos de baja gama, tal como exige el contexto de las comunidades rurales.

## Kotlin y Android Studio

- **Lenguaje:** Kotlin.
- **IDE:** Android Studio.
- **Interfaz de usuario:** Jetpack Compose con Material Design 3, garantizando un diseño responsivo y adaptable a múltiples tamaños de pantalla.
- **Navegación:** Navigation Compose para la gestión del stack de pantallas.

## Librerías y servicios complementarios

### Persistencia y almacenamiento local
- **Room:** base de datos SQLite con soporte para entidades `Visitor`, `PendingSync`, `EntrepreneurProfile`, `InsightCache`. Permite consultas reactivas y almacenamiento offline.
- **DataStore (Preferences):** para guardar configuración de usuario (idioma, tipo de emprendimiento, preferencias de sincronización, token de autenticación).

### Conectividad y sincronización
- **Retrofit + OkHttp:** cliente HTTP para el consumo de APIs REST (sincronización de datos, descarga de tendencias turísticas).
- **WorkManager:** programación de tareas en segundo plano para la sincronización diferida. Configurado con restricciones de batería (`BatteryNotLow`) y conectividad (`NetworkType.CONNECTED`) para minimizar el consumo energético en zonas con acceso limitado a electricidad.

### Arquitectura y buenas prácticas
- **MVVM + Clean Architecture:** separación en capas `data` (Room, Retrofit, DataStore), `domain` (casos de uso) y `presentation` (ViewModels + Compose UI).
- **Hilt:** inyección de dependencias para desacoplar componentes y facilitar el mantenimiento y las pruebas.

### Calidad de datos y validación
- Lógica de validación integrada en los ViewModels: campos obligatorios, rangos numéricos predefinidos y listas de selección. Se asigna un puntaje de completitud a cada registro antes de marcarlo como sincronizable, garantizando que solo datos de calidad alimenten los reportes del MINCETUR.

### Síntesis de voz (multimodalidad)
- **Sherpa-ONNX:** motor de inferencia offline para síntesis de voz (TTS) que corre localmente en Android sin necesidad de servicios en la nube.
- **Modelo MMS de Meta (variante quechua):** checkpoint preentrenado para quechua, convertido a formato ONNX y empaquetado dentro de la aplicación. Esto permite generar audios personalizados a partir de los insights sin conexión a internet.
- **Reproducción de audio:** `MediaPlayer` de Android para reproducir los archivos de audio generados.

### Visualización de datos
- **Vico:** librería de gráficos para Compose, utilizada en el panel de emprendedor para representar visualmente la evolución de visitas y la distribución de procedencias.

### Internacionalización
- Recursos `strings.xml` en español y quechua (`values-es`, `values-qu`). Los pictogramas y las voces sintéticas están culturalmente adaptados al contexto andino.

*Todas las librerías de terceros se incluyen de forma local o mediante repositorios oficiales de Maven Central, asegurando la compatibilidad con los lineamientos de seguridad del MINCETUR.*