# Evidencia técnica de paginación de Procesos, Conciliaciones y Reuniones — SCRUM-268

## Entorno

- PostgreSQL Testcontainers
- postgres:16-alpine
- Java 21
- Spring Boot / Spring Data JPA
- Hibernate
- dataset sintético construido por los tests JPA
- rama feature/scrum-268-legal-activity-pagination

## Gate consolidado PostgreSQL

El comando final ejecutado localmente para validar las tres entidades:

```powershell
.\mvnw.cmd test "-Dtest=ProcesoRepositoryJpaTest,ConciliacionRepositoryJpaTest,ReunionConciliacionRepositoryJpaTest"
```

**Resultado:**

```text
Tests run: 45
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
Total time: 01:03 min
Finished at: 2026-09-04T11:18:46-05:00
```

Se trata de tres suites JPA ejecutadas contra PostgreSQL real mediante Testcontainers, las cuales cubren los criterios de persistencia y consulta evaluados para SCRUM-268.

## Tabla de cobertura

| Módulo | Paginación | Total visible | Búsqueda | Estado | Fechas | Scope | Fail closed | Orden estable | N+1 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Procesos | Verificado | Verificado | Verificado | Verificado | Verificado | ESTUDIANTE, ASESOR, MONITOR | Verificado (perfil no soportado/Administrativo) | Verificado | Verificado |
| Conciliaciones | Verificado | Verificado | Verificado | Verificado | Verificado | ASESOR, MONITOR, CONCILIADOR, ESTUDIANTE, GLOBAL/ADMIN | Verificado (perfil no soportado/Administrativo) | Verificado | Verificado |
| Reuniones | Verificado | Verificado | Verificado | Verificado | Verificado | ASESOR, MONITOR, CONCILIADOR, ESTUDIANTE, GLOBAL/ADMIN | Verificado (perfil no soportado/Administrativo) | Verificado | Verificado |

> *Nota sobre Procesos: CONCILIADOR y los perfiles no soportados se verifican explícitamente con comportamiento fail-closed.*

## Evidencia de consultas SQL y ausencia de N+1

Los tres tests JPA contienen una prueba denominada `accesoAGettersDeProjectionNoDebeGenerarNMasUno()`.
El proceso de verificación funciona así:

1. Se habilitan Hibernate Statistics.
2. Se limpia el contexto de persistencia.
3. Se limpian las estadísticas.
4. Se obtiene una página desde el repositorio.
5. Se acceden absolutamente todos los getters de las projections devueltas.
6. Se verifica `prepareStatementCount` en las estadísticas de Hibernate.

En los escenarios evaluados, el acceso a todos los getters de las projections no genera sentencias SQL adicionales después de ejecutar la consulta paginada.

| Módulo | Sentencias SQL esperadas | Interpretación |
| --- | ---: | --- |
| Procesos | 2 | content query + countQuery; sin consultas adicionales por elemento |
| Conciliaciones | 2 | content query + countQuery; sin consultas adicionales por elemento |
| Reuniones | 2 | content query + countQuery; sin consultas adicionales por elemento |

Acceder a los getters de las projections no incrementa la cantidad de sentencias SQL.

## Evidencia por módulo

### Procesos

El módulo está cubierto por `ProcesoRepositoryJpaTest`. Las pruebas verifican:
- paginación y `totalElements`.
- página fuera de rango (devuelve lista vacía preservando conteo).
- búsqueda multicampo.
- filtros por estado y fechas.
- filtros combinados.
- exclusión de registros inactivos y consultas en estado archivado.
- fail closed para roles incorrectos.
- uso adecuado de la projection `ProcesoResumenProjection`.
- orden estable.
- control estricto de N+1.

El rango temporal de la búsqueda en Procesos se aplica lógicamente sobre el campo `fechaCreacion`.

### Conciliaciones

El módulo está cubierto por `ConciliacionRepositoryJpaTest`.
Comparte los mismos criterios exhaustivos de Procesos, extendiendo su validación de scope para abarcar:
- `ASESOR`
- `MONITOR`
- `CONCILIADOR`
- `ESTUDIANTE` (ya sea asociado directamente a la conciliación o por medio de su consulta raíz)
- alcance global/admin
- fail closed para perfiles no soportados (ej: administrativos genéricos)

El rango temporal de la búsqueda de listado aplica sobre el campo `fechaCreacion`.

### Reuniones

El módulo está cubierto por `ReunionConciliacionRepositoryJpaTest`.
Verifica paginación, metadatos, filtros (incluyendo una amplia búsqueda de coincidencia por observacion, estado, estudiante, conciliador, etc.), orden estable y N+1.
El rango temporal en el listado de Reuniones utiliza de forma natural el campo `fechaReunion`.

También contiene validaciones estrictas y dedicadas a la consulta de Agenda.

