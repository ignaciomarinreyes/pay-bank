# Pagos

> Ámbito: Global (proyectos afectados: `pay-bank-backend`, `pay-bank-frontend`)

## 1. Objetivo

Permitir que el usuario autenticado envíe dinero desde su cuenta a la cuenta de otro usuario, indicando el destinatario, el importe y, si quiere, un concepto. Es la operación que genera los movimientos que se consultan en `002-movimientos`.

## 2. Descripción funcional

1. El usuario autenticado accede a la pantalla "Nuevo pago" desde la navegación de la aplicación.
2. La pantalla muestra su saldo disponible y un formulario con tres campos: destinatario (alias del otro usuario), importe en euros y concepto (opcional).
3. Al pulsar "Pagar", si los datos son válidos, el pago se realiza sin paso de confirmación intermedio.
4. Si el pago se realiza, se descuenta el importe del saldo del usuario, se suma al saldo del destinatario y se registran un cargo en la cuenta del usuario y un abono en la del destinatario. El usuario es llevado a la pantalla "Movimientos", donde el cargo aparece el primero.
5. Si el pago no puede realizarse (datos inválidos, destinatario inexistente, pago a sí mismo, saldo insuficiente o error), el usuario permanece en el formulario con los datos introducidos y ve el motivo.

## 3. Requisitos funcionales

- **RF-01**: El sistema debe ofrecer al usuario autenticado una entrada "Nuevo pago" en la navegación que lleve al formulario de pago.
- **RF-02**: El sistema debe mostrar en el formulario el saldo disponible del usuario.
- **RF-03**: El sistema debe ofrecer un formulario con los campos destinatario, importe y concepto, y una acción "Pagar".
- **RF-04**: El sistema debe impedir el envío mientras el destinatario o el importe estén vacíos, indicando qué campo es obligatorio.
- **RF-05**: El sistema debe rechazar un importe que no sea un número mayor que 0 con un máximo de 2 decimales.
- **RF-06**: El sistema debe rechazar un concepto de más de 140 caracteres.
- **RF-07**: El sistema debe rechazar el pago si el destinatario no corresponde a ningún usuario registrado.
- **RF-08**: El sistema debe rechazar el pago si el destinatario es el propio usuario.
- **RF-09**: El sistema debe rechazar el pago si el importe es mayor que el saldo disponible del usuario.
- **RF-10**: El sistema debe, al realizar el pago, restar el importe del saldo del usuario, sumarlo al saldo del destinatario y registrar un cargo en la cuenta del usuario y un abono en la del destinatario, con el importe, el concepto, la fecha y hora del pago y el saldo resultante de cada cuenta.
- **RF-11**: El sistema debe llevar al usuario a la pantalla "Movimientos" tras un pago realizado.
- **RF-12**: El sistema debe mantener los datos introducidos y mostrar el motivo cuando el pago no se realice.
- **RF-13**: El sistema debe impedir que una misma pulsación de "Pagar" realice más de un pago, deshabilitando la acción mientras el pago está en curso.

## 4. Reglas de negocio

- **RN-01**: El destinatario se identifica por su alias, sin distinguir mayúsculas de minúsculas (coherente con `001-inicio-sesion`).
- **RN-02**: El importe se expresa en euros, debe ser mayor que 0 (mínimo 0,01 €) y tener como máximo 2 decimales. Se admite la coma o el punto como separador decimal y no se admite separador de miles.
- **RN-03**: No hay importe máximo por pago más allá del saldo disponible: el saldo del usuario nunca puede quedar negativo. Un pago que deja el saldo exactamente en 0,00 € está permitido.
- **RN-04**: El concepto es opcional y tiene como máximo 140 caracteres. Un concepto vacío o formado solo por espacios se trata como sin concepto.
- **RN-05**: Un pago se realiza completo o no se realiza: no puede quedar un saldo actualizado sin sus movimientos ni un cargo sin su abono.
- **RN-06**: Si se realizan a la vez varios pagos desde la misma cuenta, ninguno puede dejar el saldo negativo.
- **RN-07**: El usuario que paga es siempre el usuario autenticado; no puede indicar otra cuenta de origen.
- **RN-08**: Los mensajes de error son:
  - Destinatario vacío: "El destinatario es obligatorio".
  - Importe vacío: "El importe es obligatorio".
  - Importe no válido: "Introduce un importe mayor que 0 con un máximo de 2 decimales".
  - Concepto demasiado largo: "El concepto no puede superar los 140 caracteres".
  - Destinatario inexistente: "No existe ningún usuario con ese alias".
  - Pago a uno mismo: "No puedes hacerte un pago a ti mismo".
  - Saldo insuficiente: "Saldo insuficiente".
  - Cualquier otro fallo: "No se ha podido realizar el pago. Inténtalo de nuevo más tarde."

## 5. Criterios de aceptación

