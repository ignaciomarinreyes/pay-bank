# Plan de implementación: Movimientos

> Especificación: `002-movimientos` · [spec.md](../../../docs/002-movimientos/spec.md)
> Ámbito de la spec: Global · Proyecto de este plan: `pay-bank-backend`
> Otros planes de esta spec: `pay-bank-frontend/docs/002-movimientos/plan.md`

## Alcance en este proyecto

- **RF**: RF-02, RF-03, RF-04 (tamaño de página en servidor), RF-05 (datos de cada movimiento), RF-06 y RF-07 (datos de paginación: página, total de páginas).
- **RN**: RN-01 (signo del importe en los datos), RN-02, RN-06, RN-07.
- **CA**: CA-02, CA-03, CA-14 y la parte de datos de CA-04, CA-05, CA-07, CA-08, CA-09, CA-11, CA-12.

## 1. Resumen de la estrategia

1. Crear el modelo de dominio de la constitución: `Account` (1-1 con `User`), `Payment` (cuenta de origen y destino) y `Movement` (cargo/abono de una cuenta, ligado a su `Payment`). En esta funcionalidad no se crean pagos desde la API.
2. Añadir el objeto de paginación compartido `PageQuery` y el `CurrentUserProvider` previstos en la constitución.
3. Definir en `openapi.yaml` la consulta paginada de movimientos del usuario actual y sus DTOs.
4. Implementar repositorio (consulta paginada con proyección y orden fijo), servicio, mapper y controlador.
5. Ampliar `data.sql` con cuentas, pagos y movimientos de ejemplo para uso manual.
6. Ajustar la constitución para el saldo inicial y cubrir los CA con tests.

## 2. Contexto técnico

- Implementado en `001-inicio-sesion`: `User`, `UserRepository`, `JwtAuthFilter` (principal `AuthenticatedUser(id, alias)`), `SecurityConfig` (todo lo no público exige autenticación → `401` `ProblemDetail`), `GlobalExceptionHandler`, `Clock` inyectable, `data.sql` idempotente (`ON CONFLICT`) solo en la aplicación (`spring.sql.init.mode: never` en tests), tests con `@SpringBootTest` + MockMvc + `MutableClock` y `@DataJpaTest`.
- Paquetes `account/*`, `payment/*` y `common/web` solo tienen `package-info.java`. No existen `PageQuery` ni `CurrentUserProvider`.
- Constitución: package-by-feature, controladores que implementan interfaces generadas, importes `BigDecimal`, paginación siempre en servidor con `PageQuery` (`page = 0`, `size = 20`, `q` opcional) y respuestas `*PageResponse` (`content`, `page`, `size`, `totalElements`, `totalPages`), usuario actual siempre desde el token.

## 3. Decisiones técnicas

- **DT-01**: Se crea ahora el modelo completo `Account` + `Payment` + `Movement`. `Movement` referencia su `Payment`; la contraparte y el concepto se obtienen del pago, no se duplican.
  - Alternativas consideradas: `Movement` con alias de contraparte y concepto copiados, creando `Payment` en la spec de Pagos (obligaría a migrar datos después).
  - Motivo: decisión del usuario; sigue el modelo de la constitución y la spec de Pagos solo añadirá la lógica de creación.
- **DT-02**: `Movement` guarda `type` (`CHARGE` / `CREDIT`), `amount` positivo, `balanceAfter` (saldo tras aplicarlo, RN-07) y `createdAt` (igual al del pago). El saldo tras el movimiento se persiste al crearlo, no se recalcula al consultar.
  - Alternativas consideradas: calcular `balanceAfter` con una función de ventana sobre todos los movimientos de la cuenta en cada consulta.
  - Motivo: la consulta paginada no tiene que recorrer todo el histórico; el pago (spec de Pagos) ya conoce el saldo resultante dentro de su transacción.
- **DT-03**: `Account.balance` (`BigDecimal`, escala 2) almacena el saldo actual = saldo inicial + suma de movimientos. El saldo inicial no es un movimiento.
  - Alternativas consideradas: movimiento de apertura (contradice la spec: los movimientos son cargos y abonos de pagos).
  - Motivo: decisión del usuario; sin saldo de partida el primer pagador quedaría en negativo. Se matiza la constitución (CB-12).
- **DT-04**: Consulta con proyección JPQL (`MovementRow`) que une `Movement` → `Payment` → cuentas → usuarios y resuelve la contraparte en la propia consulta (`CHARGE` → alias del destino; `CREDIT` → alias del origen), filtrando por `account.user.id` del usuario actual. Orden fijo `createdAt desc, id desc` (RF-03, RN-06) dentro de la consulta, con `countQuery` explícita.
  - Alternativas consideradas: cargar entidades con `join fetch` y mapear en memoria (más consultas y datos); ordenar con `Sort` del `Pageable` (permitiría que el cliente cambie el orden).
  - Motivo: una consulta por página más el recuento; orden no manipulable.
