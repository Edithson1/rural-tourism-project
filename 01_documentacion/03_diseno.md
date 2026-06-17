# 3. Diseño de la Aplicación

## Mockups y prototipos

Se elaboraron prototipos utilizando **Figma**, cubriendo el flujo principal de la aplicación. A continuación se presentan las pantallas clave:

| Pantalla | Descripción |
|----------|-------------|
| Onboarding | Tres pasos con pictogramas que explican las funciones: “Registra visitas”, “Mira tus resultados” y “Recibe consejos”. Incluye selección de idioma (español/quechua). |
| Perfil del emprendedor | Permite elegir el tipo de emprendimiento (hospedaje, alimentación, artesanía o varios). Guarda la configuración local. |
| Registro de visita | Formulario adaptable al tipo de negocio. Campos obligatorios con asterisco, listas desplegables para nacionalidad y rango de gasto, y un selector de servicios consumidos. Contador de registros en la parte superior. |
| Panel de control | Muestra los insights del mes: un gráfico de barras con la evolución de visitas, un mapa pictográfico de procedencias y una tarjeta con la recomendación quincenal. Junto al resumen textual, un botón de audio para escuchar el insight en quechua o español. |
| Sincronización | Indicador minimalista (nube con check) que muestra si los datos están pendientes de sincronizar o ya fueron enviados. |

## Herramientas utilizadas

- **Figma:** diseño de interfaces, prototipado interactivo y definición de guías de estilo (colores, tipografía adaptada a legibilidad en pantallas pequeñas, iconografía cultural).
- **Material Design 3:** sistema de componentes y tokens de diseño para Android, que garantiza coherencia visual, accesibilidad y adaptación a distintos tamaños de pantalla.

## Flujo de pantallas

El flujo **implementado** en el prototipo sigue la siguiente secuencia:

1. **Inicio (Splash)** → **Onboarding** (solo la primera vez, 3 pasos con selección de idioma) → **Configuración de perfil**.
2. **Configuración de perfil:** nombre y tipo de emprendimiento (Hospedaje, Alimentación, Artesanía o Varios) → **catálogo inicial de productos** → guardado local.
3. **Pantalla principal** organizada en cuatro secciones con barra de navegación inferior (o lateral en horizontal):
   - **Home:** resumen, consejos del emprendedor y acceso al **Dashboard de insights**.
   - **Visitas:** historial de registros y **botón para nuevo registro** → formulario con selección de nacionalidad, productos, descuentos y total → guardado offline.
   - **Mapa:** mapa de procedencias de los turistas (OpenStreetMap offline).
   - **Perfil:** datos del negocio, idioma, moneda, ayuda, privacidad y sincronización.
4. **Sincronización entre dispositivos (P2P):** emparejamiento por **código QR** sobre la red local; el usuario ve el estado de la conexión. *(La sincronización automática en segundo plano con WorkManager es una mejora planificada — ver `04_tecnologia.md` §4.2.)*
5. **Insights:** panel con gráficos (visitantes, nacionalidades, servicios, horas pico, ingresos) y filtros por periodo.

> **Nota de estado.** Algunas capacidades del diseño objetivo aún están en desarrollo: API de recomendaciones, traducción offline del contenido dinámico, **audio con voz real** (hoy simulado) y notificaciones. El detalle del avance se documenta en `04_tecnologia.md` (§4.2) y `06_funcionalidad.md`.