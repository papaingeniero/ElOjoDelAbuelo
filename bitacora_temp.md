## 🚀 Phase Release: v3.9.11 "El Pulso Firme" | Fecha: 13 de Febrero de 2026

### 📜 1. El Problema (Stability Check)
Tras el gran lanzamiento de "El Centinela Inteligente" (v3.9.10), detectamos dos áreas de fricción que empañaban la experiencia de uso diario:
1.  **Aritmia del Stream**: El video en vivo sufría muertes súbitas a los 15 segundos debido a una validación draconiana de URLs en el servidor.
2.  **Ceguera del Parásito**: La miniatura de "Grabación en Curso" aparecía vacía, impidiendo verificar visualmente qué estaba capturando la cámara.

### 🛠️ 2. La Solución (Madurez Técnica)
Esta versión de estabilización aplica cirugía correctiva en el corazón del servidor `NanoHttpServer`:
- **Routing Tolerante**: Implementamos lógica `startsWith` para aceptar latidos con parámetros, vital para la salud a largo plazo de las conexiones.
- **Higiene de Memoria**: Blindamos el mapa de sesiones contra ataques de agotamiento y aseguramos la limpieza de variables globales en el cliente JS.

### 🎓 3. Lecciones Aprendidas
- **La Robustez está en los Detalles**: Un simple `.equals()` puede tirar abajo un sistema de vigilancia perfecto. La programación defensiva en interfaces de red no es opcional.
- **Ciclo de Vida en SPA**: En aplicaciones Web de una sola página, el recolector de basura no es tu niñera. Limpia lo que ensucias.
