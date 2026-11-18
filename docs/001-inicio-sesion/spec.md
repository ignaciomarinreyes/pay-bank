# Inicio de sesión

> Ámbito: Global (proyectos afectados: `pay-bank-backend`, `pay-bank-frontend`)

## 1. Objetivo

Garantizar que solo los usuarios identificados pueden usar Pay Bank. El usuario se identifica con su alias y su contraseña y obtiene una sesión que le da acceso al resto de la aplicación (panel, pagos, movimientos y listados de pagos). Sin una sesión válida no se puede acceder a ninguna otra pantalla ni a ninguna operación de la API.

## 2. Descripción funcional

1. Un usuario sin sesión que abre la aplicación, o que intenta acceder a cualquier pantalla distinta de la de inicio de sesión, es llevado a la pantalla de inicio de sesión.
2. En esa pantalla introduce su alias y su contraseña y envía el formulario.
3. Si las credenciales son correctas, el sistema le abre una sesión y le lleva a la pantalla a la que intentaba acceder o, si no intentaba acceder a ninguna en concreto, al panel. Durante la sesión se muestra su alias en la aplicación.
4. Si las credenciales no son correctas, permanece en la pantalla de inicio de sesión y ve un mensaje de error genérico que no revela si el alias existe.
5. La sesión se compone de una credencial de acceso de corta duración (15 minutos) y una credencial de renovación de larga duración (7 días). Mientras la credencial de renovación sea válida, la sesión se renueva automáticamente sin pedir de nuevo alias y contraseña, también tras recargar la página o cerrar y volver a abrir el navegador.
6. Cada renovación sustituye ambas credenciales por otras nuevas; la credencial de renovación usada deja de ser válida.
7. El usuario puede cerrar la sesión en cualquier momento. Al hacerlo, su credencial de renovación queda invalidada en el servidor y vuelve a la pantalla de inicio de sesión.
8. Si la sesión no puede mantenerse (credencial de renovación caducada, invalidada o rechazada), la sesión se cierra y el usuario vuelve a la pantalla de inicio de sesión.

Los usuarios ya existen en el sistema (precargados); esta funcionalidad no los da de alta.

## 3. Requisitos funcionales

- **RF-01**: El sistema debe ofrecer una pantalla de inicio de sesión con un campo de alias, un campo de contraseña (con el contenido oculto) y una acción para enviar el formulario.
- **RF-02**: El sistema debe impedir el envío del formulario mientras el alias o la contraseña estén vacíos, indicando qué campo es obligatorio.
- **RF-03**: El sistema debe validar el alias y la contraseña contra los usuarios registrados y, si son correctos, abrir una sesión emitiendo una credencial de acceso y una credencial de renovación.
- **RF-04**: El sistema debe rechazar las credenciales incorrectas mostrando el mensaje "Alias o contraseña incorrectos", tanto si el alias no existe como si la contraseña no corresponde.
- **RF-05**: El sistema debe impedir el acceso a cualquier pantalla distinta de la de inicio de sesión a un usuario sin sesión válida, llevándole a la pantalla de inicio de sesión.
- **RF-06**: El sistema debe rechazar como no autenticada cualquier operación de la API, salvo el inicio de sesión y la renovación de la sesión, que no presente una credencial de acceso válida y vigente.
- **RF-07**: Tras un inicio de sesión correcto, el sistema debe llevar al usuario a la pantalla protegida a la que intentaba acceder antes de ser enviado al inicio de sesión o, en su defecto, al panel.
- **RF-08**: El sistema debe mantener la sesión al recargar la página o al cerrar y volver a abrir el navegador mientras la credencial de renovación siga vigente.
- **RF-09**: El sistema debe renovar la sesión automáticamente, sin pedir de nuevo alias y contraseña, cuando la credencial de acceso caduque y la credencial de renovación siga vigente.
- **RF-10**: El sistema debe, en cada renovación, emitir una nueva credencial de acceso y una nueva credencial de renovación e invalidar la credencial de renovación utilizada.
- **RF-11**: El sistema debe ofrecer al usuario autenticado una acción para cerrar sesión que invalide su credencial de renovación en el servidor, elimine la sesión en el navegador y le lleve a la pantalla de inicio de sesión.
- **RF-12**: El sistema debe cerrar la sesión y llevar al usuario a la pantalla de inicio de sesión cuando la renovación no sea posible (credencial de renovación caducada, invalidada o rechazada).
- **RF-13**: El sistema debe mostrar el alias del usuario autenticado mientras su sesión esté abierta.
- **RF-14**: El sistema debe llevar al panel a un usuario con sesión válida que acceda a la pantalla de inicio de sesión.

## 4. Reglas de negocio

- **RN-01**: El alias no distingue mayúsculas de minúsculas ("Ana", "ana" y "ANA" identifican al mismo usuario). La contraseña sí las distingue.
- **RN-02**: La credencial de acceso tiene una validez de 15 minutos desde su emisión.
- **RN-03**: La credencial de renovación tiene una validez de 7 días desde su emisión. Como cada renovación emite una nueva, la sesión se mantiene mientras el usuario no pase más de 7 días sin renovarla.
- **RN-04**: Una credencial de renovación es de un solo uso: tras utilizarse en una renovación, o tras cerrar sesión, deja de ser válida.
- **RN-05**: El mensaje ante credenciales incorrectas es siempre el mismo y no revela si el alias existe.
- **RN-06**: No existen roles: cualquier usuario autenticado tiene acceso a toda la aplicación; la única distinción es entre usuario autenticado y no autenticado.

