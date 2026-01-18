---
description: Protocolo para desplegar una versión de prueba (Snapshot) en el dispositivo
---

# Workflow: Desplegar Snapshot (Dev)

Usa este workflow SIEMPRE que necesites probar cambios en el dispositivo sin crear una Release oficial.

## 1. Configuración de Versión
1.  **Calcula la Versión Dev**:
    *   Mira la última versión en `build.gradle` (ej: `3.8.0`).
    *   Incrementa el PATCH o MINOR según corresponda.
    *   AÑADE OBLIGATORIAMENTE el sufijo `-dev` (ej: `3.9.0-dev`).
2.  **Edita `app/build.gradle`**:
    *   Actualiza `versionName` con la versión calculada (ej: `3.9.0-dev`).
    *   *Nota*: NO toques el `versionCode` ni otros campos.

## 2. Compilación e Instalación
1.  **Limpia y Compila**:
    *   Command: `./gradlew assembleDebug`
2.  **Instala**:
    *   Command: `adb install -r app/build/outputs/apk/debug/app-debug.apk`
3.  **Ejecuta**:
    *   Command: `adb shell am start -n com.elojodelabuelo.rescue/com.elojodelabuelo.MainActivity`

## 3. Verificación
1.  **Confirma**: Pide al usuario que verifique que la app muestra la versión terminada en `-dev`.
