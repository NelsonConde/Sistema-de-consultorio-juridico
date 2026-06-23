# Matriz de actualización documental

Esta matriz indica qué documentos revisar según el tipo de cambio realizado en el código fuente.

## Cambios por módulo

| Cambio en código | Documentos a revisar |
|---|---|
| Controller de autenticación | `api/autenticacion.md`, `03-autenticacion-autorizacion.md`, `frontend/autenticacion-sesion.md` |
| Seguridad, JWT o sesión | `02-configuracion-seguridad.md`, `03-autenticacion-autorizacion.md`, `backend/autenticacion-seguridad.md` |
| Roles o permisos | `04-permisos-roles-alcance.md`, `api/usuarios-roles-permisos.md`, `reglas/permisos.md`, `frontend/navegacion-permisos.md` |
| Cambio de perfil | `backend/perfiles.md`, `api/usuarios-roles-permisos.md`, `decisiones/estrategia-perfiles.md` |
| Perfiles internos | `backend/perfiles.md`, `api/perfiles.md`, `reglas/permisos.md`, `base-datos/entidades-principales.md` |
| Personas | `backend/personas.md`, `api/personas.md`, `frontend/modulos/personas.md`, `base-datos/entidades-principales.md` |
| Catálogos | `backend/catalogos.md`, `api/catalogos.md`, `frontend/modulos/catalogos.md`, `base-datos/estados-y-catalogos.md` |
| Consultas | `backend/consultas.md`, `api/consultas.md`, `reglas/consultas.md`, `frontend/modulos/consultas.md` |
| Procesos | `backend/procesos.md`, `api/procesos.md`, `reglas/procesos.md`, `frontend/modulos/procesos.md` |
| Seguimientos | `backend/seguimientos.md`, `api/seguimientos.md`, `reglas/seguimientos.md`, `frontend/modulos/seguimientos.md` |
| Respuestas de seguimiento | `backend/seguimientos.md`, `api/seguimientos.md`, `reglas/seguimientos.md`, `frontend/modulos/seguimientos.md` |
| Conciliaciones | `backend/conciliaciones.md`, `api/conciliaciones.md`, `reglas/conciliaciones.md`, `frontend/modulos/conciliaciones.md` |
| Reuniones de conciliación | `backend/conciliaciones.md`, `api/conciliaciones.md`, `frontend/modulos/reuniones-conciliacion.md` |
| Estadísticas | `backend/estadisticas.md`, `api/estadisticas.md`, `reglas/estadisticas.md`, `frontend/modulos/estadisticas.md` |
| Auditoría | `backend/auditoria.md`, `api/auditoria.md`, `frontend/modulos/usuarios-roles.md` |
| Archivos | `backend/archivos.md`, `api/archivos.md`, `reglas/archivos.md`, documentos frontend que carguen o descarguen archivos |

## Cambios transversales

| Tipo de cambio | Documentos a revisar |
|---|---|
| Nueva entidad JPA | `base-datos/entidades-principales.md`, `base-datos/relaciones.md`, módulo backend/API correspondiente |
| Nuevo estado funcional | `base-datos/estados-y-catalogos.md`, `reglas/<modulo>.md`, API y backend del módulo |
| Nuevo permiso | `04-permisos-roles-alcance.md`, `reglas/permisos.md`, `api/usuarios-roles-permisos.md`, `frontend/navegacion-permisos.md` |
| Nuevo componente frontend | `frontend/estructura.md`, documento del módulo correspondiente |
| Nueva ruta frontend | `frontend/estructura.md`, `frontend/README.md`, documento del módulo correspondiente |
| Nuevo test unitario | `06-pruebas.md`, documento del módulo si aplica |
| Nueva decisión técnica | `decisiones/README.md` y archivo nuevo o existente en `decisiones/` |

## Cambios de documentación

Cuando se agregue un archivo nuevo en `doc/`, revisar:

- `doc/README.md`;
- README de la carpeta correspondiente;
- `mantenimiento/estado-cobertura-documental.md`;
- `mantenimiento/matriz-actualizacion-documental.md` si aplica.
