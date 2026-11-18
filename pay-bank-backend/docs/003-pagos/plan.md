# Plan de implementación: Pagos

> Especificación: `003-pagos` · [spec.md](../../../docs/003-pagos/spec.md)
> Ámbito de la spec: Global · Proyecto de este plan: `pay-bank-backend`
> Otros planes de esta spec: `pay-bank-frontend/docs/003-pagos/plan.md`

## Alcance en este proyecto

- **RF**: RF-02 (dato del saldo disponible), RF-05, RF-06, RF-07, RF-08, RF-09, RF-10 (validaciones en servidor y realización del pago).
- **RN**: RN-01, RN-02 (importe > 0 y máximo 2 decimales; el separador decimal lo resuelve el frontend), RN-03, RN-04, RN-05, RN-06, RN-07, RN-08 (identificación del error mediante código; el texto lo pone el frontend).
- **CA**: CA-07, CA-08, CA-09, CA-10, CA-11 (datos), CA-12 (datos), CA-15, CA-16, CA-17 y la parte de servidor de CA-02, CA-04, CA-05, CA-06.

## 1. Resumen de la estrategia

1. Ampliar `openapi.yaml` con `GET /api/accounts/me` (saldo disponible) y `POST /api/payments` (realizar pago), y sus DTOs.
2. Añadir a `GlobalExceptionHandler` una propiedad `code` en los `ProblemDetail` y las excepciones de negocio del pago (`422`).
3. Añadir a `Account` las operaciones de cargo y abono y a `AccountRepository` la lectura con bloqueo pesimista.
4. Implementar `PaymentService` (validación, bloqueo de las dos cuentas en orden, comprobación de saldo, actualización de saldos, creación del pago y de sus dos movimientos en una transacción) y `PaymentController`.
5. Exponer el saldo con `AccountService.getMyAccount` en el `AccountController` existente.
6. Cubrir los CA con tests de integración, incluidos los de concurrencia y de fallo a mitad de pago.

## 2. Contexto técnico

- Ya existen (de `002-movimientos`): entidades `Account` (`balance` `numeric(19,2)`, 1-1 con `User`), `Payment` (origen, destino, importe, concepto nullable, `createdAt`) y `Movement` (`type`, `amount` positivo, `balanceAfter`, `createdAt`); `AccountRepository`, `PaymentRepository`, `MovementRepository`; `AccountService` y `AccountController implements AccountsApi` (`GET /api/accounts/me/movements`); `CurrentUserProvider`; `PageQuery`.
- Ya existen (de `001-inicio-sesion`): `User.normalizeAlias`, `UserRepository.findByAlias`, `Clock` inyectable con `MutableClock` en tests, `GlobalExceptionHandler` con `ProblemDetail` (`400` validación, `401`), tests MockMvc con login real y `TestDataFactory` (usuarios, cuentas, pagos con sus dos movimientos).
- Constitución: importes `BigDecimal`; un pago es una única transacción (`@Transactional` en servicio) que crea el `Payment`, genera los dos `Movement` y actualiza saldos; no se permite importe ≤ 0, pago a uno mismo ni saldo de origen negativo; usuario actual siempre desde el token; saldo = saldo inicial + movimientos.
- Base de datos: PostgreSQL en la aplicación y HSQLDB en tests; ambas admiten `SELECT … FOR UPDATE`.

## 3. Decisiones técnicas

- **DT-01**: Concurrencia con **bloqueo pesimista**: dentro de la transacción se obtienen los ids de las dos cuentas y se cargan con `PESSIMISTIC_WRITE` (`SELECT … FOR UPDATE`) **en orden ascendente de id**; después se comprueba el saldo, se actualizan ambos saldos y se crean los movimientos con el `balanceAfter` exacto.
  - Alternativas consideradas: bloqueo optimista con versión y reintentos; `UPDATE` condicional del saldo y relectura posterior.
  - Motivo: decisión del usuario; garantiza RN-06 y un `balanceAfter` coherente, y el orden fijo evita interbloqueos entre pagos cruzados (A→B y B→A a la vez).
- **DT-02**: Errores de negocio con **`422`** y `ProblemDetail` con propiedad `code`: `RECIPIENT_NOT_FOUND`, `SELF_PAYMENT`, `INSUFFICIENT_BALANCE`, `ACCOUNT_NOT_FOUND` (el usuario autenticado no tiene cuenta). Los errores de validación de entrada siguen siendo `400` y pasan a llevar `code = VALIDATION_ERROR` (incluidos los ya existentes de login y paginación). El frontend traduce el `code` al texto de RN-08.
  - Alternativas consideradas: un estado HTTP distinto por error (`404`, `409`, `422`).
  - Motivo: decisión del usuario; un único estado para reglas de negocio y un código estable que no depende del texto.
