# Evidencia técnica — SCRUM-272

## Entorno

| Elemento | Valor |
|---|---|
| Historia | SCRUM-272 — BE: paginar usuarios y cinco tipos de perfil |
| Rama auditada | `feature/scrum-272-user-profile-pagination` |
| HEAD auditado | `632aeb8` (`feat: complete student profile pagination`) |
| Fecha de auditoría final | 2026-09-04 |
| Base de la evidencia | Código local real, historial Git y clases de prueba existentes |
| Maven en Bloque D | No ejecutado por restricción expresa |
| Gates A/B/C | Reportados por el usuario como ejecutados correctamente |
| Swagger/OpenAPI | Verificación estática del contrato de código; aplicación no levantada |
| Métricas P95/payload | No medidas en SCRUM-272 |

Al iniciar la auditoría, `git status --short` no reportó cambios pendientes: A, B y C estaban committeados y no había cambios Java locales. Esta evidencia es el único archivo creado por el Bloque D.

## Alcance técnico

Los seis recursos ofrecen un listado raíz paginado con página pública uno-basada, tamaño entre 1 y 50, búsqueda opcional normalizada, ordenamiento por lista blanca, dirección `asc`/`desc`, filtro `activo` y respuesta `PageResponseDTO<ResumenDTO>`. Cada consulta usa `Page<ResumenProjection>` y proyecta únicamente campos escalares.

“Uniforme” se interpreta como un contrato base común de paginación; no obliga a introducir filtros de dominio inexistentes. Por ello, `tipoPerfil` solo aplica a UsuarioSistema y `tipoConciliador` solo aplica a Conciliador.

### UsuarioSistema

- Endpoint raíz: `GET /api/usuarios-sistema`.
- Respuesta: `PageResponseDTO<UsuarioSistemaResumenDTO>`.
- Filtros: `search`, `activo` y `tipoPerfil`.
- Búsqueda case-insensitive sobre `username` y nombre del rol.
- Orden permitido: `id`, `username`, `activo`, `rolNombre`, `tipoPerfil`.
- Repositorio: `Page<UsuarioSistemaResumenProjection>`, JPQL con `countQuery` equivalente.
- Detalle y operaciones previas preservados: `/activos`, `/{id}`, cambio de estado y cambios de perfil.

### Administrativo

- Endpoint raíz: `GET /api/administrativos`.
- Respuesta: `PageResponseDTO<AdministrativoResumenDTO>`.
- Filtros: `search` y `activo`; no existe clasificación funcional adicional de tipo.
- Búsqueda case-insensitive sobre `nombre`, `documento`, `email`, `usuario` y `codigo`.
- Orden permitido: `id`, `nombre`, `documento`, `email`, `usuario`, `codigo`, `activo`, `directora`.
- Repositorio: `Page<AdministrativoResumenProjection>`, JPQL con `countQuery` equivalente.
- Detalle y operaciones previas preservados, incluidos `/activos` y `/directoras`.

### Asesor

- Endpoint raíz: `GET /api/asesores`.
- Respuesta: `PageResponseDTO<AsesorResumenDTO>`.
- Filtros: `search` y `activo`; no existe clasificación funcional adicional de tipo.
- Búsqueda case-insensitive sobre `nombre`, `documento`, `email`, `usuario` y `codigo`.
- Orden permitido: `id`, `nombre`, `documento`, `email`, `usuario`, `codigo`, `activo`, `areaNombre`.
- Repositorio: `Page<AsesorResumenProjection>`, JPQL con `countQuery` equivalente.
- Detalle, `/activos` y operaciones CRUD preservados.

### Monitor

- Endpoint raíz: `GET /api/monitores`.
- Respuesta: `PageResponseDTO<MonitorResumenDTO>`.
- Filtros: `search` y `activo`; no existe clasificación funcional adicional de tipo.
- Búsqueda case-insensitive sobre `nombre`, `documento`, `email`, `usuario` y `codigo`.
- Orden permitido: `id`, `nombre`, `documento`, `email`, `usuario`, `codigo`, `activo`, `sedeNombre`.
- Repositorio: `Page<MonitorResumenProjection>`, JPQL con `countQuery` equivalente.
- Detalle, `/activos` y operaciones CRUD preservados.

### Conciliador

