# SCRUM-323 — Diagnóstico de respuestas HTTP 401 en estadísticas

## 1. Identificación y alcance

- Fecha de caracterización inicial: **2026-11-03**, zona **America/Bogota**.
- Reconciliación con Jira y `develop`: **2026-11-04**.
- Fecha de ejecución real: **2026-11-04**, zona **America/Bogota (UTC-05:00)**.
- Proyecto: Sistema de Gestión de Casos Jurídicos.
- Rama: `test/scrum-323-statistics-401-diagnosis`.
- Commit ejecutado: `04c60eb49339a1090ed0d0c4b37bebb8fe736d96`.
- Base inicial: `develop` en `ac16f63`; base de integración revisada: `develop` en `6d637af29f8410a0a7e82ec210707a20326ae2b2`.
- Incidencia: [SCRUM-323](https://gestion-casos-juridicos.atlassian.net/browse/SCRUM-323).
- Relacionados: SCRUM-319, SCRUM-364 y observación de CP-23.
- Backend: Spring Boot 4.0.6, Java 21, `127.0.0.1:8080`.
- Frontend: Next.js, Node.js 22, `localhost:3000`.
- Base: PostgreSQL **16.15** en Docker, `127.0.0.1:55432/legal_cases_scrum323_qa`.

El objetivo fue complementar las pruebas automatizadas aisladas de la PR #84 con peticiones procesadas por el backend y el frontend reales, usando una sesión vigente y otra cuyo JWT expiró realmente. No se intentó reproducir ni reinterpretar el ambiente histórico de CP-23.

La caracterización inicial se realizó sin backend local, Docker, PostgreSQL ni sesiones reales. Ese antecedente justificó la observación de revisión independiente: MockMvc y Playwright demostraban contratos controlados, pero no una ejecución integral. Las secciones siguientes conservan esa evidencia y añaden, sin reemplazarla, la ejecución QA local posterior.

## 2. Entorno QA local

### 2.1. Antecedente de la caracterización inicial

| Elemento | Estado registrado inicialmente |
|---|---|
| Sistema operativo | Windows y PowerShell |
| Java | Temurin OpenJDK 21.0.12.1 |
| Backend local | No escuchaba en `127.0.0.1:8080` |
| Docker y PostgreSQL | No utilizados en esa fase |
| Autenticación HTTP | Simulada mediante filtro exclusivo de pruebas |
| Estadísticas en pruebas HTTP | `EstadisticasService` sustituido por mock |
| Respuestas frontend | Simuladas mediante `page.route` |

Estas condiciones describen exclusivamente la fase inicial y no la ejecución QA real documentada a continuación.

### 2.2. Entorno de la ejecución QA real

Se usó el contenedor exclusivo `scrum323-postgres16-qa`, con volumen `scrum323-postgres16-qa-data`. PostgreSQL 18 de Windows no fue modificado. La base es UTF-8 y exige autenticación por contraseña.

La compilación Maven con Java 21 terminó con código `0`. El artefacto ejecutado fue `legal-cases-backend.jar`, SHA-256 `EFCB17DE464F2AE6C693C4054335C3A9A0EDBD33BDC12FF764DA8452C902679E`, generado desde el commit indicado.

Flyway se ejecutó mediante el artefacto oficial de migraciones: `migrate` y después `validate`, ambos satisfactorios. La versión final fue `20261102.02`. No se ejecutaron `baseline`, `clean` ni `repair`. En runtime, el backend solo validó las migraciones; Hibernate usó `validate`.

Se crearon cuentas separadas de migración y runtime con privilegios mínimos. El usuario sintético activo `scrum323.reportes.qa@example.invalid` quedó asociado a un rol QA independiente cuyo único permiso es `Ver reportes`. Las credenciales permanecen en archivos protegidos fuera del repositorio.

La configuración externa del backend reemplazó el `application.properties` del repositorio mediante `spring.config.location`. Base, servidor, CORS, cookies y servicios auxiliares quedaron limitados a localhost; los schedulers configurables quedaron deshabilitados. La configuración externa del frontend fijó la API en `http://localhost:8080/api`. El JWT se configuró con duración real de **15 000 ms**. CSRF permaneció habilitado.

## 3. Evidencia automatizada aislada

Las pruebas de MockMvc y Playwright existentes caracterizan reglas de seguridad y comportamiento de interfaz con respuestas controladas. Son útiles como regresión, pero por sí solas no demuestran una sesión real expirada, una consulta real a PostgreSQL ni un PDF generado por la aplicación en ejecución.

La evidencia de las secciones 4 y 5 no usa MockMvc, mocks, `page.route` ni interceptores sintéticos.

### 3.1. Resultados históricos conservados

| Comando o suite | Resultado registrado |
|---|---|
| `.\mvnw.cmd -Dtest=EstadisticasControllerAuthorizationTest test` | **PASS:** 3 pruebas, 0 fallos, 0 errores |
| `.\mvnw.cmd "-Dtest=SecurityExceptionHandlerTest,JwtAuthenticationFilterAuthVersionTest" test` | **PASS:** 4 pruebas, 0 fallos, 0 errores |
| `npx playwright test e2e/statistics-401-diagnosis.spec.js --project=chromium --workers=1 --reporter=line` | **PASS:** 5 pruebas, 0 fallos |
| `.\mvnw.cmd -Dtest=ActuatorObservabilityTest test` | **BUILD SUCCESS**; el conteo detallado no fue aportado |
| `EstadisticasControllerHttpSecurityTest`, primera versión | **PASS:** 6 pruebas, 0 fallos, 0 errores |
| `EstadisticasControllerHttpSecurityTest`, versión ampliada | **PASS:** 9 pruebas, 0 fallos, 0 errores, 0 omitidas; 9.939 s en el reporte Surefire registrado |

La suite ampliada cubrió endpoints semestrales y por rango: `401` sin autenticación, `403` con autoridad insuficiente, casos autorizados y cabecera de correlación. El PDF usado en esa prueba era sintético y no debe confundirse con los PDF reales de las secciones 4 y 5.

En la primera ejecución Playwright fallaron dos aserciones de la propia prueba: se observaron dos solicitudes a `/api/estadisticas/semestres` cuando se esperaba una y el diálogo de exportación PDF no se atendía antes de esperar el clic. Se ajustó exclusivamente `frontend/e2e/statistics-401-diagnosis.spec.js`; la repetición terminó con `5 passed (33.0s)`. La causa de la petición duplicada no quedó demostrada.

La inspección y caracterización simulada también conservaron estos hallazgos:

- `SecurityConfig` exige autenticación para endpoints no públicos y `SecurityExceptionHandler` construye respuestas JSON `401` y `403`.
- `JwtAuthenticationFilter` obtiene la autenticación desde cookie.
- Los reportes globales y sus PDF exigen `VER_REPORTES`; `/api/estadisticas/semestres` permite `VER_REPORTES` o `VER_CONSULTAS`.
- Las respuestas verificadas incorporan `X-Request-ID` y el campo JSON `correlacionId`.
- El frontend redirige al inicio ante `401` de `/api/auth/me`, mientras otras operaciones de estadísticas presentan mensajes o alertas diferentes. La falta de uniformidad visual permanece como hallazgo independiente de SCRUM-364.

### 3.2. Verificación automatizada posterior

Después de la ejecución HTTP real se ejecutó este conjunto focalizado:

```powershell
.\mvnw.cmd '-Dtest=EstadisticasControllerAuthorizationTest,EstadisticasControllerHttpSecurityTest,EstadisticasRangoQueryServiceTest,EstadisticasPerfilActualQueryServiceTest,PeriodoEstadisticoServiceTest,JwtAuthenticationFilterAuthVersionTest,SecurityExceptionHandlerTest,CorrelationIdFilterTest' test
```

Resultado: **BUILD SUCCESS**, 34 pruebas, 0 fallos, 0 errores y 0 omitidas.

## 4. Ejecución HTTP real directa

Parámetros idénticos en ambos escenarios:

`fechaInicio=2026-01-01&fechaFin=2026-11-04`

### Escenario A — sesión vigente

| Fecha/hora (UTC-05:00) | Método y endpoint | Estado de sesión | HTTP | Request/correlation ID | Resultado |
|---|---|---:|---:|---|---|
| 2026-11-04 19:33:11.285 | `POST /api/auth/login` | Inicio | 200 | `2f8ab467-f6f6-4135-b92b-ac63df6987e6` | Cookie JWT y CSRF emitidos |
| 2026-11-04 19:33:11.991 | `GET /api/auth/me` | Vigente | 200 | `2766f218-46c3-4806-9716-800339a8b960` | Usuario autenticado; permiso `Ver reportes` confirmado |
| 2026-11-04 19:33:12.205 | `GET /api/estadisticas/reporte?fechaInicio=2026-01-01&fechaFin=2026-11-04` | Vigente | 200 | `ed99dfe5-6e16-4583-91a3-42801af632fb` | JSON real; total 0, finalizadas 0, pendientes 0 |
| 2026-11-04 19:33:12.490 | `GET /api/estadisticas/reporte/pdf?fechaInicio=2026-01-01&fechaFin=2026-11-04` | Vigente | 200 | `1ec0f040-3959-4aee-993d-3266295a96bd` | `application/pdf`, 1692 bytes, firma `%PDF-` |

El PDF directo se guardó fuera del repositorio. SHA-256: `F0BF5E084F979DE4FA30984720D24E11B92F90EE3D401B72A7AE0DE08CCC59BF`.

### Escenario B — sesión expirada realmente

Se inició una sesión nueva, se confirmó inicialmente válida y se esperaron 17 segundos, superando los 15 segundos configurados. No se borraron cookies ni se simularon respuestas.

| Fecha/hora (UTC-05:00) | Método y endpoint | Estado de sesión | HTTP | Request/correlation ID | Resultado |
|---|---|---:|---:|---|---|
| 2026-11-04 19:33:13.129 | `POST /api/auth/login` | Inicio | 200 | `8440a30b-b22f-48a4-81b6-48a2dd50310a` | Sesión nueva |
| 2026-11-04 19:33:13.266 | `GET /api/auth/me` | Vigente | 200 | `e30ac9f9-c458-4709-b1f0-c2214de021cc` | Validez inicial confirmada |
| 2026-11-04 19:33:30.317 | `GET /api/auth/me` | Expirada | 401 | `9d73e88c-419e-4c9e-9762-5b78b08b7ccb` | Expiración confirmada por el backend |
| 2026-11-04 19:33:30.433 | `GET /api/estadisticas/reporte?fechaInicio=2026-01-01&fechaFin=2026-11-04` | Expirada | 401 | `fcc3212b-6767-450d-9a2d-443ab7c0590a` | Sin estadísticas |
| 2026-11-04 19:33:30.456 | `GET /api/estadisticas/reporte/pdf?fechaInicio=2026-01-01&fechaFin=2026-11-04` | Expirada | 401 | `f39ee198-e616-4090-b108-b3a1e76b459f` | Respuesta JSON de seguridad; no se descargó PDF |

El endpoint PDF expirado respondió **HTTP 401 y no entregó archivo**. La consulta y la descarga de PDF devuelven `401` cuando el JWT está expirado. No es un `403` por falta de permiso. Como contraste real, una petición del mismo usuario vigente a `GET /api/consultas?estado=ACTIVO&page=1&size=10`, recurso para el cual el rol QA no tenía permiso, devolvió `403` con ID `1c9bd467-870e-48d5-9808-7d9eeb7f9101`.

## 5. Ejecución real desde el frontend

Se utilizó Microsoft Edge headless contra el Next.js real y el backend real, sin sustitución de red.

### Sesión vigente

| Fecha/hora (UTC-05:00) | Petición | HTTP | Request/correlation ID | Resultado visible |
|---|---|---:|---|---|
| 2026-11-04 19:37:39.576 | Consulta de estadísticas | 200 | `e2405538-ef87-452e-958b-51980a720d19` | Informe mostrado |
| 2026-11-04 19:37:39.744 | Descarga PDF | 200 | `fb936ba9-7696-4923-8967-d4f6be8238ad` | Descarga terminada; 1692 bytes y firma `%PDF-` |

El PDF descargado por el frontend tuvo SHA-256 `F0BCA0657BF3E2A8EB32B6C4B8DF6FFD2722B39333F29A9B5CFD37E2A7341ACD`.

Los dos SHA-256 documentados corresponden a **dos descargas independientes**: una solicitud HTTP directa y una descarga iniciada desde el frontend. No se espera que sus hashes sean iguales porque cada PDF fue generado por separado.

### Sesión expirada

Tras un login nuevo, la consulta inicial respondió 200 (`5211c2cc-0f0b-45c8-8db7-5c02e54dab8e`). Después de 17 segundos:

| Fecha/hora (UTC-05:00) | Petición | HTTP | Request/correlation ID | Resultado visible |
|---|---|---:|---|---|
| 2026-11-04 19:38:00.557 | Descarga PDF | 401 | `d24fd6a6-5317-4bed-9d00-196bece72096` | No hubo archivo; alerta `El PDF no está disponible aún.` |
| 2026-11-04 19:38:00.861 | Consulta de estadísticas | 401 | `58a41d57-213b-4d28-88b2-27ec775dcf33` | La página mostró el estado relacionado con sesión |
| 2026-11-04 19:38:01.335 | `GET /api/auth/me` tras recarga | 401 | `f5cb2045-d789-4b4b-96f3-80214bc34299` | Redirección a `http://localhost:3000/` |

Se observaron peticiones duplicadas de autenticación y estadísticas durante el modo de desarrollo de Next.js. Cada una tuvo su propio ID; esto no cambia la clasificación 200/401 y queda como limitación del modo de ejecución, no como conclusión sobre producción.

## 6. Diagnóstico

La reproducción local demuestra que:

1. Una sesión vigente con `Ver reportes` obtiene la estadística y el PDF reales con HTTP 200.
2. El mismo flujo y los mismos parámetros devuelven HTTP 401 después de la expiración real del JWT.
3. El PDF no se genera ni descarga con la sesión expirada.
4. La falta de permiso se distingue como HTTP 403 mientras la sesión sigue vigente.

Este es el comportamiento de seguridad esperado del entorno QA local. No se identificó un defecto funcional de backend en esta reproducción. El texto genérico mostrado por el frontend al fallar la descarga expirada puede mejorarse en una incidencia separada si producto requiere distinguir explícitamente sesión vencida; no se modificó código funcional en SCRUM-323.

### Incidente histórico CP-23

La causa histórica permanece **INCONCLUSA**. No existe en esta investigación una traza original de CP-23 que demuestre qué petición produjo el 401 ni el estado de su sesión. La reproducción local no autoriza a atribuir retrospectivamente CP-23 a expiración del JWT.

El resultado histórico de CP-23 se conserva como **«Aprobó»**.

También se conserva la trazabilidad previa: no se demostró relación causal con CP-26 y la diferencia de referencias RF96/RF97 frente a RF56/RF57 continúa siendo un asunto documental separado. La ejecución QA completa los criterios locales de comparación, pero no sustituye una traza histórica inexistente.

## 7. Criterios de aceptación

| Criterio | Evidencia | Estado |
|---|---|---|
| Probar estadísticas con sesión vigente | Backend real: consulta y PDF 200 | Cumplido en QA local |
| Repetir la misma operación con sesión expirada | JWT de 15 s, espera real de 17 s; consulta y PDF 401 | Cumplido en QA local |
| Identificar petición y distinguir 401/403 | Matrices con método, endpoint, timestamp e ID; contraste real 403 | Cumplido en QA local |
| Verificar generación y descarga PDF | Dos descargas reales, firma, tamaño y SHA-256 | Cumplido en QA local |
| Registrar comportamiento del frontend | Alerta, estado de página y redirección documentados | Cumplido en QA local |
| Determinar causa histórica de CP-23 | Falta la traza histórica original | Inconcluso; no bloquea la caracterización local |
| Conservar CP-23 «Aprobó» | No se alteró | Cumplido |

## 8. Comandos y archivos de trazabilidad

Además de los comandos automatizados conservados en la sección 3, se ejecutaron `migrate` y `validate` mediante el artefacto oficial de migraciones, y el backend se inició con `spring.config.location` apuntando al archivo externo protegido. Los comandos completos que contenían rutas a archivos de credenciales no se publican para evitar convertir detalles operativos sensibles en evidencia versionada.

No se ejecutaron `baseline`, `clean` ni `repair`. No se publican cookies, JWT, contraseñas ni encabezados de autenticación.

Archivos de trabajo y referencias conservados de la fase inicial:

- `backend/app/src/test/java/co/edu/ufps/legal_cases/business/controller/estadisticas/EstadisticasControllerHttpSecurityTest.java`.
- `frontend/e2e/statistics-401-diagnosis.spec.js`.
- `doc/evidencias/SCRUM-364-diagnostico-fe-estadisticas-401.md` como referencia no modificada.
- Commit inicial de la evidencia: [`bff55e4`](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/commit/bff55e4d4c1113b8d45e257f686bf63c5a75605d).
- Pull request: [#84](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/84).

## 9. Limitaciones y conclusión

- La base QA estaba vacía de casos; por ello el reporte válido contiene conteos cero, pero fue calculado por el servicio real y el PDF fue generado efectivamente.
- La ejecución corresponde al commit y ambiente local descritos, no al ambiente histórico de CP-23.
- Durante la preparación inicial del frontend, Next.js intentó obtener automáticamente un binario nativo faltante. La ejecución válida posterior se realizó en un contenedor Node 22 ya disponible, sin `pull`, con dependencias locales y API restringida a localhost.
- Los IDs registrados son los valores reales de `X-Request-ID`; no se observaron otros correlation IDs.

La evidencia solicitada para QA local está completa y es técnicamente verificable. La PR #84 queda **lista para nueva revisión independiente** en cuanto al alcance de SCRUM-323, manteniendo explícitamente inconclusa la causalidad histórica de CP-23.
