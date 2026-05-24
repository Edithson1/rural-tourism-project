# Plan de Implementación: Detección Activa de Conexión (Heartbeat Rápido)

Este plan describe la implementación de un sistema de "latido" (Heartbeat) optimizado para detectar desconexiones en tiempo real en aproximadamente 5 segundos.

## Cambios Propuestos

### Lógica de Sincronización

#### [SyncViewModel.kt](file:///C:/Users/RICARDO/Downloads/desarrollo%20sistemas%20web/Proyecto/02_app/app/src/main/java/upch/mluque/final_project/sync/SyncViewModel.kt)

- **Bucle de Heartbeat**:
    - Enviar un `Ping` cada **2 segundos** (en lugar de 5) mientras el socket esté abierto.
    - Mantener un registro `lastResponseTimestamp`.
- **Detección de Timeout**:
    - Un proceso de verificación cada 2 segundos comprobará si `currentTime - lastResponseTimestamp > 5000ms`.
    - Si se supera el umbral, se forzará `_isConnected.value = false` y se cerrará el socket.
- **Respuesta Instantánea**:
    - Actualizar `lastResponseTimestamp` con CUALQUIER mensaje recibido (no solo Pongs), ya que cualquier actividad confirma que el otro lado está vivo.

---

### Interfaz de Usuario

#### [ProfileScreen.kt](file:///C:/Users/RICARDO/Downloads/desarrollo%20sistemas%20web/Proyecto/02_app/app/src/main/java/upch/mluque/final_project/ui/screens/ProfileScreen.kt)

- Agregar un indicador de estado circular en la esquina superior derecha de la pantalla.
- Colores: Verde (Conectado), Rojo/Gris (Desconectado).
- Solo visible si el dispositivo está vinculado.

#### [LinkedDevicesScreen.kt](file:///C:/Users/RICARDO/Downloads/desarrollo%20sistemas%20web/Proyecto/02_app/app/src/main/java/upch/mluque/final_project/ui/screens/LinkedDevicesScreen.kt)

- La UI ya está conectada al estado `isConnected`, por lo que se actualizará automáticamente con la nueva precisión del Heartbeat.

---

## Plan de Verificación

### Pruebas Manuales
1.  **Detección Ultra-Rápida:**
    - Conectar dispositivos.
    - Poner modo avión en uno.
    - El otro dispositivo debe marcar "Desconectado" en máximo 6 segundos.
2.  **Indicador de Perfil:**
    - Verificar que el punto verde en el Servidor sea estable mientras el Cliente navega por la app.
3.  **Estabilidad de Red:**
    - Verificar que el envío constante de Pings (cada 2s) no genere lag en la sincronización de visitas.
