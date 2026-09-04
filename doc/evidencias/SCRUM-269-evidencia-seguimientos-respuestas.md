# Evidencia técnica de paginación de Seguimientos y respuestas — SCRUM-269

## Entorno

- PostgreSQL mediante Testcontainers
- postgres:16-alpine
- Java 21
- Spring Boot / Spring Data JPA
- Hibernate
- rama feature/scrum-269-follow-up-pagination

## Alcance técnico

### Bloque A — Seguimientos paginados

**GET /api/seguimientos**

Retorna: `PageResponseDTO<SeguimientoResumenDTO>`

Parámetros soportados:
- search
- page
- size
- sortBy
- direction
- estado
- fechaDesde
- fechaHasta
- consultaId
- autorId

Explicación:
- Se implementó la paginación a nivel de PostgreSQL para el listado de seguimientos.
- Se utiliza una consulta de contenido (content query) y su `countQuery` equivalente, garantizando que el paginado y los totales sean precisos.
- El filtrado por roles (scope) que antes se realizaba en memoria tras cargar todos los registros, ahora se encuentra acotado antes de paginar.
- Scopes aplicados:
  - ADMIN: acceso global.
  - ASESOR: seguimiento a consultas propias o de estudiantes asignados.
  - MONITOR: seguimiento a consultas asignadas.
  - ESTUDIANTE: únicamente consultas propias y cuyo seguimiento tenga `notificarEstudiante=true`.
- Los perfiles no soportados operan bajo un modelo fail-closed (no reciben registros).
- Se utiliza una projection escalar (e.g., `SeguimientoResumenProjection`) que no incluye adjuntos ni binarios (DTO resumen limpio).
- La ausencia de problemas N+1 fue verificada mediante Hibernate Statistics.

### Bloque B — listados heredados y Agenda

- Listado por autor acotado por el scope directamente en SQL.
- Listado por fecha de entrega acotado por scope.
- Los seguimientos visibles para el estudiante no sufren post-filtrado en memoria.
- **GET /api/seguimientos/calendario** requiere los parámetros `from` y `to`.
- Se consulta exclusivamente el rango temporal `[from,to)`.
- Soporta un máximo de 3 meses.
- La Agenda utiliza el método `buscarParaAgenda` mediante `SeguimientoAgendaProjection`.
- No se utiliza el método `findAll()` para listar los seguimientos del calendario.
- La Agenda no realiza filtrado de fechas ni de autorización después de recuperar todos los registros; esto se efectúa nativamente en la consulta JPQL/SQL.
- Las Alertas Disciplinarias se preservaron intactas porque ya utilizaban queries SQL correctamente acotadas.
- El Scheduler no se modificó porque ya trabaja sobre una consulta de notificaciones acotada y no pertenece al defecto SCRUM-269.

### Bloque C — respuestas pendientes

**GET /api/seguimientos/respuestas/pendientes**

Retorna: `PageResponseDTO<SeguimientoRespuestaResponseDTO>`

Parámetros soportados:
- search
- page
- size
- sortBy
- direction
- fechaDesde
- fechaHasta

Explicación:
- Filtra únicamente respuestas en estado PENDIENTE.
- Valida que la respuesta y el seguimiento asociado estén activos.
- Excluye consultas que se encuentren en estado ARCHIVADA.
- El scope de autorización (ADMIN/ASESOR/MONITOR) se aplica en la consulta JPQL.
- Perfiles no soportados tienen comportamiento fail-closed.
- Búsqueda de texto habilitada por contenido de la respuesta y nombre del estudiante.
- El filtro por rango de fechas aplica sobre `fechaCreacion`.
- Se utilizan consultas de contenido y conteo (`value`/`countQuery` equivalentes).
- Utiliza la projection `SeguimientoRespuestaPendienteProjection`.
- Se eliminó el filtro antiguo de memoria `filter(seguimientoRespuestaAccessService::puedeRevisarRespuesta)` que se aplicaba después de recuperar registros.
- La consulta de contenido y el conteo no sufren de N+1.

## Tabla de cobertura

| Flujo | Paginación | Total visible | Búsqueda | Fechas | Scope SQL | Fail closed | Orden estable | N+1 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Seguimientos principales | Verificado | Verificado | Verificado | Verificado | Verificado | Verificado | Verificado | Verificado |
| Seguimientos Agenda | Consulta por rango | N/A | N/A | Verificado | Verificado | Verificado | Verificado | Verificado |
| Respuestas pendientes | Verificado | Verificado | Verificado | Verificado | Verificado | Verificado | Verificado | Verificado |

## Seguridad y regresión SEC-06

- En los listados optimizados por SCRUM-269, los scopes de autorización
  se resuelven en la consulta antes de devolver los registros.
- No existe autorización post-paginación (en memoria) en los nuevos listados.
- ASESOR: `consulta.asesor.id = perfilId OR consulta.estudiante.asesor.id = perfilId`.
- MONITOR: `consulta.monitor.id = perfilId`.
- ESTUDIANTE en listado principal de Seguimientos: `consulta.estudiante.id = perfilId AND seguimiento.notificarEstudiante = true`.
- En el caso de las respuestas pendientes, no se permite acceso a perfiles ESTUDIANTE ni CONCILIADOR.
- Los perfiles no soportados no reciben registros.
- Las consultas ARCHIVADAS quedan fuera de los flujos operativos.
- Las pruebas de la suite de SEC-06 (`SeguimientoControllerAuthorizationTest`, `SeguimientoRespuestaControllerAuthorizationTest`, `SeguimientoAccessServiceTest`) no regresan y fueron ejecutadas con éxito.

