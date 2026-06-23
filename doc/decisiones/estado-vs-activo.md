# Decisión técnica - Estado funcional vs activo lógico

## Contexto

El sistema maneja recursos con ciclos de vida operativos y también necesita conservar información histórica. Algunos recursos tienen estados funcionales, como consulta, proceso, seguimiento, respuesta de seguimiento y conciliación. Otros recursos usan `activo` para indicar si están disponibles o desactivados.

## Decisión

El sistema separa:

```text
estado funcional
activo lógico
```

El estado funcional representa el ciclo de negocio. El activo lógico representa disponibilidad operativa o desactivación sin eliminación física.

## Definiciones

### Estado funcional

Representa el momento del flujo de negocio.

Ejemplos:

- `Consulta.estado`;
- `Proceso.estado`;
- `Seguimiento.estado`;
- `SeguimientoRespuesta.estado`;
- `Conciliacion.estado` mediante `EstadoConciliacion`.

### Activo lógico

Representa si un registro está disponible para operación o fue desactivado.

Ejemplos:

- `activo` en perfiles;
- `activo` en catálogos;
- `activo` en procesos;
- `activo` en seguimientos;
- `activo` en respuestas de seguimiento;
- `activo` en conciliaciones;
- `activo` en notificaciones.

## Justificación

Separar estos conceptos evita mezclar decisiones funcionales con borrado lógico.

Ejemplo correcto:

```text
La consulta se cierra con estado CERRADO.
La consulta se archiva con estado ARCHIVADO.
```

Ejemplo correcto:

```text
Un catálogo se desactiva con activo=false para no ofrecerse en nuevas operaciones.
```

## Aplicación en consulta

Estados:

```text
PENDIENTE
ACTIVO
EN_PROCESO
URGENTE
CERRADO
ARCHIVADO
```

Criterios:

- `PENDIENTE` es el estado inicial;
- `CERRADO` representa cierre operativo;
- `ARCHIVADO` representa archivo histórico;
- cerrar exige resultado y ausencia de pendientes;
- archivar solo aplica sobre consultas cerradas;
- desarchivar vuelve a `CERRADO`, no reabre operación.

## Aplicación en proceso

Estados:

```text
PENDIENTE
SENTENCIA_FAVORABLE
SENTENCIA_DESFAVORABLE
DESISTIMIENTO
RECHAZO
PRESCRIPCION
```

Criterios:

- `PENDIENTE` indica ausencia de resultado final;
- cualquier estado distinto de `PENDIENTE` es final según `EstadoProceso.esFinal()`;
- el radicado puede estar vacío en pendiente;
- los estados finales exigen radicado;
- `activo=false` representa eliminación lógica, no resultado procesal.

## Aplicación en seguimiento

Estados:

```text
PENDIENTE
COMPLETADO
CANCELADO
```

Criterios:

- `PENDIENTE` bloquea cierre de consulta;
- `COMPLETADO` o `CANCELADO` dejan de representar pendiente operativo;
- `activo=false` conserva trazabilidad sin operar el seguimiento.

## Aplicación en respuesta de seguimiento

Estados:

```text
PENDIENTE
APROBADA
RECHAZADA
```

Criterios:

- `PENDIENTE` bloquea cierre de consulta;
- `APROBADA` y `RECHAZADA` representan decisión de revisión;
- `RECHAZADA` requiere observación.

## Aplicación en conciliación

Conciliación usa catálogo persistente:

```text
estado_conciliacion
```

Códigos:

```text
EN_ESPERA
ESPERANDO_REUNION
REUNION_PROGRAMADA
COMPLETO_CONCILIADO
COMPLETO_NO_CONCILIADO
```

Criterios:

- estados pendientes bloquean cierre de consulta;
- estados finales se aplican con acta;
- `activo=false` representa desactivación lógica y cancela notificaciones pendientes;
- el estado funcional se conserva como parte del historial.

## Impacto en frontend

El frontend debe tratar `estado` y `activo` como campos distintos:

- `estado` determina flujo, acciones disponibles y etiquetas funcionales;
- `activo` determina disponibilidad o visibilidad operativa;
- las operaciones de ciclo de vida usan endpoints específicos.

## Criterios de mantenimiento

Cuando se agregue un nuevo estado o una nueva regla de activo lógico, revisar:

- entidades JPA;
- enums o catálogos;
- servicios de comando;
- validadores;
- filtros de listados;
- reglas de cierre;
- documentación de API y reglas.
