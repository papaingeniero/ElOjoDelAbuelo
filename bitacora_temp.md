
### ✅ v3.9.5-dev.14: Fix Pantalla Negra + Modo Eco Funcional

**El Problema (v3.9.5-dev.13)**:
La Activity mostraba pantalla negra (solo botón APAGAR visible) después de aplicar el Modo Eco Térmico.

**Diagnóstico de Gemini (Correcto)**:
El driver de cámara del Galaxy S (Android 2.3) tiene un comportamiento peculiar:
*   Cuando hacemos `stopPreview()` + `setPreviewDisplay()`, el driver "olvida" el enlace entre la cámara y la pantalla.
*   Aunque el callback de software sigue funcionando (grabación, detección), el pipeline de video hacia el SurfaceView queda desconectado.
*   **Solución**: Hay que "re-enganchar" el callback con `setPreviewCallbackWithBuffer(null)` + `setPreviewCallbackWithBuffer(previewCallback)` para despertar la señal de video hacia la pantalla.

**El Fix**:
```java
// En setPreviewSurface(), después de cambiar superficie:
try {
    instance.camera.setPreviewCallbackWithBuffer(null); // Limpieza suave
    instance.camera.setPreviewCallbackWithBuffer(instance.previewCallback); // ¡CONEXIÓN!
} catch (Exception e) {
    Log.e(TAG, "Error re-hooking callback", e);
}
```

**Resultado**:
*   ✅ Imagen de cámara visible en Activity
*   ✅ Activity estable (no crashea)
*   ✅ Modo Eco activo (throttling + límite FPS)

**Colaboración Claude + Gemini**:
*   **Claude**: Implementó el Modo Eco original
*   **Gemini**: Diagnosticó correctamente el problema de pantalla negra y propuso el fix del re-enganche
