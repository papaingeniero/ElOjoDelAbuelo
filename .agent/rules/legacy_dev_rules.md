---
trigger: always_on
glob:
description:
---
# Reglas de Desarrollo: El Ojo del Abuelo (Legacy)

Estas reglas son MANDATORIAS y deben ser seguidas por el agente en cada interacción.

## 1. Autorización y Seguridad (CRÍTICO)
*   🔴 **SIN PERMISO, NO SE TOCA**: NUNCA modificar código, ejecutar comandos de Git, ni subir cambios a GitHub sin pedir autorización explícita al usuario primero.
*   🇪🇸 **Idioma**: Todo el proyecto (Commits, Docs, Código) debe estar en **ESPAÑOL**.
*   👀 **Lectura Permitida**: Eres libre de leer archivos, listar directorios y consultar el estado de Git proactivamente.

## 2. Restricciones Físicas (Hardware Legacy)
El dispositivo objetivo es un **Samsung Galaxy S (GT-I9000)**.
*   **RAM**: Máximo 512 MB.
    *   🚫 **PROHIBIDO**: Crear objetos (`new`) dentro de bucles críticos como `onPreviewFrame`. Genera stuttering por Garbage Collection.
    *   ✅ **OBLIGATORIO**: Reutilizar objetos y usar Buffers estáticos.
*   **CPU**: Single Core 1GHz.
    *   **Arquitectura Zero CPU**: La prioridad absoluta es usar `SurfaceView` directo para que el hardware de cámara pinte en pantalla sin pasar por la CPU.
    *   **Modo Híbrido**: Si se necesita procesamiento (Motion/Web), usar `addCallbackBuffer` manualmente para minimizar impacto.

## 3. Estilo y Estabilidad
*   **Código Defensivo**: Todo bloque que toque Hardware (Cámara, Sensores) o I/O debe estar envuelto en `try-catch`. Un crash en un servicio de seguridad es inaceptable.
*   **Compatibilidad**:
    *   Usar APIs compatibles con **API Level 10** (Android 2.3) siempre que sea posible.
    *   Mantener `minSdk 19` (Android 4.4) solo porque usamos una ROM CyanogenMod específica.

## 4. Estándar de Git (Enfoque Educativo)
Como proyecto Open Source didáctico, el historial de Git es nuestro libro de texto.
*   **Título (Subject)**: Corto, técnico e Imperativo. `<tipo>: <descripción breve>` (Max 50 chars).
*   **Cuerpo (Body)**: **OBLIGATORIO y PEDAGÓGICO**.
    *   No te limites a decir *qué* cambiaste. Explica el **POR QUÉ**.
    *   Incluye contexto técnico ("En Android 2.3 esto fallaba porque...").
    *   Menciona alternativas descartadas ("Intentamos X, pero la RAM se llenaba").
    *   El objetivo es que un estudiante lea el commit y aprenda una lección de ingeniería.
