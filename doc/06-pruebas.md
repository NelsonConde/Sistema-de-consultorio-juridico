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
| `PerfilEstudianteReactivacionPostgreSqlTest` | Verifica 53 escenarios (P01-P53) de identidad, política de estado, autenticación, autorización, cambios explícitos y concurrencia para los cinco perfiles sobre PostgreSQL real. |
| `GlobalExceptionHandlerReasignacionTest` | Comprueba la respuesta segura cuando el historial obligatorio no está disponible. |
| `ProcesoValidatorTest` | Valida radicado condicional: permitido en pendiente y obligatorio en estados finales. |
| `SeguimientoValidatorTest` | Valida que `notificarEstudiante=true` requiera estudiante asignado y activo. |
| `SeguimientoRespuestaValidatorTest` | Valida decisiones sobre respuestas, incluyendo observación obligatoria al rechazar. |
| `PerfilEstadoServiceTest` | Valida delegación por Strategy para desactivar perfil actual. |
| `PerfilUsuarioResolverServiceTest` | Valida delegación por Strategy para resolver perfil activo del usuario. |
| `PerfilEstadoHandlerRegistryTest` | Valida registro y resolución de handlers de estado de perfil. |
| `PerfilUsuarioActivoResolverRegistryTest` | Valida registro y resolución de resolvers de perfil activo. |
| `PerfilOperacionEstadoValidatorTest` | Valida la política central para activación y desactivación directa de los cinco tipos de perfil. |
| `UsuarioSistemaPerfilEstadoServiceTest` | Conserva cobertura del componente legado; los cinco command services ya no lo usan en operaciones directas de perfil. |
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
- el estado del perfil y la habilitación de la cuenta son independientes;
- las operaciones directas incompatibles responden `409` sin efectos parciales;
- el cambio explícito serializa por cuenta, conserva historial y rechaza estados con múltiples perfiles activos sin sanearlos.

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

`PerfilEstudianteReactivacionPostgreSqlTest` verifica la política implementada para los cinco tipos de perfil. Usa el contexto completo de Spring Boot, `MockMvc`, la cadena real de Spring Security, JWT en cookie, CSRF válido, JPA y PostgreSQL 16 mediante Testcontainers. No sustituye la autenticación con `@WithMockUser`.

Los casos P01 a P12 cubren:

- identidad compartida comprobada mediante `usuario_sistema_id` y diferenciación frente a cuentas distintas;
- estado inicial con ASESOR como `tipoPerfilActual` y estudiante histórico inactivo;
- rechazo de reactivación, desactivación y rehabilitación indirecta del estudiante histórico;
- conservación de `/api/auth/me`, la cuenta, el rol y los permisos del perfil asesor tras los rechazos;
- rechazo por falta de permiso y rechazo por falta de alcance como controles diferentes;
- cambio explícito ASESOR a ESTUDIANTE, conservación de cuenta, actualización de rol y perfil actual, e historial;
- rechazo de desactivación directa del estudiante histórico y del asesor actual;
- rechazo de autenticación cuando el perfil indicado por `tipoPerfilActual` está inactivo;
- bloqueo y rollback del cambio explícito cuando el asesor tiene consultas operativas;
- aislamiento respecto de cuentas y perfiles con información sintética semejante.

P13 a P44 extienden la regresión a:

- independencia de una cuenta suspendida respecto de la activación del perfil actual;
- protección de `PATCH /activo` y `DELETE` para ESTUDIANTE, ASESOR, MONITOR, CONCILIADOR y ADMINISTRATIVO;
- rechazo de perfiles históricos y de coexistencia con otro perfil activo;
- transiciones explícitas de ida y regreso, con rol, `tipoPerfilActual` e historial coherentes;
- restricciones por consultas operativas y continuidad del último administrador habilitado.

P45 a P49 y P51 a P53 son pruebas coordinadas con transacciones independientes y tiempos máximos controlados. P50 completa la revisión de integridad con un estado preexistente incompatible:

| Caso | Concurrencia o estado previo | Resultado verificado |
|---|---|---|
| P45 | Dos cambios explícitos simultáneos sobre la misma cuenta. | Las transiciones se serializan por cuenta; el estado final y el historial quedan coherentes, sin bloqueo indefinido. |
| P46 | Activación histórica durante un cambio explícito. | La activación espera el bloqueo y luego se rechaza; no deja cambios parciales ni historial adicional. |
| P47 | Activación del perfil actual concurrente con un cambio explícito. | Ambas operaciones se ordenan atómicamente y la transición conserva un único perfil activo. |
| P48 | Suspensión de cuenta concurrente con activación directa. | La cuenta permanece suspendida; la operación de perfil no la rehabilita. |
| P49 | Transición desde ADMINISTRATIVO concurrente con retiro de directora. | No hay interbloqueo y se conserva la continuidad administrativa. |
| P50 | Cambio explícito con múltiples perfiles activos preexistentes. | Responde `409`, no genera historial ni sanea automáticamente los perfiles. |
| P51 | Transición no administrativa hacia ADMINISTRATIVO concurrente con suspensión de la misma cuenta. | Las operaciones se serializan sin interbloqueo ni pérdida de actualización; la cuenta termina suspendida, con rol y perfil actual administrativos, un único perfil activo y un único historial de transición. |
| P52 | Cambio autenticado de contraseña concurrente con transición explícita hacia ADMINISTRATIVO. | Ambas transacciones se serializan por la cuenta; se conserva la nueva contraseña, el rol y perfil de destino, un único perfil activo y un único historial. |
| P53 | Restablecimiento de contraseña concurrente con suspensión de la misma cuenta. | Se conserva la nueva contraseña y la cuenta termina suspendida sin alterar rol, perfil vigente ni historial; el token consumido no puede reutilizarse. |

