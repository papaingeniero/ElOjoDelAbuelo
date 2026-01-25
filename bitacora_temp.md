

## 🚀 Release v3.9.6: "The Bunker" (Stability & Cool Down) | 2026-01-25

**La Consolidación del Búnker**:
Esta versión transforma al "Abuelo" de un prototipo funcional a una herramienta de vigilancia robusta y "fail-safe". Hemos atacado los dos mayores enemigos: **Calor** e **Inestabilidad**.

### 1. La Batalla Térmica (De 41°C a la Estabilidad) ❄️
Descubrimos que el driver de Samsung es "entusiasta" y corre a 30 FPS aunque no se lo pidas.
*   **FPS Throttle**: Intentamos métodos agresivos (Kamikaze) pero el driver se resistió. Optamos por la diplomacia: solicitamos rangos variables (15-30) y dejamos que el "Old School" API gestione.
*   **Hardware Zoom vs LCD Pan**:
    *   **Decisión**: Usar el ISP (Hardware) para hacer el zoom en lugar de recortar bitmaps con la CPU.
    *   **Coste**: Se pierde la capacidad de hacer "Pan & Zoom" táctil en la pantalla del móvil (SurfaceView), ya que la imagen que llega a la memoria ya está recortada por el hardware.
    *   **Beneficio**: Bajada drástica de carga de CPU. El Ojo prioriza la vigilancia web sobre la visualización local.

### 2. Arquitectura "Fail-Safe" (A prueba de balas) 🛡️
*   **Watchdog (Dead Man's Switch)**: Si el WiFi falla o el cliente cierra el navegador "mal", el servidor corta el stream en 5 segundos.
*   **Socket Guillotine**: Un truco sucio pero efectivo en JS (asignar un píxel 1x1 base64) para obligar a Chrome a soltar el socket TCP inmediatemente.
*   **Anti-Amnesia**: El driver tiene "pérdida de memoria a corto plazo" cuando apagas la pantalla. Ahora le recordamos imperativamente qué zoom debe tener cada vez que despierta (Retardo Táctico de 1.5s).

**Estado Final**:
Un sistema que puede ser abandonado en un cajón durante días sin calentarse por conexiones fantasma ni perder su configuración al dormir.

