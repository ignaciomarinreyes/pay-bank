---
name: sdd-spec
description: Crea o actualiza la especificación funcional (spec.md) de una funcionalidad siguiendo Spec-Driven Development (SDD), primera fase del flujo spec -> plan -> task. Solo se ejecuta cuando el usuario la invoca manualmente con /sdd-spec.
disable-model-invocation: true
---

> **Invocación exclusivamente manual.** Esta skill solo se ejecuta cuando el usuario la llama explícitamente con `/sdd-spec`. Claude no debe activarla por iniciativa propia, aunque la conversación trate sobre especificaciones o requisitos.

# SDD · Fase 1: Especificación

Esta skill cubre **solo la fase de especificación** del flujo SDD:

```
spec  ->  plan  ->  task
 ^^^^
```

- **spec** (esta skill): QUÉ hay que construir y POR QUÉ. Sin detalles de implementación.
- **plan** (fase siguiente): CÓMO se va a construir (arquitectura, componentes, decisiones técnicas).
- **task** (fase final): desglose del plan en tareas ejecutables.

Reglas del flujo:
- No avances a plan ni a task dentro de esta skill. No escribas código, ni plan técnico, ni lista de tareas.
- La especificación debe quedar **cerrada** (sin ambigüedades abiertas y aprobada por el usuario) antes de poder pasar a la fase de plan.
- Al terminar, indica al usuario que el siguiente paso es crear el plan a partir de esta spec.

## Paso 1 · Entender la petición

Lee la petición del usuario y reúne el contexto necesario:
- Revisa el repositorio para conocer los proyectos existentes (carpetas con `pom.xml`, `package.json`, `build.gradle`, etc.) y sus carpetas `docs/`.
- Si existe `docs/constitution.md` en el proyecto afectado (p. ej. `pay-bank-backend/docs/constitution.md`), léelo y respeta sus principios.
- Lee especificaciones existentes relacionadas para mantener coherencia de términos y no contradecirlas.

## Paso 2 · Determinar el ámbito (global o de proyecto)

| Ámbito | Cuándo | Dónde va la spec | Dónde irán plan y tareas |
|---|---|---|---|
| **Global** | El comportamiento esperado es común a varios proyectos (p. ej. backend y frontend) o puede reutilizarse en varios | `docs/<Id-nombre>/spec.md` en la raíz del repositorio | Un `plan.md` y un `task.md` **por cada proyecto afectado**, en `<proyecto>/docs/<Id-nombre>/` |
| **Proyecto** | Afecta exclusivamente a un único proyecto | `<proyecto>/docs/<Id-nombre>/spec.md` | En la misma carpeta que la spec |

Ejemplo:

```
/
├── docs/
│   └── 001-registro-usuarios/         # spec global: backend y frontend
│       └── spec.md
├── backend/
│   └── docs/
│       ├── constitution.md            # principios y restricciones técnicas del backend
│       ├── 001-registro-usuarios/     # plan y tareas del backend para la spec global
│       │   ├── plan.md
│       │   └── task.md
│       └── 002-optimizacion-consultas/ # spec solo del backend
│           ├── spec.md
│           ├── plan.md
│           └── task.md
└── frontend/
    └── docs/
        ├── constitution.md            # principios y restricciones técnicas del frontend
        └── 001-registro-usuarios/     # plan y tareas del frontend para la spec global
            ├── plan.md
            └── task.md
```

- Si afecta o puede ser reutilizada por varios proyectos → global. La spec es **única**: no se duplica en cada proyecto, porque describe un único comportamiento esperado.
- Si afecta exclusivamente a un proyecto → de ese proyecto. No crees una spec global si solo hay un proyecto involucrado.
- **Si hay cualquier ambigüedad (o no está claro qué proyectos están afectados), pregunta al usuario antes de decidir.** No lo asumas.
- En una spec global, indica en la cabecera qué proyectos están afectados (línea `Ámbito` de la plantilla); las fases de plan y task la usan para saber dónde crear sus ficheros.
- Esta skill solo crea `spec.md`. No crees las carpetas de los proyectos para el plan ni las tareas: lo hacen `/sdd-plan` y `/sdd-task`.

## Paso 3 · Comprobar si la especificación ya existe

Antes de crear nada, busca duplicados en **todas** las carpetas `docs/` (global y de todos los proyectos), no solo en la del ámbito elegido:
- Busca carpetas `docs/*/spec.md` y compara nombre de carpeta, título y objetivo con la funcionalidad solicitada.
- Si encuentras una o varias especificaciones que **podrían** corresponder a la misma funcionalidad:
  - Muéstralas al usuario (ruta y título) y pregunta si se trata de la misma funcionalidad.
  - **No modifiques ni sobrescribas** ninguna especificación existente hasta haber confirmado su correspondencia con la funcionalidad solicitada.
  - Si es la misma: actualiza esa spec (editando solo lo necesario y conservando sus identificadores existentes; los nuevos requisitos continúan la numeración, nunca se renumeran ni reutilizan IDs).
  - Si no es la misma: crea una nueva y, si procede, menciona la relacionada en "Restricciones y consideraciones" o "Fuera de alcance".

