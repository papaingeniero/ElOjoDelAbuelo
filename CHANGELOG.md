# Changelog

All notable changes to the "El Ojo Del Abuelo" project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
