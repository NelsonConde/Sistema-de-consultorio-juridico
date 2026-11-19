# Registro de cambios — Sistema de Gestión de Casos Jurídicos

Este archivo registra los cambios técnicos del Sistema de Gestión de Casos Jurídicos. Se utiliza la convención de versiones `vMAJOR.MINOR.PATCH` para identificar las etiquetas del producto.

## [Unreleased]

Candidato PATCH `v1.3.1` para los cambios integrados en `develop` después de la publicación de `v1.3.0`. **No existe todavía etiqueta, Release ni commit de `main` para `v1.3.1`.**

### Fixed

- Desactivación de personas: obtener el detalle vigente inmediatamente antes de la operación y enviar la versión de recurso actual para respetar el control de concurrencia ([PR #117](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/117), SCRUM-386).

### Tests and evidence

- Cuatro casos de integración y regresión (INT-01 a INT-04): historial de reasignación PostgreSQL, estadísticas/PDF y fechas locales, importación XLSX, resiliencia de notificaciones y correo ([PR #116](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/116), SCRUM-380).
- Nuevas pruebas Java y evidencias versionadas en `doc/evidencias/sprint-8-scrum-380/`. Las cuatro fichas documentan PASS en el ambiente de QA local; estos resultados no certifican por sí solos una ejecución real en `staging`.
- CI de `develop` del commit `58bce3b61ac23dd2636f9850680a4557aeed7c23`: Backend, Frontend, Playwright E2E, construcción Docker y análisis de código finalizados satisfactoriamente, después de repetir el job Backend por un fallo de descarga del Maven Wrapper.

### Release control

- Alcance previsto: corrección compatible y evidencias QA; clasificación PATCH bajo SemVer.
- Sin cambios nuevos de migración SQL ni de los manifiestos Maven/npm dentro de PR #116/#117.
- Documentación de preparación: [preflight-liberacion-v1.3.1.md](doc/mantenimiento/preflight-liberacion-v1.3.1.md).
- Pendiente: revisión independiente del PR documental, promoción `develop` → `staging`, CI y validación correspondiente, aprobación expresa del Product Owner para `main`, promoción `staging` → `main`, verificación del commit definitivo, etiqueta Git, notas de entrega y registro de línea base.

## [v1.3.0] - 2026-11-15

La etiqueta `v1.3.0` identifica el commit `d3f71570d41033092ffe366ee177f85fabeaa6fb` de `main`, con [publicación oficial en GitHub](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/releases/tag/v1.3.0). Se trata de una versión MINOR con funcionalidades compatibles y correcciones.

### Added

- Invitación inicial tras la creación de cuentas y administración de activaciones pendientes, con recuperación operativa ([PR #105](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/105)).
- Programación de la primera reunión de conciliación desde la interfaz ([PR #106](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/106)).
- Plantilla XLSX válida para importación de estudiantes ([PR #107](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/107)).
- Gestión documental de procesos: carga, listado, descarga, nuevas versiones, historial, eliminación autorizada y reintento seguro de confirmación ([PR #111](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/111)).

### Fixed

- Validación de encabezados XLSX inválidos con error de cliente en vez de fallo interno ([PR #108](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/108)).
- Tareas pendientes de Inicio consultadas sin depender de la primera página de consultas ([PR #109](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/109)).
- Fecha predeterminada de estadísticas calculada según el calendario local, no UTC ([PR #110](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/110)).
- Etiquetas de estadísticas y PDF alineadas con la semántica temporal de los datos ([PR #111](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/111)).

### Release verification and traceability

- [PR #113](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/113): preparación de la liberación, validación de imagen Docker y documentación.
- [PR #114](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/114): promoción a `staging`.
- [PR #115](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/115): promoción a `main`.
- Los 8 controles CI del commit final de `main` concluyeron satisfactoriamente. Railway informó `SUCCESS` en la compilación y arranque del backend de ese commit, con conexión PostgreSQL y validación de 4 migraciones Flyway; el despliegue fue posteriormente detenido.
- Las verificaciones técnicas y la publicación GitHub no sustituyen la evidencia documental de aprobación del Product Owner ni la aceptación funcional correspondiente.

## [v1.2.1] - 2026-11-07

La etiqueta `v1.2.1` identifica el commit `26251d026911d395315f7621aaf610b6da729f1a` de `main`. [Release publicada](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/releases/tag/v1.2.1). Es una actualización correctiva (PATCH) del proceso de construcción Docker del backend para Railway.

### Fixed

- Instalación de `unzip` únicamente en la etapa de construcción del [Dockerfile del backend](backend/app/Dockerfile) para permitir la verificación SHA-256 de la distribución de Maven Wrapper sin desactivar el control de integridad.
- Restablecimiento de la compilación y publicación de la imagen Docker en Railway, manteniendo Java 21 y Maven 3.9.14.

### Verification

- Construcción Docker local satisfactoria con `docker build --no-cache -f backend/app/Dockerfile backend/app`; el empaquetado utiliza `-DskipTests` y no acredita la ejecución de pruebas unitarias.
- Comprobaciones automáticas de GitHub Actions y revisiones independientes de los cambios de los PR correspondientes.
- Registros de Railway: imagen Docker compilada y publicada; Spring Boot 4.0.6 inició correctamente y estableció conexión PostgreSQL mediante Supabase Pooler.
- Flyway validó correctamente 4 migraciones; JPA/Hibernate inicializó el contexto de persistencia. La navegación funcional del sistema desplegado fue comprobada por el responsable.

### Traceability

- [PR #99](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/99): corrección del Dockerfile.
- [PR #100](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/100): promoción a `staging`.
- [PR #101](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/101): promoción a `main`.

Esta corrección no modificó código de aplicación, contratos API, migraciones, credenciales ni la etiqueta `v1.2.0`. La presente actualización documental se realiza después de publicar `v1.2.1` y no forma parte del commit etiquetado.

## [v1.2.0] - 2026-11-07

La etiqueta `v1.2.0` apunta al commit `c979530401c3b004907cedc29920e4b0a1e9a168` de `main`, integrado mediante el PR [#92](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/92). [Release publicada](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/releases/tag/v1.2.0) y [CI de `main` satisfactoria](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/actions/runs/42278117887). La publicación identifica el código; no acredita por sí sola respaldo, migración ni correspondencia del despliegue en Railway.

### Added

- Reasignación controlada de responsables de consultas con historial inmutable, validaciones de autorización y pruebas asociadas.
- Scripts de creación, verificación y recuperación para el historial de reasignaciones, con ejecución manual documentada.
- Artefacto de migración Flyway separado del backend web: el migrador ejecuta operaciones controladas y el runtime valida el historial y esquema sin aplicar DDL.
- Política de ciclo de vida para operaciones sobre perfiles y cobertura de pruebas para conflictos, concurrencia y reactivación.
- Pruebas de expiración de sesión y continuidad de operaciones autenticadas.

### Changed

- Serialización de cambios de credenciales, recuperación de contraseña, estados de cuenta y transiciones de perfil para reducir pérdidas de actualización concurrentes.
- Manejo frontend de conflictos en operaciones de perfiles.
- Actualización de Next.js de `^16.2.6` a `^16.4.1`; el lockfile resuelve `16.4.1`.
- Documentación técnica de autenticación, perfiles, consultas, migraciones, pruebas, seguridad y despliegue alineada con la implementación de esta versión.

### Security

- El workflow de GitHub Actions de esta versión declara `permissions: contents: read` para limitar el token del workflow a lectura del repositorio.
- La documentación diferencia los resultados históricos de auditoría de dependencias de las comprobaciones ejecutadas por integración continua.

## [v1.1.0] - 2026-10-05

La etiqueta histórica `v1.1.0` apunta al commit `c02f66d04b1baabb6a29d01e5768fbad9ed93d9c`. Este identificador permanece fijo aunque la rama `main` avance.

### Added

- Paginación, búsqueda y ordenamiento del lado del servidor en módulos operativos.
- Gestión documental versionada con validación de integridad, recuperación y controles de concurrencia.
- Consulta agregada de documentos del expediente y generación autorizada de la ficha integral en PDF.
- Autogestión de datos de contacto y contraseña desde el perfil propio.

### Changed

- Flujos frontend para paginación, carga, descarga y consulta de documentos.
- Workflow de integración continua para validar pushes y Pull Requests de las ramas permanentes.

La etiqueta identifica el código versionado en `main`, pero no demuestra por sí sola que ese commit esté desplegado en producción.

## [v1.0.1] - 2026-09-15

La etiqueta `v1.0.1` apunta al commit `337d57949a4513857976ddc21acf0405968f7696`, contenido en el historial de `main`.

### Security

- Restauración de `frontend/postcss.config.mjs` a una configuración declarativa, eliminando instrucciones ejecutables no autorizadas.

La etiqueta identifica un commit del historial de `main`, pero no demuestra por sí sola que ese commit esté desplegado en producción.