## 5. Criterios de aceptación

- **CA-01 (RF-01)**: Dado un usuario sin sesión, cuando abre la pantalla de inicio de sesión, entonces ve un campo de alias, un campo de contraseña cuyo contenido se muestra oculto y un botón para iniciar sesión.
- **CA-02 (RF-02)**: Dado el formulario de inicio de sesión con el alias o la contraseña vacíos, cuando el usuario intenta enviarlo, entonces el formulario no se envía y se indica que el campo vacío es obligatorio.
- **CA-03 (RF-03, RF-07)**: Dado un usuario registrado con alias "ana" sin sesión que abre directamente la pantalla de inicio de sesión, cuando introduce "ana" y su contraseña correcta, entonces se abre su sesión y se le muestra el panel.
- **CA-04 (RF-03, RN-01)**: Dado un usuario registrado con alias "ana", cuando inicia sesión con el alias "ANA" y su contraseña correcta, entonces se abre su sesión.
- **CA-05 (RF-04, RN-05)**: Dado un usuario registrado con alias "ana", cuando inicia sesión con "ana" y una contraseña incorrecta, entonces permanece en la pantalla de inicio de sesión, ve el mensaje "Alias o contraseña incorrectos" y no se abre ninguna sesión.
- **CA-06 (RF-04, RN-05)**: Dado que no existe ningún usuario con alias "noexiste", cuando se intenta iniciar sesión con ese alias y cualquier contraseña, entonces se muestra exactamente el mismo mensaje "Alias o contraseña incorrectos" que en CA-05.
- **CA-07 (RN-01)**: Dado un usuario registrado cuya contraseña es "Secreta1", cuando inicia sesión con su alias y la contraseña "secreta1", entonces se muestra el mensaje "Alias o contraseña incorrectos".
- **CA-08 (RF-05, RF-07)**: Dado un usuario sin sesión, cuando intenta acceder a la pantalla de movimientos, entonces se le lleva a la pantalla de inicio de sesión y, tras iniciar sesión correctamente, se le muestra la pantalla de movimientos.
- **CA-09 (RF-06)**: Dada una operación de la API distinta del inicio de sesión y de la renovación, cuando se invoca sin credencial de acceso, con una credencial manipulada o con una credencial caducada, entonces se rechaza como no autenticada y no se devuelve ningún dato.
- **CA-10 (RF-08)**: Dado un usuario con sesión abierta y credencial de renovación vigente, cuando recarga la página o cierra y vuelve a abrir el navegador, entonces sigue autenticado sin introducir de nuevo alias y contraseña.
- **CA-11 (RF-09, RN-02)**: Dado un usuario cuya credencial de acceso se emitió hace más de 15 minutos y cuya credencial de renovación sigue vigente, cuando realiza una acción en la aplicación, entonces la acción se completa sin que se le pidan de nuevo alias y contraseña.
- **CA-12 (RF-10, RN-04)**: Dada una credencial de renovación que ya se ha utilizado en una renovación, cuando se intenta renovar de nuevo con ella, entonces la renovación se rechaza.
- **CA-13 (RF-12, RN-03)**: Dado un usuario cuya credencial de renovación se emitió hace más de 7 días sin haberse renovado, cuando abre la aplicación o realiza una acción, entonces se cierra su sesión y se le lleva a la pantalla de inicio de sesión.
- **CA-14 (RF-11, RN-04)**: Dado un usuario autenticado, cuando cierra sesión, entonces se le lleva a la pantalla de inicio de sesión, al intentar acceder al panel se le vuelve a llevar al inicio de sesión, y la credencial de renovación que tenía es rechazada si se intenta usar para renovar.
- **CA-15 (RF-13)**: Dado un usuario que ha iniciado sesión con el alias "ana", cuando navega por cualquier pantalla de la aplicación, entonces ve su alias "ana" mostrado en la aplicación.
- **CA-16 (RF-14)**: Dado un usuario con sesión válida, cuando accede a la pantalla de inicio de sesión, entonces se le lleva al panel.
- **CA-17 (RN-06)**: Dados dos usuarios registrados cualesquiera, cuando cada uno inicia sesión, entonces ambos tienen acceso a las mismas pantallas de la aplicación.

## 6. Restricciones y consideraciones

- La credencial de acceso es un token JWT emitido por el backend; el frontend lo obtiene en el inicio de sesión y lo presenta en cada operación (restricción impuesta por las constituciones de ambos proyectos).
- El frontend nunca envía el identificador del usuario actual en las operaciones: el backend lo obtiene siempre de la credencial de acceso.
- Las contraseñas se almacenan cifradas y nunca se devuelven ni se muestran.
- La documentación interactiva de la API queda accesible sin sesión, igual que el inicio de sesión y la renovación.

## 7. Fuera de alcance

- Alta o registro de usuarios (los usuarios están precargados).
- Recuperación o cambio de contraseña.
- Bloqueo de la cuenta tras intentos fallidos de inicio de sesión.
- Roles y permisos diferenciados.
- Gestión de sesiones activas en varios dispositivos (listar o cerrar sesiones de otros dispositivos).
- Autenticación de doble factor o mediante proveedores externos.
