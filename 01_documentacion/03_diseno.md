# 3. Diseño de la Aplicación

## Mockups y prototipos

Se elaboraron prototipos de alta fidelidad utilizando **Figma**, cubriendo el flujo principal de la aplicación. A continuación se presentan las pantallas clave:

| Pantalla | Descripción |
|----------|-------------|
| Onboarding | Tres pasos con pictogramas que explican las funciones: “Registra visitas”, “Mira tus resultados” y “Recibe consejos”. Incluye selección de idioma (español/quechua). |
| Perfil del emprendedor | Permite elegir el tipo de emprendimiento (hospedaje, alimentación, artesanía o varios). Guarda la configuración local. |
| Registro de visita | Formulario adaptable al tipo de negocio. Campos obligatorios con asterisco, listas desplegables para nacionalidad y rango de gasto, y un selector de servicios consumidos. Contador de registros en la parte superior. |
| Panel de control | Muestra los insights del mes: un gráfico de barras con la evolución de visitas, un mapa pictográfico de procedencias y una tarjeta con la recomendación quincenal. Junto al resumen textual, un botón de audio para escuchar el insight en quechua o español. |
| Sincronización | Indicador minimalista (nube con check) que muestra si los datos están pendientes de sincronizar o ya fueron enviados. |

*Nota: Insertar aquí las capturas de los mockups (ej. [MOCKUP 1], [MOCKUP 2]).*

## Herramientas utilizadas

- **Figma:** diseño de interfaces, prototipado interactivo y definición de guías de estilo (colores, tipografía adaptada a legibilidad en pantallas pequeñas, iconografía cultural).
- **Material Design 3:** sistema de componentes y tokens de diseño para Android, que garantiza coherencia visual, accesibilidad y adaptación a distintos tamaños de pantalla.

## Flujo de pantallas

El flujo principal de la aplicación sigue la siguiente secuencia:

1. **Inicio (Splash)** → **Onboarding** (solo la primera vez) → **Pantalla de idioma**.
2. **Selección de comunidad y tipo de emprendimiento** → guardado local.
3. **Pantalla principal (Dashboard)** con acceso rápido a:
   - **Nuevo registro** (botón flotante) → formulario con validaciones → guardado offline.
   - **Historial de registros** (lista con posibilidad de editar/eliminar).
   - **Panel de insights** (gráficos y recomendaciones).
4. **Sincronización en segundo plano** (sin pantalla dedicada): WorkManager ejecuta la subida de datos cuando se cumplen las condiciones (Wi‑Fi + batería suficiente). El usuario solo ve un ícono de estado.
5. **Reportes** (vista simple para el emprendedor) y opción de exportar resumen (versión futura).