## [v3.9.8] - La Actualización "Cool & Efficient"

* **📉 Retorno Estratégico a CIF (352x288):** Tras experimentar con VGA (640x480) y detectar un aumento insostenible de temperatura (+18°C vs basal), se revierte a CIF manteniendo todas las optimizaciones de código creadas para VGA. Resultado: Máxima eficiencia térmica.

* **❄️ Optimización "Short-Circuit JPEG":**
    - Intervención en el ciclo de vida del frame en `SentinelService`.
    - Si no hay grabación, ni UI activa, ni cliente Web: Se **ABORTA** la compresión JPEG (`yuv.compressToJpeg`) y se devuelve el buffer.
    - Impacto: CPU cercana al 0% en reposo absoluto.

* **🔄 Detección de Movimiento RAW (Sin Rotación):**
    - Eliminada la rotación incondicional de frames. El detector ahora trabaja sobre la imagen invertida/raw (el movimiento es agnóstico a la orientación).
    - La rotación solo se aplica CPU-intensivamente **SI Y SOLO SI** se va a grabar el archivo en disco.

* **🗑️ Limpieza de Procesos Fantasma:**
    - Eliminado callback vacío en `MainActivity` que forzaba generación de JPEGs innecesarios.
    - Eliminada lógica `bestFrameJpeg` (Smart Thumbnails): Se sustituye por vídeo, ahorrando la búsqueda y compresión del "mejor frame".

* **✍️ OSD Condicional:**
    - La fecha y hora ya no se "tatúan" en frames descartados. Solo se procesa el OSD si el frame va a grabación o streaming en vivo.

* **🛡️ Ajuste Thermal Guardian:**
    - Umbral de protección establecido en **44°C** (basado en pruebas donde la pantalla + carga de trabajo llevaban al límite). Desactiva detección de movimiento al superar el límite.

* **👁️ Ajuste de Detección:**
    - Stride reajustado a **10** para mantener precisión en resolución CIF.