- **DT-05**: La API devuelve `amount` **con signo** (negativo en `CHARGE`, positivo en `CREDIT`), `balanceAfter` sin signo explícito, `createdAt` en ISO-8601 UTC y `type`. El formato visible (euros, separadores, fecha local) es responsabilidad del frontend.
  - Alternativas consideradas: `amount` siempre positivo y signo deducido del `type` en el frontend.
  - Motivo: el dato ya expresa el sentido del dinero (RN-01) y evita duplicar la regla en el cliente.
- **DT-06**: `PageQuery` (`common/web`) como `record(page, size, q)` con valores por defecto `0` / `20`, validación `page ≥ 0` y `1 ≤ size ≤ 100`, y conversión a `Pageable` sin orden. El parámetro `q` no se usa en este endpoint (sin búsqueda, ver spec "Fuera de alcance"). Parámetros fuera de rango → `400` `ProblemDetail`.
  - Alternativas consideradas: aceptar cualquier `size` (permitiría pedir listas sin límite, contra la constitución).
  - Motivo: convención de la constitución y límite explícito.
- **DT-07**: `CurrentUserProvider` (`security/`) devuelve el `AuthenticatedUser` del `SecurityContext`; el endpoint es `GET /api/accounts/me/movements` (sin identificador de usuario ni de cuenta en la URL).
  - Alternativas consideradas: `GET /api/accounts/{id}/movements` con comprobación de propiedad.
  - Motivo: la constitución exige obtener el usuario del token, nunca de la petición.
- **DT-08**: Si el usuario no tiene cuenta, la consulta devuelve una página vacía (`totalElements = 0`) en lugar de un error.
  - Alternativas consideradas: `404` "cuenta no encontrada".
  - Motivo: para el usuario es una cuenta sin movimientos (RF-08 en frontend); evita un caso de error sin valor funcional.
- **DT-09**: Datos de ejemplo en `data.sql`: cuentas para `ana`, `maria` y un nuevo usuario `luis` con saldo inicial 1.000,00 €, ~30 pagos entre `ana` y `maria` (algunos sin concepto) y uno entre `maria` y `luis`, con sus movimientos y `balanceAfter` calculados en el propio script (función de ventana). Todas las sentencias son idempotentes (`ON CONFLICT` / `WHERE NOT EXISTS`) y el saldo de cada cuenta se fija al `balanceAfter` de su último movimiento.
  - Alternativas consideradas: solo cuentas sin movimientos en `data.sql`.
  - Motivo: decisión del usuario; permite probar a mano varias páginas y los casos de la spec.

## 4. Componentes afectados

| Componente | Proyecto | Tipo de cambio (nuevo / modificado / eliminado) | Relacionado con |
|---|---|---|---|
| `docs/api/openapi.yaml` | pay-bank-backend | modificado | RF-04, RF-05, RF-07, DT-05, DT-06 |
| `account/model/Account` | pay-bank-backend | nuevo | DT-01, DT-03 |
| `account/model/Movement`, `account/model/MovementType` | pay-bank-backend | nuevo | RN-01, RN-07, DT-01, DT-02 |
| `payment/model/Payment` | pay-bank-backend | nuevo | RN-02, DT-01 |
| `account/repository/AccountRepository` | pay-bank-backend | nuevo | DT-01 |
| `account/repository/MovementRepository`, `account/repository/MovementRow` | pay-bank-backend | nuevo | RF-02, RF-03, RF-04, RN-02, RN-06, DT-04 |
| `payment/repository/PaymentRepository` | pay-bank-backend | nuevo | DT-01 |
| `common/web/PageQuery` | pay-bank-backend | nuevo | RF-04, DT-06 |
| `security/CurrentUserProvider` | pay-bank-backend | nuevo | RF-02, DT-07 |
| `account/mapper/MovementMapper` | pay-bank-backend | nuevo | RF-05, RN-01, DT-05 |
| `account/service/AccountService` | pay-bank-backend | nuevo | RF-02, RF-04, DT-08 |
| `account/controller/AccountController` | pay-bank-backend | nuevo | RF-02, RF-04, RF-07 |
| `exception/GlobalExceptionHandler` | pay-bank-backend | modificado | DT-06 |
| `src/main/resources/data.sql` | pay-bank-backend | modificado | DT-09 |
| `docs/constitution.md` | pay-bank-backend | modificado | DT-03 |
| Tests | pay-bank-backend | nuevo | CA del alcance |

## 5. Cambios necesarios