- **DT-03**: Orden de comprobaciones: (1) validación de entrada (`400`); (2) cuenta del usuario (`ACCOUNT_NOT_FOUND`); (3) destinatario por alias normalizado (RN-01) con cuenta (`RECIPIENT_NOT_FOUND`; un usuario sin cuenta se trata como inexistente); (4) destinatario distinto del usuario (`SELF_PAYMENT`); (5) bloqueo y saldo suficiente (`INSUFFICIENT_BALANCE`).
  - Alternativas consideradas: comprobar el saldo antes que el destinatario.
  - Motivo: los errores sobre el destinatario no dependen del estado de la cuenta y no requieren bloqueo.
- **DT-04**: Validación del importe: obligatorio y `> 0` con Bean Validation generada desde OpenAPI (`exclusiveMinimum`); el máximo de 2 decimales se comprueba en `PaymentService` (`stripTrailingZeros().scale() <= 2`) y se rechaza con `400 VALIDATION_ERROR`. El importe viaja como número JSON y se deserializa directamente a `BigDecimal` (sin pasar por `double`). Se guarda con escala 2.
  - Alternativas consideradas: importe como cadena en el contrato.
  - Motivo: coherente con `002-movimientos` (importes numéricos) y exacto al deserializar a `BigDecimal`.
- **DT-05**: Concepto: `maxLength: 140` validado con Bean Validation; en el servicio se aplica `trim()` y un concepto vacío tras el recorte se guarda como `null` (RN-04).
  - Alternativas consideradas: validar la longitud tras el recorte.
  - Motivo: la longitud es la del texto que el usuario escribió, igual que comprueba el formulario.
- **DT-06**: `Account` gana `debit(BigDecimal)` y `credit(BigDecimal)` que actualizan `balance`; la regla de saldo no negativo se comprueba en `PaymentService` antes de `debit` para lanzar la excepción de negocio. El `createdAt` del pago y de sus movimientos se toma del `Clock` inyectable.
  - Alternativas consideradas: actualizar saldos con consultas `UPDATE` directas.
  - Motivo: con las filas bloqueadas, el modelo de dominio basta y se mantiene legible.
- **DT-07**: Nuevo `GET /api/accounts/me` que devuelve `AccountResponse { balance }` (RF-02), en el `AccountController` existente. Lo reutilizará el panel.
  - Alternativas consideradas: devolver el saldo dentro de la respuesta de movimientos.
  - Motivo: recurso propio y reutilizable; no acopla el formulario de pago al listado.
- **DT-08**: No se usa clave de idempotencia: RF-13 se cumple en el frontend deshabilitando la acción mientras el pago está en curso.
  - Alternativas consideradas: cabecera `Idempotency-Key` con registro de claves.
  - Motivo: la spec no exige deduplicación en servidor; queda como riesgo aceptado.

## 4. Componentes afectados

| Componente | Proyecto | Tipo de cambio (nuevo / modificado / eliminado) | Relacionado con |
|---|---|---|---|
| `docs/api/openapi.yaml` | pay-bank-backend | modificado | RF-02, RF-05, RF-06, RF-10, DT-02, DT-04, DT-07 |
| `account/model/Account` | pay-bank-backend | modificado | RF-10, DT-06 |
| `account/repository/AccountRepository` | pay-bank-backend | modificado | RN-06, DT-01 |
| `account/service/AccountService`, `account/controller/AccountController` | pay-bank-backend | modificado | RF-02, DT-07 |
| `account/mapper/AccountMapper` | pay-bank-backend | nuevo | RF-02 |
| `payment/exception/*` | pay-bank-backend | nuevo | RF-07, RF-08, RF-09, DT-02 |
| `payment/service/PaymentService` | pay-bank-backend | nuevo | RF-05–RF-10, RN-01–RN-07, DT-01, DT-03–DT-06 |
| `payment/mapper/PaymentMapper` | pay-bank-backend | nuevo | RF-10 |
| `payment/controller/PaymentController` | pay-bank-backend | nuevo | RF-10, RN-07 |
| `exception/GlobalExceptionHandler` | pay-bank-backend | modificado | RN-08, DT-02 |
| Tests | pay-bank-backend | nuevo / modificado | CA del alcance |

