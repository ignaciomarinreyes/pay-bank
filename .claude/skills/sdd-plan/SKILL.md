---
name: sdd-plan
description: Crea o actualiza el plan de implementación (plan.md) de una especificación existente siguiendo Spec-Driven Development (SDD), segunda fase del flujo spec -> plan -> task. Solo se ejecuta cuando el usuario la invoca manualmente con /sdd-plan.
disable-model-invocation: true
---

> **Invocación exclusivamente manual.** Esta skill solo se ejecuta cuando el usuario la llama explícitamente con `/sdd-plan`. Claude no debe activarla por iniciativa propia.

# SDD · Fase 2: Plan de implementación

Esta skill cubre **solo la fase de plan** del flujo SDD:

```
spec  ->  plan  ->  task
          ^^^^
```

- **spec** (fase anterior): QUÉ hay que construir y POR QUÉ.
- **plan** (esta skill): CÓMO se va a construir: estrategia, decisiones técnicas, componentes afectados y cambios necesarios.
- **task** (fase siguiente): desglose del plan en tareas ejecutables.

Reglas del flujo:
- No se puede crear un plan sin una `spec.md` existente y cerrada.
- No escribas código ni generes la lista de tareas dentro de esta skill.
- No modifiques la `spec.md`. Si al planificar detectas que la spec es incompleta, contradictoria o inviable, detente e indícalo al usuario: la corrección se hace en la fase de spec (`/sdd-spec`), no en el plan.
- Al terminar, indica al usuario que el siguiente paso es la fase de **task**.

## Paso 1 · Localizar la especificación asociada

- Si el usuario indica la spec (identificador, nombre o ruta), localízala. Si no, busca en **todas** las carpetas `docs/` del repositorio (raíz y proyectos) los ficheros `docs/*/spec.md` y pregunta al usuario a cuál corresponde el plan. No lo asumas si hay más de una candidata.
- Si no existe ninguna spec asociada, **no crees el plan**: indica al usuario que primero debe crearla con `/sdd-spec`.
- Lee la spec completa. Comprueba que esté cerrada: sin `[PENDIENTE: …]` ni preguntas abiertas. Si no lo está, informa al usuario y no continúes hasta que se resuelva en la spec.

## Paso 2 · Determinar la ubicación (el ámbito lo fija la spec)

El diseño técnico es siempre **de un proyecto**, por lo que el `plan.md` vive en la carpeta `docs/` del proyecto que lo implementa, en una carpeta con **el mismo nombre** que la de la spec:

| Spec | Plan(es) |
|---|---|
| De proyecto: `<proyecto>/docs/<Id-nombre>/spec.md` | Un único `plan.md` en la misma carpeta: `<proyecto>/docs/<Id-nombre>/plan.md` |
| Global: `docs/<Id-nombre>/spec.md` (raíz) | Un `plan.md` **por cada proyecto afectado**: `<proyecto>/docs/<Id-nombre>/plan.md`. Nunca en `docs/` de la raíz |

Ejemplo con la spec global `docs/001-registro-usuarios/spec.md` que afecta a backend y frontend:

```
backend/docs/001-registro-usuarios/plan.md
frontend/docs/001-registro-usuarios/plan.md
```

- Para una spec global, los proyectos afectados se toman de la cabecera `Ámbito` de la spec. Si no constan o hay dudas, pregunta al usuario. Pregunta también si quiere generar los planes de todos los proyectos afectados o solo el de alguno.
- Crea la carpeta `<proyecto>/docs/<Id-nombre>/` si no existe, con exactamente el mismo nombre que la carpeta de la spec.
- **Nunca cambies el ámbito** de la especificación al crear su plan, ni muevas, copies o renombres la spec. Aunque creas que el ámbito debería ser otro (p. ej. una spec global que en realidad solo afecta a un proyecto), coméntalo al usuario pero no lo cambies.
- El fichero se llama siempre `plan.md`.

## Paso 3 · Comprobar si ya existe un plan

Cada especificación tiene como máximo un `plan.md` vigente **por proyecto**.

- Comprueba la existencia de `plan.md` en cada ubicación de destino del Paso 2.
- Si ya existe un `plan.md` en alguna de ellas:
  - **Léelo completo** antes de hacer nada y comprueba su relación con la spec actual (¿cubre todos los RF/RN/CA? ¿referencia identificadores que ya no existen? ¿hay cambios en la spec posteriores al plan?).
  - Resume al usuario su estado y las diferencias detectadas, y pregunta qué hacer: actualizar el plan existente (editando solo lo necesario y conservando los identificadores ya asignados) o rehacerlo.
  - **No lo modifiques ni lo sobrescribas** sin esa revisión previa y la confirmación del usuario.
  - Nunca crees un segundo plan (`plan-v2.md`, `plan-nuevo.md`…) en la misma carpeta.

