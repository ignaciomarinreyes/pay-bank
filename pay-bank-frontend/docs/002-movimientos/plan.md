# Plan de implementación: Movimientos

> Especificación: `002-movimientos` · [spec.md](../../../docs/002-movimientos/spec.md)
> Ámbito de la spec: Global · Proyecto de este plan: `pay-bank-frontend`
> Otros planes de esta spec: `pay-bank-backend/docs/002-movimientos/plan.md`

## Alcance en este proyecto

- **RF**: RF-01, RF-04 (pide páginas de 20), RF-05 (presentación), RF-06, RF-07, RF-08, RF-09.
- **RN**: RN-01 (se muestra el signo recibido), RN-03, RN-04, RN-05.
- **CA**: CA-01, CA-06, CA-10, CA-13 y la parte visible de CA-03, CA-04, CA-05, CA-07, CA-08, CA-09, CA-11, CA-12.

## 1. Resumen de la estrategia

1. Configurar el locale español de la aplicación y crear en `shared` un pipe de importes en euros y un componente de paginación reutilizables.
2. Crear la funcionalidad `features/movements` (servicio, modelos y página) con carga diferida bajo el shell.
3. Reflejar la página actual en la URL (`?page=N`) y cargar los datos reaccionando a ese parámetro.
4. Añadir "Movimientos" a la navegación lateral.
5. Cubrir los CA con tests unitarios (Vitest) y verificación manual con los datos de `data.sql`.

## 2. Contexto técnico

- Implementado en `001-inicio-sesion`: `AuthService`, `authInterceptor` (añade el token y renueva ante `401`), `authGuard` en la ruta del shell (`canActivate` y `canActivateChild`), rutas con `withComponentInputBinding()`, `environment.apiBaseUrl`.
- Ya existen: `shared/models/page-response.ts` (`PageResponse<T>` con `content`, `page`, `size`, `totalElements`, `totalPages`), `shared/components/page-title`, `layout/sidebar` con `navItems: readonly NavItem[] = []` (`label`, `path`) y `routerLinkActive`, ruta del shell con `children: []`.
- No hay locale configurado (`LOCALE_ID` por defecto `en-US`), ni pipes, ni componente de paginación.
- Constitución: signals-first, zoneless, OnPush, control de flujo `@if`/`@for`, llamadas HTTP solo desde servicios, paginación siempre en servidor enviando `page`/`size`, funcionalidades aisladas y con carga diferida, `shared` sin lógica de negocio y comunicado por `input`/`output`.
- Contrato de la API: ver "Contratos e integraciones" en `pay-bank-backend/docs/002-movimientos/plan.md`.

## 3. Decisiones técnicas

- **DT-01**: Locale de la aplicación `es` (`registerLocaleData(localeEs)` y `LOCALE_ID = 'es'` en `app.config.ts`). Las fechas se formatean con `DatePipe` y el patrón `dd/MM/yyyy HH:mm`, que usa la zona horaria del navegador (RN-04).
  - Alternativas consideradas: pasar el locale en cada uso del pipe; `Intl.DateTimeFormat` a mano.
  - Motivo: un único punto de configuración, aplicable a futuras pantallas (panel, pagos).
- **DT-02**: Pipe `AmountPipe` (`appAmount`) en `shared/pipes`: formatea con `formatNumber(valor absoluto, 'es', '1.2-2')` y añade " €" detrás; con la opción `signed` antepone "+" o "-" según el signo del valor (RN-01, RN-03). Se usa con signo para el importe y sin signo para el saldo.
  - Alternativas consideradas: `CurrencyPipe` (no muestra "+" en positivos); `Intl.NumberFormat('es-ES')` (no agrupa miles en números de 4 cifras, "1234,56", contra RN-03).
  - Motivo: control exacto del formato exigido por RN-03.
- **DT-03**: La página actual vive en la URL como query param **en base 1** (`/movements?page=2` = "Página 2"); al API se envía en base 0. Un valor ausente, no numérico o menor que 1 se trata como 1. Si la página pedida es mayor que el total de páginas (y hay movimientos), se sustituye la URL por la última página (`replaceUrl`).
  - Alternativas consideradas: estado interno del componente (se pierde al recargar).
  - Motivo: decisión del usuario; recarga, enlace y botón Atrás mantienen la página.
- **DT-04**: Carga de datos reactiva con APIs estables: `page` llega como `input()` (query param, por `withComponentInputBinding`), se convierte en un `computed` numérico, y `toObservable` + `switchMap` (cancela la petición anterior) + `toSignal` producen un estado `{ status: 'loading' | 'loaded' | 'error', data? }`.
  - Alternativas consideradas: `rxResource` / `httpResource`.
  - Motivo: patrón ya probado y estable en el proyecto (signals + RxJS para HTTP, según la constitución).
