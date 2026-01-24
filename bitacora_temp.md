
### 👻 v3.9.5-dev.25: "Ghost Hunter" (CSI 3.0) - Filtro Inteligente de Score

**Estado**: 🧪 **EN PRUEBAS (SNAPSHOT)**

**Nueva Estrategia (La Solución David)**:
Abandonamos la idea de "tiempos muertos" (ceguera) y pasamos a **inteligencia de señal**.

**Lógica Implementada**:
- **Ventana de Vigilancia**: 30 segundos tras cada grabación (`delta < 30000`).
- **Discriminador**: Si `score > 2500` (cebollazo masivo) → **BLOQUEO Y FOTO**.
- **Paso Libre**: Si `score <= 2500` (movimiento humano normal ~700) → **GRABAR SIEMPRE**, incluso si han pasado 0.1 segundos.

**Hipótesis**:
El "fantasma" es un pico de ruido masivo (>3000) provocado por el reajuste del sensor. El movimiento humano real es mucho más sutil. Este filtro debería matar al fantasma sin dejar ciego al Abuelo ante un intruso real rápido.

**Evidencia Forense**:
Los bloqueos guardarán una foto en `/sdcard/ElOjoDelAbuelo/DebugGhost/GHOST_Flash_...jpg` para confirmar visualmente qué es el "cebollazo".
