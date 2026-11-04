
# SCRUM-323 — Diagnóstico de respuestas HTTP 401 en estadísticas


**Fecha:** 2026-11-03 (America/Bogota)

**Proyecto:** Sistema de Gestión de Casos Jurídicos

**Rama de trabajo:** `test/scrum-323-statistics-401-diagnosis`

**Base inicial:** `develop`, commit `ac16f63`

**Incidencia:** https://gestion-casos-juridicos.atlassian.net/browse/SCRUM-323

**Relacionado:** SCRUM-319, SCRUM-364 y observación de CP-23


## 1. Objetivo y alcance

Caracterizar las respuestas de autenticación y autorización de los endpoints de estadísticas, preservar la trazabilidad de CP-23 y determinar hasta qué punto la evidencia disponible permite explicar el HTTP `401` observado históricamente.

Este trabajo **no modifica** permisos, roles, política de autenticación, servicios de negocio, migraciones ni resultados históricos de auditoría. La investigación distingue las pruebas automatizadas con datos simulados de una reproducción en el entorno original.

## 2. Entorno y método

| Elemento | Evidencia disponible |
|---|---|
| Sistema operativo | Windows, terminal PowerShell |
| Java | Temurin OpenJDK 21.0.12.1 |
| Backend | Spring Boot 4.0.6, Maven Wrapper y JUnit/MockMvc |
| Frontend | Next.js / Playwright Chromium |
| Backend local en `127.0.0.1:8080` | No estaba escuchando (`TcpTestSucceeded: False`) |
| Docker | No disponible en el equipo del ejecutor |
| Base de datos y sesiones reales | No usadas |
| Autenticación en pruebas HTTP | Simulada por filtro restringido a la configuración de test |
| Servicio de estadísticas en pruebas HTTP | Mock de `EstadisticasService` |
| Respuestas frontend | Mock sintético mediante `page.route` |

**Límite metodológico:** MockMvc verifica la cadena HTTP y las reglas de seguridad en un contexto controlado; no comprueba la generación real de estadísticas con PostgreSQL ni una sesión real vencida. Playwright verifica presentación y navegación ante respuestas simuladas; no demuestra cuál petición respondió `401` en CP-23.

## 3. Pruebas ejecutadas

| Comando o suite | Resultado registrado |
|---|---|
| `.\mvnw.cmd -Dtest=EstadisticasControllerAuthorizationTest test` | **PASS:** 3 pruebas, 0 fallos, 0 errores |
| `.\mvnw.cmd "-Dtest=SecurityExceptionHandlerTest,JwtAuthenticationFilterAuthVersionTest" test` | **PASS:** 4 pruebas, 0 fallos, 0 errores |
| `npx playwright test e2e/statistics-401-diagnosis.spec.js --project=chromium --workers=1 --reporter=line` | **PASS:** 5 pruebas, 0 fallos |
| `.\mvnw.cmd -Dtest=ActuatorObservabilityTest test` | **BUILD SUCCESS**; conteo detallado no aportado |
| `.\mvnw.cmd -Dtest=EstadisticasControllerHttpSecurityTest test` — primera versión | **PASS:** 6 pruebas, 0 fallos, 0 errores |
| `.\mvnw.cmd -Dtest=EstadisticasControllerHttpSecurityTest test` — versión ampliada | **PASS:** 9 pruebas, 0 fallos, 0 errores, 0 omitidas |
| `git diff --check` y `git diff --cached --check` | Sin advertencias en la salida aportada |

La suite `EstadisticasControllerHttpSecurityTest` incluye pruebas de los endpoints semestrales y por rango. Los escenarios comprueban rechazo `401` sin autenticación, `403` con autoridad insuficiente, casos autorizados y cabecera de correlación.

El contenido PDF del caso autorizado es sintético.

**Evidencia confirmada:** el reporte Surefire de `EstadisticasControllerHttpSecurityTest` registra 9 pruebas ejecutadas, 0 fallos, 0 errores y 0 omitidas, con una duración de 9.939 segundos. salida actual de `target/surefire-reports/co.edu.ufps.legal_cases.business.controller.estadisticas.EstadisticasControllerHttpSecurityTest.txt` después de añadir los tres escenarios por rango.

