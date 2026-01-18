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
**Lección**: Un sistema de seguridad que solo funciona "a veces" no es seguro. La cobertura debe ser universal, independientemente del modo de operación.

