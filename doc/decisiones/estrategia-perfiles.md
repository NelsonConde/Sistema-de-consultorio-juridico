# Decisión técnica - Estrategias de perfiles

## Contexto

El sistema permite que un `UsuarioSistema` esté asociado a distintos tipos de perfil real: asesor, estudiante, monitor, administrativo o conciliador.

El cambio y resolución de perfil requieren lógica diferente según el tipo de perfil. Para evitar servicios con condicionales extensos y para mantener el sistema extensible, se usa el patrón Strategy.

## Decisión

El sistema usa estrategias para tres responsabilidades relacionadas con perfiles:

1. crear o actualizar el perfil destino en un cambio de perfil;
2. desactivar el perfil anterior;
3. resolver el perfil activo del usuario autenticado.

## Estrategia de cambio de perfil

Interfaz:

```text
PerfilCambioHandler
```

Registry:

```text
PerfilCambioHandlerRegistry
```

Handlers concretos:

```text
CambiarAAsesorHandler
CambiarAEstudianteHandler
CambiarAMonitorHandler
CambiarAAdministrativoHandler
CambiarAConciliadorHandler
```

Uso:

- `UsuarioCambioPerfilService` recibe la solicitud de cambio;
- resuelve el handler según el tipo de perfil destino;
- delega la creación o actualización del perfil correspondiente;
- registra historial de cambio.

## Estrategia de estado de perfil anterior

Interfaz:

```text
PerfilEstadoHandler
```

Registry:

```text
PerfilEstadoHandlerRegistry
```

Handlers concretos:

```text
AsesorPerfilEstadoHandler
EstudiantePerfilEstadoHandler
MonitorPerfilEstadoHandler
AdministrativoPerfilEstadoHandler
ConciliadorPerfilEstadoHandler
```

Uso:

- `PerfilEstadoService` no decide con `switch`;
- delega al handler registrado para el tipo de perfil actual;
- los handlers de asesor, estudiante y monitor validan consultas operativas antes de desactivar;
- el cambio de perfil respeta las reglas de integridad operativa.

## Estrategia de resolución de perfil activo

Interfaz:

```text
PerfilUsuarioActivoResolver
```

Registry:

```text
PerfilUsuarioActivoResolverRegistry
```

Resolvers concretos:

```text
AsesorPerfilUsuarioActivoResolver
EstudiantePerfilUsuarioActivoResolver
MonitorPerfilUsuarioActivoResolver
AdministrativoPerfilUsuarioActivoResolver
ConciliadorPerfilUsuarioActivoResolver
```

Uso:

- `PerfilUsuarioResolverService` obtiene `UsuarioSistema.tipoPerfilActual`;
- solicita al registry el resolver correspondiente;
- el resolver consulta el repositorio del perfil específico;
- si el perfil no existe o está inactivo, se lanza regla de negocio.

## Justificación

El patrón Strategy permite:

- cumplir principio abierto/cerrado;
- evitar condicionales por tipo de perfil en servicios orquestadores;
- encapsular reglas específicas por perfil;
- facilitar agregar un nuevo tipo de perfil si el dominio lo requiere;
- mantener pruebas unitarias pequeñas y enfocadas.

## Relación con UsuarioSistema

El usuario del sistema conserva:

```text
tipo_perfil_actual
rol_id
activo
```

El perfil real conserva datos funcionales y `UsuarioSistema.activo` conserva exclusivamente el estado de habilitación de la cuenta. Las operaciones directas de los cinco perfiles no sincronizan ese campo, ni modifican implícitamente `tipo_perfil_actual`, `rol_id`, permisos o historial.

La política se aplica mediante `PerfilOperacionEstadoValidator`. Para un perfil vinculado, los command services obtienen `usuario_sistema_id`, adquieren un bloqueo `PESSIMISTIC_WRITE` sobre la cuenta, recargan el perfil y validan antes de persistir. Activar o desactivar un perfil histórico y desactivar el perfil actual se rechazan con `CONFLICTO_OPERACION_PERFIL`; activar el actual exige que no exista otro perfil activo.

La suspensión o habilitación de cuenta y el cambio explícito de perfil se serializan sobre la misma fila de `UsuarioSistema`. El orden aplicable es: continuidad administrativa cuando corresponde, bloqueo pesimista de la cuenta, lectura y validación, modificación y persistencia. El cambio hacia `ADMINISTRATIVO` adquiere el bloqueo administrativo aunque no reduzca la capacidad administrativa, con el fin de no invertir el orden frente a una suspensión concurrente.

La decisión anterior no implica que todos los escritores de `UsuarioSistema` estén cubiertos. Queda como riesgo técnico pendiente revisar `AuthService` y `PasswordResetService`, que modifican la contraseña, frente a pérdidas de actualización si concurren con una transición de perfil o un cambio de estado de cuenta.

`UsuarioSistemaPerfilEstadoService` permanece en el código, pero dejó de ser parte de los flujos directos de los cinco command services. No debe eliminarse sin revisar por separado sus dependencias y compatibilidad.

