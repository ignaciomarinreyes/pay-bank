# Plan de implementación: Pagos

> Especificación: `003-pagos` · [spec.md](../../../docs/003-pagos/spec.md)
> Ámbito de la spec: Global · Proyecto de este plan: `pay-bank-frontend`
> Otros planes de esta spec: `pay-bank-backend/docs/003-pagos/plan.md`

## Alcance en este proyecto

- **RF**: RF-01, RF-02 (mostrar el saldo), RF-03, RF-04, RF-05, RF-06 (validación en pantalla), RF-11, RF-12, RF-13.
- **RN**: RN-02 (formato de entrada del importe), RN-04 (longitud en pantalla), RN-08 (textos de los mensajes).
- **CA**: CA-01, CA-03, CA-13, CA-14, CA-18 y la parte visible de CA-02, CA-04, CA-05, CA-06, CA-07, CA-08, CA-09, CA-11, CA-12.

## 1. Resumen de la estrategia

1. Crear en `core` un servicio de cuenta que obtiene el saldo disponible (reutilizable por el futuro panel).
2. Crear la funcionalidad `features/payments` con el servicio de pagos y la pantalla "Nuevo pago" (formulario reactivo, validaciones en pantalla, envío, traducción de errores y navegación a "Movimientos").
3. Registrar la ruta `/payments/new` bajo el shell y añadir "Nuevo pago" a la navegación.
4. Cubrir los CA con tests unitarios (Vitest) y verificación manual contra el backend.

## 2. Contexto técnico

- Ya existen: `AuthService` e `authInterceptor` (token y renovación ante `401`), `authGuard` en la ruta del shell, rutas con `withComponentInputBinding()`, `environment.apiBaseUrl`, `LOCALE_ID = 'es'`, `AmountPipe` (`appAmount`) en `shared/pipes`, `PageTitleComponent`, `SidebarComponent` con `navItems` (ahora "Movimientos" → `/movements`), la funcionalidad `features/movements` (ruta hija `movements` del shell) y la pantalla de login con Reactive Forms y mensajes bajo cada campo.
- No hay servicio de cuenta en `core` ni funcionalidad `features/payments`.
- Constitución: signals-first, zoneless, OnPush, control de flujo `@if`/`@for`, plantillas y estilos en ficheros separados, llamadas HTTP solo desde servicios, funcionalidades aisladas con carga diferida; `core` contiene servicios singleton globales.
- Contrato de la API: ver "Contratos e integraciones" en `pay-bank-backend/docs/003-pagos/plan.md`.

## 3. Decisiones técnicas

- **DT-01**: `AccountService` (`core/services/account.service.ts`) con `getMyAccount()` y su modelo `AccountResponse`, en `core` porque el saldo lo usarán varias funcionalidades (pagos y el futuro panel) y las funcionalidades no pueden depender entre sí.
  - Alternativas consideradas: servicio dentro de `features/payments` (el panel tendría que duplicarlo).
  - Motivo: reutilización sin romper la regla de dependencias entre funcionalidades.
- **DT-02**: Formulario con **Reactive Forms** (`FormGroup` no anulable: `recipientAlias`, `amount` como texto y `concept`), igual que el login. Validadores: `recipientAlias` obligatorio; `amount` obligatorio y validador propio (patrón `^\d+([.,]\d{1,2})?$` y valor > 0); `concept` `Validators.maxLength(140)` sin atributo `maxlength` en el `input`, para poder mostrar el error de CA-06. Al pulsar "Pagar" con el formulario inválido se marcan todos los campos como tocados y no se envía nada.
  - Alternativas consideradas: Signal Forms; `input type="number"` (no admite la coma decimal de forma uniforme entre navegadores).
  - Motivo: coherencia con el login, API estable y control exacto del formato de RN-02.
- **DT-03**: Conversión del importe al enviar: se sustituye la coma por punto y se convierte a número (`"12,50"` → `12.5`). El concepto se envía tal cual si tiene contenido no blanco y como `null` en otro caso (el servidor también recorta).
  - Alternativas consideradas: enviar el texto y que el servidor interprete la coma.
  - Motivo: el contrato espera un número; la regla de entrada (RN-02) es de presentación.
