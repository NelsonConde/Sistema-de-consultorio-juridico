# Backend - Perfiles, usuarios del sistema y cambio de perfil

> Documento validado contra el código fuente actualizado del sistema. La documentación describe únicamente comportamiento implementado en backend.


## 1. Propósito del módulo

El backend de perfiles administra los perfiles operativos vinculados a los usuarios del sistema. La implementación diferencia claramente entre:

- el **usuario del sistema**, que representa la cuenta de acceso, autenticación, rol y permisos;
- el **perfil operativo**, que representa la identidad funcional dentro del consultorio jurídico;
- el **rol**, que agrupa permisos y define el tipo de perfil asociado a la cuenta.

Los perfiles implementados son:

- Administrativo;
- Asesor;
- Monitor;
- Estudiante;
- Conciliador.

Cada perfil cuenta con controller, service principal, command service, query service, validator, mapper, DTO, repository y entidad JPA. Esta organización mantiene una separación clara entre operaciones de lectura, escritura, validaciones y conversión de datos.

---

## 2. Estructura principal del backend

Los controllers del módulo son:

| Controller | Ruta base | Perfil administrado |
|---|---|---|
| `AdministrativoController` | `/api/administrativos` | Administrativos y directoras |
| `AsesorController` | `/api/asesores` | Asesores jurídicos |
| `MonitorController` | `/api/monitores` | Monitores |
| `EstudianteController` | `/api/estudiantes` | Estudiantes |
| `ConciliadorController` | `/api/conciliadores` | Conciliadores |
| `UsuarioSistemaController` | `/api/usuarios-sistema` | Usuarios de acceso y cambio de perfil |
| `RolController` | `/api/roles` | Roles del sistema |
| `PermisoController` | `/api/permisos` | Permisos del sistema |

Cada perfil usa servicios internos equivalentes:

- `AdministrativoService`, `AdministrativoCommandService`, `AdministrativoQueryService`, `AdministrativoValidator`, `AdministrativoMapper`;
- `AsesorService`, `AsesorCommandService`, `AsesorQueryService`, `AsesorValidator`, `AsesorMapper`;
- `MonitorService`, `MonitorCommandService`, `MonitorQueryService`, `MonitorValidator`, `MonitorMapper`;
- `EstudianteService`, `EstudianteCommandService`, `EstudianteQueryService`, `EstudianteValidator`, `EstudianteMapper`;
- `ConciliadorService`, `ConciliadorCommandService`, `ConciliadorQueryService`, `ConciliadorValidator`, `ConciliadorMapper`.

---

## 3. DTOs de perfiles

### 3.1 AdministrativoDTO

Campos documentados en código:

| Campo | Descripción |
|---|---|
| `id` | Identificador del perfil administrativo |
| `nombre` | Nombre completo |
| `tipoDocumentoId` | Tipo de documento asociado |
| `documento` | Número de documento |
| `email` | Correo electrónico |
| `telefono` | Teléfono |
| `usuario` | Nombre de usuario |
| `codigo` | Código institucional |
| `sedeId` | Sede asociada |
| `activo` | Estado lógico del perfil |
| `directora` | Indica si el administrativo tiene marca de directora |

### 3.2 AsesorDTO

| Campo | Descripción |
|---|---|
| `id` | Identificador del asesor |
| `nombre` | Nombre completo |
| `tipoDocumentoId` | Tipo de documento |
| `documento` | Número de documento |
| `email` | Correo electrónico |
| `telefono` | Teléfono |
| `usuario` | Usuario de acceso asociado |
| `sedeId` | Sede |
| `codigo` | Código institucional |
| `areaId` | Área jurídica del asesor |
| `activo` | Estado lógico del perfil |

### 3.3 MonitorDTO

| Campo | Descripción |
|---|---|
| `id` | Identificador del monitor |
| `nombre` | Nombre completo |
| `tipoDocumentoId` | Tipo de documento |
| `documento` | Número de documento |
| `email` | Correo electrónico |
| `telefono` | Teléfono |
| `usuario` | Nombre de usuario |
| `codigo` | Código institucional |
| `sedeId` | Sede |
| `activo` | Estado lógico |

### 3.4 EstudianteDTO

