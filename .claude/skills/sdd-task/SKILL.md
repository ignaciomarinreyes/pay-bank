---
name: sdd-task
description: Crea o actualiza las tareas de implementación (task.md) a partir de una especificación y su plan siguiendo Spec-Driven Development (SDD), última fase del flujo spec -> plan -> task. Solo se ejecuta cuando el usuario la invoca manualmente con /sdd-task.
disable-model-invocation: true
---

> **Invocación exclusivamente manual.** Esta skill solo se ejecuta cuando el usuario la llama explícitamente con `/sdd-task`. Claude no debe activarla por iniciativa propia.

# SDD · Fase 3: Tareas de implementación

Esta skill cubre **solo la fase de tareas** del flujo SDD:

```
spec  ->  plan  ->  task
                    ^^^^
```

- **spec**: QUÉ hay que construir y POR QUÉ (`RF`, `RN`, `CA`).
- **plan**: CÓMO se va a construir (`DT`, `CB`).
- **task** (esta skill): descomposición del plan en unidades de trabajo concretas y verificables (`T`).

Reglas del flujo:
- No se pueden crear tareas sin `spec.md` y `plan.md` existentes y cerrados.
- No implementes las tareas ni escribas código dentro de esta skill: solo se genera `task.md`.
- No modifiques `spec.md` ni `plan.md`. Si al desglosar detectas huecos, contradicciones o decisiones de diseño sin tomar, detente e indícalo al usuario: se corrigen en su fase (`/sdd-spec` o `/sdd-plan`), no en las tareas.

## Paso 1 · Localizar la especificación y su plan

- Si el usuario indica la especificación (identificador, nombre o ruta), localízala. Si no, busca en **todas** las carpetas `docs/` del repositorio (raíz y proyectos) las carpetas que contengan `spec.md` y pregunta al usuario a cuál corresponden las tareas. No lo asumas si hay más de una candidata.
- Localiza el `plan.md` o planes asociados (ver Paso 2): en spec de proyecto, junto a la spec; en spec global, en `<proyecto>/docs/<Id-nombre>/plan.md` de cada proyecto afectado. Si la spec es global, pregunta al usuario para qué proyecto(s) quiere generar las tareas.
- Comprueba que existen **`spec.md` y el `plan.md`** de cada proyecto para el que se van a generar tareas:
  - Si falta `spec.md`, no continúes: indica que debe crearse con `/sdd-spec`.
  - Si falta el `plan.md` de un proyecto, no generes las tareas de ese proyecto: indica que debe crearse con `/sdd-plan`.
- Si existe `docs/constitucion.md` en la raíz, léelo y respeta sus principios.
- Lee ambos ficheros completos. Comprueba que estén cerrados (sin `[PENDIENTE: …]`, preguntas ni decisiones abiertas) y que el plan sea coherente con la spec (referencia identificadores existentes y cubre los RF/RN). Si no es así, informa al usuario y no continúes hasta que se resuelva en la fase correspondiente.

## Paso 2 · Determinar la ubicación (el ámbito lo fija la spec)

Las tareas se guardan **siempre junto al `plan.md` del que derivan**, y cada proyecto tiene sus propias tareas:

| Spec | Tareas |
|---|---|
| De proyecto | `<proyecto>/docs/<Id-nombre>/task.md`, junto a `spec.md` y `plan.md` |
| Global (`docs/` de la raíz) | Un `task.md` **por proyecto afectado**: `<proyecto>/docs/<Id-nombre>/task.md`, junto al `plan.md` de ese proyecto. Nunca en `docs/` de la raíz |

```
docs/001-registro-usuarios/spec.md                  # spec global
backend/docs/001-registro-usuarios/plan.md
backend/docs/001-registro-usuarios/task.md          # tareas del backend
frontend/docs/001-registro-usuarios/plan.md
frontend/docs/001-registro-usuarios/task.md         # tareas del frontend

backend/docs/002-optimizacion-consultas/spec.md     # spec solo del backend
backend/docs/002-optimizacion-consultas/plan.md
backend/docs/002-optimizacion-consultas/task.md
```

- **Nunca cambies el ámbito** de la especificación, ni muevas, copies o renombres la spec o los planes.
- El fichero se llama siempre `task.md`.

## Paso 3 · Comprobar si ya existe `task.md`

- Si ya existe:
  - **Léelo completo** antes de hacer nada: estado de cada tarea (pendiente / en curso / completada), y su correspondencia con el plan y la spec actuales (¿tareas que referencian `CB`/`DT`/`RF` que ya no existen? ¿cambios del plan sin tareas?).
  - Resume al usuario el estado y las diferencias, y pregunta cómo proceder: actualizar el fichero existente o rehacerlo.
  - **No lo modifiques ni lo sobrescribas** sin esa revisión previa y la confirmación del usuario. Al actualizar, conserva los identificadores y el estado de las tareas existentes (en especial las completadas); las nuevas continúan la numeración, nunca se renumeran ni reutilizan IDs.
  - Nunca crees un segundo fichero de tareas en la misma carpeta.

