# Plan de implementación: Inicio de sesión

> Especificación: `001-inicio-sesion` · [spec.md](../../../docs/001-inicio-sesion/spec.md)
> Ámbito de la spec: Global · Proyecto de este plan: `pay-bank-frontend`
> Otros planes de esta spec: `pay-bank-backend/docs/001-inicio-sesion/plan.md`

## Alcance en este proyecto

- **RF**: RF-01, RF-02, RF-04 (mensaje visible), RF-05, RF-07, RF-08, RF-09, RF-11 (acción de cerrar sesión, eliminar la sesión en el navegador y volver al login), RF-12, RF-13, RF-14.
- **RN**: RN-01 y RN-04 se respetan enviando lo introducido sin transformarlo y usando siempre la última credencial recibida; RN-05 (se muestra un único mensaje para cualquier rechazo de credenciales); RN-06 (las rutas solo comprueban si hay sesión).
- **CA**: CA-01, CA-02, CA-03, CA-05 y CA-06 (parte visible), CA-08, CA-10, CA-11 (parte de cliente), CA-13 (vuelta al login), CA-14 (parte de cliente), CA-15, CA-16.

## 1. Resumen de la estrategia

1. Crear en `core` el servicio de sesión `AuthService` (estado con signals, login, renovación compartida, logout) y los modelos del contrato de autenticación.
2. Crear el interceptor HTTP que añade la credencial de acceso y, ante un `401`, renueva la sesión una vez y reintenta la petición.
3. Crear los guards `authGuard` (rutas de la aplicación) y `guestGuard` (pantalla de login), que intentan recuperar la sesión mediante la renovación antes de decidir.
4. Crear la funcionalidad `features/login` (pantalla y formulario) con carga diferida.
5. Mostrar el alias y la acción de cerrar sesión en el `header`.
6. Registrar rutas, interceptor y guards; cubrir los CA con tests unitarios (Vitest) y verificación manual contra el backend.

## 2. Contexto técnico

- Angular 22 zoneless, componentes standalone, OnPush por defecto, Vitest (`ng test`), SCSS. Plantillas y estilos en ficheros separados; código en inglés, textos de la interfaz en español.
- Ya existen: `app.config.ts` con `provideHttpClient(withFetch(), withInterceptors([]))` y `provideRouter(routes, withComponentInputBinding())`; `app.routes.ts` con la ruta `''` que carga `ShellComponent` (con `children: []` y comentario de que el guard de autenticación protegerá esas rutas) y `'**' → ''`; `layout/` con `shell`, `header` (solo la marca "Pay Bank"), `sidebar` (`navItems` vacío) y `footer`; `environments/environment*.ts` con `apiBaseUrl: 'http://localhost:8080'`; carpetas vacías `core/guards`, `core/interceptors`, `core/services`, `features/`.
- No existe aún ninguna funcionalidad (ni panel ni movimientos): el panel será la ruta hija por defecto del shell (`/`) cuando se implemente.
- No se usa `@angular/forms` todavía, pero ya está en `package.json`.
- El contrato de la API lo define el backend: ver "Contratos e integraciones" en `pay-bank-backend/docs/001-inicio-sesion/plan.md`.

## 3. Decisiones técnicas

- **DT-01**: La credencial de acceso se guarda **solo en memoria** (signal privado de `AuthService`); la de renovación la gestiona el navegador como cookie `HttpOnly` (backend:DT-03) y el frontend nunca la lee. Las peticiones a `/api/auth/*` se envían con `withCredentials: true`.
  - Alternativas consideradas: `localStorage`/`sessionStorage` para la credencial de acceso.
  - Motivo: decisión del usuario; minimiza la exposición ante XSS. Tras recargar la página se recupera la sesión con una renovación (RF-08).
- **DT-02**: Recuperación de sesión perezosa en los guards: `AuthService.ensureSession()` devuelve `true` si ya hay credencial de acceso en memoria y, si no, intenta una única renovación (`POST /api/auth/refresh`). Así, al recargar o reabrir el navegador, el guard restaura la sesión antes de decidir (RF-08, CA-10).
  - Alternativas consideradas: `provideAppInitializer` que renueva siempre al arrancar (bloquea el arranque y hace una petición también en la pantalla de login aunque no haya cookie).
  - Motivo: solo se renueva cuando hace falta y el guard tiene la respuesta antes de navegar.
