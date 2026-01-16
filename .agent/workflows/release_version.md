---
description: Proceso estandarizado para publicar una nueva versión (Release)
---

# Workflow: Publicar Nueva Versión

Sigue estos pasos estrictamente cuando el usuario solicite una "Release" o "Nueva Versión".

## 1. Preparación
1.  Pregunta al usuario el tipo de cambio (Fix, Feature, Hotfix) para decidir el número de versión.
2.  Confirma que el código compila y ha sido probado (según lo que diga el usuario).

## 2. Versionado
1.  Edita `app/build.gradle`:
    *   Incrementa `versionName` al nuevo número (ej: "3.3.2").
2.  Edita `CHANGELOG.md`:
    *   Añade una nueva entrada con la fecha de hoy y la versión.
    *   Describe los cambios bajo `### Added`, `### Fixed`, o `### Changed`.
3.  Edita `WALKTHROUGH.md` (Raíz):
    *   Actualiza el contenido para reflejar el estado actual del proyecto.

## 3. Control de Versiones (Git)
1.  **Stage**: `git add .`
2.  **Commit**:
    *   Mensaje sugerido: `tipo: Descripción breve vX.Y.Z`
    *   Ejemplo: `fix: Corrección de memoria v3.3.2`
    *   COMENTARIO: Pide confirmación al usuario antes de hacer el commit real.
3.  **Push (Rama)**:
    *   `git push origin main`
    *   COMENTARIO: Pide confirmación explícita.

## 4. Congelado (Tagging)
1.  Crea la etiqueta oficial:
    *   `git tag vX.Y.Z` (ej: `git tag v3.3.2`)
2.  Sube la etiqueta:
    *   `git push origin vX.Y.Z`
    *   ⚠️ Importante: Asegúrate de pushear el tag explícitamente.

## 5. Cierre
1.  Informa al usuario de que la versión vX.Y.Z está publicada y segura.
