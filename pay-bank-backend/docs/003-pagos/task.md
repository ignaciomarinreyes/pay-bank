# Tareas de implementación: Pagos

> Especificación: `003-pagos` · [spec.md](../../../docs/003-pagos/spec.md) · Plan: [plan.md](plan.md)
> Ámbito de la spec: Global · Proyecto de estas tareas: `pay-bank-backend`

> Estados: `[ ]` pendiente · `[~]` en curso · `[x]` completada

## Orden de ejecución

```
(T-01 ∥ T-02 ∥ T-03) → (T-04 ∥ T-05) → T-06 → T-07
```

- T-01 (contrato), T-02 (errores con `code`) y T-03 (operaciones de saldo y bloqueo) no dependen de nada.
- T-04 (saldo disponible) necesita T-01 y T-02; T-05 (servicio de pago) necesita T-02 y T-03.
- T-06 expone el pago y lo verifica de extremo a extremo; T-07 es la comprobación manual en PostgreSQL.

## Tareas

### [x] T-01: Definir el contrato de pagos en OpenAPI

- **Qué hacer**: En `docs/api/openapi.yaml`, añadir `createPayment` (`POST /api/payments`, tag `Payments`) con los esquemas `CreatePaymentRequest` y `PaymentResponse`, las respuestas `400`/`401`/`422` y la propiedad `code` en `ProblemDetail`, según "Contratos e integraciones" del plan (la operación `getMyAccount` se añade en T-04 junto con su implementación). Resultado: `mvn compile` genera `PaymentsApi` y los dos modelos (`amount` como `BigDecimal` con mínimo exclusivo 0, `concept` con `@Size(max = 140)`) y compila; Swagger UI muestra la operación. (CB-01, DT-02, DT-04 · RF-05, RF-06, RF-10, RN-08)

### [x] T-02: Añadir el código de error a las respuestas y las excepciones de negocio del pago

- **Qué hacer**: Crear `payment/exception/RecipientNotFoundException`, `SelfPaymentException`, `InsufficientBalanceException`, `InvalidPaymentAmountException` y `account/exception/AccountNotFoundException`; en `exception/GlobalExceptionHandler`, añadir la propiedad `code` a los `ProblemDetail` (`VALIDATION_ERROR` en los `400` existentes, `400` para `InvalidPaymentAmountException`) y tratar las excepciones de negocio como `422` con su código. Resultado: los tests existentes siguen pasando y se amplía `AuthControllerTests` para comprobar que el `400` de login vacío incluye `code = VALIDATION_ERROR`; un test MockMvc con un controlador de prueba en `src/test` comprueba el `422` y el `code` de cada excepción de negocio. (CB-02, CB-03, DT-02 · RF-05, RF-07, RF-08, RF-09, RN-08)

### [x] T-03: Añadir cargo, abono y lectura con bloqueo de cuentas

- **Qué hacer**: Añadir a `account/model/Account` `debit(BigDecimal)` y `credit(BigDecimal)`, y a `account/repository/AccountRepository` `findIdByUserId(Long)` y `findByIdForUpdate(Long)` con `@Lock(PESSIMISTIC_WRITE)`. Resultado: test `@DataJpaTest` que comprueba que `findIdByUserId` devuelve el id de la cuenta del usuario (vacío sin cuenta), que `findByIdForUpdate` carga la cuenta y que `debit`/`credit` actualizan y persisten el saldo con escala 2. (CB-04, DT-01, DT-06 · RF-10, RN-06)

### [x] T-04: Exponer el saldo disponible del usuario

- **Qué hacer**: Añadir a `docs/api/openapi.yaml` `getMyAccount` (`GET /api/accounts/me`, tag `Accounts`) con `AccountResponse`; añadir `AccountService.getMyAccount()`, crear `account/mapper/AccountMapper` e implementar `getMyAccount` en `account/controller/AccountController`. Resultado: `mvn compile` pasa y tests MockMvc comprueban: `ana` con 1015.00 → `200` y `balance = 1015.00`; usuario sin cuenta → `422` con `code = ACCOUNT_NOT_FOUND`; sin token → `401`. (CB-01, CB-07, DT-07 · RF-02, CA-02 · depende de T-01, T-02)