- **DT-03**: Renovación reactiva y compartida: el interceptor, ante un `401` de una petición que no sea de `/api/auth/*`, llama a `AuthService.refresh()` y reintenta la petición original **una sola vez** con la nueva credencial. `refresh()` reutiliza la renovación en curso (un único observable compartido con `shareReplay` que se limpia al terminar), de modo que varias peticiones concurrentes provocan una sola renovación (backend:DT-04).
  - Alternativas consideradas: renovación proactiva con temporizador antes de los 15 minutos (`expiresIn`), que mantiene viva la sesión aunque el usuario no haga nada y añade gestión de temporizadores.
  - Motivo: más simple y suficiente para RF-09/CA-11; la sesión solo se prolonga cuando el usuario actúa.
- **DT-04**: Si la renovación falla (`401` u otro error), `AuthService` limpia la sesión local y navega a `/login` con `returnUrl` = URL actual (RF-12, CA-13).
  - Alternativas consideradas: navegar a `/login` sin `returnUrl`.
  - Motivo: coherente con RF-07; el usuario vuelve a donde estaba tras iniciar sesión de nuevo.
- **DT-05**: Guards funcionales: `authGuard` como `canActivate` y `canActivateChild` de la ruta del shell (protege toda la aplicación, RF-05); `guestGuard` como `canActivate` de `/login` (RF-14). Si no hay sesión, `authGuard` devuelve `UrlTree` `/login?returnUrl=<url solicitada>`; si hay sesión, `guestGuard` devuelve `UrlTree` `/`.
  - Alternativas consideradas: `canMatch` (evita cargar el chunk del shell, pero `'**' → ''` y la redirección resultan menos directas).
  - Motivo: patrón estándar con guards funcionales; RN-06 (no hay roles).
- **DT-06**: Destino tras el login (RF-07): se usa `returnUrl` solo si es una ruta interna (empieza por `/`, no por `//`, y no es `/login`); en cualquier otro caso, `/` (ruta del shell donde vivirá el panel como hija por defecto).
  - Alternativas consideradas: aceptar cualquier `returnUrl`.
  - Motivo: evita redirecciones abiertas a sitios externos.
- **DT-07**: Formulario con **Reactive Forms** (`FormGroup` no anulable con `alias` y `password`, `Validators.required`). Al enviar con campos vacíos se marcan todos como tocados, no se llama a la API y se muestra bajo cada campo vacío "El alias es obligatorio" / "La contraseña es obligatoria" (RF-02). El estado de la pantalla (`submitting`, `errorMessage`) usa signals. El campo contraseña es `type="password"` (RF-01).
  - Alternativas consideradas: Signal Forms de Angular; template-driven forms.
  - Motivo: API estable y tipada; los signals se usan para el estado de la vista según la constitución.
- **DT-08**: Mensajes de error del login: `401` → "Alias o contraseña incorrectos" (RF-04, RN-05); cualquier otro error (red, `5xx`) → "No se ha podido iniciar sesión. Inténtalo de nuevo más tarde.". El botón se deshabilita mientras la petición está en curso para evitar envíos duplicados. Lo introducido se envía sin transformar (la normalización del alias la hace el backend, RN-01).
  - Alternativas consideradas: mostrar el `detail` del `ProblemDetail` del backend (en inglés).
  - Motivo: el texto visible está definido por la spec y va en español.
- **DT-09**: El `HeaderComponent` (layout) inyecta `AuthService` de `core` para mostrar el alias (`currentUser` signal) y el botón "Cerrar sesión" (RF-11, RF-13). La constitución define `core` como infraestructura usada por toda la aplicación y no prohíbe que `layout` dependa de ella.
  - Alternativas consideradas: que el `ShellComponent` obtenga el alias y lo pase al header por `input()` y reciba el cierre por `output()` (sigue necesitando `core` en `layout`, solo lo desplaza).
  - Motivo: menor acoplamiento entre componentes de layout sin ganar aislamiento real.
- **DT-10**: `logout()` llama a `POST /api/auth/logout` y, **tanto si responde bien como si falla**, limpia la sesión local y navega a `/login` (sin `returnUrl`).
  - Alternativas consideradas: esperar a que el backend confirme para limpiar.
  - Motivo: el usuario debe salir aunque haya un error de red; el backend es idempotente (backend:CB-10).

## 4. Componentes afectados