## Evidencia de consulta de reuniones para Agenda

El código en `AgendaQueryService` y `ReunionConciliacionRepository` fue comprobado:

- NO se usa `reunionRepository.findAll()` para reuniones.
- El repository recibe directamente el rango temporal.
- La semántica temporal de la query es: `fechaReunion >= desde` y `fechaReunion < hastaExclusiva`.
- El scope de autorización se incorpora a la consulta JPQL del repositorio antes de recuperar los resultados; Hibernate traduce posteriormente esta consulta a SQL.
- No existe filtrado de autorización posterior (`.filter`) sobre los resultados obtenidos de la base de datos.
- La projection `ReunionAgendaProjection` devuelve directamente los campos requeridos por la Agenda, evitando tener que navegar asociaciones de la entidad para construir cada evento.
- Los `Seguimientos` permanecen sin optimizar dentro de este ticket, dado que su refactorización corresponde a SCRUM-269.

La prueba `agendaDebeConsultarDirectamentePorRangoYScope()` en los tests JPA demuestra la efectividad de este enfoque.

## Contrato REST Uniforme

Los endpoints exponen parámetros consistentes.

| Endpoint | Respuesta | search | page | size | sortBy | direction | estado | fechaDesde | fechaHasta |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `GET /api/procesos` | `PageResponseDTO<ProcesoResumenDTO>` | ✔️ | ✔️ | ✔️ | ✔️ | ✔️ | ✔️ (EstadoProceso) | ✔️ | ✔️ |
| `GET /api/conciliaciones` | `PageResponseDTO<ConciliacionResumenDTO>` | ✔️ | ✔️ | ✔️ | ✔️ | ✔️ | ✔️ (String) | ✔️ | ✔️ |
| `GET /api/conciliaciones/reuniones` | `PageResponseDTO<ReunionConciliacionResumenDTO>` | ✔️ | ✔️ | ✔️ | ✔️ | ✔️ | ✔️ (String) | ✔️ | ✔️ |

## Swagger / OpenAPI

Estado Swagger/OpenAPI: **PENDIENTE DE VERIFICACIÓN MANUAL**

El proyecto cuenta con la dependencia Springdoc/OpenAPI habilitada, pero al momento de esta revisión el backend no se encontraba ejecutándose localmente.

Para su verificación manual:
1. Levantar normalmente el backend (`mvn spring-boot:run` o desde IDE).
2. Abrir en el navegador: `http://localhost:8080/swagger-ui/index.html`
3. Localizar los tres endpoints (`/api/procesos`, `/api/conciliaciones`, `/api/conciliaciones/reuniones`).
4. Expandirlos.
5. Verificar la existencia y correcta notación de los query parameters (`search`, `page`, `size`, `sortBy`, `direction`, `estado`, `fechaDesde`, `fechaHasta`).
6. Tomar una captura de pantalla como evidencia visual para la historia de usuario.

## Interpretación

Técnicamente:
- Los 45 tests ejecutados en el comando de QA finalizaron sin fallos ni errores.
- Toda la paginación y ordenamiento se validó contra PostgreSQL real (Testcontainers).
- Los filtros combinados y los scopes de autorización (incluyendo el fail-closed) se resuelven nativamente en la base de datos antes de conformar el objeto de la página.
- El uso de Projections personalizadas permite mantener las sentencias SELECT acotadas a lo requerido.
- En los escenarios de acceso a projections evaluados con Hibernate Statistics, los listados paginados ejecutan 2 sentencias SQL (content query + countQuery) y el acceso posterior a sus getters no genera consultas adicionales.
- La Agenda consulta directamente las reuniones comprendidas en el rango [desde, hasta), evitando cargar todas las reuniones mediante findAll() para filtrarlas posteriormente en memoria.
- En general, esto satisface técnicamente y de forma probada los criterios de SCRUM-268.

## Trazabilidad

- **Ticket**: SCRUM-268 — BE — Paginar Procesos, Conciliaciones y Reuniones
- **Rama**: feature/scrum-268-legal-activity-pagination
- **Commits principales**:
  - `aca978e` — feat: add paginated process listings
  - `2b89afd` — feat: add paginated conciliation meetings
- **Integración**: flujo directo de rama definido por el equipo; no se generó PR para esta evidencia.

## Estado final de evidencias

| Evidencia | Estado |
| --- | --- |
| PostgreSQL real / Testcontainers | VERIFICADO |
| Paginación y totalElements | VERIFICADO |
| Filtros combinados | VERIFICADO |
| Scope por perfil | VERIFICADO |
| Fail closed | VERIFICADO |
| N+1 / conteo SQL | VERIFICADO |
| Agenda por rango | VERIFICADO |
| Contrato REST | VERIFICADO |
| Swagger/OpenAPI | PENDIENTE DE VERIFICACIÓN MANUAL |
