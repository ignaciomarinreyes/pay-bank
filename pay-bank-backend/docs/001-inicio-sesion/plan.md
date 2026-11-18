# Plan de implementación: Inicio de sesión

> Especificación: `001-inicio-sesion` · [spec.md](../../../docs/001-inicio-sesion/spec.md)
> Ámbito de la spec: Global · Proyecto de este plan: `pay-bank-backend`
> Otros planes de esta spec: `pay-bank-frontend/docs/001-inicio-sesion/plan.md`

## Alcance en este proyecto

- **RF**: RF-03, RF-04 (rechazo y mensaje uniforme en la API; el texto visible lo pone el frontend), RF-06, RF-08 y RF-09 (emisión y vigencia de credenciales que permiten mantener y renovar la sesión), RF-10, RF-11 (invalidación en servidor), RF-12 (rechazo de la renovación), RF-13 (la API devuelve el alias).
- **RN**: RN-01, RN-02, RN-03, RN-04, RN-05, RN-06.
- **CA**: CA-04, CA-05, CA-06, CA-07, CA-09, CA-11, CA-12, CA-13, CA-14 y CA-17 (parte de servidor); CA-03 (parte de servidor).

## 1. Resumen de la estrategia

1. Definir en `docs/api/openapi.yaml` los tres endpoints de autenticación (`login`, `refresh`, `logout`) y sus DTOs; regenerar `AuthApi`.
2. Crear el modelo `User` y `RefreshToken`, sus repositorios y la precarga de usuarios con `data.sql`.
3. Implementar la infraestructura de seguridad: configuración de Spring Security sin estado, emisión/validación de JWT de acceso, filtro de autenticación, `UserDetailsService`, CORS con credenciales y respuesta `401` uniforme.
4. Implementar `AuthService` (login, rotación y revocación de credenciales de renovación) y `AuthController implements AuthApi`, que gestiona la cookie de renovación.
5. Añadir la gestión global de errores con `ProblemDetail`.
6. Cubrir los CA con tests de integración (MockMvc + HSQLDB) y tests unitarios del proveedor de tokens, controlando el tiempo con un `Clock` inyectable.

## 2. Contexto técnico

- Java 25, Spring Boot 4.1.0, Spring Security, Spring Data JPA, PostgreSQL (Docker) y HSQLDB en tests. Ya están en el `pom.xml`: `jjwt` 0.13.0, `springdoc`, `openapi-generator-maven-plugin` (genera `com.paybank.generated.api` / `com.paybank.generated.model` con `interfaceOnly`, `useTags`).
- El código es solo esqueleto: existen los paquetes `user/*`, `security`, `config`, `exception`, `common/web` con `package-info.java` y `PayBankApplicationTests` (`contextLoads`). No hay entidades, controladores ni configuración de seguridad.
- `openapi.yaml` tiene `paths: {}`, el esquema `bearerAuth` y seguridad global `bearerAuth`.
- `application.yml` importa `secret-local.yml` (contraseña del datasource y `jwt.secret`); `ddl-auto: update`. El `application.yml` de tests (HSQLDB, `create-drop`, `jwt.secret`) **sustituye** al principal en el classpath de tests, por lo que toda propiedad nueva necesaria en tests debe añadirse también ahí.
- Convenciones de la constitución que se siguen: package-by-feature con capas controller/service/repository, controladores que implementan interfaces generadas, código en inglés, contraseñas BCrypt, sin roles, `401` sin token o con token inválido/caducado, usuario actual siempre desde el token.

## 3. Decisiones técnicas

- **DT-01**: Credencial de acceso = JWT firmado HS256 con `jwt.secret` (jjwt), validez 15 min (RN-02). Claims: `sub` = id del usuario, `alias`, `iat`, `exp`.
  - Alternativas consideradas: RS256 con par de claves (innecesario con un único emisor/consumidor); token opaco consultado en BD (contradice la constitución).
  - Motivo: dependencia ya presente, secreto ya configurado, validación sin acceso a BD.
- **DT-02**: Credencial de renovación = valor aleatorio opaco (32 bytes de `SecureRandom`, Base64URL), no JWT. En BD se guarda solo su hash SHA-256, con fecha de expiración (7 días desde su emisión, RN-03) y fecha de revocación.
  - Alternativas consideradas: refresh JWT (no se puede invalidar sin consultar BD, que es lo que exige RN-04); guardar el valor en claro (una fuga de BD daría sesiones válidas).
  - Motivo: permite un solo uso y revocación (RN-04, RF-10, RF-11) y no expone credenciales usables si se filtra la BD.
