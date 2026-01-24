
### 👻 v3.9.5-dev.25: "Ghost Hunter" (CSI 3.0) - Filtro Inteligente

**Estado**: ❌ **FALLIDO** (Ajuste de Umbral Requerido)

**Resultado Real**:
- El fantasma logró burlar el filtro con un **Score de 2200** (inferior al umbral teórico de 2500).
- Se confirmó que el "cebollazo" no siempre es >3000, sino que puede oscilar.

**Lección Aprendida**:
- El umbral de 2500 fue demasiado optimista.
- El movimiento humano típico (caminar) suele rondar los 400-800. Un score de 2200 sigue siendo masivo para algo sutil, pero el filtro debe ser más estricto.

**Próxima Iteración Sugerida**:
- Bajar el umbral de corte a **1500** o incluso **1200**.
- Analizar si un humano moviéndose rápido puede generar 2200 (falsos negativos).
