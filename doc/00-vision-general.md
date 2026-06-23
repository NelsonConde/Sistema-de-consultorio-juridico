# Visión general del sistema
## Propósito

El Sistema de Gestión de Casos Jurídicos centraliza el registro, control y seguimiento de la atención jurídica realizada por el consultorio jurídico. La aplicación permite administrar personas, consultas, procesos, seguimientos, conciliaciones, usuarios, permisos, archivos, auditoría y estadísticas.

La solución está organizada como una aplicación web con backend REST y frontend web. El backend concentra las reglas de negocio, seguridad, persistencia, auditoría y generación de reportes. El frontend ofrece las pantallas operativas para que los usuarios interactúen con los módulos según sus permisos.

## Objetivo funcional

El sistema permite acompañar el ciclo de atención jurídica desde el ingreso de la información inicial hasta el seguimiento de actuaciones, procesos o conciliaciones asociadas. También permite consultar información de operación mediante estadísticas y reportes PDF.

## Actores internos representados en el sistema

El código fuente modela los siguientes perfiles internos:

- Administrativo;
- Asesor;
- Estudiante;
- Monitor;
- Conciliador.

Estos perfiles se relacionan con un `UsuarioSistema`, un rol y un conjunto de permisos. El perfil activo determina el alcance funcional del usuario autenticado.

## Capacidades principales

### Autenticación y sesión

El backend expone endpoints para login, consulta de usuario autenticado, cierre de sesión, cambio de contraseña, solicitud de recuperación y restablecimiento de contraseña. La sesión se maneja con JWT enviado al navegador en una cookie HTTP-only llamada `access_token`.

### Roles, permisos y alcance

El sistema combina autorización por permisos con servicios de acceso por módulo. Los controllers usan `@PreAuthorize` y los servicios de acceso validan alcance operativo según usuario, perfil y recurso.

### Personas y catálogos

El sistema administra catálogos jurídicos y auxiliares usados por formularios y flujos operativos. También gestiona personas naturales, empresas y datos complementarios.

### Consultas jurídicas

El módulo de consultas permite crear, editar, cambiar estado, archivar, desarchivar y consultar registros. El backend protege el ciclo funcional mediante reglas como:

- una consulta nueva inicia en estado `PENDIENTE`;
- el estado no se cambia desde la edición general;
- una consulta cerrada o archivada bloquea operaciones operativas;
- el cierre exige resultado o conclusión final;
- el cierre valida que no existan pendientes operativos;
- los datos estructurales se protegen cuando la consulta ya tiene actividad asociada.

### Procesos

El módulo de procesos registra procesos asociados a consultas. El código fuente implementa la regla de radicado condicional: un proceso puede permanecer `PENDIENTE` sin radicado, pero los estados finales requieren número de radicado válido.

### Seguimientos y respuestas

El módulo de seguimientos permite crear tareas asociadas a consultas, programar notificaciones y gestionar respuestas de estudiantes. El backend valida que un seguimiento visible para estudiante tenga estudiante asignado y activo. Las respuestas rechazadas requieren observación de revisión.

### Conciliaciones

El módulo de conciliaciones permite crear conciliaciones desde consultas, asignar estudiante y conciliador, programar o reprogramar reuniones, finalizar con acta, reemplazar solicitud y desactivar registros. El backend cancela notificaciones pendientes de reunión cuando la conciliación finaliza o se desactiva.

### Archivos

El sistema expone endpoints para carga, carga múltiple, descarga, listado y directorios. Los módulos de conciliación y respuestas de seguimiento usan archivos para soportes o documentos específicos.

### Auditoría

La auditoría se implementa con una anotación `@Auditable` y un aspecto AOP que registra acciones exitosas de servicios marcados. Existe un endpoint administrativo para consultar registros paginados.

### Estadísticas

El módulo de estadísticas expone reportes por semestre, rango de fechas y perfil. También genera archivos PDF de reportes estadísticos usando iText.

## Frontend

El frontend implementa rutas públicas de autenticación y rutas privadas agrupadas en dashboard. La navegación se construye por permisos y los formularios consumen el backend mediante URL centralizada y cookies de sesión.

Rutas principales observadas en el código:

- `/` para login;
- `/recuperar-password`;
- `/restablecer-password`;
- `/inicio`;
- `/admin`;
- `/roles`;
- `/asesoresymonitores`;
- `/estudiantes`;
- `/personas`;
- `/recepcion`;
- `/nuevaconsulta`;
- `/consultasjuridicas`;
- `/nuevoproceso`;
- `/procesos`;
- `/tareas`;
- `/conciliaciones`;
- `/estadisticas`;
- `/eliminacion`.

## Criterio de alcance documental

La documentación describe la versión implementada en el código fuente. No se incluyen flujos no presentes en controllers, servicios, componentes, rutas o DTOs.
acciones según permisos, pero el backend conserva la responsabilidad final de validar autorización y reglas de negocio.
