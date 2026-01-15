# Arquitectura "Zero CPU": Manual Técnico

**Versión:** v3.3.0
**Dispositivo Objetivo:** Samsung Galaxy S (i9000) - Android 2.3 Gingerbread

## 1. El Problema (Antes)
La arquitectura anterior utilizaba un `Camera.PreviewCallback` para recibir datos `NV21` en bruto, los convertía a JPEGs, luego a Bitmaps y finalmente los pintaba en un `ImageView` en la UI.
*   **Coste CPU:** Enorme (Decodificación JPEG + GC + UI Thread).
*   **Consecuencia:** Sobrecalentamiento, lag y frames perdidos.

## 2. La Solución (Hardware Preview)
Hemos cambiado a una arquitectura híbrida donde la cámara tiene **dos salidas simultáneas**:
1.  **Salida Hardware (display):** Conectada directamente al `SurfaceView` de la `MainActivity`. El chip de video (GPU/VPU) se encarga de pintar los píxeles. Coste CPU: **Casi Cero**.
2.  **Salida Software (callback):** Mantenemos el flujo de datos `NV21` para el detector de movimiento y el servidor web.

## 3. Implementación Crítica

### A. Activity (`MainActivity.java`)
La Activity ahora es "tonta". Solo ofrece una ventana (`SurfaceHolder`) al servicio.
```java
// CRÍTICO para Android 2.3
holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

// Conexión
public void surfaceCreated(SurfaceHolder holder) {
    SentinelService.setPreviewSurface(holder);
}
```

### B. Servicio (`SentinelService.java`)
El servicio gestiona el "Hot-Swap" (Cambio en caliente).
**Desafío**: En Android 2.3, llamar a `stopPreview()` borra los buffers de callback.
**Solución**: El ciclo "Stop-Switch-Refill-Start".

```java
public static void setPreviewSurface(SurfaceHolder holder) {
    // 1. FRENAR (Idle Driver)
    camera.stopPreview();

    // 2. CAMBIAR SALIDA
    camera.setPreviewDisplay(holder);

    // 3. RECUPERACIÓN (Hybrid Fix)
    // Regenerar buffers perdidos para que el software no muera
    for (int i = 0; i < 3; i++) {
        camera.addCallbackBuffer(new byte[bufferSize]);
    }

    // 4. ARRANCAR
    camera.startPreview();
}
```

## 4. Resultados
*   **Visualización:** 30fps fluidos en pantalla (Hardware).
*   **Web/Detección:** 15fps procesados en segundo plano (Software).
*   **Temperatura:** Reducción drástica.
