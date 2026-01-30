# Changelog

All notable changes to the "El Ojo Del Abuelo" project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [v3.9.7] - 2026-01-30
### Summary
"The Sharpness Update". Actualización mayor enfocada en la calidad del OSD y la usabilidad del editor.

### Highlights
- **OSD White Pro**: Texto blanco puro ultra-nítido con fondo semitransparente.
- **Live Preview CIF**: Editor WYSIWYG real.
- **Persistence**: Memoria de configuración.
- **Stability**: ADB Watchdog y mejoras térmicas.

## [v3.9.7-dev.50] - 2026-01-30
### Changed
- **OSD Style**: Cambio de estilo a "White Pro Mode".
    - Texto ahora es Blanco Puro (Luma 255, Chroma Neutra) para máxima nitidez a tamaños pequeños.
    - Se mantiene el fondo semitransparente para alto contraste.

## [v3.9.7-dev.49] - 2026-01-29
### Added
- **OSD Persistence**: La posición (X, Y) del texto ahora se guarda en memoria permanente y se restaura al reiniciar la aplicación.

## [v3.9.7-dev.48] - 2026-01-29
### Added
- **OSD Legibility**: Añadido fondo semitransparente automático tras el texto.
    - Implementación mediante sustracción de luminancia (Bit Shifting) para oscurecer el video tras las letras sin coste de CPU.

## [v3.9.7-dev.47] - 2026-01-29
### Fixed
- **OSD Graphics**: Corregido corte inferior en el texto de fecha/hora.
    - Altura del canvas aumentada de 1.5x a 2.0x.
    - Coordenada de pintado dinámica para soportar fuentes grandes sin salirse del margen.

## [v3.9.7-dev.46] - 2026-01-29
### Added
- **OSD Editor**: Implementada "Vista en Vivo" (Live Preview).
    - El fondo del editor ahora muestra el stream MJPEG de la cámara.
    - Geometría forzada a ratio CIF (352/288) para garantizar precisión WYSIWYG.

## [v3.9.7-dev.45] - 2026-01-29
### Improved
- **OSD Graphics**: Mejora drástica de nitidez en el texto sobreimpreso.
    - Implementado umbral de canal Alpha (> 50%) para eliminar bordes borrosos.
    - Aumentado brillo del texto (Luma 200).
    - Forzado de color Chroma sólido para eliminar artefactos "fantasmas" en la rejilla YUV.

## [v3.9.7-dev.44] - 2026-01-29
### Fixed
- **NanoHttpServer**: Corregido bug crítico de parsing en `/api/set_osd`. Se usaba la línea de petición cruda en lugar de la URI, provocando que el último parámetro incluyera el protocolo `HTTP/1.1` y fallara la conversión numérica.

## [v3.9.7-dev.43] - 2026-01-29
### Debugging
- **OSD**: Añadido log verbose (`DEBUG OSD: ...`) en endpoint de guardado para diagnosticar error de parámetros `null/NaN`.

## [v3.9.7-dev.42] - 2026-01-28
### Added
- **OSD**: Control de tamaño de texto dinámico.
    - **Backend**: Implementado `OSD_TEXT_SIZE` (default 12px) y persistencia en `SentinelService`.
    - **Frontend**: Nuevo deslizador (10-100px) en `WebOsdEditor.java` con previsualización en tiempo real.
    - **Api**: Soporte para parámetro `&size=NN` en `/api/set_osd`.
### Changed
- **Web UI**: Eliminado el borde verde de depuración en el editor OSD para una visualización más limpia.

## [v3.9.7-dev.41] - 2026-01-28
### Improved
- **UI UX Web**: Afinado el estilo del botón de cierre [X] en `/api/debug`.
    - **Forma Cuadrada**: Eliminado `border-radius:50%` para una estética más técnica y alineada con los botones de Android nativo antiguo.
    - **Alineación Vertical**: Ajustado `margin-top: 5px` (resetando el margen general a 0) para alinearlo visualmente con el texto del encabezado `ADMIN PANEL` (H2).

## [v3.9.7-dev.40] - 2026-01-28
### Changed
- **UI UX Web**: Ajuste de posición del botón de cierre [X] en `/api/debug`.
    - Antes: `position: fixed` (siempre visible).
    - Ahora: `float: right` (flujo normal).
    - *Objetivo*: Evitar que el botón tape contenido importante en pantallas muy pequeñas al hacer zoom o scroll, moviéndose solidariamente con el resto de la página.

## [v3.9.7-dev.39] - 2026-01-28
### Added
- **UI UX Web**: Mejora en el flujo de la pestaña de Debug.
    - **Apertura Controlada**: El enlace del dashboard ahora usa `window.open` (JS) en lugar de un enlace HTML puro. Esto permite que la nueva pestaña sea "hija" del script.
    - **Botón de Cierre**: Añadido un botón flotante [X] en la esquina superior derecha de `/api/debug` que ejecuta `window.close()`. Funciona gracias al cambio anterior en la apertura.
    - *Objetivo*: Cerrar la herramienta de diagnóstico y volver al dashboard cómodamente desde el móvil sin tener que gestionar pestañas del navegador manualmente.

## [v3.9.7-dev.38] - 2026-01-28
### Improved
- **ADB Watchdog (Smart Probe)**: Reemplazo de la detección basada en `netstat` por una basada en `Socket Real`.
    - Antes: Comprobaba si el puerto estaba "recibiendo" (LISTEN), lo que daba falsos positivos con procesos zombis.
    - Ahora: Intenta abrir un Socket TCP real a `localhost:5555` y enviar bytes. Si falla o timeout (2s), considera el servicio muerto y lo reinicia.
    - *Objetivo*: Detectar y resolver bloqueos silenciosos del ADB sin intervención humana.

## [v3.9.7-dev.37] - 2026-01-28
### Added
- **UI UX Web**: Integración de un acceso directo a "Panel de Debug" en la configuración web.
    - Ubicado estratégicamente en el footer del modal de ajustes (Opción B: Discreta).
    - Permite acceso rápido a `/api/debug` sin escribir URL manual.
    - *Objetivo*: Facilitar el diagnóstico técnico in-situ desde el propio dashboard.

## [v3.9.7-dev.36] - 2026-01-28
### Added
- **Diagnostic Upgrade (Root)**: Mejoras en el endpoint `/api/restart_adb`.
    - Ahora captura el `stderr` (salida de error) del comando root.
    - Reporta el código de salida exacto (`Exit Code`) en el log web.
    - *Objetivo*: Diagnosticar por qué fallan los reinicios de ADB cuando el comando se ejecuta pero no tiene efecto (ej: "Permission denied" o binario ocupado).

## [v3.9.7-dev.35] - 2026-01-28
### Improved
- **Diagnostic Upgrade**: Aumentada la capacidad del buffer de logs en memoria para `/api/debug`.
    - Límite anterior: 50 entradas.
    - Nuevo límite: **200 entradas**.
    - *Objetivo*: Permitir un diagnóstico más profundo sin perder el historial reciente, aprovechando el ajuste de fuente (14px) que permite visualizar más líneas por pantalla.

## [v3.9.7-dev.34] - 2026-01-28
### Fixed
- **Mobile Responsive (OOB)**: Corregido el problema de escalado de texto en la interfaz de debug (`/api/debug`) al rotar el dispositivo.
    - Added `initial-scale=1.0` al meta viewport para fijar el zoom.
    - Added `-webkit-text-size-adjust: 100%` al CSS para desactivar el "hinchado" automático de texto en iOS horizontal.

## [v3.9.7-dev.33] - 2026-01-28
### Changed
- **Configuración OOB**: Ajuste fino de tipografía en `/api/debug`.
    - Reducción de fuente de `18px` a `14px`.
    - *Razón*: 18px resultaba excesivo y reducía la densidad de información en pantalla (poca historia visible sin scroll). 14px es el punto dulce entre legibilidad y densidad.

## [v3.9.7-dev.32] - 2026-01-28
### Changed
- **Configuración OOB**: Modificación cosmética en `/api/debug` para aumentar la legibilidad de los logs del sistema.
    - Se incrementó el tamaño de fuente global a `18px` y botones a `16px`.
    - *Objetivo*: Facilitar la lectura de logs en pantallas móviles pequeñas durante operaciones de rescate.

