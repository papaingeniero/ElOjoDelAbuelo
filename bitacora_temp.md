
### 🧪 v3.9.5-dev.8: Validación de Hipótesis (Manifest versionName)

Probamos cambiar `android:versionName` de `"3.2.1-debug20d"` a `"legacy-compat"` para confirmar nuestra hipótesis:
*   **Hipótesis**: El ClassLoader de Android Legacy necesita que los atributos `versionCode` y `versionName` EXISTAN, pero no le importa su VALOR.
*   **Resultado**: ✅ Confirmado. La Activity se mantiene abierta con el nuevo valor.

Este cambio estandariza el Manifest con un nombre más semántico (`legacy-compat`) que comunica claramente su propósito: compatibilidad con dispositivos antiguos.