| Campo | Descripción |
|---|---|
| `id` | Identificador del estudiante |
| `nombre` | Nombre completo |
| `tipoDocumentoId` | Tipo de documento |
| `documento` | Número de documento |
| `email` | Correo electrónico |
| `telefono` | Teléfono |
| `usuario` | Nombre de usuario |
| `sedeId` | Sede |
| `codigo` | Código institucional |
| `asesorId` | Asesor asignado |
| `activo` | Estado lógico |
| `conciliacion` | Habilitación para conciliación |

### 3.5 ConciliadorDTO

| Campo | Descripción |
|---|---|
| `id` | Identificador del conciliador |
| `nombre` | Nombre completo |
| `tipoDocumentoId` | Tipo de documento |
| `documento` | Número de documento |
| `email` | Correo electrónico |
| `telefono` | Teléfono |
| `usuario` | Nombre de usuario |
| `sedeId` | Sede |
| `codigo` | Código institucional |
| `tipoConciliador` | Tipo funcional del conciliador |
| `activo` | Estado lógico |

---

## 4. Creación de perfiles y UsuarioSistema

Los command services de perfiles crean el perfil y luego registran el usuario del sistema asociado mediante `UsuarioSistemaRegistroService`.

El flujo general es:

1. validar permisos de gestión del perfil;
2. validar que no se envíe `id` en creación;
3. normalizar datos básicos;
4. validar campos obligatorios;
5. validar duplicados por documento, correo, teléfono, usuario y código;
6. persistir el perfil;
7. crear el `UsuarioSistema` asociado;
8. vincular el perfil con el usuario creado;
9. retornar el DTO del perfil persistido.

Este diseño mantiene una relación directa entre identidad funcional y cuenta de acceso.

---

## 5. Actualización de perfiles

La actualización por `PUT` modifica datos propios del perfil, pero conserva los campos de control funcional que cuentan con endpoints específicos.

Ejemplos:

- `activo` se modifica mediante `PATCH /{id}/activo`;
- `directora` se modifica mediante `PATCH /api/administrativos/{id}/directora`;
- `conciliacion` se modifica mediante `PATCH /api/estudiantes/{id}/conciliacion`.

En actualización se valida:

- que el perfil exista;
- que el `id` del DTO no sea diferente al de la ruta;
- que los campos obligatorios estén completos;
- que no se generen duplicados;
- que exista un cambio real antes de guardar.

---

## 6. Estado activo del perfil y UsuarioSistema

`UsuarioSistema.activo` representa exclusivamente la habilitación de la cuenta. Su ciclo de vida es independiente del estado de los perfiles y se administra mediante la operación explícita de suspensión o habilitación de cuenta.

Para los cinco tipos de perfil, una cuenta debe conservar un único perfil operativo activo. Los perfiles anteriores permanecen como registros históricos inactivos. En operaciones directas sobre un perfil vinculado a una cuenta:

- activar o desactivar un perfil histórico diferente de `tipoPerfilActual` se rechaza;
- desactivar el perfil actual se rechaza; para impedir el acceso debe suspenderse la cuenta y para cambiar de cargo debe usarse el cambio explícito de perfil;
- activar el perfil actual solo se permite si no existe otro perfil operativo activo;
- `UsuarioSistema.activo`, `tipoPerfilActual`, el rol, los permisos y el historial no se modifican implícitamente.

El rechazo responde `409 Conflict` con `detalles.codigo = CONFLICTO_OPERACION_PERFIL` y no produce cambios parciales. Si ya existen varios perfiles activos, la operación se rechaza y no intenta sanearlos automáticamente.

Los registros sin cuenta asociada conservan el comportamiento anterior y las validaciones aplicables, porque no representan una transición de acceso. La clase `UsuarioSistemaPerfilEstadoService` se conserva por compatibilidad, pero los command services de los cinco perfiles ya no la usan para sincronizar operaciones directas.

Cuando existe una cuenta asociada, los command services obtienen primero `usuario_sistema_id`, bloquean `UsuarioSistema` con `PESSIMISTIC_WRITE`, recargan el perfil, validan la política central y solo entonces aplican las validaciones específicas y persisten. La suspensión o habilitación de cuenta y los cambios explícitos comparten el bloqueo de `UsuarioSistema`. Si la operación puede afectar la continuidad administrativa, el orden es: continuidad administrativa, bloqueo de cuenta, lectura y validación, modificación y persistencia. Los cambios hacia `ADMINISTRATIVO` adquieren también el bloqueo administrativo antes del bloqueo de cuenta para conservar ese orden global.

