
### ⚡ v3.9.5-dev.1: Lazy Load (Infinite Scroll)

El usuario reportó saturación de CPU al generar la lista HTML de cientos de videos.
**La Solución**:
Implementación de carga diferida (Lazy Load):
1.  **Backend**: Nueva API `/api/list_videos` que pagina los resultados y extrae metadatos solo de los nombres de archivo (Regex), evitando I/O costoso.
2.  **Frontend**: Carga inicial vacía + `IntersectionObserver` que pide bloques de 10 videos al hacer scroll.
3.  **Javascript**: Lógica de renderizado de tarjetas en cliente y fix de compatibilidad de clicks.

Resultado: Carga inicial instantánea y navegación fluida.
