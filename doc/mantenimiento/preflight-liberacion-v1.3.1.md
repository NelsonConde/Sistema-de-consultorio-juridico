# Preparación de liberación v1.3.1 — corrección y QA Sprint 8

> **Estado:** candidata PATCH, **no liberada**. Este documento registra el alcance confirmado y los controles pendientes. La fecha, el SHA definitivo de `main` y el número de línea base solo se fijarán cuando existan.

## 1. Referencia de versión

- Última versión publicada: [v1.3.0](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/releases/tag/v1.3.0).
- Commit etiquetado v1.3.0: `d3f71570d41033092ffe366ee177f85fabeaa6fb`.
- Candidata siguiente: **v1.3.1 (PATCH)** por correcciones compatibles, confiabilidad de construcción y ampliación de QA.
- Origen verificado de los cambios: `develop` en `58bce3b61ac23dd2636f9850680a4557aeed7c23` antes de la documentación de preparación.
- Sin cambio directo en las versiones internas Maven (`0.0.1-SNAPSHOT`) ni npm (`0.1.0`); la identificación global se realiza mediante etiqueta Git conforme a la política de versionado.

## 2. Alcance incluido

| Referencia | Tipo | Resultado |
|---|---|---|
| [PR #117](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/117) / SCRUM-386 | FIX | Al desactivar una persona, consultar la versión vigente del recurso para ejecutar la solicitud PATCH con control optimista de concurrencia. |
| [PR #116](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/116) / SCRUM-380 | TEST/DOCS | Casos de integración INT-01 a INT-04: historial de reasignación, estadísticas/PDF, importación XLSX y notificaciones. Se incluyen pruebas y evidencias. |
| [PR #119](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/119) / SCRUM-340 | FIX/CI | Imagen Maven 3.9.14/Java 21 en la etapa Docker y caché/reintentos de la distribución Maven Wrapper en CI. |
| [PR #122](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/122) / SCRUM-378 | FIX/QA | Asteriscos de campos obligatorios en tareas y evidencias de pruebas funcionales FUN-01 a FUN-04. |

La comparación más reciente de `develop` contra `staging`, antes de este ajuste documental, incluyó 40 rutas: funcionalidades, pruebas y evidencias, Dockerfile, workflows y preparación de la liberación.

No se identificaron archivos nuevos ni modificaciones de migraciones SQL en ese diferencial.

## 3. Evidencia técnica disponible

- [PR #117](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/117): revisión independiente registrada, fusionado en `develop`.
- [PR #116](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/116): revisión independiente registrada, fusionado en `develop`.
- [Workflow Build](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/actions/runs/44432315450): estado final `success` tras repetir el job Backend, cuyo primer intento no pudo descargar Maven 3.9.14; Backend, Frontend, Playwright E2E y Backend Docker image satisfactoriamente finalizados.
- CodeQL de `develop`: análisis de acciones, Python, Java/Kotlin y JavaScript/TypeScript satisfactorios.
- [Auditoría QA SCRUM-380](../evidencias/SCRUM-380-auditoria-qa-integracion-sprint-8.md): 4/4 fichas INT-01–INT-04 marcadas PASS en QA local según documento, con evidencias versionadas.
- [PR #122 — SCRUM-378](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/122): FUN-01 a FUN-04 reportados como aprobados en pruebas funcionales locales, junto con ajuste visual de campos obligatorios. El reporte es una evidencia de QA local, no de validación del ambiente remoto.
- [PR #119 — SCRUM-340](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/119): revisado y fusionado a `develop`; la nueva construcción Docker con Maven preinstalado y controles CI se verificaron satisfactorios en el commit `c97c54e179bc5fdf0afcd2e8e4b54f2a60be2862`.
- Estas evidencias no prueban que la misma versión haya pasado validación funcional integrada en `staging` ni acreditan autorización de publicación del Product Owner.

## 4. Puertas de promoción

- [x] PR #118 de preparación documental revisado y fusionado a `develop`. La ampliación documental de SCRUM-378 requiere su propia revisión.
- [ ] Comparación final de `develop` frente a `staging` y `main`; verificar que no entren cambios ajenos al alcance.
- [ ] [PR #121](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/121) `develop` → `staging` mediante **merge commit**, con revisión independiente y todos los checks obligatorios satisfactorios del head vigente.
- [ ] Evidencia de validación requerida para la candidata en el ambiente autorizado. Si está suspendido, registrar explícitamente la limitación; no presentar CI como prueba de despliegue.
- [ ] Aprobación expresa de Juan Camilo Muñoz Niño, Product Owner, y aceptación del cliente cuando aplique.
- [ ] PR `staging` → `main` mediante **merge commit**, revisión independiente y gates CI exitosos.
- [ ] Verificar SHA definitivo de `main`, su correspondencia con la evidencia y el artefacto de despliegue.
- [ ] Crear la etiqueta Git `v1.3.1` **solo** sobre el SHA final verificado; publicar notas de entrega (Release) sin alterar `v1.3.0`.
- [ ] Actualizar registro oficial de línea base, Jira y documentación autorizada con versión, SHA, fecha, pruebas y aprobación.

## 5. Consideraciones

- No hacer push directo ni force push sobre `develop`, `staging` o `main`.
- No actualizar por anticipado en este documento el futuro SHA de promoción ni atribuir aprobaciones inexistentes.
- Mantener conservados los identificadores de `v1.3.0` y releases anteriores.
- No reactivar Railway ni modificar Vercel, Supabase, credenciales o la carpeta oficial de Drive como parte de la preparación documental.

## 6. Registro de promoción

| Elemento | Estado |
|---|---|
| Versión candidata | `v1.3.1` |
| Origen de cambios | PR #116, #117, #119 y #122 integrados en `develop`; documentación inicial PR #118 |
| Preparación | Rama `docs/v1-3-1-patch-release-readiness` |
| PR `develop` → `staging` | PR #121 abierto; CI y revisión final pendientes |
| Evidencia integrada de `staging` | Pendiente |
| Autorización Product Owner | Pendiente de acreditación |
| PR `staging` → `main` | Pendiente |
| SHA final `main` | Pendiente |
| Etiqueta y Release | Pendientes |
| Identificador de línea base | Pendiente de verificar |