- Endpoint raíz: `GET /api/conciliadores`.
- Respuesta: `PageResponseDTO<ConciliadorResumenDTO>`.
- Filtros: `search`, `activo` y `tipoConciliador`.
- Búsqueda case-insensitive sobre `nombre`, `documento`, `email`, `usuario` y `codigo`.
- Orden permitido: `id`, `nombre`, `documento`, `email`, `usuario`, `codigo`, `activo`, `tipoConciliador`, `sedeNombre`.
- Repositorio: `Page<ConciliadorResumenProjection>`, JPQL con `countQuery` equivalente.
- Detalle, `/activos`, método legado `listar()` y operaciones CRUD preservados.

### Estudiante

- Endpoint raíz: `GET /api/estudiantes`.
- Respuesta: `PageResponseDTO<EstudianteResumenDTO>`.
- Filtros: `search` y `activo`; no existe clasificación funcional adicional de tipo.
- Búsqueda case-insensitive sobre `nombre`, `documento`, `email`, `usuario` y `codigo`.
- Orden permitido: `id`, `nombre`, `documento`, `email`, `usuario`, `codigo`, `activo`, `sedeNombre`, `asesorNombre`, `conciliacion`.
- Repositorio: `Page<EstudianteResumenProjection>`, JPQL con `countQuery` equivalente y scope de asesor dentro de la consulta.
- Detalle, `/activos`, `/conciliacion`, `/activos/asesor/{asesorId}`, método legado `listar()` y operaciones existentes preservados.

## Contratos Swagger/OpenAPI

El proyecto declara `springdoc-openapi-starter-webmvc-ui`. Las firmas públicas de los controladores contienen tipos de retorno genéricos concretos y todos los `@RequestParam`, por lo que Springdoc puede inferir los siguientes contratos sin agregar anotaciones cosméticas. Esta conclusión es estática: no se levantó la aplicación ni se capturó un JSON OpenAPI.

| Endpoint | Response | Query parameters | DTO de contenido |
|---|---|---|---|
| `GET /api/usuarios-sistema` | `PageResponseDTO<UsuarioSistemaResumenDTO>` | `search` opcional; `page=1`; `size=10`; `sortBy=id`; `direction=desc`; `activo` opcional; `tipoPerfil` opcional | `UsuarioSistemaResumenDTO` |
| `GET /api/administrativos` | `PageResponseDTO<AdministrativoResumenDTO>` | `search` opcional; `page=1`; `size=10`; `sortBy=id`; `direction=desc`; `activo` opcional | `AdministrativoResumenDTO` |
| `GET /api/asesores` | `PageResponseDTO<AsesorResumenDTO>` | `search` opcional; `page=1`; `size=10`; `sortBy=id`; `direction=desc`; `activo` opcional | `AsesorResumenDTO` |
| `GET /api/monitores` | `PageResponseDTO<MonitorResumenDTO>` | `search` opcional; `page=1`; `size=10`; `sortBy=id`; `direction=desc`; `activo` opcional | `MonitorResumenDTO` |
| `GET /api/conciliadores` | `PageResponseDTO<ConciliadorResumenDTO>` | `search` opcional; `page=1`; `size=10`; `sortBy=id`; `direction=desc`; `activo` opcional; `tipoConciliador` opcional | `ConciliadorResumenDTO` |
| `GET /api/estudiantes` | `PageResponseDTO<EstudianteResumenDTO>` | `search` opcional; `page=1`; `size=10`; `sortBy=id`; `direction=desc`; `activo` opcional | `EstudianteResumenDTO` |

`PageResponseDTO<T>` expone `content`, `page`, `size`, `totalElements` y `totalPages`. `page` conserva el índice público solicitado; el servicio transforma internamente `page - 1` para Spring Data.

## Tabla de cobertura