## 4. Hallazgos técnicos

### 4.1. Contrato de autenticación y autorización

- `SecurityConfig` requiere autenticación para los endpoints no públicos y utiliza `SecurityExceptionHandler` para construir respuestas JSON `401` y `403`.
- `JwtAuthenticationFilter` lee la autenticación desde cookie y, cuando no puede autenticar, la cadena continúa sin contexto autenticado.
- Los endpoints globales y sus descargas PDF exigen autoridad `VER_REPORTES`.
- `/api/estadisticas/semestres` permite `VER_REPORTES` o `VER_CONSULTAS`.
- La respuesta de seguridad incorpora `X-Request-ID` y el campo JSON `correlacionId` bajo los escenarios verificados.

### 4.2. Manejo frontend del 401

La inspección de SCRUM-364 identifica comportamientos diferentes según la solicitud:

| Petición con 401 simulado | Comportamiento caracterizado |
|---|---|
| `GET /api/auth/me` | Redirige a `/` |
| `GET /api/estadisticas/semestres` | Permanece en la vista sin aviso específico de sesión |
| `GET /api/estadisticas/{año}/semestre/{semestre}` | Error genérico de estadísticas |
| `GET /api/estadisticas/reporte` | Presenta el mensaje recibido del backend cuando existe |
| `GET /api/estadisticas/{año}/semestre/{semestre}/pdf` | Alerta genérica de PDF no disponible |

Se ejecutaron cinco escenarios Playwright con mocks sintéticos.

En una primera ejecución fallaron dos aserciones de prueba:

1. El test esperaba exactamente una petición a `/api/estadisticas/semestres`, pero se registraron dos.
2. La prueba de exportación PDF agotó el tiempo de espera porque el diálogo del navegador no se atendía antes de esperar la finalización del clic.

Se ajustó exclusivamente:

`frontend/e2e/statistics-401-diagnosis.spec.js`

Después de las correcciones, la suite finalizó satisfactoriamente:

```text
Running 5 tests using 1 worker
5 passed (33.0s)
```

La doble petición a semestres se observó en la primera ejecución, pero su causa no queda demostrada con la evidencia disponible.

**Hallazgo respaldado por inspección y caracterización simulada:** el tratamiento visual de respuestas `401` no es uniforme.

Una corrección funcional, si el equipo decide realizarla, debe gestionarse como cambio independiente.

## 5. Investigación del incidente histórico CP-23

| Criterio solicitado por SCRUM-323 | Estado |
|---|---|
| Método y endpoints potenciales | Identificados por inspección y pruebas simuladas |
| Instante y endpoint exactos del `401` histórico | **No confirmados** |
| Estado de sesión vigente frente a vencida en el entorno donde ocurrió | **No comprobado** |
| `correlationId` de la petición histórica | **No disponible** |
| Confirmación de que el PDF histórico finalizó | **No comprobada** |
| Pruebas locales con MockMvc y Playwright | Ejecutadas en entorno controlado |
| Datos, cookies, credenciales o tokens sensibles publicados | Ninguno |

Las respuestas `401` producidas artificialmente no identifican el origen de la observación de CP-23.

Tampoco se ha demostrado una conexión causal con CP-26 ni con diferencias de numeración de requisitos.

Se mantiene el resultado histórico **«Aprobó»** de CP-23, sin interpretar esta investigación como repetición de la auditoría.

La diferencia de referencias RF96/RF97 frente a RF56/RF57 se conserva como asunto de trazabilidad documental separado del diagnóstico HTTP.

## 6. Resultado y límites

### 6.1. Resultado de las verificaciones

**PASS** en las suites identificadas, según las salidas disponibles.

Las pruebas automatizadas permiten comprobar:

- Rechazo de peticiones no autenticadas.
- Rechazo de peticiones con permisos insuficientes.
- Acceso a operaciones permitidas bajo autenticación simulada.
- Generación de respuestas HTTP con identificador de correlación.
- Comportamiento del frontend ante respuestas `401` sintéticas.
- Respuestas de los endpoints de estadísticas semestrales y por rango.