- **DT-03**: La credencial de renovación viaja en una cookie `refresh_token` `HttpOnly`, `SameSite=Strict`, `Path=/api/auth`, `Max-Age` = 7 días, `Secure` configurable (por defecto `true`; los navegadores la aceptan en `http://localhost`). Nunca aparece en el cuerpo de las respuestas. La credencial de acceso va en el cuerpo JSON.
  - Alternativas consideradas: ambas credenciales en el cuerpo JSON y guardadas por el frontend en `localStorage` (vulnerable a XSS durante 7 días).
  - Motivo: decisión del usuario; JavaScript no puede leer la credencial de renovación.
- **DT-04**: Rotación atómica: la renovación revoca la credencial usada con una actualización condicional (`revokedAt` solo si es nula) y solo si afecta a una fila emite el nuevo par. Si la credencial ya estaba revocada, caducada o no existe → `401`. No se revoca en cascada la "familia" de credenciales ante una reutilización.
  - Alternativas consideradas: bloqueo pesimista de la fila; revocación de todas las sesiones del usuario al detectar reutilización (no exigido por la spec).
  - Motivo: garantiza el solo uso (RN-04) aun con dos peticiones simultáneas, sin bloqueo explícito.
- **DT-05**: Spring Security sin estado: `SessionCreationPolicy.STATELESS`, CSRF deshabilitado, `JwtAuthFilter` antes de `UsernamePasswordAuthenticationFilter`, `AuthenticationEntryPoint` propio que responde `401` con `ProblemDetail`. Públicos: `POST /api/auth/login`, `POST /api/auth/refresh`, `POST /api/auth/logout`, `/swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs/**`, `/error`. Todo lo demás, `authenticated()`.
  - Alternativas consideradas: mantener CSRF con token en cookie (innecesario: la única cookie es `SameSite=Strict` y limitada a `/api/auth`, y el resto de la API usa cabecera `Authorization`).
  - Motivo: RF-06 y constitución (sin roles, RN-06).
- **DT-06**: Autenticación del login con `AuthenticationManager` + `DaoAuthenticationProvider` (`UserDetailsService` propio + `BCryptPasswordEncoder`). Cualquier fallo (`BadCredentialsException`, usuario inexistente) se traduce en una única `InvalidCredentialsException` → `401` con el mismo cuerpo.
  - Alternativas consideradas: comprobar a mano con el repositorio y `PasswordEncoder.matches`.
  - Motivo: `DaoAuthenticationProvider` ya ejecuta una comparación BCrypt ficticia cuando el usuario no existe, lo que iguala tiempos de respuesta y no revela si el alias existe (RN-05).
- **DT-07**: Alias sin distinguir mayúsculas (RN-01): se guarda normalizado en minúsculas (`toLowerCase(Locale.ROOT)`) con restricción única, y la búsqueda normaliza la entrada igual. La contraseña se compara tal cual (BCrypt).
  - Alternativas consideradas: `findByAliasIgnoreCase` con índice funcional `lower(alias)` (no lo crea `ddl-auto`).
  - Motivo: unicidad garantizada por la BD y consulta por índice simple.
- **DT-08**: Precarga de usuarios mediante `src/main/resources/data.sql` con `INSERT ... ON CONFLICT (alias) DO NOTHING` (idempotente) y contraseñas ya cifradas con BCrypt; la contraseña de prueba en claro se documenta en un comentario del propio fichero. Se activa con `spring.sql.init.mode: always` y `spring.jpa.defer-datasource-initialization: true`. En tests, `spring.sql.init.mode: never` (HSQLDB no admite `ON CONFLICT`); los tests crean sus propios usuarios.
  - Alternativas consideradas: seeder Java en perfil `dev`; inserción manual.
  - Motivo: decisión del usuario.
