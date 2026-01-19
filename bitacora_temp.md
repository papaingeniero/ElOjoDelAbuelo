
### 🚀 v3.9.2-dev.4: Refinamiento de UX y Desacople Total

Tras el despliegue inicial de la vista dual, detectamos dos fricciones que requerían intervención inmediata ("Polish"):

1.  **Desacople del Reproductor de Video**:
    *   *Problema*: El reproductor de video web (`playVideo`) inicializaba su zoom reseteando a las coordenadas `0,0` o leyendo incorrectamente las variables de hardware, lo que causaba saltos visuales.
    *   *Solución*: Implementación de variables globales en Javascript (`gWebZoom`, `gWebPanX`, `gWebPanY`). Ahora, al abrir un video, se respeta estrictamente la configuración visual definida por el usuario para la web, ignorando por completo el estado del hardware (`defZoom`).

2.  **Tendencias de Temperatura "Pegajosas" (Sticky Trends)**:
    *   *Problema*: Los indicadores de tendencia (▲/▼) desaparecían si la temperatura se mantenía estable durante 5 segundos (el ciclo de refresco), perdiendo el contexto histórico inmediato.
    *   *Solución*: Se introdujo persistencia en la variable `lastTrend`. El icono solo cambia si hay un delta de temperatura real; si la temperatura es igual a la anterior, se *mantiene* el último icono conocido. Esto permite saber si el dispositivo "viene de calentarse" o "viene de enfriarse" incluso en mesetas térmicas.

Esta versión consolida la experiencia de usuario (UX) tanto en control como en monitoreo.