## Política de estado implementada

Las reglas comunicadas como aprobadas y su correspondencia técnica son:

| Regla | Implementación |
|---|---|
| Un único perfil operativo activo | El validador consulta los cinco tipos y exige exclusividad por cuenta. |
| Sin cambios implícitos de rol o perfil actual | Las operaciones directas no modifican rol, permisos ni `tipoPerfilActual`. |
| Activación histórica prohibida | Se responde `409` con `CONFLICTO_OPERACION_PERFIL`, sin persistencia. |
| Operaciones históricas aisladas de la cuenta vigente | La desactivación y `DELETE` históricos se rechazan sin cambiar cuenta ni perfil actual. |
| Estado de cuenta independiente | Activar un perfil no habilita la cuenta y suspender la cuenta no altera perfiles. |
| Transiciones mediante cambio explícito | `UsuarioCambioPerfilService` conserva validaciones, transacción e historial. |
| Perfil actual no desactivable directamente | `PATCH /activo?activo=false` y `DELETE` se rechazan; se usa suspensión de cuenta o cambio explícito según el objetivo. |

Esta sección describe la política implementada en el repositorio. Las verificaciones técnicas reportadas se ejecutaron en corridas aisladas y no equivalen a una única suite completa de backend aprobada ni a una ejecución de CI satisfactoria; el detalle y las limitaciones se registran en `doc/06-pruebas.md`. Tampoco acredita que documentación externa del responsable funcional haya sido actualizada.

## Relación con consultas vivas

La estrategia de estado del perfil anterior respeta reglas de negocio:

- asesor con consultas operativas no se desactiva;
- estudiante con consultas operativas no se desactiva;
- monitor con consultas operativas no se desactiva.

Esto mantiene coherencia entre cambio de perfil y responsabilidad operativa.

## Pruebas relacionadas

El código incluye pruebas unitarias para validar:

- delegación de `PerfilEstadoService` hacia handler;
- delegación de `PerfilUsuarioResolverService` hacia resolver;
- registry de handlers de estado;
- registry de resolvers de perfil activo;
- política central de activación y desactivación para los cinco tipos;
- independencia entre el estado del perfil y el estado de la cuenta;
- exclusividad y serialización de cambios sobre una misma cuenta;
- suspensión concurrente con una transición hacia `ADMINISTRATIVO`, conservando cuenta suspendida, rol, perfil actual, perfil activo único e historial único.

## Criterios de mantenimiento

Al agregar un nuevo tipo de perfil:

1. agregar enum en `TipoPerfilUsuario`;
2. crear handler de cambio de perfil;
3. crear handler de estado de perfil;
4. crear resolver de perfil activo;
5. registrar repositorio y reglas de acceso;
6. agregar pruebas unitarias;
7. actualizar documentación de backend, API, reglas y decisiones.


---

## Precisiones operativas del diseño Strategy

### Datos del perfil destino

Los handlers de cambio de perfil encapsulan diferencias de obligatoriedad entre perfiles. La clase base `CambiarPerfilBaseDTO` contiene datos comunes, pero no todos los campos comunes son obligatorios para todos los destinos. Por ejemplo, asesor y estudiante exigen documento, tipo de documento y sede; administrativo y monitor los admiten como opcionales dentro del cambio de perfil; conciliador exige documento y tipo de conciliador, pero admite tipo de documento y sede como opcionales.

El correo del perfil destino no se recibe en los DTO de cambio. Se deriva del `username` de `UsuarioSistema`, porque el cambio conserva la misma cuenta de acceso.

### Reutilización controlada

Cada handler busca perfiles previos por `UsuarioSistema.id`. Si existe un perfil destino asociado al mismo usuario, lo reactiva y actualiza. Si no existe, crea uno nuevo. Esta decisión evita duplicar perfiles del mismo tipo para una misma cuenta y no reutiliza perfiles pertenecientes a otros usuarios.

Antes de ejecutar los handlers, el cambio explícito aplica la validación de continuidad administrativa cuando corresponde, bloquea `UsuarioSistema` y comprueba la exclusividad. Si encuentra dos o más perfiles activos preexistentes, rechaza la transición sin desactivar registros ni elegir automáticamente cuál debe prevalecer.

### Perfil anterior y cuenta

`PerfilEstadoHandler` desactiva el perfil anterior, pero no desactiva `UsuarioSistema`. Esa cuenta continúa vigente con el tipo de perfil y rol destino actualizados. Esta separación es necesaria para que el cambio de perfil sea una transición operativa y no una suspensión de acceso.

El perfil anterior, el destino, `tipoPerfilActual`, el rol y `UsuarioCambioPerfilHistorial` participan en la misma transacción. Las operaciones directas de estado no crean historial de transición.

### Integridad operativa

Los handlers de estado de asesor, estudiante y monitor invocan la validación de consultas operativas antes de desactivar el perfil anterior. De esta manera, el cambio explícito conserva las restricciones de responsabilidad operativa aunque la desactivación directa del perfil actual esté prohibida.
