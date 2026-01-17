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
    *   No te limites a decir *qué* cambiaste. Explica el **POR QUÉ**.
    *   Incluye contexto técnico ("En Android 2.3 esto fallaba porque...").
    *   Menciona alternativas descartadas ("Intentamos X, pero la RAM se llenaba").
    *   El objetivo es que un estudiante lea el commit y aprenda una lección de ingeniería.
*   **Sincronización Bitácora-Commit**: Si el commit incluye una nueva entrada en `BITACORA.md`, el cuerpo del mensaje del commit **DEBE INCLUIR COPIA LITERAL** del texto añadido a la Bitácora.
    *   No resumas. Copia y pega el contenido Markdown completo en el cuerpo del commit.
    *   Aprovecha que Git permite mensajes largos para preservar la narrativa técnica íntegra.

## 5. Documentación Viva (BITACORA.md)
*   **Archivo Maestro**: El archivo `BITACORA.md` en la raíz es nuestro "Pergamino Infinito" acumulativo.
*   **Proceso**: Al finalizar una tarea, el Agente debe generar un reporte interno didáctico y **AÑADIRLO (APPEND)** al final de `BITACORA.md`. NUNCA sobrescribir el archivo completo.
*   **Criterio de Inclusión (La Prueba del Café ☕)**:
    *   **CHANGELOG.md (Universal)**: Se registra **TODO** (Phases, Fixes, Typos, Versiones). Es el notario del proyecto.
    *   **BITACORA.md (Selecta)**: Se añade **SOLO** si es relevante técnicamente (Retos, Arquitectura, Lecciones).
        *   **NO incluir en Bitácora**: Cambios menores, correcciones de typos, subidas de versión rutinarias.
*   **Registro de Fallos (La Bitácora de Guerra)**:
    *   Si una Phase (o sub-phase) FALLA en su verificación, **ES OBLIGATORIO** documentar el intento fallido en `BITACORA.md` inmediatamente, antes de planificar la siguiente solución.
    *   Formato: `### ❌ Intento Fallido (vX.X.X): [Descripción Breve]`. Explicar qué se probó y por qué no funcionó.
    *   *Objetivo*: Evitar ciclos infinitos de prueba y error repitiendo los mismos pasos en el futuro.
*   **Estructura del Reporte Exitoso**:
    1.  **Título con Icono 🚀**: Atractivo y descriptivo.
    2.  **El Problema (Storytelling) 📜**: Narrativa de qué sucedía antes (ej: "El móvil se calentaba...").
    3.  **La Solución (Ingeniería) 🛠️**: Explicación técnica profunda. Usar diagramas ASCII si ayuda. Justificar decisiones.
    4.  **Lecciones Aprendidas 🎓**: Lista de conceptos clave para el estudiante (ej: "Aprendimos que el GC en Android 2.3 es lento").
    5.  **Glosario 📖**: Definiciones breves de términos técnicos complejos.
    6.  **Estilo Visual**: Uso generoso de emojis (✅, ⚠️, ℹ️) para facilitar la lectura.

## 6. La Regla del Semáforo Rojo (Integridad de Git)
*   🚦 **NO SUBIR VERSIÓN CON CAMBIOS PENDIENTES**: Está estrictamente **PROHIBIDO** incrementar el `versionName` en `build.gradle` si el comando `git status` muestra archivos modificados (dirty state) que no pertenecen a la nueva versión.
*   **Acción Requerida**: Si hay cambios pendientes:
    1.  **Commitear**: Si son cambios válidos, haz un commit antes de seguir.
    2.  **Revertir**: Si son pruebas fallidas, límpialas (`git restore`).
*   **Excepción**: El único momento donde `git status` puede estar "sucio" es justo después de editar los archivos para la *nueva* versión (build.gradle, changelog, código), momento en el cual procedemos inmediatamente al Commit de Release.

## 7. Estructura de Tareas (Safety Check)
*   Cuando generes una lista de tareas en `task.md`, la última tarea de la fase de **Ejecución** SIEMPRE debe ser un paso explícito de Git:
    *   `[ ] Commit & Tag vX.X.X (Snapshot)`
*   Esto actúa como barrera de seguridad antes de pasar a la fase de **Verificación** o a la siguiente iteración.

## 8. Protocolo de Ruptura (Circuit Breaker)
Qué hacer cuando una Verificación FALLA:
1.  🛑 **STOP**: Prohibido iniciar inmediatamente una sub-fase de corrección rápida ("Hotfix Loop").
2.  📝 **LOG**: Documentar el fallo en `BITACORA.md` (Ver Regla 5).
3.  💾 **SAVE (Commit de Estado Roto)**:
    *   **NUNCA borrar código** (`git restore`) si hay trabajo sustancial.
    *   Hacer un commit con el estado actual roto para no perder el trabajo.
    *   Prefijo del commit: `chore(debug): Snapshot intento fallido...`
    *   (Opcional) Poner un Tag si es relevante: `vX.X.X-BROKEN`.
4.  🧠 **RETHINK**: Volver obligatoriamente a la fase de **Planificación** para la siguiente iteración.