## Paso 4 · Aclarar ambigüedades (preguntar antes de asumir)

Antes de dar por cerrada la especificación, identifica lo que no esté claro: actores y permisos, datos de entrada y validaciones, casos límite y errores, reglas de negocio (límites, importes, plazos, estados), integraciones con otros sistemas, qué queda fuera de alcance.

- Plantea las preguntas al usuario de forma agrupada y numerada, priorizando las que más afectan al resultado. Si la herramienta `AskUserQuestion` está disponible, úsala para las decisiones con opciones claras.
- **No inventes requisitos, reglas ni valores** (importes, límites, plazos, roles…). Si algo se deja provisional por decisión del usuario, márcalo explícitamente como `[PENDIENTE: …]`.
- Puedes redactar un borrador para facilitar la conversación, pero la spec no está cerrada mientras queden `[PENDIENTE: …]` o preguntas sin responder.

## Paso 5 · Redactar la especificación

Usa la plantilla de [template.md](template.md). Pautas:

1. **No es obligatorio rellenar todos los apartados.** Si no hay reglas de negocio, restricciones u otros elementos particulares, deja el apartado con su encabezado y vacío. No rellenes con contenido inventado o genérico.
2. **Identificadores** para poder referenciar los elementos: `RF-01`, `RF-02`… (requisitos funcionales), `RN-01`… (reglas de negocio), `CA-01`… (criterios de aceptación). Numeración con dos dígitos, correlativa dentro de cada spec. Cuando sea útil, un CA puede indicar qué RF/RN verifica, p. ej. `CA-03 (RF-02, RN-01)`.
3. **Nada de implementación.** Describe comportamiento observable, no solución técnica: no menciones clases Java, servicios, métodos, tablas, endpoints concretos, frameworks ni patrones. Si el usuario aporta detalles técnicos, guárdalos para la fase de plan (puedes anotarlos al final de tu respuesta, no en la spec), salvo que sean una restricción real impuesta (p. ej. "debe integrarse con el sistema X existente"), que va en el apartado 6.
4. **Criterios de aceptación verificables**, siempre en formato *Dado [contexto], cuando [acción], entonces [resultado esperado]*. Deben poder comprobarse objetivamente: valores concretos, estados y mensajes observables. Evita términos vagos ("rápido", "correctamente", "fácil", "adecuado") salvo que se cuantifiquen. Incluye casos de error y límites, no solo el camino feliz. Todo RF debería estar cubierto por al menos un CA.
5. Requisitos funcionales redactados como "El sistema debe…", uno por idea, atómicos y sin ambigüedad.

## Paso 6 · Guardar el fichero

Estructura obligatoria:

```
<raíz del ámbito>/docs/<Identificador-nombreEspecificacion>/spec.md
```

- Cada especificación tiene su propia carpeta; el fichero principal se llama siempre `spec.md`.
- Nombre de carpeta `<Identificador-nombreEspecificacion>`:
  - El nombre de la carpeta va **siempre en kebab-case**: minúsculas, palabras separadas por guiones, sin tildes, ñ, espacios, guiones bajos ni mayúsculas.
  - Si en las carpetas `docs/` ya existe una convención de numeración del identificador, síguela (manteniendo siempre el kebab-case).
  - Si no existe ninguna, usa `NNN-nombre-en-kebab-case`, donde `NNN` es el siguiente número libre de tres dígitos considerando **todas** las carpetas `docs/` del repositorio, raíz y proyectos (el identificador es único en todo el espacio de trabajo). Ejemplos: `docs/001-registro-usuarios/spec.md`, `backend/docs/002-optimizacion-consultas/spec.md`.
  - El nombre debe ser breve y descriptivo.
- Crea la carpeta `docs/` si no existe.

## Paso 7 · Revisión y cierre

Antes de dar la spec por cerrada, comprueba:
- [ ] Ámbito decidido (y confirmado si había ambigüedad) y ruta correcta; si es global, proyectos afectados indicados en la cabecera.
- [ ] Sin duplicados, o correspondencia confirmada con la spec existente.
- [ ] Sin detalles de implementación.
- [ ] Identificadores únicos y correlativos.
- [ ] Todos los CA siguen Dado/Cuando/Entonces y son verificables; todo RF está cubierto por algún CA.
- [ ] Sin preguntas abiertas ni `[PENDIENTE: …]`.

Muestra al usuario un resumen (ruta del fichero, nº de RF/RN/CA, supuestos validados) y pídele confirmación. Una vez aprobada, indica que el siguiente paso del flujo SDD es la fase de **plan**, que tomará esta `spec.md` como entrada.
