
### 📦 v3.9.5-dev.15: Sistema de "Caja Negra" (Blackbox Logging)

**El Problema**:
La Activity muere inesperadamente tras horas de funcionamiento. Al ser una ROM CyanogenMod antigua (Android 4.0/2.3), el acceso a logcat es inestable o inexistente tras el crash. Nos encontrábamos "ciegos" para diagnosticar.

**La Solución (Ingeniería)**:

**Sistema de Logs Híbrido** implementado en `SentinelService`:
*   **RAM (Buffer Circular)**: Mantiene las últimas 50 líneas para visualización rápida en el servidor web (`/log`).
*   **Disco (Persistencia)**: Escribe asíncronamente en `/sdcard/ElOjoDelAbuelo/abuelolog.log` usando un hilo dedicado para no bloquear el callback de cámara.

**Sondas de Diagnóstico Inyectadas**:
*   `onCreate` → Arranque del sistema
*   `startCamera` → Estado del hardware  
*   `setPreviewSurface` → Conexión/Desconexión de pantalla
*   `previewCallback` → Detección de movimiento y grabación

**Valor Aportado**:
*   **Visibilidad Total**: Podemos ver exactamente qué operación causó la muerte sin depender de Android Studio.
*   **Depuración en Producción**: Diagnosticar fallos en el dispositivo desplegado solo conectando USB: `adb shell tail -f /sdcard/ElOjoDelAbuelo/abuelolog.log`
*   **Sin Impacto en Rendimiento**: Escritura asíncrona no bloquea el hilo principal.
