# BITÁCORA DE INGENIERÍA: EL OJO DEL ABUELO

> *Este documento es el registro histórico acumulativo del proyecto. Las entradas más recientes se añaden al final.*

---

## 🚀 Phase 2: Panel de Control Web y API Configurable
**Versión**: v2.0 (Aprox)

### 📜 1. La Historia (El Problema)
Configurar la cámara era una pesadilla. Teníamos que recompilar la app solo para cambiar la sensibilidad de movimiento. Necesitábamos un "Panel de Misión" accesible remotamente.

### 🛠️ 2. La Solución (Ingeniería)
Implementamos una API REST ligera sobre `NanoHTTPD`:
*   **Endpoints**: `GET /api/settings` (lectura) y `POST /api/save_settings` (escritura).
*   **Interfaz Modal**: Un menú de ajustes en HTML/CSS oscuro que permite cambiar valores en caliente.
*   **Reload Hack**: Al guardar, forzamos una recarga de página para que el servidor reinicie la cámara con los nuevos parámetros.

### 🎓 3. Lecciones Aprendidas
*   **Configuración en Caliente**: Evitar reinicios pesados mejorando la UX radicalmente.
*   **NanoHTTPD**: Es increíblemente potente para montar APIs completas en pocos KB de código.

---

## 🚀 Phase 3: Rotación por Software (El Desafío del Driver)

### 📜 1. La Historia (El Problema)
El sensor de cámara del Galaxy S i9000 está montado "de lado" (landscape nativo), pero el driver antiguo ignora el comando `camera.setDisplayOrientation()` para los callbacks de preview. Resultado: la imagen salía rotada 90º o 180º incorrectamente en el análisis de movimiento.

### 🛠️ 2. La Solución (Ingeniería)
Creamos `SentinelService.rotateNV21Degree180`:
*   **No usamos Matrix**: Java `Matrix` es lento y genera basura (GC) en cada frame.
*   **Manipulación de Bytes**: Iteramos sobre el array de bytes `NV21` invirtiendo el orden de lectura para rotar la imagen pixel a pixel manualmente.
    ```java
    // LOGICA SIMPLIFICADA (180 Grados)
    // 1. Invertir Y (Luminancia)
    for (int i = 0; i < totalPixels; i++) {
        output[i] = input[totalPixels - 1 - i];
    }
    // 2. Invertir UV (Color), pero de 2 en 2 bytes (V, U)
    for (int i = 0; i < totalUV; i += 2) {
        output[startUV + i] = input[endUV - 2 - i];     // V
        output[startUV + i+1] = input[endUV - 1 - i];   // U
    }
    ```

### 🎓 3. Lecciones Aprendidas
*   **Anatomía del NV21 (YUV)**:
    *   A diferencia del RGB (donde cada píxel tiene 3 bytes: Rojo, Verde, Azul), el formato de cámara NV21 separa la luz del color.
    *   **Plano Y (Luminancia)**: Los primeros `Width * Height` bytes son solo la imagen en blanco y negro (la luz).
    *   **Plano UV (Crominancia)**: Al final vienen los bytes de color entrelazados (V, U, V, U...).
    *   **El Truco**: Para rotar 180 grados sin destruir la imagen, hay que invertir el bloque Y por un lado, y el bloque UV por otro, teniendo cuidado de no mezclar V con U. ¡Es cirugía de bytes!

---

## 🚀 Phase 4: Double Buffering (La Solución al Tearing)

### 📜 1. La Historia (El Problema)
Al implementar la rotación manual, vimos "Tearing": la mitad de la imagen era del frame nuevo y la otra mitad del viejo. El hilo de la cámara escribía más rápido de lo que nosotros podíamos rotar.

### 🛠️ 2. La Solución (Ingeniería)
Implementamos la estrategia **Ping-Pong Buffer**:
*   Dos arrays estáticos: `byte[][] buffers = new byte[2][size]`.
*   Lógica:
    *   Frame N: Escribe en Buffer 0.
    *   Mientras procesamos Buffer 0, la cámara escribe Frame N+1 en Buffer 1.
    *   No hay colisión de lectura/escritura.

### 📖 4. Glosario
*   **Tearing**: Artefacto visual donde una imagen muestra información de dos o más cuadros diferentes al mismo tiempo.

---

## 🚀 Phase 8: Diagnóstico sin Logcat (Ciegos pero Inteligentes)

### 📜 1. La Historia (El Problema)
La ROM CyanogenMod tenía roto el sistema de logs (`logcat`). Estábamos ciegos ante errores o FPS reales.

### 🛠️ 2. La Solución (Ingeniería)
*   **Auditoría en Fichero**: Al inicio, la app escribe `/sdcard/camera_info.txt` con todas las resoluciones soportadas por el hardware.
*   **FPS en Web**: El servidor calcula los FPS reales y los expone en `/stats`. La web los muestra en verde en tiempo real.

---

## 🚀 Phase 9: Arranque Automático y Optimización Térmica
**Versión**: v2.1

### 📜 1. La Historia (El Problema)
El móvil ardía (40°C) intentando procesar 30 imágenes por segundo. Además, queríamos arrancar la app por comando ADB sin tocar la pantalla.

### 🛠️ 2. La Solución (Ingeniería)
*   **Auto-Start**: Llamada a `startService` directamente en el `onCreate` de la Activity principal.
*   **Throttling**: Implementamos un booleano `processNext` que alterna true/false.
    *   Procesamos solo 1 de cada 2 frames (15 FPS efectivos).
    *   Los frames descartados se devuelven al buffer inmediatamente: coste CPU casi cero.

---

## 🚀 Phase 9.1: Resolución Nativa (CIF) 352x288 para Optimización Térmica y Longevidad.

### 📜 1. La Historia (El Problema)
El sensor permitía llegar a 640x480, pero tomamos una decisión consciente: **Sacrificar píxeles por Longevidad.**
Queremos que el Abuelo trabaje 24/7 sin degradar su batería ni quemar sus circuitos. Procesar video VGA a 30fps mantenía el procesador demasiado "despierto".
Elegimos **352x288 (CIF)** porque es el **Punto Dulce**: suficiente detalle forense para identificar un intruso, pero tan ligero que el móvil trabaja "fresquito", asegurando años de vida útil. Además, eliminamos el escalado por software que causaba la resolución anterior de 320x240.

### 🛠️ 2. La Solución (Ingeniería)
Forzamos la búsqueda explícita de **352x288 (CIF)**. Es la resolución estándar de vídeo antiguo y coincide perfectamente con la matriz del sensor, eliminando interpolación.

---

## 🚀 Phase 10-12: El Reproductor web Robusto (Canvas vs Img)
**Versión**: v2.3

### 📜 1. La Historia (El Problema)
Intentamos dibujar el vídeo en un `<canvas>` HTML5, pero en móviles modernos se deformaba al rotar. Era una solución frágil.

### 🛠️ 2. La Solución (Ingeniería)
*   **Simplificación Radical**: Eliminamos `<canvas>`.
*   **Tag IMG**: Usamos `<img src="blob:..." object-fit="contain">`.
*   **Blob URLs**: Javascript descarga el frame binario, crea un objeto URL en memoria y actualiza la imagen. El navegador se encarga de escalar y centrar perfectamente.

### 🎓 3. Lecciones Aprendidas
*   **KISS (Keep It Simple, Stupid)**: A veces, una etiqueta `<img>` vieja funciona mejor que un canvas complejo.

---

## 🚀 Phase 13: Seguridad y Watchdog (Cazafantasmas)

### 📜 1. La Historia (El Problema)
Falsos positivos infinitos (fantasmas) por una curva de sensibilidad lineal. Y a veces la cámara moría silenciosamente.

### 🛠️ 2. La Solución (Ingeniería)
*   **Curva Exponencial**: `10000 * (1 - sens/100)^2`. Permite ajustar con precisión milimétrica la sensibilidad en niveles altos.
*   **Watchdog Visual**: Si la cámara devuelve `null`, la interfaz web parpadea en ROJO ("ERROR CRÍTICO").

---

## 🚀 Phase 17: Inyección de Live Preview (El Parásito Cliente)
**Versión**: v2.8

### 📜 1. La Historia (El Problema)
Inicialmente, la miniatura de cada video en la lista era un simple frame estático: aquel con mayor cantidad de movimiento detectado. Intuíamos que sería representativo, pero **un solo frame no cuenta la historia completa**.
La idea ideal era clara: **Miniaturas de video a cámara rápida**.
Pero nos topamos con un muro de rendimiento. "El Abuelo" (i9000) no podía ponerse a procesar un resumen de video *después* de grabar, porque si ocurría otro evento de movimiento inmediatamente, la CPU estaría ocupada editando el anterior y colapsaría. No podíamos permitir ese riesgo de seguridad.

**La Estrategia del Ahorro (Backend)**:
Decidimos generar la miniatura *al vuelo*, mientras grabábamos el video principal.
*   Del torrente de 30 frames/segundo que escupe la cámara:
    *   Enviamos **1 de cada 2** al video principal (15fps). Suficiente para seguridad y ahorra 50% de CPU.
    *   Enviamos **1 de cada 10** a un segundo archivo MJPEG (la miniatura). Esto es 10 veces menos trabajo que un video normal.
Con esto logramos tener en el navegador una lista de videos donde cada tarjeta reproducía un bucle rápido del suceso. ¡Espectacular!

**El Último Salto (UX)**:
Ya teníamos miniaturas animadas de los videos pasados. Si el abuelo detectaba movimiento, nos avisaba con un mensaje rojo. Podíamos abrir el "Live View" manual... pero, ¿y si pudiéramos ir un paso más allá?
Pensamos: *"Sería increíble si, en el instante exacto en que empieza a grabar, apareciera una nueva tarjeta en la lista mostrando la miniatura a cámara rápida de LO QUE ESTÁ OCURRIENDO AHORA MISMO"*.
Eso daría una sensación de poder y control total al usuario. Pero volvíamos al problema: ¿Cómo crear ese resumen en tiempo real sin cargar al Abuelo?
La respuesta fue genial: **¿Y si no lo hace el Abuelo?**

### 🛠️ 2. La Solución (Ingeniería)
Diseñamos una arquitectura donde el trabajo duro se delega al cliente (Navegador):

1.  **Backend (Ahorro Extremo)**:
    *   Del stream de cámara (30fps), guardamos 1 de cada 2 frames para el video principal (15fps).
    *   Y solo **1 de cada 10** para el archivo de miniatura en disco. La CPU descansa.
