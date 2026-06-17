# 6. Funcionalidad y Estado de Implementación

Este documento resume los módulos **ya implementados** en el prototipo y las funcionalidades **pendientes**, sirviendo como referencia rápida del avance real del proyecto.

> **Leyenda:** ✅ Implementado · 🟡 Parcial / simulado · ⏳ Pendiente

---

## 6.1 Módulos implementados

### Arranque y onboarding ✅
- **Splash** (`SplashScreen`) que espera la carga de la configuración local.
- **Onboarding** de 3 pasos con pictogramas y **selección de idioma** (español, quechua, inglés, portugués).
- **Configuración de perfil** (`ProfileSetupScreen`): nombre y tipo de emprendimiento (Hospedaje, Alimentación, Artesanía o Varios), con precarga automática de productos según la categoría.
- **Catálogo inicial de productos** (`ProductCatalogScreen` en modo setup) como último paso del alta.

### Registro de visitas ✅
- **Alta de visita** (`AddVisitScreen`): selección de **nacionalidad con bandera**, selección de **productos/servicios consumidos** con cantidades, aplicación de **descuentos** (fijo o porcentual) y cálculo automático de subtotal y total.
- **Historial de visitas** (`VisitsScreen`) y **detalle de visita** (`VisitDetailScreen`).
- Persistencia **offline** inmediata en Room.

### Catálogo de productos ✅
- Gestión completa (`ProductCatalogScreen`, `ProductEditorScreen`): crear, editar y eliminar productos con nombre, precio base y categoría.

### Panel de control / Dashboard ✅
- **Gráficos profesionales con Vico** (encapsulados en `ui/components/VicoCharts.kt`), usados tanto en Home como en el Dashboard.
- **Dashboard en pestañas temáticas** (`DashboardScreen` / `DashboardViewModel`):
  - **Resumen:** KPIs de total de visitantes, país líder, servicio estrella, **ticket promedio** e ingresos estimados.
  - **Visitantes:** ranking de nacionalidades y **visitas por día de la semana** (gráficos de barras Vico).
  - **Ventas:** **ingresos en el tiempo** (gráfico de líneas Vico, con granularidad día/semana/mes según el filtro), ticket promedio y distribución de servicios.
  - **Tiempos:** horas de mayor afluencia.
- **Filtro por periodo** compartido entre pestañas: todos, últimos 7 días, mes actual, año actual.
- Resumen textual generado por idioma (`getInsightsSummary`).

### Mapa de procedencias ✅
- **Mapa OpenStreetMap offline** (`MapScreen`, `FullscreenMapScreen`, `OsmMapView`) con marcadores por país construidos desde un GeoJSON local, y modo de visualización por puntos.

### Sincronización entre dispositivos ✅
- **Sync P2P local** por sockets TCP, descubrimiento por NSD/mDNS y **emparejamiento por QR** (`ShowQrScreen`, `ScanQrScreen`, `LinkedDevicesScreen`, `SyncStatusScreen`).

### Perfil y configuración ✅
- **Perfil** (`ProfileScreen`, `EditProfileScreen`) con foto, datos del negocio y accesos a ajustes.
- **Selección de idioma** (`LanguageSelectionScreen`), **selección de moneda y tipos de cambio** (`CurrencySelectionScreen`, S//USD/EUR).
- **Ayuda** (`HelpScreen`) y **política de privacidad** (`PrivacyPolicyScreen`).

### Multimodalidad e idiomas 🟡
- ✅ Interfaz traducida a **4 idiomas** mediante `strings.xml` (`UiTranslations`).
- 🟡 **Reproductor de audio simulado** (`AudioPlayerUI`): barra de progreso y subtítulos por tiempo, **sin síntesis de voz real** todavía.

---

## 6.2 Funcionalidades pendientes

| # | Funcionalidad | Estado | Descripción |
|---|---------------|--------|-------------|
| 1 | **API de recomendaciones** | ⏳ | Integrar un servicio remoto (Retrofit + OkHttp) que genere recomendaciones personalizadas por tipo de negocio, con caché offline. Hoy los consejos son textos locales por defecto. |
| 2 | **Traducción offline de contenido dinámico** | ⏳ | Modelos de traducción automática offline para traducir también los **textos nuevos que introduce el usuario** (productos, consejos, resúmenes), no solo la UI estática. |
| 3 | **Texto a voz (TTS) multilingüe** | 🟡→⏳ | Reemplazar el audio simulado por síntesis de voz real offline (Sherpa-ONNX + modelos MMS de Meta, incl. quechua) reproducida con MediaPlayer, en los distintos idiomas. |
| 4 | **Sistema de notificaciones** | ⏳ | Alertar de **modificaciones nuevas** recibidas por sincronización y del **establecimiento de conexión** con la API o con un dispositivo par. El permiso `POST_NOTIFICATIONS` ya está declarado. |
| 5 | **Mejoras de interfaz (UI)** | ⏳ | Corregir y pulir varias secciones de la interfaz: consistencia visual, comportamiento responsive y refinamiento de flujos. |

---

## 6.3 Resumen del estado

El prototipo cubre de forma sólida el **núcleo offline-first**: registro de visitas, catálogo, dashboard de insights, mapa de procedencias y sincronización P2P entre dispositivos, en cuatro idiomas. Las piezas pendientes se concentran en los **servicios inteligentes y de accesibilidad** (recomendaciones vía API, traducción y voz offline, notificaciones) y en el **pulido de la interfaz**, que constituyen la siguiente fase de desarrollo.
