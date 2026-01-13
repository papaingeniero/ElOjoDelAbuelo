# Protocolo de Desarrollo: El Ojo Del Abuelo

Para garantizar la estabilidad y la trazabilidad del proyecto, cada cambio de código debe seguir estrictamente los siguientes 4 pasos antes de considerarse "Listo":

## 1. Documentación Didáctica (Brain & Walkthrough)
*   **Qué**: Escribir en `WALKTHROUGH.md` (y `CHANGELOG.md`) qué se ha hecho.
*   **Cómo**: Explicar los cambios de forma técnica pero comprensible ("Didáctica").
*   **Por qué**: Justificar la razón del cambio (ej. "Para evitar un Crash en Android 2.3 debido a...").

## 2. Control de Versiones (App Versioning)
*   Antes de compilar, incrementar siempre la versión en:
    *   `app/build.gradle` (`versionName`)
    *   `app/src/main/AndroidManifest.xml` (`android:versionName`)
*   *Nota*: Esto permite verificar visualmente en el dispositivo que se está ejecutando la última build.

## 3. Sincronización (Git)
*   Ejecutar `git add .`, `git commit -m "..."`, y `git push origin main`.
*   El código local y remoto deben estar siempre sincronizados tras cada iteración funcional.

## 4. Despliegue y Verificación (Deploy)
*   Construir: `./gradlew assembleDebug`
*   Instalar: `adb install -r ...`
*   Lanzar: `adb shell am start ...`
*   **Verificar**: Confirmar con el usuario que la versión mostrada es la correcta y el bug está resuelto.
