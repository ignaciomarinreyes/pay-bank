# Pay Bank Frontend

La interfaz web de la aplicación de pagos Pay Bank, construida con Angular y TypeScript.

## Propósito

**Pay Bank** es una aplicación de pagos que permite a un usuario realizar un pago a otro usuario y consultar sus movimientos, los pagos realizados, los pagos recibidos y su saldo total.

Este repositorio contiene el **frontend** del proyecto: la aplicación web con la que el usuario interactúa y que envía todas las peticiones al backend a través de su API HTTP. El frontend no contiene lógica de negocio propia sobre pagos o saldos: muestra los datos que devuelve el backend y le envía las operaciones del usuario.

Sigue una arquitectura **signals-first**, con **componentes standalone** y **sin Zone.js** (zoneless).

## Relación con el backend

El **backend** está en el proyecto hermano `../pay-bank-backend` (Java 25 / Spring Boot 4). Su documentación es la fuente de verdad de la API HTTP, del modelo de datos y del comportamiento del backend:

- `../pay-bank-backend/docs/constitution.md` — principios y restricciones técnicas del backend.
- `../pay-bank-backend/docs/api/openapi.yaml` — contrato de la API (endpoints, DTOs, códigos de error).
- `../pay-bank-backend/docs/specifications/` — especificaciones funcionales.

Consúltala **antes** de escribir código del frontend que dependa de la API. Los cambios que afecten al backend se hacen en su propio proyecto, nunca se suplen con lógica en el frontend.

Con el backend en ejecución, la API está disponible en `http://localhost:8080` y su documentación interactiva en [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html).

## Funcionalidades principales

- **Inicio de sesión** — Formulario de login que obtiene el token JWT del backend; el resto de la aplicación solo es accesible con un usuario autenticado.
- **Panel** — Vista principal con el saldo total de la cuenta del usuario.
- **Pagos** — Formulario para realizar un pago a otro usuario indicando el destinatario, el importe y un concepto opcional.
- **Movimientos** — Listado paginado del histórico de movimientos de la cuenta (cargos y abonos).
- **Pagos realizados y recibidos** — Listados paginados, por separado, de los pagos enviados y de los pagos recibidos.

## Stack tecnológico

| Capa | Tecnología |
| --- | --- |
| Framework | Angular 22 (estable actual, LTS hasta junio de 2028) |
| Lenguaje | TypeScript ~6.0 |
| Estilos | SCSS con propiedades personalizadas de CSS (custom properties) |
| Enrutado | Angular Router (rutas con carga diferida) |
| Reactividad | Signals (inputs, outputs, signals) y RxJS para flujos asíncronos |
| Detección de cambios | Zoneless (sin Zone.js) |
| Build | Angular CLI application builder (esbuild / Vite) |
| Node.js | v24 LTS o superior |

## Arquitectura

La aplicación sigue una **arquitectura por capas** con **componentes standalone** (sin NgModules). Cada funcionalidad es un dominio aislado con sus propios componentes, servicios y modelos, y se carga de forma **diferida** (lazy loading) desde el router.

```
src/app
├── app.ts                      # Componente raíz
├── app.config.ts               # Providers de la aplicación
├── app.routes.ts               # Definición de rutas
├── core/                       # Infraestructura transversal y singleton
│   ├── guards/                 # Guards de rutas (p. ej. autenticación)
│   ├── interceptors/           # Interceptores HTTP (p. ej. token JWT, errores)
│   └── services/               # Servicios globales singleton (api, auth, logger)
├── features/                   # Dominios de negocio (una carpeta por funcionalidad)
│   ├── login/                  # Inicio de sesión
│   ├── dashboard/              # Panel con el saldo total
│   ├── payments/               # Nuevo pago, pagos realizados y pagos recibidos
│   └── movements/              # Histórico de movimientos de la cuenta
├── layout/                     # Estructura (shell) de la aplicación
│   ├── shell/                  # Layout principal: header + sidebar + contenido
│   ├── header/                 # Barra superior
│   ├── sidebar/                # Navegación lateral
│   └── footer/                 # Pie de página
└── shared/                     # Código reutilizable e independiente de las funcionalidades
    ├── components/             # Componentes de presentación (p. ej. page-title, paginator)
    ├── directives/             # Directivas reutilizables
    ├── pipes/                  # Pipes reutilizables (p. ej. formato de importes)
    └── models/                 # Modelos e interfaces compartidos (p. ej. PageResponse)
```

### Responsabilidades de cada capa