- **DT-04**: Traducción de errores del servidor a los textos de RN-08 según `code` del `ProblemDetail`: `RECIPIENT_NOT_FOUND`, `SELF_PAYMENT`, `INSUFFICIENT_BALANCE` a su mensaje; cualquier otro caso (`VALIDATION_ERROR`, `ACCOUNT_NOT_FOUND`, `5xx`, red) al mensaje genérico. El mensaje se muestra en un aviso (`role="alert"`) sobre el formulario y los datos introducidos no se borran (RF-12).
  - Alternativas consideradas: mostrar los errores del servidor bajo cada campo.
  - Motivo: los errores de servidor no corresponden a un campo editable concreto salvo el destinatario; un único aviso es más claro y simple.
- **DT-05**: Estado de la pantalla con signals: `submitting` deshabilita "Pagar" mientras hay un envío en curso y se ignoran nuevos envíos (RF-13); `serverError` guarda el mensaje del último fallo y se limpia al reenviar. Tras `201` se navega a `/movements` (RF-11).
  - Alternativas consideradas: navegar a `/movements?page=1` con un aviso de éxito.
  - Motivo: la spec deja fuera el aviso de éxito; la primera página es la de por defecto.
- **DT-06**: El saldo se carga al abrir la pantalla con `toSignal(accountService.getMyAccount())` y se muestra como "Saldo disponible: X" con `AmountPipe` (sin signo). Si la carga falla, la línea del saldo no se muestra y el formulario sigue disponible.
  - Alternativas consideradas: bloquear el formulario sin saldo.
  - Motivo: el servidor valida el saldo en cualquier caso (RF-09); no conocerlo no impide pagar.
- **DT-07**: Ruta `/payments/new` (hija `payments` del shell, con `payments.routes.ts` y la ruta `new`), dejando `/payments/...` para los futuros listados de pagos realizados y recibidos. Entrada de navegación "Nuevo pago" antes de "Movimientos".
  - Alternativas consideradas: ruta `/pay`.
  - Motivo: agrupa todo lo de pagos bajo una misma funcionalidad, como prevé la constitución.

## 4. Componentes afectados

| Componente | Proyecto | Tipo de cambio (nuevo / modificado / eliminado) | Relacionado con |
|---|---|---|---|
| `core/services/account.models.ts`, `core/services/account.service.ts` | pay-bank-frontend | nuevo | RF-02, DT-01 |
| `features/payments/payments.models.ts` | pay-bank-frontend | nuevo | backend:CB-01 |
| `features/payments/payments.service.ts` | pay-bank-frontend | nuevo | RF-03 |
| `features/payments/new-payment-page/new-payment-page.{ts,html,scss}` | pay-bank-frontend | nuevo | RF-02–RF-06, RF-11–RF-13, RN-02, RN-04, RN-08, DT-02–DT-06 |
| `features/payments/payments.routes.ts` | pay-bank-frontend | nuevo | RF-01, DT-07 |
| `app.routes.ts` | pay-bank-frontend | modificado | RF-01 |
| `layout/sidebar/sidebar.ts` | pay-bank-frontend | modificado | RF-01 |
| Tests `*.spec.ts` | pay-bank-frontend | nuevo / modificado | CA del alcance |

## 5. Cambios necesarios

- **CB-01**: Crear `core/services/account.models.ts` (`AccountResponse`) y `core/services/account.service.ts` (`AccountService.getMyAccount()`: `GET {apiBaseUrl}/api/accounts/me`) — Cubre: RF-02
- **CB-02**: Crear `features/payments/payments.models.ts` (`CreatePaymentRequest`, `PaymentResponse`, tipo de los códigos de error) y `features/payments/payments.service.ts` (`PaymentsService.createPayment(request)`: `POST {apiBaseUrl}/api/payments`), reflejo exacto de backend:CB-01 — Cubre: RF-03, RF-10 (consumo)
- **CB-03**: Crear `NewPaymentPageComponent` (`app-new-payment-page`): título "Nuevo pago", saldo según DT-06, formulario según DT-02 con etiquetas "Destinatario", "Importe (€)" y "Concepto (opcional)", mensajes de validación de RN-08 bajo cada campo, conversión de DT-03, envío y estado de DT-05, traducción de errores de DT-04 y navegación a `/movements` — Cubre: RF-02, RF-03, RF-04, RF-05, RF-06, RF-11, RF-12, RF-13, RN-02, RN-04, RN-08
- **CB-04**: Crear `features/payments/payments.routes.ts` (ruta `new`) y registrar `{ path: 'payments', loadChildren: … }` como hija del shell en `app.routes.ts` — Cubre: RF-01
- **CB-05**: Añadir `{ label: 'Nuevo pago', path: '/payments/new' }` a `navItems` del `SidebarComponent` (antes de "Movimientos") y actualizar su test — Cubre: RF-01