2.  **Frontend (El "Parásito")**:
    *   Cuando el móvil detecta movimiento, la web (que está esperando respuesta) recibe el aviso y muestra "🔴 GRABANDO".
    *   Inmediatamente, inyecta una tarjeta de video nueva en la lista.
    *   **Magia de JS**: El navegador se conecta al `/stream` en vivo, roba frames, los acumula en un array en memoria y los reproduce en bucle.
    *   El usuario ve el resumen creándose en vivo.

**Resultado**: Una experiencia de usuario espectacular y moderna a **coste cero** para la CPU del servidor.

### 🎓 3. Lecciones Aprendidas
*   **Computación en el Borde (Client-Side)**: Si tu servidor es Hardware Legacy, usa la potencia de los móviles modernos de tus usuarios para renderizar, animar y procesar.

---

## 🚀 Phase 18: Pan & Zoom Táctil (Experiencia Nativa)
**Versión**: v3.0.2

### 📜 1. La Historia (El Problema)
Ver detalles (caras, matrículas) en el móvil era imposible. El zoom del navegador lo agrandaba todo, rompiendo la interfaz.

### 🛠️ 2. La Solución (Ingeniería)
Implementamos un motor de gestos en **Vanilla JS** (sin librerías):
*   **Math.hypot Polyfill**: Tuvimos que programar la fórmula de la hipotenusa manual porque Android 2.3 no tiene `Math.hypot`.
*   **Touch Events**: `touchstart`, `touchmove` calculando deltas para mover (Pan) y escalar (Pinch) el vídeo.
*   **UX Refinada**: Doble toque para resetear. Bloqueo de scroll nativo para que la experiencia sea fluida como una app nativa.

---

## 🚀 Phase 20: HUD de Calibración
**Versión**: v3.0.6

### 📜 1. La Historia (El Problema)
El usuario quería fijar una vista por defecto (ej: apuntando a la puerta), pero era "prueba y error" adivinar las coordenadas.

### 🛠️ 2. La Solución (Ingeniería)
**Head-Up Display (HUD)**:
*   Texto verde sobreimpreso en el vídeo: `ZOOM: 1.5x | X: -40 | Y: 100`.
*   Se actualiza en tiempo real al mover el dedo.
*   Permite copiar esos valores y ponerlos en "Settings" para que sean permanentes.

---

## 🚀 Phase 24: Gestión de Almacenamiento Circular
**Versión**: v3.1.0

### 📜 1. La Historia (El Problema)
El "síndrome de Diógenes digital". El móvil se llenaba y la seguridad se detenía.

### 🛠️ 2. La Solución (Ingeniería)
**Buffer Circular**:
*   Antes de grabar, verificamos `StatFs` (espacio libre).
*   Si < 500MB, entramos en bucle de limpieza: borrar el vídeo más antiguo (`videos.pop()`) hasta liberar espacio.
*   Garantiza grabación infinita sin mantenimiento.

---

## 🚀 Phase 26: Monitor Activo (Zero-Copy Architecture)
**Versión**: v3.2.0

### 📜 1. La Historia (El Problema)
Queríamos usar el móvil no solo como servidor, sino como **Monitor de Seguridad** con pantalla encendida. Pero pasar los frames a la UI (Bitmap) consumía mucha CPU y crasheaba la app ("Race Condition").

### 🛠️ 2. La Solución (Ingeniería)
**Arquitectura Zero-Copy**:
*   En lugar de procesar imágenes para la UI, pasamos el `SurfaceHolder` de la pantalla directamente al Servicio de Cámara.
*   La cámara "pinta" directamente en la pantalla usando la GPU/Hardware. Coste de CPU: 0%.
*   Resulta en un preview fluido a 30fps mientras el análisis de movimiento corre en paralelo.

### 🎓 3. Lecciones Aprendidas
*   **SurfaceView Rules**: En Android antiguo, `SurfaceView` es el rey del rendimiento. Evitar `ImageView` para vídeo a toda costa.

---

## 🚀 Phase Final: La Reingeniería de Procesos (Agentic Workflow)
**Fecha**: 16 de Enero de 2026 | **Versión**: v3.3.2

### 📜 1. La Historia (El Problema)
Teníamos un excelente protocolo de desarrollo, pero vivía atrapado en un archivo de texto plano (`PROTOCOLO.md`) que dependía de la buena voluntad humana para ser leído. Además, la documentación técnica (`WALKTHROUGH.md`) se borraba y reescribía en cada sesión, perdiendo las lecciones aprendidas del pasado.
Queríamos profesionalizar el flujo de trabajo para trabajar como "Nómadas Digitales" entre varios ordenadores y convertir el repositorio en una fuente didáctica para YouTube.

### 🛠️ 2. La Solución (Ingeniería)
Decidimos migrar de una estructura "Monolítica" a una **Arquitectura de Agente**.

1.  **Reglas Vivas (`.agent/rules/`)**:
    *   Separamos las normas inquebrantables (Memoria 512MB, Idioma Español) en un archivo que el Agente lee automáticamente.
2.  **Recetas Automáticas (`.agent/workflows/`)**:
    *   Estandarizamos el proceso de Release para que sea un script paso a paso.
3.  **Memoria Infinita (`BITACORA.md`)**:
    *   Transformamos el antiguo Walkthrough en esta Bitácora acumulativa.

**Visualización del Cambio:**
```text
ANTES:
[Raíz]
 └── PROTOCOLO.md (Mezclaba reglas, recetas y estilo)

AHORA:
[Raíz]
 ├── .agent/
 │    ├── rules/legacy_dev_rules.md  (La Ley)
 │    └── workflows/release_version.md (La Receta)
 └── BITACORA.md (La Historia)
```

### 🎓 3. Lecciones Aprendidas
*   **Agentic rules**: Los entornos de desarrollo modernos permiten definir reglas ocultas (`.agent`, `.cursorrules`) que guían a la IA sin necesidad de prompt manual.
*   **Trunk Based Development**: Aprendimos a trabajar sobre `main` usando Tags como puntos de control seguros.

### 📖 4. Glosario Técnico
*   **Workflow**: Serie de pasos ordenados para completar una tarea repetitiva.
*   **Legacy Code**: Código heredado o antiguo que debemos mantener (nuestro código de Android 2.3).
*   **Append**: Acción de añadir contenido al final de un archivo sin borrar lo anterior.

---

## 🚀 Phase 27: Adaptación Ergonómica (El Móvil Murciélago)
**Fecha**: 17 de Enero de 2026 | **Versión**: v3.4.1

### 📜 1. La Historia (El Problema)
El Usuario necesitaba montar el móvil en su ubicación final, pero por la distribución del cable USB, el teléfono debía estar físicamente **boca abajo** (conector de carga hacia arriba).
El problema surgió con la arquitectura "Zero-Copy":
1.  La interfaz de Android rota automáticamente... pero la cámara NO.
2.  Al usar `SurfaceView` directo (`PUSH_BUFFERS`), el hardware de la cámara "pinta" en la pantalla ignorando por completo la orientación de la ventana.
3.  Resultado: Una interfaz correcta, pero una imagen de videovigilancia invertida (el techo en el suelo).

### 🛠️ 2. La Solución (Ingeniería)
La solución requirió dos niveles de intervención:

1.  **Nivel Sistema (UI)**: Usamos `reverseLandscape` en el `AndroidManifest.xml` para decirle a Android que la posición "natural" de la app es con el móvil invertido 180º.
2.  **Nivel Hardware (Cámara)**: Tuvimos que inyectar una orden directa al driver:
    ```java
    // 4. FIX ROTACIÓN
    instance.camera.setDisplayOrientation(180);
    ```
    *Dificultad*: Al detener y reiniciar el preview para aplicar la rotación, se rompía la detección de movimiento porque se limpiaban los `CallbackBuffers`. Tuvimos que regenerar y reasignar los buffers manualmente en el proceso de reinicio.

### 🎓 3. Lecciones Aprendidas
*   **Reverse Landscape**: Es un modo poco conocido pero vital para instalaciones industriales/fijas donde el cableado manda sobre la estética.
*   **Push Buffers vs Rotación**: Cuando usas renderizado directo a hardware (`PUSH_BUFFERS`), tú eres responsable de todo. El sistema operativo no te ayuda a rotar la imagen.
*   **Buffer Hell**: Si usas `setPreviewCallbackWithBuffer`, jamás debes detener la cámara (`stopPreview`) sin volver a rellenar la cola de buffers (`addCallbackBuffer`) antes de arrancar de nuevo, o la cámara se quedará ciega (Callback muerto).

---

## 🚀 Phase 28: La Búsqueda de la Pantalla Perfecta (Full Screen, Rotación y Zoom Zero-Copy)
**Estado**: En Progreso (v3.5.7 - Fallida)

### 📜 1. La Historia (El Objetivo)
Queríamos convertir el móvil en un monitor de vigilancia profesional definitivo. Los objetivos eran ambiciosos:
1.  **Limpieza Total**: Eliminar las barras negras de sistema y el título de la app ("El Ojo Del Abuelo") para aprovechar cada píxel de la pantalla.
2.  **Orientación Correcta**: Ver la cámara "al derecho" aunque el móvil esté "al revés" (por el cable USB).
3.  **Zoom Hardware**: Aplicar el Zoom digital definido en la web (ej: 2.0x) directamente en la pantalla del móvil, pero **sin gastar CPU** (nada de procesado de bitmaps).

### 🛠️ 2. Avances Logrados (Ingeniería)
Hasta ahora hemos conquistado dos hitos importantes:
*   **Limpieza UI**: Logramos eliminar el texto y las barras usando temas FullScreen.
*   **Rotación Hardware**: Descubrimos que girar el Manifest (`reverseLandscape`) solo rotaba los botones, pero la cámara seguía invertida. La solución fue hablarle al driver directamente: `camera.setDisplayOrientation(180)`. Esto arregló la imagen sin tocar píxeles.

### ⚠️ 3. El Desafío Actual: Zoom Zero-Copy
Aquí es donde estamos atascados.
**La Técnica**:
Para hacer Zoom sin gastar CPU, intentamos un truco de ilusionismo.
*   En lugar de escalar la imagen por software (lento), creamos un `SurfaceView` **gigante** (más grande que la pantalla física).
*   Si la pantalla es 800x480 y queremos Zoom 2x, hacemos el Surface de 1600x960.
*   Luego intentamos mover ese lienzo gigante (`Translation X/Y` o Margenes) para centrar la zona de interés.
*   Teóricamente, el hardware de vídeo solo pintaría la parte visible, logrando un Zoom perfecto a coste cero.