| Capa | Responsabilidad | Reglas |
| --- | --- | --- |
| **core** | Infraestructura transversal usada por toda la aplicación. | Solo contiene singletons: servicios globales, interceptores HTTP y guards de rutas. No contiene lógica específica de una funcionalidad ni componentes de UI. |
| **features** | Dominios de negocio. Cada funcionalidad es dueña de sus rutas, páginas, componentes y servicios. | Puede depender de `core`, `layout` y `shared`, pero nunca de otra funcionalidad. Cada funcionalidad se carga de forma diferida. |
| **layout** | La estructura de la aplicación (header, sidebar, footer y outlet del contenido principal). | Solo contiene componentes de layout. Sin lógica de negocio. |
| **shared** | Componentes de presentación, directivas, pipes y modelos reutilizables. | Independiente de las funcionalidades y sin lógica de negocio. Los componentes se comunican exclusivamente mediante inputs y outputs. |

### Reglas de dependencia

- Las dependencias fluyen **hacia abajo**: `app` → `core` / `features` / `layout` → `shared`.
- Las funcionalidades **no pueden** depender unas de otras.
- `shared` nunca importa de `features` ni de `core`.

## Convenciones

### Idioma

- **El código se escribe en inglés**: identificadores, nombres de ficheros, clases, selectores, comentarios en el código, textos de commits y nombres de rutas.
- La documentación del proyecto (este fichero y las especificaciones SDD) se escribe en español.

### Nomenclatura y estructura de componentes

- **Ficheros**: kebab-case siguiendo las convenciones de Angular CLI, p. ej. `payment-list.ts`, `payment-list.html`, `payment-list.scss`, `payment-list.spec.ts`.
- **Clases**: PascalCase, p. ej. `PaymentListComponent`.
- **Selectores**: kebab-case con prefijo `app-`, p. ej. `app-payment-list`.
- Las plantillas y estilos van siempre en **ficheros separados** (`templateUrl` / `styleUrl`), nunca en línea en los metadatos de `@Component`.

### Reactividad y plantillas

- Se prefieren los **signals** frente a propiedades planas para el estado reactivo. RxJS se usa para flujos y fuentes de datos asíncronas (p. ej. peticiones HTTP).
- Se usa el **control de flujo** integrado (`@if`, `@for`, `@switch`) en lugar de `*ngIf`, `*ngFor` o `*ngSwitch`.
- Los componentes usan detección de cambios **OnPush** (valor por defecto).

### Comunicación con el backend

- Toda llamada HTTP se hace desde servicios (de `core` o de la propia funcionalidad), nunca directamente desde los componentes.
- Los modelos de petición y respuesta reflejan exactamente los DTOs definidos en `../pay-bank-backend/docs/api/openapi.yaml`. Si el contrato cambia, se actualizan los modelos del frontend.
- La URL base de la API se configura en un único punto (environment / provider), nunca repartida por el código.

### Autenticación

- El inicio de sesión obtiene el token JWT del backend; un interceptor HTTP lo añade a la cabecera `Authorization: Bearer <token>` de cada petición.
- Todas las rutas, salvo la de login, están protegidas por un guard de autenticación.
- Una respuesta `401` del backend (token ausente, inválido o caducado) cierra la sesión en el frontend y redirige al login.
- El frontend nunca envía el identificador del usuario actual en las peticiones: el backend lo obtiene siempre del token.

### La paginación es siempre en servidor

El backend pagina y filtra en la consulta (objeto de paginación `PageQuery` → `Pageable`/`Page` de Spring Data) y devuelve un `*PageResponse` (`content`, `page`, `size`, `totalElements`, `totalPages`). Los servicios de las funcionalidades envían siempre `page`/`size` (y `q` cuando hay búsqueda); nunca se pagina ni se filtra en memoria.

### La búsqueda en servidor usa debounce

Los campos de búsqueda alimentan un flujo RxJS encadenado con `debounceTime`, `distinctUntilChanged` y `switchMap`, que vuelve a la página `0` y cancela cualquier petición en curso.

## Puesta en marcha

### Requisitos

- Node.js v24 LTS o superior
- npm
- El backend (`../pay-bank-backend`) en ejecución para usar la aplicación contra la API real

### Instalación

```bash
npm install
```

### Servidor de desarrollo

```bash
npm start
```

Abre `http://localhost:4200/` en el navegador. La aplicación se recarga automáticamente al cambiar el código fuente.

### Tests

```bash
npm test
```

### Build

```bash
npm run build
```

## Estado

Este proyecto está en una fase temprana de desarrollo. Las especificaciones se definen siguiendo el flujo SDD (spec → plan → task).