## Paso 4 · Analizar el código y el contexto técnico

Si existe `docs/constitution.md` en el proyecto afectado (p. ej. `pay-bank-backend/docs/constitution.md`), léelo y respeta sus principios y restricciones técnicas.

Antes de redactar, investiga el proyecto (o proyectos, si la spec es global) afectado:
- Stack, estructura de módulos/paquetes, convenciones y patrones existentes, tests actuales.
- Componentes que ya resuelven parte de la funcionalidad y que conviene reutilizar.
- Puntos de integración y dependencias externas mencionadas en la spec.
- Otros `plan.md` existentes, para mantener coherencia de decisiones.

Basa el plan en lo que realmente hay en el código; no supongas nombres de clases, módulos o rutas sin comprobarlos.

## Paso 5 · Aclarar decisiones (preguntar antes de asumir)

Si hay decisiones técnicas con varias alternativas razonables y sin una opción evidente por las convenciones del proyecto (p. ej. librería a usar, modelo de datos, sincronía/asincronía, estrategia de migración), plantéalas al usuario con tu recomendación antes de cerrar el plan. Si la herramienta `AskUserQuestion` está disponible, úsala. Lo que el propio código ya decide (convenciones existentes), síguelo sin preguntar.

## Paso 6 · Redactar el plan

Usa la plantilla de [template.md](template.md). Pautas:

1. **No dupliques la spec.** No copies ni reformules los requisitos funcionales, reglas de negocio o criterios de aceptación: **referéncialos por su identificador** (`RF-02`, `RN-01`, `CA-04`).
2. **Contenido obligatorio:** estrategia de implementación, decisiones técnicas relevantes, componentes afectados y cambios necesarios.
3. **Identificadores propios** para poder referenciarlos desde la fase de tareas:
   - `DT-01`, `DT-02`… para decisiones técnicas (con alternativas consideradas y motivo).
   - `CB-01`, `CB-02`… para cada cambio necesario.
4. **Trazabilidad completa:** todo RF y RN de la spec debe estar cubierto por al menos un cambio (`CB-xx`), y todo CA debe tener definida cómo se verificará. Si algo de la spec no se cubre, indícalo explícitamente y justifícalo.
5. **Nivel de detalle:** suficiente para que la fase de tareas pueda desglosarlo sin volver a tomar decisiones de diseño (componentes, contratos e interfaces relevantes, modelo de datos, migraciones, configuración), pero sin escribir el código.
6. Deja vacíos los apartados que no apliquen (p. ej. sin migraciones o sin riesgos relevantes); no los rellenes con contenido genérico.
7. **Specs globales: un plan por proyecto.**
   - Cada `plan.md` describe solo el diseño técnico de **su** proyecto. No describas componentes ni cambios de otro proyecto.
   - Indica en el apartado "Alcance en este proyecto" qué RF/RN/CA de la spec aplican a este proyecto. La trazabilidad se cumple **entre todos los planes**: cada RF/RN debe estar cubierto por algún `CB` de alguno de los planes, y cada CA verificado en alguno. Lo que cubre otro proyecto se indica en "Elementos de la spec no cubiertos" remitiendo a su plan (p. ej. `RF-04 → frontend`).
   - Los **contratos compartidos** entre proyectos (API, eventos, formatos de datos) deben ser coherentes. Defínelos en el plan del proyecto que los provee (normalmente el backend) y referéncialos desde el otro, sin duplicarlos. Si el otro plan ya existe, léelo y respeta lo acordado; si detectas una incoherencia, avisa al usuario.
   - Los identificadores `DT` y `CB` son propios de cada plan. Para referenciar elementos de otro proyecto, anteponle el proyecto: `backend:CB-02`.

## Paso 7 · Revisión y cierre

Antes de dar el plan por cerrado, comprueba:
- [ ] Existe la spec asociada, está cerrada y no se ha modificado.
- [ ] Ubicación correcta: misma carpeta que la spec (spec de proyecto) o `<proyecto>/docs/<Id-nombre>/` de cada proyecto afectado (spec global); ámbito sin cambiar.
- [ ] No había plan previo, o se revisó y el usuario confirmó cómo proceder.
- [ ] Incluye estrategia, decisiones técnicas, componentes afectados y cambios necesarios.
- [ ] No duplica requisitos de la spec; los referencia por identificador.
- [ ] Todos los RF/RN cubiertos por algún `CB-xx` y todos los CA con verificación definida (en spec global, considerando el conjunto de planes de los proyectos).
- [ ] En spec global: contratos compartidos coherentes entre los planes de los proyectos.
- [ ] Sin decisiones abiertas.

Muestra al usuario un resumen (ruta de cada fichero, decisiones técnicas principales, nº de cambios, riesgos) y pídele aprobación. Una vez aprobado, indica que el siguiente paso del flujo SDD es la fase de **task**, que tomará este `plan.md` como entrada.
