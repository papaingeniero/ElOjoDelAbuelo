---
description: Protocolo para desplegar una versión de prueba (Snapshot) en el dispositivo
---

# Workflow: Desplegar Snapshot (Iterativo - Protocolo Científico)

Usa este workflow para pruebas. TRATA CADA SNAPSHOT COMO UN EXPERIMENTO. Si falla, documéntalo antes de arreglarlo.

## 1. El Ciclo de Prueba
1.  **Calcula la Versión Dev**:
    *   Inicio de Feature: `vX.Y.Z-dev.1` (Ej: `v3.9.0-dev.1`).
    *   Corrección tras fallo: Mantén la base, incrementa el sufijo: `vX.Y.Z-dev.2`, `vX.Y.Z-dev.3`...
    *   *Objetivo*: No "quemar" números de versión finales en pruebas internas. SemVer: `dev.x` < `Release`.
2.  **Edita `app/build.gradle`**:
    *   Actualiza `versionName`.
// turbo
3.  **Compila y Despliega**:
    *   `./gradlew assembleDebug`
    *   `adb install -r app/build/outputs/apk/debug/app-debug.apk`
    *   `adb shell am start -n com.elojodelabuelo.rescue/com.elojodelabuelo.MainActivity`

## 2. Gestión de Fallos (Si el usuario dice "No funciona")
1.  **STOP**: No corrijas el código todavía.
2.  **REGISTRO FORENSE (OBLIGATORIO)**:
    *   Edita `BITACORA.md`: Añade sección `### ❌ Intento Fallido (vX.Y.Z-dev.N): [Por qué falló]`. (Regla 5)
3.  **COMMIT DEL ERROR**: Guarda el estado actual para la posteridad.
    *   `git add .`
    *   `git commit -m "vX.Y.Z-dev.N chore(debug): FAILED - [Descripción del Fallo]" -m "$(cat BITACORA.md | tail -n 10)"`
    *   `git push origin main`
    *   *Objetivo*: Que el fallo y su análisis queden registrados en la historia de Git y suban inmediatamente a la nube según la Sync Policy.
4.  **CORRIGE E INCREMENTA SUFIJO**:
    *   Aplica el fix en el código.
    *   Sube el sufijo en `build.gradle` (vX.Y.Z-dev.N+1).
    *   Vuelve al punto 1 (Desplegar).

## 3. Éxito y Cierre (Protocolo de Cierre Cuaternario - Regla 7)
Si el usuario valida el funcionamiento:

1.  **Ejecutar Protocolo**:
    *   [ ] **Incrementar versión** en `build.gradle` (vX.Y.Z-dev.N+1). (Si aplica nueva iteración)
    *   [ ] **Actualizar BITACORA.md**: Añadir entrada final `### 🚀 vX.Y.Z-dev.N: [Solución Definitiva]` + Detalles técnicos.
    *   [ ] **Actualizar CHANGELOG.md**: Resumen técnico.
    *   [ ] **Commit**:
        `git add .`
        `git commit -m "vX.Y.Z-dev.N <tipo>: <descripción>" -m "$(cat BITACORA.md | tail -n 20)"`
2.  **Push Final**: `git push origin main`.
3.  **Reporte Final Estandarizado (Verificación de 8 Puntos)**:
    Generar el reporte final verificando:
    1. Versión (`build.gradle`)
    2. Compilación y Despliegue (OK)
    3. Ejecución en Dispositivo (Corriendo)
    4. Bitácora (Updated)
    5. Changelog (Updated)
    6. Commit (Hash)
    7. Push (Main)
    8. Git Status (Clean)
