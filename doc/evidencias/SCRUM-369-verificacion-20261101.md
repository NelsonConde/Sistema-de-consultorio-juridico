# SCRUM-369 — Revisión actualizada de habilitación de conciliación

Fecha: 01/11/2026, America/Bogota.
Base: `develop` `21d752b66ed574f1be2f646cb21052fc74242bcd`.
Corrección integrada: PR #53, merge `6abf268`.

SCRUM-368 figura Finalizada. El informe preliminar de la rama
`test/scrum-369-student-conciliation-review`, fechado 15/10, describía la entrega
faltante de entonces; no representa el estado actual ni se altera esa historia.

## Verificación ejecutada

**PASS, 6 invocaciones de integración específicas de estudiantes** dentro de
`Sprint7LocalQaPostgreSqlTest`:

- Alta HTTP con `true`, `false`, `null` y propiedad ausente. Para cada caso se
  contrasta respuesta, fila PostgreSQL, GET detalle, listado y selector de
  conciliación. Solo `true` habilita; `false`, `null` y ausencia producen false.
- La actualización general conserva el valor de conciliación existente aunque
  el payload intente cambiarlo; el endpoint específico sí lo modifica.
- Sin autenticación: 401 y ausencia de estudiante y cuenta persistidos.
- Asesor autenticado con permiso legado de creación: 403 y ausencia de
  estudiante y cuenta persistidos. Se comprueba el control de rol del servicio,
  además de la seguridad HTTP.

**PASS, 10 tests complementarios:** 5 del servicio de creación/actualización,
1 de importación Excel y 4 del selector paginado. Son tests con repositorios o
servicios simulados; la importación no se presenta como una ejecución HTTP con BD.

La suite completa ejecutada junto con SCRUM-367 totaliza **29 tests: 0 fallos,
0 errores, 0 omitidos**. No sumar nuevamente los 10 tests a ese total.

## Ambiente, reproducción y límites

Java 21.0.12.1, Maven 3.9.14 y PostgreSQL 18.6 local, cluster temporal sintético.
MockMvc ejecuta controladores y filtros reales; no se simulan repositorios en
la clase de integración. El esquema se genera con `create-drop` solo en la BD
QA. No valida migraciones, `ddl-auto=validate` ni un despliegue.

```sh
bash docs/testing/ops/run-sprint7-local-qa.sh
```

El runner tiene limpieza de cluster en éxito y error. El test de integración se
omite en ejecuciones ordinarias sin la variable de activación y rechaza URLs
que no sean loopback y bases con prefijo `legal_cases_qa_`.

**Pendiente para aceptación integral:** recorrido visual FE–API–listado–selector
sobre la misma BD, importación Excel persistente y comprobación de exclusión real
de estudiantes ajenos/inactivos. El selector tiene cobertura unitaria de alcance,
pero no se presenta como prueba HTTP de filas ajenas.

## Estado propuesto

Mantener **En revisión** con evidencia actualizada. El antiguo bloqueo por falta
de SCRUM-368 ya no aplica. No recomendar Done hasta completar o aceptar los
límites anteriores y consolidar el resultado con el responsable principal.
