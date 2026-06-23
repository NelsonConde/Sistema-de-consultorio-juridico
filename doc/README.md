# Documentación técnica del Sistema de Gestión de Casos Jurídicos

Esta carpeta contiene la documentación técnica del sistema web para la gestión de casos jurídicos del consultorio jurídico. La documentación se organiza por capas y módulos para facilitar la revisión del backend, frontend, API, reglas de negocio, base de datos, decisiones técnicas y mantenimiento.

## Fuente de verdad

La fuente principal de esta documentación es el código fuente vigente del proyecto. Los documentos describen funcionalidades, módulos, endpoints, reglas y validaciones que existen en la implementación actual.

Cuando exista diferencia entre documentación histórica y código fuente, prevalece el código fuente actualizado.

## Alcance del sistema documentado

El sistema implementa una aplicación web con backend Spring Boot y frontend Next.js para apoyar el ciclo operativo de un consultorio jurídico universitario. La versión documentada cubre:

- autenticación, sesión y recuperación de contraseña;
- usuarios del sistema, roles, permisos y cambio de perfil;
- perfiles internos: administrativos, asesores, estudiantes, monitores y conciliadores;
- catálogos generales y catálogos jurídicos;
- personas naturales y empresas relacionadas con la atención jurídica;
- consultas jurídicas y su ciclo funcional;
- procesos asociados a consultas;
- seguimientos, tareas y respuestas de seguimiento;
- conciliaciones, reuniones y notificaciones de reunión;
- gestión de archivos;
- auditoría de acciones relevantes;
- estadísticas, reportes por semestre, reportes por rango y reportes PDF;
- pruebas unitarias sobre reglas críticas de negocio y estrategias de seguridad.

## Estructura documental

```text
doc/
  README.md
  00-vision-general.md
  01-arquitectura.md
  02-configuracion-seguridad.md
  03-autenticacion-autorizacion.md
  04-permisos-roles-alcance.md
  05-estandar-api-errores.md
  06-pruebas.md

  api/
    Contratos HTTP expuestos por los controllers del backend.

  backend/
    Documentación de módulos, servicios, validadores, estrategias y reglas de backend.

  frontend/
    Documentación de rutas, componentes, formularios, navegación y consumo de API.

  reglas/
    Reglas de negocio vigentes por módulo.

  base-datos/
    Entidades principales, relaciones, estados y catálogos.

  decisiones/
    Decisiones técnicas y criterios de diseño aplicados.

  mantenimiento/
    Guías para conservar la documentación alineada con el código.
```

## Backend

El backend se encuentra en:

```text
backend/app
```

Tecnologías principales observadas en el código fuente:

- Java 21;
- Spring Boot;
- Spring MVC;
- Spring Security;
- Spring Data JPA;
- PostgreSQL;
- Jakarta Validation;
- Lombok;
- Maven;
- JWT;
- cookie HTTP-only para sesión;
- AOP para auditoría;
- iText para reportes PDF de estadísticas;
- Apache POI para importación de estudiantes;
- servicios de correo para recuperación de contraseña y notificaciones.

## Frontend

El frontend se encuentra en:

```text
frontend
```

Tecnologías principales observadas en el código fuente:

- Next.js con App Router;
- React;
- Tailwind CSS;
- shadcn/Radix UI;
- fetch API;
- configuración centralizada de URL de backend;
- manejo de sesión mediante cookie enviada con `credentials: "include"`;
- formularios y componentes reutilizables;
- navegación condicionada por permisos;
- Playwright configurado como herramienta de pruebas frontend.

## Convenciones de documentación

Esta documentación sigue estas reglas:

1. Documentar únicamente funcionalidades presentes en el código fuente.
2. Evitar datos sensibles, credenciales, tokens, llaves o contraseñas reales.
3. Describir el comportamiento implementado en tono técnico y afirmativo.
4. Separar contrato HTTP, reglas de negocio, frontend, backend y base de datos.
5. Actualizar los documentos cuando cambien controllers, DTOs, services, validadores, permisos o rutas frontend.

## Módulos principales

| Módulo | Descripción |
|---|---|
| Autenticación | Inicio de sesión, sesión actual, cierre de sesión, cambio y recuperación de contraseña. |
| Seguridad | JWT, cookie HTTP-only, filtros, CORS, roles, permisos y autorización por método. |
| Usuarios y perfiles | Gestión de usuarios del sistema, roles, permisos, perfiles internos y cambio de perfil. |
| Catálogos | Áreas, temas, tipos, sedes, documentos, departamentos, municipios, barrios, nacionalidades y otros datos auxiliares. |
| Personas | Personas, empresas y datos relacionados con la atención jurídica. |
| Consultas | Registro, edición controlada, estados, archivo, desarchivo, responsables y validaciones de trazabilidad. |
| Procesos | Procesos asociados a consultas, radicado condicional y estados funcionales. |
| Seguimientos | Tareas, notificaciones, respuestas de estudiantes y revisión por usuarios autorizados. |
| Conciliaciones | Conciliaciones desde consulta, reuniones, asignaciones, documentos y cierre con acta. |
| Archivos | Carga, descarga, listado y administración de archivos del sistema. |
| Auditoría | Registro de acciones relevantes mediante AOP y consulta paginada. |
| Estadísticas | Reportes por semestre, rango, perfil y exportación PDF. |
| Pruebas | Pruebas unitarias para reglas críticas, validadores y estrategias. |

## Lectura recomendada

1. `00-vision-general.md` para entender el sistema completo.
2. `01-arquitectura.md` para entender la separación de capas.
3. `03-autenticacion-autorizacion.md` y `04-permisos-roles-alcance.md` para comprender seguridad y alcance.
4. `api/` para contratos HTTP.
5. `backend/`, `frontend/` y `reglas/` para detalles por módulo.
