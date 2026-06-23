# Decisión técnica - Permisos y alcance

## Contexto

El sistema maneja distintos tipos de usuarios internos: administrativos, asesores, estudiantes, monitores y conciliadores.

Cada perfil puede tener permisos funcionales, pero no todos los usuarios con un permiso deben poder operar todos los recursos del sistema.

Ejemplos:

- un asesor puede ver consultas dentro de su alcance;
- un estudiante accede a recursos relacionados con sus consultas;
- un conciliador opera conciliaciones asignadas;
- un administrativo con permisos puede operar de forma global según su rol.

## Decisión

El sistema separa dos conceptos:

```text
permiso funcional
alcance real
```

Un permiso habilita la acción general. El alcance determina si el usuario puede ejecutar esa acción sobre un recurso específico.

Regla central:

```text
autenticación + permiso funcional + perfil activo + alcance real + regla de negocio
```

## Justificación

La separación evita que un permiso amplio otorgue acceso global indebido. También permite que el sistema refleje reglas reales del consultorio jurídico:

- asesor opera recursos relacionados con su área, sus consultas o sus estudiantes;
- estudiante opera únicamente recursos que le corresponden;
- monitor opera recursos asignados;
- conciliador opera conciliaciones asignadas;
- administrativo opera según permisos asignados a su rol.

## Componentes relacionados

| Componente | Propósito |
|---|---|
| `UsuarioSistema` | Identidad autenticada. |
| `Rol` | Agrupa permisos. |
| `Permiso` | Habilita acciones y navegación. |
| `AccessService` | Valida permisos y alcance en backend. |
| `UsuarioActualService` | Obtiene usuario y perfil actual. |
| `PerfilUsuarioResolverService` | Resuelve perfil activo mediante Strategy. |
| `PermissionSidebar` | Filtra navegación en frontend con permisos visibles. |

## Permisos funcionales

Los permisos se centralizan como nombres funcionales. El backend usa constantes y validaciones para proteger endpoints. El frontend utiliza permisos para mostrar u ocultar opciones de navegación y acciones.

Ejemplos de permisos documentados en módulos:

- ver consultas;
- crear consultas;
- editar consultas;
- cambiar estado de consultas;
- ver procesos;
- gestionar procesos;
- ver seguimientos;
- responder seguimientos;
- ver conciliaciones;
- gestionar conciliaciones;
- concluir conciliaciones;
- ver reportes;
- ver auditoría;
- gestionar usuarios, roles y permisos.

## Alcance por perfil

| Perfil | Alcance operativo general |
|---|---|
| Administrativo | Opera según permisos del rol asignado. |
| Asesor | Opera consultas y recursos dentro de su relación funcional. |
| Estudiante | Accede a consultas, seguimientos y recursos que le correspondan. |
| Monitor | Opera consultas asignadas según permisos. |
| Conciliador | Opera conciliaciones asignadas y sus reuniones. |

## Aplicación por módulo

### Consultas

El acceso se valida con `ConsultaAccessService`. El alcance se aplica para crear, editar, cambiar estado, archivar, desarchivar y asignar responsables.

### Procesos

El acceso se valida con `ProcesoAccessService`. El alcance depende de la consulta asociada y de la capacidad de gestionar procesos.

### Seguimientos

El acceso se valida con `SeguimientoAccessService` y `SeguimientoRespuestaAccessService`. El estudiante puede responder seguimientos visibles cuando están asociados a su consulta.

### Conciliación

El acceso se valida con `ConciliacionAccessService` y alcance específico de conciliación. El conciliador opera conciliaciones asignadas.

### Estadísticas

El módulo usa permisos como `VER_REPORTES` para reportes generales y `VER_CONSULTAS` para vistas relacionadas con alcance operativo.

## Impacto en frontend

La navegación no reemplaza las validaciones del backend. El frontend usa permisos para mejorar experiencia:

- mostrar módulos disponibles;
- ocultar rutas o acciones no aplicables;
- ajustar formularios según permisos;
- evitar llamadas innecesarias.

El backend conserva la validación final.

## Criterios de mantenimiento

Cuando se agregue un endpoint o acción:

1. Definir permiso funcional.
2. Validar alcance real en backend si el recurso pertenece a un usuario, perfil o consulta.
3. Ajustar navegación frontend si corresponde.
4. Documentar API, backend, reglas y frontend.