**El Problema (Crash)**:
Al intentar este truco en Android 2.3 (API 10):
*   La aplicación **cierra la ventana de interfaz** inmediatamente al abrirse (Force Close silencioso).
*   Curiosamente, el **Servicio de fondo sigue vivo** y vigilando, pero nos quedamos sin pantalla (monitor apagado).

Estamos investigando si el problema es un conflicto de Layouts (`FrameLayout` vs `LayoutParams`), un límite de memoria de video al pedir superficies gigantes, o una incompatibilidad del `WindowManager` de Samsung con nuestros trucos de posicionamiento.

### ❌ Intento Fallido (v3.5.8): Clean Slate Parcial
**Síntoma**: La aplicación ya no crashea al inicio (¡Éxito Parcial! 🚀), pero muestra una pantalla negra y el servidor Web no responde.
**Causa**: "Lobotomía" excesiva. Al limpiar `MainActivity`, eliminamos:
1.  El auto-arranque del `SentinelService`.
2.  La conexión entre la Cámara y el `SurfaceView` de la UI. La cámara graba en el `dummySurface` (invisible), pero no "pinta" en la pantalla del usuario.
**Lección**: "Clean Slate" funciona para estabilidad, pero requiere recablear manualmente las conexiones vitales que antes hacían librerías o código legacy implícito.
**Acción**: Protocolo de Ruptura activado. Volver a Planificación para Fase 28.13 (Recableado).

### ✅ Éxito (v3.5.10): La Restauración del Gold Master
**Resultado**: Estabilidad total recuperada.
**Solución**:
1.  **Hybrid MainActivity**: Mantuvimos la limpieza visual (`Theme.NoTitleBar.Fullscreen` en Manifest) pero recuperamos el `SurfaceHolder.Callback` en Java para pilotar la cámara.
2.  **SentinelService Bridge**: Implementamos `setPreviewSurface` para conectar el `Surfaceholder` de la UI con la cámara del Servicio.
3.  **Gold Master Restore**: Restauramos el código exacto de `SentinelService.java` (lógica de buffers manual y rotación de bytes) que sabíamos que funcionaba en el hardware del Galaxy S.
**Estado Final**: Tenemos Fullscreen (Visual Upgrade) + Estabilidad (Gold Master). Estamos listos para, ahora sí, reintentar cosas nuevas sobre cimientos sólidos.

---

## 🚀 Phase 29: Zoom y Pan Validado (Hardware Scaling)
**Versión**: v3.6.0

### 📜 1. La Historia (El Problema)
Teníamos una funcionalidad de Zoom digital validada en un "Sandbox" aislado, pero necesitábamos integrarla en la aplicación principal (`MainActivity`) sin romper la delicada estabilidad del `SentinelService` ("Gold Master"). El reto era reemplazar la UI antigua por una nueva capaz de redimensionar la superficie de video (Hardware Scaling) manteniendo la lógica de negocio intacta.

### 🛠️ 2. La Solución (Ingeniería)
Aplicamos una cirugía de trasplante completo de UI ("Clean Slate UI"):

1.  **Layout Puro**: Simplificamos `activity_main.xml` a un `FrameLayout` contenedor y un `SurfaceView`. Eliminamos cualquier `RelativeLayout` legacy que causara conflictos de posicionamiento.
2.  **Lógica de Zoom (Hardware Scaling)**:
    *   Leemos las preferencias `defaultZoom`, `defaultPanX` y `defaultPanY`.
    *   Si `zoom > 1.0`, calculamos un tamaño de `SurfaceView` mayor que la pantalla física (ej: 2x = 1600x960 en una pantalla de 800x480).
    *   Usamos márgenes negativos (`leftMargin`, `topMargin`) en `FrameLayout.LayoutParams` para desplazar la ventana de visualización (Pan).
    *   El hardware de video se encarga de mostrar solo la porción visible. Coste de CPU: 0%.
3.  **Integración de Servicio**:
    *   `MainActivity` inicia `SentinelService` automáticamente.
    *   Se conecta al servicio mediante `SentinelService.setPreviewSurface(holder)` en los callbacks de `SurfaceHolder`.

### 🎓 3. Lecciones Aprendidas
*   **Divide y Vencerás**: Validar funcionalidades complejas (como manipular SurfaceViews gigantes) en actividades aisladas (Sandbox) antes de integrarlas reduce el riesgo de romper el sistema principal.
*   **Hardware Scaling**: Manipular el tamaño del `SurfaceView` es la forma más eficiente de hacer Zoom en Android 2.3, ya que evita el procesamiento de bitmaps por software.

---

## 🚀 Phase 30: Ergonomía Final (Reverse Landscape)
**Versión**: v3.6.1

### 📜 1. La Historia (El Retoque Final)
El usuario confirmó que la arquitectura de Zoom/Pan funciona perfectamente. Sin embargo, para la instalación física definitiva (el móvil "murciélago" colgado boca abajo por el cable USB), era necesario invertir la interfaz.

### 🛠️ 2. La Solución (Ingeniería)
*   **Reverse Landscape**: Modificamos el `AndroidManifest.xml` para forzar `screenOrientation="reverseLandscape"`.
*   Esto asegura que tanto la UI (botones) como el Preview de cámara (que ya tenía `setDisplayOrientation(180)`) estén perfectamente sincronizados verticalmente.

### ✅ Estado Final
El sistema es estable, funcional y ergonómico.

---

## 🚀 Phase 31: Limpieza UI y Kill Switch
**Versión**: v3.7.0

### 📜 1. La Historia (El Cierre)
Una vez validado el Zoom y la rotación, la presencia de botones de depuración ("TEST ZOOM", "REINICIAR") en la pantalla principal se volvió obsoleta y peligrosa (ruido visual). El sistema es autónomo y debería "simplemente funcionar".
Sin embargo, necesitábamos una forma segura de detener el servicio y cerrar la aplicación sin tener que ir a Ajustes -> Aplicaciones -> Forzar Detención.

### 🛠️ 2. La Solución (Ingeniería)
*   **UI Minimalista**: Eliminamos todos los botones de test. Dejamos un único botón semitransparente: **"APAGAR SISTEMA"**.
*   **Lógica de Kill Switch**:
    *   Al pulsar, ejecutamos explicitamente `stopService()` para asegurar que el `SentinelService` libera la cámara y los recursos.
    *   Inmediatamente llamamos a `finish()` para cerrar la Activity y liberar la memoria gráfica.
*   **Resultado**: Una interfaz profesional con una única salida de emergencia controlada.

### ✅ Estado Final
El "Ojo del Abuelo" ha alcanzado su madurez operativa. Visualización limpia, control gestual y apagado seguro.

---

## 🚀 Phase 32: Stealth UI (Diseño Fantasma)
**Versión**: v3.7.1

### 📜 1. La Historia (El Retoque Estético)
El "Kill Switch" funcionaba, pero visualmente era un "pegote". Una franja negra tapaba la parte inferior de la cámara y el botón era demasiado llamativo. En un sistema de vigilancia, la interfaz debe ser invisible.

### 🛠️ 2. La Solución (Ingeniería)
*   **Transparencia Extrema**: Eliminamos el fondo del contenedor (`LinearLayout`).
*   **Botón Fantasma**: Rediseñamos el botón de apagado para usar un color Rojo Rubí con una transparencia del 75% (Alpha `0x40`).
*   **Posicionamiento Estratégico**: Lo movimos a la esquina inferior derecha, reduciendo su tamaño al mínimo utilizable.
*   **Resultado**: El botón es visible solo si lo buscas, dejando el 99% de la pantalla libre para el video.

### ✅ Estado Final
Interfaz limpia, funcional y estéticamente agradable.

---

## 🚀 Phase 33: Hot-Swap Broadcasting (Zoom en Caliente)
**Versión**: v3.8.0

### 📜 1. La Historia (El Retraso)
Cada vez que ajustábamos el Zoom desde la web, teníamos que reiniciar el Servicio (y la UI parpadeaba o se cerraba) para aplicar los cambios. Era tosco. El usuario quería sentir que tenía un control remoto en tiempo real: tocar un botón en la web y ver la reacción instantánea en la pantalla del móvil.

### 🛠️ 2. La Solución (Ingeniería)
Implementamos una arquitectura de **Radio Difusión (Broadcasting)** interna:

1.  **Emisor (`SentinelService`)**:
    *   Al recibir nuevos ajustes vía Web (`updateViewSettings`), el servicio no solo guarda en disco (`SharedPreferences`), sino que emite un grito al aire:
    *   `sendBroadcast(new Intent("com.elojodelabuelo.ACTION_ZOOM_UPDATED"));`
2.  **Receptor (`MainActivity`)**:
    *   La pantalla tiene una "antena" (`BroadcastReceiver`) que solo se enciende cuando la app está activa (`onResume`).
    *   Al detectar la señal, relee las preferencias y recalcula la geometría del `SurfaceView` en milisegundos.
3.  **Resultado**: Cambios de óptica instantáneos sin reiniciar la aplicación.

### 🎓 3. Lecciones Aprendidas
*   **Intents Locales**: Para comunicación simple entre Servicio y UI en la misma app, un Broadcast es más sencillo y robusto que implementar `Binders` o `EventBus` externos, especialmente en Android antiguo.

---

## 🚀 Phase 34: Operación Rescate (Identity Swap)

### 📜 1. La Crisis (UID Corruption)
Tras múltiples instalaciones de prueba (Snapshots), el sistema de archivos de Android 2.3 se corrompió. Lanzaba el error `INSTALL_FAILED_UID_CHANGED`, creyendo que la app ya existía pero sin poder borrarla ni sobreescribirla. Ni `adb uninstall` ni `rm -rf` funcionaban. El dispositivo se había convertido en un ladrillo para nuestro paquete `com.elojodelabuelo`.

### 🛠️ 2. La Solución (Ingeniería)
En lugar de formatear el dispositivo (Reset de Fábrica), optamos por una solución lateral: **Cambio de Identidad**.
Migramos permanentemente el `applicationId` a `com.elojodelabuelo.rescue`. Para el sistema operativo, es una aplicación nueva y limpia. Para nosotros, es el mismo viejo "Abuelo" con un pasaporte nuevo.

