# Pay Bank Backend

Una aplicación de pagos construida con Java 25 y Spring Boot 4.

## Propósito

Una aplicación de pagos que permite a un usuario realizar un pago a otro usuario. Además, permite consultar los movimientos, los pagos realizados y los pagos recibidos, junto con la consulta del saldo total.

## Funcionalidades principales

- **Autenticación** — Inicio de sesión con Spring Security y JWT; todos los endpoints de negocio requieren un usuario autenticado
- **Cuentas** — Cada usuario dispone de una cuenta con su saldo disponible
- **Pagos** — Un usuario realiza un pago a otro usuario indicando el destinatario, el importe y un concepto opcional
- **Movimientos** — Consulta del histórico de movimientos de la cuenta (cargos y abonos) generados por los pagos
- **Pagos realizados y recibidos** — Consulta por separado de los pagos enviados y de los pagos recibidos por el usuario
- **Saldo total** — Consulta del saldo actual de la cuenta del usuario

## Modelo de dominio de alto nivel

```
User (1) ----> (1) Account (1) ----> (N) Movement
                    |    ^
          origen    |    |   destino
                    v    |
                   (N) Payment (N)
```

- Un **Payment** tiene siempre una cuenta de origen y una cuenta de destino distintas.
- Cada pago genera dos **Movement**: un cargo en la cuenta de origen y un abono en la cuenta de destino.
- El **saldo** de una **Account** es su saldo inicial más la suma de sus movimientos.

## Stack tecnológico

| Componente | Versión |
|-----------|---------|
| Java | 25 LTS |
| Spring Boot | 4.1.0 |
| Spring Framework | 7.0.x |
| Spring Data JPA | - |
| Base de datos | PostgreSQL (Docker) |
| Herramienta de build | Maven |

## Arquitectura

Arquitectura de tres capas organizada por funcionalidad (package-by-feature):

```
┌──────────────────────────────────────────────────────────────────┐
│                        CAPA CONTROLLER                           │
│                                                                  │
│  ┌──────────────────────┐  ┌──────────────────────────┐          │
│  │  user/controller/    │  │  payment/controller/     │          │
│  │  UserController      │  │  PaymentController       │          │
│  │  AuthController      │  │  (implements PaymentsApi)│          │
│  │  (implement AuthApi, │  │  account/controller/     │          │
│  │   UsersApi)          │  │  AccountController       │          │
│  │                      │  │  (implements AccountsApi)│          │
│  └──────────┬───────────┘  └─────────────┬────────────┘          │
│             │                            │                       │
├─────────────┼────────────────────────────┼───────────────────────┤
│             │        CAPA SERVICE        │                       │
│             ▼                            ▼                       │
│  ┌──────────────────────┐  ┌──────────────────────────┐          │
│  │  user/service/       │  │  payment/service/        │          │
│  │  UserService         │  │  PaymentService          │          │
│  │  AuthService         │  │  account/service/        │          │
│  │                      │  │  AccountService          │          │
│  └──────────┬───────────┘  └─────────────┬────────────┘          │
│             │                            │                       │
├─────────────┼────────────────────────────┼───────────────────────┤
│             │      CAPA REPOSITORY       │                       │
│             ▼                            ▼                       │
│  ┌──────────────────────┐  ┌──────────────────────────┐          │
│  │  user/repository/    │  │  payment/repository/     │          │
│  │  UserRepository      │  │  PaymentRepository       │          │
│  │  RefreshTokenRepo    │  │  account/repository/     │          │
│  │                      │  │  AccountRepository       │          │
│  │                      │  │  MovementRepository      │          │
│  └──────────────────────┘  └──────────────────────────┘          │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│                      PAQUETES DE SOPORTE                         │
│                                                                  │
│  config/          security/           exception/                 │
│  ┌────────────┐   ┌──────────────┐   ┌──────────────────────┐    │
│  │JwtConfig   │   │JwtTokenProv. │   │GlobalExceptionHandler│    │
│  │SecurityCfg │   │JwtAuthFilter │   │(ControllerAdvice)    │    │
│  │            │   │UserDetailsSvc│   └──────────────────────┘    │
│  │            │   │CurrentUserPrv│                               │
│  └────────────┘   └──────────────┘                               │
│                                                                  │
│  generated/ (OpenAPI)     mapper/                                │
│  ┌──────────────────┐    ┌──────────────┐                        │
│  │AuthApi           │    │PaymentMapper │                        │
│  │UsersApi          │    │AccountMapper │                        │
│  │PaymentsApi       │    │MovementMapper│                        │
│  │AccountsApi       │    │UserMapper    │                        │
│  │LoginRequest (+n) │    └──────────────┘                        │
│  └──────────────────┘                                            │
└──────────────────────────────────────────────────────────────────┘
```

