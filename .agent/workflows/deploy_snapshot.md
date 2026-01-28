---
description: Protocolo para desplegar una versión de prueba (Snapshot) en el dispositivo
---

Este workflow describe los pasos críticos para desplegar una Snapshot de desarrollo.
Sigue el orden estricto.

# 1. Alineación del Agente
- [ ] Leer y comprender `legacy_dev_rules.md`.
- [ ] Confirmar que no hay ambigüedades.

# 2. Identidad (Versionado)
- [ ] Incrementar `versionName` en `app/build.gradle`.
    - Formato: `vX.Y.Z-dev.N` (incrementar N).
    - **Regla**: Si hay cambios de código, SUBE la versión.
- [ ] Verificar que `git status` está limpio (excepto build.gradle y los cambios actuales).

# 3. Compilación y Despliegue
- [ ] Ejecutar `./gradlew assembleDebug`.
- [ ] Instalar APK: `adb install -r app/build/outputs/apk/debug/app-debug.apk`.
- [ ] (Opcional) Lanzar app: `adb shell am start -n com.elojodelabuelo/.MainActivity`.

# 4. Documentación (Bitácora)
- [ ] Actualizar `BITACORA.md`.
    - Añadir entrada al final (APPEND).
    - Usar formato rico (Iconos, Storytelling, Lecciones).
    - Incluir la "Lección del Día".
- [ ] Actualizar `CHANGELOG.md`.
    - Resumen técnico breve.

# 5. Git Snapshot
- [ ] `git add .`
- [ ] `git commit -m "vX.Y.Z-dev.N <tipo>: <descripción>"`
    - Cuerpo del commit: Copia LITERAL de la entrada de Bitácora.
- [ ] `git push origin main`.
- [ ] `git tag vX.Y.Z-dev.N`
- [ ] `git push origin vX.Y.Z-dev.N`

# 6. Verificación de 8 Puntos (Reporte Final)
Generar tabla final:
1. Versión: [vX.Y.Z-dev.N]
2. Build: [✅ OK]
3. Install: [✅ OK]
4. Bitácora: [✅ Updated]
5. Changelog: [✅ Updated]
6. Commit: [Hash]
7. Push: [✅ OK]
8. Status: [Clean]
