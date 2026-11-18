# Tareas de implementación: Inicio de sesión

> Especificación: `001-inicio-sesion` · [spec.md](../../../docs/001-inicio-sesion/spec.md) · Plan: [plan.md](plan.md)
> Ámbito de la spec: Global · Proyecto de estas tareas: `pay-bank-frontend`

> Estados: `[ ]` pendiente · `[~]` en curso · `[x]` completada

## Orden de ejecución

```
T-01 → T-02 → (T-03 ∥ T-04 ∥ T-05 ∥ T-07) → T-06 → T-08
```

- T-01 a T-07 se desarrollan y prueban contra `HttpTestingController`, sin necesidad del backend; T-01 solo requiere que el contrato esté definido (backend:T-01).
- T-06 integra los guards (T-04) y la pantalla de login (T-05) en las rutas.
- T-08 es la verificación manual contra el backend y requiere backend:T-09, backend:T-10 y backend:T-11.

## Tareas

### [ ] T-01: Crear los modelos del contrato de autenticación

- **Qué hacer**: Crear `src/app/core/services/auth.models.ts` con `LoginRequest` y `AuthResponse`, reflejo exacto de los esquemas de `pay-bank-backend/docs/api/openapi.yaml`. Resultado: `npm run build` compila sin errores y los nombres y tipos de los campos coinciden con el contrato. (CB-01 · RF-13 · depende de backend:T-01)

### [ ] T-02: Implementar el servicio de sesión

- **Qué hacer**: Crear `src/app/core/services/auth.service.ts` (`AuthService`, `providedIn: 'root'`) con el estado en signals y los métodos `login`, `refresh`, `ensureSession`, `logout`, `accessTokenValue` y `clearSession` según CB-02, usando `environment.apiBaseUrl` y `withCredentials: true` en `/api/auth/*`. Resultado: tests Vitest con `HttpTestingController` que comprueban: `login` guarda token y alias (`isAuthenticated()` y `currentUser()` cambian); dos llamadas simultáneas a `refresh()` generan una sola petición; `ensureSession()` devuelve `true` sin petición si hay token y, sin token, hace una renovación y devuelve `true`/`false` según la respuesta; un fallo de `refresh()` limpia la sesión y navega a `/login` con `returnUrl` de la URL actual; `logout()` llama a `/api/auth/logout`, limpia la sesión y navega a `/login` tanto si responde `204` como si falla. (CB-02, DT-01, DT-02, DT-03, DT-04, DT-10 · RF-08, RF-09, RF-11, RF-12, RF-13, RN-04, CA-10, CA-13, CA-14 · depende de T-01)

### [ ] T-03: Añadir el interceptor de autenticación

- **Qué hacer**: Crear `src/app/core/interceptors/auth.interceptor.ts` (`authInterceptor`) según CB-03 y registrarlo en `withInterceptors([authInterceptor])` de `src/app/app.config.ts`. Resultado: tests Vitest que comprueban: añade `Authorization: Bearer <token>` a peticiones a `apiBaseUrl` y no a `/api/auth/*` ni a otros orígenes; ante un `401` hace una renovación y reintenta una sola vez con el nuevo token; dos peticiones concurrentes con `401` producen una sola petición a `/api/auth/refresh`; si el reintento vuelve a dar `401` el error se propaga sin nueva renovación; si la renovación falla, se navega a `/login` con `returnUrl`. (CB-03, DT-03, DT-04 · RF-09, RF-12, CA-11, CA-13 · depende de T-02)

### [ ] T-04: Crear los guards de autenticación

- **Qué hacer**: Crear `src/app/core/guards/auth.guard.ts` (`authGuard`) y `src/app/core/guards/guest.guard.ts` (`guestGuard`) según CB-04 y CB-05. Resultado: tests Vitest con `AuthService` simulado que comprueban: `authGuard` permite el acceso si `ensureSession()` es `true` y, si es `false`, devuelve `UrlTree` `/login?returnUrl=<url solicitada>`; `guestGuard` devuelve `UrlTree` `/` si hay sesión y permite el acceso si no la hay. (CB-04, CB-05, DT-02, DT-05 · RF-05, RF-07, RF-08, RF-14, RN-06 · depende de T-02)