- **DT-09**: CORS con credenciales para el origen del frontend, configurable con `cors.allowed-origins` (por defecto `http://localhost:4200`), métodos `GET, POST, PUT, PATCH, DELETE, OPTIONS`, cabeceras `Authorization, Content-Type`, `allowCredentials = true`.
  - Alternativas consideradas: proxy de desarrollo de Angular (el frontend ya define `apiBaseUrl` absoluta a `http://localhost:8080`).
  - Motivo: coherente con la configuración actual del frontend.
- **DT-10**: Errores con `ProblemDetail` (RFC 9457) desde `GlobalExceptionHandler` (`@RestControllerAdvice`): `400` validación, `401` credenciales/token/renovación. Los `title`/`detail` van en inglés; el texto visible al usuario ("Alias o contraseña incorrectos") lo pone el frontend a partir del `401` del login.
  - Alternativas consideradas: DTO de error propio en OpenAPI.
  - Motivo: soporte nativo de Spring y formato estándar.
- **DT-11**: Tiempo inyectable: bean `Clock` (UTC) en `config/` usado por `JwtTokenProvider` (también en el parser de jjwt) y `AuthService`. Duraciones configurables con `@ConfigurationProperties` en `JwtConfig` (`jwt.secret`, `jwt.access-token-ttl: 15m`, `jwt.refresh-token-ttl: 7d`), validando que el secreto tenga al menos 32 caracteres.
  - Alternativas consideradas: esperar tiempos reales o manipular el token en tests.
  - Motivo: permite verificar CA-11 y CA-13 de forma determinista.
- **DT-12**: La respuesta de `login` y `refresh` incluye el `alias` del usuario, que el frontend usa para mostrarlo (RF-13) también tras recargar la página.
  - Alternativas consideradas: endpoint `GET /api/users/me`; que el frontend decodifique el JWT.
  - Motivo: evita una llamada extra y que el frontend dependa del formato interno del token.

## 4. Componentes afectados

| Componente | Proyecto | Tipo de cambio (nuevo / modificado / eliminado) | Relacionado con |
|---|---|---|---|
| `docs/api/openapi.yaml` | pay-bank-backend | modificado | RF-03, RF-10, RF-11, DT-03, DT-10, DT-12 |
| `user/model/User` | pay-bank-backend | nuevo | RF-03, RN-01, DT-07 |
| `user/model/RefreshToken` | pay-bank-backend | nuevo | RF-10, RF-11, RN-03, RN-04, DT-02 |
| `user/repository/UserRepository` | pay-bank-backend | nuevo | RF-03, DT-07 |
| `user/repository/RefreshTokenRepository` | pay-bank-backend | nuevo | RF-10, RF-11, DT-04 |
| `user/service/AuthService` | pay-bank-backend | nuevo | RF-03, RF-04, RF-10, RF-11, RF-12, DT-02, DT-04, DT-06 |
| `user/controller/AuthController` | pay-bank-backend | nuevo | RF-03, RF-10, RF-11, DT-03 |
| `user/exception/InvalidCredentialsException`, `InvalidRefreshTokenException` | pay-bank-backend | nuevo | RF-04, RF-12, RN-05 |
| `security/JwtTokenProvider` | pay-bank-backend | nuevo | RF-03, RF-06, RN-02, DT-01 |
| `security/JwtAuthFilter` | pay-bank-backend | nuevo | RF-06, DT-05 |
| `security/AppUserDetailsService` | pay-bank-backend | nuevo | RF-03, RN-01, DT-06, DT-07 |
| `security/AuthenticatedUser` | pay-bank-backend | nuevo | RF-06 |
| `security/RestAuthenticationEntryPoint` | pay-bank-backend | nuevo | RF-06, DT-05, DT-10 |
| `config/SecurityConfig` | pay-bank-backend | nuevo | RF-06, RN-06, DT-05, DT-06, DT-09 |
| `config/JwtConfig` | pay-bank-backend | nuevo | RN-02, RN-03, DT-11 |
| `config/ClockConfig` | pay-bank-backend | nuevo | DT-11 |
| `exception/GlobalExceptionHandler` | pay-bank-backend | nuevo | RF-04, RF-12, DT-10 |
| `src/main/resources/application.yml` | pay-bank-backend | modificado | DT-03, DT-08, DT-09, DT-11 |
| `src/main/resources/data.sql` | pay-bank-backend | nuevo | DT-08 |
| `src/test/resources/application.yml` | pay-bank-backend | modificado | DT-08, DT-11 |
| Tests de integración y unitarios | pay-bank-backend | nuevo | Todos los CA del alcance |

