# 4. Tecnologías Utilizadas

La aplicación se construye íntegramente sobre el ecosistema nativo de Android, priorizando el rendimiento, la eficiencia energética y la compatibilidad con dispositivos de baja gama, tal como exige el contexto de las comunidades rurales. A continuación se describe la pila tecnológica **efectivamente implementada** en el prototipo y, al final, las **tecnologías planificadas** que aún se encuentran en desarrollo.

> **Leyenda de estado:** ✅ Implementado · 🟡 Parcial / simulado · ⏳ Pendiente / en desarrollo

---

## 4.1 Stack implementado

### Lenguaje, IDE e interfaz
- ✅ **Kotlin** como lenguaje único, por su concisión, seguridad frente a nulos y adopción oficial en Android.
- ✅ **Android Studio** como entorno de desarrollo.
- ✅ **Jetpack Compose + Material Design 3** para una interfaz declarativa, reactiva y adaptable. La app soporta layouts en **vertical y horizontal** (uso de `NavigationRail` en landscape y `BottomNavigationBar` en portrait).
- ✅ **Navigation Compose** para la gestión del stack de pantallas y el paso de argumentos entre destinos, complementado con un `HorizontalPager` para las cuatro secciones principales (Home, Visitas, Mapa, Perfil).
- ✅ **Compilación:** `minSdk = 24`, `targetSdk = 36`. Plugins KSP y Kotlin Serialization.

### Persistencia y almacenamiento local
- ✅ **Room** (base de datos SQLite, versión de esquema 9) con tres entidades:
  - `AppSettings`: configuración del emprendedor (idioma, nombre y categoría del negocio, foto de perfil, moneda preferida, tipos de cambio USD/EUR, velocidad de voz, textos de consejos y resumen del mapa por idioma, identificadores de dispositivo).
  - `Visit`: registro de cada visita (nacionalidad + bandera, productos consumidos, subtotal, descuento, total, fecha, estado de envío/sincronización).
  - `Product`: catálogo de productos/servicios del negocio (nombre, precio base, categoría).
  - Consultas reactivas mediante `Flow` y un `DataRepository` que centraliza el acceso a los DAOs.
- ✅ **TypeConverters + kotlinx.serialization** para almacenar estructuras complejas (lista de productos seleccionados, mapas de textos por idioma) dentro de Room.

> Nota: la configuración del usuario se almacena como una fila de Room (`AppSettings`, id = 1) en lugar de DataStore. Ver §4.3.

### Sincronización entre dispositivos (offline, P2P)
La sincronización implementada **no depende de un servidor en la nube**; opera dispositivo a dispositivo dentro de la misma red local, lo que es coherente con el enfoque *offline-first*:
- ✅ **Sockets TCP** (`ServerSocket`/`Socket`) en `SyncManager`, que intercambian mensajes `SyncMessage` serializados en JSON línea por línea.
- ✅ **Network Service Discovery (NSD / mDNS)** en `NsdHelper` para que los dispositivos se descubran automáticamente en la red.
- ✅ **Emparejamiento por código QR**: un dispositivo actúa como servidor y muestra un QR (`ShowQrScreen`); el otro lo escanea (`ScanQrScreen`) para conectarse. Pantallas de gestión: `LinkedDevicesScreen` y `SyncStatusScreen`.
- ✅ **ML Kit Barcode Scanning** + **Play Services Code Scanner** + **ZXing** para la generación y lectura de los códigos QR.
- ✅ **NetworkMonitor** para observar el estado de conectividad del dispositivo.

### Mapa y visualización geográfica
- ✅ **osmdroid (OpenStreetMap)** para el mapa de procedencias de los turistas, renderizado offline a partir de un GeoJSON local (`assets/countries5.json`). Se calculan centroides por país para ubicar los marcadores. Pantallas `MapScreen` y `FullscreenMapScreen`.
- ✅ **Coil** para la carga eficiente de imágenes (p. ej. foto de perfil).

### Visualización de datos (Home y Dashboard)
- ✅ **Vico** (librería de gráficos para Compose) para los gráficos de Home y del Dashboard, dándoles un acabado profesional (ejes, escalado automático). Todo el uso de Vico está encapsulado en `ui/components/VicoCharts.kt` (`VicoColumnChart`, `VicoLineChart`), de modo que el resto de la app es agnóstica a la versión de la librería.
- ✅ **Dashboard organizado en pestañas temáticas** (`DashboardScreen` / `DashboardViewModel`): **Resumen** (KPIs: total de visitantes, país líder, servicio estrella, ticket promedio e ingresos), **Visitantes** (ranking de nacionalidades y visitas por día de la semana), **Ventas** (ingresos en el tiempo, ticket promedio y distribución de servicios) y **Tiempos** (horas de mayor afluencia). Un filtro de periodo (todos, últimos 7 días, mes, año) se comparte entre las pestañas.