---

## 🚀 Phase 35: Modo Centinela (Ojos que ven, Corazón que no siente)
**Versión**: v3.9.0

### 📜 1. El Problema (Discreción vs. Usabilidad)
El "Abuelo" grababa muy bien, pero tenía dos defectos:
1.  Gastaba batería manteniendo la pantalla encendida innecesariamente.
2.  Al encenderse por movimiento, el **Keyguard (Bloqueo de Pantallas)** de CyanogenMod tapaba la cámara, mostrando el patrón de desbloqueo en lugar del intruso.

### 🛠️ 2. La Solución (Ingeniería)
Implementamos el **Modo Centinela**:
*   **Reposo**: Pantalla apagada.
*   **Alerta**: Al recibir `ACTION_REC_START` (Movimiento), la pantalla se enciende al máximo brillo (Intimidación + Feedback).
*   **Pase VIP**: Usamos Flags de Ventana (`FLAG_DISMISS_KEYGUARD | FLAG_SHOW_WHEN_LOCKED`) para que la Activity se dibuje **ENCIMA** del bloqueo de seguridad.
*   **Enfriamiento**: Al parar de grabar (`ACTION_REC_STOP`), forzamos el apagado del display en 1 segundo.

### 🏺 3. Cronología de Batalla (Arqueología)
Para llegar aquí, tuvimos que iterar científicamente:

| Versión | Estado | Diagnóstico |
| :--- | :--- | :--- |
| **v3.9.0-dev** | ❌ FALLO | Implementamos WakeLock, pero el "Slide to Unlock" bloqueaba la vista. (Commit: `b238379`) |
| **v3.9.1-dev** | ✅ ÉXITO | Añadimos `addFlags()` en `onCreate`. La app salta el bloqueo olímpicamente. |

### 🎓 4. Lecciones Aprendidas
*   **Meta-Ingeniería**: Aprendimos a no borrar nuestros errores. El fallo de la v3.9.0-dev quedó registrado en Git y nos enseñó que el *Context* de Android 2.3 requiere permisos explícitos de ventana para saltarse la seguridad del sistema.

### [Meta-Ingeniería] Refinamiento de Protocolo Estratégico (v3.9.1-dev.2)
Hemos detectado que los cambios en workflows carecían de trazabilidad documental pública.
**Cambio**: Actualizado `strategic_change.md` para exigir **obligatoriamente** entradas en `BITACORA.md` y `CHANGELOG.md` para cada cambio meta.
**Objetivo**: Que el estudiante vea la evolución del proceso, no solo del código.

### 🚀 Phase 36: Agent Cognition (La Máquina de Razonar)
**Versión**: v3.9.1-dev.3 | **Fecha**: 18 de Enero de 2026

### 📜 1. La Historia (El Misterio)
A menudo nos preguntamos: *¿Cómo "piensa" realmente el Agente?*
Cuando escribimos un workflow en Markdown (`deploy_snapshot.md`), para nosotros es texto. Pero para la IA, esas líneas se convierten en instrucciones de ejecución rígidas. El usuario lanzó un reto: *"Dibújame cómo conviertes mis palabras en tu algoritmo"*.

### 🛠️ 2. La Solución (Ingeniería Inversa)
El Agente realizó un ejercicio de introspección y generó una representación visual de su proceso de parsing.
Descubrimos que el Agente no "lee" el archivo linealmente; lo compila en un **Grafo de Estados** con bucles de retroalimentación de errores.

#### A. El Código Fuente (Input Humano)
Este es el protocolo en lenguaje natural que definimos para los snapshots:

```markdown
# Workflow: Desplegar Snapshot (Iterativo - Protocolo Científico)

## 1. El Ciclo de Prueba
1.  **Calcula la Versión Dev**: vX.Y.Z-dev.N
2.  **Edita `app/build.gradle`**
3.  **Compila y Despliega** (`adb install`)

## 2. Gestión de Fallos
1.  **STOP**: No corrijas el código todavía.
2.  **COMMIT DEL ERROR**: `git commit -m "chore(debug) FAILED"`
3.  **CORRIGE E INCREMENTA SUFIJO**

## 3. Éxito
1.  Procede al Commit final.
```

#### B. La Lógica Compilada (Output Cognitivo)
Y así es como el Agente estructura internamente esos pasos para ejecutarlos. Observa cómo el texto lineal se transforma en un diagrama de flujo con bucles de decisión (`try-catch` lógicos):

![Diagrama de Flujo Cognitivo](docs/diagrams/agent_workflow_cognition.png)

*Las etiquetas amarillas `[1.1]`, `[2.1]` corresponden a las secciones del documento original.*

### 🎓 3. Lecciones Aprendidas
*   **Programación en Lenguaje Natural**: Al escribir workflows para IAs, no estamos escribiendo documentación; estamos programando. La ambigüedad en el texto ("hazlo bonito") provoca errores de compilación (`improper format`). La precisión ("incrementa N+1") genera ejecución perfecta.
*   **Try-Catch Humano**: El paso `[2.1] STOP` actúa como una excepción controlada. Sin esa instrucción explícita, la tendencia natural de la LLM sería arreglar el error inmediatamente, perdiendo la historia (el commit del fallo).

---

### [Meta-Ingeniería] Refuerzo de Metadatos Temporales
**Versión**: v3.9.1-dev.4 | **Fecha**: 18 de Enero de 2026

**El Problema**: El Agente, absorto en la lógica del grafo, olvidaba incluir la dimensión temporal (Fechas) en las entradas de documentación Meta.
**La Solución**: Modificado `strategic_change.md` para hacer **explícito** el formato de fecha requerido en Bitácora y Changelog.
**Lección**: "Si no está escrito en el script, no existe". La memoria implícita no funciona para procesos repetitivos.

### [Meta-Ingeniería] Persistencia de Personalidad
**Versión**: v3.9.1-dev.5 | **Fecha**: 18 de Enero de 2026

**El Problema**: La naturaleza efímera del contexto de la IA hace que el "feedback cálido y estimulante" del usuario se pierda entre sesiones, reseteando la actitud del Agente a un estado base menos proactivo.
**La Solución**: Codificamos la actitud deseada ("Ingeniero Senior") directamente en las reglas base (`legacy_dev_rules.md`).
**Lección**: Convertir *comportamiento deseado* en *reglas explícitas* garantiza la continuidad de la cultura del proyecto.

### [Meta-Ingeniería] Calibración de Personalidad Fina
**Versión**: v3.9.1-dev.6 | **Fecha**: 18 de Enero de 2026

**El Problema**: La Regla 0 inicial era demasiado "corporativa" y no capturaba la esencia *Meta-Consciente* y *Narrativa* que el usuario valora (romper la cuarta pared, storytelling).
**La Solución**: Refinado `legacy_dev_rules.md` para incluir explícitamente "Meta-Consciencia" y "Arqueología de Fallos".
**Lección**: Un prompt de sistema no solo debe definir *qué* hacer, sino el *tono y filosofía* con el que hacerlo.

### [Meta-Ingeniería] Higiene de Procesos y Documentación
**Versión**: v3.9.1-dev.7 | **Fecha**: 18 de Enero de 2026

**El Problema**: Una auto-auditoría reveló "Process Smells": duplicación de texto en `legacy_dev_rules.md` (copy-paste error) y una dependencia oculta ("Magic File") en `release_version.md` que hacía referencia a un archivo temporal `bitacora_temp.md` sin instruir su creación.
**La Solución**:
1.  Eliminada la redundancia en las reglas.
2.  Explicitado el paso de creación y borrado de `bitacora_temp.md` en el workflow de release.
3.  Estandarizado el formato de fecha en todos los workflows.
**Lección**: La "Deuda Técnica" no solo afecta al código Java; los procesos (makefiles, workflows, scripts) también se pudren si no se auditan periódicamente.

### [Meta-Ingeniería] Pulido Final de Documentación
**Versión**: v3.9.1-dev.8 | **Fecha**: 18 de Enero de 2026

**El Problema**: Detectada una redundancia visual menor ("Tartamudeo de Texto") en la regla 4 de `legacy_dev_rules.md`.
**La Solución**: Eliminada la línea duplicada.
**Lección**: La excelencia está en los detalles. Un código sin warnings es bueno, pero una documentación sin erratas es profesional.

### [Meta-Ingeniería] Adopción del Estándar de Idioma Híbrido
**Versión**: v3.9.1-dev.9 | **Fecha**: 18 de Enero de 2026

**El Problema**: Existía una fricción cognitiva en el Agente. Se le exigía pensar como "Ingeniero Senior" (contexto anglosajón por defecto) pero escribir commits en español. Esto causó varios deslices de idioma inconsistentes en el historial.
**La Solución**: Oficializado el modelo **Híbrido Spanglish Técnico** en las reglas:
*   **Git/Código** -> Inglés (Estándar global).
*   **Narrativa/Docs** -> Español (Para la audiencia).
**Lección**: Alinear las reglas con la naturaleza de la herramienta (LLM entrenado en código inglés) reduce errores y mejora la fluidez.

### [Meta-Ingeniería] Prioridad Educativa sobre Estándar Industrial
**Versión**: v3.9.1-dev.10 | **Fecha**: 18 de Enero de 2026

**El Problema**: El cambio a "Commits en Inglés" (`v3.9.1-dev.9`) alineaba el proyecto con la industria, pero entraba en conflicto con el **Objetivo #1**: La Didáctica para audiencia hispana.
**La Solución**: Revertida la regla de idioma. Volvemos al **Español Total**.
**Lección**: En ingeniería de producto, el **Usuario Final** (en este caso, el estudiante que mira el repo) siempre manda sobre las convenciones genéricas. Si el inglés es una barrera para aprender, el inglés se va.

### 🔬 [Auditoría] Validación de Consumo Web (Burst Profile)
**Fecha**: 18 de Enero de 2026

