# Tareas de implementación: Inicio de sesión

> Especificación: `001-inicio-sesion` · [spec.md](../../../docs/001-inicio-sesion/spec.md) · Plan: [plan.md](plan.md)
> Ámbito de la spec: Global · Proyecto de estas tareas: `pay-bank-backend`

> Estados: `[ ]` pendiente · `[~]` en curso · `[x]` completada

## Orden de ejecución

```
(T-01 ∥ T-02 ∥ T-03) → (T-04 ∥ T-05 ∥ T-07 ∥ T-11) → (T-06 ∥ T-08) → T-09 → T-10
```

- T-01, T-02 y T-03 no tienen dependencias.
- T-04 depende de T-02; T-05 y T-11 de T-03; T-07 de T-01.
- T-06 necesita T-04 y T-05; T-08 necesita T-03, T-04, T-05 y T-07.
- T-09 integra seguridad y servicio (T-06 + T-08); T-10 completa la verificación de la renovación y el cierre de sesión.

## Tareas

### [ ] T-01: Definir el contrato de autenticación en OpenAPI

- **Qué hacer**: En `docs/api/openapi.yaml`, añadir bajo el tag `Auth` las operaciones `login`, `refresh` y `logout` (`/api/auth/*`, con `security: []`), los esquemas `LoginRequest` y `AuthResponse`, el parámetro de cookie `refresh_token` y las respuestas de error `ProblemDetail`, tal como se definen en "Contratos e integraciones" del plan. Resultado: `mvn generate-sources` termina sin errores y genera `com.paybank.generated.api.AuthApi` y `com.paybank.generated.model.LoginRequest` / `AuthResponse`; Swagger UI muestra las tres operaciones. (CB-01, DT-03, DT-10, DT-12 · RF-03, RF-10, RF-11, RF-13)

### [ ] T-02: Configurar duraciones de credenciales y reloj

- **Qué hacer**: Crear `config/JwtConfig` (`@ConfigurationProperties("jwt")` con `secret`, `accessTokenTtl`, `refreshTokenTtl` y validación de secreto ≥ 32 caracteres) y `config/ClockConfig` (bean `Clock` UTC). Añadir `jwt.access-token-ttl: 15m` y `jwt.refresh-token-ttl: 7d` en `src/main/resources/application.yml` y en `src/test/resources/application.yml`. Resultado: `PayBankApplicationTests.contextLoads` pasa y un test comprueba que arrancar con un secreto de menos de 32 caracteres hace fallar el contexto. (CB-04, DT-11 · RN-02, RN-03)

### [ ] T-03: Crear las entidades y repositorios de usuario y credencial de renovación

- **Qué hacer**: En `user/model` crear `User` (tabla `users`) y `RefreshToken` (tabla `refresh_tokens`) según "Modelo de datos y migraciones" del plan; en `user/repository` crear `UserRepository.findByAlias` y `RefreshTokenRepository` con `findByTokenHash` y la revocación condicional `@Modifying`. Resultado: tests `@DataJpaTest` (HSQLDB) que comprueban que dos usuarios con el mismo alias violan la restricción única, que `findByAlias` encuentra al usuario guardado y que la revocación condicional devuelve 1 la primera vez y 0 la segunda sobre la misma credencial. (CB-02, CB-03, DT-02, DT-04, DT-07 · RN-01, RN-04)

### [ ] T-04: Implementar el proveedor de tokens JWT

- **Qué hacer**: Crear `security/AuthenticatedUser` (id, alias) y `security/JwtTokenProvider` con `generateAccessToken(User)` y `parse(String)` usando `JwtConfig` y el `Clock`. Resultado: tests unitarios con un `Clock` fijo que comprueban que el token generado contiene `sub` = id y `alias`, que expira a los 15 minutos (válido a los 14:59, rechazado a los 15:01) y que se rechazan un token con firma alterada y un texto mal formado. (CB-05, DT-01, DT-11 · RF-03, RF-06, RN-02, CA-09 · depende de T-02)

### [ ] T-05: Configurar la autenticación por alias y contraseña

- **Qué hacer**: Crear `security/AppUserDetailsService` (búsqueda por alias normalizado en minúsculas) y, en `config/SecurityConfig`, los beans `BCryptPasswordEncoder` y `AuthenticationManager` con `DaoAuthenticationProvider`. Resultado: test que, con un usuario `ana` guardado con contraseña cifrada, autentica con `ana` y con `ANA`, rechaza la contraseña con distinta capitalización y rechaza un alias inexistente con `BadCredentialsException` (sin revelar `UsernameNotFoundException`). (CB-06, DT-06, DT-07 · RF-03, RN-01, RN-05 · depende de T-03)

### [ ] T-06: Proteger la API con el filtro JWT

