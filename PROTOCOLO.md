# PROTOCOLO DE DESARROLLO Y DESPLIEGUE - EL OJO DEL ABUELO

Este documento define el estándar de calidad y el flujo de trabajo obligatorio para cualquier modificación en el proyecto.

## 1. REGLAS DE ORO (HARDWARE LEGACY)
*   **Dispositivo Objetivo**: Samsung Galaxy S (GT-I9000).
*   **Sistema Operativo**: Android 2.3.3 (Gingerbread) - API Level 10.
*   **Restricción de Memoria**: Máximo cuidado con la RAM (512MB). Evitar `new` en bucles críticos (`onPreviewFrame`).
*   **Restricción de CPU**: Un solo núcleo a 1GHz.
    *   **Zero CPU**: Usar `SurfaceView` directo para preview siempre que sea posible.
    *   **Hybrid Mode**: Si se requiere procesamiento (Motion/Web), usar buffers manuales (`addCallbackBuffer`).

## 2. FLUJO DE IMPLEMENTACIÓN
1.  **Análisis**: Entender el impacto en CPU y Memoria antes de codificar.
2.  **Código Defensivo**: Todo bloque crítico (Cámara, I/O) debe tener `try-catch` para evitar crasheos fatales.
3.  **Compatibilidad**: Usar APIs compatibles con API 10 (ej: `SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS`).

## 3. PROCESO DE RELASE (CIERRE DE TAREA)
Cada vez que se complete una funcionalidad o arreglo (éxito confirmado), se deben ejecutar estos pasos **SIN EXCEPCIÓN**:

### A. Versionado
*   Incrementar `versionName` en `app/build.gradle`.
*   Formato: `vX.Y.Z` (SemVer).

### B. Changelog
*   Editar `CHANGELOG.md`.
*   Añadir entrada bajo la nueva versión con la fecha de hoy.
*   Categorías: `Added`, `Changed`, `Fixed`, `Removed`.

### C. Documentación Técnica
*   Si la arquitectura cambia (como "Hardware Preview"), crear un documento específico en `docs/` explicando el "CÓMO" y el "POR QUÉ".
*   **WALKTHROUGH DE SESIÓN**: Copiar SIEMPRE el archivo `walkthrough.md` (generado por el Agente) a la raíz del proyecto como `WALKTHROUGH.md`.
    *   Este archivo debe reflejar los detalles técnicos específicos de la versión actual.
*   Referenciar estos documentos en el chat final.

### D. Control de Versiones (Git)
*   Añadir todos los archivos modificados.
*   Mensaje de Commit obligatorio: `tipo: Descripción breve vX.Y.Z`
    *   Ejemplo: `feat: Zero CPU Architecture implemented v3.3.0`

## 4. LISTA DE VERIFICACIÓN FINAL
- [ ] ¿Compila sin errores?
- [ ] ¿Se ha probado en el dispositivo real?
- [ ] ¿Se ha actualizado el número de versión?
- [ ] ¿Esta el Changelog al día?
- [ ] ¿El archivo WALKTHROUGH.md ha sido actualizado en la raíz?
- [ ] ¿El código respeta la memoria y CPU del i9000?

---
*Este archivo es la fuente de la verdad para el comportamiento del Agente.*
