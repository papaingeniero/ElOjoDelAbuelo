# Backlog - El Ojo del Abuelo

Este documento registra funcionalidades pendientes, bugs conocidos y mejoras técnicas del proyecto.

---

## 🔴 P0 - Crítico (Bloquean Funcionalidad Core)

### 🐛 Bugs
*Ninguno actualmente.*

### ✨ Features
*Ninguna actualmente.*

---

## 🟡 P1 - Alta Prioridad (Mejoran UX/Estabilidad)

### 🐛 Bugs
- [ ] **[B001]** **Click events dejan de funcionar tras crear tarjeta AJAX en tiempo real**
  - **Síntoma**: Cuando se detecta movimiento y se crea dinámicamente una nueva tarjeta de video vía AJAX, todos los eventos `onclick` de las tarjetas (nuevas y existentes) dejan de responder. La página se vuelve completamente no-interactiva.
  - **Contexto**: Ocurre en el dashboard web (`NanoHttpServer.java` → `generateDashboardHtml()`).
  - **Hipótesis**: Posible conflicto entre event listeners estáticos y DOM dinámico. Las tarjetas creadas con `innerHTML` no heredan los handlers de click, y algo en el proceso rompe los listeners existentes.
  - **Impacto**: Alto - Impide reproducir videos después de la primera detección de movimiento.
  - **Archivos Relacionados**: `NanoHttpServer.java` (JavaScript inline, función `renderCards()` y lógica AJAX de actualización en tiempo real)

- [ ] **[B002]** **Modal "VER CÁMARA EN VIVO" no aplica zoom/pan del navegador (webZoom, webPanX, webPanY)**
  - **Síntoma**: Al abrir el modal de stream en vivo, la imagen de la cámara se muestra sin aplicar las preferencias de zoom y pan guardadas (`webZoom`, `webPanX`, `webPanY`).
  - **Contexto**: Las preferencias están guardadas como porcentajes para ser reutilizables en miniaturas, reproducción de videos grabados y stream en vivo. Funcionan correctamente en miniaturas y videos grabados, pero NO en el modal de cámara en vivo.
  - **Comportamiento Esperado**: El stream en vivo debería aplicar automáticamente `transform: scale(webZoom) translate(webPanX%, webPanY%)` al `<img>` del modal.
  - **Impacto**: Medio - El usuario ve la cámara en vivo sin el encuadre configurado, obligándole a ajustar manualmente cada vez.
  - **Archivos Relacionados**: `NanoHttpServer.java` (HTML/CSS/JS del modal de stream en vivo)





### ✨ Features
- [ ] **[F001]** **Controles táctiles interactivos en modal de stream en vivo**
  - **Descripción**: Implementar la misma funcionalidad de zoom/pan interactivo que existe en la reproducción de videos grabados, pero para el modal "VER CÁMARA EN VIVO".
  - **Gestos a Implementar**:
    - **Pinch (2 dedos)**: Zoom in/out dinámico
    - **Doble tap/click**: Reset a zoom/pan por defecto (`webZoom`, `webPanX`, `webPanY`)
    - **Drag (1 dedo)**: Desplazar la imagen (pan)
  - **Beneficio**: Paridad de UX entre stream en vivo y reproducción de videos. Permite al usuario explorar la imagen en tiempo real con los mismos controles familiares.
  - **Complejidad Estimada**: Media - Reutilizar lógica existente del reproductor de videos grabados.
  - **Archivos Relacionados**: `NanoHttpServer.java` (JavaScript del modal de stream, touch event handlers)

### 🔧 Mejoras Técnicas
- [ ] **[T001]** Placeholder: Ejemplo de refactor o mejora técnica

---

## 🟢 P2 - Baja Prioridad (Nice to Have)

### ✨ Features
- [ ] **[F100]** Placeholder: Feature futura de baja prioridad

### 🔧 Mejoras Técnicas
- [ ] **[T100]** Placeholder: Optimización no urgente

---

## 💡 Ideas / Backlog Futuro
*Ideas sin priorizar aún:*
- Ejemplo: Integración con servicio X
- Ejemplo: Soporte para dispositivo Y

---

## ✅ Completados Recientemente (Últimos 5)

- [x] **[B003]** Regresión de temperatura (42°C→39°C) + Activity crash → Resuelto en `v3.9.5-dev.11`
- [x] **[B000]** AndroidManifest crash por atributos faltantes → Resuelto en `v3.9.5-dev.6`
- [x] **[F000]** Lazy Load para lista de videos → Implementado en `v3.9.5-dev.1`

---

## 📖 Convenciones

### IDs
- **B###**: Bug
- **F###**: Feature
- **T###**: Tech Debt / Mejora Técnica

### Prioridades
- **P0**: Crítico (bloquea funcionalidad)
- **P1**: Alta (mejora significativa)
- **P2**: Baja (nice to have)

### Workflow
1. Añadir item con `[ ]` (pendiente)
2. Al completar, mover a "Completados" con `[x]` y versión
3. Mantener solo últimos 5 completados (archivar resto en BITACORA.md)
