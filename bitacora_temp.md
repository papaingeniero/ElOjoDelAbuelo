
---

## 🧊 v3.9.5-dev.18: Proyecto "Ice Age" - Estabilización Térmica Total

### 📜 El Problema (Storytelling)
El Abuelo sufría de dos males graves:
1. **Fiebre Crónica (42-44°C)**: El zoom por software escalaba bitmaps en CPU, saturando el procesador single-core.
2. **El Fantasma Post-Grabación**: Al cerrar un vídeo, el detector comparaba frames desfasados y disparaba grabaciones falsas.

### 🛠️ La Solución (Ingeniería Profunda)

#### A. "La Lupa Fría" - Zoom por Hardware 🔍❄️
```
ANTES: MainActivity.applyZoomLogic() → Bitmap.createScaledBitmap() → 🔥 CPU al 100%
AHORA: Camera.Parameters.setZoom(index) → 🧊 Hardware lo hace gratis
```
- Se lee `defaultZoom` de SharedPreferences
- Se busca el "escalón" hardware más cercano en `zoomRatios`
- Se aplica con `params.setZoom(bestIndex)`

#### B. "El Pintor Vago" - Deep Sleep 0.5 FPS 💤
```java
// En processFrame():
if (!isRecording && uiPreviewCallback == null) {
    if (System.currentTimeMillis() % 1000 > 200) return; // ~1 frame/seg
}
```
El servicio detecta: "Pantalla apagada + Sin grabar = A dormir".

#### C. "El Interruptor de Luz" - Sincronización Activity↔Service 💡
```
onResume() → setUiCallback(callback)  → "Estoy despierto" 🟢
onPause()  → setUiCallback(null)      → "Me duermo" 🔴
```

#### D. "El Caza-Fantasmas" - Reset del Detector 👻🚫
```java
// En closeRecordingFile():
motionDetector = new MotionDetector(); // Borra la memoria
```
El primer frame post-grabación es la nueva referencia. No hay "salto temporal".

### 🎓 Lecciones Aprendidas
1. **Hardware > Software**: En dispositivos legacy, delegar al hardware es SIEMPRE mejor.
2. **El Garbage Collector es tu enemigo**: Evitar crear objetos en bucles de frames.
3. **Los retardos de I/O causan bugs fantasma**: El disco SD es lento; hay que resetear estado tras escribir.
4. **Sincronización explícita mata bugs**: `null` es mejor que "adivinar" si la pantalla está encendida.

### 📖 Glosario
- **NV21**: Formato de imagen nativo de cámaras Android (YUV 4:2:0).
- **zoomRatios**: Lista de escalones de zoom soportados por hardware (ej: 100, 125, 150... = 1x, 1.25x, 1.5x...).
- **Ghost Trigger**: Detección falsa de movimiento causada por comparar frames temporalmente distantes.

### ✅ Estado Final Esperado
| Escenario | Temperatura | FPS Procesado |
|-----------|-------------|---------------|
| Pantalla ON | ~38°C | 5-15 FPS |
| Pantalla OFF (idle) | ~30-35°C | 0.5 FPS |
| Grabando | ~40°C | 15 FPS |