## 5. Cambios necesarios

- **CB-01**: Definir en `openapi.yaml` el tag `Auth` con `POST /api/auth/login`, `POST /api/auth/refresh` y `POST /api/auth/logout` (todos con `security: []`), los esquemas `LoginRequest` y `AuthResponse`, la cookie `refresh_token` y las respuestas de error `ProblemDetail`; regenerar `AuthApi` (ver "Contratos e integraciones") — Cubre: RF-03, RF-10, RF-11, RF-13
- **CB-02**: Crear la entidad `User` (tabla `users`: `id` Long identidad, `alias` único no nulo normalizado en minúsculas, `password` hash BCrypt no nulo) y `UserRepository.findByAlias(String)` — Cubre: RF-03, RN-01
- **CB-03**: Crear la entidad `RefreshToken` (tabla `refresh_tokens`: `id`, `user` `@ManyToOne` obligatorio, `tokenHash` único no nulo, `createdAt`, `expiresAt`, `revokedAt` nullable) y `RefreshTokenRepository` con `findByTokenHash` y una actualización `@Modifying` que revoca por `id` solo si `revokedAt` es nula y devuelve el nº de filas afectadas — Cubre: RF-10, RF-11, RN-03, RN-04
- **CB-04**: Crear `JwtConfig` (`@ConfigurationProperties("jwt")`: `secret`, `accessTokenTtl`, `refreshTokenTtl`, validación de secreto ≥ 32 caracteres) y `ClockConfig` (bean `Clock.systemUTC()`); añadir a `application.yml` y al de tests `jwt.access-token-ttl: 15m` y `jwt.refresh-token-ttl: 7d` — Cubre: RN-02, RN-03
- **CB-05**: Crear `JwtTokenProvider`: `generateAccessToken(User)` (HS256, claims de DT-01, `exp` = ahora + 15 min según `Clock`) y `parse(String)` que devuelve `AuthenticatedUser(id, alias)` o lanza excepción si la firma no es válida, el token está mal formado o ha caducado — Cubre: RF-03, RF-06, RN-02
- **CB-06**: Crear `AppUserDetailsService` (carga por alias normalizado; `UsernameNotFoundException` si no existe), `BCryptPasswordEncoder` y `AuthenticationManager` con `DaoAuthenticationProvider` — Cubre: RF-03, RN-01, RN-05
- **CB-07**: Crear `JwtAuthFilter` (`OncePerRequestFilter`): si hay cabecera `Authorization: Bearer <token>` válida, establece en el `SecurityContext` una autenticación con principal `AuthenticatedUser` y sin autoridades; si el token no es válido, no autentica (el entry point responde `401`) — Cubre: RF-06, RN-06
- **CB-08**: Crear `RestAuthenticationEntryPoint` (respuesta `401` `application/problem+json`) y `SecurityConfig` según DT-05 y DT-09 (`cors.allowed-origins` en `application.yml`) — Cubre: RF-06, RN-06
- **CB-09**: Crear `AuthService`:
  - `login(alias, password)`: autentica con el `AuthenticationManager`; si falla lanza `InvalidCredentialsException`; si no, genera token de acceso y una credencial de renovación nueva (guarda su hash con `expiresAt` = ahora + 7 días) y devuelve ambos valores más el alias.
  - `refresh(rawToken)`: si falta, no existe, está caducada (`expiresAt` ≤ ahora) o revocada → `InvalidRefreshTokenException`; si no, la revoca con la actualización condicional (si afecta 0 filas → `InvalidRefreshTokenException`) y emite un nuevo par. Transaccional.
  - `logout(rawToken)`: si existe y no está revocada, la revoca; idempotente, nunca falla.
  — Cubre: RF-03, RF-04, RF-10, RF-11, RF-12, RN-03, RN-04, RN-05