Antes de crear datos, la prueba confirma que el datasource activo coincide exactamente con la URL del Testcontainer, que el catálogo es `legal_cases_test`, que el producto es PostgreSQL y que su versión mayor es 16.

Ejecución focalizada verificada desde `backend/app`:

```powershell
.\mvnw.cmd '-Dtest=PerfilEstudianteReactivacionPostgreSqlTest' test
```

La ejecución histórica del diagnóstico registró:

- 12 pruebas de integración de reactivación de perfiles aprobadas;
- 24 pruebas unitarias relacionadas con estado, resolución y cambio de perfil aprobadas;
- 7 pruebas de seguridad por expiración de sesión aprobadas;
- 43 pruebas en total, sin fallos, errores ni omisiones.

Este total de 43 corresponde al diagnóstico original y no a la regresión actual, a CI ni a la suite completa.

P01 a P12 comenzaron como pruebas de caracterización del comportamiento anterior. Sus resultados originales se conservan en `doc/evidencias/diagnostico-reactivacion-perfiles.md`; las expectativas automatizadas fueron actualizadas después de implementar la política aprobada. Un resultado PASS demuestra el comportamiento esperado por la prueba, no sustituye la revisión funcional o de QA.

La ejecución focalizada histórica de `PerfilEstudianteReactivacionPostgreSqlTest` registró 51 pruebas aprobadas (P01-P51), sin fallos, errores ni omisiones. Incluye Java 21, PostgreSQL 16 Testcontainers, seguridad HTTP real, concurrencia coordinada en P45 a P49 y P51, y el rechazo de inconsistencia en P50. Esta ejecución precede a la incorporación de P52 y P53.

Una segunda ejecución focalizada e independiente cubrió continuidad administrativa, repositorios de invariantes, cambio explícito, suspensión y sesiones, validadores y operaciones de los cinco perfiles: 72 pruebas aprobadas, sin fallos, errores ni omisiones. El resultado agregado de ambas ejecuciones fue 123/123, pero no corresponde a una única invocación de Maven, a la suite completa ni a CI.

Tras incorporar la protección preventiva de credenciales, una ejecución focalizada posterior y separada cubrió `PerfilEstudianteReactivacionPostgreSqlTest` (53), `EstudianteSesionExpiradaPostgreSqlTest` (7) y `AuthControllerCsrfTest` (3): 63/63 aprobadas, sin fallos, errores ni omisiones, con Java 21 y PostgreSQL 16 Testcontainers donde correspondía. P52 y P53 verificaron persistencia real, transacciones independientes y coordinación determinista; este resultado no reemplaza ni se suma como una única suite a las ejecuciones históricas anteriores.

En una ejecución puntual adicional de P52 y P53, ambas pruebas de concurrencia de credenciales aprobaron (2/2), con Java 21 y PostgreSQL 16 Testcontainers. Este resultado solo acredita esas dos pruebas y no representa una ejecución conjunta de 53/53.

Una ejecución histórica de la suite de backend registró 528 pruebas: 527 aprobadas y una fallida, `LoggingSafetyConfigurationTest`, asociada a la configuración local protegida. Ese resultado pertenece a esa ejecución y no se presenta como la validación posterior. El archivo protegido no se modificó como parte de esta implementación.

El detalle de escenarios, resultados HTTP, persistencia, riesgos, vacíos y propuesta funcional se encuentra en `doc/evidencias/diagnostico-reactivacion-perfiles.md`.

### Validaciones aisladas posteriores

Los resultados siguientes pertenecen a ejecuciones focalizadas y aisladas; no se suman para afirmar que una única suite completa haya pasado:

| Área | Ejecución | Resultado reportado |
|---|---|---|
| Backend focalizado anterior | Selección de pruebas de perfiles, servicios, validación y concurrencia | 116/116 aprobadas en su ejecución histórica. |
| Reactivación y concurrencia de perfiles, ejecución histórica | `PerfilEstudianteReactivacionPostgreSqlTest` P01-P51 con Java 21 y PostgreSQL 16 efímero | 51/51 aprobadas, incluidos P45 a P51. |
| Regresiones relacionadas posteriores | Continuidad administrativa, repositorios de invariantes, cambio explícito, suspensión y sesiones, validadores y operaciones de perfiles | 72/72 aprobadas en una segunda ejecución independiente. Total aritmético de ambas ejecuciones posteriores: 123/123. |
| Credenciales y concurrencia | Perfiles/concurrencia P01-P53, sesiones expiradas y contrato CSRF, en ejecución focalizada posterior | 63/63 aprobadas: 53 de perfiles, 7 de sesión y 3 de CSRF; no equivale a suite Maven completa ni a CI. |
| Concurrencia de credenciales, ejecución puntual | P52 y P53 de `PerfilEstudianteReactivacionPostgreSqlTest`, con Java 21 y PostgreSQL 16 Testcontainers | 2/2 aprobadas; resultado limitado a estos dos escenarios, no una ejecución conjunta de 53/53. |
| Recuperación de documentos | `DocumentBackupRestoreIT` contra LocalStack aislado y cuenta sintética | Aprobada. |
| Persistencia entre contenedores | `DocumentContainerPersistenceIT`, fases `WRITE`, `VERIFY` y `CLEANUP` | Las tres fases aprobadas en invocaciones separadas. |
| Vencimiento de URL prefirmada | `DocumentPresignedUrlExpirationIT` con LocalStack 4.14.0 y `S3_SKIP_SIGNATURE_VALIDATION=0` | URL vigente: HTTP 200; vencida: HTTP 403; renovada: HTTP 200 con los bytes esperados. |
| Seguridad de logging | `LoggingSafetyConfigurationTest` con Java 21 y `application.properties` sintético en una copia temporal | 3/3 aprobadas. Valida la prueba con configuración sintética; no valida el archivo privado original ni la configuración de CI. |
| Suite Maven ampliada posterior | Ejecución aislada con 529 pruebas contabilizadas | 526 aprobadas, cero fallos de aserción y 3 errores de ejecución: faltaban `DOCUMENT_S3_ENDPOINT`, `DOCUMENT_PHASE` y LocalStack en `localhost:4566`. Los errores impiden declarar verde esa ejecución; las pruebas operacionales se aprobaron después, por separado. |

No se reportó una nueva ejecución completa de Maven después de las ejecuciones aisladas que resolvieron esos pre-requisitos. Por ello, no hay evidencia de una suite backend completa, limpia y aprobada en una sola corrida.

## Ejecución frontend

El frontend define scripts de prueba en `package.json`:

```bash
npm run test
npm run test:ui
npm run test:debug
npm run test:headed
```

La validación local de esta implementación registró:

| Comando | Resultado |
|---|---|
| `npm run test` | 26 de 26 pruebas Playwright aprobadas, incluidas las que distinguen `CONFLICTO_OPERACION_PERFIL` de conflictos de concurrencia. |
| `npm run build` | Build de producción satisfactorio. |
| `npm run lint` | Código 0, sin errores: 186 advertencias no bloqueantes y 5 diagnósticos informativos. |

Estas cifras corresponden a ejecuciones locales, no a resultados de CI ni a una aprobación de QA.

La validación frontend posterior a la actualización compatible de dependencias usó Node.js 22 y Next.js 16.3.8: Playwright pasó 26/26, el build de producción terminó correctamente y el lint finalizó con código 0, cero errores y 186 advertencias preexistentes. La auditoría de dependencias de esa ejecución reportó 7 vulnerabilidades altas y 2 moderadas en el árbol completo, y 0 vulnerabilidades en el árbol de dependencias instalado omitiendo `devDependencies` (`npm audit --omit=dev`); estos resultados y los riesgos residuales se detallan en `doc/02-configuracion-seguridad.md`. El resultado de `npm audit --omit=dev` no garantiza por sí solo la seguridad del bundle generado, la configuración de despliegue ni la infraestructura de ejecución.

La comprobación aislada de lint usó Node.js 22 y npm 10.9.4. Tanto `npm ci` como `npm run lint` finalizaron con código 0. El frontend actual analizó 150 archivos y registró 186 advertencias, 5 diagnósticos informativos y ningún error. La comparación reproducible contra `HEAD`, con 149 archivos analizados, registró las mismas 186 advertencias y los mismos 5 diagnósticos informativos, también sin errores y con código 0. No se identificaron diagnósticos nuevos en los cambios locales. Las advertencias existentes no bloquean el comando de lint ni acreditan por sí solas una ejecución integral satisfactoria de Maven o de CI.

## Alcance de CI

El workflow versionado configura Java 21 y ejecuta `./mvnw test` y el empaquetado backend; para frontend configura Node.js 22, ejecuta `npm ci`, `npm run build` y `npm run lint`. No incluye un arranque explícito de LocalStack, la generación de la configuración sintética usada por `LoggingSafetyConfigurationTest`, `npm audit` ni Playwright. Las pruebas operacionales `*IT` se ejecutaron explícitamente y en infraestructura efímera; las validaciones locales descritas arriba no acreditan un resultado de CI.

## Criterio documental

La documentación de pruebas se limita a pruebas presentes en el código fuente o configuraciones existentes. Los documentos de detalle pueden ampliar escenarios manuales o de integración cuando existan como archivos del proyecto.