### [x] T-05: Implementar el servicio de pago

- **Qué hacer**: Crear `payment/service/PaymentService.createPayment` según CB-05 (orden de DT-03, escala de DT-04, concepto de DT-05, bloqueo en orden de id de DT-01, `debit`/`credit`, `Payment` y sus movimientos `CHARGE`/`CREDIT` con `balanceAfter` y `createdAt` del `Clock`) y ampliar `TestDataFactory` para crear cuentas con un saldo dado. Resultado: tests de servicio con usuario autenticado en el `SecurityContext` que comprueban cada excepción en su caso; saldo exacto (100.00 → paga 100.00 → 0.00); importe con 3 decimales rechazado; concepto `"   "` guardado como `null`; dos hilos pagando 60.00 desde 100.00 → un éxito, un `InsufficientBalanceException`, saldo 40.00; pagos cruzados simultáneos `ana`→`maria` y `maria`→`ana` terminan ambos sin interbloqueo; con `MockitoSpyBean` sobre `MovementRepository` que falla al guardar el segundo movimiento, no cambia ningún saldo ni queda `Payment` ni `Movement` del pago. (CB-05, CB-08, DT-01, DT-03, DT-04, DT-05, DT-06 · RF-05, RF-07, RF-08, RF-09, RF-10, RN-01, RN-02, RN-03, RN-04, RN-05, RN-06, RN-07, CA-10, CA-15, CA-16 · depende de T-02, T-03)

### [x] T-06: Exponer el endpoint de pago y verificarlo de extremo a extremo

- **Qué hacer**: Crear `payment/mapper/PaymentMapper` y `payment/controller/PaymentController implements PaymentsApi` (`201` con `PaymentResponse`). Resultado: tests MockMvc con login real: `amount` `0`, `-5` y `10.999` → `400 VALIDATION_ERROR` sin pagos; `12.50` → `201` con `amount = 12.50`; concepto de 141 caracteres → `400`, de 140 → `201`; destinatario `noexiste` → `422 RECIPIENT_NOT_FOUND` y saldo sin cambios; `ana` a `ANA` → `422 SELF_PAYMENT`; saldo 100.00 y pago 100.01 → `422 INSUFFICIENT_BALANCE` y saldo 100.00; pago 100.00 desde 100.00 → `201` y `balanceAfter = 0.00`; `ana` (1000.00) paga 250.00 a `Maria` con "Cena" → `201`, saldos 750.00 / 750.00 y movimientos de ambos según CA-11 en `GET /api/accounts/me/movements`; concepto vacío o con espacios → `concept = null` en pago y movimientos; sin token → `401` sin pagos. (CB-06, CB-08 · RF-05, RF-06, RF-07, RF-08, RF-09, RF-10, CA-04, CA-05, CA-06, CA-07, CA-08, CA-09, CA-10, CA-11, CA-12, CA-17 · depende de T-01, T-05)

### [x] T-07: Verificar los pagos en PostgreSQL

- **Qué hacer**: Con `docker compose up -d db` y `mvn spring-boot:run`, desde Swagger UI con los usuarios de `data.sql`: consultar el saldo de `ana`, pagar a `maria` y comprobar en PostgreSQL que existe el `Payment`, el cargo y el abono con su `balanceAfter` y que los saldos de ambas cuentas coinciden con ese `balanceAfter`; probar destinatario inexistente, pago a uno mismo y saldo insuficiente. Resultado: todas las comprobaciones se cumplen y `mvn test` pasa. (CB-01, CB-05, CB-06, CB-07 · CA-02, CA-07, CA-08, CA-09, CA-11 · depende de T-04, T-06)
