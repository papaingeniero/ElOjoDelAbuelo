
### 🛡️ v3.9.5-dev.6: Blindaje del AndroidManifest (Meta-Ingeniería)

Tras una investigación exhaustiva comparando v3.9.2 (funcionaba) con v3.9.5 (crasheaba), descubrimos que **la liturgia de cámara era idéntica**. El problema estaba en el `AndroidManifest.xml`.

**El Hallazgo**:
Durante optimizaciones anteriores, eliminamos los atributos `android:versionCode` y `android:versionName` del Manifest porque Gradle los ignora. Sin embargo, el runtime de Android 2.3/4.x (CyanogenMod en el Galaxy S GT-I9000) **requiere estos campos** para resolver correctamente las Activities mediante el ClassLoader.

**La Solución**:
1.  Restauramos el `AndroidManifest.xml` exactamente como estaba en v3.9.2.
2.  Creamos la **Regla 10** en `legacy_dev_rules.md` para blindar estos atributos:
    *   🚫 NUNCA eliminar `android:versionCode` ni `android:versionName`
    *   Mantenerlos como valores fijos legacy (`1` y `3.2.1-debug20d`)
    *   El versionado real sigue en `build.gradle`

**Lecciones Aprendidas**:
*   Los dispositivos legacy tienen dependencias ocultas en campos que builds modernos ignoran.
*   Documentar restricciones de hardware en las reglas del agente previene regresiones futuras.
