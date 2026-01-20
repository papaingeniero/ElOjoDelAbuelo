# Contexto de Arquitectura Web: El Ojo del Abuelo

Este documento define el funcionamiento base de la interfaz web para garantizar que el Agente siempre entienda el flujo de usuario y la estructura de la página.

## Funcionamiento de la Página Principal del Navegador

En la página principal del navegador se carga arriba una serie de iconos con información sobre su estado:
*   **Estado**: Vigilando o Grabando (recibido por long polling en tiempo real).
*   **Sensores**: Temperatura del teléfono, batería, y espacio libre en disco (actualizado por AJAX cada 5 segundos).
*   **Configuración**: A la derecha hay un icono de engranaje que abre una **ventana modal** para configurar las preferencias de la aplicación.

### Live View
Debajo, hay un botón de **VER CÁMARA EN VIVO**, que al pincharlo abre una ventana modal con el stream o imagen de la cámara en vivo.

### Galería de Videos
Debajo en la página principal se muestra la lista de videos con todos los videos grabados, cada vez que carga la página.
*   **Tarjeta de Video**: Cada elemento muestra el video animado en miniatura, título, espacio, duración, fecha y hora.
*   **Reproducción**: Al hacer clic en una tarjeta se abre una **ventana modal con el reproductor de video** que reproduce ese video en grande.
*   **Persistencia de Zoom**: El reproductor aplica los valores de zoom y pan pre-establecidos por defecto en preferencias.