- **CB-01**: Crear la entidad `Account` (tabla `accounts`: `id`, `user` `@OneToOne` único no nulo, `balance` `numeric(19,2)` no nulo) y `AccountRepository` — Cubre: DT-01, DT-03
- **CB-02**: Crear la entidad `Payment` (tabla `payments`: `id`, `originAccount` y `destinationAccount` `@ManyToOne` no nulos, `amount` `numeric(19,2)` no nulo, `concept` nullable, `createdAt` no nulo) y `PaymentRepository` — Cubre: RN-02
- **CB-03**: Crear `MovementType` (`CHARGE`, `CREDIT`, persistido como texto) y la entidad `Movement` (tabla `movements`: `id`, `account` y `payment` `@ManyToOne` no nulos, `type`, `amount` positivo y `balanceAfter` `numeric(19,2)`, `createdAt`; índice `(account_id, created_at, id)`) — Cubre: RN-01, RN-07
- **CB-04**: Crear `MovementRow` (proyección: `id`, `createdAt`, `type`, `amount`, `balanceAfter`, `concept`, `counterpartyAlias`) y `MovementRepository.findPageByUserId(Long userId, Pageable)` con la consulta y `countQuery` de DT-04 — Cubre: RF-02, RF-03, RF-04, RN-02, RN-06
- **CB-05**: Crear `common/web/PageQuery` según DT-06 y su traducción a `400` en `GlobalExceptionHandler` (parámetros de paginación inválidos) — Cubre: RF-04
- **CB-06**: Crear `security/CurrentUserProvider` (`currentUser(): AuthenticatedUser`; si no hay usuario autenticado lanza una excepción, caso que la seguridad ya impide) — Cubre: RF-02
- **CB-07**: Definir en `openapi.yaml` (tag `Accounts`) `GET /api/accounts/me/movements` (`operationId: getMyMovements`) con parámetros de consulta `page` y `size`, y los esquemas `MovementResponse` y `MovementPageResponse` (ver "Contratos e integraciones"); regenerar `AccountsApi` — Cubre: RF-04, RF-05, RF-07
- **CB-08**: Crear `account/mapper/MovementMapper` (`MovementRow` → `MovementResponse`, aplicando el signo de DT-05 y convirtiendo `createdAt` a `OffsetDateTime` UTC) — Cubre: RF-05, RN-01
- **CB-09**: Crear `account/service/AccountService.getMyMovements(PageQuery)`: usuario de `CurrentUserProvider`, consulta paginada, mapeo y construcción de `MovementPageResponse` (página vacía si no hay cuenta, DT-08). Transacción de solo lectura — Cubre: RF-02, RF-04, RF-07
- **CB-10**: Crear `account/controller/AccountController implements AccountsApi` que construye `PageQuery` con `page`/`size` y delega en `AccountService` — Cubre: RF-02, RF-04
- **CB-11**: Ampliar `data.sql` según DT-09 (usuario `luis` con contraseña documentada en comentario, cuentas, pagos y movimientos idempotentes y saldos coherentes) — Cubre: datos para la verificación manual
- **CB-12**: Matizar en `docs/constitution.md` el modelo de dominio: "El saldo de una Account es su saldo inicial más la suma de sus movimientos" — Cubre: DT-03
- **CB-13**: Tests (ver apartado 6) con un helper de test que crea usuarios, cuentas, pagos y sus dos movimientos con `balanceAfter` y `createdAt` controlados — Cubre: verificación de los CA del alcance

### Modelo de datos y migraciones

- `accounts`: `id` (PK), `user_id` (FK a `users`, único, no nulo), `balance` (`numeric(19,2)`, no nulo).
- `payments`: `id` (PK), `origin_account_id`, `destination_account_id` (FK a `accounts`, no nulos), `amount` (`numeric(19,2)`, no nulo), `concept` (varchar, nullable), `created_at` (no nulo).
- `movements`: `id` (PK), `account_id` (FK a `accounts`), `payment_id` (FK a `payments`), `type` (varchar: `CHARGE`/`CREDIT`), `amount` (`numeric(19,2)`, positivo), `balance_after` (`numeric(19,2)`), `created_at`; índice `(account_id, created_at, id)`.
- Sin migraciones: lo crea Hibernate (`ddl-auto`). Las reglas de creación de pagos (importe > 0, no a uno mismo, saldo no negativo) quedan para la spec de Pagos.

### Contratos e integraciones

Contrato compartido con el frontend (fuente de verdad: `docs/api/openapi.yaml`):

`GET /api/accounts/me/movements?page={page}&size={size}` (requiere `Authorization: Bearer`)

| Parámetro | Tipo | Defecto | Restricción |
|---|---|---|---|
| `page` | integer | 0 | ≥ 0 (base 0) |
| `size` | integer | 20 | 1–100 |