## 5. Cambios necesarios

- **CB-01**: Definir en `openapi.yaml` `GET /api/accounts/me` (`operationId: getMyAccount`, tag `Accounts`) con `AccountResponse`, y `POST /api/payments` (`operationId: createPayment`, tag `Payments`) con `CreatePaymentRequest` y `PaymentResponse`, y las respuestas `400`/`401`/`422` con `ProblemDetail` (que pasa a documentar la propiedad `code`); regenerar `AccountsApi` y `PaymentsApi` (ver "Contratos e integraciones") — Cubre: RF-02, RF-05, RF-06, RF-10, RN-08
- **CB-02**: Añadir a `GlobalExceptionHandler` la propiedad `code` en los `ProblemDetail` (`VALIDATION_ERROR` en los `400` existentes) y el tratamiento de las excepciones de negocio del pago como `422` con su código — Cubre: RN-08
- **CB-03**: Crear en `payment/exception` `RecipientNotFoundException`, `SelfPaymentException`, `InsufficientBalanceException`, `InvalidPaymentAmountException` (`400`) y en `account/exception` `AccountNotFoundException` — Cubre: RF-05, RF-07, RF-08, RF-09
- **CB-04**: Añadir a `Account` `debit(BigDecimal)` y `credit(BigDecimal)`, y a `AccountRepository` `findIdByUserId(Long)` y `findByIdForUpdate(Long)` con `@Lock(PESSIMISTIC_WRITE)` — Cubre: RF-10, RN-06
- **CB-05**: Crear `payment/service/PaymentService.createPayment(alias, amount, concept)` (`@Transactional`): usuario de `CurrentUserProvider`, comprobaciones en el orden de DT-03, escala de DT-04, concepto de DT-05, bloqueo de DT-01, `debit`/`credit`, creación del `Payment` y de un `Movement` `CHARGE` (origen) y uno `CREDIT` (destino) con su `balanceAfter` y el `createdAt` del `Clock` — Cubre: RF-05, RF-07, RF-08, RF-09, RF-10, RN-01, RN-02, RN-03, RN-04, RN-05, RN-06, RN-07
- **CB-06**: Crear `payment/mapper/PaymentMapper` y `payment/controller/PaymentController implements PaymentsApi` que devuelve `201` con `PaymentResponse` — Cubre: RF-10
- **CB-07**: Añadir `AccountService.getMyAccount()` (cuenta del usuario actual o `AccountNotFoundException`), `account/mapper/AccountMapper` e implementar `getMyAccount` en `AccountController` — Cubre: RF-02
- **CB-08**: Tests (ver apartado 6), ampliando `TestDataFactory` con la creación de cuentas con saldo dado y usando `MockitoSpyBean` sobre `MovementRepository` para forzar un fallo a mitad del pago — Cubre: verificación de los CA del alcance

### Modelo de datos y migraciones

- Sin tablas ni columnas nuevas: se usan `accounts`, `payments` y `movements` creadas en `002-movimientos`.
- El importe y los saldos se guardan con escala 2.

### Contratos e integraciones

Contrato compartido con el frontend (fuente de verdad: `docs/api/openapi.yaml`). Todas las operaciones requieren `Authorization: Bearer`.

**`GET /api/accounts/me`** → `200 AccountResponse { balance: number (2 decimales) }`; `401`; `422 ACCOUNT_NOT_FOUND` si el usuario no tiene cuenta.

**`POST /api/payments`**

- Petición `CreatePaymentRequest { recipientAlias: string (obligatorio, minLength 1), amount: number (obligatorio, > 0, máximo 2 decimales), concept: string | null (opcional, maxLength 140) }`.
- `201 PaymentResponse { id: integer(int64), recipientAlias: string, amount: number, concept: string | null, createdAt: string(date-time, UTC), balanceAfter: number }` (`balanceAfter` = saldo del usuario tras el pago).
- `400 ProblemDetail` con `code = VALIDATION_ERROR` (campos ausentes o vacíos, importe ≤ 0 o con más de 2 decimales, concepto > 140).
- `401 ProblemDetail` (sin sesión).
- `422 ProblemDetail` con `code`: `RECIPIENT_NOT_FOUND`, `SELF_PAYMENT`, `INSUFFICIENT_BALANCE`, `ACCOUNT_NOT_FOUND`.

`ProblemDetail` documenta la propiedad opcional `code: string`. Los movimientos generados siguen el contrato de `pay-bank-backend/docs/002-movimientos/plan.md`.

