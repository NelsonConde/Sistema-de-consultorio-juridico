# SCRUM-375 — Revisión del diagnóstico SCRUM-357 / SCRUM-374

Fecha: 15/10/2026, America/Bogota. **Resultado técnico: PASS para la
caracterización histórica ejecutada. Revisión humana del PR y cierre pendientes.**
Ejecución asistida por Codex desde la sesión del solicitante. No constituye una
aprobación funcional ni una firma de José Manuel o del autor original.

## Identidad del trabajo y versiones

SCRUM-374 está finalizada y entrega el diagnóstico de SCRUM-357 mediante el
PR [#38](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/38).
Se revisaron su informe, sus doce pruebas y los comentarios de entrega en Jira.

- Código ejecutado: commit del diagnóstico
  `09b7bbe273173b8433d4c7adfb8bf1da03dc9613`.
- Integración histórica: `6017dcaa81dd7f7d61256568830e539aa27fcbce`.
- Evidencia original: `doc/evidencias/diagnostico-reactivacion-perfiles.md`.
- Suite: `PerfilEstudianteReactivacionPostgreSqlTest` de ese commit.
- Artefacto de esta revisión: rama `test/scrum-375-profile-diagnosis-review`,
  creada desde `develop` en `9fbd16320dbd944804d303b6cb1af9b607d74674`.

Se extrajo el árbol original con `git archive` a un directorio aislado antes de
compilar. No se modificaron las expectativas del diagnóstico para obtener PASS.
La suite homónima actual ya incorpora la política de SCRUM-376; ejecutarla no
demostraría la reproducción del comportamiento histórico de SCRUM-374.

## Ambiente, comando y resultado nuevo

Java 21.0.12.1, Maven Wrapper 3.9.14, Spring Boot 4.0.6,
Testcontainers 2.0.5, Podman 6.1.3 rootless, PostgreSQL 16 efímero.
Imagen `postgres:16-alpine`, digest observado:
`sha256:1a66d744c1b459e13b05a8fca341da84cb63383e99ce262210efee5a319d4551`.

Desde `backend/app` del árbol **09b7bbe**:

```bash
DOCKER_HOST=unix:///run/user/1000/podman/podman.sock \
TESTCONTAINERS_RYUK_DISABLED=true \
./mvnw -B -Dstyle.color=never -Dtest=PerfilEstudianteReactivacionPostgreSqlTest test
```

El socket corresponde al usuario local de esta ejecución; adaptar su ruta en
otro ambiente. Se desactivó Ryuk para Podman rootless y se comprobó al terminar
que no quedaban contenedores con label `org.testcontainers=true`.

| Suite histórica | Pruebas | Fallos | Errores | Omitidas | Resultado |
|---|---:|---:|---:|---:|---|
| PerfilEstudianteReactivacionPostgreSqlTest | 12 | 0 | 0 | 0 | PASS de diagnóstico |

Fin: 18:27:42 -05:00. Duración Maven: 1 min 46 s; duración de la clase: 60,65 s.
Ver [resumen filtrado](SCRUM-375-resultados.txt).

## Contraste independiente de criterios

| Criterio de SCRUM-357 | Comprobación | Resultado y límite |
|---|---|---|
| Identidad, rol, estado y relaciones antes/después | P01 usa FK para distinguir cuenta compartida de identidades semejantes. P02-P05 comprueban perfil actual ASESOR, rol, permisos, reactivación y alcance. | PASS: la identidad no se deduce de correos o nombres. |
| Contrastar requisitos y perfil actual | El informe histórico separa reglas existentes, vacíos y propuestas RF-P01 a RF-P06. Jira SCRUM-374 registra posteriormente la aprobación comunicada y la separación de SCRUM-376. | PASS documental para el diagnóstico. No se atribuye una aprobación nueva ni se audita una fuente oficial de Drive. |
| Reactivación, cambio explícito y efectos de acceso | P03, P07-P10 reproducen coexistencia, suspensión y rehabilitación indirectas; P06 distingue cambio explícito y registro de historial. | PASS de caracterización; los riesgos reproducidos no son conformidad funcional. |
| Atomicidad y aislamiento | P11 comprueba rechazo y ausencia de transición ante consulta operativa; P12 comprueba ausencia de efectos sobre otra identidad. | PASS en la muestra probada. |
| Decisión y seguimiento separados | El diagnóstico no implementa la política. La corrección posterior está identificada como SCRUM-376 / PR #39. | PASS de trazabilidad; esta revisión no aprueba la corrección. |

Se confirmó nuevamente el comportamiento anterior: reactivar un estudiante
histórico podía conservar ASESOR como perfil actual y dejar ambos perfiles
activos; modificar el perfil no actual podía suspender o rehabilitar la cuenta.
Las doce pruebas pasan porque describen ese comportamiento. No se declara que
sea aceptable bajo la política aprobada posteriormente.

## Evidencia, aislamiento y límites

La suite usa Spring completo, MockMvc con JWT/CSRF reales y PostgreSQL real;
comprueba URL JDBC, catálogo `legal_cases_test`, producto y versión mayor 16
antes de preparar datos. Solo usa fixtures sintéticos. El schema se crea y
elimina en el Testcontainer; no se consultaron datos de producción ni se
invocaron integraciones externas de correo o almacenamiento.

El adjunto contiene solo líneas de resumen Maven. La búsqueda de valores con
estructura JWT en el log de esta ejecución encontró cero; esto no equivale a una
auditoría exhaustiva de secretos. No se publican cookies, passwords, JWT,
propiedades JVM, payloads o volcados de requests.

Las 12 pruebas son una ejecución nueva. Las 31 pruebas relacionadas citadas
en la evidencia histórica no se reutilizan como ejecución nueva de esta revisión.
No se ejecutó navegador, suite completa ni auditoría de datos existentes; no se
verificó la corrección SCRUM-376 ni el despliegue actual. No se afirma una CI
nueva ni se cambia la identidad del diagnóstico por la del arreglo posterior.

El cambio requiere revisión de una persona distinta del autor antes de integrar;
el resultado no cierra automáticamente SCRUM-375 ni su padre SCRUM-357.