## [v3.9.7-dev.31] - 2026-01-28
### Added
- **ADB Panic Button**: Implementación de un "Botón de Pánico" en la interfaz oculta de debug (`/api/debug`).
    - *Funcionalidad*: Permite reiniciar el servicio ADB (`adbd`) mediante un comando root (`su -c ...`) ejecutado desde el servidor web.
    - *Objetivo*: Recuperar la conectividad de depuración cuando el puerto 5555 entra en estado "zombi" sin necesidad de acceso físico al dispositivo.
    - *UX*: Acceso restringido y advertencia visual ("⚠️ REINICIAR SERVICIO ADB").

## [v3.9.7-dev.30] - 2026-01-28
### Meta-Engineering
- **Workflow Refactor**: Eliminado el tagging automático en snapshots de desarrollo (`deploy_snapshot.md`).
    - Las versiones `-dev.N` ya no generarán tags en GitHub.
    - Los Tags se reservan exclusivamente para versiones estables (`release_version.md`).

## [v3.9.7-dev.29] - 2026-01-28
### Meta-Engineering
- **Workflow Upgrade (Git Hygiene)**: Modificado `deploy_snapshot.md` para prevenir la contaminación del repositorio con archivos temporales.
    - Implementado `git reset commit_msg.txt` antes del commit para excluir el mensaje del staging area.
    - *Objetivo*: Eliminar el estado "deleted but not staged" y los commits de limpieza posteriores.

## [v3.9.7-dev.28] - 2026-01-28
### Meta-Engineering
- **Rules Refactor**: Simplificación del Reporte Final en Regla 7 (`legacy_dev_rules.md`).
    - Fusionado "Commit" y "Push" en un solo ítem.
    - Reducción de 8 a **7 Puntos de Verificación**.
- **Workflow Update**: Actualizado `deploy_snapshot.md` para reflejar la estructura de 7 puntos.

## [v3.9.7-dev.27] - 2026-01-28
### Meta-Engineering
- **Rules Upgrade**: Actualizada Regla 7 en `legacy_dev_rules.md` para hacer **obligatorio** el "Reporte de Incidentes y Resoluciones" en el cierre de tareas.
- **Workflow Upgrade**: Modificado `deploy_snapshot.md` para incluir la sección de Reporte de Incidentes en la estructura de salida.
    - *Objetivo*: Garantizar trazabilidad total de los errores intermedios y sus correcciones.

## [v3.9.7-dev.26] - 2026-01-28
### Added
- **Web OSD Editor**: Implementación de un Editor OSD modular para configurar la posición de la fecha/hora en la imagen.
    - **Back-end Lightweight**: Gestión de coordenadas mediante variables volátiles (`SentinelService.OSD_X_PCT`) para escritura sin reinicios.
    - **Front-end Lazy**: Carga del editor HTML/JS solo bajo demanda (`WebOsdEditor.java`) para no engordar el servidor principal.
    - **UX**: Acceso integrado en el Modal de Ajustes (Botón "AJUSTAR POSICIÓN").
### Fixed
- **NanoHttpServer**: Implementado helper `sendStringResponse` y carga correcta de `Properties` para solucionar errores de compilación al parsear parámetros URL.

## [v3.9.7-dev.21] - 2026-01-27
### Fixed
- **Core**: Estandarización de resolución a **352x288** (CIF) en todo el código base.
    - Se eliminaron las referencias experimentales a `320x200` para garantizar la coherencia con el sensor de hardware.

## [v3.9.7-dev.12] - 2026-01-26
- **v3.9.7-dev.25**:
    - `ui(web)`: 🧹 Eliminado indicador de FPS en tarjetas de video para limpiar la interfaz.
- **v3.9.7-dev.24**:
    - `feat(adb)`: 🐕 Watchdog V2 ADB con reinicio preventivo anti-zombi cada 3 horas.
- **v3.9.7-dev.23**:
    - `chore(logs)`: 📝 Corregido mensaje de log de resolución para validar CIF como "Nativa/Óptima" en lugar de "Culpable".
- **v3.9.7-dev.22**:
    - `feat(adb)`: 🐕 Implementado Watchdog para auto-recuperación del daemon ADB (Puerto 5555) mediante reinicio root selectivo.
### Changed
- **Web Dashboard (UI)**: Ajuste de padding en `.library`.
    - Eliminado el `padding-top` (set a 0px) para maximizar el uso del espacio bajo la cabecera.

## [v3.9.7-dev.11] - 2026-01-26
### Changed
- **Web Dashboard (UI)**: Compactación de la interfaz de usuario.
    - Reducción de padding en `.header` de 20px a 12px.
    - Reducción de padding vertical en `.live-btn` de 15px a 6px para un look más estilizado.

## [v3.9.7-dev.10] - 2026-01-26
### Changed
- **Web Dashboard (UI)**: Ajuste de dimensiones de miniaturas a **110x90**.
    - *Objetivo*: Recuperar la proporción exacta (11:9) del sensor CIF (352x288) para eliminar la distorsión visual.
    - *Fix*: Actualizado `width`, `height` y `min-width` en el CSS inyectado.

## [v3.9.7-dev.9] - 2026-01-26
### Engineering & Process
- **Sync Policy**: Implementado `git push` obligatorio tras cada commit de fallo en el workflow de snapshots. (Garantía de redundancia total).

## [v3.9.7-dev.8] - 2026-01-26
### Engineering & Process
- **Workflow /deploy_snapshot**: Fortificado el protocolo de despliegue.
    - Se obliga por contrato a realizar el **Registro Forense** en la Bitácora antes de corregir un fallo.
    - Se integra el **Protocolo de Cierre Cuaternario** explícitamente al final del ciclo de vida del snapshot.

## [v3.9.7-dev.7] - 2026-01-25
### Fixed
- **Web Dashboard (JS)**: Corrección definitiva del crash de `onclick` con validación estricta de tipos.
    - *Causa*: `getAttribute` puede devolver objetos no string en ciertos contextos de DOM antiguos/raros, o valores vacíos que evaluaban falsos positivos.
    - *Fix*: Añadido `typeof clickAttr === 'string'` para blindar la llamada a `.indexOf()`.

## [v3.9.7-dev.6] - 2026-01-25
### Fixed
- **Web Dashboard (JS)**: Corrección de error `NullPointerException` en Javascript al intentar marcar un vídeo como visto mientras hay una grabación en curso.
    - *Causa*: La tarjeta "Parásito" tiene la clase `.video-item` pero no tiene atributo `onclick`. El iterador fallaba al leer `null.indexOf`.
    - *Fix*: Validación defensiva de existencia del atributo antes de leerlo.

## [v3.9.7-dev.5] - 2026-01-25
### Changed
- **Web Dashboard (JS)**: Corrección de coherencia visual en miniatura "Parásito" (grabación en curso).
    - Añadida clase `.mini-canvas` al canvas inyectado dinámicamente y llamada inmediata a `updateWebTransformFromInputs()`.
    - *Resultado*: La tarjeta de grabación activa ahora respeta el Zoom/Pan digital configurado, igual que el resto de la galería.

## [v3.9.7-dev.4] - 2026-01-25
### Fixed
- **Web Dashboard (AJAX)**: Eliminada etiqueta `<img>` redundante que cubría el canvas de animación en las nuevas tarjetas generadas dinámicamente (`finalizeRecordingCard`).
    - *Cleanup*: Se elimina código muerto que intentaba cargar una imagen estática fallida sobre la animación real.

## [v3.9.7-dev.3] - 2026-01-25
### Changed
- **Web Dashboard (UI)**: Activado "Modo Cine" (Jumbo Thumbnails).
    - Aumento del tamaño de miniaturas de 80px a 150px (Casi 4:3).
    - Nuevo estilo con bordes reducidos (8px) y placeholders con degradado para mejorar la estética mientras carga.

## [v3.9.7-dev.2] - 2026-01-25
### Fixed
- **Web Dashboard (Cliente)**: Implementado **Smart Rendering** (Intersection Observer).
    - Las minicartas de vídeo ahora pausan su animación JS cuando salen de la pantalla.
    - *Objetivo*: Eliminar el sobrecalentamiento en el dispositivo cliente (iPhone 15 Pro, etc.) al evitar renderizar vídeos invisibles.

## [v3.9.7-dev.1] - 2026-01-25
### Changed
- **SentinelService**: Activado modo "Ultra Vago" (Ultra Lazy).
    - Se reduce el muestreo de vigilancia de `skipTarget=5` (~6 FPS) a `skipTarget=10` (~3 FPS) cuando no se está grabando.
    - *Objetivo*: Reducir aún más la carga de CPU en reposo para combatir picos de 43°C.

