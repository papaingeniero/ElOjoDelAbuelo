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
3.  **Compila y Despliega**:
    *   `./gradlew assembleDebug`
    *   `adb install -r app/build/outputs/apk/debug/app-debug.apk`
    *   `adb shell am start -n com.elojodelabuelo.rescue/com.elojodelabuelo.MainActivity`

## 2. Gestión de Fallos (Si el usuario dice "No funciona")
1.  **STOP**: No corrijas el código todavía.
2.  **COMMIT DEL ERROR**: Guarda el estado actual para la posteridad.
    *   `git add .`
    *   `git commit -m "chore(debug): Snapshot vX.Y.Z-dev FAILED - [Descripción del Fallo]"`
    *   *Objetivo*: Que el fallo quede registrado en la historia de Git.
3.  **CORRIGE E INCREMENTA SUFIJO**:
    *   Ahora aplica el fix en el código.
    *   Sube el sufijo en `build.gradle` (vX.Y.Z-dev.N+1).
    *   Vuelve al punto 1 (Desplegar).

## 3. Éxito
1.  Si el usuario valida el snapshot:
    *   Procede al Commit final (`feat: ...`) o Release.