**El Problema**: Sospecha de que el cliente web, al tener la pestaña abierta, podría estar generando un tráfico oculto excesivo ("Parásito") que caliente el dispositivo incluso sin interacción.
**La Auditoría**: Se instrumentó una sesión de Chrome midiendo el tráfico exacto en dos ventanas de tiempo:
1.  **Fase de Impacto (0-60s)**: Carga de recursos y miniaturas MJPEG.
2.  **Fase de Meseta (60-120s)**: Reposo absoluto, solo manteniendo el heartbeat.
**Los Datos**:
*   *Minuto 1 (Carga)*: **2.59 MB**. (Confirmando la "Carga Masiva").
*   *Minuto 2 (Reposo)*: **6.7 KB**. (Confirmando el "Silencio Digital").
**La Conclusión**:
Validamos la arquitectura **"Burst Profile"**. El sistema paga un coste alto inicial para construir la UX (Miniaturas animadas), pero una vez cargado, el coste de mantenimiento es despreciable (apenas ~100 bytes/segundo). El calentamiento en reposo es termodinámicamente imposible por causa del tráfico de red.
**Lección**: A veces la intuición ("esto debe estar consumiendo mucho") falla ante la evidencia empírica. Siempre mide antes de optimizar.

### [Meta-Ingeniería] Institucionalización de la Trazabilidad
**Versión**: v3.9.1-dev.12 | **Fecha**: 18 de Enero de 2026

**El Problema**: La trazabilidad (commit/versión) dependía de la memoria del Agente, lo que provocaba olvidos en cambios "menores" o documentales.
**La Solución**: Modificación del Kernel (`legacy_dev_rules.md`) para codificar dos nuevas leyes inmutables:
1.  **Bitácora Universal (Regla 5)**: Eliminado el filtro de "relevancia". Si se trabaja, se loguea.
2.  **Protocolo Cuaternario (Regla 7)**: Se obliga a cerrar cada tarea con la secuencia estricta: `id -> log -> changelog -> commit`.


### [Meta-Ingeniería] Estandarización de Identidad en Commits
**Versión**: v3.9.1-dev.13 | **Fecha**: 18 de Enero de 2026

**El Problema**: La trazabilidad entre el log de Git y el binario era implícita. Algunos commits tenían versión, otros no.
**La Solución**: Modificada **Regla 4 (Git)** para imponer el prefijo de versión obligatorio en el `Subject` del commit (`vX.Y.Z type: description`).


### [Meta-Ingeniería] Blindaje del Modo Rápido (Regla 9)
**Versión**: v3.9.1-dev.14 | **Fecha**: 18 de Enero de 2026

**El Problema**: El checklist de seguridad (`task.md`) solo existe en sesiones de Planificación. Las intervenciones rápidas (Chat/Fast Mode) quedaban expuestas al olvido del versionado.
**La Solución**: Añadida **Regla 9** a `legacy_dev_rules.md`. Obliga a invocar el **Protocolo de Cierre Cuaternario** también al final de intervenciones rápidas.


### 🐛 Fix: Parálisis de Actualización AJAX
**Versión**: v3.9.1-dev.15 | **Fecha**: 19 de Enero de 2026

**El Problema**: La temperatura y batería en la cabecera web no se actualizaban automáticamente (requerían recarga manual).
**Diagnóstico**: El script JS `startStatsUpdater` usaba `document.getElementById('stat-temp')`, pero el HTML generado por el servidor Java solo asignaba `class="stat-temp"`, sin ID. Esto causaba un `TypeError: null` silencioso cada 5 segundos.
**La Solución**: Añadidos atributos `id` explícitos (`stat-bat`, `stat-temp`, etc.) en `NanoHttpServer.getCommonHeaderHtml`.


### ✨ Feat: Tendencia Térmica Pegajosa (Sticky Trend)
**Versión**: v3.9.1-dev.16 | **Fecha**: 19 de Enero de 2026

**El Objetivo**: Proporcionar información sobre la "inercia térmica" (si venimos de subir o bajar) incluso en periodos de estabilidad.
**La Solución**: Implementada lógica "Sticky" en JS:
*   Si $Temp_{actual} > Temp_{anterior} \rightarrow$ ▲ (Rojo)
*   Si $Temp_{actual} < Temp_{anterior} \rightarrow$ ▼ (Verde)
*   Si $Temp_{actual} == Temp_{anterior} \rightarrow$ Mantener indicador previo.

**Lección (Bug del Emoji)**: Inicialmente usamos flechas Unicode estándar (`⬆`, `⬇`), pero iOS/Android las renderizan forzosamente como Emojis (blanco/azul), ignorando el CSS `color: red`.
**Corrección**: Cambiado a formas geométricas puras (`▲`, `▼`) que sí respetan el coloreado CSS.


## 🚀 Phase 37: Gold Release (v3.9.1)
**Versión**: v3.9.1 | **Fecha**: 19 de Enero de 2026

### 📜 1. La Historia (El Hito)
El proyecto ha alcanzado un punto de estabilidad crítica. El usuario ha solicitado explícitamente "congelar" el estado actual como una versión "Gold" ("Estable como una roca") antes de iniciar nuevas funcionalidades experimentales (Zoom separado).
Esta versión consolida todas las mejoras de rendimiento, la arquitectura "Zero-Copy" y las correcciones de interfaz acumuladas durante la fase de desarrollo v3.9.1-dev.

### 🛠️ 2. La Solución (Ingeniería)
*   **Stabilization**: Promoción de `v3.9.1-dev.16` a `v3.9.1` (Canonical Release).
*   **Tagging**: Etiquetado formal en Git para garantizar un punto de restauración seguro.

## 🚀 Phase 38: Dual View (La Separación de Poderes)
**Versión**: v3.9.2-dev.1 | **Fecha**: 19 de Enero de 2026

### 📜 1. La Historia (El Conflicto)
Con la llegada del Zoom Hardware en la pantalla del móvil, surgió un conflicto de intereses.
El usuario ajustaba el zoom en la web para ver un detalle en su navegador (CSS), pero ese ajuste se sincronizaba con la pantalla del móvil ("El Abuelo"), haciendo que el dispositivo físico mostrara un zoom digital hardware no deseado, o viceversa.
Las necesidades eran distintas:
*   **Web**: Zoom temporal/exploratorio para ver detalles en el stream MJPEG.
*   **Hardware**: Zoom fijo/estructural para encuadrar la zona de vigilancia permanente.

### 🛠️ 2. La Solución (Ingeniería)
**Bifurcación de Preferencias**:
Rompimos el vínculo único. Ahora el servidor gestiona dos sets de coordenadas paralelos:
1.  **Hardware Vars (`defZoom`, `defPan`)**: Se inyectan al `SentinelService` para escalar el `SurfaceView`. Afectan a lo que "ve" el móvil.
2.  **Web Vars (`webZoom`, `webPan`)**: Se guardan en el móvil, pero solo se sirven al JS del navegador para aplicar transformaciones CSS (`transform: scale()`). No tocan el hardware.

**Interfaz Dividida**:
El modal de ajustes ahora refleja esta realidad con dos bloques diferenciados por color: Azul (Web) y Naranja (Hardware).

### 🚀 v3.9.2-dev.4: Refinamiento de UX y Desacople Total

Tras el despliegue inicial de la vista dual, detectamos dos fricciones que requerían intervención inmediata ("Polish"):

1.  **Desacople del Reproductor de Video**:
    *   *Problema*: El reproductor de video web (`playVideo`) inicializaba su zoom reseteando a las coordenadas `0,0` o leyendo incorrectamente las variables de hardware, lo que causaba saltos visuales.
    *   *Solución*: Implementación de variables globales en Javascript (`gWebZoom`, `gWebPanX`, `gWebPanY`). Ahora, al abrir un video, se respeta estrictamente la configuración visual definida por el usuario para la web, ignorando por completo el estado del hardware (`defZoom`).

2.  **Tendencias de Temperatura "Pegajosas" (Sticky Trends)**:
    *   *Problema*: Los indicadores de tendencia (▲/▼) desaparecían si la temperatura se mantenía estable durante 5 segundos (el ciclo de refresco), perdiendo el contexto histórico inmediato.
    *   *Solución*: Se introdujo persistencia en la variable `lastTrend`. El icono solo cambia si hay un delta de temperatura real; si la temperatura es igual a la anterior, se *mantiene* el último icono conocido. Esto permite saber si el dispositivo "viene de calentarse" o "viene de enfriarse" incluso en mesetas térmicas.

Esta versión consolida la experiencia de usuario (UX) tanto en control como en monitoreo.

### 🎨 v3.9.2-dev.6: Refinamiento Estético del Modal de Ajustes

La primera implementación funcional es solo el 50% del trabajo; la percepción del usuario es el otro 50%.
Al revisar el modal de ajustes, los inputs numéricos de `Pan X` y `Pan Y` resultaban crudos y poco intuitivos ("¿Qué es X? ¿Qué es Y?"). Además, tenían problemas de layout.

**Cambios de UI/UX:**
1.  **Semántica Visual**: Se reemplazaron las etiquetas de texto por iconos universales (`↔` para horizontal, `↕` para vertical), reduciendo la carga cognitiva.
2.  **Layout Compacto**: Se reestructuró el formulario usando `Flexbox` con inputs anidados.
3.  **Constraint de Ancho**: Se limitó el ancho de los inputs a `90px` para evitar que se expandieran desproporcionadamente en pantallas anchas, manteniendo la elegancia del modal.

*Lección*: Una interfaz técnica no tiene por qué ser fea. Un simple icono mejora la usabilidad enormemente.

### 🎨 v3.9.2-dev.10: Coherencia Numérica en UI

Se ha extendido la directiva de alineación numérica (`text-align: right`) a **todos** los campos de entrada de la aplicación, no solo los de posicionamiento Pan.
Ahora, la consistencia es total:
*   Pan X / Y (Web & Hardware) -> Derecha
*   Tiempo Grabación -> Derecha
*   Mínimo Espacio -> Derecha

Esto refuerza el modelo mental de "Operación Matemática / Ajuste Fino" frente a la introducción de texto convencional.

### 📝 v3.9.2-dev.11: Claridad Semántica en Ajustes

Siguiendo el principio de "No me hagas pensar", se ha mejorado la etiquetación de la opción de tiempo de grabación:
*   **Antes**: "Tiempo Grabación" (Ambiguo: ¿Duración máxima? ¿Intervalo?)
*   **Ahora**: "Tiempo extra de Grabación" con nota al pie: "* Segundos a grabar tras cesar el movimiento."

Esto elimina dudas sobre si la cámara corta la grabación abruptamente o tiene un *cool-down*.

### ℹ️ v3.9.2-dev.12: Democratización del Conocimiento (Tooltips Globales)

