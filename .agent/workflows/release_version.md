---
description: Proceso estandarizado para publicar una nueva versión (Release)
---

# Workflow: Publicar Nueva Versión

Sigue estos pasos estrictamente cuando el usuario solicite una "Release" o "Nueva Versión".
Este workflow implementa las salvaguardas definidas en `legacy_dev_rules.md`.

## 1. Safety Check (Semáforo)
1.  **Ejecuta**: `git status --porcelain`
2.  **Verifica**:
    *   Si hay salida (archivos modificados o unstaged antes de empezar): **STOP**. Se aplica la Regla del Semáforo Rojo.
    *   Exige al usuario consolidar los cambios pendientes antes de iniciar la Release.
    *   Solo continúa si el output está vacío (clean state).

## 2. Preparación y Documentación
1.  **Arqueología (Memoria Histórica)**:
    *   Ejecuta `git log --oneline --decorate --graph -n 20` (o busca `grep="FAILED"`).
    *   Identifica snapshots fallidos (`chore(debug): ... FAILED`) desde la última versión.
    *   **Obligatorio**: Si hubo fallos, documéntalos en la Bitácora (Sección "Cronología/Intentos").
2.  **Bitácora (Fuente de Verdad)**:
    *   Edita `BITACORA.md` (append).
    *   Añade la crónica completa de los cambios de esta versión.
    *   **Formato**: `## 🚀 Phase X: Título | Fecha: DD de Mes de YYYY`.
    *   **IMPORTANTE**: Guarda este mismo texto también en un archivo temporal `bitacora_temp.md` para usarlo en el tag.
    *   Este texto SERÁ el cuerpo del commit, así que esfuérzate.
2.  **Versionado**:
    *   Edita `app/build.gradle`: Incrementa `versionName`.
    *   Edita `CHANGELOG.md`: Añade entrada estandar.

## 3. Ejecución de Release (Git)
1.  **Stage**: `git add .`
    *   Esto capturará: build.gradle, CHANGELOG, BITACORA y el código modificado.
2.  **Commit (Sincronizado)**:
    *   *Subject*: `tipo: Descripción breve vX.Y.Z`
    *   *Body*: **COPIA LITERAL** del texto añadido a `BITACORA.md`.
    *   Ejecuta el commit.
3.  **Tag (Enriquecido)**:
    *   Prepara el mensaje del tag combinando la Bitácora y el Changelog.
    *   Comando estructurado: `git tag -a vX.Y.Z --cleanup=verbatim -m "Release vX.Y.Z" -m "$(cat bitacora_temp.md)" -m "$(cat CHANGELOG.md)"`
    *   Elimina el temporal: `rm bitacora_temp.md`
    *   Asegúrate de que la info sea rica y legible.
    *   Push: `git push origin vX.Y.Z`
4.  **Push Main**:
    *   `git push origin main`

## 4. Cierre
1.  Confirma que `git status` vuelve a estar limpio.
2.  Informa al usuario: "Release vX.Y.Z desplegada, documentada y etiquetada."
