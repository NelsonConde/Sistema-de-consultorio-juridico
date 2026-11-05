# SCRUM-373 — Revisión del diagnóstico SCRUM-356 / SCRUM-372

Fecha: 15/10/2026, America/Bogota. **Resultado técnico: PASS en el alcance
automatizado ejecutado. Revisión humana del PR y cierre Jira pendientes.**
Ejecución asistida por Codex desde la sesión del solicitante; no constituye
aprobación en nombre de José Manuel ni del autor original.

## Fuente y versiones

- SCRUM-372 figura finalizada y entrega el diagnóstico de SCRUM-356.
- PR original [#37](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/37),
  integración `aca0b1e3b63b79eed112e231203a2bf1603e222f`.
- Fuente revisada: `doc/evidencias/diagnostico-seguridad-sesion-expirada.md` y
  `EstudianteSesionExpiradaPostgreSqlTest`.
- Nueva ejecución: `develop` en `9fbd16320dbd944804d303b6cb1af9b607d74674`,
  rama `test/scrum-373-session-expiration-review`. No se cambió código de aplicación.
- Java 21.0.12.1; Maven Wrapper 3.9.14; Spring Boot 4.0.6;
  Testcontainers 2.0.5; Podman 6.1.3 rootless; PostgreSQL 16 efímero.
- Imagen `postgres:16-alpine`, digest observado
  `sha256:1a66d744c1b459e13b05a8fca341da84cb63383e99ce262210efee5a319d4551`.

## Comandos y resultados reales

Desde `backend/app` en el checkout de la versión indicada:

```bash
DOCKER_HOST=unix:///run/user/1000/podman/podman.sock \
TESTCONTAINERS_RYUK_DISABLED=true \
./mvnw -B -Dstyle.color=never -Dtest=EstudianteSesionExpiradaPostgreSqlTest test

./mvnw -B -Dstyle.color=never \
  -Dtest=CsrfFilterSecurityTest,SecurityExceptionHandlerTest,AuthControllerCsrfTest test
```

La ruta del socket corresponde al usuario de esta ejecución; en otra máquina
debe apuntar a su socket Podman accesible. El socket estaba inactivo y se inició
solo para las pruebas; no se habilitó su arranque permanente. Ryuk se desactivó
para Podman rootless. Se comprobó que tras SCRUM-373 no quedaban contenedores
con label `org.testcontainers=true`.

| Suite | Pruebas | Fallos | Errores | Omitidas | Resultado |
|---|---:|---:|---:|---:|---|
| EstudianteSesionExpiradaPostgreSqlTest | 7 | 0 | 0 | 0 | PASS |
| CsrfFilterSecurityTest | 3 | 0 | 0 | 0 | PASS |
| SecurityExceptionHandlerTest | 2 | 0 | 0 | 0 | PASS |
| AuthControllerCsrfTest | 3 | 0 | 0 | 0 | PASS |
| Total de estas ejecuciones | 15 | 0 | 0 | 0 | PASS |

La integración terminó a las 18:25:06 -05:00, duración Maven 1 min 20 s;
las pruebas relacionadas terminaron a las 18:22:07 -05:00, duración 12,274 s.
El orden de las filas no implica orden cronológico. El resumen filtrado está
en [SCRUM-373-resultados.txt](SCRUM-373-resultados.txt).

El primer intento mediante `/var/run/docker.sock` terminó antes de ejecutar los
casos: 1 error de inicialización por acceso denegado al daemon. No se clasificó
como defecto funcional ni como prueba omitida. El cambio de runtime a Podman
permitió ejecutar realmente las siete pruebas; no se modificaron expectativas.

## Contraste de aceptación

| Criterio de SCRUM-356 | Evidencia revisada y nueva ejecución | Resultado |
|---|---|---|
| Comparación autorizada | CP-01 exige HTTP 201, un estudiante, una cuenta, relación y auditoría de éxito. | PASS |
| Ausente, expirado y vigente | CP-02 a CP-04 usan CSRF válido y JWT ausente, expirado firmado o inválido; exigen HTTP 401 y cero persistencia. CP-01 usa JWT vigente. | PASS |
| Expiración real | CP-03 firma con `exp` 60 s anterior al instante de prueba. CP-06 comprueba `/api/auth/me` y espera expiración del JWT de TTL 2 s antes del POST, con espera acotada a 5 s. | PASS |
| Separar CSRF de autenticación | CSRF se obtiene del endpoint real; CP-05 sin CSRF exige 403. Las otras pruebas aíslan el rechazo de autenticación con CSRF válido. | PASS |
| Sin efectos indebidos | Consultas directas a PostgreSQL comprueban cuenta, perfil y FK. CP-07 vuelve a comprobar los rechazos y ausencia de auditoría de creación exitosa. | PASS |
| Conclusión del diagnóstico | Alta sin sesión válida no reproducida para `POST /api/estudiantes`; conservar alcance y limitaciones. | PASS delimitado |

La hora de servidor procede de los timestamps locales de la ejecución y el JWT
se construye con ese reloj. No se publica el token ni su contenido. Inactividad
visual y expiración de cookie no se confunden con la expiración JWT probada.
No se observó un bypass que justifique convertir este diagnóstico en un Bug.

## Evidencia y límites

Se publican exclusivamente líneas de resultado Maven; no se copian volcados
MockMvc, variables de entorno, cookies, payloads o propiedades JVM. Una búsqueda
de valores con estructura JWT en los logs de ambas ejecuciones encontró cero;
esta comprobación de patrón no sustituye una auditoría exhaustiva de secretos.
Las pruebas de auditoría verifican eventos de rechazo y ausencia de éxito
indebido. No se usaron credenciales ni datos de usuarios reales.

Las pruebas usan contexto Spring completo, cadena real de seguridad, JWT reales,
CSRF real y datasource comprobado contra el Testcontainer. El esquema se crea y
elimina solo en el contenedor. No se consultó el datasource de la aplicación,
no se llamó a Brevo ni al Storage externo y no se modificó producción.

No se ejecutó navegador, suite completa ni todos los endpoints de alta. Estas
15 pruebas son una ejecución nueva, distinta de las 7+13 históricas del informe
original; no se reutiliza CI anterior ni se afirma CI para este commit nuevo.
El PR requiere revisión independiente antes de integrar y cerrar SCRUM-373 o
consolidar el cierre de SCRUM-356.
