# Registro de cambios — Sistema de Gestión de Casos Jurídicos

Este archivo registra los cambios técnicos del Sistema de Gestión de Casos Jurídicos. Se utiliza la convención de versiones `vMAJOR.MINOR.PATCH` para identificar las etiquetas del producto.

## [Unreleased]

Los siguientes cambios están presentes en `develop` y no se presentan como publicados en `main` ni como desplegados en producción.

### Added

- Reasignación controlada de responsables de consultas con historial inmutable, validaciones de autorización y pruebas asociadas.
- Scripts de creación, verificación y recuperación para el historial de reasignaciones, con ejecución manual documentada.
- Política de ciclo de vida para operaciones sobre perfiles y cobertura de pruebas para conflictos, concurrencia y reactivación.
- Pruebas de expiración de sesión y continuidad de operaciones autenticadas.

### Changed

- Serialización de cambios de credenciales, recuperación de contraseña, estados de cuenta y transiciones de perfil para reducir pérdidas de actualización concurrentes.
- Manejo frontend de conflictos en operaciones de perfiles.
- Actualización de Next.js declarada en `develop` de `^16.2.6` a `^16.4.1`; el lockfile resuelve `16.4.1`.
- Documentación técnica de autenticación, perfiles, consultas, migraciones, pruebas, seguridad y despliegue alineada con la implementación de `develop`.

### Security

- El workflow de GitHub Actions declara `permissions: contents: read` en `develop` para limitar el token del workflow a lectura del repositorio.
- La documentación diferencia los resultados históricos de auditoría de dependencias de las comprobaciones ejecutadas por integración continua.

## [v1.1.0] - 2026-10-05

La etiqueta `v1.1.0` apunta al commit `c02f66d04b1baabb6a29d01e5768fbad9ed93d9c`, que coincide con la referencia actual de `main` verificada para este registro.

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
