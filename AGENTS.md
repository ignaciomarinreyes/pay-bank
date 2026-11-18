# AGENTS.md

Guía para agentes de IA que trabajen en el repositorio **pay-bank**.

## Enfoque de desarrollo: Spec-Driven Development (SDD)

El proyecto se desarrolla siguiendo **Spec-Driven Development (SDD)**: ninguna funcionalidad se implementa sin una especificación, un plan y unas tareas previas. El flujo es siempre:

```
spec  ->  plan  ->  task  ->  implementación
```

| Fase | Pregunta que responde | Artefacto |
|------|-----------------------|-----------|
| **spec** | QUÉ hay que construir y POR QUÉ (sin detalles técnicos) | `spec.md` |
| **plan** | CÓMO se va a construir (arquitectura, decisiones técnicas, componentes afectados) | `plan.md` |
| **task** | Desglose del plan en tareas concretas y verificables | `task.md` |

La implementación debe ceñirse a las tareas de `task.md`, que a su vez derivan del `plan.md` y de la `spec.md`. Si durante la implementación aparece algo no contemplado, se actualizan primero los documentos SDD correspondientes.

## Skills de apoyo

Cada fase del flujo SDD se apoya en una skill ubicada en `.claude/skills/`:

| Skill | Fase | Descripción |
|-------|------|-------------|
| `/sdd-spec` | 1 · Especificación | Crea o actualiza `spec.md` de una funcionalidad |
| `/sdd-plan` | 2 · Plan | Crea o actualiza `plan.md` a partir de una spec existente |
| `/sdd-task` | 3 · Tareas | Crea o actualiza `task.md` a partir de la spec y su plan |

Estas skills son de **invocación exclusivamente manual**: solo se ejecutan cuando el usuario las llama explícitamente. Consulta su `SKILL.md` para conocer las reglas detalladas de cada fase (ubicación, nomenclatura, identificadores, etc.).

## Documentación del proyecto

Toda la documentación necesaria para la realización del proyecto se encuentra en las carpetas `docs/`:

- **`docs/` en la raíz del repositorio**: documentación global y especificaciones comunes a varios proyectos (p. ej. funcionalidades que afectan a backend y frontend).
- **`docs/` de cada proyecto**: documentación específica del proyecto, incluyendo su `constitution.md` (principios y restricciones técnicas) y las carpetas de cada funcionalidad con su `spec.md`, `plan.md` y `task.md`.

```
pay-bank/
├── AGENTS.md
├── docs/                         # Documentación y specs globales
│   └── <Id-nombre>/spec.md
├── pay-bank-backend/
│   └── docs/
│       ├── constitution.md       # Principios del backend
│       └── <Id-nombre>/          # spec.md, plan.md, task.md
└── pay-bank-frontend/
    └── docs/
        ├── constitution.md       # Principios del frontend
        └── <Id-nombre>/          # spec.md, plan.md, task.md
```

Antes de realizar cualquier cambio en un proyecto:

1. Lee su `docs/constitution.md` y respeta sus principios.
2. Localiza la spec, el plan y las tareas de la funcionalidad en la carpeta `docs/` correspondiente (raíz o proyecto).
3. Implementa únicamente lo definido en `task.md`.
