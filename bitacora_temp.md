
### ⚠️ v3.9.5-dev.19: UI Rescue (Intento Parcial) - Imagen Congelada

**Estado**: ⚠️ **PARCIALMENTE FALLIDO**

**El Experimento**:
Se intentó recuperar la visualización de la cámara en `MainActivity` forzando `MATCH_PARENT` en el `SurfaceView` y simplificando el ciclo de vida.

**Resultado Observado**:
- ✅ **Pantalla Negra Eliminada**: La imagen de la cámara YA SE VE en la pantalla.
- ❌ **Imagen Congelada**: La vista muestra el *primer fotograma* y se queda estática. No hay video fluido en la pantalla del teléfono.
- ℹ️ **Servicio**: El servicio sigue funcionando (web y detección), pero la UI local no refresca.

**Diagnóstico Preliminar**:
Al eliminar la gestión de zoom por software y tocar la inicialización del `SurfaceView`, es probable que el "pipe" de renderizado directo (`camera.setPreviewDisplay(holder)`) esté entrando en conflicto con el mecanismo de buffers (`setPreviewCallbackWithBuffer`) en este hardware específico. El primer frame llega, se pinta, y luego el flujo se detiene o se desvía exclusivamente al callback, ignorando la pantalla.

**Acción**: Se oficializa esta versión para tener un punto de control donde "la pantalla se enciende", aunque falte revivir el flujo de video.