### Configuración y seguridad

- Sin propiedades nuevas. Ambos endpoints quedan protegidos por la regla general `anyRequest().authenticated()`.
- La cuenta de origen se obtiene siempre del usuario del token (RN-07); la petición no tiene ningún campo de origen.

## 6. Estrategia de verificación

Tests de integración MockMvc (login real, HSQLDB, `TestDataFactory`) salvo que se indique otra cosa:

| Criterio de aceptación | Cómo se verifica (tipo de prueba / nivel) | Cambios implicados |
|---|---|---|
| CA-02 (datos) | `GET /api/accounts/me` de `ana` con 1015.00 → `balance = 1015.00` | CB-07 |
| CA-04 (servidor) | `amount` `0`, `-5` y `10.999` → `400 VALIDATION_ERROR`, sin pagos | CB-01, CB-05 |
| CA-05 (servidor) | `amount` `12.50` → `201` y `amount = 12.50` | CB-05 |
| CA-06 (servidor) | concepto de 141 caracteres → `400 VALIDATION_ERROR`; de 140 → `201` | CB-01 |
| CA-07 | destinatario `noexiste` → `422 RECIPIENT_NOT_FOUND`, saldo sin cambios, sin pagos | CB-05 |
| CA-08 | `ana` paga a `ANA` → `422 SELF_PAYMENT` | CB-05 |
| CA-09 | saldo 100.00, pago 100.01 → `422 INSUFFICIENT_BALANCE`, saldo 100.00 | CB-05 |
| CA-10 | saldo 100.00, pago 100.00 → `201`, saldo 0.00 | CB-05 |
| CA-11 (datos) | `ana` 1000.00 y `maria` 500.00; `ana` paga 250.00 a `Maria` con "Cena" → `201`; saldos 750.00 / 750.00; `GET /api/accounts/me/movements` de `ana`: primero `CHARGE`, `-250.00`, `maria`, "Cena", `balanceAfter 750.00`; de `maria`: `CREDIT`, `250.00`, `ana`, `balanceAfter 750.00` | CB-04, CB-05 |
| CA-12 (datos) | concepto `"   "` y concepto ausente → `201` y `concept = null` en el pago y en los movimientos | CB-05 |
| CA-15 | `MockitoSpyBean` sobre `MovementRepository` que falla al guardar el segundo movimiento → `500`; saldos sin cambios, sin `Payment` ni `Movement` de ese pago | CB-05 |
| CA-16 | test de servicio: dos hilos pagan 60.00 desde una cuenta con 100.00 → uno `INSUFFICIENT_BALANCE`, saldo final 40.00 y dos movimientos en total; además pagos cruzados `ana`→`maria` y `maria`→`ana` simultáneos terminan sin interbloqueo | CB-04, CB-05 |
| CA-17 | sin token → `401`, sin pagos | seguridad existente |
| CA-01, CA-03, CA-13, CA-14, CA-18 y la parte visible del resto | Se verifican en el frontend | `pay-bank-frontend` |

Además: comprobación manual en Swagger UI con los usuarios de `data.sql`.

## 7. Riesgos y mitigaciones

- **Doble pago por reintentos de red**: sin clave de idempotencia (DT-08), un reenvío del navegador podría duplicar el pago. Mitigación: botón deshabilitado durante el envío (frontend); se puede añadir idempotencia en una spec futura.
- **Bloqueos en HSQLDB vs PostgreSQL**: el comportamiento de `FOR UPDATE` difiere entre motores. Mitigación: el test de concurrencia se ejecuta en HSQLDB y la verificación manual en PostgreSQL.
- **Cambio en los `400` existentes**: añadir `code` a los `ProblemDetail` no rompe a los consumidores actuales (propiedad adicional).
- **Saldos de `data.sql`**: la sentencia que fija el saldo al último `balanceAfter` sigue siendo coherente con los pagos reales, que mantienen ambos valores.

## 8. Elementos de la spec no cubiertos

- RF-01, RF-03, RF-04, RF-11, RF-12, RF-13 → `pay-bank-frontend` (navegación, formulario, validación en pantalla, navegación tras pagar, conservación de datos, botón deshabilitado).
- RN-02 (separador decimal coma/punto) y RN-08 (textos de los mensajes) → `pay-bank-frontend`.
- CA-01, CA-03, CA-13, CA-14, CA-18 y la parte visible de CA-02, CA-04, CA-05, CA-06, CA-07, CA-08, CA-09, CA-11, CA-12 → `pay-bank-frontend`.
