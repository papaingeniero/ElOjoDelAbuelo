
### ❌ v3.9.5-dev.13: Intento Fallido - Modo Eco Térmico (Pantalla Negra)

**Objetivo**: Reducir temperatura de 42-43°C a 38-39°C mediante throttling agresivo de frames.

**Cambios Aplicados**:
1.  **Variable `frameSkipCounter`**: Contador para throttling dinámico.
2.  **`setupCameraParameters()`**: Límite de 20 FPS por hardware.
3.  **`previewCallback` con Modo Eco**: 
    *   En idle: procesa 1 de cada 5 frames (~4 FPS efectivos)
    *   Grabando: procesa 1 de cada 2 frames (~10 FPS efectivos)

**Resultado**:
⚠️ **FALLO PARCIAL**: La Activity se mantiene abierta (no crashea), pero la imagen de la cámara NO se muestra en pantalla. Solo es visible el botón rojo de APAGAR.

**Hipótesis del Fallo**:
*   El throttling tan agresivo (1 de cada 5) puede estar impidiendo que lleguen suficientes frames al `SurfaceView` para que el hardware de video lo interprete.
*   La combinación de `addCallbackBuffer` con salto de frames puede desincronizar el buffer pool.

**Próximos Pasos**:
*   Investigar si el problema es el ratio de skip (probar 2:1 en lugar de 5:1)
*   O si el problema es el orden de operaciones en el callback