### [ ] T-05: Crear la pantalla de inicio de sesión

- **Qué hacer**: Crear la funcionalidad `src/app/features/login/` con `login.routes.ts` y `login-page/login-page.{ts,html,scss}` (`LoginPageComponent`, `app-login-page`) según CB-06, DT-06, DT-07 y DT-08. Resultado: tests Vitest del componente que comprueban: se muestran el campo alias, el campo contraseña con `type="password"` y el botón "Iniciar sesión"; enviar con alias vacío, contraseña vacía o ambos no hace petición y muestra el mensaje de obligatorio del campo vacío; un `401` muestra "Alias o contraseña incorrectos" y la sesión sigue cerrada; un `500` muestra el mensaje genérico; el botón está deshabilitado mientras la petición está en curso; tras el éxito navega a `returnUrl` si es interna y a `/` si falta o es externa (`//evil.com`, `https://…`) o es `/login`. (CB-06, DT-06, DT-07, DT-08 · RF-01, RF-02, RF-04, RF-07, RN-01, RN-05, CA-01, CA-02, CA-05, CA-06 · depende de T-02)

### [ ] T-06: Registrar las rutas protegidas y la ruta de login

- **Qué hacer**: Modificar `src/app/app.routes.ts` según CB-07 (ruta `login` con `guestGuard` y carga diferida de `features/login`; `authGuard` como `canActivate` y `canActivateChild` del shell). Resultado: tests Vitest con `RouterTestingHarness`, las rutas reales y rutas hijas de prueba en `/` y `/movements`, que comprueban: sin sesión y con renovación `401`, navegar a `/movements` lleva a `/login?returnUrl=%2Fmovements` y tras un login correcto se muestra `/movements`; login correcto entrando directamente en `/login` lleva a `/`; sin token en memoria pero con renovación `200`, navegar a `/` muestra el shell sin pasar por el login; con sesión, navegar a `/login` lleva a `/`; tras `logout()`, navegar a `/` con renovación `401` lleva a `/login`. (CB-07, DT-05 · RF-05, RF-07, RF-08, RF-14, CA-03, CA-08, CA-10, CA-14, CA-16 · depende de T-04, T-05)

### [ ] T-07: Mostrar el alias y la acción de cerrar sesión en el header

- **Qué hacer**: Modificar `src/app/layout/header/header.{ts,html,scss}` según CB-08 y DT-09. Resultado: tests Vitest del componente con `AuthService` simulado que comprueban que con `currentUser = 'ana'` se muestra "ana" y que pulsar "Cerrar sesión" invoca `logout()` una vez. (CB-08, DT-09 · RF-11, RF-13, CA-14, CA-15 · depende de T-02)

### [ ] T-08: Verificar el inicio de sesión contra el backend

- **Qué hacer**: Con el backend en ejecución (`docker compose up -d db` y `mvn spring-boot:run`) y `npm start`, comprobar en el navegador con un usuario de `data.sql`, sin cambios de código salvo correcciones de defectos encontrados: login correcto lleva a `/` y el header muestra el alias; credenciales incorrectas muestran "Alias o contraseña incorrectos"; recargar la página y cerrar/reabrir el navegador mantienen la sesión; tras más de 15 min sin actividad, una navegación o petición sigue funcionando sin pedir credenciales y en la pestaña de red se ve una única llamada a `/api/auth/refresh`; "Cerrar sesión" lleva a `/login` y recargar no recupera la sesión; la cookie `refresh_token` aparece como `HttpOnly` y no es accesible desde `document.cookie`. Resultado: `npm test` y `npm run build` pasan y todas las comprobaciones anteriores se cumplen. (CB-02, CB-03, CB-06, CB-07, CB-08 · CA-03, CA-05, CA-10, CA-11, CA-14, CA-15 · depende de T-03, T-06, T-07, backend:T-09, backend:T-10, backend:T-11)