| Módulo | Paginación/totales | Filtros | Orden estable | Projection | Controller | Autorización | N+1 |
|---|---|---|---|---|---|---|---|
| UsuarioSistema | `UsuarioSistemaRepositoryJpaTest` | `UsuarioSistemaRepositoryJpaTest`, `UsuarioSistemaServiceTest` | Sí | Sí | `UsuarioSistemaControllerTest` | `UsuarioSistemaControllerAuthorizationTest` | Sí |
| Administrativo | `PerfilPaginadoRepositoryJpaTest` | `PerfilPaginadoRepositoryJpaTest`, `AdministrativoQueryServiceTest` | Sí | Sí | `PerfilPaginadoControllerTest` | `PerfilPaginadoControllerAuthorizationTest` | Sí |
| Asesor | `PerfilPaginadoRepositoryJpaTest` | `PerfilPaginadoRepositoryJpaTest`, `AsesorQueryServiceTest` | Sí | Sí | `PerfilPaginadoControllerTest` | `PerfilPaginadoControllerAuthorizationTest` | Sí |
| Monitor | `PerfilPaginadoRepositoryJpaTest` | `PerfilPaginadoRepositoryJpaTest`, `MonitorQueryServiceTest` | Sí | Sí | `PerfilPaginadoControllerTest` | `PerfilPaginadoControllerAuthorizationTest` | Sí |
| Conciliador | `PerfilPaginadoRepositoryJpaTest` | `PerfilPaginadoRepositoryJpaTest`, `ConciliadorQueryServiceTest` | Sí | Sí | `PerfilPaginadoControllerTest` | `PerfilPaginadoControllerAuthorizationTest` | Sí |
| Estudiante | `EstudiantePaginadoRepositoryJpaTest` | `EstudiantePaginadoRepositoryJpaTest`, `EstudianteQueryServicePaginadoTest` | Sí | Sí | `EstudiantePaginadoControllerTest` | `EstudiantePaginadoControllerAuthorizationTest` | Sí |

Las validaciones de servicio son uniformes:

- `page >= 1`.
- `size` entre 1 y 50.
- `search`: `null` permanece `null`; se aplica `trim`, se colapsan espacios, blank se vuelve `null` y se rechazan más de 100 caracteres.
- `sortBy`: lista blanca por módulo; `null`, blank y campos desconocidos se rechazan.
- `direction`: únicamente `asc` o `desc`, sin sensibilidad a mayúsculas; `null` y blank se rechazan.
- Los campos de texto usan orden case-insensitive; `Long`, `Boolean` y enum no lo usan.
- Si el orden principal no es `id`, se agrega `id ASC` como desempate estable.

## Privacidad de DTO resumen

| DTO | Campos | Secretos | Entidades JPA | Estado |
|---|---|---|---|---|
| `UsuarioSistemaResumenDTO` | `id`, `username`, `activo`, `rolId`, `rolNombre`, `tipoPerfil` | Ninguno | Ninguna | VERIFICADO |
| `AdministrativoResumenDTO` | `id`, `nombre`, `documento`, `email`, `usuario`, `codigo`, `activo`, `directora`, `sedeId`, `sedeNombre` | Ninguno | Ninguna | VERIFICADO |
| `AsesorResumenDTO` | `id`, `nombre`, `documento`, `email`, `usuario`, `codigo`, `activo`, `areaId`, `areaNombre`, `sedeId`, `sedeNombre` | Ninguno | Ninguna | VERIFICADO |
| `MonitorResumenDTO` | `id`, `nombre`, `documento`, `email`, `usuario`, `codigo`, `activo`, `sedeId`, `sedeNombre` | Ninguno | Ninguna | VERIFICADO |
| `ConciliadorResumenDTO` | `id`, `nombre`, `documento`, `email`, `usuario`, `codigo`, `activo`, `tipoConciliador`, `sedeId`, `sedeNombre` | Ninguno | Ninguna | VERIFICADO |
| `EstudianteResumenDTO` | `id`, `nombre`, `documento`, `email`, `usuario`, `codigo`, `activo`, `sedeId`, `sedeNombre`, `asesorId`, `asesorNombre`, `conciliacion` | Ninguno | Ninguna | VERIFICADO |

La búsqueda estática sobre DTO y projection resumen no encontró `password`, `passwordHash`, `token`, `secret`, `permisos`, `refreshToken`, `resetToken`, `credencial` ni propiedades de entidades `UsuarioSistema`, `Rol` o `Permiso`. Las pruebas de controller también afirman la ausencia de los campos sensibles en el JSON de los seis listados; UsuarioSistema comprueba además que no expone `perfilId`.

## Seguridad y autorización

Las authorities siguientes son las constantes reales usadas por `@PreAuthorize`; no se infieren nombres de roles:

