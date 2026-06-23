# Reglas de negocio

Esta carpeta documenta las reglas de negocio vigentes del sistema de gestión de casos jurídicos.

Las reglas descritas corresponden al comportamiento implementado en backend. El frontend puede ayudar a orientar al usuario, pero las validaciones críticas se aplican en los servicios de backend.

## Índice

| Documento | Contenido |
|---|---|
| `consultas.md` | Reglas de creación, responsables, estados, cierre, archivo y trazabilidad de consultas. |
| `procesos.md` | Reglas de procesos, radicado, estados, órganos de control y especialidades. |
| `seguimientos.md` | Reglas de seguimientos, respuestas, revisión, visibilidad y notificaciones. |
| `conciliaciones.md` | Reglas de conciliación, responsables, reuniones, actas, estados y notificaciones. |
| `permisos.md` | Reglas de roles, permisos, alcance y perfiles activos. |
| `estadisticas.md` | Reglas de acceso y cálculo de estadísticas operativas. |
| `archivos.md` | Reglas de carga, descarga, rutas, seguridad documental y archivos de conciliación. |

## Principios generales

- Las reglas críticas se validan en backend.
- El frontend organiza formularios y navegación, pero no reemplaza las validaciones del backend.
- Los permisos funcionales se complementan con validaciones de alcance.
- Los estados funcionales se cambian mediante endpoints específicos.
- El campo `activo` se usa para disponibilidad operativa o desactivación lógica según entidad.
- Las operaciones históricas conservan trazabilidad.
- Los documentos y archivos se gestionan con rutas controladas.

## Estado funcional y activo lógico

El sistema diferencia entre estado funcional y activo lógico.

| Concepto | Uso |
|---|---|
| Estado funcional | Representa el ciclo de vida del recurso. |
| Activo lógico | Representa disponibilidad operativa o desactivación lógica. |

Ejemplos:

- una consulta usa estados como `PENDIENTE`, `ACTIVO`, `EN_PROCESO`, `URGENTE`, `CERRADO` y `ARCHIVADO`;
- un proceso usa `PENDIENTE` y estados finales como `SENTENCIA_FAVORABLE`, `RECHAZO`, `SENTENCIA_DESFAVORABLE` o `CONCILIACION`;
- una conciliación usa estados técnicos del catálogo `estado_conciliacion`;
- perfiles, catálogos, seguimientos y otros registros usan `activo` para disponibilidad operativa.

## Relación con documentación técnica

Las reglas de esta carpeta se complementan con:

| Carpeta | Relación |
|---|---|
| `backend/` | Describe cómo se implementan las reglas en servicios, validadores y repositorios. |
| `api/` | Expone cómo se invocan las reglas desde endpoints REST. |
| `frontend/` | Describe cómo se presentan los formularios y acciones al usuario. |
| `base-datos/` | Documenta entidades, relaciones, estados y catálogos vinculados. |
| `decisiones/` | Explica decisiones de diseño relacionadas con reglas transversales. |

## Mantenimiento de reglas

Cuando una regla cambie en el código, se revisan los documentos relacionados en `reglas/`, `backend/` y `api/`. Si el cambio también afecta formularios o navegación, se revisa la sección correspondiente en `frontend/`.