- **Qué hacer**: Crear `security/JwtAuthFilter`, `security/RestAuthenticationEntryPoint` y completar `config/SecurityConfig` (sin estado, CSRF deshabilitado, rutas públicas, CORS con credenciales) según DT-05 y DT-09; añadir `cors.allowed-origins: http://localhost:4200` a ambos `application.yml`. Crear en `src/test` el controlador de prueba `/api/test/ping`. Resultado: tests de integración MockMvc en los que `/api/test/ping` responde `401` con `application/problem+json` sin cabecera, con token de firma alterada y con token caducado (reloj de test avanzado más de 15 min), y `200` con token válido de dos usuarios distintos; `/v3/api-docs` responde `200` sin token; una petición preflight `OPTIONS` desde `http://localhost:4200` devuelve `Access-Control-Allow-Credentials: true`. (CB-07, CB-08, CB-13, DT-05, DT-09 · RF-06, RN-06, CA-09, CA-17 · depende de T-04, T-05)

### [ ] T-07: Añadir las excepciones de autenticación y la gestión global de errores

- **Qué hacer**: Crear `user/exception/InvalidCredentialsException` e `InvalidRefreshTokenException` y `exception/GlobalExceptionHandler` (`@RestControllerAdvice`) que las traduce a `401` `ProblemDetail` y los errores de validación a `400` con el detalle de los campos. Resultado: test de MockMvc (con un controlador de prueba en `src/test`) que comprueba los `status`, `title` y `Content-Type: application/problem+json` de cada caso. (CB-11, DT-10 · RF-04, RF-12, RN-05 · depende de T-01)

### [ ] T-08: Implementar el servicio de autenticación

- **Qué hacer**: Crear `user/service/AuthService` con `login`, `refresh` y `logout` según CB-09 (generación de credencial de renovación aleatoria, persistencia de su hash SHA-256, rotación transaccional con la revocación condicional, logout idempotente). Resultado: tests de servicio con `Clock` ajustable que comprueban: login correcto devuelve token, credencial y alias, y guarda solo el hash; credenciales incorrectas lanzan `InvalidCredentialsException` sin crear filas; `refresh` rechaza credencial ausente, desconocida, caducada (7 días + 1 s) o revocada; `refresh` válido revoca la anterior y crea otra; `logout` revoca y no falla sin credencial; dos `refresh` concurrentes con la misma credencial producen exactamente un éxito. (CB-09, DT-02, DT-04, DT-06 · RF-03, RF-04, RF-10, RF-11, RF-12, RN-03, RN-04, RN-05 · depende de T-03, T-04, T-05, T-07)

### [ ] T-09: Exponer los endpoints de autenticación y verificar el inicio de sesión

- **Qué hacer**: Crear `user/controller/AuthController implements AuthApi` que gestiona la cookie `refresh_token` según DT-03 y CB-10, y añadir `auth.refresh-cookie.secure` a ambos `application.yml`. Resultado: tests de integración MockMvc: login correcto → `200`, `AuthResponse` con `alias` y `expiresIn = 900` y `Set-Cookie` `HttpOnly; SameSite=Strict; Path=/api/auth`; login con `ANA` → `200` y `alias = "ana"`; contraseña incorrecta → `401` sin `Set-Cookie`; alias inexistente → `401` con cuerpo idéntico al anterior; contraseña con distinta capitalización → `401`; alias o contraseña vacíos → `400`. (CB-10, DT-03 · RF-03, RF-13, CA-03, CA-04, CA-05, CA-06, CA-07 · depende de T-06, T-08)

### [ ] T-10: Verificar la renovación y el cierre de sesión de extremo a extremo

- **Qué hacer**: Añadir tests de integración MockMvc con `Clock` ajustable sobre los endpoints de T-09 y `/api/test/ping`. Resultado: tras login y avance de 16 min, `/api/test/ping` con el token da `401`, `refresh` con la cookie da `200` y el nuevo token accede; reutilizar una cookie ya usada en `refresh` da `401` con cookie de borrado; `refresh` a los 6 días da `200` y a los 7 días + 1 s sin renovar da `401`; `logout` da `204` con cookie de borrado y después `refresh` con la cookie anterior da `401`; `logout` sin cookie da `204`. (CB-10, CB-13 · RF-09, RF-10, RF-11, RF-12, CA-11, CA-12, CA-13, CA-14 · depende de T-09)

### [ ] T-11: Precargar usuarios de prueba

- **Qué hacer**: Crear `src/main/resources/data.sql` con al menos dos usuarios (alias en minúsculas, hash BCrypt, contraseña en claro documentada en un comentario) usando `ON CONFLICT (alias) DO NOTHING`; añadir `spring.sql.init.mode: always` y `spring.jpa.defer-datasource-initialization: true` al `application.yml` principal y `spring.sql.init.mode: never` al de tests. Resultado: `mvn test` pasa (sin ejecutar `data.sql`); con `docker compose up -d db` y `mvn spring-boot:run`, la tabla `users` contiene los usuarios precargados y reiniciar la aplicación no los duplica ni falla; una vez completada T-09, el login desde Swagger UI con uno de ellos devuelve `200`. (CB-12, DT-08 · RF-03 · depende de T-03)