### Modelo de datos y migraciones

### Contratos e integraciones

- Consume `GET /api/accounts/me` y `POST /api/payments` tal como se definen en `pay-bank-backend/docs/003-pagos/plan.md` ("Contratos e integraciones") y en `pay-bank-backend/docs/api/openapi.yaml`. No se duplican aquí.
- El token lo añade el `authInterceptor` existente; la petición nunca incluye la cuenta ni el usuario de origen.

### Configuración y seguridad

## 6. Estrategia de verificación

Tests unitarios con Vitest (`ng test`) y `HttpTestingController`:

| Criterio de aceptación | Cómo se verifica (tipo de prueba / nivel) | Cambios implicados |
|---|---|---|
| CA-01 | Unitario del sidebar: enlace "Nuevo pago" a `/payments/new`. Unitario de rutas: con sesión, `/payments/new` muestra el formulario con los tres campos y "Pagar" | CB-03, CB-04, CB-05 |
| CA-02 (visible) | Unitario de la página: respuesta `{ balance: 1015 }` → "Saldo disponible: 1.015,00 €"; error al cargar → sin línea de saldo y formulario visible | CB-01, CB-03 |
| CA-03 | Unitario: destinatario o importe vacíos → sin petición y mensaje del campo vacío | CB-03 |
| CA-04 (visible) | Unitario: importes `0`, `-5`, `10,999`, `abc` → sin petición y mensaje de importe no válido | CB-03 |
| CA-05 (visible) | Unitario: `12,50` y `12.50` → cuerpo con `amount: 12.5` | CB-03 |
| CA-06 (visible) | Unitario: concepto de 141 caracteres → sin petición y mensaje; de 140 → se envía | CB-03 |
| CA-07, CA-08, CA-09 (visible) | Unitario: `422` con `code` `RECIPIENT_NOT_FOUND`, `SELF_PAYMENT`, `INSUFFICIENT_BALANCE` → su mensaje de RN-08 | CB-03 |
| CA-11 (visible) | Unitario: `201` → navegación a `/movements`; cuerpo con `recipientAlias: 'Maria'`, `amount: 250`, `concept: 'Cena'` | CB-02, CB-03 |
| CA-12 (visible) | Unitario: concepto vacío o con espacios → `concept: null` en el cuerpo | CB-03 |
| CA-13 | Unitario: tras `INSUFFICIENT_BALANCE` los tres campos conservan su valor | CB-03 |
| CA-14 | Unitario: durante el envío "Pagar" está deshabilitado y un segundo envío no genera otra petición | CB-03 |
| CA-18 | Unitario: `500` y `400 VALIDATION_ERROR` → mensaje genérico y datos conservados | CB-03 |
| CA-10, CA-15, CA-16, CA-17 | Se verifican en el backend | `pay-bank-backend` |

Además: verificación manual en el navegador con los datos de `data.sql` (pago correcto con coma decimal y vuelta a Movimientos con el cargo el primero, saldo insuficiente, destinatario inexistente, pago a uno mismo).

## 7. Riesgos y mitigaciones

- **Precisión del importe en JavaScript**: un importe con 2 decimales convertido a `number` y serializado a JSON conserva su representación decimal (`12.5`, `1234.56`), y el backend lo deserializa a `BigDecimal`. Mitigación: el test de CA-05 comprueba el cuerpo enviado.
- **Conflicto de nombres de ruta** con los futuros listados de pagos: la ruta `new` es explícita y no colisiona con rutas como `sent` o `received`.

## 8. Elementos de la spec no cubiertos

- RF-07, RF-08, RF-09, RF-10 y la validación en servidor de RF-05 y RF-06 → `pay-bank-backend`.
- RN-01, RN-03, RN-05, RN-06, RN-07 y la regla de RN-04 en servidor → `pay-bank-backend`.
- CA-10, CA-15, CA-16, CA-17 y la parte de datos de CA-02, CA-04–CA-09, CA-11, CA-12 → `pay-bank-backend`.