## Paso 4 · Analizar el código

Revisa el código del proyecto o proyectos afectados para que las tareas sean concretas y realistas: ubicaciones reales donde se harán los cambios, tests existentes, comandos de build y test. No inventes rutas ni componentes que no existan o que el plan no prevea crear.

## Paso 5 · Descomponer el plan en tareas

Usa la plantilla de [template.md](template.md) **tal cual**: la cabecera, el apartado "Orden de ejecución" y, por cada tarea, solo su título (`### [ ] T-NN: …`) y el campo **Qué hacer**. No añadas otros campos ni una matriz de trazabilidad. Pautas:

1. **Unidades de trabajo concretas**: cada tarea debe poder completarse y revisarse por sí sola (orientativamente, un cambio acotado que cabe en un commit o PR pequeño). Si un `CB` es grande, divídelo en varias tareas; si varios `CB` son triviales y muy acoplados, pueden agruparse.
2. **Título**: breve, en imperativo, describiendo la acción (p. ej. `Añadir validación de email duplicado en el alta`).
3. **Contenido de "Qué hacer"**: un texto breve que recoge, en este orden:
   - la acción concreta y dónde se realiza (proyecto, módulo o ubicación real del código);
   - el **resultado esperado**, de forma objetivamente comprobable (tests que deben pasar, comportamiento observable). Nada de "mejorar", "revisar" o "ajustar" sin un resultado comprobable;
   - las **referencias** entre paréntesis al final: elementos del plan que implementa y de la spec que cubre o verifica, y dependencias. Ejemplo: `(CB-02, DT-01 · RF-03, CA-02 · depende de T-01)`.
4. **Dependencias y orden**: minimiza dependencias. Ordena las tareas para que ninguna dependa de una posterior y resume en "Orden de ejecución" la secuencia y las tareas que pueden ejecutarse en paralelo (p. ej. `T-01 → (T-02 ∥ T-03) → T-04`).
5. **Trazabilidad sin matriz**: todo `CB` del plan debe quedar cubierto por al menos una tarea y todo `CA` de la spec debe verificarse en alguna tarea. Compruébalo al redactar a partir de las referencias de cada tarea; no se escribe una tabla aparte.
6. **Sin duplicar**: no copies requisitos de la spec ni explicaciones o decisiones del plan; **referéncialos por su identificador**. "Qué hacer" dice lo que hay que hacer, no repite el porqué.
7. **Identificadores** `T-01`, `T-02`… correlativos, con estado inicial `[ ]` (pendiente). Estados: `[ ]` pendiente, `[~]` en curso, `[x]` completada.
8. **Pruebas incluidas**: las pruebas forman parte de la tarea que implementa el cambio o van en tareas propias, pero ningún `CA` puede quedar sin tarea que lo verifique.
9. **Specs globales: tareas por proyecto.** Cada `task.md` desglosa solo el `plan.md` de su proyecto. La cobertura de `CB` es por plan; la de los `CA` se cumple entre los `task.md` de todos los proyectos afectados. Las referencias a otro proyecto llevan su prefijo, tanto en dependencias (`depende de backend:T-03`) como en elementos del plan (`backend:CB-02`).
10. No se toman decisiones de diseño nuevas en esta fase. Si para desglosar hace falta una decisión que el plan no recoge, pregunta al usuario y propón actualizar el plan con `/sdd-plan`.

## Paso 6 · Revisión y cierre

Antes de dar las tareas por cerradas, comprueba:
- [ ] Existen `spec.md` y `plan.md`, están cerrados y no se han modificado.
- [ ] Cada `task.md` está junto al `plan.md` de su proyecto; ámbito sin cambiar.
- [ ] En spec global: dependencias entre proyectos indicadas con prefijo de proyecto.
- [ ] No había `task.md` previo, o se revisó y el usuario confirmó cómo proceder.
- [ ] Cada tarea sigue la plantilla (título + "Qué hacer") y su "Qué hacer" incluye acción, resultado esperado comprobable y referencias.
- [ ] Dependencias explícitas, orden coherente y "Orden de ejecución" rellenado.
- [ ] Todos los `CB` cubiertos por alguna tarea y todos los `CA` verificados por alguna tarea (según las referencias de las tareas).
- [ ] No se duplica contenido de la spec ni del plan.

Muestra al usuario un resumen (ruta del fichero, nº de tareas, dependencias principales, tareas paralelizables) y pídele aprobación. Una vez aprobado, el flujo SDD queda completo para esta funcionalidad: la implementación se hará tarea a tarea, actualizando su estado en `task.md`.