| Endpoint raíz | Authorities admitidas por controller | Validación de servicio |
|---|---|---|
| UsuarioSistema | `VER_USUARIOS`, `GESTIONAR_USUARIOS` | Seguridad de método en controller |
| Administrativo | `VER_ADMINISTRADORES`, `GESTIONAR_ADMINISTRADORES`, `GESTIONAR_USUARIOS` | `AdministrativoAccessService.validarPuedeVerAdministradores()` exige administrador operativo |
| Asesor | `VER_ASESORES_MONITORES`, `GESTIONAR_ASESORES_MONITORES`, `GESTIONAR_USUARIOS` | `AsesorMonitorAccessService.validarPuedeListarAsesoresYMonitores()` |
| Monitor | `VER_ASESORES_MONITORES`, `GESTIONAR_ASESORES_MONITORES`, `GESTIONAR_USUARIOS` | `AsesorMonitorAccessService.validarPuedeListarAsesoresYMonitores()` |
| Conciliador | `VER_CONCILIADORES`, `GESTIONAR_CONCILIADORES`, `GESTIONAR_USUARIOS` | `ConciliadorAccessService.validarPuedeListarConciliadores()` |
| Estudiante | `VER_ESTUDIANTES`, `VER_PERFILES_AUXILIARES`, `GESTIONAR_USUARIOS` | `EstudianteAccessService.validarPuedeListarEstudiantes()` y resolución posterior de scope |

Los tests de autorización ejercitan cada authority admitida y rechazan una authority irrelevante. En los módulos con `AccessService`, la validación ocurre antes de consultar el repositorio. No se amplían permisos de endpoints auxiliares ni de commands.

## Scope especial de Estudiante

El listado raíz resuelve el alcance después de validar autorización y parámetros, y antes de construir/ejecutar la consulta paginada:

1. `EstudianteQueryService.buscar(...)` llama a `validarPuedeListarEstudiantes()`.
2. Valida `page`, `size`, `search`, `sortBy` y `direction`.
3. `resolverAlcanceListado()` decide la visibilidad.
4. Administrador operativo: `asesorIdScope = null`, visión global.
5. Asesor: `obtenerAsesorActualId()` obtiene `PerfilUsuarioActual.perfilId` y lo usa como `asesorIdScope`; no usa el id de `UsuarioSistema`.
6. Otro perfil con permiso de endpoint: respuesta vacía, sin consultar el repositorio y sin obtener visión global.
7. Si el perfil asesor no puede resolverse, se lanza `AccessDeniedException`: comportamiento fail-closed.
8. El repositorio aplica `(:asesorIdScope IS NULL OR asesor.id = :asesorIdScope)` tanto en la consulta de contenido como en la `countQuery`.

Por tanto, `totalElements` y `totalPages` representan únicamente el scope visible. No existe `Page -> stream -> filter` ni `findAll -> subList` en el nuevo GET raíz. El post-filter conservado en `/conciliacion` pertenece al endpoint auxiliar no paginado y queda fuera de esta regla.

`EstudiantePaginadoRepositoryJpaTest.scopeAsesorAFiltraAntesDePaginarYCalculaTotalesDelScope` usa dos asesores con cuatro estudiantes cada uno y tamaño 2: comprueba dos páginas del asesor A, cuatro elementos totales, dos páginas totales y ausencia completa de estudiantes del asesor B. Otra prueba verifica el scope del asesor B.

## Evidencia PostgreSQL y N+1

Las tres suites JPA heredan de `PostgreSqlIntegrationTest`, que usa Testcontainers con `postgres:16-alpine`:

- `UsuarioSistemaRepositoryJpaTest`.
- `PerfilPaginadoRepositoryJpaTest` para Administrativo, Asesor, Monitor y Conciliador.
- `EstudiantePaginadoRepositoryJpaTest`.

Las suites verifican página 1/página 2, totales, filtros, búsquedas case-insensitive, combinaciones de filtros, orden estable y campos de projection. Cada repositorio declara una `countQuery` explícita equivalente a sus filtros de contenido.

Para N+1, las pruebas hacen `flush`, `clear` y `statistics.clear()` después del fixture; ejecutan la consulta, acceden a todos los getters de la projection y confirman que el conteo permanece en 2 statements (contenido + count), sin cargas/fetches de colecciones adicionales donde aplica. No se mide el setup.

