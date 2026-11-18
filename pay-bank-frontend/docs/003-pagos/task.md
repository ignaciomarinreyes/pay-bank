# Tareas de implementación: Pagos

> Especificación: `003-pagos` · [spec.md](../../../docs/003-pagos/spec.md) · Plan: [plan.md](plan.md)
> Ámbito de la spec: Global · Proyecto de estas tareas: `pay-bank-frontend`

> Estados: `[ ]` pendiente · `[~]` en curso · `[x]` completada

## Orden de ejecución

```
(T-01 ∥ T-02) → T-03 → T-04 → T-05
```

- T-01 (servicio de cuenta) y T-02 (servicio de pagos) son independientes; solo requieren el contrato definido (backend:T-04 y backend:T-01).
- T-03 (pantalla "Nuevo pago") usa ambos; T-04 la conecta a rutas y navegación.
- T-05 es la verificación manual contra el backend y requiere backend:T-04 y backend:T-06.

## Tareas

### [x] T-01: Crear el servicio de cuenta en core

- **Qué hacer**: Crear `src/app/core/services/account.models.ts` (`AccountResponse`) y `src/app/core/services/account.service.ts` (`AccountService.getMyAccount()`) según CB-01. Resultado: test Vitest con `HttpTestingController` que comprueba `GET {apiBaseUrl}/api/accounts/me` y que devuelve la respuesta tal cual; `npm run build` compila. (CB-01, DT-01 · RF-02 · depende de backend:T-04)

### [x] T-02: Crear los modelos y el servicio de pagos

- **Qué hacer**: Crear `src/app/features/payments/payments.models.ts` y `src/app/features/payments/payments.service.ts` (`PaymentsService.createPayment`) según CB-02. Resultado: test Vitest con `HttpTestingController` que comprueba `POST {apiBaseUrl}/api/payments` con el cuerpo recibido y que devuelve la respuesta `201` tal cual. (CB-02 · RF-03 · depende de backend:T-01)

### [x] T-03: Crear la pantalla "Nuevo pago"

- **Qué hacer**: Crear `src/app/features/payments/new-payment-page/new-payment-page.{ts,html,scss}` (`NewPaymentPageComponent`) según CB-03, DT-02, DT-03, DT-04, DT-05 y DT-06. Resultado: tests Vitest con `HttpTestingController` y `Router.navigateByUrl` espiado que comprueban: campos destinatario, importe y concepto y botón "Pagar"; saldo `1015` → "Saldo disponible: 1.015,00 €" y, si su carga falla, sin línea de saldo y con formulario; destinatario o importe vacíos → sin petición y su mensaje; importes `0`, `-5`, `10,999`, `abc` → sin petición y mensaje de importe no válido; `12,50` y `12.50` → `amount: 12.5`; concepto de 141 caracteres → sin petición y mensaje, de 140 → se envía; concepto vacío o con espacios → `concept: null`; `Maria`, `250`, `Cena` → cuerpo correspondiente y, con `201`, navegación a `/movements`; `422` con `RECIPIENT_NOT_FOUND`, `SELF_PAYMENT`, `INSUFFICIENT_BALANCE` → su mensaje de RN-08; `500` y `400 VALIDATION_ERROR` → mensaje genérico; tras un error los tres campos conservan su valor; durante el envío "Pagar" está deshabilitado y un segundo envío no genera otra petición. (CB-03, DT-02, DT-03, DT-04, DT-05, DT-06 · RF-02, RF-03, RF-04, RF-05, RF-06, RF-11, RF-12, RF-13, RN-02, RN-04, RN-08, CA-02, CA-03, CA-04, CA-05, CA-06, CA-07, CA-08, CA-09, CA-11, CA-12, CA-13, CA-14, CA-18 · depende de T-01, T-02)

### [x] T-04: Añadir la ruta y la entrada de navegación de pagos

- **Qué hacer**: Crear `src/app/features/payments/payments.routes.ts` (ruta `new`), registrar la hija `payments` del shell en `src/app/app.routes.ts` y añadir "Nuevo pago" antes de "Movimientos" en `navItems` de `src/app/layout/sidebar/sidebar.ts`, actualizando `sidebar.spec.ts`. Resultado: tests Vitest que comprueban los enlaces "Nuevo pago" (`/payments/new`) y "Movimientos" (`/movements`) en ese orden y que, con sesión, `/payments/new` muestra el formulario (sin sesión lleva al login); `npm test` y `npm run build` pasan. (CB-04, CB-05, DT-07 · RF-01, CA-01 · depende de T-03)

### [x] T-05: Verificar los pagos contra el backend

- **Qué hacer**: Con PostgreSQL, el backend y `npm start`, iniciar sesión con `ana` (datos de `data.sql`) y comprobar en el navegador, sin cambios de código salvo correcciones de defectos: "Nuevo pago" muestra el saldo disponible; un pago a `maria` con importe escrito con coma lleva a "Movimientos" con el cargo el primero y el saldo actualizado; `maria` ve el abono; destinatario inexistente, pago a `ANA` e importe mayor que el saldo muestran su mensaje y conservan los datos. Resultado: todas las comprobaciones se cumplen. (CB-03, CB-04, CB-05 · CA-01, CA-02, CA-05, CA-07, CA-08, CA-09, CA-11, CA-13 · depende de T-04, backend:T-04, backend:T-06)