- **CB-10**: Crear `AuthController implements AuthApi`: traduce `AuthService` a respuestas; en `login` y `refresh` devuelve `AuthResponse` y la cabecera `Set-Cookie` con la nueva credencial (DT-03); en `logout` devuelve `204` y una cookie de borrado (`Max-Age=0`, mismos `Path`/`SameSite`); si `refresh` falla, `401` y también la cookie de borrado — Cubre: RF-03, RF-10, RF-11, RF-12, RF-13
- **CB-11**: Crear `InvalidCredentialsException`, `InvalidRefreshTokenException` y `GlobalExceptionHandler` (`401` con `title` "Invalid credentials" / "Invalid refresh token"; `400` con detalle de campos para errores de validación de `LoginRequest`) — Cubre: RF-04, RF-12, RN-05
- **CB-12**: Crear `data.sql` con al menos dos usuarios de prueba (alias en minúsculas, hash BCrypt, contraseña en claro documentada en comentario) usando `ON CONFLICT (alias) DO NOTHING`; en `application.yml` `spring.sql.init.mode: always` y `spring.jpa.defer-datasource-initialization: true`; en el `application.yml` de tests `spring.sql.init.mode: never` — Cubre: precondición de RF-03 (usuarios precargados)
- **CB-13**: Tests (ver apartado 6): integración con `@SpringBootTest` + MockMvc + HSQLDB y un `Clock` de test ajustable; unitarios de `JwtTokenProvider`; un controlador protegido de prueba (`/api/test/ping`, solo en `src/test`) para comprobar la protección de la API — Cubre: verificación de todos los CA del alcance

### Modelo de datos y migraciones

- Tabla `users`: `id` (PK, identidad), `alias` (varchar, único, no nulo, minúsculas), `password` (varchar, no nulo, hash BCrypt).
- Tabla `refresh_tokens`: `id` (PK, identidad), `user_id` (FK a `users`, no nulo), `token_hash` (varchar(64), único, no nulo), `created_at`, `expires_at` (no nulos), `revoked_at` (nullable).
- Sin migraciones: el esquema lo crea Hibernate (`ddl-auto: update` / `create-drop`), según la constitución.
- Las credenciales de renovación caducadas o revocadas no se purgan en esta funcionalidad (ver riesgos).

### Contratos e integraciones

Contrato compartido con el frontend (fuente de verdad: `docs/api/openapi.yaml`):

| Operación | Petición | Respuestas |
|---|---|---|
| `POST /api/auth/login` (`operationId: login`) | Cuerpo `LoginRequest { alias: string (obligatorio, minLength 1), password: string (obligatorio, minLength 1) }` | `200` `AuthResponse` + `Set-Cookie: refresh_token`; `400` `ProblemDetail` (validación); `401` `ProblemDetail` (credenciales incorrectas, mismo cuerpo exista o no el alias) |
| `POST /api/auth/refresh` (`operationId: refresh`) | Sin cuerpo; cookie `refresh_token` | `200` `AuthResponse` + nueva `Set-Cookie: refresh_token`; `401` `ProblemDetail` + cookie de borrado (ausente, desconocida, caducada o ya usada/revocada) |
| `POST /api/auth/logout` (`operationId: logout`) | Sin cuerpo; cookie `refresh_token` opcional | `204` + cookie de borrado (siempre, idempotente) |

- `AuthResponse { accessToken: string, tokenType: "Bearer", expiresIn: integer (segundos, 900), alias: string }`.
- Resto de la API: cabecera `Authorization: Bearer <accessToken>`; sin ella o con token inválido/caducado → `401` `ProblemDetail`.
- El navegador debe enviar las peticiones a `/api/auth/*` con credenciales (cookies) para que viaje `refresh_token`.

### Configuración y seguridad

- `application.yml`: `jwt.access-token-ttl: 15m`, `jwt.refresh-token-ttl: 7d`, `auth.refresh-cookie.secure: true`, `cors.allowed-origins: http://localhost:4200`, `spring.sql.init.mode: always`, `spring.jpa.defer-datasource-initialization: true`. `jwt.secret` sigue en `secret-local.yml`.
- `src/test/resources/application.yml`: añadir `jwt.access-token-ttl`, `jwt.refresh-token-ttl`, `cors.allowed-origins` y `spring.sql.init.mode: never`.
- Contraseñas con BCrypt; la credencial de renovación solo se persiste como hash SHA-256.
- Nunca se registran en logs contraseñas ni credenciales.

## 6. Estrategia de verificación