## Compatibilidad de endpoints

| Módulo | Detalle preservado | Endpoints/operaciones preservados relevantes |
|---|---|---|
| UsuarioSistema | `GET /api/usuarios-sistema/{id}` | `/activos`, cambio de estado y cambios de perfil Administrativo/Estudiante/Asesor/Monitor/Conciliador |
| Administrativo | `GET /api/administrativos/{id}` | `/activos`, `/directoras`, crear, actualizar, cambiar estado/directora y eliminar |
| Asesor | `GET /api/asesores/{id}` | `/activos`, crear, actualizar, cambiar estado y eliminar |
| Monitor | `GET /api/monitores/{id}` | `/activos`, crear, actualizar, cambiar estado y eliminar |
| Conciliador | `GET /api/conciliadores/{id}` | `/activos`, crear, actualizar, cambiar estado y eliminar |
| Estudiante | `GET /api/estudiantes/{id}` | `/activos`, `/conciliacion`, `/activos/asesor/{asesorId}`, crear, actualizar, cambiar estado/conciliación, eliminar e importar |

Los endpoints auxiliares siguen retornando sus contratos no paginados; no se migraron en SCRUM-272. Su sustitución eventual corresponde a SCRUM-273/frontend.

## Gates técnicos ejecutados

El usuario informó que los gates Maven de A/B/C finalizaron correctamente. El Bloque D no volvió a ejecutarlos. Las clases existentes que constituyen la evidencia reproducible son:

**Bloque A — UsuarioSistema**

- `UsuarioSistemaRepositoryJpaTest`
- `UsuarioSistemaServiceTest`
- `UsuarioSistemaControllerTest`
- `UsuarioSistemaControllerAuthorizationTest`

**Bloque B — Administrativo, Asesor y Monitor**

- `PerfilPaginadoRepositoryJpaTest`
- `AdministrativoQueryServiceTest`
- `AsesorQueryServiceTest`
- `MonitorQueryServiceTest`
- `PerfilPaginadoControllerTest`
- `PerfilPaginadoControllerAuthorizationTest`

**Bloque C — Conciliador y Estudiante**

- `ConciliadorQueryServiceTest`
- `EstudianteQueryServicePaginadoTest`
- `EstudianteAccessServiceScopeTest`
- `EstudiantePaginadoControllerTest`
- `EstudiantePaginadoControllerAuthorizationTest`
- `EstudiantePaginadoRepositoryJpaTest`

`PerfilPaginadoRepositoryJpaTest`, `PerfilPaginadoControllerTest` y `PerfilPaginadoControllerAuthorizationTest` también contienen la cobertura de Conciliador añadida en C1.

## Regresión SEC-08 / SEC-10

La trazabilidad se limita a pruebas existentes; no se asignan nombres inventados:

| Regresión | Evidencia principal existente | Cobertura |
|---|---|---|
| SEC-08 | `AdministracionInvariantConcurrencyTest` | Dos desactivaciones concurrentes no dejan cero administradores; dos retiros concurrentes no dejan cero directoras; dos retiros concurrentes no eliminan la última capacidad de recuperación. El fixture contiene identificadores explícitos `SEC-08`. |
| SEC-08 | `AdministracionInvariantRepositoryJpaTest`, `AdministracionInvariantPolicyTest`, `UsuarioSistemaServiceInvariantTest`, `AdministrativoCommandServiceInvariantTest` | Locks/consultas de invariantes, política y orden de ejecución de guards antes de consultar o mutar usuarios/administrativos. |
| SEC-10 | `SecurityDataInitializerRolesBaseTest` | Reinicio sin duplicados, creación de cinco roles base, rechazo de rol legacy sin código base y rechazo de tipo de perfil incompatible; cubre el inicializador que emite validaciones explícitas `SEC-10`. |
| SEC-10 | `UsuarioSistemaRegistroRolBaseTest`, `RolServiceTipoPerfilTest`, `RolServiceInvariantTest`, `PermisoServiceInvariantTest` | Asignación de rol base, compatibilidad rol/perfil e invariantes de rol/permiso. |
| Aislamiento/cambio de perfil y estado | `UsuarioCambioPerfilServiceInvariantTest`, `UsuarioSistemaPerfilEstadoServiceTest`, `UsuarioActualServiceAdministrativeTest`, `EstudianteAccessServiceScopeTest` | Guard antes de desactivar perfil anterior, sincronización de estado, resolución administrativa y separación entre id de usuario e id del perfil asesor. |