## [v3.9.6] - 2026-01-25 "The Bunker" 🏯
### Added (Stability & Cool Down)
- **Watchdog (Interruptor de Hombre Muerto)**: Sistema Dual (Cliente/Servidor) que corta el stream si no hay latidos cada 5s. Elimina "Conexiones Zombie".
- **Hardware Zoom Nativo**: Zoom por ISP (GPU) en lugar de CPU. Reduce temperatura pero elimina pan/zoom digital en pantalla local.
- **Anti-Amnesia Zoom**: Restauración imperativa del zoom tras reciclaje de memoria (apagado de pantalla).
- **Socket Guillotine**: Cierre forzoso de sockets MJPEG al cerrar modales en Web (1x1 pixel trick).

### Changed
- **FPS Limiter**: Estrangulamiento del driver a 15-30 FPS (respetando hardware) para evitar el calentamiento a 30 FPS fijos.
- **UX Web**: 
    - Full Bleed Video Cards (Object-fit: cover).
    - Eliminado `location.reload()` (Fallo Silencioso) para evitar saltos visuales.
    - Headers `Cache-Control: no-cache` para veracidad en Settings.

## [v3.9.6-dev.15] - 2026-01-25
### Changed
- **SentinelService**: Implementado "Retardo Táctico" (1.5s) al aplicar el zoom tras recuperar la superficie (`setPreviewSurface`).
    - *Razón*: Evitar Condición de Carrera. El driver antiguo resetea a 1x al hacer `startPreview()`. Esperamos a que termine de inicializarse para "martillear" el zoom guardado.
    - *Resultado*: La cámara arranca en 1x y salta al zoom correcto (ej: 2.5x) automáticamente tras 1.5s.

## [v3.9.6-dev.14] - 2026-01-25
### Changed
- **SentinelService**: Modificación en `enforceSavedHardwareZoom()` para aplicar el zoom **siempre** (Imperativo), eliminando la comprobación de estado previo que fallaba por caché falsa.
    - *Objetivo*: Corregir amnesia del driver que reportaba zoom aplicado cuando visualmente estaba a 1x.

## [v3.9.6-dev.13] - 2026-01-25
### Added
- **Watchdog (Interruptor de Hombre Muerto)**:
    - **Servidor**: Temporizador de 5s que corta el stream MJPEG si no recibe "latidos".
    - **Cliente (Web)**: Endopoint `/api/keepalive` llamado cada 2s mientras el visor está activo.
    - *Objetivo*: Eliminar conexiones "zombies" que calientan la CPU.
- **Hardware Zoom Persistence**:
    - Nuevo método `enforceSavedHardwareZoom()` en `SentinelService`.
    - Se restaura el zoom guardado automáticamente cuando Android recicla la `SurfaceView`.
### Fixed
- **API Settings**: Cabecera `Cache-Control: no-cache` añadida para evitar que el navegador mienta sobre el estado real del zoom.

## [v3.9.6-dev.12] - 2026-01-25
### Fixed
- **Web Dashboard**: Implementada "La Guillotina de Conexión".
    - Al cerrar el modal de Live View, se fuerza la carga de un GIF transparente 1x1 en el `src` de la imagen.
    - *Efecto*: Esto obliga al navegador a cortar el socket MJPEG inmediatamente, evitando que siga consumiendo ancho de banda y CPU del dispositivo en segundo plano.

## [v3.9.6-dev.11] - 2026-01-25
### Fixed
- **Web Dashboard**: Corregido error de sintaxis en JavaScript inyectado (`NanoHttpServer`).
    - Faltaba un salto de línea (`\n`) tras un comentario `//`, lo que comentaba accidentalmente el cierre de la función `});` y rompía la carga de scripts.

## [v3.9.6-dev.10] - 2026-01-25
### Reverted
- **SentinelService**: Retirada la estrategia "Kamikaze" de FPS que causaba `RuntimeException`.
- **Estabilidad**: Vuelta a la configuración automática "Safe Mode" que respeta lo que el hardware dicte (aunque sean 30 FPS).
    - *Prioridad*: "Better Warm & Stable than Cool & Broken".

## [v3.9.6-dev.9] - 2026-01-25
### Changed
- **SentinelService**: Estrategia "Kamikaze" para FPS.
    - Se ignora la lista de tasas soportadas (que miente diciendo solo 30 FPS).
    - Se fuerza `setPreviewFrameRate(15)` esperando que el hardware responda.
    - Se intenta estrangular el rango a `[15000, 15000]` como segunda medida.
    - *Objetivo*: Romper la barrera de los 30 FPS a la fuerza bruta.

## [v3.9.6-dev.8] - 2026-01-25
### Changed
- **SentinelService**: Implementación de estrategia "Old School" para limitar FPS.
    - Se usa la API deprecada `setPreviewFrameRate()` para forzar tasas fijas (ej: 15 FPS) en hardware antiguo.
    - Fallback a `setPreviewFpsRange` si la API antigua falla.
    - *Objetivo*: Evitar que el driver escale a 30 FPS automáticamente cuando el rango es [15-30].

## [v3.9.6-dev.7] - 2026-01-25
### Changed
- **SentinelService**: Auditoría y limitación de FPS por Hardware.
    - Se listan todos los rangos de FPS disponibles en el log (`/log`).
    - Se intenta forzar el driver a un rango máximo de 15 FPS para reducir la carga de E/S y temperatura base.
    - *Objetivo*: Reducir de 30 FPS ("calefacción central") a 15 FPS para vigilancia en reposo.

## [v3.9.6-dev.6] - 2026-01-25
### Changed
- **Web Dashboard**: Eliminado el `location.reload()` (recarga forzosa) ante fallos menores de AJAX al finalizar tarjetas. Se prefiere fallo silencioso o reintento suave para evitar "ceborrazos" visuales.

## [v3.9.6-dev.5] - 2026-01-24
### Added
- **SRE Monitoring**: Sistema de trazas completo (Logging) para monitorizar salud del sistema.
    - **Heartbeat**: Latido cada 60s con Temp, RAM y FPS (Procesados vs Skipped).
    - **Thermal Alerts**: Avisos inteligentes de cambio de estado (Overheat Triggered/Cleared).
    - **Web Activity**: Log de accesos a dashboard, streaming y vídeo.
    - **Performance**: Aviso `⚠️ CPU SLOW` si la compresión JPEG supera los 100ms.

## [v3.9.6-dev.4] - 2026-01-24
### Changed
- **Web Dashboard**: Cambio de `object-fit: contain` a `cover`. El vídeo ahora ocupa todo el alto disponible sin bandas negras, recortando los laterales si es necesario (Full Bleed).

## [v3.9.6-dev.3] - 2026-01-24
### Added
- **SentinelService**: Traza forense (Log) para identificar resolución y ratio de aspecto exactos elegidos por el hardware.

## [v3.9.6-dev.2] - 2026-01-24
### Fixed
- **Web Dashboard**: Corregido bug de actualización de stats en ventanas modales (IDs duplicados -> querySelectorAll).

## [v3.9.6-dev.1] - 2026-01-24
### Fixed
- **Web Dashboard**: Corregido CSS (`transform-origin: 0 0`) en la vista en vivo para evitar que el video se visualice fuera de la pantalla.

## [v3.9.5] - 2026-01-24 "Ice Age Stable" 🧊
### Added
- **Hardware Zoom**: `SentinelService` ahora gestiona el zoom nativamente (GPU/Driver), eliminando la carga de escalado por software en CPU.
- **Pintor Vago PRO**: Algoritmo de Deep Sleep que limita el procesado a **0.5 FPS** (1 frame cada 2000ms) cuando la pantalla está apagada y no hay grabación.
- **Amnesia Fix**: Persistencia de la superficie de vídeo (`activeSurfaceHolder`) para soportar cambios de configuración (rotación) sin congelar la imagen.
- **UI Rescue**: Inicialización robusta de `SurfaceView` (`MATCH_PARENT` en `onCreate`) para evitar condiciones de carrera con el driver gráfico antiguo.

### Changed
- **Optimización Térmica**: Reducción drástica de temperatura en reposo (~38°C vs 44°C).
- **Limpieza**: Eliminado código experimental de "Ghost Hunter" tras descartar la necesidad de filtros forenses complejos.

