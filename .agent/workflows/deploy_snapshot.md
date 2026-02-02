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
    - **⚠️ OBLIGATORIO: AÑADIR AL PRINCIPIO (PREPEND)**.

# 5. Git Snapshot
- [ ] Generar archivo temporal `commit_msg.txt` con el contenido de la Bitácora.
- [ ] `git add .`
- [ ] `git reset commit_msg.txt` (⚠️ IMPRESCINDIBLE: Saca el archivo temporal del stage para no ensuciar el repo).
- [ ] `git commit -F commit_msg.txt`
- [ ] `rm commit_msg.txt`
- [ ] `git push origin main`.

# 6. Verificación de 7 Puntos (Reporte Final)
Generar reporte estructurado:
1. **Tabla de Verificación de 7 Puntos**:
   - Versión, Build, Install, Bitácora, Changelog, Commit y Push, Status.
2. **Resumen de Cambios**:
   - Lista de cambios técnicos y funcionales.
3. **Reporte de Incidentes y Resoluciones (OBLIGATORIO)**:
   - Detalle de CADA error encontrado en cualquier paso (1-7) y su solución.
   - Formato: `❌ Error: [Descripción] -> ✅ Solución: [Acción tomada]`.