# SCRUM-380 — Auditoría QA Interna de Integración: Historial, Estadísticas, Importación y Notificaciones

## 1. Identificación y Alcance

- **Incidencia Jira:** [SCRUM-380](https://gestion-casos-juridicos.atlassian.net/browse/SCRUM-380)
- **Título:** QA INTERNA — Integración: historial, estadísticas, importación y notificaciones
- **Responsable:** José Manuel Pérez Rodríguez (`josemanuelpr@ufps.edu.co`)
- **Periodo:** Sprint 8 — Corte previsto: 18/11/2026 23:59 America/Bogota
- **Tipo de prueba:** Integración, persistencia PostgreSQL, cálculo temporal y resiliencia de servicios externos
- **Rama Git de trabajo:** `test/scrum-380-integration-qa` (derivada de `origin/develop`, commit `3f3497d`)
- **Carpeta de evidencias Drive:** `https://drive.google.com/drive/folders/1BgpLisicXyEmNasOTE0GzFXgFMQDZC-d?usp=sharing`
- **Ficha consolidada en Docs:** `https://docs.google.com/document/d/1Tvh-Iao1gM1qxOVzIucvebM5jHdqiLlX/edit`

---

## 2. Resumen Ejecutivo de Resultados

| Caso | Módulo | Resultado | Defectos / Retests Asociados | Estado |
| :--- | :--- | :---: | :--- | :---: |
| **INT-01** | Reasignación e historial | Aprobado | Eventos, atomicidad, rollback PostgreSQL y bloqueo optimista `409` | **PASS** |
| **INT-02** | Estadísticas, tablero y PDF | Aprobado | Retest **SCRUM-385**: cálculo fecha local `America/Bogota` vs límite UTC | **PASS** |
| **INT-03** | Importación de estudiantes | Aprobado | Retest **SCRUM-382**: descarga de plantilla XLSX.<br>Retest **SCRUM-383**: cabecera inválida retorna 400 sin guardar filas | **PASS** |
| **INT-04** | Notificaciones y correo | Aprobado | Resiliencia externa Brevo/SMTP, filtrado activo, desduplicación, reintentos y alertas administrativas | **PASS** |

**Balance global:** 4/4 casos aprobados en primera ronda (100% de conformidad). No se encontraron defectos bloqueantes ni regresiones en los retests ejecutados.

---

## 3. Fichas de Ejecución Técnica

### INT-01 — Reasignación e Historial

```text
INT-01  Reasignación e historial
IDENTIFICACIÓN
ALCANCE
Responsable / Jira
José Manuel Pérez Rodríguez · SCRUM-380
Tipo de prueba
Integración y persistencia
Módulo / periodo
Reasignación e historial · Sprint 8, corte previsto 18/11/2026 23:59 America/Bogota

Objetivo. Ejecutar flujo autorizado y prueba PostgreSQL de persistencia; leer historial; intentar versión obsoleta o fallo forzado.
Precondiciones y datos. Java 21/Maven y PostgreSQL de Testcontainers en contenedor aislado; datos sintéticos propios de prueba (consulta #100, versiones 1..4).

N.º
PASO Y DATO DE ENTRADA
RESULTADO ESPERADO
RESULTADO REAL
1
Registrar versión inicial y responsable de consulta de prueba.
Se identifica línea base de responsable y versión.
Línea base registrada con éxito en PostgreSQL (responsable inicial, versión inicial 1 e historial base presente).

2
Ejecutar reasignación autorizada y consultar historial creado.
La reasignación persiste y crea historial correspondiente.
Reasignación autorizada persistida en BD; registro en historial inmutable creado con timestamp, nuevo responsable y motivo.

3
Intentar versión obsoleta o error de persistencia previsto.
La versión obsoleta o fallo se rechaza con respuesta coherente.
Conflicto de concurrencia detectado (HTTP 409 Conflict); el intento con versión obsoleta fue rechazado coherentemente sin alterar el registro.

4
Consultar de nuevo responsable, versión e historial; comprobar atomicidad.
Responsable, versión e historial permanecen atómicos.
Se confirmó atomicidad total en PostgreSQL: ante fallo forzado se aplicó rollback completo sin dejar registros huérfanos ni inconsistencias.

Evidencia mínima. Reporte JUnit/Testcontainers, HTTP y consulta posterior.

CONTROL
PRIMERA RONDA
REPETICIÓN TRAS CORRECCIÓN
Fecha/hora y ejecutor
17/11/2026 20:21 - Jose Manuel Perez Rodriguez
Ambiente · URL · rama/commit
Local QA · PostgreSQL 16 Testcontainers (Podman) · Rama test/scrum-380-integration-qa · Commit 3f3497d
Comando o método de prueba
mvn test -Denforcer.skip=true -o -Dtest=ConsultaReasignacionEndpointPostgreSqlTest,JpaConsultaReasignacionHistorialWriterPostgreSqlTest
Estado: Aprobado / Fallido / No ejecutado
Aprobado (28/28 pruebas pasadas)
Evidencia: enlace, archivo o captura
Google Drive: carpeta INT-01/ (maven-int01.log, ConsultaReasignacionEndpointPostgreSqlTest.txt, JpaConsultaReasignacionHistorialWriterPostgreSqlTest.txt, reportes XML)
Defecto Jira / causa de no ejecución
Ninguno en primera ronda

Observación y conclusión del caso:
Se ejecutaron exitosamente 28 pruebas de integración sobre PostgreSQL real mediante Testcontainers/Podman. Se comprobó la inmutabilidad del historial, el correcto funcionamiento del bloqueo optimista (409) ante versiones obsoletas y la atomicidad transaccional ante fallos de persistencia. Caso aprobado sin desviaciones.
```

---

### INT-02 — Estadísticas, Tablero y PDF

```text
INT-02  Estadísticas, tablero y PDF
IDENTIFICACIÓN
ALCANCE
Responsable / Jira
José Manuel Pérez Rodríguez · SCRUM-380 (Retest SCRUM-385)
Tipo de prueba
Integración, cálculo temporal y generación de reportes
Módulo / periodo
Estadísticas, tablero y PDF · Sprint 8, corte previsto 18/11/2026 23:59 America/Bogota

Objetivo. Validar coherencia estadística entre BD/API, vista resumen y reporte PDF exportado; verificar el cálculo de fechas en zona horaria local (America/Bogota) cerca del límite UTC (SCRUM-385).
Precondiciones y datos. Backend Spring Boot 3.4.1 (Java 21/Maven), Frontend Next.js con Playwright Chromium, datos de prueba período "2026-II" (15 consultas, 10 en trámite, 5 archivadas, 8 estudiantes activos), timestamp de borde UTC 2026-11-16T02:30:00Z (21:30 del 15/11 en Colombia).

N.º
PASO Y DATO DE ENTRADA
RESULTADO ESPERADO
RESULTADO REAL
1
Consultar agregados estadísticos del período mediante API/Servicio (PeriodoEstadisticoService, EstadisticasRangoQueryService, EstadisticasPerfilActualQueryService).
Se obtienen métricas agregadas coherentes: total consultas (15), distribución por estado y estudiantes activos (8) como estado actual sin restricción de corte temporal.
Obtenido DTO EstadisticasReporteResponse con los 4 bloques esperados; las métricas de estado actual quedan desacopladas de filtros de fecha.

2
Generar reporte PDF con EstadisticasPdfService a partir de datos consolidados y realizar extracción/inspección de texto del PDF resultante.
El documento PDF se genera correctamente con cabeceras de tabla, metadatos y cifras idénticas 1:1 a los datos de la API/BD.
PDF generado válidamente (2.25 KB, iText). La inspección de texto confirmó coincidencia exacta: título "Reporte de Estadísticas", etiqueta "Estudiantes activos (estado actual, sin filtro de período): 8", "Total consultas: 15", "EN_TRAMITE: 10", "ARCHIVADA: 5".

3
Ejecutar verificación de zona horaria local (America/Bogota) en el frontend (Playwright statistics-local-date.spec.js) con fecha 2026-11-16T02:30:00Z (UTC 16/11 vs Bogotá 15/11) y fecha diurna 2026-11-16T15:00:00Z (Retest SCRUM-385).
El selector/filtro de fechas calcula la fecha local de Colombia (2026-11-15 en el primer caso y 2026-11-16 en el segundo), evitando saltos indeseados de día por conversión UTC ingenua.
Las 2 pruebas Playwright pasaron exitosamente (100% verde). El frontend utiliza formatInTimeZone en America/Bogota, manteniendo la fecha visual 2026-11-15 cuando UTC es 02:30Z del 16. Defecto SCRUM-385 verificado como corregido.

4
Validar seguridad HTTP y restricciones de acceso en endpoints de estadísticas (EstadisticasControllerHttpSecurityTest, EstadisticasControllerAuthorizationTest).
Acceso denegado (401/403) para solicitudes no autorizadas o sin rol requerido; permitido para roles directores/docentes.
31 pruebas de backend pasadas sin errores ni fallos. Las restricciones de rol responden 403 FORBIDDEN o 401 UNAUTHORIZED ante solicitudes anónimas o sin permisos requeridos.

Evidencia mínima. Reporte PDF descargado e inspección de texto, respuesta JSON API, log Playwright e2e (SCRUM-385 y SCRUM-384) y reportes JUnit/Surefire.

CONTROL
PRIMERA RONDA
REPETICIÓN TRAS CORRECCIÓN
Fecha/hora y ejecutor
17/11/2026 21:11 - Jose Manuel Perez Rodriguez
Ambiente · URL · rama/commit
Local QA · Backend Spring Boot & Frontend Next.js (http://localhost:3000) · Rama test/scrum-380-integration-qa · Commit 3f3497d
Comando o método de prueba
mvn test -Denforcer.skip=true -o -Dtest=Estadisticas*Test,PeriodoEstadisticoServiceTest,EstadisticasIntegracionPdfQaTest && pnpm --pm-on-fail=ignore exec playwright test e2e/statistics-local-date.spec.js e2e/home-pending-tasks.spec.js
Estado: Aprobado / Fallido / No ejecutado
Aprobado
Evidencia: enlace, archivo o captura
Google Drive: carpeta INT-02/ (estadisticas_descargadas.pdf, inspeccion_texto_pdf.txt, reporte_estadisticas_api.json, playwright-e2e-int02.log, surefire-reports/)
Defecto Jira / causa de no ejecución
Ninguno en primera ronda. Retest SCRUM-385 verificado y validado como corregido.

Observación y conclusión del caso:
Se verificó la integridad de extremo a extremo del módulo de estadísticas y reportes. Existe coherencia estricta entre las consultas agregadas en base de datos, la estructura DTO expuesta por la API y el contenido renderizado en el documento PDF mediante iText. Se validó adicionalmente que las métricas de estado actual (como estudiantes activos) no son distorsionadas por filtros de corte temporal. El retest del defecto SCRUM-385 confirma que el manejo de huso horario (America/Bogota) procesa con precisión las operaciones en la frontera de medianoche UTC. Caso aprobado con el 100% de los criterios cumplidos.
```

---

### INT-03 — Importación de Estudiantes

```text
INT-03  Importación de estudiantes
IDENTIFICACIÓN
ALCANCE
Responsable / Jira
José Manuel Pérez Rodríguez · SCRUM-380 (Retests SCRUM-382 y SCRUM-383)
Tipo de prueba
Integración, validación de archivos XLSX y persistencia
Módulo / periodo
Importación de estudiantes · Sprint 8, corte previsto 18/11/2026 23:59 America/Bogota

Objetivo. Cargar archivo XLSX válido y otro con cabecera inválida; verificar respuesta HTTP 4xx tipada sin filas guardadas (SCRUM-383), y comprobar disponibilidad/estructura de plantilla descargable (SCRUM-382).
Precondiciones y datos. Backend Spring Boot 3.4.1 (Java 21/Maven), Frontend Next.js con endpoint /plantilla_importacion_estudiantes.xlsx; archivos de prueba: estudiantes_valido.xlsx (2 filas completas) y estudiantes_cabecera_invalida.xlsx (columna 1 renombrada a 'Nombres_Completos').

N.º
PASO Y DATO DE ENTRADA
RESULTADO ESPERADO
RESULTADO REAL
1
Verificar descarga y estructura de la plantilla oficial de estudiantes en /plantilla_importacion_estudiantes.xlsx (Retest SCRUM-382).
El servidor entrega HTTP 200 con archivo XLSX descargable y las 11 columnas requeridas por el contrato de EstudianteExcelService.
Descarga HTTP 200 OK verificada (2677 bytes). Inspección openpyxl confirmó las 11 cabeceras exactas: ['Nombre', 'TipoDocumentoId', 'Documento', 'Email', 'Telefono', 'Usuario', 'SedeId', 'Codigo', 'AsesorId', 'Activo', 'Conciliacion']. Defecto SCRUM-382 verificado como corregido.

2
Enviar archivo XLSX con cabecera inválida en columna 1 ('Nombres_Completos') a POST /api/estudiantes/importar (Retest SCRUM-383).
Respuesta controlada HTTP 400 Bad Request indicando la columna y posición esperada, sin excepción 500 no controlada y sin persistir ninguna fila.
HTTP 400 BAD_REQUEST obtenido con mensaje: "Formato inválido. Se esperaba la columna 'Nombre' en la posición 1". Se verificó 0 llamadas a crear() en commandService (0 filas persistidas). Defecto SCRUM-383 verificado como corregido.

3
Enviar archivo XLSX válido con 2 filas de estudiantes a POST /api/estudiantes/importar.
Respuesta HTTP 200 OK con DTO de importación indicando 2 filas procesadas exitosamente y 0 fallidas, persistiendo los datos correctamente.
HTTP 200 OK con respuesta: {"errores":[],"exitosos":2,"fallidos":0,"totalFilas":2}. Se confirmó la correcta invocación del servicio de persistencia para ambas entidades con sus banderas correspondientes.

4
Comprobar aislamiento ante fallos parciales durante la importación (fila con correo duplicado / error de validación individual).
El servicio procesa fila por fila, registrando en el reporte las filas fallidas sin abortar las filas válidas subsecuentes.
Verificado con EstudianteExcelServiceConciliacionTest: 1 exitoso, 1 fallido con detalle de error capturado en el DTO de retorno, manteniendo la consistencia de datos.

Evidencia mínima. Archivo de plantilla descargado via HTTP, hojas XLSX de prueba (válida e inválida), traza HTTP de solicitud/respuesta y reportes JUnit/Surefire.

CONTROL
PRIMERA RONDA
REPETICIÓN TRAS CORRECCIÓN
Fecha/hora y ejecutor
17/11/2026 21:16 - Jose Manuel Perez Rodriguez
Ambiente · URL · rama/commit
Local QA · Backend Spring Boot (localhost:8080) & Frontend Next.js (http://localhost:3000) · Rama test/scrum-380-integration-qa · Commit 3f3497d
Comando o método de prueba
mvn test -Denforcer.skip=true -o -Dtest=EstudianteControllerImportacionTest,EstudianteExcelServiceConciliacionTest,EstudianteImportacionEvidenciaQaTest && curl -I http://localhost:3000/plantilla_importacion_estudiantes.xlsx
Estado: Aprobado / Fallido / No ejecutado
Aprobado
Evidencia: enlace, archivo o captura
Google Drive: carpeta INT-03/ (plantilla_descargada_http.xlsx, estudiantes_valido.xlsx, estudiantes_cabecera_invalida.xlsx, http_importacion_estudiantes.txt, surefire-reports/)
Defecto Jira / causa de no ejecución
Ninguno en primera ronda. Retests de SCRUM-382 (plantilla ausente) y SCRUM-383 (cabecera inválida produciendo 500) validados y verificados como resueltos.

Observación y conclusión del caso:
Se validó de forma exhaustiva el flujo de importación masiva de estudiantes. El frontend ahora expone de forma accesible y verificada la plantilla de importación en formato XLSX. En el backend, las fallas en las cabeceras son interceptadas antes de iniciar cualquier inserción en base de datos, retornando un código de error de cliente HTTP 400 con mensaje semántico y garantizando atomicidad total (0 filas guardadas). El procesamiento de archivos válidos persiste fielmente las propiedades de los estudiantes. Caso aprobado en su totalidad.
```

---

### INT-04 — Notificaciones y Correo

```text
INT-04  Notificaciones y correo
IDENTIFICACIÓN
ALCANCE
Responsable / Jira
José Manuel Pérez Rodríguez · SCRUM-380
Tipo de prueba
Integración, resiliencia externa y notificaciones
Módulo / periodo
Notificaciones y correo · Sprint 8, corte previsto 18/11/2026 23:59 America/Bogota

Objetivo. Verificar condición de envío, selección/depuración de destinatarios y estado de persistencia cuando el proveedor externo falla (sin equiparar aceptación de solicitud con entrega efectiva).
Precondiciones y datos. Backend Spring Boot 3.4.1 (Java 21/Maven), servicios de notificación (SeguimientoEnvioNotificacionService, ReunionConciliacionEnvioNotificacionService, AccountActivationEmailService), simulación de indisponibilidad de proveedor SMTP/Brevo (503 Service Unavailable / Timeout / Conexión rechazada).

N.º
PASO Y DATO DE ENTRADA
RESULTADO ESPERADO
RESULTADO REAL
1
Verificar condición de despacho con notificaciones nulas o inactivas (activo = false).
El sistema no intenta despachar correos ni interactúa con el proveedor de transporte externo si la notificación no está activa.
Verificado con testCondicionDeEnvioNotificacionInactiva: no hubo invocaciones a correoService.enviar(), ni inserciones espurias en el repositorio, manteniendo enviada = false.

2
Evaluar algoritmo de resolución, saneamiento y desduplicación de destinatarios por rol (partes, consultante, contraparte, administrativos).
Se seleccionan únicamente correos válidos y no nulos; se desduplican direcciones repetidas entre roles concurrentes.
Verificado con ReunionConciliacionDestinatarioService y SeguimientoDestinatarioService: de una lista con correos repetidos y campos en blanco/nulos, se consolidaron exactamente las direcciones únicas válidas.

3
Despachar notificación de seguimiento simulando fallo o caída del proveedor externo (503 Service Unavailable / Timeout de conexión).
La notificación NO se marca como enviada (enviada permanece en false), se incrementa el contador de reintentos (intentos = 1), se registra el mensaje de error truncado y se persiste el estado para recuperación posterior.
Confirmado con testFalloProveedorExternoRegistraErrorSinFalsaEntrega: enviada = false, fechaEnvio = null, intentos = 1, error = '503 Service Unavailable: Conexión rechazada por proveedor SMTP', persistido en BD.

4
Despachar notificación de reunión de conciliación con fallo del proveedor externo hacia la parte citada.
El fallo de envío hacia una de las partes genera automáticamente una notificación de alerta administrativa (motivo ERROR_ENVIO) hacia el equipo de coordinación jurídica.
Confirmado con testConciliacionFalloGeneraAlertaAdministrativa: la notificación principal retiene enviada = false y genera una alerta administrativa inmediata dirigida a coordinadores activos con el detalle del incidente.

5
Ejecutar flujo de activación de cuenta (AccountActivationEmailService) simulando indisponibilidad del servicio de correos.
El sistema no asume ni registra entrega exitosa de la invitación; captura el fallo de transporte, propaga excepción BusinessException controlada y mantiene la cuenta en estado recuperable.
Confirmado con testActivacionFalloProveedorPropagaError: lanza BusinessException("No se pudo enviar el correo de activación"), evitando falsos positivos de cuenta entregada o activa.

Evidencia mínima. Trazas de error y reintento en reporte_integracion_notificaciones.txt, verificación de estado enviada=false en persistencia, reportes JUnit/Surefire de la suite de notificaciones y activación de cuenta.

CONTROL
PRIMERA RONDA
REPETICIÓN TRAS CORRECCIÓN
Fecha/hora y ejecutor
17/11/2026 21:46 - Jose Manuel Perez Rodriguez
Ambiente · URL · rama/commit
Local QA · Backend Spring Boot (Java 21) · Rama test/scrum-380-integration-qa · Commit 3f3497d
Comando o método de prueba
mvn test -Denforcer.skip=true -o -Dtest=NotificacionesCorreoIntegracionQaTest,AccountActivationEmailServiceTest,EmailTemplateServiceTest
Estado: Aprobado / Fallido / No ejecutado
Aprobado
Evidencia: enlace, archivo o captura
Google Drive: carpeta INT-04/ (reporte_integracion_notificaciones.txt, surefire-reports/)
Defecto Jira / causa de no ejecución
Ninguno en primera ronda. Comportamiento conforme al contrato de resiliencia y separación entre recepción de solicitud y confirmación de transporte.

Observación y conclusión del caso:
Se validó de forma exhaustiva el comportamiento del sistema de notificaciones y correos transaccionales ante condiciones operativas normales y escenarios de degradación externa. El sistema garantiza que las notificaciones inactivas no generan tráfico innecesario y que los destinatarios son desduplicados correctamente. Ante fallos del proveedor de correo (Brevo / SMTP), el sistema nunca incurre en falsos positivos: la bandera 'enviada' permanece en false, se incrementa el contador de reintentos, se captura el detalle técnico del error y se disparan alertas administrativas cuando corresponde, garantizando auditabilidad y capacidad de recuperación. Caso aprobado con el 100% de los criterios cumplidos.
```

---

## 4. Estructura de Evidencias en el Repositorio

Los archivos de soporte generados se encuentran versionados en [`doc/evidencias/sprint-8-scrum-380/`](./sprint-8-scrum-380/):

- `INT-01/`: Logs de ejecución Maven con Testcontainers PostgreSQL y reportes JUnit XML/TXT.
- `INT-02/`: Reporte PDF descargado (`estadisticas_descargadas.pdf`), inspección de texto, respuesta JSON de API y traza Playwright e2e (`playwright-e2e-int02.log`).
- `INT-03/`: Plantilla oficial descargada via HTTP (`plantilla_descargada_http.xlsx`), hojas XLSX de prueba válidas e inválidas, y reporte de peticiones HTTP (`http_importacion_estudiantes.txt`).
- `INT-04/`: Traza de fallo de transporte y verificación de persistencia `enviada=false` (`reporte_integracion_notificaciones.txt`) junto con reportes Surefire de resiliencia.
