# Pruebas del sistema

## Propósito

El backend incluye pruebas automatizadas para carga del contexto de aplicación, reglas críticas de negocio, validadores y estrategias de seguridad. Estas pruebas sirven como soporte técnico para validar comportamientos funcionales importantes del sistema.

## Ubicación

Las pruebas backend se encuentran en:

```text
backend/app/src/test/java
```

El frontend cuenta con configuración de Playwright en:

```text
frontend/playwright.config.js
```

y scripts asociados en `frontend/package.json`.

## Pruebas backend identificadas

| Prueba | Propósito |
|---|---|
| `ConsultaEstadoServiceTest` | Valida cierre de consulta, pendientes operativos, archivo, desarchivo y bloqueo de operaciones en consultas cerradas o archivadas. |
| `ConsultaCambioEstructuralValidatorTest` | Valida protección de datos estructurales cuando una consulta ya tiene actividad asociada. |
| `ConsultaResponsableOperacionServiceTest` | Valida bloqueo de desactivación de asesor, estudiante o monitor con consultas operativas. |
| `ConsultaReasignacionDTOTest` | Valida campos obligatorios, identificadores positivos y versión no negativa del contrato de reasignación. |
| `ConsultaControllerAuthorizationTest` | Verifica que el endpoint de reasignación exige `ASIGNAR_RESPONSABLES_CONSULTA`. |
| `ConsultaAccessServiceReasignacionTest` | Valida permiso, restricción del perfil estudiante y alcance efectivo sobre la consulta. |
| `ConsultaValidatorReasignacionTest` | Valida compatibilidad entre área, asesor y estudiante después de una reasignación. |
| `ConsultaReasignacionServiceTest` | Valida la orquestación, el cambio de un único rol, la conservación de relaciones y la propagación de errores históricos. |
| `JpaConsultaReasignacionHistorialWriterPostgreSqlTest` | Verifica campos, eventos sucesivos, restricciones, inmutabilidad y participación transaccional en PostgreSQL. |
| `ConsultaReasignacionEndpointPostgreSqlTest` | Ejecuta el endpoint con Spring Security, JWT, CSRF, JPA y PostgreSQL; cubre autorización, rollback y concurrencia. |
| `EstudianteSesionExpiradaPostgreSqlTest` | Comprueba con la cadena real de Spring Security que la creación de estudiante y cuenta solo ocurre con JWT y CSRF válidos, y que los intentos rechazados no persisten datos. |
| `PerfilEstudianteReactivacionPostgreSqlTest` | Caracteriza identidad compartida, reactivación, autenticación, autorización, desactivación y cambio explícito entre perfiles ESTUDIANTE y ASESOR sobre PostgreSQL real. |
| `GlobalExceptionHandlerReasignacionTest` | Comprueba la respuesta segura cuando el historial obligatorio no está disponible. |
| `ProcesoValidatorTest` | Valida radicado condicional: permitido en pendiente y obligatorio en estados finales. |
| `SeguimientoValidatorTest` | Valida que `notificarEstudiante=true` requiera estudiante asignado y activo. |
| `SeguimientoRespuestaValidatorTest` | Valida decisiones sobre respuestas, incluyendo observación obligatoria al rechazar. |
| `PerfilEstadoServiceTest` | Valida delegación por Strategy para desactivar perfil actual. |
| `PerfilUsuarioResolverServiceTest` | Valida delegación por Strategy para resolver perfil activo del usuario. |
| `PerfilEstadoHandlerRegistryTest` | Valida registro y resolución de handlers de estado de perfil. |
| `PerfilUsuarioActivoResolverRegistryTest` | Valida registro y resolución de resolvers de perfil activo. |
| `UsuarioSistemaPerfilEstadoServiceTest` | Valida sincronización entre perfil y usuario del sistema. |
| `LegalCasesApplicationTests.contextLoads()` | Verifica que el contexto principal de Spring Boot pueda cargarse durante la ejecución de pruebas. |

`LegalCasesApplicationTests.contextLoads()` utiliza `@SpringBootTest` para validar la carga del contexto de la aplicación. Las demás pruebas listadas se orientan a reglas funcionales, validadores y estrategias del backend.

## Reglas críticas cubiertas

Las pruebas actuales cubren reglas como:

