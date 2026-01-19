
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
