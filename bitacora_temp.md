
### 👻 v3.9.5-dev.26: "Ghost Hunter" (CSI 3.1) - Intento 4 (Umbral 1500)

**Estado**: ⏭️ **SALTADO**

**Razón**:
El usuario decidió implementar un control más granulado antes de seguir ajustando solo el umbral numérico. Se opta por introducir un interruptor maestro (`flag`) para habilitar/deshabilitar la lógica completa sin recompilar.

---

### 👻 v3.9.5-dev.27: "Ghost Hunter Switch" - Control Manual

**Estado**: 🧪 **EN PRUEBAS (SNAPSHOT)**

**Configuración Actual**:
- `useGhostHunter = false` (Desactivado por defecto).
- Lógica de disparo modificada a valores "imposibles" (`delta < 0`, `score > 5500`) para garantizar que NO actúe a menos que se cambie el código o se inyecte la configuración.

**Objetivo**:
Tener una versión base donde el sistema anti-fantasmas está presente pero inactivo, permitiendo activarlo a demanda para pruebas A/B de comportamiento del sensor.
