
### ✅ v3.9.5-dev.21: Release Candidate - Estabilidad Térmica y Visual Confirmada

**Estado**: 🚀 **ÉXITO TOTAL**

**Verificación en Dispositivo Real**:
- ✅ **Hot-Swap Rotation**: Cambio de rotación 0° ↔ 180° desde la web **OK**. La cámara se reinicia y recupera la imagen en pantalla sin congelarse gracias al `activeSurfaceHolder`.
- ✅ **Fluidez UI**: La Activity muestra la cámara fluida tras cada activación por movimiento.
- ✅ **Temperatura**: Estable en **38°C** en modo vigilancia (vs 42-44°C anteriores). El "Pintor Vago PRO" (0.5 FPS) está funcionando.

**Resumen de Cambios (La Solución Definitiva)**:
1. **Amnesia Fix**: Variable `static activeSurfaceHolder` en `SentinelService` evita que el servicio pierda la pantalla al reiniciarse.
2. **Pintor Vago PRO**: Lógica `if (now - lastLazyTime < 2000) return;` impone un límite físico de 0.5 FPS al procesado de frames en reposo.
3. **UI Rescue**: `MATCH_PARENT` en `onCreate` evita condiciones de carrera.

**Próximos Pasos**:
- Monitorear estabilidad a largo plazo (24h+).
- Disfrutar de un "Abuelo" más fresco y estable. 🧊👴
