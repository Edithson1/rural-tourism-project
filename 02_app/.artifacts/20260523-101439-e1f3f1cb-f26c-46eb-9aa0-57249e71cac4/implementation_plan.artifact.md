# Plan de Implementación: Refuerzo Estético y Responsividad

Este plan describe las mejoras visuales para los modos claro/oscuro y la adaptación de la interfaz para pantallas horizontales (tablets).

## Cambios Propuestos

### 1. Sistema de Colores y Temas

#### [Color.kt](file:///C:/Users/RICARDO/Downloads/desarrollo%20sistemas%20web/Proyecto/02_app/app/src/main/java/upch/mluque/final_project/ui/theme/Color.kt)
- Redefinir el modo oscuro para que sea más cercano al negro pero con matices azules fríos.
- Ajustar colores de acento para mayor contraste.

```kotlin
// Nuevos Colores Oscuros (Más Negro, Matices Fríos)
val DeepBlackBackground = Color(0xFF07090D)
val DeepBlackSurface = Color(0xFF11141B)
val HighContrastBlue = Color(0xFF38BDF8) // Mantener para contraste
val ColdGray = Color(0xFF94A3B8)

// Ajuste Colores Claros (Más Cálidos)
val WarmSurfaceVariant = Color(0xFFF5EFE6) // Reemplaza F0F0F0
```

#### [Theme.kt](file:///C:/Users/RICARDO/Downloads/desarrollo%20sistemas%20web/Proyecto/02_app/app/src/main/java/upch/mluque/final_project/ui/theme/Theme.kt)
- Actualizar `DarkColorScheme` con los nuevos colores "Deep Black".
- Corregir `surfaceVariant` en `LightColorScheme` para eliminar tonos azulados en modo claro.

---

### 2. Responsividad (Layouts Adaptativos)

Implementar detección de ancho de pantalla para reorganizar elementos en horizontal/tablets.

#### [HomeScreen.kt](file:///C:/Users/RICARDO/Downloads/desarrollo%20sistemas%20web/Proyecto/02_app/app/src/main/java/upch/mluque/final_project/ui/screens/HomeScreen.kt)
- Si el ancho es > 600dp: Usar un `Row` principal.
    - Columna Izquierda: Bienvenida + Gráfico de Visitas.
    - Columna Derecha: Tip del día + Registros Recientes.

#### [ProfileScreen.kt](file:///C:/Users/RICARDO/Downloads/desarrollo%20sistemas%20web/Proyecto/02_app/app/src/main/java/upch/mluque/final_project/ui/screens/ProfileScreen.kt)
- Si el ancho es > 600dp:
    - Columna Izquierda: Tarjeta de perfil y estado de conexión.
    - Columna Derecha: Secciones de Configuración e Información.
- **Corrección Estética**: Reemplazar colores hardcodeados (`0xFF0F3D3E`) por `MaterialTheme.colorScheme`.

---

### 3. Ajustes de Contraste y Consistencia

#### [VisitsScreen.kt](file:///C:/Users/RICARDO/Downloads/desarrollo%20sistemas%20web/Proyecto/02_app/app/src/main/java/upch/mluque/final_project/ui/screens/VisitsScreen.kt) y [AddVisitScreen.kt](file:///C:/Users/RICARDO/Downloads/desarrollo%20sistemas%20web/Proyecto/02_app/app/src/main/java/upch/mluque/final_project/ui/screens/AddVisitScreen.kt)
- Asegurar que los componentes de selección y tarjetas resalten sobre el fondo.
- Eliminar cualquier rastro de azul en modo claro.

## Plan de Verificación

### Pruebas Visuales
1.  **Modo Oscuro**: Verificar que el fondo sea casi negro (`0xFF07090D`) y que los textos azules tengan alto contraste.
2.  **Modo Claro**: Verificar que no existan bordes o superficies azuladas (usar tonos crema/marrón).
3.  **Rotación de Pantalla**:
    - En un emulador de Tablet o girando el móvil: Confirmar que `HomeScreen` y `ProfileScreen` se dividen en dos columnas.
    - Verificar que el contenido no se vea excesivamente estirado horizontalmente.

### Herramientas
- Uso de `Preview` con diferentes configuraciones de pantalla (`widthDp = 800`).
- Inspección manual en dispositivo físico/emulador.
