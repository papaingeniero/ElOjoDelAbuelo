
### 📋 v3.9.5-dev.12: Migración a GitHub Issues (Meta-Ingeniería)

Transición completa del sistema de gestión de tareas de BACKLOG.md local a GitHub Issues nativo.

**El Cambio**:
*   **Regla 11 Actualizada**: Ahora los agentes deben usar `gh` CLI para consultar (`gh issue list`), crear (`gh issue create`) y cerrar (`gh issue close`) issues.
*   **Templates Estandarizados**: Documentados en la regla para bugs (label: bug) y features (label: enhancement).
*   **Migración Completa**: Las 7 issues del BACKLOG.md fueron creadas en GitHub:
    *   **Abiertas**: #1 (B001), #2 (B002), #3 (B004), #4 (F001)
    *   **Cerradas**: #5 (B003), #6 (B000), #7 (F000)
*   **BACKLOG.md**: Archivado como `.archived` y restaurado para referencia local opcional.

**Beneficio**:
Centralización en GitHub Issues permite mejor trazabilidad, integración con commits (ej: `Fixes #3`), y colaboración más fluida. Los agentes ahora tienen una fuente de verdad única y sincronizada.