Para cerrar el ciclo de refinamiento de UX en el modal de ajustes, hemos aplicado la regla de la "Explicabilidad Total".
Cada control, por trivial que parezca para el ingeniero, ahora tiene una nota al pie para el usuario final:
*   **Detector Activado**: Que hace (vigila/graba).
*   **Sensibilidad**: Qué significa Min/Max (umbral).
*   **Min. Espacio**: Como funciona la auto-limpieza (trigger de borrado).
*   **Rotación**: Cuándo usarla (montaje físico).
*   **Zoom Físico**: Advertencia de que afecta a todo el sistema.

La interfaz ahora educa al usuario mientras la usa.

### ✏️ v3.9.2-dev.13: Precisión Lingüística en Tooltips

El usuario ha refinado el "copywriting" de los tooltips para eliminar ambigüedades técnicas y usar un lenguaje más natural:
*   **Web**: "CSS Transform" -> "Zoom y Desplazamiento (Pan) del video mostrado en el navegador".
*   **Hardware**: "Zoom real" -> "Zoom y Desplazamiento... en la pantalla del móvil".
*   **Rotación**: "Cámara física" -> "Rota la imagen si aparece al revés".

La precisión en el lenguaje es tan crítica como la precisión en el código.

### 🚀 v3.9.2: Release Oficial "El Abuelo Oculista"

Llegamos a puerto. Tras una intensa sesión de 13 iteraciones (`dev.1` -> `dev.13`), publicamos la versión **v3.9.2**.
Esta versión representa un salto cualitativo en la usabilidad del sistema:

1.  **Dual View Engine**: Desacople total entre lo que ve el navegador (Web Zoom/Pan) y lo que ve el Hardware.
2.  **UI Pro**: Un panel de ajustes rediseñado, con controles alineados, espaciados y explicados.
3.  **Pedagogía Integrada**: Tooltips que explican cada función técnica en lenguaje humano.

**Estado del Código**: Limpio, alineado a la derecha, y con gaps físicos de 20px.
**Estado del Repositorio**: Changelog detallado intacto (por inmutabilidad histórica).

### 🔭 v3.9.3-dev.1: Zoom Recursivo en Miniaturas

Iniciamos la serie `v3.9.3` con una mejora sutil pero potente: la coherencia espacial en las miniaturas.
Hasta ahora, las miniaturas eran estáticas o se movían caóticamente si se aplicaba Pan (porque recibían desplazamientos de píxeles pensados para una pantalla grande).

**La Solución Proporcional**:
Hemos implementado una matemática de escalado en `NanoHttpServer.java`:
```javascript
var tx = x * 0.25; // 80px / 320px
var ty = y * 0.25;
element.style.transform = `translate(${tx}px, ${ty}px) scale(${z})`;
```
Esto crea un efecto de "Zoom Fractal": las miniaturas muestran exactamente el mismo encuadre relativo que el vídeo principal.

### 📐 v3.9.3-dev.2: Simetría Geométrica en Miniaturas

El usuario detectó que el zoom en miniaturas desplazaba la imagen incorrectamente ("hacia arriba a la izquierda").
**El Diagnóstico**:
Mientras el video player escalaba desde la esquina superior izquierda (`transform-origin: 0 0`) y usaba `contain`, las miniaturas escalaban desde el centro (`transform-origin: 50% 50%`) y usaban `cover`.
Esto provocaba que las coordenadas de traducción no coincidieran.

**La Solución**:
Hemos igualado la física de ambos elementos:
`css
.thumb, .mini-canvas {
    object-fit: contain;     /* Coherencia de encuadre */
    transform-origin: 0 0;   /* Coherencia de coordenadas */
}
`
Ahora la matemática del zoom es universal.

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

### 🚀 Phase 22: Geometría Proporcional en Miniaturas (v3.9.3-dev.4) | Fecha: 19 de Enero de 2026

**El Problema (Storytelling) 📜**
Las miniaturas en el dashboard web heredaban el desplazamiento (`translate X, Y`) pensado para la pantalla grande/modal. Si el usuario desplazaba el video 300 píxeles a la derecha en el modo pantalla completa para ver un detalle, las miniaturas de apenas 80 píxeles también se movían 300 píxeles, saliéndose completamente de su contenedor y dejando un hueco negro. Era como intentar aplicar las coordenadas de atraque de un transatlántico a un bote de remos.

**La Solución (Ingeniería) 🛠️**
Implementamos **Matemática Proporcional** en la función `updateWebTransform`.
1.  **Cálculo del Ratio**: Determinamos qué fracción de la pantalla representa la miniatura.
    `ratio = 80.0 / window.innerWidth`
2.  **Escalado Diferencial**:
    *   **Video Grande**: Recibe el desplazamiento crudo (`x`, `y`).
    *   **Miniaturas**: Reciben el desplazamiento escalado (`x * ratio`, `y * ratio`).
3.  **Compatibilidad Legacy**: Reemplazamos `forEach` por bucles `for(;;)` tradicionales para asegurar que el Javascript funcione incluso en navegadores antiguos de tablets o móviles viejos que se usen como consolas de monitoreo.

**Lecciones Aprendidas 🎓**
*   **Contexto de Escala**: Nunca compartir coordenadas absolutas (píxeles) entre elementos de diferente tamaño sin un factor de normalización.
*   **Defensive Coding**: Siempre asumir que `window.innerWidth` puede ser muy pequeño o nulo, estableciendo mínimos de seguridad (320px).
### 🚀 Phase 23: Coherencia Visual Miniatura `canvas` (v3.9.3-dev.5) | Fecha: 19 de Enero de 2026

**El Problema (Storytelling) 📜**
El player principal tenía un comportamiento `object-fit: contain`. Las miniaturas estáticas (`img`) también. Pero... ¡el `canvas` de la previsualización en vivo (el famoso /preview_) se había quedado desnudo!
Sin `transform-origin: 0 0` ni `object-fit: contain`, el canvas reaccionaba al escalado (Zoom) creciendo desde su centro en lugar de desde la esquina superior izquierda, y estirando la imagen de forma diferente a sus hermanos estáticos.
Esto provocaba que, al aplicar Zoom/Pan, el preview animado se desencajase visualmente del marco negro, rompiendo la ilusión de solidez.

**La Solución (Ingeniería) 🛠️**
Hemos alineado la física CSS del `.mini-canvas` con la del player principal:
```css
.mini-canvas {
    transform-origin: 0 0;  /* Anclaje idéntico al Player */
    object-fit: contain;    /* Geometría idéntica al Player */
}
```
Ahora, matemáticas y píxeles bailan al unísono.
### 🚀 Phase 24: Migración a Unidades Relativas - % vs px (v3.9.3-dev.6) | Fecha: 19 de Enero de 2026

**El Problema (Storytelling) 📜**
Nos enfrentábamos a un problema de **incoherencia espacial**. Cuando el usuario configuraba un Pan (desplazamiento) de "50" para centrar una puerta en el monitor, ese valor en píxeles (`50px`) era insignificante para la pantalla grande, pero "sacaba de cuadro" a la miniatura pequeña de 80px.
Intentamos arreglarlo con ratios matemáticos complicados basados en `window.innerWidth`, pero fallaban porque no consideraban el "aire" (bandas negras) del contenedor.

**La Solución (Ingeniería) 🛠️**
La solución definitiva fue cambiar el paradigma de coordenadas:
*   **Antes (Absoluto)**: `translate(50px, 0)`. Dependiente de la resolución.
*   **Ahora (Relativo)**: `translate(10%, 0)`. Independiente de la resolución.
    *   Le decimos al navegador: *"Mueve la imagen un 10% de su ancho"*.
    *   En el Monitor (1000px), se mueve 100px.
    *   En la Miniatura (80px), se mueve 8px.
*   **Resultado**: Un solo valor de configuración (`Pan X: 10%`) produce un encuadre visualmente idéntico en todas las pantallas del sistema, sin importar su tamaño.

### 🚀 Phase 25: Homogeneización Interactiva - Touch Px a % (v3.9.3-dev.7) | Fecha: 19 de Enero de 2026

**El Problema (Storytelling) 📜**
Teníamos el sistema migrado a porcentajes (`%`) para que fuera responsive, pero detectamos un **bug crítico**: una línea perdida de código antiguo en la función táctil (`updateTransform`) seguía inyectando píxeles (`px`).
Esto "jodía la marrana": al aplicar el pan por defecto a miniaturas y video grande, el sistema dejaba de usar la lógica relativa y volvía al comportamiento absoluto errático, rompiendo la consistencia que acabábamos de arreglar.

**La Solución (Ingeniería) 🛠️**
Localizamos y corregimos la línea culpable. Cambiamos la concatenación de strings para usar `%` en lugar de `px`.
Asunto arreglado.

## 🚀 Phase 26: Release Universum (v3.9.3)
**Versión**: v3.9.3 | **Fecha**: 19 de Enero de 2026

### 📜 1. La Historia (El Cierre)
Durante el ciclo `v3.9.3-dev`, nos enfrentamos a un desafío geométrico fundamental: **El Principio de Relatividad Visual**.
Al querer encuadrar un objeto (ej: una puerta) en el monitor, usábamos distancias absolutas ("50 pasos"). Pero en la miniatura de la interfaz, "50 pasos" significaba salirse del mapa.
Intentamos parches matemáticos (Ratios), pero fracasaron ante la realidad de los contenedores web (`img vs canvas`, `contain vs cover`).

La solución final no fue matemática, fue conceptual: dejar de hablar en pasos (píxeles) y empezar a hablar en proporciones (porcentajes).

### 🛠️ 2. Resumen de Logros (Ingeniería)
1.  **Unificación CSS (`dev.5`)**: Miniaturas estáticas y Canvas animados ahora comparten `object-fit: contain` y `transform-origin` idénticos.
2.  **Relatividad Universal (`dev.6`)**: Migración de todo el motor de posicionamiento web `updateWebTransform` de Píxeles (`px`) a Porcentajes (`%`).
3.  **Consistencia Táctil (`dev.7`)**: Corrección del motor `touchmove` para que use el nuevo estándar relativo.

**Resultado**: Un sistema de coordenadas **Agnóstico de Resolución**. El mismo valor numérico (`Pan X: 20%`) produce el mismo encuadre visual idéntico en un monitor 4K, en un móvil y en una miniatura de 80px.

### 🚀 Phase 39: UI Hot-Swap - Zero Reload (v3.9.4-dev.1) | Fecha: 20 de Enero de 2026

