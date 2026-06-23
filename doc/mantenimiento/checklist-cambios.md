# Checklist de actualización documental

Este checklist se usa cuando se modifica código fuente y se requiere mantener la documentación técnica alineada.

## Revisión inicial

- Identificar el módulo modificado.
- Revisar si cambió un controller o endpoint.
- Revisar si cambió un DTO de entrada o salida.
- Revisar si cambió una entidad, relación o estado.
- Revisar si cambió una regla de negocio.
- Revisar si cambió un permiso, rol o regla de alcance.
- Revisar si cambió una ruta, componente o formulario frontend.
- Revisar si cambió una decisión transversal del diseño.

## Backend

Cuando cambie backend, revisar:

- `doc/backend/<modulo>.md`;
- `doc/api/<modulo>.md`;
- `doc/reglas/<modulo>.md`;
- `doc/base-datos/*` si hay cambios de entidad o relación;
- `doc/decisiones/*` si hay cambios de diseño transversal.

## API

Cuando cambie un endpoint, revisar:

- ruta exacta;
- método HTTP;
- permisos;
- parámetros de path;
- query params;
- cuerpo de request;
- cuerpo de response;
- códigos de error relevantes;
- relación con formularios frontend.

## Frontend

Cuando cambie frontend, revisar:

- ruta en App Router;
- componente principal de formulario o vista;
- consumo de API;
- manejo de errores;
- navegación por permisos;
- validaciones de formulario;
- componentes reutilizables afectados.

## Reglas de negocio

Cuando cambie una regla, revisar:

- validator relacionado;
- service o command service relacionado;
- access service si involucra permisos;
- pruebas unitarias asociadas;
- documentos de reglas, backend y API.

## Seguridad y configuración

Cuando cambie configuración o seguridad, revisar:

- `02-configuracion-seguridad.md`;
- `03-autenticacion-autorizacion.md`;
- `04-permisos-roles-alcance.md`;
- `frontend/configuracion-api.md`;
- `frontend/autenticacion-sesion.md`.

## Revisión final del texto

Antes de cerrar un cambio documental:

- verificar que lo documentado exista en código;
- verificar que no haya endpoints inventados;
- verificar que no haya credenciales reales;
- verificar que no haya valores privados;
- verificar que los índices incluyan documentos nuevos;
- verificar que los enlaces relativos correspondan a archivos existentes.
