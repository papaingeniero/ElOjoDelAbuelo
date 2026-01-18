---
description: Protocolo para realizar cambios en la "Inteligencia del Agente" (Workflows, Reglas)
---

# Workflow: Cambio Estratégico (Meta-Ingeniería)

Usa este workflow cuando modifiques archivos en `.agent/` o cambies la forma de trabajar. Estos cambios son tan importantes como el código fuente.

## 1. Validación de Impacto
1.  **Reflexiona**: ¿Por qué cambio esto?
    *   ¿Hemos aprendido algo nuevo?
    *   ¿Algo funciona mal en el proceso actual?
2.  **Versionado (OBLIGATORIO)**:
    *   Cualquier mejora en la inteligencia del agente merece un `checkpoint`.
    *   Calcula la siguiente versión dev: `vX.Y.Z-dev.N` -> `vX.Y.Z-dev.N+1`.
    *   Actualiza `app/build.gradle`.

## 2. Ejecución del Cambio
1.  Modifica los archivos necesarios (`.agent/workflows/*.md`, `legacy_dev_rules.md`, etc.).
2.  Verifica que las instrucciones sean precisas y no contradictorias.

## 3. El Commit Pedagógico (Regla de Oro)
Los commits de tipo `meta` son lecciones para los estudiantes.
*   **Subject**: `meta: <Descripción corta del cambio de proceso>`
*   **Body**:
    *   Explica el **PROBLEMA** del proceso anterior ("No sabíamos qué snapshot falló...").
    *   Explica la **SOLUCIÓN** estratégica ("Introducimos Arqueología en el release...").
    *   Menciona qué regla o principio de ingeniería se aplica.

## 4. Cierre
*   `git add .`
*   `git commit -m "meta: ..." -m "..."`
*   `git push origin main`