### Arquitectura y buenas prácticas
- ✅ **MVVM** con `ViewModel`s de Android (`MainViewModel`, `DashboardViewModel`, `SyncViewModel`) y estado reactivo mediante `StateFlow`.
- ✅ Organización en paquetes: `data` (Room + repositorio + modelos), `sync` (P2P), `ui` (features, components, navigation, theme), `utils`.
- ✅ **Validación en los ViewModels / pantallas**: campos obligatorios y cálculo de totales con descuentos antes de guardar una visita.

### Internacionalización
- ✅ Recursos `strings.xml` para **cuatro idiomas**: español (`values/`, por defecto), inglés (`values-en/`), portugués (`values-pt/`) y quechua (`values-qu/`).
- ✅ Utilidad `UiTranslations` para resolver textos según el idioma seleccionado en runtime.

---

## 4.2 Funcionalidades en desarrollo (pendientes)

Las siguientes capacidades forman parte del diseño objetivo del producto pero **aún no están implementadas** o se encuentran simuladas. Constituyen el roadmap técnico inmediato.

### ⏳ Integración de la API de recomendaciones
- Servicio remoto que, combinando los datos locales del emprendedor con tendencias turísticas globales, devuelva recomendaciones personalizadas según el tipo de negocio (hospedaje, alimentación, artesanía).
- Requiere incorporar un cliente HTTP (**Retrofit + OkHttp**) y un contrato de API, además de una capa de caché local para mantener el comportamiento offline-first cuando no haya conexión.
- Hoy los consejos del emprendedor (`entrepreneurTips`) son textos por defecto almacenados localmente, no provenientes de la API.

### ⏳ Traducción offline de contenido dinámico
- Modelos de **traducción automática offline** para traducir también los textos **nuevos introducidos por el usuario** (p. ej. nombres de productos, consejos, resúmenes) a los cuatro idiomas soportados.
- Actualmente solo se traduce la interfaz estática (`strings.xml`); el contenido dinámico se muestra en el idioma en que fue ingresado o con plantillas predefinidas por idioma.

### 🟡 Síntesis de voz (Texto a Voz) multilingüe
- **Estado actual:** la reproducción de audio está **simulada**. El componente `AudioPlayerUI` muestra una barra de progreso y subtítulos sincronizados por tiempo, pero **no genera ni reproduce voz real** (el código lo marca explícitamente como "simulación de audio").
- **Pendiente:** integrar un motor de **TTS offline** (p. ej. **Sherpa-ONNX** con modelos **MMS de Meta**, incluida la variante en quechua) más **MediaPlayer** para reproducir los audios de los insights en los distintos idiomas, superando las barreras de alfabetización.

### ⏳ Sistema de notificaciones
- Notificaciones para alertar al usuario de:
  - **Modificaciones nuevas** recibidas (datos sincronizados desde otro dispositivo).
  - **Establecimiento de conexión con la API** / con un dispositivo par.
- El permiso `POST_NOTIFICATIONS` ya está declarado en el manifiesto, pero la lógica de emisión de notificaciones aún no se implementa. Se evaluará el uso de **WorkManager** para tareas en segundo plano asociadas.

### ⏳ Ajustes de interfaz de usuario
- Pendiente corregir y pulir varias secciones de la UI (consistencia visual, comportamiento en distintos tamaños de pantalla, refinamiento de flujos). Trabajo continuo de QA visual.

---

## 4.3 Diferencias respecto al diseño inicial

Para mantener la trazabilidad, se documentan las decisiones donde la implementación difiere de la propuesta tecnológica original:

| Tecnología propuesta | Estado real | Observación |
|----------------------|-------------|-------------|
| Retrofit + OkHttp | ⏳ Pendiente | Se incorporará junto con la API de recomendaciones. |
| WorkManager (sync en segundo plano) | ⏳ Pendiente | La sync actual es P2P en primer plano por sockets TCP. |
| DataStore (Preferences) | 🔁 Sustituido | La configuración se persiste en Room (`AppSettings`). |
| Hilt (inyección de dependencias) | 🔁 No usado | Instanciación manual de repositorio/DB en los ViewModels. |
| Vico (gráficos) | ✅ Implementado | Integrado en Home y Dashboard; uso encapsulado en `VicoCharts.kt`. |
| Clean Architecture (capa `domain`) | 🔁 Simplificado | Arquitectura de dos capas (`data` + `ui`); la lógica vive en los ViewModels. |
| Sherpa-ONNX + MMS (TTS) | 🟡 Simulado | Reproductor de audio sin síntesis de voz real (ver §4.2). |
| Idiomas es + qu | ➕ Ampliado | Se soportan 4 idiomas: es, en, pt, qu. |
| osmdroid / mapa | ➕ Añadido | No estaba en la propuesta original y ya está implementado. |
| Catálogo de productos, descuentos y multimoneda | ➕ Añadido | Funcionalidad de negocio no contemplada inicialmente, ya implementada. |

*Todas las librerías de terceros se incluyen mediante repositorios oficiales de Maven Central, garantizando la compatibilidad con los lineamientos de seguridad del MINCETUR.*
