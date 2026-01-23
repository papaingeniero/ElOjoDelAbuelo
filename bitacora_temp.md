
### 📊 v3.9.5-dev.16: Trazas de Ciclo de Vida en MainActivity

**Mejora del Sistema de Caja Negra**:
Se añaden 3 trazas adicionales en `MainActivity.java` para completar la visibilidad del ciclo de vida de la UI:

*   `onCreate` → `"MainActivity: CREATED"` (La pantalla intenta arrancar)
*   `onResume` → `"MainActivity: RESUMED (Visible)"` (La pantalla se ve)
*   `onPause` → `"MainActivity: PAUSED (Background)"` (Sistema mata UI o pantalla apagada)

**Secuencia de Diagnóstico Esperada**:
```
Sentinel: CREATING... (El servicio vive)
MainActivity: CREATED (La pantalla intenta arrancar)
MainActivity: RESUMED (La pantalla se ve)
Sentinel: Surface ATTACHED (La cámara se conecta)
... (pasa el tiempo) ...
MainActivity: PAUSED -> Sentinel: Surface DETACHED
```

**Valor Añadido**:
Con esta secuencia podremos identificar exactamente en qué punto del ciclo de vida muere la Activity tras horas de funcionamiento.
