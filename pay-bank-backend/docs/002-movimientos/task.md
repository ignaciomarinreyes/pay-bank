# Tareas de implementación: Movimientos

> Especificación: `002-movimientos` · [spec.md](../../../docs/002-movimientos/spec.md) · Plan: [plan.md](plan.md)
> Ámbito de la spec: Global · Proyecto de estas tareas: `pay-bank-backend`

> Estados: `[ ]` pendiente · `[~]` en curso · `[x]` completada

## Orden de ejecución

```
(T-01 ∥ T-02 ∥ T-03 ∥ T-04 ∥ T-08) → (T-05 ∥ T-07) → T-06
```

- T-01 (modelo), T-02 (`PageQuery`), T-03 (`CurrentUserProvider`), T-04 (contrato OpenAPI) y T-08 (constitución) no dependen de nada.
- T-05 (consulta paginada) y T-07 (`data.sql`) necesitan el modelo de T-01.
- T-06 integra todo en el endpoint y verifica los CA de extremo a extremo.

## Tareas

### [x] T-01: Crear las entidades y repositorios de cuenta, pago y movimiento

- **Qué hacer**: Crear `account/model/Account`, `account/model/MovementType`, `account/model/Movement` y `payment/model/Payment` según "Modelo de datos y migraciones" del plan, y `account/repository/AccountRepository` y `payment/repository/PaymentRepository`. Resultado: test `@DataJpaTest` que guarda un usuario con su cuenta, un pago entre dos cuentas y sus dos movimientos y los recupera con sus importes con escala 2, `type` persistido como texto y `balanceAfter`; y que comprueba que una segunda cuenta para el mismo usuario viola la restricción única. `mvn test` pasa. (CB-01, CB-02, CB-03, DT-01, DT-02, DT-03 · RN-01, RN-07)

### [x] T-02: Añadir el objeto de paginación compartido

- **Qué hacer**: Crear `common/web/PageQuery` (valores por defecto, validación de rangos y conversión a `Pageable` sin orden) y el tratamiento de los parámetros de paginación inválidos como `400` `ProblemDetail` en `exception/GlobalExceptionHandler`. Resultado: tests unitarios que comprueban los valores por defecto `page = 0` / `size = 20`, la conversión a `Pageable` y el rechazo de `page = -1`, `size = 0` y `size = 101`. (CB-05, DT-06 · RF-04)

### [x] T-03: Añadir el proveedor del usuario actual

- **Qué hacer**: Crear `security/CurrentUserProvider` que devuelve el `AuthenticatedUser` del `SecurityContext`. Resultado: tests unitarios que, con un contexto autenticado por `JwtAuthFilter` (principal `AuthenticatedUser`), devuelven su `id` y `alias`, y que sin autenticación lanzan excepción. (CB-06, DT-07 · RF-02)

### [x] T-04: Definir el contrato de movimientos en OpenAPI

- **Qué hacer**: En `docs/api/openapi.yaml`, añadir bajo el tag `Accounts` la operación `getMyMovements` (`GET /api/accounts/me/movements` con `page` y `size`) y los esquemas `MovementResponse` y `MovementPageResponse` tal como se definen en "Contratos e integraciones" del plan. Resultado: `mvn generate-sources` genera `com.paybank.generated.api.AccountsApi` y los modelos `MovementResponse` y `MovementPageResponse` (importes como `BigDecimal`, `createdAt` como `OffsetDateTime`); Swagger UI muestra la operación. (CB-07, DT-05, DT-06 · RF-04, RF-05, RF-07)

### [x] T-05: Implementar la consulta paginada de movimientos

- **Qué hacer**: Crear `account/repository/MovementRow` y `MovementRepository.findPageByUserId` con la consulta y `countQuery` de DT-04, y un helper de test en `src/test/java/com/paybank/support` que crea usuarios, cuentas y pagos con sus dos movimientos (`createdAt` y `balanceAfter` controlados). Resultado: tests `@DataJpaTest` que comprueban: solo se devuelven movimientos del usuario indicado; orden por `createdAt` descendente y, a igualdad, por `id` descendente; contraparte = destino en `CHARGE` y origen en `CREDIT`; concepto `null` cuando el pago no lo tiene; con 45 movimientos, la página 0 tiene 20 elementos y `totalElements = 45`, y la página 2 tiene 5. (CB-04, CB-13, DT-04 · RF-02, RF-03, RF-04, RN-02, RN-06, CA-02, CA-03 · depende de T-01)

### [x] T-06: Exponer el endpoint de movimientos y verificarlo de extremo a extremo

- **Qué hacer**: Crear `account/mapper/MovementMapper`, `account/service/AccountService.getMyMovements` y `account/controller/AccountController implements AccountsApi` según CB-08, CB-09 y CB-10. Resultado: tests de integración MockMvc (con el helper de T-05 y tokens de login) que comprueban: sin token → `401` `ProblemDetail`; `ana` no ve el pago `maria` → `luis`; 45 movimientos → página 0 con 20 elementos, `totalPages = 3`, y `page = 2` con los 5 más antiguos; pago `ana` → `maria` de 1234.56 "Alquiler" → `type = CHARGE`, `amount = -1234.56`, `counterpartyAlias = maria`, `concept = Alquiler`; pago `maria` → `ana` de 50 → `type = CREDIT`, `amount = 50.00`; `balanceAfter` 150.00 y 120.00 devueltos tal cual; pago sin concepto → `concept = null`; usuario sin movimientos y usuario sin cuenta → `content = []`, `totalElements = 0`; `size = 101` → `400`. (CB-08, CB-09, CB-10, CB-13, DT-05, DT-07, DT-08 · RF-02, RF-04, RF-05, RF-07, RN-01, RN-07, CA-02, CA-04, CA-05, CA-07, CA-08, CA-09, CA-11, CA-12, CA-14 · depende de T-02, T-03, T-04, T-05)

### [x] T-07: Ampliar los datos de ejemplo

- **Qué hacer**: Ampliar `src/main/resources/data.sql` según DT-09 y CB-11 (usuario `luis`, cuentas con saldo inicial, pagos de ejemplo con y sin concepto, movimientos con `balanceAfter` y saldos finales coherentes), con sentencias idempotentes. Resultado: `mvn test` pasa (sin ejecutar `data.sql`); con `docker compose up -d db` y `mvn spring-boot:run`, `ana` tiene más de 20 movimientos, existe el pago `maria` → `luis`, el saldo de cada cuenta coincide con el `balanceAfter` de su último movimiento, y volver a ejecutar el script no crea filas nuevas. (CB-11, DT-09 · depende de T-01)

### [x] T-08: Matizar el saldo de la cuenta en la constitución

- **Qué hacer**: En `docs/constitution.md` (apartado "Modelo de dominio de alto nivel"), sustituir la regla del saldo por la de CB-12. Resultado: la constitución indica que el saldo de una `Account` es su saldo inicial más la suma de sus movimientos y no queda ninguna otra frase que lo contradiga. (CB-12, DT-03)