- **DT-05**: Componente de presentación `PaginatorComponent` (`app-paginator`) en `shared/components`: `input` `page` (base 1) y `totalPages`, `output` `pageChange`; muestra "Anterior", "Página X de Y" y "Siguiente", deshabilitando el botón no aplicable (RF-06, RF-07). La página de movimientos lo oculta cuando no hay movimientos (RF-08).
  - Alternativas consideradas: controles dentro de la página de movimientos.
  - Motivo: la constitución prevé un paginador compartido; lo reutilizarán los listados de pagos.
- **DT-06**: El listado se presenta como tabla con columnas Fecha, Tipo, Contraparte, Concepto, Importe y Saldo, en el orden recibido del backend (el orden lo garantiza el servidor). `CHARGE` → "Cargo", `CREDIT` → "Abono"; concepto `null` o vacío → "—" (RN-05). Mientras carga se muestra "Cargando movimientos…".
  - Alternativas consideradas: lista de tarjetas.
  - Motivo: lectura comparativa de importes y saldos; en móvil la tabla se desplaza horizontalmente dentro de su contenedor.
- **DT-07**: Ante cualquier error de la consulta (salvo `401`, que ya gestiona el interceptor) se muestra el mensaje de RF-09 en lugar de la tabla y del paginador.
  - Alternativas consideradas: botón "Reintentar".
  - Motivo: no está en la spec; el usuario puede recargar.

## 4. Componentes afectados

| Componente | Proyecto | Tipo de cambio (nuevo / modificado / eliminado) | Relacionado con |
|---|---|---|---|
| `app.config.ts` | pay-bank-frontend | modificado | RN-03, RN-04, DT-01 |
| `shared/pipes/amount.pipe.ts` | pay-bank-frontend | nuevo | RN-01, RN-03, DT-02 |
| `shared/components/paginator/paginator.{ts,html,scss}` | pay-bank-frontend | nuevo | RF-06, RF-07, DT-05 |
| `features/movements/movements.models.ts` | pay-bank-frontend | nuevo | backend:CB-07 |
| `features/movements/movements.service.ts` | pay-bank-frontend | nuevo | RF-04 |
| `features/movements/movements-page/movements-page.{ts,html,scss}` | pay-bank-frontend | nuevo | RF-04–RF-09, RN-04, RN-05, DT-03, DT-04, DT-06, DT-07 |
| `features/movements/movements.routes.ts` | pay-bank-frontend | nuevo | RF-01 |
| `app.routes.ts` | pay-bank-frontend | modificado | RF-01 |
| `layout/sidebar/sidebar.ts` | pay-bank-frontend | modificado | RF-01 |
| Tests `*.spec.ts` | pay-bank-frontend | nuevo | CA del alcance |

## 5. Cambios necesarios

- **CB-01**: Registrar el locale `es` y proveer `LOCALE_ID = 'es'` en `app.config.ts` — Cubre: RN-03, RN-04
- **CB-02**: Crear `AmountPipe` según DT-02 — Cubre: RN-01, RN-03
- **CB-03**: Crear `PaginatorComponent` según DT-05 — Cubre: RF-06, RF-07
- **CB-04**: Crear `features/movements/movements.models.ts` con `MovementType` (`'CHARGE' | 'CREDIT'`) y `MovementResponse`, reflejo exacto del contrato de backend:CB-07; la página usa `PageResponse<MovementResponse>` de `shared/models` — Cubre: RF-05
- **CB-05**: Crear `MovementsService.getMyMovements(page: number): Observable<PageResponse<MovementResponse>>` (`GET {apiBaseUrl}/api/accounts/me/movements` con `page` en base 0 y `size = 20`) — Cubre: RF-04
- **CB-06**: Crear `MovementsPageComponent` (`app-movements-page`): título "Movimientos" con `app-page-title`, gestión de la página según DT-03, carga según DT-04, tabla según DT-06 con `DatePipe` (`dd/MM/yyyy HH:mm`) y `AmountPipe` (importe con signo, saldo sin signo), mensaje "No hay movimientos" sin paginador cuando `totalElements = 0`, mensaje de error de DT-07, y `PaginatorComponent` que navega con `router.navigate([], { queryParams: { page } })` — Cubre: RF-04, RF-05, RF-06, RF-07, RF-08, RF-09, RN-04, RN-05
- **CB-07**: Crear `movements.routes.ts` y registrar `{ path: 'movements', loadChildren: … }` como hija de la ruta del shell en `app.routes.ts` (protegida por el `authGuard` existente) — Cubre: RF-01
- **CB-08**: Añadir `{ label: 'Movimientos', path: '/movements' }` a `navItems` del `SidebarComponent` — Cubre: RF-01