## [v3.9.5-dev.27] - 2026-01-24
### Added
- **Ghost Hunter Switch**: Variable `useGhostHunter` para activar/desactivar la protección anti-fantasmas.
  - Actualmente desactivada por defecto (`false`) y con umbrales relajados (`delta < 0`, `score > 5500`) para pruebas de control.

## [v3.9.5-dev.26] - 2026-01-24
### Fixed
- **Ghost Hunter (CSI 3.1)**: Ajustado umbral de "Filtro Inteligente Score".
  - Nuevo límite: Bloquea picos > **1500** (bajado desde 2500) tras detectar fantasma con score 2200.

## [v3.9.5-dev.25] - 2026-01-24
### Fixed
- **Ghost Hunter (CSI 3.0)**: Filtro Inteligente de "Score" (Discriminación por Intensidad).
  - Umbral de seguridad dinámico: Bloquea picos > 2500 de Score durante 30s tras grabar.
  - Elimina la "ceguera global": Permite grabar movimiento humano normal (~700 Score) inmediatamente.
  - Captura forense: Guarda el frame culpable en `/DebugGhost` con prefijo `GHOST_Flash_`.

## [v3.9.5-dev.24] - 2026-01-24
### Fixed
- **Ghost Hunter (CSI 2.1)**: Ampliada zona de peligro a **8000ms**.
- **Traceability**: Log del "Delta Time" siempre activo al detectar movimiento real.

## [v3.9.5-dev.23] - 2026-01-24
### Fixed
- **Ghost Hunter (CSI 2.0)**: Ampliada zona de peligro a **5000ms**.
- **Traceability**: Log del "Delta Time" al iniciar grabación para saber cuánto tiempo real pasa entre stop y start.

## [v3.9.5-dev.22] - 2026-01-24
### Fixed
- **Ghost Hunter (CSI)**: Implementada "Zona de Peligro" (3000ms) tras grabación.
  - Bloquea falsos positivos causados por el latigazo de exposición de la cámara al parar de grabar.
  - Guarda evidencia visual del "fantasma" en `/sdcard/ElOjoDelAbuelo/DebugGhost/`.

## [v3.9.5-dev.21] - 2026-01-23
### Fixed
- **Amnesia Fix**: Persistencia de `activeSurfaceHolder` para reconectar pantalla física tras reinicios (ej: rotación).
- **Pintor Vago PRO**: Cronómetro estricto (`activeSurfaceHolder` y `lastLazyTime`) para throttling a 0.5 FPS (1 frame/2s) REALES cuando no hay actividad.

## [v3.9.5-dev.20] - 2026-01-23
### Fixed
- **UI Freeze ([B006])**: Movida configuración de layout `MATCH_PARENT` de `onResume` a `onCreate`. **Resuelto arranque fluido**.
- **Cleanup**: Eliminado check innecesario de instancia en `onPause`.

### Known Issues
- ⚠️ **Hot-Swap Rotation Freeze**: Cambiar la rotación (0/180) desde la web congela la imagen en la pantalla del teléfono hasta que se reinicia la app.

## [v3.9.5-dev.19] - 2026-01-23
### Fixed
- **UI Rescue**: Restaurada visualización de cámara (`MATCH_PARENT`) tras eliminar zoom software.
- **Traceability**: Nuevos logs en `onResume`/`onPause`.

### Known Issues
- ⚠️ **Frozen Frame**: La imagen en pantalla se congela en el primer fotograma (no hay vídeo fluido local).

## [v3.9.5-dev.18] - 2026-01-23 "Ice Age" ❄️
### Added
- **Hardware Zoom** ("La Lupa Fría"): Zoom gestionado por el driver de cámara (`params.setZoom()`) en lugar de escalado por software. Cero coste de CPU.
- **Pintor Vago**: Modo deep sleep a 0.5 FPS cuando pantalla apagada y sin grabación (`processFrame` throttling).
- **Interruptor de Luz**: Sincronización estricta `onResume/onPause` ↔ `setUiCallback(null)` para activar modo eco instantáneamente.
- **Caza-Fantasmas**: Reset del `MotionDetector` tras cada grabación para eliminar falsos positivos por "salto temporal".
- **Live Zoom**: Cambios de zoom desde la web se aplican en caliente sin reiniciar cámara.

### Removed
- `applyZoomLogic()` en MainActivity (zoom por software eliminado).
- Manipulación de `LayoutParams` para escalado de View.
- Broadcast `ACTION_ZOOM_UPDATED` (ya no es necesario).

### Fixed
- **Ghost Trigger [B005]**: Grabaciones fantasma encadenadas tras terminar una real.
- **Thermal Regression [B004]**: Temperatura estable ~35°C en reposo vs. 42-44°C anterior.
- **OOM Crashes**: Eliminados al no crear bitmaps escalados en memoria.

## [v3.9.5-dev.17] - 2026-01-23
### Added
- **Debug**: Trazas adicionales en `MainActivity`:
  - `Broadcast -> [action]` → Eventos del sistema recibidos
  - `User Kill Switch` → Usuario pulsa botón APAGAR

## [v3.9.5-dev.16] - 2026-01-23
### Added
- **Debug**: Trazas de ciclo de vida en `MainActivity` para diagnóstico del crash diferido:
  - `CREATED` → Pantalla intenta arrancar
  - `RESUMED (Visible)` → Pantalla visible
  - `PAUSED (Background)` → Sistema mata la UI o pantalla apagada

## [v3.9.5-dev.15] - 2026-01-23
### Added
- **Feature**: Sistema de "Caja Negra" (Blackbox Logging) para diagnóstico del crash diferido.
  - Buffer RAM circular (50 líneas) para visualización web en `/log`
  - Persistencia asíncrona en disco: `/sdcard/ElOjoDelAbuelo/abuelolog.log`
  - Sondas de diagnóstico en: `onCreate`, `startCamera`, `setPreviewSurface`, `previewCallback`
- **Debug**: Permite auditoría forense post-mortem vía `adb shell tail -f /sdcard/ElOjoDelAbuelo/abuelolog.log`

## [v3.9.5-dev.14] - 2026-01-22
### Fixed
- **Critical**: Solucionada pantalla negra en Activity. El driver de cámara del Galaxy S necesita el re-enganche del callback (`setPreviewCallbackWithBuffer`) en `setPreviewSurface` para activar el enlace pantalla↔driver.
- **Stability**: Modo Eco Térmico ahora funciona correctamente con imagen visible.

## [v3.9.5-dev.13] - 2026-01-22
### Added
- **Experimental**: Modo Eco Térmico - Throttling dinámico de frames (5:1 en idle, 2:1 grabando) + límite hardware 20 FPS.

### Known Issues
- ⚠️ **Pantalla negra**: La Activity se mantiene abierta pero no muestra la imagen de la cámara (solo botón APAGAR visible). Requiere investigación.