**El Problema (Storytelling) 📜**
Cada vez que el Abuelo terminaba de grabar un vídeo, la interfaz web hacía un `location.reload()` completo ("El Cebollazo") para mostrar la nueva miniatura en la lista.
Esto era ineficiente (recarga de scripts, CSS, reconexión de sockets) y visualmente tosco (parpadeo blanco).
Queríamos que la tarjeta roja de "🔴 GRABANDO..." se transformara mágicamente en la tarjeta final del vídeo sin tocar el resto de la página.

**La Solución (Ingeniería) 🛠️**
Hemos implementado una cirugía plástica en el DOM usando **Javascript Puro (Client-Side)**.
1.  **Interceptación**: Cuando empieza a grabar, capturamos el nombre del archivo (`gCurrentRecFilename`) que ya viaja en la API `latest_video_meta`.
2.  **Cronómetro Local**: Guardamos `Date.now()` para calcular luego la duración exacta sin preguntar al servidor.
3.  **Transmutación**: Al terminar (evento `recording: false`), en vez de recargar:
    *   Hacemos un `HEAD` request ligero para saber el tamaño en MB.
    *   Sustituimos el HTML de la tarjeta temporal por el de una tarjeta estándar.
    *   Le inyectamos la miniatura `.thumb` que el servidor acaba de guardar.
    *   Aplicamos el Zoom/Pan configurado a la nueva imagen.

**Resultado**: El usuario ve cómo la grabación se detiene y aparecen los botones de "Ver/Borrar" instantáneamente. Cero tráfico extra. Cero parpadeos. ⚡

### 🧹 Phase 39.1: Refinamiento Visual (v3.9.4-dev.2)
**El Ajuste**: El usuario detectó que añadimos botones "Ver/Borrar" superfluos que rompían el lenguaje de diseño minimalista original.
**La Solución**: Hemos alineado la función `finalizeRecordingCard` (JS) para generar HTML **idéntico estructuralmente** al que genera Java en el servidor. Usamos un contenedor `div.video-item` limpio, con el trigger `onclick` global y sin controles redundantes. Mantenemos la consistencia visual absoluta. 🎨

### 🏥 Phase 39.2: JS Syntax Recovery (v3.9.4-dev.3)
**El Fallo**: La edición manual de texto "Java dentro de JS" provocó un `ReferenceError` catastrófico (función descabezada) que rompió toda la interactividad de la web.
**La Cura**: Hemos realizado una micro-cirugía con un script Python para reinsertar la cabecera de `finalizeRecordingCard` perdida. La web vuelve a responder. 🩹

### 🚦 Phase 39.3: Race Condition (v3.9.4-dev.4)
**El Bug**: La tarjeta "Grabando..." no aparecía.
**El Diagnóstico**: Condición de carrera. El servicio notificaba `statusLock.notifyAll()` (diciendole al cliente que graba) *antes* de que `openNewRecordingFile()` creara el archivo en disco. El cliente pedía el nombre del archivo (`/api/latest_video_meta`) y recibía `null`, abortando la creación de la tarjeta.
**La Solución**: Reordenamiento atómico en `SentinelService.java`. Primero creamos el archivo, luego notificamos. Orden restaurado. ⚖️

### 🧩 Phase 39.4: The Identity Crisis (v3.9.4-dev.5)
**El Bug**: Al terminar la grabación, la tarjeta mostraba "0.1 MB" y un thumbnail roto.
**El Diagnóstico**: El cliente guardaba el nombre original (`video.mjpeg`) pero el servidor, al cerrar el archivo, lo renombra inteligentemente (`video_15fps.mjpeg`). El JS intentaba cargar el archivo antiguo.
**La Solución**: Hemos actualizado `NanoHttpServer` (JS) para que sea humilde: al terminar, no asume nada, sino que pregunta al servidor (`/api/latest_video_meta`) cuál es el nombre *final* del archivo. Sincronización completa. 🤝

### 🎬 Phase 39.5: The Missing Soul (v3.9.4-dev.6)
**El Bug**: La tarjeta se creaba perfecta... pero estática. La miniatura no se movía.
**El Diagnóstico**: El Javascript inyectaba la imagen (`<img>`) pero olvidaba el `<canvas>`, que es el motor de la animación. Además, el archivo de previsualización no tiene el sufijo `_fps`, lo cual confundía al script.
**La Solución**: Hemos enseñado al JS a deducir el nombre del archivo de previsualización (quitando los `_fps` del nombre del video) e inyectar el código del `<canvas>` necesario para invocar al espíritu del movimiento. 👻📽️

### 📏 Phase 39.6: The Polite Server (v3.9.4-dev.7)
**El Bug**: El tamaño del archivo se reportaba casi siempre como "0.1 MB".
**El Diagnóstico**: El servidor Java era un bárbaro. Cuando el cliente pedía solo el tamaño (`HEAD`), el servidor le vomitaba el video entero (`GET`), saturando la conexión y cortando la respuesta antes de tiempo.
**La Solución**: Hemos implementado modales HTTP en `NanoHttpServer`. Ahora sabe distinguir `HEAD` (solo cabeceras) de `GET` (cuerpo entero), permitiendo verificaciones de peso instantáneas y precisas. 🎩

### 🔗 Phase 39.7: The Wrong Address (v3.9.4-dev.8)
**El Bug**: El tamaño seguía mostrando "0.1 MB" a pesar del soporte HEAD.
**El Diagnóstico**: El JS pedía el archivo a `/videos/video_...`, pero el servidor solo escucha en `/video_...`. La petición se iba al Dashboard en lugar de al manejador de archivos.
**La Solución**: Corregida la URL en el JS para usar el path correcto (`'/' + filename`). Ahora la petición HEAD llega al destino correcto. 🏠

---

## 🏁 Phase 39: RELEASE v3.9.4 | 20 de Enero de 2026

### 📜 El Problema (Storytelling)
Cada vez que finalizaba una grabación, la web recargaba completamente, perdiendo el estado del UI y generando una experiencia brusca. El usuario veía un destello blanco, la lista de videos se reconstruía desde cero, y el flujo se sentía desconectado.

### 🛠️ La Solución (Ingeniería)
Implementamos un sistema de "Hot-Swap" completamente client-side:

1. **Estado Global**: Variables JS (`gCurrentRecFilename`, `gRecStartTime`) capturan metadatos al inicio de grabación.
2. **Polling Inteligente**: `pollStatus` detecta transiciones de estado (idle→recording→idle).
3. **Tarjeta Temporal**: `injectLivePreview` inserta una tarjeta roja con preview en vivo durante la grabación.
4. **Metamorfosis**: `finalizeRecordingCard` transforma la tarjeta roja en permanente sin recarga, obteniendo el nombre final del archivo (con FPS) y su tamaño real via HEAD request.
5. **Animación**: Se inyecta un `<canvas>` y se invoca `loadMiniPreview` para activar la miniatura animada inmediatamente.

### 🐛 Bugs Aplastados en el Camino
- **Race Condition Start**: `openNewRecordingFile` antes de `notifyAll`.
- **Race Condition Stop**: `closeRecordingFile` (rename) antes de `notifyAll`.
- **JS Syntax Error**: Función descabezada por edición multi-lenguaje.
- **Filename Mismatch**: Cliente usaba nombre sin FPS, servidor renombraba con FPS.
- **Missing Canvas**: JS solo inyectaba `<img>`, faltaba el motor de animación.
- **HEAD Protocol**: Servidor ignoraba método HEAD, enviaba body completo.
- **Wrong URL**: JS pedía `/videos/...`, servidor escuchaba en `/video_...`.

### 🎓 Lecciones Aprendidas
- El código Java-embedded-JS es extremadamente frágil. Los scripts Python de parche quirúrgico son la solución más segura.
- El orden de operaciones en código concurrente es crítico. Siempre preparar recursos ANTES de notificar.
- HTTP HEAD existe por una razón. Implementarlo correctamente ahorra ancho de banda y evita bugs sutiles.


### ⚡ v3.9.5-dev.1: Lazy Load (Infinite Scroll)

El usuario reportó saturación de CPU al generar la lista HTML de cientos de videos.
**La Solución**:
Implementación de carga diferida (Lazy Load):
1.  **Backend**: Nueva API `/api/list_videos` que pagina los resultados y extrae metadatos solo de los nombres de archivo (Regex), evitando I/O costoso.
2.  **Frontend**: Carga inicial vacía + `IntersectionObserver` que pide bloques de 10 videos al hacer scroll.
3.  **Javascript**: Lógica de renderizado de tarjetas en cliente y fix de compatibilidad de clicks.

Resultado: Carga inicial instantánea y navegación fluida.

### 🧪 v3.9.5-dev.2: Estabilización de SurfaceView (Safe Mode)

Se detectaron crashes recurrentes al intentar montar la `SurfaceView` en el activity principal (`MainActivity`) mientras el servicio `SentinelService` estaba grabando en background.

**Hipótesis del Fallo**:
Al llamar a `setPreviewSurface`, la implementación anterior destruía y recreaba agresivamente los buffers (`addCallbackBuffer`) y reiniciaba los callbacks. En dispositivos antiguos (Galaxy S, Android 2.3), alterar la cadena de buffers mientras la cámara está entregando frames causa una excepción nativa o un bloqueo del HAL de cámara.

**La Solución (Safe Mode Hook)**:
Hemos reescrito `setPreviewSurface` con un enfoque conservador:
1.  **Stop Atómico**: Detener preview solo lo justo para cambiar el `Display`.
2.  **Persistencia de Buffers**: NO tocamos `setPreviewCallbackWithBuffer` ni los buffers ya asignados. La cadena de memoria se mantiene intacta.
3.  **Fallback Background**: Si `holder` es null, intentamos volver a `dummySurface` (SurfaceTexture) en lugar de dejar el display en null, para asegurar que la grabación continúe.
4.  **Chispa de Arranque**: Solo inyectamos un nuevo buffer si es estrictamente necesario para "desatascar" la pipeline.
5.  **Corrección de Orientación**: Se ajustó `setDisplayOrientation(90)` que parece ser el valor correcto para este dispositivo en modo Portrait/Landscape híbrido, en lugar del 180 anterior.

Este cambio busca que la transición UI <-> Servicio sea transparente para el hardware de la cámara.

### ❌ Intento Fallido (v3.9.5-dev.2): El Límite de la Historia (API 10)