La política funcional se resume así:

| Regla | Comportamiento |
|---|---|
| Exclusividad operativa | Solo puede haber un perfil activo por cuenta entre los cinco tipos; los anteriores se conservan como históricos inactivos. Activar el actual solo se permite cuando no existe otro perfil activo. |
| Sin cambios implícitos | Las operaciones directas de perfil no cambian la cuenta, el tipo de perfil actual, el rol, los permisos ni el historial. |
| Activación histórica | Activar directamente un perfil histórico se rechaza sin cambios parciales; para hacerlo vigente se usa el cambio explícito. |
| Desactivación histórica | Desactivar o eliminar lógicamente un perfil histórico mediante su endpoint directo se rechaza sin cambios parciales. |
| Estado de cuenta independiente | Suspender o habilitar `UsuarioSistema` no cambia los perfiles; activar un perfil tampoco habilita una cuenta suspendida. |
| Transición explícita | El cambio de tipo valida el destino y la exclusividad, aplica las estrategias de perfil, rol e historial en una transacción y mantiene las restricciones operativas existentes. |
| Perfil actual | Su desactivación o eliminación lógica directa se rechaza. Para impedir acceso se suspende la cuenta; para cambiar de perfil se usa la transición explícita. |

Los rechazos de política usan HTTP `409 Conflict` con el código funcional `CONFLICTO_OPERACION_PERFIL`. Una cuenta que ya tenga más de un perfil activo se rechaza como inconsistente; el flujo no elige un perfil ni sanea los datos automáticamente.

---

## 7. Protección de responsables con consultas operativas

El servicio `ConsultaResponsableOperacionService` protege la integridad operativa de las consultas al impedir la desactivación de responsables con consultas vivas.

Se aplica a:

- asesores;
- estudiantes;
- monitores.

Estados considerados operativos:

- `PENDIENTE`;
- `ACTIVO`;
- `EN_PROCESO`;
- `URGENTE`.

Para asesores se validan dos escenarios:

1. consultas asignadas directamente al asesor;
2. consultas de estudiantes vinculados al asesor.

Esto conserva la trazabilidad de responsables internos en casos jurídicos activos.

---

## 8. Habilitación de estudiantes para conciliación

El estudiante cuenta con un campo funcional `conciliacion`, administrado mediante endpoint específico. Este valor permite identificar estudiantes habilitados para participar en flujos de conciliación.

El backend expone:

- listado general de estudiantes;
- listado de estudiantes activos;
- listado de estudiantes habilitados para conciliación;
- listado de estudiantes activos por asesor;
- cambio de estado activo;
- cambio de habilitación para conciliación;
- importación de estudiantes desde archivo.

---

## 9. Cambio de perfil con Strategy

El cambio de perfil está implementado con estrategias para evitar lógica centralizada por tipo de perfil.

### 9.1 Perfil destino

La creación o actualización del perfil destino se resuelve mediante:

- `PerfilCambioHandler`;
- `PerfilCambioHandlerRegistry`;
- `CambiarAAdministrativoHandler`;
- `CambiarAAsesorHandler`;
- `CambiarAEstudianteHandler`;
- `CambiarAMonitorHandler`;
- `CambiarAConciliadorHandler`.

Cada handler conoce:

- el tipo de perfil que atiende;
- el DTO esperado;
- el repositorio correspondiente;
- las validaciones de duplicados;
- la forma de activar o reutilizar el perfil destino.

### 9.2 Desactivación del perfil anterior

La desactivación del perfil anterior se resuelve mediante:

- `PerfilEstadoHandler`;
- `PerfilEstadoHandlerRegistry`;
- `AdministrativoPerfilEstadoHandler`;
- `AsesorPerfilEstadoHandler`;
- `EstudiantePerfilEstadoHandler`;
- `MonitorPerfilEstadoHandler`;
- `ConciliadorPerfilEstadoHandler`.

Los handlers de asesor, estudiante y monitor aplican la validación de consultas operativas antes de desactivar el perfil anterior.