### 6.2. Resultado del diagnóstico histórico

**INCONCLUSO.**

No se dispone de una traza HTTP real sanitizada del entorno donde ocurrió el incidente CP-23 ni de una reproducción con sesiones reales en un entorno equivalente autorizado.

No es posible clasificar todavía el `401` histórico como rechazo legítimo por sesión vencida o como defecto del backend.

Se dispone de un hallazgo independiente de interfaz: las operaciones de estadísticas manejan `401` de forma diferente según la solicitud.

### 6.3. Restricciones de la investigación

- No se utilizó PostgreSQL.
- No se inició una instancia local del backend.
- No se utilizaron sesiones autenticadas reales.
- No se consultaron datos jurídicos.
- No se modificaron permisos ni roles.
- No se reprodujo el incidente histórico CP-23.
- No se modificó el resultado histórico de auditoría.

## 7. Evidencia adicional necesaria para cerrar la investigación causal

1. En un entorno de QA autorizado, capturar exclusivamente fecha y hora, método HTTP, ruta sin identificadores personales, código de estado y `X-Request-ID` o `correlationId`, cuando exista.

2. Repetir la misma operación con sesión vigente y sesión vencida, registrando si la respuesta afecta a `/api/auth/me`, estadísticas JSON o exportación PDF.

3. Confirmar si el PDF se descargó correctamente y si el `401` correspondió a una petición independiente.

4. Si se demuestra un defecto del producto, abrir o vincular un Bug independiente con los pasos de reproducción, resultado observado y comportamiento esperado.

5. Tras la revisión independiente de Nelson y/o José Manuel, comunicar el resultado a la coordinación documental de SCRUM-319 para su incorporación al registro compartido de Sprint 2.

No alterar el registro histórico de auditoría sin autorización del responsable documental.

## 8. Archivos de trabajo y revisión

### 8.1. Archivos creados

`backend/app/src/test/java/co/edu/ufps/legal_cases/business/controller/estadisticas/EstadisticasControllerHttpSecurityTest.java`

`doc/evidencias/SCRUM-323-diagnostico-backend-estadisticas-401.md`

### 8.2. Archivos modificados

`frontend/e2e/statistics-401-diagnosis.spec.js`

### 8.3. Documentación relacionada

`doc/evidencias/SCRUM-364-diagnostico-fe-estadisticas-401.md`

Este documento se utiliza como referencia y no requiere modificaciones dentro de SCRUM-323.

### 8.4. Estado de entrega

| Elemento | Estado |
|---|---|
| Pruebas unitarias y de seguridad existentes | PASS |
| Pruebas Playwright focalizadas | PASS |
| Pruebas HTTP controladas con MockMvc | BUILD SUCCESS |
| Documento de diagnóstico | Preparado |
| Reproducción del incidente histórico | INCONCLUSA |
| Commit | Pendiente |
| Pull Request | Pendiente |
| Revisión independiente | Pendiente |

## 9. Conclusión

Las verificaciones automatizadas de SCRUM-323 respaldan el comportamiento esperado de la autenticación y autorización HTTP de los endpoints de estadísticas en condiciones controladas.

Se confirmó también que el frontend presenta comportamientos diferentes frente a respuestas `401`, hallazgo previamente identificado en SCRUM-364 y caracterizado mediante Playwright.

No obstante, la evidencia obtenida no permite identificar la causa de la respuesta `401` observada históricamente durante CP-23.

Por tanto, el resultado del diagnóstico causal permanece **INCONCLUSO** hasta disponer de una reproducción autorizada o una traza real que permita identificar la petición, el estado de autenticación y la respuesta del servidor.

El resultado histórico **«Aprobó»** de CP-23 permanece sin modificaciones.

Cualquier defecto funcional confirmado posteriormente deberá registrarse y gestionarse de forma independiente, conservando la trazabilidad con SCRUM-319 y SCRUM-323.
