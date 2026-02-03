#### 🛠️ La Solución
1.  **Promoción de Variable**:  deja de ser una variable local en  y pasa a ser propiedad de clase .
2.  **Inyección Correcta**: En , pasamos explícitamente  a .

#### 🎓 Lecciones Aprendidas
*   *Variable Scope Blindness*: Cuidado con definir variables importantes dentro de métodos si luego las necesitas en el cierre.
*   Ahora sí: **Preview = Timelapse (1 FPS)** --> Transcodificación Rápida --> Subida Ligera.

### ✂️ v3.9.9-dev.14 Optimization: Solo Previews

#### 📜 El Contexto
El usuario ha determinado que el vídeo completo (Evidencia, ~5-10FPS) no es necesario en Telegram. El Timelapse (Preview, 1FPS) es suficiente para la vigilancia.

#### 🛠️ Cambios
*   **SentinelService**: Comentada la llamada a `TelegramUplink.enviarClip`.
*   **Resultado**: Ahorro drástico de ancho de banda y batería en cada detección. Solo se sube lo esencial.

### 🚀 v3.9.9-dev.14 Snapshot Deployment

#### 📜 El Contexto
Despliegue de validación para confirmar que el "Ajuste Quirúrgico" funciona: solo previews ligeros, cero vídeos pesados.

#### 🛠️ Checkpoint
*   **Version**: `v3.9.9-dev.14`
*   **Objetivo**: Verificar en Telegram que solo llega 1 mensaje (el preview MP4/AVI) por movimiento.
