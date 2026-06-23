# Relaciones principales de datos

Este documento resume las relaciones entre entidades persistentes del sistema. La información se basa en las relaciones JPA y en el uso que los servicios hacen de ellas.

## 1. Relación de seguridad y perfiles

```text
rol 1---N usuario_sistema
rol N---N permiso
usuario_sistema 1---1 perfil real activo
usuario_sistema 1---N usuario_cambio_perfil_historial
```

El perfil real activo se determina por:

```text
UsuarioSistema.tipoPerfilActual
```

y se resuelve mediante estrategias por tipo de perfil.

Relaciones por perfil:

```text
usuario_sistema -> administrativo
usuario_sistema -> asesor
usuario_sistema -> monitor
usuario_sistema -> estudiante
usuario_sistema -> conciliador
```

## 2. Relación de asesor, estudiante y consulta

```text
asesor 1---N estudiante
asesor 1---N consulta
monitor 1---N consulta
estudiante 1---N consulta
```

Uso funcional:

- el asesor puede operar recursos dentro de su alcance;
- el estudiante participa en consultas asignadas y seguimientos visibles;
- el monitor puede estar asociado a consultas;
- asesor, estudiante y monitor no se desactivan si tienen consultas operativas vivas.

## 3. Relación de consulta con catálogos

```text
consulta N---1 sede
consulta N---1 area
consulta N---1 tema
consulta N---1 tipo
consulta N---1 persona principal
```

Jerarquía validada por servicios:

```text
area -> tema -> tipo
```

Reglas:

- el tema debe pertenecer al área;
- el tipo debe pertenecer al tema;
- el asesor debe pertenecer al área;
- el estudiante debe ser coherente con el asesor y el área.

## 4. Relación de consulta con personas relacionadas

```text
consulta N---N persona mediante consulta_parte
consulta N---N persona mediante consulta_contraparte
```

La consulta tiene:

- una persona principal obligatoria;
- partes adicionales opcionales;
- contrapartes opcionales.

Los servicios validan que una persona no se repita indebidamente entre estos grupos.

## 5. Relación de consulta con módulos operativos

```text
consulta 1---N proceso
consulta 1---N seguimiento
consulta 1---N conciliacion
```

Estas relaciones son centrales para el cierre de consulta.

Una consulta con actividad asociada conserva protegidos sus datos estructurales. Los servicios detectan actividad a partir de:

```text
proceso activo
seguimiento activo
conciliacion activa
```

## 6. Relación de proceso

```text
proceso N---1 consulta
proceso N---1 departamento
proceso N---1 organo_control
proceso N---1 especialidad
organo_control 1---N especialidad
```

El radicado pertenece al proceso y conserva unicidad cuando existe.

## 7. Relación de seguimiento

```text
seguimiento N---1 consulta
seguimiento N---1 categoria_seguimiento
seguimiento N---1 usuario_sistema como autor
seguimiento 1---N seguimiento_respuesta
seguimiento 1---N seguimiento_notificacion
```

Respuesta:

```text
seguimiento_respuesta N---1 seguimiento
seguimiento_respuesta N---1 estudiante
seguimiento_respuesta N---1 usuario_sistema como revisor
```

Notificaciones:

```text
seguimiento_notificacion N---1 seguimiento
```

## 8. Relación de conciliación

```text
conciliacion N---1 consulta
conciliacion N---1 estudiante
conciliacion N---1 conciliador
conciliacion N---1 estado_conciliacion
conciliacion N---1 usuario_sistema como solicitante
conciliacion 1---1 reunion_conciliacion
conciliacion 1---N reunion_conciliacion_historial
conciliacion 1---N reunion_conciliacion_notificacion
```

La conciliación toma el contexto principal desde la consulta.

## 9. Relación de reunión de conciliación

```text
reunion_conciliacion 1---1 conciliacion
reunion_conciliacion N---1 sede
reunion_conciliacion_historial N---1 conciliacion
reunion_conciliacion_historial N---1 sede anterior
reunion_conciliacion_historial N---1 sede nueva
reunion_conciliacion_historial N---1 usuario_sistema
reunion_conciliacion_notificacion N---1 conciliacion
```

## 10. Relación de persona con catálogos

```text
persona N---1 tipo_persona
persona N---1 tipo_documento
persona N---1 nacionalidad
persona N---1 condicion
persona N---1 municipio
persona N---1 barrio
persona N---1 ocupacion
persona N---1 empresa
```

## 11. Relación de auditoría

```text
audit_logs registra acción, entidad lógica, id afectado y usuario textual
```

La auditoría se produce mediante aspecto asociado a métodos anotados con `@Auditable`.

## 12. Relación de estadísticas

Las estadísticas se calculan desde entidades operativas, principalmente:

```text
consulta
proceso
seguimiento
conciliacion
perfiles internos
catálogos jurídicos
```

Los reportes estadísticos se generan dinámicamente desde servicios de consulta y generación PDF a partir de las entidades operativas relacionadas.