- una consulta no se cierra sin resultado;
- una consulta no se cierra con procesos, seguimientos, respuestas, notificaciones o conciliaciones pendientes;
- una consulta cerrada o archivada no permite operaciones operativas;
- una consulta con actividad asociada protege sus datos estructurales;
- un proceso pendiente puede existir sin radicado;
- un proceso final requiere radicado;
- una respuesta rechazada requiere observación;
- un seguimiento visible para estudiante requiere estudiante asignado y activo;
- asesor, estudiante y monitor no se desactivan si tienen consultas operativas;
- una reasignación modifica solo el rol solicitado y conserva los demás datos de la consulta;
- el historial registra el responsable anterior y el nuevo sin sobrescribir eventos sucesivos;
- consultas cerradas o archivadas, responsables inválidos, motivos vacíos y versiones desactualizadas no producen eventos de éxito;
- un fallo real al insertar el historial revierte el responsable y la versión de la consulta;
- dos transacciones con la misma versión no producen actualizaciones perdidas: solo una se confirma y solo esa genera historial;
- los accesos derivados cambian con la asignación vigente sin ampliar el alcance a consultas ajenas;
- el cambio y resolución de perfil usan estrategias registradas;
- el estado de perfil y usuario del sistema se sincroniza.

## Ejecución de pruebas backend

Desde la carpeta del backend:

```bash
cd backend/app
./mvnw test
```

En Windows:

```powershell
cd backend/app
.\mvnw.cmd test
```

## Integración con PostgreSQL y seguridad HTTP

Las pruebas `JpaConsultaReasignacionHistorialWriterPostgreSqlTest`, `ConsultaReasignacionEndpointPostgreSqlTest`, `EstudianteSesionExpiradaPostgreSqlTest` y `PerfilEstudianteReactivacionPostgreSqlTest` usan PostgreSQL 16 mediante Testcontainers. El contenedor es efímero y crea exclusivamente el esquema de prueba; no utiliza una base persistente del desarrollador.

La prueba del endpoint carga el contexto completo de Spring Boot y ejecuta `MockMvc` a través de la cadena real de seguridad. Obtiene el token CSRF desde `/api/auth/csrf`, usa el JWT en cookie y verifica respuestas `200`, `400`, `401`, `403` y `409` según el escenario.

La migración `backend/app/db/migration/V20261011_01__create_consulta_reasignacion_historial.sql` se aplica dentro del contenedor antes de comprobar atomicidad e integridad. Las pruebas fuerzan una violación de clave foránea real para acreditar rollback y coordinan dos transacciones independientes para comprobar el bloqueo optimista sin depender solo de mocks.

Ejecución focalizada:

```bash
cd backend/app
./mvnw -Dtest=ConsultaReasignacionEndpointPostgreSqlTest,JpaConsultaReasignacionHistorialWriterPostgreSqlTest test
```

Se validó además el contrato mediante una operación manual: una solicitud vigente obtuvo `200 OK` y persistió el evento histórico; un segundo intento con la versión anterior obtuvo `409 Conflict` sin modificar nuevamente la consulta ni añadir otro evento.

### Expiración de sesión durante la creación de usuarios

`EstudianteSesionExpiradaPostgreSqlTest` ejecuta `POST /api/estudiantes` mediante `MockMvc`, el contexto completo de Spring Boot, la cadena real de Spring Security y PostgreSQL 16 de Testcontainers. No sustituye la autenticación con `@WithMockUser`.

La prueba cubre:

- JWT vigente y CSRF válido: responde `201` y persiste un estudiante, su cuenta y la relación entre ambos;
- JWT ausente con CSRF válido: responde `401` y no persiste datos;
- JWT correctamente firmado pero expirado con CSRF válido: responde `401` y no persiste datos;
- JWT inválido con CSRF válido: responde `401` y no persiste datos;
- JWT vigente sin CSRF: responde `403` y no persiste datos;
- sesión inicialmente válida cuyo JWT vence antes de enviar el formulario: responde `401` y no persiste datos;
- verificación final de perfiles, cuentas, relaciones y auditorías para separar rechazos legítimos de una creación exitosa.

Los escenarios obtienen el CSRF mediante `GET /api/auth/csrf` y transportan el JWT en la cookie `access_token`. El caso de expiración posterior mantiene un CSRF válido y usa un JWT de vigencia corta, por lo que distingue un rechazo de autenticación de un rechazo CSRF. Esta es una prueba HTTP automatizada, no una ejecución visual del frontend.

