
### 🔥 v3.9.5-dev.11: Fix Crítico - Race Condition en HAL de Cámara

**El Problema**:
La Activity crasheaba esporádicamente después de la instalación inicial, y la temperatura del dispositivo se mantenía alta (42°C) incluso en reposo.

**Diagnóstico Colaborativo (Claude + Gemini)**:
1.  **Claude** investigó comparando v3.9.2 vs v3.9.5 y no encontró diferencias funcionales obvias.
2.  **Gemini** (autor original de v3.9.2) identificó que las líneas de reset del callback en `setPreviewSurface` causaban una **race condition** en el driver de cámara del Galaxy S (Android 2.3).
3.  La documentación de arquitectura (`HARDWARE_PREVIEW_WALKTHROUGH.md`) confirmaba que lo correcto era NO tocar el callback durante cambios de superficie.

**El Parche**:
Eliminadas estas dos líneas tóxicas:
```java
instance.camera.setPreviewCallbackWithBuffer(null);
instance.camera.setPreviewCallbackWithBuffer(instance.previewCallback);
```
Manteniendo solo la regeneración de buffers que es vital para la detección de movimiento.

**Resultados Medidos**:
*   ✅ Activity estable (no más crashes)
*   ✅ Temperatura: 42°C → 39-40°C (-3°C)
*   ✅ `mediaserver` ya no estresa el HAL innecesariamente

**Lecciones Aprendidas**:
*   En hardware legacy, menos es más: tocar el callback durante un cambio de superficie era redundante y dañino.
*   La colaboración entre agentes (Claude investigando, Gemini diagnosticando) resultó efectiva.
*   La documentación de arquitectura (`HARDWARE_PREVIEW_WALKTHROUGH.md`) es la fuente de verdad.