- **CA-01 (RF-01, RF-03)**: Dado un usuario autenticado, cuando pulsa "Nuevo pago" en la navegación, entonces ve el formulario con los campos destinatario, importe y concepto y el botón "Pagar".
- **CA-02 (RF-02)**: Dado que "ana" tiene un saldo de 1.015,00 €, cuando abre "Nuevo pago", entonces ve "Saldo disponible: 1.015,00 €".
- **CA-03 (RF-04, RN-08)**: Dado el formulario con el destinatario vacío o el importe vacío, cuando el usuario pulsa "Pagar", entonces no se realiza ningún pago y se muestra "El destinatario es obligatorio" o "El importe es obligatorio" según el campo vacío.
- **CA-04 (RF-05, RN-02, RN-08)**: Dado el formulario, cuando el usuario introduce como importe "0", "-5", "10,999" o "abc" y pulsa "Pagar", entonces no se realiza ningún pago y se muestra "Introduce un importe mayor que 0 con un máximo de 2 decimales".
- **CA-05 (RN-02)**: Dado que "ana" tiene saldo suficiente, cuando paga a "maria" un importe escrito como "12,50" y otro escrito como "12.50", entonces ambos pagos se realizan por 12,50 €.
- **CA-06 (RF-06, RN-04, RN-08)**: Dado el formulario, cuando el usuario introduce un concepto de 141 caracteres y pulsa "Pagar", entonces no se realiza ningún pago y se muestra "El concepto no puede superar los 140 caracteres"; con un concepto de 140 caracteres el pago se realiza.
- **CA-07 (RF-07, RN-08)**: Dado que no existe el usuario "noexiste", cuando "ana" intenta pagarle 10 €, entonces no se realiza ningún pago, su saldo no cambia y ve "No existe ningún usuario con ese alias".
- **CA-08 (RF-08, RN-01, RN-08)**: Dado el usuario "ana", cuando intenta pagar 10 € al destinatario "ANA", entonces no se realiza ningún pago y ve "No puedes hacerte un pago a ti mismo".
- **CA-09 (RF-09, RN-03, RN-08)**: Dado que "ana" tiene un saldo de 100,00 €, cuando intenta pagar 100,01 € a "maria", entonces no se realiza ningún pago, su saldo sigue en 100,00 € y ve "Saldo insuficiente".
- **CA-10 (RN-03)**: Dado que "ana" tiene un saldo de 100,00 €, cuando paga 100,00 € a "maria", entonces el pago se realiza y su saldo queda en 0,00 €.
- **CA-11 (RF-10, RF-11, RN-01)**: Dado que "ana" tiene 1.000,00 € y "maria" 500,00 €, cuando "ana" paga 250,00 € a "Maria" con concepto "Cena", entonces el saldo de "ana" pasa a 750,00 €, el de "maria" a 750,00 €, "ana" es llevada a "Movimientos" y ve primero un cargo de "-250,00 €" a "maria" con concepto "Cena" y saldo "750,00 €"; y "maria" ve un abono de "+250,00 €" de "ana" con saldo "750,00 €".
- **CA-12 (RN-04)**: Dado el formulario, cuando el usuario paga con el concepto vacío o solo con espacios, entonces el pago se realiza y en sus movimientos el concepto aparece como "—".
- **CA-13 (RF-12)**: Dado un pago rechazado por saldo insuficiente, cuando se muestra el error, entonces el destinatario, el importe y el concepto introducidos siguen en el formulario.
- **CA-14 (RF-13)**: Dado un pago en curso, cuando el usuario vuelve a pulsar "Pagar" antes de recibir la respuesta, entonces el botón está deshabilitado y solo se realiza un pago.
- **CA-15 (RN-05)**: Dado un pago que falla después de haber empezado a procesarse, cuando termina, entonces ni el saldo del usuario ni el del destinatario han cambiado y no existe ningún cargo ni abono de ese pago.
- **CA-16 (RN-06)**: Dado que "ana" tiene 100,00 €, cuando se envían a la vez dos pagos de 60,00 € desde su cuenta, entonces solo uno se realiza, el otro se rechaza por saldo insuficiente y su saldo queda en 40,00 €.
- **CA-17 (RN-07)**: Dada una petición de pago sin usuario autenticado, cuando se realiza, entonces se rechaza y no se realiza ningún pago.
- **CA-18 (RN-08)**: Dado que el pago falla por un error no previsto, cuando el usuario pulsa "Pagar", entonces ve "No se ha podido realizar el pago. Inténtalo de nuevo más tarde." y los datos siguen en el formulario.

## 6. Restricciones y consideraciones

- Requiere un usuario autenticado según `001-inicio-sesion`.
- Cada pago genera los movimientos que se consultan en `002-movimientos`, respetando su definición de cargo, abono, contraparte y saldo tras el movimiento, y su formato de importes.
- El usuario que paga y su cuenta se obtienen siempre de su sesión, nunca de un dato que indique el propio usuario.
- Las validaciones se aplican siempre en el servidor, aunque la pantalla las compruebe también antes de enviar.

## 7. Fuera de alcance

- Listados de pagos realizados y pagos recibidos (funcionalidad propia).
- Búsqueda o sugerencia de destinatarios mientras se escribe.
- Paso de confirmación antes de realizar el pago.
- Mensaje de confirmación tras el pago (el resultado se ve en "Movimientos").
- Límites de importe por pago o diarios, comisiones y divisas distintas del euro.
- Anular, devolver o programar pagos.