## Evidencia PostgreSQL y ausencia de N+1

**Seguimientos paginados:**
- **content query**: incluye filtros de paginación y scope delegados a base de datos.
- **countQuery**: equivalente al content query.
- **projection**: extrae únicamente campos requeridos. Los getters no disparan consultas adicionales.

**Respuestas pendientes:**
- **content query** y **countQuery** equivalentes.
- **projection** nativa, los getters no disparan consultas adicionales.

**Agenda:**
- Una única consulta por rango/projection.
- Sin carga del histórico completo mediante `findAll`.
- Los getters de Agenda no provocan N+1 según las pruebas de carga de estadísticas de Hibernate existentes.

Condiciones conceptuales (traducidas de JPQL por Hibernate a PostgreSQL):
```sql
WHERE activo = true
AND consulta.estado <> ARCHIVADO
AND scope
AND filtros
AND rango temporal
```

## Contratos REST

| Endpoint | Respuesta | search | page | size | sortBy | direction | estado | fechaDesde | fechaHasta | consultaId | autorId | from/to |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `GET /api/seguimientos` | `PageResponseDTO<SeguimientoResumenDTO>` | ✔️ | ✔️ | ✔️ | ✔️ | ✔️ | ✔️ | ✔️ | ✔️ | ✔️ | ✔️ | ❌ |
| `GET /api/seguimientos/respuestas/pendientes` | `PageResponseDTO<SeguimientoRespuestaResponseDTO>` | ✔️ | ✔️ | ✔️ | ✔️ | ✔️ | ❌ | ✔️ | ✔️ | ❌ | ❌ | ❌ |
| `GET /api/seguimientos/calendario` | `List<SeguimientoResponseDTO>` | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ✔️ |

## Gates técnicos ejecutados

Los gates de A, B y C fueron ejecutados y terminaron en `BUILD SUCCESS`.

**Bloque A:**
- SeguimientoRepositoryJpaTest
- SeguimientoQueryServiceTest
- SeguimientoControllerTest
- SeguimientoControllerAuthorizationTest
- SeguimientoAccessServiceTest

**Bloque B:**
- SeguimientoRepositoryJpaTest
- SeguimientoQueryServiceTest
- SeguimientoControllerTest
- SeguimientoControllerAuthorizationTest
- AgendaQueryServiceTest
- SeguimientoAccessServiceTest

**Bloque C:**
- SeguimientoRespuestaRepositoryJpaTest
- SeguimientoRespuestaQueryServiceTest
- SeguimientoRespuestaControllerTest
- SeguimientoRespuestaControllerAuthorizationTest
- SeguimientoRespuestaValidatorTest

## Trazabilidad de Aceptación

| Criterio Jira | Evidencia técnica | Estado |
| --- | --- | --- |
| Ningún seguimiento ajeno entra al conjunto previo a paginar | Scope resuelto antes de devolver registros. | VERIFICADO |
| Totales respetan alcance | `countQuery` incorpora alcance. | VERIFICADO |
| Filtros de fecha validados | Búsqueda temporal acotada en listados. | VERIFICADO |
| Alertas/calendario consultan solo ventana/rango | El calendario consulta Seguimientos mediante `buscarParaAgenda` en `[from,to)` sin cargar el histórico completo; las alertas disciplinarias conservan sus consultas específicas acotadas por estado activo, consulta no archivada y scope, sin `findAll()`. | VERIFICADO |
| DTO resumen no incluye adjuntos/binarios | Projections escalares excluyen campos pesados. | VERIFICADO |
| Pruebas SEC-06 no regresan | Suites AuthorizationTest preservadas. | VERIFICADO |

## Trazabilidad Git

- **Rama:** `feature/scrum-269-follow-up-pagination`
- **Commits relevantes:**
  - `0fc6ede` feat: paginate pending follow-up responses
  - `6057bec` feat: optimize follow-up calendar queries
  - `f5565a0` feat: add paginated follow-up listings
- **Integración:** Flujo directo de ramas definido por el equipo; no se generó PR.
- **Estado previo al commit de evidencia:** únicamente
  `doc/evidencias/SCRUM-269-evidencia-seguimientos-respuestas.md`
  se encontraba sin trackear; no había cambios pendientes en producción ni tests.

## Estado final de evidencias

| Evidencia | Estado |
| --- | --- |
| PostgreSQL/Testcontainers | VERIFICADO |
| Paginación Seguimientos | VERIFICADO |
| Totales visibles | VERIFICADO |
| Scope SQL | VERIFICADO |
| Filtros temporales | VERIFICADO |
| Agenda por rango | VERIFICADO |
| Respuestas pendientes paginadas | VERIFICADO |
| Fail closed | VERIFICADO |
| N+1 | VERIFICADO |
| SEC-06 | VERIFICADO |
| DTO sin binarios | VERIFICADO |
| Contrato REST | VERIFICADO |
