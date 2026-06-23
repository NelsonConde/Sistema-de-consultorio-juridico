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

El sistema conserva sincronizado el estado del perfil operativo con el usuario de acceso asociado cuando el cambio se realiza desde los command services de perfiles.

La clase `UsuarioSistemaPerfilEstadoService` sincroniza el estado de `UsuarioSistema` cuando se desactiva o reactiva un perfil desde los command services de:

- Administrativo;
- Asesor;
- Estudiante;
- Monitor;
- Conciliador.

La sincronización se ejecuta de forma segura:

- cuando el perfil tiene un usuario asociado, sincroniza el estado de la cuenta de acceso;
- persiste el usuario asociado únicamente cuando su estado requiere actualización;
- si el perfil se desactiva, el usuario asociado queda inactivo;
- si el perfil se reactiva, el usuario asociado queda activo.

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

- sincronización de perfil y usuario del sistema;
- desactivación mediante Strategy;
- resolución de perfil activo mediante Strategy;
- registry de handlers de estado;
- registry de resolvers de perfil;
- bloqueo de responsables con consultas operativas.

Pruebas observadas:

- `UsuarioSistemaPerfilEstadoServiceTest`;
- `PerfilEstadoServiceTest`;
- `PerfilUsuarioResolverServiceTest`;
- `PerfilEstadoHandlerRegistryTest`;
- `PerfilUsuarioActivoResolverRegistryTest`;
- `ConsultaResponsableOperacionServiceTest`.


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

### 15.4 Sincronización de estado

`UsuarioSistemaPerfilEstadoService` se invoca desde los command services de perfiles cuando se cambia el estado del perfil. Por eso, desactivar o reactivar un perfil sincroniza la cuenta de acceso asociada.

El servicio `UsuarioSistemaService.cambiarEstado` administra directamente el estado del `UsuarioSistema`. El estado del perfil operativo se gestiona desde los command services de perfiles, donde `UsuarioSistemaPerfilEstadoService` sincroniza la cuenta asociada.

### 15.5 Cambio de perfil

El cambio de perfil tiene reglas propias:

- el email del perfil destino se toma desde `UsuarioSistema.username`;
- el perfil destino puede crearse o reactivarse si ya existía para el mismo usuario;
- el perfil anterior se desactiva y el `UsuarioSistema` permanece activo con el perfil destino;
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
