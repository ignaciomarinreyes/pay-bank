# Movimientos

> Ámbito: Global (proyectos afectados: `pay-bank-backend`, `pay-bank-frontend`)

## 1. Objetivo

Permitir que el usuario autenticado consulte el histórico de movimientos de su cuenta (cargos y abonos) para saber qué dinero ha salido y entrado, con quién, por qué concepto y cómo ha evolucionado su saldo.

## 2. Descripción funcional

1. El usuario autenticado accede a la pantalla "Movimientos" desde la navegación de la aplicación.
2. La pantalla muestra los movimientos de su propia cuenta, del más reciente al más antiguo, en páginas de 20 movimientos.
3. De cada movimiento se muestra: fecha y hora, tipo (Cargo o Abono), importe con signo, alias de la contraparte, concepto y saldo de la cuenta tras el movimiento.
4. El usuario puede pasar a la página siguiente o a la anterior y ve en qué página está y cuántas hay en total.
5. Si la cuenta no tiene movimientos, se muestra el mensaje "No hay movimientos".
6. Si los movimientos no se pueden cargar, se muestra un mensaje de error.

Los movimientos se generan al realizar pagos entre usuarios: cada pago produce un cargo en la cuenta de quien paga y un abono en la de quien recibe. Esta funcionalidad solo los consulta.

## 3. Requisitos funcionales

- **RF-01**: El sistema debe ofrecer al usuario autenticado una entrada "Movimientos" en la navegación que lleve al listado de movimientos de su cuenta.
- **RF-02**: El sistema debe mostrar únicamente los movimientos de la cuenta del usuario autenticado.
- **RF-03**: El sistema debe mostrar los movimientos ordenados del más reciente al más antiguo.
- **RF-04**: El sistema debe mostrar los movimientos en páginas de 20 elementos.
- **RF-05**: El sistema debe mostrar de cada movimiento su fecha y hora, su tipo (Cargo o Abono), su importe con signo, el alias de la contraparte, el concepto y el saldo de la cuenta tras el movimiento.
- **RF-06**: El sistema debe permitir ir a la página siguiente y a la página anterior, deshabilitando la acción que no sea posible (anterior en la primera página, siguiente en la última).
- **RF-07**: El sistema debe indicar la página actual y el número total de páginas.
- **RF-08**: El sistema debe mostrar el mensaje "No hay movimientos", sin controles de paginación, cuando la cuenta no tenga movimientos.
- **RF-09**: El sistema debe mostrar el mensaje "No se han podido cargar los movimientos. Inténtalo de nuevo más tarde." cuando la consulta falle.

## 4. Reglas de negocio

- **RN-01**: Un cargo es dinero que sale de la cuenta (el usuario ha pagado) y se muestra con importe negativo; un abono es dinero que entra (el usuario ha recibido un pago) y se muestra con importe positivo.
- **RN-02**: La contraparte de un cargo es el destinatario del pago; la de un abono, quien lo realizó.
- **RN-03**: Los importes se muestran en euros, con dos decimales, separador de miles "." y separador decimal ",", con signo y el símbolo "€" detrás (p. ej. "-1.234,56 €", "+50,00 €").
- **RN-04**: La fecha y hora se muestran en formato "dd/mm/aaaa hh:mm" (24 h) en la hora local del usuario.
- **RN-05**: Si un movimiento no tiene concepto, se muestra "—" en su lugar.
- **RN-06**: Si dos movimientos tienen la misma fecha y hora, aparece primero el que se registró después.
- **RN-07**: El saldo tras el movimiento es el saldo de la cuenta inmediatamente después de aplicar ese movimiento.

## 5. Criterios de aceptación