### 9.3 Resolución del perfil activo

La resolución del perfil activo del usuario autenticado se resuelve mediante:

- `PerfilUsuarioActivoResolver`;
- `PerfilUsuarioActivoResolverRegistry`;
- resolvers concretos por tipo de perfil.

`PerfilUsuarioResolverService` delega la búsqueda del perfil activo al resolver correspondiente, según `tipoPerfilActual` del usuario.

---

## 10. Historial de cambio de perfil

El cambio de perfil registra historial mediante `UsuarioCambioPerfilHistorialService` y la entidad `UsuarioCambioPerfilHistorial`.

El historial conserva:

- usuario del sistema;
- tipo de perfil anterior;
- identificador del perfil anterior;
- rol anterior;
- tipo de perfil nuevo;
- identificador del perfil nuevo;
- rol nuevo;
- usuario que realiza el cambio;
- motivo del cambio.

El motivo es obligatorio y tiene validación de longitud máxima.

---

## 11. Usuarios del sistema

`UsuarioSistema` representa la cuenta de acceso. El DTO `UsuarioSistemaDTO` expone:

| Campo | Descripción |
|---|---|
| `id` | Identificador del usuario del sistema |
| `username` | Nombre de usuario |
| `activo` | Estado de acceso |
| `rolId` | Rol actual |
| `rolNombre` | Nombre del rol |
| `perfilId` | Perfil operativo actual |
| `tipoPerfil` | Tipo de perfil actual |
| `permisos` | Permisos efectivos asociados al rol |

Los usuarios del sistema se listan, consultan, activan/desactivan y cambian de perfil mediante endpoints específicos.

---

## 12. Roles y permisos

Los roles agrupan permisos y están vinculados al tipo de perfil esperado para el usuario.

`RolDTO` contiene:

- `id`;
- `nombre`;
- `descripcion`;
- `activo`;
- `permisoIds`;
- `permisos`.

`PermisoDTO` contiene:

- `id`;
- `nombre`;
- `descripcion`;
- `activo`.

Los permisos se asignan y retiran de roles mediante endpoints dedicados, manteniendo explícita la relación rol-permiso.

---

## 13. Alcance por perfil

El backend usa servicios de acceso para validar permisos y alcance operativo:

- `AdministrativoAccessService`;
- `AsesorMonitorAccessService`;
- `ConciliadorAccessService`;
- `EstudianteAccessService`;
- services de acceso de consultas, procesos, seguimientos y conciliaciones.

Esta arquitectura permite que cada módulo combine permisos globales con reglas de alcance por usuario autenticado.

---

## 14. Pruebas relacionadas

El comportamiento de perfiles y estrategias cuenta con pruebas unitarias sobre:

- validación central de operaciones directas y exclusividad de perfiles;
- independencia entre el estado del perfil y el estado de la cuenta;
- desactivación mediante Strategy;
- resolución de perfil activo mediante Strategy;
- registry de handlers de estado;
- registry de resolvers de perfil;
- bloqueo de responsables con consultas operativas.

Pruebas observadas:

- `PerfilOperacionEstadoValidatorTest`;
- `UsuarioSistemaPerfilEstadoServiceTest` (cobertura del componente conservado, no del flujo directo vigente);
- `PerfilEstadoServiceTest`;
- `PerfilUsuarioResolverServiceTest`;
- `PerfilEstadoHandlerRegistryTest`;
- `PerfilUsuarioActivoResolverRegistryTest`;
- `ConsultaResponsableOperacionServiceTest`.

La integración HTTP y transaccional se cubre en `PerfilEstudianteReactivacionPostgreSqlTest`, incluidos los cinco tipos de perfil, el contrato `409`, los cambios explícitos, la concurrencia P45 a P49, el estado inconsistente P50, la serialización entre suspensión y transición hacia `ADMINISTRATIVO` en P51 y las concurrencias de credenciales P52 y P53. La evidencia de ejecución y sus limitaciones se registran en `doc/06-pruebas.md`.


---

## 15. Precisiones de implementación validadas en código

### 15.1 Gestión de administrativos

