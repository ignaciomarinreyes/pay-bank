# Plan de implementación: [Nombre de la funcionalidad]

> Especificación: `<Identificador-nombreEspecificacion>` · [spec.md](<ruta relativa a la spec>)
> Ámbito de la spec: Global | Proyecto · Proyecto de este plan: `<nombre-proyecto>`
> Otros planes de esta spec: `<proyecto>/docs/<Identificador-nombreEspecificacion>/plan.md` (solo spec global)

<!-- Ruta relativa a la spec: `spec.md` si es de proyecto; `../../../docs/<Identificador-nombreEspecificacion>/spec.md` si es global. -->

## Alcance en este proyecto

<!-- Solo spec global: RF/RN/CA de la spec que implementa este proyecto. Vacío en spec de proyecto. -->

## 1. Resumen de la estrategia

<!-- Enfoque general de la implementación en pocas líneas: qué se va a hacer y en qué orden lógico. -->

## 2. Contexto técnico

<!-- Estado actual relevante del código: stack, módulos existentes, patrones y convenciones que se seguirán, componentes que se reutilizan. -->

## 3. Decisiones técnicas

- **DT-01**: [Decisión]
  - Alternativas consideradas: ...
  - Motivo: ...

## 4. Componentes afectados

| Componente | Proyecto | Tipo de cambio (nuevo / modificado / eliminado) | Relacionado con |
|---|---|---|---|
| ... | ... | ... | RF-01, DT-01 |

## 5. Cambios necesarios

- **CB-01**: [Descripción del cambio] — Cubre: RF-01, RN-01
- **CB-02**: [Descripción del cambio] — Cubre: RF-02

### Modelo de datos y migraciones

<!-- Cambios en entidades, esquemas o datos existentes. Vacío si no aplica. -->

### Contratos e integraciones

<!-- Interfaces, APIs, eventos o integraciones con otros sistemas que se crean o modifican. Vacío si no aplica. -->

### Configuración y seguridad

<!-- Parámetros, permisos, secretos, feature flags. Vacío si no aplica. -->

## 6. Estrategia de verificación

| Criterio de aceptación | Cómo se verifica (tipo de prueba / nivel) | Cambios implicados |
|---|---|---|
| CA-01 | ... | CB-01 |

## 7. Riesgos y mitigaciones

<!-- Riesgos técnicos, impacto en otras funcionalidades, compatibilidad hacia atrás, rendimiento. Vacío si no aplica. -->

## 8. Elementos de la spec no cubiertos

<!-- Requisitos que este plan no cubre y por qué. En spec global, indica qué proyecto los cubre (p. ej. RF-04 → frontend). Vacío si todo está cubierto. -->