- **CA-01 (RF-01)**: Dado un usuario autenticado, cuando pulsa "Movimientos" en la navegación, entonces ve el listado de movimientos de su cuenta.
- **CA-02 (RF-02)**: Dado que "maria" pagó 20,00 € a "luis" y "ana" no ha participado en ese pago, cuando "ana" consulta sus movimientos, entonces ese pago no aparece en su listado.
- **CA-03 (RF-03, RN-06)**: Dada una cuenta con movimientos del 10/11/2026 10:00, 12/11/2026 09:30 y 11/11/2026 18:00, cuando el usuario consulta sus movimientos, entonces aparecen en el orden 12/11/2026 09:30, 11/11/2026 18:00, 10/11/2026 10:00.
- **CA-04 (RF-04, RF-07)**: Dada una cuenta con 45 movimientos, cuando el usuario abre el listado, entonces ve los 20 más recientes y la indicación "Página 1 de 3".
- **CA-05 (RF-06, RF-07)**: Dada una cuenta con 45 movimientos en la página 1, cuando el usuario pulsa "Siguiente" dos veces, entonces ve en la página 3 los 5 movimientos más antiguos, la indicación "Página 3 de 3" y la acción "Siguiente" deshabilitada.
- **CA-06 (RF-06)**: Dada una cuenta con 45 movimientos en la página 1, cuando el usuario ve el listado, entonces la acción "Anterior" está deshabilitada; y cuando está en la página 2 y pulsa "Anterior", entonces vuelve a la página 1.
- **CA-07 (RF-05, RN-01, RN-02, RN-03)**: Dado que "ana" pagó 1.234,56 € a "maria" con concepto "Alquiler", cuando "ana" consulta sus movimientos, entonces ve un movimiento de tipo "Cargo", importe "-1.234,56 €", contraparte "maria" y concepto "Alquiler".
- **CA-08 (RF-05, RN-01, RN-02, RN-03)**: Dado que "maria" pagó 50 € a "ana", cuando "ana" consulta sus movimientos, entonces ve un movimiento de tipo "Abono", importe "+50,00 €" y contraparte "maria".
- **CA-09 (RF-05, RN-07)**: Dada una cuenta con saldo de 100,00 € que recibe un abono de 50,00 € y después realiza un cargo de 30,00 €, cuando el usuario consulta sus movimientos, entonces el abono muestra saldo "150,00 €" y el cargo muestra saldo "120,00 €".
- **CA-10 (RF-05, RN-04)**: Dado un movimiento producido el 18/11/2026 a las 14:05 hora local del usuario, cuando consulta sus movimientos, entonces la fecha se muestra como "18/11/2026 14:05".
- **CA-11 (RN-05)**: Dado un movimiento cuyo pago no tiene concepto, cuando el usuario consulta sus movimientos, entonces en el concepto se muestra "—".
- **CA-12 (RF-08)**: Dado un usuario cuya cuenta no tiene movimientos, cuando abre el listado, entonces ve el mensaje "No hay movimientos" y no ve controles de paginación.
- **CA-13 (RF-09)**: Dado que la consulta de movimientos falla, cuando el usuario abre el listado, entonces ve el mensaje "No se han podido cargar los movimientos. Inténtalo de nuevo más tarde.".
- **CA-14 (RF-02)**: Dada una consulta de movimientos sin un usuario autenticado, cuando se realiza, entonces se rechaza y no se devuelve ningún movimiento.

## 6. Restricciones y consideraciones

- Requiere un usuario autenticado según la spec `001-inicio-sesion`.
- El usuario cuyos movimientos se consultan se obtiene siempre de su sesión, nunca de un dato que indique el propio usuario.
- La paginación se hace en el servidor: nunca se obtienen todos los movimientos de una vez.
- Los movimientos se crean al realizar pagos, que se especificarán en su propia spec; esta funcionalidad depende de que exista la cuenta del usuario y no modifica movimientos ni saldos.

## 7. Fuera de alcance

- Creación de movimientos y realización de pagos.
- Filtros (por tipo, por fechas) y búsqueda por texto.
- Elegir el número de movimientos por página o saltar a una página concreta.
- Detalle de un movimiento en una pantalla aparte.
- Exportar movimientos (PDF, CSV…).
- Consulta del saldo total (pertenece al panel).
