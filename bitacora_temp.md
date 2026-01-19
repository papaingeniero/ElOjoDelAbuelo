
### 🧮 v3.9.3-dev.3: El Algoritmo de "Ratio Dinámico"

El usuario reportó que, pese a la corrección geométrica, el desplazamiento seguía viéndose mal en Chrome (Escritorio).
**La Causa**:
Usábamos un factor fijo (`0.25`) asumiendo que el User veía el video en una pantalla de ~320px.
En escritorio (1000px+), el usuario hace un Pan de 100px (10% de pantalla), pero la miniatura recibía 25px (30% de su ancho). Resultado: la miniatura "corría" más rápido que el video.

**La Solución (Dynamic Ratio)**:
Ahora calculamos el factor de escala en tiempo real en Javascript:
```javascript
var mainWidth = liveStream.offsetWidth || document.body.clientWidth;
var ratio = 80.0 / mainWidth;
var tx = x * ratio;
```
Si la pantalla es gigante, el ratio baja (ej: 0.08). Si es pequeña, sube (ej: 0.25).
La física se auto-ajusta al dispositivo del observador.
