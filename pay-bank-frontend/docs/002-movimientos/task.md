# Tareas de implementación: Movimientos

> Especificación: `002-movimientos` · [spec.md](../../../docs/002-movimientos/spec.md) · Plan: [plan.md](plan.md)
> Ámbito de la spec: Global · Proyecto de estas tareas: `pay-bank-frontend`

> Estados: `[ ]` pendiente · `[~]` en curso · `[x]` completada

## Orden de ejecución

```
(T-01 ∥ T-02 ∥ T-03) → T-04 → T-05 → T-06
```

- T-01 (locale y pipe de importes), T-02 (paginador) y T-03 (modelos y servicio) son independientes; T-03 solo requiere el contrato definido (backend:T-04).
- T-04 (página de movimientos) usa las tres anteriores; T-05 la conecta a rutas y navegación.
- T-06 es la verificación manual contra el backend y requiere backend:T-06 y backend:T-07.

## Tareas

### [x] T-01: Configurar el locale español y crear el pipe de importes

- **Qué hacer**: En `src/app/app.config.ts`, registrar el locale `es` y proveer `LOCALE_ID` según CB-01; crear `src/app/shared/pipes/amount.pipe.ts` (`AmountPipe`, `appAmount`) según DT-02. Resultado: tests Vitest del pipe: con signo, `-1234.56` → "-1.234,56 €" y `50` → "+50,00 €"; sin signo, `150` → "150,00 €", `120` → "120,00 €" y `1234567.8` → "1.234.567,80 €". (CB-01, CB-02, DT-01, DT-02 · RN-01, RN-03, CA-07, CA-08, CA-09)

### [x] T-02: Crear el componente de paginación compartido

- **Qué hacer**: Crear `src/app/shared/components/paginator/paginator.{ts,html,scss}` (`PaginatorComponent`, `app-paginator`) según DT-05. Resultado: tests Vitest que comprueban: con `page = 1` y `totalPages = 3` se muestra "Página 1 de 3" y "Anterior" está deshabilitado; con `page = 3` de 3 "Siguiente" está deshabilitado; con `page = 2`, pulsar "Anterior" emite `pageChange(1)` y pulsar "Siguiente" emite `pageChange(3)`. (CB-03, DT-05 · RF-06, RF-07, CA-05, CA-06)

### [x] T-03: Crear los modelos y el servicio de movimientos

- **Qué hacer**: Crear `src/app/features/movements/movements.models.ts` y `src/app/features/movements/movements.service.ts` (`MovementsService.getMyMovements`) según CB-04 y CB-05. Resultado: tests Vitest con `HttpTestingController` que comprueban que `getMyMovements(0)` hace `GET {apiBaseUrl}/api/accounts/me/movements?page=0&size=20` y `getMyMovements(2)` envía `page=2`, devolviendo la respuesta tal cual; `npm run build` compila. (CB-04, CB-05 · RF-04, RF-05 · depende de backend:T-04)

### [x] T-04: Crear la página de movimientos

- **Qué hacer**: Crear `src/app/features/movements/movements-page/movements-page.{ts,html,scss}` (`MovementsPageComponent`) según CB-06, DT-03, DT-04, DT-06 y DT-07. Resultado: tests Vitest con `HttpTestingController` que comprueban: sin `page` se pide `page=0` y con `page = '3'` se pide `page=2`; `page = 'abc'` y `page = '0'` piden `page=0`; las filas se pintan en el orden recibido; con 20 elementos y `totalPages = 3` hay 20 filas y "Página 1 de 3"; `CHARGE` → "Cargo" con "-1.234,56 €" y `CREDIT` → "Abono" con "+50,00 €", contraparte y concepto visibles; saldo "150,00 €"; un `createdAt` del 18/11/2026 14:05 local se muestra "18/11/2026 14:05"; `concept: null` → "—"; `totalElements = 0` → "No hay movimientos" sin paginador; respuesta `500` → mensaje de RF-09 sin tabla ni paginador; `pageChange(2)` navega a `?page=2`; si la página pedida supera `totalPages` se navega a la última con `replaceUrl`. (CB-06, DT-03, DT-04, DT-06, DT-07 · RF-04, RF-05, RF-06, RF-07, RF-08, RF-09, RN-04, RN-05, CA-03, CA-04, CA-05, CA-06, CA-07, CA-08, CA-09, CA-10, CA-11, CA-12, CA-13 · depende de T-01, T-02, T-03)

### [x] T-05: Añadir la ruta y la entrada de navegación de movimientos

- **Qué hacer**: Crear `src/app/features/movements/movements.routes.ts`, registrarlo como hija `movements` de la ruta del shell en `src/app/app.routes.ts` y añadir la entrada a `navItems` de `src/app/layout/sidebar/sidebar.ts` según CB-07 y CB-08. Resultado: tests Vitest que comprueban que el sidebar muestra el enlace "Movimientos" a `/movements` y que, con sesión, navegar a `/movements` muestra la página de movimientos (y sin sesión lleva al login, por el guard existente); `npm test` y `npm run build` pasan. (CB-07, CB-08 · RF-01, CA-01 · depende de T-04)

### [x] T-06: Verificar los movimientos contra el backend

- **Qué hacer**: Con PostgreSQL, el backend (`mvn spring-boot:run`) y `npm start`, iniciar sesión con `ana` (datos de `data.sql`) y comprobar en el navegador, sin cambios de código salvo correcciones de defectos: el enlace "Movimientos" abre el listado; se ven 20 movimientos del más reciente al más antiguo con fecha, tipo, contraparte, concepto ("—" si falta), importe con signo y saldo en formato español; "Siguiente" y "Anterior" cambian de página y actualizan `?page=N`; recargar en la página 2 la mantiene y el botón Atrás del navegador vuelve a la anterior; el pago `maria` → `luis` no aparece para `ana` y sí para `luis`. Resultado: todas las comprobaciones se cumplen. (CB-06, CB-07, CB-08 · CA-01, CA-02, CA-03, CA-04, CA-05, CA-06, CA-07, CA-11 · depende de T-05, backend:T-06, backend:T-07)
