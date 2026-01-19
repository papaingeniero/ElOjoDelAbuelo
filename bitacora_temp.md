
### 🎨 v3.9.2-dev.6: Refinamiento Estético del Modal de Ajustes

La primera implementación funcional es solo el 50% del trabajo; la percepción del usuario es el otro 50%.
Al revisar el modal de ajustes, los inputs numéricos de `Pan X` y `Pan Y` resultaban crudos y poco intuitivos ("¿Qué es X? ¿Qué es Y?"). Además, tenían problemas de layout.

**Cambios de UI/UX:**
1.  **Semántica Visual**: Se reemplazaron las etiquetas de texto por iconos universales (`↔` para horizontal, `↕` para vertical), reduciendo la carga cognitiva.
2.  **Layout Compacto**: Se reestructuró el formulario usando `Flexbox` con inputs anidados.
3.  **Constraint de Ancho**: Se limitó el ancho de los inputs a `90px` para evitar que se expandieran desproporcionadamente en pantallas anchas, manteniendo la elegancia del modal.

*Lección*: Una interfaz técnica no tiene por qué ser fea. Un simple icono mejora la usabilidad enormemente.