SCRUM-272 no reimplementa estas invariantes. La auditoría no encontró relajación de authorities, exposición de permisos ni conversión accidental del id de usuario en id de perfil.

## Trazabilidad de aceptación

| Criterio Jira | Implementación | Evidencia | Estado |
|---|---|---|---|
| Seis listados uniformes | Seis GET raíz con paginación uno-basada, defaults comunes y `PageResponseDTO` | Controladores y tabla Swagger/OpenAPI | VERIFICADO |
| DTO resumen | Un `ResumenDTO` y una projection escalar por módulo | Tabla de privacidad y repositorios | VERIFICADO |
| Sin secretos | DTO mínimos sin hashes, tokens, secretos ni permisos | Inspección estática y assertions JSON de controller | VERIFICADO |
| Activo/inactivo | Parámetro `activo` nullable en los seis listados, aplicado en JPQL antes de paginar | Repositorios y suites JPA | VERIFICADO |
| Tipo donde aplica | `tipoPerfil` en UsuarioSistema; `tipoConciliador` en Conciliador | Firmas de controller, JPQL y tests | VERIFICADO |
| Tipo en Administrativo/Asesor/Monitor/Estudiante | No existe clasificación funcional adicional que filtrar | Modelo y contratos reales | NO APLICA |
| Orden estable | Lista blanca y `id ASC` como desempate salvo orden principal por id | Servicios y tests de orden estable | VERIFICADO |
| Size máximo 50 | Validación `1..50` en los seis servicios | Tests unitarios de validación | VERIFICADO |
| Permisos backend | `@PreAuthorize` real y AccessServices donde corresponde | Tests de autorización y servicios | VERIFICADO |
| Scope Estudiante antes de paginar | Scope de asesor en JPQL y count; otros perfiles sin ampliación | `EstudianteQueryServicePaginadoTest`, `EstudianteAccessServiceScopeTest`, `EstudiantePaginadoRepositoryJpaTest` | VERIFICADO |
| Detalle preservado | `GET /{id}` y operaciones/auxiliares existentes continúan publicados | Tabla de compatibilidad | VERIFICADO |
| Pruebas por perfil | Suites de repository, service, controller y autorización para los seis módulos | Tabla de cobertura y gates A/B/C | VERIFICADO |
| SEC-08 / SEC-10 | Suites de invariantes, concurrencia, roles base y perfil/estado existentes | Sección de regresión | VERIFICADO |
| Swagger | Springdoc puede inferir respuestas genéricas y RequestParam desde firmas públicas | Tabla exacta de contratos; verificación estática | VERIFICADO |
| Integración | Commits A/B/C presentes en la rama objetivo | Historial Git local | VERIFICADO |
| PR | Flujo directo por rama/commits definido por el equipo | Decisión de integración suministrada para SCRUM-272 | NO APLICA |

## Trazabilidad Git

Rama: `feature/scrum-272-user-profile-pagination`.

Commits relevantes, obtenidos de `git log`:

| Commit | Bloque | Descripción real |
|---|---|---|
| `152b1b8` | A | `feat: paginate system users` |
| `71e59a0` | B | `feat: paginate administrative profiles` |
| `77e034b` | C1 | `feat: paginate conciliator profiles` |
| `632aeb8` | C2 | `feat: complete student profile pagination` |

Integración: flujo directo mediante rama y commits, según decisión del equipo.

PR: no generado por decisión del flujo de integración; su ausencia no constituye un fallo técnico de SCRUM-272.

## Estado final

**LISTO PARA CIERRE.**

La auditoría estática no encontró un incumplimiento real de aceptación ni un defecto objetivo de producción. Los seis listados tienen paginación en base de datos, contratos resumen escalares, filtros de dominio aplicables, totales correctos por consulta/scope, orden estable, límite de tamaño, seguridad backend y compatibilidad con detalle/operaciones existentes. No quedan bloqueos técnicos identificados en el alcance de SCRUM-272.

No se ejecutó Maven en el Bloque D, no se levantó la aplicación, no se generó PR y no se modificaron archivos Java.