La infraestructura heredada de `PostgreSqlIntegrationTest` dirige las propiedades de datasource al contenedor efímero. Antes de preparar datos, la prueba confirma que la conexión activa coincide con el Testcontainer y que el catálogo es `legal_cases_test`.

Ejecución focalizada verificada desde `backend/app`:

```powershell
.\mvnw.cmd '-Dtest=EstudianteSesionExpiradaPostgreSqlTest' test
```

La ejecución local focalizada registró 7 pruebas aprobadas, sin fallos, errores ni pruebas omitidas. También se ejecutaron 13 pruebas existentes relacionadas con CSRF, respuestas de seguridad, sesión autenticada, rotación CSRF y registro de usuarios; las 13 fueron aprobadas. Estos resultados no equivalen a una ejecución de la suite completa.

### Reactivación y cambio explícito de perfiles

`PerfilEstudianteReactivacionPostgreSqlTest` caracteriza la interacción entre una cuenta compartida y sus perfiles ESTUDIANTE y ASESOR. Usa el contexto completo de Spring Boot, `MockMvc`, la cadena real de Spring Security, JWT en cookie, CSRF válido, JPA y PostgreSQL 16 mediante Testcontainers. No sustituye la autenticación con `@WithMockUser`.

Los casos P01 a P12 cubren:

- identidad compartida comprobada mediante `usuario_sistema_id` y diferenciación frente a cuentas distintas;
- estado inicial con ASESOR como `tipoPerfilActual` y estudiante histórico inactivo;
- reactivación directa del estudiante y comprobación de ambos perfiles activos;
- respuesta de `/api/auth/me` y permisos efectivos del rol asesor;
- rechazo por falta de permiso y rechazo por falta de alcance como controles diferentes;
- cambio explícito ASESOR a ESTUDIANTE, conservación de cuenta, actualización de rol y perfil actual, e historial;
- desactivación directa del estudiante no actual y del asesor actual;
- rehabilitación indirecta de una cuenta inactiva al reactivar un perfil histórico;
- rechazo de autenticación cuando el perfil indicado por `tipoPerfilActual` está inactivo;
- bloqueo y rollback del cambio explícito cuando el asesor tiene consultas operativas;
- aislamiento respecto de cuentas y perfiles con información sintética semejante.

Antes de crear datos, la prueba confirma que el datasource activo coincide exactamente con la URL del Testcontainer, que el catálogo es `legal_cases_test`, que el producto es PostgreSQL y que su versión mayor es 16.

Ejecución focalizada verificada desde `backend/app`:

```powershell
.\mvnw.cmd '-Dtest=PerfilEstudianteReactivacionPostgreSqlTest' test
```

La ejecución local del diagnóstico registró:

- 12 pruebas de integración de reactivación de perfiles aprobadas;
- 24 pruebas unitarias relacionadas con estado, resolución y cambio de perfil aprobadas;
- 7 pruebas de seguridad por expiración de sesión aprobadas;
- 43 pruebas en total, sin fallos, errores ni omisiones.

Este total corresponde a ejecuciones focalizadas locales y no a CI ni a la suite completa.

Los casos P01 a P12 son pruebas de caracterización: acreditan de forma reproducible lo que hace la implementación actual, incluso cuando el resultado revela un riesgo o un vacío funcional. Un resultado PASS no afirma que la conducta observada sea la política de negocio correcta.

Si se formalizan nuevas reglas de exclusividad, rechazo de activación histórica o control independiente de la cuenta, algunas expectativas actuales deberán cambiar. En ese momento se requerirán pruebas de regresión que esperen el nuevo contrato, por ejemplo `409 Conflict` sin mutaciones para una activación incompatible.

El detalle de escenarios, resultados HTTP, persistencia, riesgos, vacíos y propuesta funcional se encuentra en `doc/evidencias/diagnostico-reactivacion-perfiles.md`.

## Ejecución frontend

El frontend define scripts de prueba en `package.json`:

```bash
npm run test
npm run test:ui
npm run test:debug
npm run test:headed
```

## Criterio documental

La documentación de pruebas se limita a pruebas presentes en el código fuente o configuraciones existentes. Los documentos de detalle pueden ampliar escenarios manuales o de integración cuando existan como archivos del proyecto.
