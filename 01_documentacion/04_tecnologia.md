# 4. Tecnologías Utilizadas

La aplicación se construirá íntegramente sobre el ecosistema nativo de Android, priorizando el rendimiento, la eficiencia energética y la compatibilidad con dispositivos de baja gama, tal como exige el contexto de las comunidades rurales. A continuación se describe la pila tecnológica seleccionada para el desarrollo del prototipo.

## Kotlin y Android Studio

- **Lenguaje:** Kotlin, por su concisión, seguridad frente a nulos y adopción oficial en el ecosistema Android.
- **IDE:** Android Studio, entorno de desarrollo integrado recomendado por Google.
- **Interfaz de usuario:** Jetpack Compose con Material Design 3, que permitirá construir una interfaz declarativa, reactiva y adaptable a múltiples tamaños de pantalla.
- **Navegación:** Navigation Compose, que facilitará la gestión del stack de pantallas y el paso de argumentos entre destinos.

## Librerías y servicios complementarios seleccionados

### Persistencia y almacenamiento local
- **Room:** base de datos SQLite con soporte para entidades como `Visitor`, `PendingSync`, `EntrepreneurProfile` e `InsightCache`. Permitirá consultas reactivas (Flow) y almacenamiento offline de todos los registros.
- **DataStore (Preferences):** alternativa moderna a SharedPreferences, que se usará para guardar la configuración del usuario (idioma, tipo de emprendimiento, preferencias de sincronización, token de autenticación).

### Conectividad y sincronización
- **Retrofit + OkHttp:** cliente HTTP que se empleará para consumir las APIs REST de sincronización de datos y descarga de tendencias turísticas.
- **WorkManager:** biblioteca de Android Jetpack que programará tareas de sincronización en segundo plano de forma diferida y confiable. Se configurará con restricciones de batería (`BatteryNotLow`) y conectividad (`NetworkType.CONNECTED`) para minimizar el consumo energético en zonas con acceso limitado a electricidad.

### Arquitectura y buenas prácticas
- **MVVM + Clean Architecture:** el código se organizará en tres capas:
  - `data`: Room, Retrofit y DataStore.
  - `domain`: casos de uso y lógica de negocio.
  - `presentation`: ViewModels y vistas en Jetpack Compose.
- **Hilt:** inyección de dependencias que desacoplará los componentes, facilitando el mantenimiento, la escalabilidad y las pruebas unitarias.

### Calidad de datos y validación
- Se implementará lógica de validación en los ViewModels: campos obligatorios, rangos numéricos predefinidos y listas de selección. Cada registro recibirá un puntaje de completitud antes de ser marcado como sincronizable, garantizando que solo datos de calidad alimenten los futuros reportes del MINCETUR.

### Síntesis de voz (multimodalidad)
- **Sherpa-ONNX:** motor de inferencia offline para síntesis de voz (TTS) que se ejecutará localmente en Android sin depender de servicios en la nube.
- **Modelo MMS de Meta (variante quechua):** checkpoint preentrenado para quechua, convertido a formato ONNX, que se empaquetará dentro de la aplicación. Esto permitirá generar audios personalizados a partir de los insights sin necesidad de conexión a internet.
- **MediaPlayer:** componente de Android utilizado para reproducir los archivos de audio generados.

### Visualización de datos
- **Vico:** librería de gráficos para Compose que se usará en el panel del emprendedor para representar visualmente la evolución de visitas y la distribución de procedencias de los turistas.

### Internacionalización
- Se emplearán recursos `strings.xml` para español y quechua (`values-es`, `values-qu`). Los pictogramas y las voces sintéticas estarán culturalmente adaptados al contexto andino.

*Todas las librerías de terceros se incluirán de forma local o mediante repositorios oficiales de Maven Central, garantizando la compatibilidad con los lineamientos de seguridad del MINCETUR.*