## [v3.9.5-dev.12] - 2026-01-21
### Changed
- **Meta**: Migración completa de BACKLOG.md a GitHub Issues. Actualizada Regla 11 en `legacy_dev_rules.md` para usar `gh` CLI.
- **Workflow**: Creadas 7 issues en GitHub (4 abiertas: #1-#4, 3 cerradas: #5-#7).
- **Docs**: BACKLOG.md archivado como BACKLOG.md.archived y restaurado para referencia local.

## [v3.9.5-dev.11] - 2026-01-21
### Fixed
- **Critical**: Eliminadas líneas de reset de callback en `setPreviewSurface` que causaban race condition en HAL de cámara Android 2.3. Alineado con arquitectura documentada en `HARDWARE_PREVIEW_WALKTHROUGH.md`.
- **Stability**: Activity ya no crashea después de la instalación inicial.
- **Performance**: Temperatura reducida de 42°C a 39-40°C en reposo (~3°C menos).

## [v3.9.5-dev.10] - 2026-01-21
### Changed
- **Meta**: Nueva Regla 11 en `legacy_dev_rules.md` para gestión de BACKLOG.md. Incluye templates obligatorios para bugs (B###), features (F###) y tech debt (T###).

## [v3.9.5-dev.9] - 2026-01-21
### Added
- **Docs**: Creado `BACKLOG.md` para gestión de bugs, features y mejoras técnicas. Incluye 3 bugs (B001-B003) y 1 feature (F001) documentados.

## [v3.9.5-dev.8] - 2026-01-21
### Changed
- **Config**: Estandarizado `versionName` en AndroidManifest.xml a `legacy-compat` para mayor claridad semántica. Confirma que el valor del string no afecta la estabilidad.

## [v3.9.5-dev.7] - 2026-01-21
### Changed
- **Meta**: Actualizada Regla 7 en `legacy_dev_rules.md` para estandarizar el formato del reporte final de cierre (Verificación de 6 Puntos).

## [v3.9.5-dev.6] - 2026-01-21
### Fixed
- **Stability**: Restaurado `AndroidManifest.xml` exactamente como en v3.9.2 (con `versionCode` y `versionName` legacy). Esto corrige el crash de la Activity en Android Legacy.

### Changed
- **Meta**: Nueva Regla 10 en `legacy_dev_rules.md` para proteger los atributos del Manifest.

## [v3.9.5] - 2026-01-21
### Added
- **Performance**: Lazy Load (Infinite Scroll) para la lista de videos en el dashboard web. Reduce drásticamente el tiempo de carga inicial y el consumo de CPU.
- **API**: Nuevo endpoint `/api/list_videos` con paginación JSON.

### Fixed
- **Stability**: Restaurado protocolo completo de gestión de cámara (reinicialización de buffers y callbacks) para evitar el cierre de la Activity en Android Legacy.
- **Compatibility**: Restaurado atributo `package` en AndroidManifest.xml, requerido por el runtime de Android 2.3/4.x para resolver clases.
- **UI**: Corregido handler de clicks en tarjetas de video para mayor compatibilidad con WebViews antiguos.

## [v3.9.5-dev.4] - 2026-01-21
### Fixed
- **Stability**: Restaurada lógica completa de gestión de cámara v3.9.4. Se vuelve a usar la reinicialización total de buffers y callbacks al cambiar de superficie para evitar el cierre inesperado de la Activity en Android 2.3.

## [v3.9.5-dev.3] - 2026-01-21
### Fixed
- **Crash**: Revertido uso de `setPreviewTexture` (API 11+) incompatible con Android 2.3 (API 10). Restaurado `setPreviewDisplay(null)` para soporte Legacy.

## [v3.9.5-dev.2] - 2026-01-21
### Fixed
- **Stability**: Implementación "Safe Mode" en `setPreviewSurface` para evitar crashes al cambiar entre background y foreground. No se reinician los buffers ni callbacks innecesariamente.
- **Hardware**: Ajuste de rotación hardware a 90 grados (estándar Galaxy S) en lugar de 180.

## [v3.9.5-dev.1] - 2026-01-20
### Added
- **Performance**: Lazy Load (Infinite Scroll) para la lista de videos. Reduce consumo de CPU y tiempo de carga inicial.
- **API**: Nuevo endpoint `/api/list_videos` en JSON.
- **Fix**: Handler de clicks en tarjetas de video para mayor compatibilidad.

## [v3.9.4] - 2026-01-20
### Added
- **UX**: Zero-Reload Recording Cycle (Hot-Swap). La tarjeta de grabación se transforma en tarjeta de video sin recargar la página.
### Fixed
- **UI**: Eliminados botones redundantes y corregida inconsistencia visual.
- **Core**: Corregida condición de carrera al iniciar grabación.
- **Protocol**: Soporte para método `HEAD` en servidor HTTP.
- **Bugfix**: Múltiples correcciones de sincronización y URLs.

## [v3.9.3] - 2026-01-20
### Added
- **Web Engine**: Sistema de posicionamiento relativo (Responsive Pan/Zoom).
- **UX**: Coherencia visual total entre Monitor (Grande), Miniaturas (Pequeñas) y Preview Animado.

## [v3.9.3-dev.7] - 2026-01-19
### Changed
- **Bugfix (JS)**: Corregida una línea errante que seguía usando `px` en lugar de `%`. Esto provocaba que el sistema ignorase la nueva lógica responsive al aplicar el pan por defecto a las miniaturas y a la vista del video en grande.

## [v3.9.3-dev.6] - 2026-01-19
### Changed
- **Web Engine**: Cambio de paradigma en el posicionamiento de video (Pan). Se sustituyen los píxeles (`px`) por porcentajes (`%`) relativos al ancho de la imagen.
- **Fix**: Soluciona la discrepancia visual entre el video monitor (grande) y las miniaturas (pequeñas), garantizando un encuadre idéntico independientemente del tamaño de pantalla.

## [v3.9.3-dev.5] - 2026-01-19
### Fixed
- **CSS**: Sincronización de geometría (`object-fit: contain` + `transform-origin`) para elementos `.mini-canvas`. Asegura que las miniaturas animadas respondan al Zoom/Pan exactamente igual que las estáticas.

## [v3.9.3-dev.4] - 2026-01-19
### Fixed
- **Web UI**: Cálculo matemático de miniaturas sincronizado con el ancho del viewport. Soluciona el problema de desaparición de miniaturas al desplazar el video main.
>>>>>>> origin/main

## [v3.9.3-dev.3] - 2026-01-19
### Fixed
- **UI Logic**: Desplazamiento de Pan adaptable al ancho de pantalla (Dynamic Ratio). Soluciona el problema de "movimiento excesivo" en navegadores de escritorio.

## [v3.9.3-dev.2] - 2026-01-19
### Fixed
- **UI**: Corrección geométrica de miniaturas. Se alinean origen (0,0) y ajuste (contain) con el reproductor principal para un escalado idéntico.

## [v3.9.3-dev.1] - 2026-01-19
### Added
- **UI**: Sincronización de Zoom y Pan en las miniaturas de la lista de videos.
- **Fix**: Corrección de escala de Pan en miniaturas (Factor 0.25) para evitar desplazamiento excesivo.

## [v3.9.2] - 2026-01-19
### 🚀 Release
- Elevación a **Estable** del ciclo de mejoras de UI y gestión de preferencias independientes (Web vs Hardware).
- Se han consolidado 13 iteraciones de desarrollo (dev.1 a dev.13) manteniendo todo el detalle histórico a continuación.

## [v3.9.2-dev.13] - 2026-01-19
### Changed
- **Content**: Corrección lingüística de tooltips para mayor claridad y terminología orientada al usuario.

## [v3.9.2-dev.12] - 2026-01-19
### Added
- **UI/UX**: Tooltips explicativos en TODAS las opciones de configuración (Sensibilidad, Espacio, Rotación, Hardware Zoom).

## [v3.9.2-dev.11] - 2026-01-19
### Changed
- **UI**: Mejorada la descripción del "Tiempo extra de grabación" con tooltip explicativo.

## [v3.9.2-dev.10] - 2026-01-19
### Changed
- **UI**: Alineación numérica a la derecha en TODOS los inputs (Time, Storage, Pan).

## [v3.9.2-dev.6] - 2026-01-19
### Added
- **UI**: Diseño "Pro" para controles de Pan X/Y con iconos (↔, ↕) y ancho optimizado.

## [v3.9.2-dev.4] - 2026-01-19
### Added
- **UI**: Iconos de tendencia de temperatura persistentes ("Sticky Trends").
- **FIX**: Desacople total del Zoom del Video Player (ahora usa `webZoom` settings).
- **JS**: Variables globales para gestión robusta de transformaciones CSS.

## [v3.9.2-dev.1] - 2026-01-19
### Added
- **Dual View Preferences**: Separated Zoom/Pan settings for Web Dashboard vs Android Display.
    - **Web**: `webZoom`, `webPanX`, `webPanY` (CSS Transform).
    - **Hardware**: `defZoom`, `defPanX`, `defPanY` (SurfaceView Scaling).
- **Settings UI**: Split configuration modal into "Vista Web" and "Vista Abuelo".

## [v3.9.1] - 2026-01-19
### Released
- **Gold Master**: Versión estable consolidada. Incluye todas las mejoras de la serie v3.9.1-dev (Sticky Trends, correcciones de UI, Conformidad de Reglas).

## [v3.9.1-dev.16] - 2026-01-19
### Added
- **Web Dashboard**: New "Sticky Trend" indicators for temperature. Shows a Red ▲ when heating up and Green ▼ when cooling down. The indicator persists during stability to show recent history.
### Fixed
- **UI**: Removed duplicate "v" prefix in version display string (now shows `v3.9...` instead of `vv3.9...`).
- **UI**: Replaced Emoji arrows with Geometric shapes to ensure CSS coloring works on mobile devices.

## [v3.9.1-dev.15] - 2026-01-19
### Fixed
- **Web Dashboard**: Fixed broken AJAX status updates (Temperature/Battery) by adding missing `id` attributes to header elements. The dashboard now updates in real-time every 5 seconds.

## [v3.9.1-dev.14] - 2026-01-18
### Compliance & Engineering
- **Rules Upgrade**: Added **Rule 9 (Fast Mode Protocol)**. Mandates the execution of the Quaternary Closing Protocol even during fast/chat interventions to prevent versioning gaps when not using `task.md`.

## [v3.9.1-dev.13] - 2026-01-18
### Compliance & Engineering
- **Rules Upgrade**: Enforced version prefix in Git Commit Subject (Rule 4). All commits must now start with `vX.Y.Z`.

## [v3.9.1-dev.12] - 2026-01-18
### Compliance & Engineering
- **Rules Upgrade**: Modified `legacy_dev_rules.md` to institute the **Quaternary Closing Protocol** (Version -> Bitacora -> Changelog -> Commit) and **Universal Logging** (Rule 5), removing subjectivity from documentation criteria.

## [v3.9.1-dev.11] - 2026-01-18
### Compliance & Engineering
- **Policy Change**: Adopted **Absolute Traceability**. The entire repository (Code + Docs) is now treated as a single product entity. Any change to artifacts triggers a semantic version bump.
- **Audit (Web Performance)**: Investigated suspect web client traffic ("Client Parasite").
    - **Finding**: Confirmed "Burst Profile" behavior. High load on open (2.59MB/min), negligible load on idle (6.7KB/min).
    - **Verdict**: Validated architecture. Passive heat generation from web client is physically impossible.

## [v3.9.1-dev.10] - 2026-01-18
### Engineering & Process
- **Language Policy**: Reverted to **Spanish Only** for all artifacts (Commits, Code, Docs) to prioritize educational accessibility for the target audience.
- **Polish (Docs)**: Removed minor text duplication in `legacy_dev_rules.md` to achieve pristine documentation state.
- **Hotfix (Docs)**: Cleaned up duplicate rules in `legacy_dev_rules.md` and fixed "Magic File" ambiguity in `release_version.md`.
- **Rules**: Refined "Personality Persistence" in `legacy_dev_rules.md` to emphasize Meta-Consciousness, Storytelling, and Intellectual Honesty.
- **Strategic Protocol**: Updated `strategic_change.md` to rigorously enforce date formatting in documentation entries.
- **Documentation**: Added `docs/diagrams/agent_workflow_cognition.png` and deep-dive analysis in Bitacora (Phase 36) visualizing how the Agent interprets workflow files as state graphs.
- **Strategic Protocol**: Updated `strategic_change.md` to mandate `BITACORA.md` and `CHANGELOG.md` updates for every meta-change.

## [v3.9.0] - 2026-01-18
### Added
- **Sentinel Mode**:
    - **Smart Screen**: Screen wakes up with max brightness on motion detection (`ACTION_REC_START`).
    - **Power Saving**: Screen sleeps automatically 1s after recording stops.
    - **VIP Pass**: App dismisses Keyguard ("Slide to Unlock") and shows over Lockscreen using `FLAG_DISMISS_KEYGUARD`.


## [v3.8.0] - 2026-01-18
### Added
- **Hot-Swap Zoom**: Broadcast system to apply View Settings (Zoom/Pan) instantly without restarting the app.
### Changed
- **Rescue Identity**: Permanently changed `applicationId` to `com.elojodelabuelo.rescue` to bypass Android UID Corruption issues.

## [v3.7.1] - 2026-01-18
### Changed
- **UI Polish**: "Stealth Mode" for the Kill Switch button. Increased transparency (25% Alpha), reduced size, and corner positioning.

## [v3.7.0] - 2026-01-18
### Changed
- **UI Cleanup**: Removed "Restart" and "Test Zoom" debug buttons.
- **Kill Switch**: Added a single "APAGAR SISTEMA" button that safely stops `SentinelService` and closes the App.

## [v3.6.1] - 2026-01-18
### Changed
- **Ergonomics**: Forced `reverseLandscape` in Manifest to match the physical mounting of the device (USB up).

## [v3.6.0] - 2026-01-18
### Added
- **Hardware Zoom & Pan**: Final integration of verified Zoom features into `MainActivity`.
- **UI**: Complete rewrite of `activity_main.xml` to FrameLayout for Clean Slate stability.
- **Service Bridge**: Robust `SentinelService` connection logic with Hardware Scaling support.
## [v3.5.10] - 2026-01-17
### Fixed
- **GOLD MASTER**: Restauración forzosa de `SentinelService.java` a la "Edición de Oro".
- **Buffer Fix**: Se incluye la lógica crítica de regeneración de buffers en `setPreviewSurface` para prevenir el congelamiento del video.
- **Rotation Fix**: `setDisplayOrientation(180)` incluido nativamente en el método de switch de superficie.

## [v3.5.9] - 2026-01-17
### Fixed
- **Hybrid Fix**: Recuperación de `SurfaceHolder.Callback` en `MainActivity` para conectar la cámara a la pantalla.
- **Bridge**: Nuevo método `SentinelService.setPreviewSurface` para alternar dinámicamente entre `SurfaceView` (Foreground) y `dummyTexture` (Background).
- **Auto-Start**: Restaurado el inicio automático del servicio al abrir la App para garantizar disponibilidad Web.

## [v3.5.8] - 2026-01-17
### Fixed
- **Clean Slate**: Reescritura completa de `MainActivity` y `layout` para eliminar conflictos de ventana.
- **Refactor**: Eliminación de `setFlags` y atributos legacy que causaban conflictos en Android 2.3.
- **Safety**: Bloques `try-catch` globales en `onCreate` y `onResume`.

## [v3.5.7] - 2026-01-17
### Fixed
- **Conflict Resolution**: Removed `getWindow().setFlags(...)` in `MainActivity.java` to resolve a fatal conflict with the `Theme.NoTitleBar.Fullscreen` defined in the Manifest on Android 2.3. The theme now solely handles the full-screen behavior.

## [v3.5.6] - 2026-01-17
### Fixed
- **Manifest Revert**: Reverted `screenOrientation` to `landscape` and added `configChanges="orientation|keyboardHidden"` in `AndroidManifest.xml`. The `reverseLandscape` option was causing a native crash on the targeted Samsung Android 2.3 ROM. Priority is stability over native rotation.
- **Theme**: Applied `Theme.NoTitleBar.Fullscreen` directly in Manifest for cleaner startup.

## [v3.5.5] - 2026-01-17
### Fixed
- **Defensive Crash Prevention**: Completely rewrote `MainActivity` to be "Defensive". Removed `requestWindowFeature` (potential conflict), added `instanceof` checks for `LayoutParams` (to prevent XML cache mismatch crashes), and wrapped `onCreate` and `onResume` in global `try-catch(Throwable)` blocks. The app should now degrade gracefully instead of Force Closing.

## [v3.5.4] - 2026-01-17
### Fixed
- **Robust UI Initialization**: "Armored" `MainActivity.onResume` with a NullPtr check for `SurfaceView` and a global `try-catch(Throwable)` block to prevent Force Close on startup. If UI initialization fails, the app will now stay open and show a Toast error instead of crashing.

## [v3.5.3] - 2026-01-17
### Fixed
- **Startup Crash**: Fixed a critical crash on launch caused by an invalid `android:layout_centerInParent` attribute in a FrameLayout and ensured `requestWindowFeature` is called strictly *before* `setContentView` in `MainActivity`.

## [v3.5.2] - 2026-01-17
### Fixed
- **Zoom Persistence**: Modified `MainActivity.onResume` to read Zoom/Pan settings directly from `SharedPreferences` ("SentinelPrefs") instead of relying on static variables, ensuring the correct value is applied after a web configuration change and app restart. Debug Toast updated to show "Zoom Disco: X.Xx".

## [v3.5.1] - 2026-01-17
### Fixed
- **Visual Clean**: Removed residual "El Ojo Del Abuelo" text overlay for a completely clean view.
- **Hardware Zoom**: Fixed `onResume` logic to use specific `FrameLayout.LayoutParams`, ensuring the digital zoom actually scales the SurfaceView on the physical screen. Added Debug Toast.

## [v3.5.0] - 2026-01-17
### Added
- **Visual Upgrade**: Fully immersive Full-Screen mode in `MainActivity` (No title, no status bar).
- **Hardware Zoom**: `MainActivity` now applies the Zoom/Pan settings directly to the display Surface using `LayoutParams`, allowing "Zero-CPU" digital zoom on the physical screen.

## [v3.4.1] - 2026-01-17
### Fixed
- **Camera Rotation**: Implemented `setDisplayOrientation(180)` in `SentinelService`. The `reverseLandscape` in Manifest only rotated the UI; this fix forces the Hardware Camera Preview to match the inverted orientation.

## [v3.4.0] - 2026-01-17
### Changed
- **Ergonomics**: Changed `screenOrientation` to `reverseLandscape` in `AndroidManifest.xml`. This rotates the UI and Camera 180 degrees to accommodate devices mounted with the USB port facing upwards.

## [v3.3.3] - 2026-01-17
### Changed
- **Documentation**: Finalized `BITACORA.md` with deep historical context, detailed engineering decisions (nv21, thermal optimization), and "Client Parasite" architecture storytelling.

## [v3.3.2] - 2026-01-16
### Changed
- **Developer Experience**: Refactored the development protocol from a single markdown file to a structured Agentic Architecture (`.agent/rules` and `.agent/workflows`).
- **Documentation**: Updated contribution guidelines to mandate educational commit messages.

## [v3.3.1] - 2026-01-15
### Fixed
- **Orientation**: Forced application to `landscape` mode in Manifest to correct vertical display issue on Hardware Preview.

## [v3.3.0] - 2026-01-15 (Arquitectura "Zero CPU")
### Added
- **Hardware Preview**: Implemented direct `SurfaceView` binding for the Camera. The preview stream now bypasses the CPU completely, rendering directly to the screen overlay.
- **Hybrid Mode**: Implemented a "Buffer Refill" mechanism (`addCallbackBuffer` + `stop/start` cycle) to ensure that Software Processing (Motion Detection / Web Stream) works simultaneously with the Hardware Preview.
- **Legacy Support**: Added `SURFACE_TYPE_PUSH_BUFFERS` to ensure compatibility with Android 2.3 (Gingerbread) drivers.

### Changed
- **Architecture**: Removed all `BitmapFactory` and `ImageView` logic from `MainActivity`. The UI is now purely a passive viewport for the Camera Hardware.
- **Performance**: Drastic reduction in CPU usage during preview (approx. -60% load), eliminating UI lag and thermal throttling.

## [v3.2.0] - 2026-01-13
### Added
- **Active Monitor**: The device now wakes up instantly when motion is detected.
- **Max Brightness**: Screen brightness is forced to 100% during recording for clear monitoring.
- **Zero-Copy Preview**: Implemented Hardware-Direct video preview for consistent 30 FPS without CPU overhead.
- **Auto-Sleep**: Screen turns off automatically 1 second after recording stops to save battery.

## [v3.1.1] - 2026-01-13
### Changed
- **UX**: Detailed Settings Modal now has vertical scrolling (`max-height: 85vh`), improving usability on small screens.

## [v3.1.0] - 2026-01-13
### Added
- **Auto Storage Management**: "Circular Buffer" feature. The app now automatically deletes old videos when disk space is low (configurable, default <500MB).
- **Settings**: New option to configure "Minimum Free Space" in the settings modal.

## [v3.0.14] - 2026-01-12
### Added
- **Live View Modal**: Replaced raw stream tab with a proper in-app modal containing the Persistent Header (Battery/Temp) and "Close" button.

## [v3.0.13] - 2026-01-12
### Fixed
- **SOLVED**: Definitive removal of duplicated garbage code in JS generation.

## [v3.0.12] - 2026-01-12
### Fixed
- **URGENT**: Removed Java source code leaking into JavaScript output, which caused full UI paralysis.

## [v3.0.11] - 2026-01-12
### Fixed
- **Critical**: Resolved UI unresponsiveness caused by malformed HTML/CSS injection.
- **Protocol**: Enforced strict version bumping for every deployment to ensure cache busting.

## [v3.0.10] - 2026-01-12
### Improved
- **UX**: Persistent Status Header (Battery, Temp, Storage) now visible inside Video Player and Live View.
- **Code**: Refactored HTML generation to use DRY principles and class-based DOM updates for synchronized status.

## [v3.0.9] - 2026-01-12
### Improved
- **UI**: Dashboard video list now displays formatted Date (DD/MM/YYYY), Time (HH:mm:ss), and FPS instead of raw filenames.
- **UI**: File Size and Duration metadata are now displayed in **bold** for better readability.

## [v3.0.8] - 2026-01-12
### Added
- **UX**: Visual indicator (dimming) for watched videos in the current session.
- **UX**: Added metadata to video cards: File size (MB/KB) and Duration (calculated from timestamp).

## [v3.0.7] - 2026-01-12
### Fixed
- **UI**: Fixed a critical bug where the header (Title/Settings) would disappear on mobile devices when the video library loaded. Implemented strict CSS Flexbox containment.

## [v3.0.6] - 2026-01-12
### Added
- **HUD**: Implemented a "Calibration HUD" overlay on the video player. Displays real-time Zoom (x), Pan X, and Pan Y values during gestures to assist with setting defaults.

## [v3.0.5] - 2026-01-12
### Added
- **UI**: Added "Close" (X) button to Settings Modal header.
- **Storage**: Added "Delete All Videos" button in Settings (Danger Zone).
- **UX**: Added "Default View" preferences (Zoom, Pan X, Pan Y) that persist across restarts.
- **Docs**: Updated Walkthrough with new features.

## [v3.0.4] - 2026-01-11
### Fixed
- **Ghost Reset Bug**: Fixed an annoyance where releasing a pinch-zoom gesture quickly would accidentally trigger the "Double-Tap Reset" logic, snapping the video back to 1x size. The system now intelligently ignores "taps" that occur immediately after a multi-touch gesture.

## [v3.0.3] - 2026-01-11
### Added
- **Double-Tap Reset**: Implemented a "Quick Reset" gesture. Double-tapping the video instantly resets zoom and position to default.

### Changed
- **UX Refinement**:
    - **Smart Pan Constraint**: Disabled panning when the image is at original scale (1x) to prevent dragging the image into empty space.
    - **Auto-Center**: When pinching out to minimum zoom (1x), the image now automatically snaps to center (0,0), ensuring a clean reset.

## [v3.0.2] - 2026-01-11
### Fixed
- **Pan Logic Error**: Fixed a critical bug where `dy` (vertical delta) was undefined in the `touchmove` handler, causing the script to crash silently when attempting to pan the image with one finger.

## [v3.0.1] - 2026-01-11
### Fixed
- **Pan Locking**: Removed the artificial restriction that prevented panning unless `zoom > 1`. Now panning is always active when using one finger, ensuring smooth navigation even at near-1x zoom levels or after gesture switches.

## [v3.0.0] - 2026-01-11
### Major Update
- **Interactive Pan & Zoom**: Final, polished implementation of the video player zoom.
  - Removed all debug overlays and temporary logging.
  - Implemented persistent zoom state during playback.
  - Optimized for iOS Safari (iPhone 15 Pro) and Android 2.3 compatibility.

## [v2.9.9] - 2026-01-11
### Fixed
- **Zoom Reset Bug**: Removed critical logic error where `resetZoom()` was called on every frame update, preventing zoom from persisting during playback. Zoom state is now preserved while the video plays.

## [v2.9.8] - 2026-01-11
### Fixed
- **iOS Zoom Fix**: Added `{ passive: false }` to touch event listeners. This is mandatory for modern iOS Safari to respect `preventDefault()` and allow custom zoom logic without the browser interfering.

## [v2.9.7] - 2026-01-11
### Fixed
- **Zoom Jitter**: Aggressively blocked default browser touch events to prevent the native zoom from fighting with the custom pinch-to-zoom logic, eliminating the "trembling" effect.

## [v2.9.6] - 2026-01-11
### Fixed
- **JS Runtime Error**: Fixed `ReferenceError: ratio is not defined` in the zoom logic which was silently crashing the touch handler. Zoom should now be functional.

## [v2.9.5] - 2026-01-11
### Debugging
- **On-Screen Console**: Retrying injection of the debug overlay, as it failed to appear in v2.9.4.

## [v2.9.4] - 2026-01-11
### Debugging
- **On-Screen Console**: Added a temporary debug overlay to visualize touch coordinates, scale factors, and matrix calculations in real-time for troubleshooting Android 2.3 zoom issues.

## [v2.9.3] - 2026-01-11
### Fixed
- **JS Syntax Error**: Restored missing closing braces in the `touchend` event listener that were accidentally removed, restoring functionality to all buttons and touch interactions.

## [v2.9.2] - 2026-01-11
### Fixed
- **Zoom Physics**: Replaced modern `Math.hypot` with ES3 `Math.sqrt` to prevent crashes/jitter on Android 2.3.
- **Focal Point**: Implemented matrix math to ensure the zoom centers on the user's fingers, fixing the "drift to corner" issue.
- **Touch Drift**: Corrected state reset logic when lifting fingers to prevent image jumping.

## [v2.9.1] - 2026-01-11
### Fixed
- **Zoom Jitter**: Fixed an issue where the zoom level would reset abruptly when adjusting the pinch gesture. Zoom is now smooth and cumulative.

## [v2.9.0] - 2026-01-11
### Added
- **Interactive Player**: Added Pan & Zoom support using multi-touch gestures (Pinch-to-Zoom).
- **UX**: Prevented UI scaling on mobile devices to ensure controls remain accessible while zooming the video.

## [v2.8.2] - 2026-01-10
### Changed
- **UX**: Implemented "Hot-Swap" logic. Live Preview cards are automatically replaced by the final static video card (with real metadata/filesize) 3 seconds after recording stops.

## [v2.8.1] - 2026-01-10
### Changed
- **UX**: Live Preview animation now loops indefinitely after recording stops (previously froze on last frame). This provides better context until page reload.

## [v2.8.0] - 2026-01-10 (Live Injection)
### Added
- **Live Preview Injection**: New recordings appear instantly in the list with a "GRABANDO" status.
- **Client Parasite**: Javascript captures frames from the hidden `/stream` to generate an animated preview in real-time.
- **Backend API**: Added `/api/latest_video_meta` for lightweight metadata retrieval.

## [v2.7.0] - 2026-01-10 (Auto-Deploy Verified)
### Added
- **Diagnostics**: Added "Boot Time" timestamp to the dashboard footer.
- **Workflow**: Automated version bumping and Git Protocol verification (Self-Test).
 
## [v2.6.0] - 2026-01-09
### Added
- **UI Version Display**: Dashboard header now dynamically displays the app version (e.g., "v2.6").
- **Visual Polish**: Improved alignment of "Live Camera" button and removed stray escaped characters in HTML.

## [v2.5.0] - 2026-01-09
### Fixed
- **Ghost Recordings**: Replaced linear sensitivity formula with an exponential one (`10000 * (1 - sens/100)^2`), eliminating false positives at low sensitivity.
- **0KB Files**: Reordered processing logic to prioritize disk writing before network broadcasting.
- **Camera Reliability**: Implemented a "Watchdog" that flags critical hardware errors (null data) and displays a red alert in the Web UI.

## [v2.4.0] - 2026-01-09
### Refactor
- **Web Player**: Replaced `<canvas>` implementation with native `<img>` tag using Blob URLs.
- **Benefits**: Fixed visual glitches (split images) on rotated videos and improved rendering performance.

## [v2.3.0] - 2026-01-08 (Milestone: "Cool & Stable")
### Added
- **Native Resolution (CIF)**: Forced camera preview to **352x288** to match hardware capabilities, replacing the heuristic resolution search.
- **Frame Throttling**: Implemented a "Process 1 / Skip 1" strategy in `SentinelService`.
- **Diagnostics**:
    - File-based camera audit (`/sdcard/camera_info.txt`).
    - Real-time FPS counter on Web Dashboard.
    - Auto-start surveillance on app launch.

### Changed
- **Optimization**:
    - CPU load reduced by ~50% via frame throttling.
    - Operating temperature stabilized (40°C -> 38°C).
    - FPS stabilized at 15 FPS (derived from 30 FPS hardware input).

## [v2.2.0] - 2026-01-08
### Added
- **Double Buffering (Ping-Pong)**: Implementation of a `rotationBuffers` pool (size 2) in `SentinelService`. This decouples the camera writing thread from the background reading thread, completely eliminating screen tearing artifacts.
- **Software Rotation (180°)**: New `rotateNV21Degree180` method that manually inverts the Y and UV planes of the NV21 byte array. This serves as a critical workaround for the i9000's lack of hardware preview rotation support.

### Changed
- **Memory Optimization**: Rotation buffers are now reused to prevent Garbage Collection churn during high-frequency preview callbacks.
- **Preview Logic**: Removed `params.setRotation()` calls affecting the preview stream as they were ineffective on the target hardware.

## [v2.1.0] - 2026-01-07
### Added
- **Web Settings UI**: Added a configuration modal to the dashboard allowing remote control of:
  - Motion Sensitivity (0-100%).
  - Recording Timeout (10s/30s/60s).
  - Detector Status (On/Off).
  - Rotation Toggle (0°/180°).
- **Animated Thumbnails**: Dashboard now renders a live 1fps preview using a secondary MJPEG stream.

## v3.5.8 (Hotfix: Clean Slate)
- **FIX**: Reescritura completa de `MainActivity` y `layout` para eliminar conflictos de ventana.
- **REF**: Eliminación de `setFlags` y atributos legacy en XML.
- **SAFE**: `try-catch` blocks en `onCreate` y `onResume`.

## v3.5.7 (Hotfix: Window Flags)
- **FIX**: `WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS` was causing a black bar at the bottom of the screen on some devices. Replaced with `FLAG_LAYOUT_NO_LIMITS` for full-screen immersion.

### Changed
- **Performance**: Relocated the motion threshold calculation (`500 - sensitivity * 4.9`) out of the `onPreviewFrame` hot loop. It is now calculated only upon settings change, saving ~15% CPU per frame.
- **API**: Added endpoints `/api/settings` (GET) and `/api/save_settings` (POST) to `NanoHttpServer`.

## [v2.0.0] - 2026-01-07
### Added
- **Persistence Layer**: Integrated `SharedPreferences` to store and load application state (Sensitivity, Rotation, Timeout) across device reboots.
- **Hot-Swap Logic**: `updateSettings()` method allows changing core parameters without restarting the service (except for rotation, which requires camera re-init).

## [v1.0.0] - Initial Release
### Added
- **Motion Detection Algorithm**: Optimized pixel-difference logic.
- **Thermal Guardian**: Automatic safety shut-off when CPU temperature > 45°.
- **System Stats**: Battery and Storage monitoring.
- **NanoHTTPD**: Lightweight Web Server.
- **ui(web)**: Eliminados atributos de tamaño fijo en miniatura de grabación activa para igualar proporción.
- **fix(ux)**: Corrección de alineamiento visual pendiente (miniatura desplazada).
- **ui(web)**: Ajuste de canvas parásito a 320x200px para mejorar alineamiento visual.
- **fix(web)**: Restaurada clase CSS .thumb perdida en componente parásito.
- **ui(web)**: Forzada resolución nativa 352x288 en previews MJPEG para evitar distorsión de aspecto.
- **css**: Añadido object-position: center para unificar recorte de miniaturas.

## [v3.9.7-dev.38] - 2026-01-30
### Changed
- **Resolution Upgrade**: Aumento de la resolución de cámara de **352x288 (CIF)** a **640x480 (VGA)**.
    - **Back-end**: Actualizados parámetros de  y lógica de selección de "Mejor Tamaño" en .
    - **Visuals**: La imagen en el LCD del dispositivo ahora coincide 1:1 con la resolución vertical física (480px), eliminando el difuminado por interpolación y mejorando drásticamente la nitidez del Zoom Hardware.
    - **Web**: El Zoom Digital en navegador (x2, x3) se beneficia de la triple densidad de píxeles (~300k vs ~100k).

## [v3.9.7-dev.38] - 2026-01-30
### Changed
- **Resolution Upgrade**: Aumento de la resolución de cámara de **352x288 (CIF)** a **640x480 (VGA)**.
    - **Back-end**: Actualizados parámetros de Camera.Parameters y lógica de selección de "Mejor Tamaño" en SentinelService.
    - **Visuals**: La imagen en el LCD del dispositivo ahora coincide 1:1 con la resolución vertical física (480px), eliminando el difuminado por interpolación y mejorando drásticamente la nitidez del Zoom Hardware.
    - **Web**: El Zoom Digital en navegador (x2, x3) se beneficia de la triple densidad de píxeles (~300k vs ~100k).