| Criterio de aceptación | Cómo se verifica (tipo de prueba / nivel) | Cambios implicados |
|---|---|---|
| CA-03 (servidor) | Integración: login con credenciales correctas → `200`, `AuthResponse` con `accessToken`, `alias`, `expiresIn = 900` y `Set-Cookie refresh_token` `HttpOnly; SameSite=Strict; Path=/api/auth` | CB-01, CB-09, CB-10 |
| CA-04 | Integración: usuario `ana` creado en el test; login con `ANA` → `200` y `alias = "ana"` | CB-02, CB-06 |
| CA-05 | Integración: contraseña incorrecta → `401`, sin `Set-Cookie`, sin filas nuevas en `refresh_tokens` | CB-09, CB-11 |
| CA-06 | Integración: alias inexistente → `401` con cuerpo idéntico (mismos `status`, `title`, `detail`) al de CA-05 | CB-06, CB-11 |
| CA-07 | Integración: contraseña `Secreta1` y login con `secreta1` → `401` | CB-06 |
| CA-09 | Integración contra `/api/test/ping`: sin cabecera, con token de firma alterada y con token caducado (reloj de test avanzado > 15 min) → `401` sin cuerpo de datos; con token válido → `200`. Unitarios de `JwtTokenProvider` para firma inválida, malformado y caducado | CB-05, CB-07, CB-08 |
| CA-11 | Integración: tras login, avanzar el reloj 16 min → `/api/test/ping` con el token `401`; `refresh` con la cookie → `200` y el nuevo token accede a `/api/test/ping` | CB-05, CB-09, CB-10 |
| CA-12 | Integración: `refresh` con una cookie → `200`; repetir con la misma cookie → `401` y cookie de borrado | CB-03, CB-09 |
| CA-13 | Integración: tras login, avanzar el reloj 7 días + 1 s → `refresh` `401`; con 6 días → `200` | CB-04, CB-09 |
| CA-14 (servidor) | Integración: `logout` con la cookie → `204` y cookie de borrado; `refresh` con la cookie anterior → `401`; `logout` sin cookie → `204` | CB-09, CB-10 |
| CA-17 | Integración: dos usuarios distintos inician sesión y ambos obtienen `200` en `/api/test/ping` | CB-07, CB-08 |
| CA-01, CA-02, CA-08, CA-10, CA-15, CA-16 | Se verifican en el frontend | `pay-bank-frontend` |

Además: test de `AuthService` que lanza dos `refresh` concurrentes con la misma credencial y comprueba que solo uno tiene éxito (DT-04), y comprobación manual en Swagger UI de los tres endpoints con los usuarios de `data.sql`.

## 7. Riesgos y mitigaciones

- **Renovaciones simultáneas desde varias pestañas** con la misma cookie: la segunda fallaría y cerraría la sesión de esa pestaña. Mitigación: el frontend comparte una única renovación en curso por pestaña; si aún ocurre entre pestañas, el usuario vuelve a iniciar sesión (comportamiento seguro). Se podría añadir un margen de gracia en el futuro.
- **Crecimiento de `refresh_tokens`**: no se purgan las caducadas/revocadas. Impacto bajo en esta fase; una tarea de limpieza programada queda para otra funcionalidad.
- **Cookie `Secure` en HTTP**: los navegadores la aceptan en `localhost`; para otros hosts de desarrollo sin HTTPS habría que poner `auth.refresh-cookie.secure: false`.
- **`data.sql` específico de PostgreSQL**: no se ejecuta en tests (`spring.sql.init.mode: never`); un error de sintaxis solo se detectaría al arrancar la aplicación. Mitigación: comprobación manual al arrancar con Docker.
- **`application.yml` de tests sustituye al principal**: si se olvida una propiedad nueva en el de tests, falla el contexto. Mitigación: `contextLoads` existente lo detecta.

## 8. Elementos de la spec no cubiertos

- RF-01, RF-02, RF-05, RF-07, RF-14 → `pay-bank-frontend` (pantallas, validación del formulario y navegación).
- RF-04 (texto "Alias o contraseña incorrectos"), RF-08 (conservar la sesión al recargar), RF-09 (renovación automática transparente), RF-11 (eliminar la sesión en el navegador y volver al login), RF-12 (volver al login), RF-13 (mostrar el alias) → parte visible en `pay-bank-frontend`; este plan aporta el contrato que lo hace posible.
- CA-01, CA-02, CA-08, CA-10, CA-15, CA-16 y la parte visible de CA-03 y CA-14 → `pay-bank-frontend`.