La gestión de administrativos combina la anotación de permisos del controller con `AdministrativoAccessService`, que exige que el usuario autenticado opere como administrador y que su perfil administrativo activo tenga `directora=true`. Esta regla aplica a creación, actualización, cambio de estado, cambio de marca de directora y eliminación lógica de administrativos.

### 15.2 Alcance de estudiantes

`EstudianteQueryService` aplica alcance posterior a la validación de permisos:

- administrador: consulta todos los estudiantes según el método invocado;
- asesor: consulta únicamente estudiantes asociados a su asesoría;
- otros perfiles: el listado operativo aplica el alcance funcional definido por `EstudianteAccessService`.

Para `listarActivosPorAsesor`, el administrador puede consultar cualquier asesor; el asesor autenticado solo puede consultar su propio id de asesor.

`listarConConciliacion` retorna estudiantes activos con `conciliacion=true` y luego aplica la regla de visibilidad definida por `EstudianteAccessService`.

### 15.3 Importación masiva de estudiantes

El flujo de importación lee un archivo Excel enviado como `archivo`, procesa la primera hoja y valida los encabezados esperados en el orden definido, sin distinguir mayúsculas y minúsculas. Cada fila se convierte a `EstudianteDTO` y pasa por `EstudianteCommandService.crear`, reutilizando validaciones de campos, catálogos, duplicados, asesor activo y creación de `UsuarioSistema`.

Encabezados esperados:

```text
Nombre, TipoDocumentoId, Documento, Email, Telefono, Usuario, SedeId, Codigo, AsesorId, Activo, Conciliacion
```

La importación reporta resultado por filas mediante `ImportacionEstudiantesDTO`.

### 15.4 Separación entre cuenta y perfil

`UsuarioSistemaService.cambiarEstado` administra explícitamente la habilitación de la cuenta. Los command services de perfiles administran el estado del registro operativo sin habilitar ni suspender automáticamente esa cuenta.

La autenticación requiere que la cuenta, el rol y el perfil indicado por `tipoPerfilActual` estén habilitados. Activar el perfil actual de una cuenta suspendida no recupera el acceso; la cuenta debe habilitarse mediante su operación específica.

La modificación del estado de cuenta usa bloqueo pesimista para no sobrescribir concurrentemente el rol o `tipoPerfilActual` de una transición explícita. Los cambios de contraseña de `AuthService` y `PasswordResetService` reutilizan el mismo bloqueo de cuenta y modifican la instancia recargada bajo bloqueo; en el restablecimiento, el token activo se relee después para impedir su reutilización concurrente. Esta revisión preventiva no acredita que todos los demás escritores presentes o futuros de `UsuarioSistema` estén cubiertos.

### 15.5 Cambio de perfil

El cambio de perfil tiene reglas propias:

- el email del perfil destino se toma desde `UsuarioSistema.username`;
- el perfil destino puede crearse o reactivarse si ya existía para el mismo usuario;
- el perfil anterior se desactiva y el `UsuarioSistema` permanece activo con el perfil destino;
- cuando corresponde, se valida y bloquea primero la continuidad administrativa; después se bloquea la cuenta y se valida que no existan varios perfiles operativos activos;
- un estado histórico inconsistente se rechaza sin corrección automática;
- el cambio de perfil actualiza de forma atómica `tipoPerfilActual`, rol, perfiles e historial;
- la obligatoriedad de `documento`, `tipoDocumentoId`, `sedeId` y campos específicos depende del handler de Strategy del perfil destino.

| Perfil destino | Reglas de datos aplicadas por handler |
|---|---|
| Administrativo | Documento, tipo de documento y sede opcionales; `directora` opcional. |
| Asesor | Documento, tipo de documento, sede y área obligatorios. |
| Estudiante | Documento, tipo de documento, sede y asesor obligatorios; `conciliacion` opcional. |
| Monitor | Documento, tipo de documento y sede opcionales. |
| Conciliador | Documento y tipo de conciliador obligatorios; tipo de documento y sede opcionales. |

### 15.6 Roles y permisos

La creación de rol exige una lista de permisos activos con al menos un elemento. En actualización, informar `permisoIds` reemplaza la lista completa de permisos; omitir `permisoIds` conserva la asignación existente.

La API de permisos administra el ciclo de vida mediante activación y desactivación lógica con `PATCH /api/permisos/{id}/activo?activo=true|false`.