### Responsabilidades de cada capa

| Capa | Responsabilidad | Comunicación |
|-------|---------------|---------------|
| **Controller** | Gestión de peticiones/respuestas HTTP, validación, delega en los servicios | Recibe HTTP, invoca métodos de servicio, devuelve respuestas HTTP |
| **Service** | Lógica de negocio, orquestación, transacciones | Invoca repositorios, mapea entidades ↔ DTOs |
| **Repository** | Acceso a datos mediante Spring Data JPA | Persiste y consulta entidades |

### Organización de paquetes

Cada funcionalidad es autocontenida, con sus propias capas:

```
com.paybank.
├── user/                  # Gestión de usuarios y autenticación
│   ├── controller/        # UserController, AuthController
│   ├── service/           # UserService, AuthService
│   ├── repository/        # UserRepository, RefreshTokenRepository
│   ├── mapper/            # UserMapper
│   ├── model/             # User, RefreshToken
│   └── exception/         # InvalidCredentialsException, UserNotFoundException, ...
├── account/               # Cuenta, saldo y movimientos
│   ├── controller/        # AccountController
│   ├── service/           # AccountService
│   ├── repository/        # AccountRepository, MovementRepository
│   ├── mapper/            # AccountMapper, MovementMapper
│   ├── model/             # Account, Movement, MovementType
│   └── exception/         # AccountNotFoundException, ...
├── payment/               # Pagos entre usuarios
│   ├── controller/        # PaymentController
│   ├── service/           # PaymentService
│   ├── repository/        # PaymentRepository
│   ├── mapper/            # PaymentMapper
│   ├── model/             # Payment
│   └── exception/         # InsufficientBalanceException, SelfPaymentException, ...
├── common/                # Utilidades compartidas (p. ej. common/web/PageQuery)
├── config/                # Configuración compartida (JwtConfig, SecurityConfig)
├── security/              # Proveedor de tokens JWT, filtro de autenticación, UserDetailsService, CurrentUserProvider
├── exception/             # Gestión global de excepciones (@ControllerAdvice)
└── generated/             # Interfaces y DTOs generados desde OpenAPI
```

## Convenciones

### Idioma

- **El código se escribe en inglés**: identificadores, nombres de paquetes, clases, métodos y variables, comentarios en el código, mensajes de log, textos de commits, nombres de endpoints y el contrato OpenAPI (`docs/api/openapi.yaml`).
- La documentación del proyecto (este fichero y las especificaciones SDD) se escribe en español.

### La paginación es siempre en servidor

Todos los endpoints de listado/colección (movimientos, pagos realizados, pagos recibidos, usuarios...) deben paginarse y filtrarse en la consulta a base de datos, nunca en memoria. Cada endpoint de listado usa el objeto de paginación compartido `PageQuery` (`com.paybank.common.web.PageQuery`, valores por defecto `page = 0`, `size = 20`, búsqueda opcional `q`) convertido al `Pageable`/`Page` de Spring Data, y devuelve un `*PageResponse` (`content`, `page`, `size`, `totalElements`, `totalPages`). Nunca se devuelven listas sin límite.

### Seguridad

- Los endpoints se protegen con Spring Security. No existen roles: la única distinción es entre usuario autenticado y no autenticado.
- Todos los endpoints requieren un usuario autenticado mediante un token JWT válido, salvo el inicio de sesión (`AuthApi`) y la documentación OpenAPI/Swagger.
- Una petición sin token o con un token inválido o caducado se rechaza con `401`.
- Las contraseñas se almacenan cifradas con BCrypt.

### Integridad de los pagos

- Los importes se representan siempre con `BigDecimal` (nunca `double` ni `float`).
- Un pago es una única transacción (`@Transactional` en la capa de servicio): crear el `Payment`, generar los dos `Movement` y actualizar los saldos se confirma o se revierte en bloque.
- No se permite un pago con importe menor o igual a cero, a uno mismo ni que deje el saldo de la cuenta de origen en negativo.
- El usuario solo puede consultar su propia cuenta, sus movimientos y sus pagos; el usuario actual se obtiene siempre del token mediante `CurrentUserProvider`, nunca de un parámetro de la petición.

## Contrato de API y generación de código

El contrato de la API se define en `docs/api/openapi.yaml` como única fuente de verdad. Los DTOs y las interfaces de los controladores se generan a partir de él.

### Generar DTOs e interfaces de controlador

```bash
mvn generate-sources
```

Ejecuta el `openapi-generator-maven-plugin`, que produce:

- **Interfaces de controlador** en `com.paybank.generated.api` — p. ej. `AuthApi.java`, `UsersApi.java`, `AccountsApi.java`, `PaymentsApi.java`
- **DTOs** en `com.paybank.generated.model` — p. ej. `LoginRequest.java`, `CreatePaymentRequest.java`

La salida se genera en `target/generated-sources/openapi/` y se incluye automáticamente en el classpath. La generación también se ejecuta como parte de `mvn compile` y `mvn test`.

### Actualizar el contrato de la API

Edita `docs/api/openapi.yaml` y vuelve a generar:

```bash
mvn generate-sources
```

Implementa las interfaces generadas en tus controladores (p. ej. `PaymentController implements PaymentsApi`).

## Ejecución con Docker

### Arquitectura

`docker-compose.yml` proporciona **únicamente la base de datos PostgreSQL** (servicio `db`). El backend se ejecuta siempre en la máquina con `mvn spring-boot:run`. Los valores de configuración del contenedor usan valores por defecto en línea (`${VAR:-default}`), por lo que no se necesita un fichero adicional.

### Requisitos previos

- Docker Desktop 4.x+
- Docker Compose 2.x+ (incluido en Docker Desktop)
- Java 25 LTS
- Maven 3.9+

### Flujo de trabajo

**PostgreSQL en Docker, aplicación en la máquina.** Esto permite recarga en caliente con Spring DevTools, depuración desde el IDE y respuesta inmediata.

```bash
# Arrancar la base de datos en segundo plano
docker compose up -d db

# Ejecutar la aplicación con recarga en caliente
mvn spring-boot:run
```

Al editar cualquier fichero `.java`, Spring DevTools reinicia la aplicación automáticamente (~1-2 s). No es necesario reconstruir nada en Docker.

#### Secretos locales (`secret-local.yml`)

Todos los secretos viven en **`pay-bank-backend/secret-local.yml`**, un fichero ignorado por git e importado por `application.yml` (`spring.config.import: optional:file:./secret-local.yml`). Se lee de forma relativa a la carpeta desde la que se arranca la aplicación, por lo que `mvn spring-boot:run` debe ejecutarse desde `pay-bank-backend`. `application.yml` no contiene secretos. Si el fichero no existe, la aplicación no arranca (la contraseña del datasource y `jwt.secret` quedan sin resolver). Créalo con:

```yaml
spring:
  datasource:
    password: pay_bank_password     # coincide con POSTGRES_PASSWORD del contenedor de base de datos
jwt:
  secret: "secreto-de-32-caracteres-minimo-para-jwt!!"   # al menos 32 caracteres
```

Nunca se debe subir este fichero al repositorio ni poner secretos reales en `application.yml`.

### Ciclo de vida del contenedor

```bash
# Arrancar la base de datos en segundo plano
docker compose up -d db

# Pararla (los datos persisten en el volumen)
docker compose down

# Parar y borrar el volumen (⚠️ destruye los datos de la base de datos)
docker compose down -v

# Ver los logs de la base de datos
docker compose logs -f db
```

### Volúmenes

Los datos de PostgreSQL se almacenan en un volumen de Docker:

| Nombre del volumen | Ubicación |
|-------------|----------|
| `pay-bank-backend_pgdata` | Gestionado por Docker (`/var/lib/docker/volumes/`) |

Los datos persisten entre reinicios del contenedor. Usa `docker compose down -v` para borrarlos.

### Documentación OpenAPI

Con la aplicación en ejecución, abre en el navegador:

- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) — documentación interactiva de la API
- **OpenAPI JSON**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs) — especificación OpenAPI en formato JSON

## Gestión del esquema de base de datos

El esquema de base de datos lo gestiona Hibernate mediante `spring.jpa.hibernate.ddl-auto`:

| Contexto | `ddl-auto` | Comportamiento |
|---------|-----------|----------|
| Aplicación | `update` | Crea las tablas/columnas que falten al arrancar y **conserva los datos** entre reinicios — práctico para probar manualmente desde Swagger |
| Tests | `create-drop` | Recrea un esquema limpio al iniciar cada contexto de test y lo elimina al terminar — tests de integración repetibles y aislados |

> Nota sobre `update`: solo añade lo que falta. Las columnas o restricciones eliminadas de las entidades no se borran. Si se necesita partir de cero, reinicia la base de datos manualmente (p. ej. `docker compose down -v`).

## Tests

```bash
mvn test
```

Los tests usan HSQLDB (embebida) y no requieren Docker ni PostgreSQL.

## Build

```bash
mvn clean compile
```

## Estado

Este proyecto está en una fase temprana de desarrollo. Las especificaciones se definen en las carpetas `docs/` siguiendo el flujo SDD (spec → plan → task).