| Componente | Proyecto | Tipo de cambio (nuevo / modificado / eliminado) | Relacionado con |
|---|---|---|---|
| `core/services/auth.models.ts` | pay-bank-frontend | nuevo | backend:CB-01 |
| `core/services/auth.service.ts` | pay-bank-frontend | nuevo | RF-08, RF-09, RF-11, RF-12, RF-13, DT-01–DT-04, DT-10 |
| `core/interceptors/auth.interceptor.ts` | pay-bank-frontend | nuevo | RF-09, RF-12, DT-01, DT-03 |
| `core/guards/auth.guard.ts` | pay-bank-frontend | nuevo | RF-05, RF-07, RF-08, DT-02, DT-05 |
| `core/guards/guest.guard.ts` | pay-bank-frontend | nuevo | RF-14, DT-05 |
| `features/login/login.routes.ts` | pay-bank-frontend | nuevo | RF-01 |
| `features/login/login-page/login-page.{ts,html,scss}` | pay-bank-frontend | nuevo | RF-01, RF-02, RF-04, RF-07, DT-06–DT-08 |
| `layout/header/header.{ts,html,scss}` | pay-bank-frontend | modificado | RF-11, RF-13, DT-09 |
| `app.routes.ts` | pay-bank-frontend | modificado | RF-05, RF-14, DT-05 |
| `app.config.ts` | pay-bank-frontend | modificado | DT-03 |
| Tests `*.spec.ts` de los anteriores | pay-bank-frontend | nuevo | CA del alcance |

## 5. Cambios necesarios

- **CB-01**: Crear `core/services/auth.models.ts` con `LoginRequest { alias: string; password: string }` y `AuthResponse { accessToken: string; tokenType: 'Bearer'; expiresIn: number; alias: string }`, reflejo exacto del contrato de backend:CB-01 — Cubre: RF-03 (consumo), RF-13
- **CB-02**: Crear `AuthService` (`providedIn: 'root'`):
  - Estado: signals privados `accessToken` y `currentUser` (alias); públicos de solo lectura `currentUser` e `isAuthenticated` (`computed`).
  - `login(request): Observable<void>`: `POST {apiBaseUrl}/api/auth/login` con `withCredentials`; guarda token y alias.
  - `refresh(): Observable<string>`: `POST {apiBaseUrl}/api/auth/refresh` con `withCredentials`, compartido mientras esté en curso (DT-03); en éxito actualiza token y alias; en error limpia la sesión y propaga el error.
  - `ensureSession(): Observable<boolean>` (DT-02).
  - `logout(): void` (DT-10).
  - `accessTokenValue()` y `clearSession()` para uso del interceptor.
  — Cubre: RF-08, RF-09, RF-11, RF-12, RF-13, RN-04
- **CB-03**: Crear `authInterceptor` (`HttpInterceptorFn`): para peticiones a `apiBaseUrl` que no sean `/api/auth/*`, añade `Authorization: Bearer <token>` si hay token; ante `401`, `refresh()` y reintenta una vez con el nuevo token; si la renovación falla, `AuthService` lleva al login con `returnUrl` (DT-04) y se propaga el error. No toca peticiones a otros orígenes. Registrar en `withInterceptors([authInterceptor])` de `app.config.ts` — Cubre: RF-09, RF-12
- **CB-04**: Crear `authGuard` (`CanActivateFn` usada también como `canActivateChild`): `ensureSession()`; si `false`, `UrlTree` a `/login` con `queryParams.returnUrl = state.url` — Cubre: RF-05, RF-07, RF-08, RN-06
- **CB-05**: Crear `guestGuard` (`CanActivateFn`): `ensureSession()`; si `true`, `UrlTree` a `/` — Cubre: RF-14
- **CB-06**: Crear la funcionalidad `features/login`: `login.routes.ts` y `LoginPageComponent` (`app-login-page`) con el formulario de DT-07, botón "Iniciar sesión", mensajes de DT-08, `returnUrl` recibido como `input()` (gracias a `withComponentInputBinding`) y navegación tras el éxito según DT-06. La página se muestra fuera del shell (sin header ni sidebar) — Cubre: RF-01, RF-02, RF-04, RF-07, RN-01, RN-05
- **CB-07**: Modificar `app.routes.ts`: añadir `{ path: 'login', canActivate: [guestGuard], loadChildren: () => import('./features/login/login.routes') }` antes de la ruta del shell; añadir `canActivate: [authGuard]` y `canActivateChild: [authGuard]` a la ruta `''` del shell; mantener `'**' → ''` — Cubre: RF-05, RF-14
- **CB-08**: Modificar `HeaderComponent`: mostrar `currentUser()` (alias) y un botón "Cerrar sesión" que invoca `AuthService.logout()`, con estilos SCSS acordes al header actual — Cubre: RF-11, RF-13

### Modelo de datos y migraciones

### Contratos e integraciones

- Consume `POST /api/auth/login`, `POST /api/auth/refresh` y `POST /api/auth/logout` tal como se definen en `pay-bank-backend/docs/001-inicio-sesion/plan.md` (apartado "Contratos e integraciones") y en `pay-bank-backend/docs/api/openapi.yaml`. No se duplican aquí.
- El resto de llamadas a la API llevan `Authorization: Bearer <accessToken>` (añadida por el interceptor). Nunca se envía el identificador del usuario.

