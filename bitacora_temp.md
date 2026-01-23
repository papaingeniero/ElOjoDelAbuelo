
### 🟢 v3.9.5-dev.20: UI Rescue (Éxito Parcial) - Cámara Fluida con Bug de Rotación

**Estado**: 🟢 **SOLUCIONADO (Con Notas)**

**Resultado del Fix [B006]**:
Mover la configuración de Layout (`MATCH_PARENT`) a `onCreate` ha resuelto el problema base.
- ✅ **Arranque en frío**: La cámara se ve a pantalla completa y fluida (FPS correctos).
- ✅ **Estabilidad**: No hay crashes inmediatos.

**Nuevo Hallazgo (Bug de Rotación en Caliente)**:
El usuario reporta un comportamiento específico relacionado con el cambio de configuración en tiempo de ejecución:
1.  Si se cambia la rotación (0° ↔ 180°) desde la Web (Preferencias) mientras la app corre.
2.  Al ocurrir el siguiente evento de grabación (pantalla ON), la imagen se **CONGELA** en el primer frame.
3.  **Workaround**: Reiniciar la app (`Kill` + `Start`) aplica la rotación correctamente y la cámara vuelve a verse fluida.

**Diagnóstico**:
El cambio de parámetros de cámara en caliente (`setParameters`) para la rotación podría estar desincronizando el buffer del `SurfaceView` en este hardware legacy, similar a lo que pasaba con el layout. Al reiniciar, la configuración se carga desde cero limpiamente.

**Conclusión**:
La versión es funcional para operación normal. El cambio de rotación requerirá un reinicio manual de la app hasta que se implemente un reinicio suave de la cámara más robusto.