### Modelo de datos y migraciones

### Contratos e integraciones

- Consume `GET /api/accounts/me/movements` tal como se define en `pay-bank-backend/docs/002-movimientos/plan.md` ("Contratos e integraciones") y en `pay-bank-backend/docs/api/openapi.yaml`. No se duplica aquí.
- El token lo añade el `authInterceptor` existente; nunca se envía el identificador del usuario ni de la cuenta.

### Configuración y seguridad

- `LOCALE_ID = 'es'` para toda la aplicación (CB-01). Sin otras variables nuevas.

## 6. Estrategia de verificación

Tests unitarios con Vitest (`ng test`) y `HttpTestingController`:

| Criterio de aceptación | Cómo se verifica (tipo de prueba / nivel) | Cambios implicados |
|---|---|---|
| CA-01 | Unitario del sidebar: existe el enlace "Movimientos" a `/movements`. Unitario de rutas: `/movements` con sesión muestra la página de movimientos | CB-07, CB-08 |
| CA-03 (visible) | Unitario de la página: las filas se pintan en el orden recibido del servicio | CB-06 |
| CA-04 (visible) | Unitario de la página: sin `page` en la URL se pide `page=0&size=20`; con respuesta de 20 elementos y `totalPages = 3` se pintan 20 filas y "Página 1 de 3" | CB-05, CB-06 |
| CA-05 | Unitario del paginador: en la página 3 de 3 "Siguiente" está deshabilitado. Unitario de la página: con `?page=3` se pide `page=2` y se pinta "Página 3 de 3" | CB-03, CB-06 |
| CA-06 | Unitario del paginador: en la página 1 "Anterior" está deshabilitado; en la 2, pulsar "Anterior" emite `pageChange(1)`. Unitario de la página: `pageChange` navega a `?page=1` | CB-03, CB-06 |
| CA-07, CA-08 (visible) | Unitario del pipe: `-1234.56` con signo → "-1.234,56 €", `50` → "+50,00 €". Unitario de la página: `CHARGE` → "Cargo", `CREDIT` → "Abono", contraparte y concepto visibles | CB-02, CB-06 |
| CA-09 (visible) | Unitario del pipe sin signo: `150` → "150,00 €", `120` → "120,00 €" | CB-02 |
| CA-10 | Unitario de la página: un `createdAt` construido para el 18/11/2026 14:05 en la zona horaria local se muestra "18/11/2026 14:05" | CB-01, CB-06 |
| CA-11 | Unitario de la página: `concept: null` → "—" | CB-06 |
| CA-12 (visible) | Unitario de la página: respuesta con `totalElements = 0` → "No hay movimientos" y sin paginador | CB-06 |
| CA-13 | Unitario de la página: respuesta `500` → mensaje de RF-09, sin tabla ni paginador | CB-06 |
| CA-02, CA-14 | Se verifican en el backend | `pay-bank-backend` |

Además: tests de DT-03 (`?page=abc` y `?page=0` → página 1; página mayor que el total → URL reemplazada por la última) y verificación manual en el navegador con los datos de `data.sql` (varias páginas, recarga en una página intermedia, botón Atrás).

## 7. Riesgos y mitigaciones

- **Agrupación de miles en el locale `es`**: `Intl` no agrupa números de 4 cifras en español; si `formatNumber` de Angular se comportara igual, CA-07 fallaría. Mitigación: el test del pipe con `1234.56` lo detecta y, en ese caso, el pipe inserta el separador de miles por sí mismo (mismo contrato, sin cambiar el plan).
- **Zona horaria de los tests**: CA-10 depende de la zona del entorno. Mitigación: el test construye la fecha esperada a partir de una fecha local, no de una cadena UTC fija.
- **`LOCALE_ID` global**: afecta a cualquier pipe de fecha o número futuro; es el comportamiento deseado para una aplicación en español.

## 8. Elementos de la spec no cubiertos

- RF-02, RF-03 (orden), RN-02, RN-06, RN-07 → `pay-bank-backend` (filtrado por usuario, orden, contraparte y saldo tras el movimiento).
- CA-02, CA-14 y la parte de datos de CA-03, CA-04, CA-05, CA-07, CA-08, CA-09, CA-11, CA-12 → `pay-bank-backend`.