Respuestas: `200` `MovementPageResponse`; `400` `ProblemDetail` (parámetros fuera de rango); `401` `ProblemDetail` (sin sesión).

- `MovementPageResponse { content: MovementResponse[], page: integer, size: integer, totalElements: integer(int64), totalPages: integer }`. Una página posterior a la última devuelve `content: []` con los totales reales.
- `MovementResponse { id: integer(int64), createdAt: string(date-time, UTC), type: "CHARGE" | "CREDIT", amount: number (2 decimales, negativo si CHARGE), balanceAfter: number (2 decimales), counterpartyAlias: string, concept: string | null }`.
- Orden: `createdAt` descendente y, a igualdad, `id` descendente.

### Configuración y seguridad

- Sin propiedades nuevas. El endpoint queda protegido por la regla general `anyRequest().authenticated()` existente.
- `data.sql` añade el usuario de prueba `luis` (hash BCrypt, contraseña en claro documentada en el comentario del fichero).

## 6. Estrategia de verificación

| Criterio de aceptación | Cómo se verifica (tipo de prueba / nivel) | Cambios implicados |
|---|---|---|
| CA-02 | Integración MockMvc: pago `maria` → `luis`; `ana` consulta y ese pago no aparece; `luis` sí lo ve | CB-04, CB-06, CB-09 |
| CA-03 | `@DataJpaTest` y MockMvc: tres movimientos con fechas desordenadas → orden descendente; dos con igual `createdAt` → primero el de mayor `id` | CB-04 |
| CA-04 (datos) | MockMvc: 45 movimientos → `content` de 20, `page = 0`, `totalPages = 3`, `totalElements = 45` | CB-04, CB-09 |
| CA-05 (datos) | MockMvc: `page = 2` → 5 movimientos, los más antiguos | CB-04, CB-09 |
| CA-07 (datos) | MockMvc: pago `ana` → `maria` de 1234.56 "Alquiler" → para `ana` `type = CHARGE`, `amount = -1234.56`, `counterpartyAlias = maria`, `concept = Alquiler` | CB-04, CB-08 |
| CA-08 (datos) | MockMvc: pago `maria` → `ana` de 50 → para `ana` `type = CREDIT`, `amount = 50.00`, `counterpartyAlias = maria` | CB-04, CB-08 |
| CA-09 (datos) | MockMvc: movimientos con `balanceAfter` 150.00 y 120.00 → se devuelven tal cual | CB-03, CB-08 |
| CA-11 (datos) | MockMvc: pago sin concepto → `concept = null` | CB-08 |
| CA-12 (datos) | MockMvc: usuario sin movimientos y usuario sin cuenta → `content = []`, `totalElements = 0`, `totalPages = 0` | CB-09 |
| CA-14 | MockMvc: sin token → `401` `ProblemDetail` y sin datos | CB-10 (seguridad existente) |
| CA-01, CA-06, CA-10, CA-13 | Se verifican en el frontend | `pay-bank-frontend` |

Además: tests de `PageQuery` (`page = -1` y `size = 0` / `101` → `400`), y comprobación manual al arrancar con PostgreSQL de que `data.sql` crea los datos, que reiniciar no los duplica y que el saldo de cada cuenta coincide con el `balanceAfter` de su último movimiento.

## 7. Riesgos y mitigaciones

- **`data.sql` más complejo y solo PostgreSQL**: no se ejecuta en tests. Mitigación: comprobación manual al arrancar (ver apartado 6).
- **Saldos de ejemplo incoherentes al convivir con pagos reales** (cuando exista la spec de Pagos): la sentencia que fija el saldo usa el `balanceAfter` del último movimiento, que los pagos reales también mantendrán, por lo que sigue siendo idempotente.
- **Concurrencia de `balanceAfter`**: lo asignará la spec de Pagos dentro de su transacción; esta funcionalidad solo lee.
- **Cambio de la constitución** (DT-03): matiz explícito del modelo, no cambia ningún comportamiento ya implementado.

## 8. Elementos de la spec no cubiertos

- RF-01, RF-08, RF-09 → `pay-bank-frontend` (navegación, mensaje de lista vacía y de error).
- RF-05 (formato visible), RF-06 y RF-07 (controles y texto "Página X de Y") → `pay-bank-frontend`, sobre los datos de este plan.
- RN-03, RN-04, RN-05 (formato de importes, fechas y concepto vacío) → `pay-bank-frontend`.
- CA-01, CA-06, CA-10, CA-13 y la parte visible de CA-04, CA-05, CA-07, CA-08, CA-09, CA-11, CA-12 → `pay-bank-frontend`.