### Configuración y seguridad

- Se reutiliza `environment.apiBaseUrl` como único punto de configuración de la URL del backend; no se añaden variables nuevas.
- El backend debe permitir CORS con credenciales para `http://localhost:4200` (backend:DT-09).
- Ninguna credencial se guarda en `localStorage`, `sessionStorage` ni cookies accesibles desde JavaScript.

## 6. Estrategia de verificación

Tests unitarios con Vitest (`ng test`), `HttpTestingController` para HTTP y `RouterTestingHarness` con rutas de prueba (un componente ficticio en `/movements` y en `/`, ya que el panel y los movimientos aún no existen):

| Criterio de aceptación | Cómo se verifica (tipo de prueba / nivel) | Cambios implicados |
|---|---|---|
| CA-01 | Unitario de componente: existen los campos alias y contraseña (`type="password"`) y el botón "Iniciar sesión" | CB-06 |
| CA-02 | Unitario de componente: enviar con alias vacío, con contraseña vacía y con ambos vacíos → no hay petición HTTP y se muestra el mensaje de obligatorio del campo vacío | CB-06 |
| CA-03 | Unitario: login correcto sin `returnUrl` → se navega a `/`. Manual contra el backend con un usuario de `data.sql` | CB-02, CB-06 |
| CA-05, CA-06 | Unitario de componente: respuesta `401` → se muestra "Alias o contraseña incorrectos", se sigue en `/login` y `isAuthenticated()` es `false`. Error `500` → mensaje genérico de DT-08 | CB-06 |
| CA-08 | Unitario de router: sin sesión y con renovación `401`, navegar a `/movements` → `/login?returnUrl=%2Fmovements`; tras login correcto → `/movements`. `returnUrl` externo (`//evil.com`) → `/` | CB-04, CB-06, CB-07 |
| CA-10 | Unitario: sin token en memoria, `authGuard` hace una renovación con éxito y permite el acceso sin pasar por el login. Manual: recargar la página y cerrar/reabrir el navegador con sesión abierta | CB-02, CB-04 |
| CA-11 | Unitario del interceptor: petición con `401` → una renovación y reintento con el nuevo token; dos peticiones concurrentes con `401` → una sola petición a `/api/auth/refresh`. Manual: esperar > 15 min y realizar una acción | CB-02, CB-03 |
| CA-13 | Unitario del interceptor/servicio: renovación con `401` → sesión limpia y navegación a `/login` con `returnUrl` | CB-02, CB-03 |
| CA-14 | Unitario: `logout()` llama a `/api/auth/logout`, limpia la sesión y navega a `/login` (también si el backend falla); después, navegar a `/` con renovación `401` → `/login` | CB-02, CB-08 |
| CA-15 | Unitario de componente: con `currentUser = 'ana'`, el header muestra "ana" | CB-08 |
| CA-16 | Unitario de router: con sesión, navegar a `/login` → `/` | CB-05, CB-07 |
| CA-04, CA-07, CA-09, CA-12, CA-17 | Se verifican en el backend | `pay-bank-backend` |

## 7. Riesgos y mitigaciones

- **Panel y movimientos aún no existen**: CA-03 y CA-08 se verifican con rutas de prueba; al implementar el panel debe registrarse como ruta hija por defecto del shell (`/`) para que el destino tras el login sea el panel sin cambiar este plan.
- **Varias pestañas renovando a la vez** con la misma cookie: la renovación es de un solo uso (backend:DT-04) y una pestaña podría volver al login. Mitigación parcial: renovación compartida dentro de cada pestaña (DT-03). Riesgo aceptado.
- **Reintento infinito**: el interceptor nunca intercepta `/api/auth/*` y reintenta como máximo una vez, por lo que un `401` tras renovar se propaga sin bucles.
- **Cookies entre `localhost:4200` y `localhost:8080`**: son el mismo sitio, por lo que `SameSite=Strict` permite el envío; requiere `withCredentials: true` en `/api/auth/*` y CORS con credenciales en el backend. Si no se configura, la sesión no sobrevive a la recarga (detectable en la verificación manual de CA-10).

## 8. Elementos de la spec no cubiertos

- RF-03, RF-06, RF-10 y la invalidación en servidor de RF-11 → `pay-bank-backend` (backend:CB-05–CB-10).
- RN-01 (comparación sin mayúsculas), RN-02, RN-03, RN-04 (solo uso) y RN-05 (respuesta uniforme de la API) → `pay-bank-backend`.
- CA-04, CA-07, CA-09, CA-12, CA-17 → `pay-bank-backend`.