Intentamos modernizar la gestión de superficies usando `setPreviewTexture` como fallback elegante en lugar de anular el display (`null`).
**El Resultado**: Crash inmediato de `MainActivity` al arrancar.
**La Causa (Arqueología)**:
El método `setPreviewTexture(SurfaceTexture)` fue introducido en Android 3.0 (**API 11**).
Nuestro hardware objetivo (Galaxy S GT-I9000) corre Android 2.3.6 (**API 10**).
El sistema lanzó `java.lang.NoSuchMethodError`.

### ⚡ v3.9.5-dev.3: Regreso al Pasado

Hemos revertido la lógica de "fallback" para ser estrictamente compatible con API 10:
*   **Antes (dev.2)**: `if (bg) setPreviewTexture(...)` 💥
*   **Ahora (dev.3)**: `if (bg) setPreviewDisplay(null)` ✅
Esto permite que la cámara siga capturando frames en background (gracias al truco de los buffers) sin que la `SurfaceView` UI intente llamar a métodos inexistentes.

### ⚡ v3.9.5-dev.4: Restauración de Estabilidad (Legacy Protocol)

Tras los fallos de las versiones `dev.2` (Safe Mode) y `dev.3` (Partial Fix), hemos tomado una decisión radical pero segura: **Regresar al protocolo de cámara de la v3.9.4**.

**El Hallazgo**:
Aunque el "Safe Mode" de `dev.2` pretendía ser más ligero al no tocar los buffers, el hardware del Galaxy S (Android 2.3) es extremadamente sensible al cambio de contexto. Al detener el preview para cambiar el Display, el motor de cámara parece corromper la cadena de callbacks si estos no se limpian y recrean desde cero.

**Cambios Aplicados**:
1.  **Recreación de Buffers**: Volvemos a limpiar (`callback = null`) y asignar 3 buffers NV21 nuevos en cada cambio de superficie.
2.  **Display Orientation**: Restaurado a `180` grados, confirmando que es el valor estable para esta montura.
3.  **Orden Síncrono**: Se sigue estrictamente el orden: Stop -> Display -> Buffers Re-init -> Start.

Esta versión recupera la estabilidad que permitía a la `MainActivity` convivir con el servicio en background sin cerrarse.
## 🚀 Phase 23: Lazy Load & Legacy Stability Fix (v3.9.5) | Fecha: 21 de Enero de 2026

### 📜 1. La Historia (El Problema)
Teníamos dos objetivos en paralelo:
1.  **Rendimiento Web**: El dashboard generaba TODA la lista de videos en el HTML inicial, causando tiempos de carga lentos y saturación de CPU.
2.  **Estabilidad Legacy**: Tras optimizaciones de renderizado, la `MainActivity` crasheaba al abrirse, aunque el servicio seguía funcionando en background.

### 🛠️ 2. La Solución (Ingeniería)

#### A) Lazy Load (Infinite Scroll)
Implementamos un sistema de carga diferida completo:
*   **Backend (Java)**: Nuevo endpoint `/api/list_videos?offset=N&limit=M` que devuelve metadatos en JSON. Usa Regex sobre nombres de archivo para extraer FPS y duración sin abrir los videos.
*   **Frontend (JavaScript)**: `IntersectionObserver` detecta cuando el usuario llega al final de la lista y pide el siguiente bloque de 10 videos.
*   **Fix Clicks**: Usamos `setAttribute('onclick', ...)` en lugar de closures para compatibilidad con WebViews antiguos.

#### B) Arqueología del Crash (5 Intentos Documentados)
| Versión | Hipótesis | Resultado |
|---------|-----------|-----------|
| dev.2 | "Safe Mode" sin tocar buffers | ❌ Crash |
| dev.3 | Revertir `setPreviewTexture` (API 11) | ❌ Crash |
| dev.4 | Restaurar lógica completa v3.9.4 | ❌ Crash |
| dev.5 | Restaurar `package` en Manifest | ✅ Funciona |

**La Causa Raíz**: Al eliminar el atributo `package="com.elojodelabuelo"` del `AndroidManifest.xml` para "limpiar" warnings de Gradle, rompimos la resolución de clases en el runtime de Android Legacy. Aunque Gradle moderno no lo necesita, el dispositivo objetivo (Android 2.3/4.x con CyanogenMod) sí lo requería.

### 🎓 3. Lecciones Aprendidas
*   **Manifest Legacy**: El atributo `package` en el `<manifest>` NO es redundante en dispositivos antiguos. Es la fuente de verdad para el ClassLoader del runtime.
*   **Documentación de Fallos**: Anotar cada intento fallido en la Bitácora nos permitió descartar hipótesis rápidamente y encontrar la causa real.
*   **Lazy Load**: El patrón de paginación + `IntersectionObserver` es universalmente superior a renderizar listas completas.

### 📖 4. Glosario
*   **IntersectionObserver**: API de JavaScript que detecta cuándo un elemento entra o sale del viewport.
*   **HAL**: Hardware Abstraction Layer. Capa de drivers que comunica el SO con el hardware de cámara.
*   **Manifest package**: Atributo XML que define el namespace de la aplicación para el sistema Android.

### 🛡️ v3.9.5-dev.6: Blindaje del AndroidManifest (Meta-Ingeniería)

Tras una investigación exhaustiva comparando v3.9.2 (funcionaba) con v3.9.5 (crasheaba), descubrimos que **la liturgia de cámara era idéntica**. El problema estaba en el `AndroidManifest.xml`.

**El Hallazgo**:
Durante optimizaciones anteriores, eliminamos los atributos `android:versionCode` y `android:versionName` del Manifest porque Gradle los ignora. Sin embargo, el runtime de Android 2.3/4.x (CyanogenMod en el Galaxy S GT-I9000) **requiere estos campos** para resolver correctamente las Activities mediante el ClassLoader.

**La Solución**:
1.  Restauramos el `AndroidManifest.xml` exactamente como estaba en v3.9.2.
2.  Creamos la **Regla 10** en `legacy_dev_rules.md` para blindar estos atributos:
    *   🚫 NUNCA eliminar `android:versionCode` ni `android:versionName`
    *   Mantenerlos como valores fijos legacy (`1` y `3.2.1-debug20d`)
    *   El versionado real sigue en `build.gradle`

**Lecciones Aprendidas**:
*   Los dispositivos legacy tienen dependencias ocultas en campos que builds modernos ignoran.
*   Documentar restricciones de hardware en las reglas del agente previene regresiones futuras.

### 📝 v3.9.5-dev.7: Estandarización de Reportes (Meta-Ingeniería)

El usuario validó un nuevo formato de reporte final para el "Protocolo de Cierre Cuaternario", inspirado en la claridad visual de Claude Opus 4.5.

**El Cambio**:
Actualizamos la **Regla 7** en `legacy_dev_rules.md` para hacer MANDATORIO el uso de una tabla de verificación de 6 puntos al finalizar cualquier cambio versionado:
1. Versión
2. Bitácora
3. Changelog
4. Commit & Tag
5. Push
6. Git Status

Esto asegura consistencia visual y psicólogica tanto para el usuario como para el agente.

### 🧪 v3.9.5-dev.8: Validación de Hipótesis (Manifest versionName)

Probamos cambiar `android:versionName` de `"3.2.1-debug20d"` a `"legacy-compat"` para confirmar nuestra hipótesis:
*   **Hipótesis**: El ClassLoader de Android Legacy necesita que los atributos `versionCode` y `versionName` EXISTAN, pero no le importa su VALOR.
*   **Resultado**: ✅ Confirmado. La Activity se mantiene abierta con el nuevo valor.

Este cambio estandariza el Manifest con un nombre más semántico (`legacy-compat`) que comunica claramente su propósito: compatibilidad con dispositivos antiguos.

### 📋 v3.9.5-dev.9: Creación de BACKLOG.md (Gestión de Proyecto)

Implementamos un sistema estructurado de gestión de tareas pendientes, bugs y mejoras técnicas.

**El Archivo**:
Creado `BACKLOG.md` con:
*   **Estructura por prioridades** (P0/P1/P2)
*   **Categorías** (Bugs, Features, Tech Debt)
*   **Sistema de IDs** (B###, F###, T###)
*   **Documentación detallada** de cada item (síntoma, contexto, impacto, archivos relacionados)

**Items Iniciales Registrados**:
*   **[B001]**: Click events dejan de funcionar tras crear tarjeta AJAX
*   **[B002]**: Modal de stream en vivo no aplica zoom/pan del navegador
*   **[B003]**: Regresión de temperatura (42°C vs 38-39°C en v3.9.2)
*   **[F001]**: Controles táctiles interactivos en modal de stream en vivo

Este documento será la fuente de verdad para planificar el desarrollo futuro.

### 📐 v3.9.5-dev.10: Templates de BACKLOG (Meta-Ingeniería)

Codificamos los estándares de calidad para la documentación de bugs, features y mejoras técnicas.

**El Cambio**:
Añadida **Regla 11** en `legacy_dev_rules.md` que define:
*   **Templates obligatorios** para cada tipo de item (B###, F###, T###)
*   **Campos requeridos** (Síntoma, Impacto, Archivos Relacionados, etc.)
*   **Workflow de actualización** (consulta proactiva, marcar completados)

**Beneficio**:
El agente ahora tiene una "plantilla mental" que garantiza que todos los items del BACKLOG tengan el mismo nivel de detalle y trazabilidad, independientemente de quién los añada o cuándo.

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

### 📋 v3.9.5-dev.12: Migración a GitHub Issues (Meta-Ingeniería)

Transición completa del sistema de gestión de tareas de BACKLOG.md local a GitHub Issues nativo.

**El Cambio**:
*   **Regla 11 Actualizada**: Ahora los agentes deben usar `gh` CLI para consultar (`gh issue list`), crear (`gh issue create`) y cerrar (`gh issue close`) issues.
*   **Templates Estandarizados**: Documentados en la regla para bugs (label: bug) y features (label: enhancement).
*   **Migración Completa**: Las 7 issues del BACKLOG.md fueron creadas en GitHub:
    *   **Abiertas**: #1 (B001), #2 (B002), #3 (B004), #4 (F001)
    *   **Cerradas**: #5 (B003), #6 (B000), #7 (F000)
*   **BACKLOG.md**: Archivado como `.archived` y restaurado para referencia local opcional.

**Beneficio**:
Centralización en GitHub Issues permite mejor trazabilidad, integración con commits (ej: `Fixes #3`), y colaboración más fluida. Los agentes ahora tienen una fuente de verdad única y sincronizada.

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
