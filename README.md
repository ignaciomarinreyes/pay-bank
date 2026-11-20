# Pay Bank

Aplicación de pagos que permite a un usuario realizar pagos a otros usuarios y consultar su saldo, sus movimientos y los pagos realizados y recibidos.

> 🎬 **Vídeo demostración:** [cómo funciona la aplicación y cómo se aplica la metodología SDD](https://youtu.be/LJ3_igzqZ_Q)

El repositorio contiene dos proyectos:

| Proyecto | Tecnología | Puerto |
|----------|------------|--------|
| [`pay-bank-backend`](pay-bank-backend) | Java 25 · Spring Boot 4 · PostgreSQL | `8080` |
| [`pay-bank-frontend`](pay-bank-frontend) | Angular 22 · TypeScript | `4200` |

## Requisitos previos

- **Java 25** (JDK)
- **Maven** 3.9 o superior
- **Docker** y Docker Compose (para la base de datos PostgreSQL)
- **Node.js** (LTS actual) y **npm**

## Puesta en marcha

Arranca los servicios en este orden: base de datos → backend → frontend.

### 1. Base de datos

Desde `pay-bank-backend`, levanta PostgreSQL con Docker:

```bash
cd pay-bank-backend
docker compose up -d
```

Se crea la base de datos `pay_bank` en `localhost:5432`.

### 2. Backend

Desde la carpeta `pay-bank-backend` (es importante ejecutarlo ahí, porque los secretos locales se leen de `./secret-local.yml`):

```bash
mvn spring-boot:run
```

- API: http://localhost:8080
- Documentación interactiva (Swagger UI): http://localhost:8080/swagger-ui.html

Al arrancar, el esquema se crea automáticamente y se cargan datos de ejemplo (`src/main/resources/data.sql`) con los usuarios de prueba:

| Alias | Contraseña |
|-------|------------|
| `ana` | `ana-pass` |
| `maria` | `maria-pass` |
| `luis` | `luis-pass` |

### 3. Frontend

En otra terminal:

```bash
cd pay-bank-frontend
npm install
npm start
```

Abre http://localhost:4200 e inicia sesión con uno de los usuarios de prueba.

### Otros comandos útiles

| Comando | Proyecto | Descripción |
|---------|----------|-------------|
| `mvn test` | backend | Ejecuta los tests |
| `npm test` | frontend | Ejecuta los tests (Vitest) |
| `npm run build` | frontend | Genera la build en `dist/` |
| `docker compose down` | backend | Detiene la base de datos (añade `-v` para borrar sus datos) |

## Metodología: Spec-Driven Development (SDD)

El proyecto se desarrolla siguiendo **Spec-Driven Development**: ninguna funcionalidad se implementa sin haber definido antes qué se quiere construir, cómo y en qué pasos. Cada funcionalidad recorre siempre tres fases documentales antes de llegar al código:

```
spec  ->  plan  ->  task  ->  implementación
```

| Fase | Pregunta que responde | Documento |
|------|-----------------------|-----------|
| **Spec** | **Qué** hay que construir y **por qué**, sin detalles técnicos | `spec.md` |
| **Plan** | **Cómo** se va a construir: arquitectura, decisiones técnicas y componentes afectados | `plan.md` |
| **Task** | Desglose del plan en tareas concretas y verificables | `task.md` |

La implementación se ciñe a las tareas de `task.md`, que derivan del plan y este de la spec. Si durante el desarrollo surge algo no previsto, primero se actualizan los documentos y después el código. Así la documentación se mantiene como fuente de verdad y el trabajo (humano o de agentes de IA) es trazable y revisable.

### Dónde está la documentación

- `docs/` (raíz): specs globales que afectan a varios proyectos.
- `pay-bank-backend/docs/` y `pay-bank-frontend/docs/`: la `constitution.md` de cada proyecto (principios y restricciones técnicas) y una carpeta por funcionalidad (`<Id-nombre>/`) con su `spec.md`, `plan.md` y `task.md`.

Cada fase cuenta con una skill de apoyo en `.claude/skills/` (`/sdd-spec`, `/sdd-plan`, `/sdd-task`). Consulta [AGENTS.md](AGENTS.md) para el detalle del flujo.
