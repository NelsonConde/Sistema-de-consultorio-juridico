# Preparación de liberación v1.3.1 — corrección y QA Sprint 8

> **Estado:** candidata PATCH, **no liberada**. Este documento registra el alcance confirmado y los controles pendientes. La fecha, el SHA definitivo de `main` y el número de línea base solo se fijarán cuando existan.

## 1. Referencia de versión

- Última versión publicada: [v1.3.0](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/releases/tag/v1.3.0).
- Commit etiquetado v1.3.0: `d3f71570d41033092ffe366ee177f85fabeaa6fb`.
- Candidata siguiente: **v1.3.1 (PATCH)** por un arreglo compatible y ampliación de pruebas.
- Origen verificado de los cambios: `develop` en `58bce3b61ac23dd2636f9850680a4557aeed7c23` antes de la documentación de preparación.
- Sin cambio directo en las versiones internas Maven (`0.0.1-SNAPSHOT`) ni npm (`0.1.0`); la identificación global se realiza mediante etiqueta Git conforme a la política de versionado.

## 2. Alcance incluido

| Referencia | Tipo | Resultado |
|---|---|---|
| [PR #117](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/117) / SCRUM-386 | FIX | Al desactivar una persona, consultar la versión vigente del recurso para ejecutar la solicitud PATCH con control optimista de concurrencia. |
| [PR #116](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/116) / SCRUM-380 | TEST/DOCS | Casos de integración INT-01 a INT-04: historial de reasignación, estadísticas/PDF, importación XLSX y notificaciones. Se incluyen pruebas y evidencias. |

Comparación inicial con `main`: 15 archivos (14 archivos añadidos para pruebas/evidencia y una modificación a `frontend/src/components/forms/persona/PersonasForm.jsx`), antes de agregar la documentación de liberación.

No se identificaron archivos nuevos ni modificaciones de migraciones SQL en ese diferencial.

## 3. Evidencia técnica disponible

- [PR #117](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/117): revisión independiente registrada, fusionado en `develop`.
- [PR #116](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/116): revisión independiente registrada, fusionado en `develop`.
- [Workflow Build](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/actions/runs/44432315450): estado final `success` tras repetir el job Backend, cuyo primer intento no pudo descargar Maven 3.9.14; Backend, Frontend, Playwright E2E y Backend Docker image satisfactoriamente finalizados.
- CodeQL de `develop`: análisis de acciones, Python, Java/Kotlin y JavaScript/TypeScript satisfactorios.
- [Auditoría QA SCRUM-380](../evidencias/SCRUM-380-auditoria-qa-integracion-sprint-8.md): 4/4 fichas INT-01–INT-04 marcadas PASS en QA local según documento, con evidencias versionadas.
- Estas evidencias no prueban que la misma versión haya pasado validación funcional integrada en `staging` ni acreditan autorización de publicación del Product Owner.

## 4. Puertas de promoción

- [ ] PR documental hacia `develop` revisado por una persona diferente del autor y CI aprobado.
- [ ] Comparación final de `develop` frente a `staging` y `main`; verificar que no entren cambios ajenos al alcance.
- [ ] PR `develop` → `staging` mediante **merge commit**, con revisión independiente y todos los checks obligatorios satisfactorios.
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
| Origen de cambios | PR #116 + PR #117 en `develop` |
| Preparación | Rama `docs/v1-3-1-patch-release-readiness` |
| PR `develop` → `staging` | Pendiente |
| Evidencia integrada de `staging` | Pendiente |
| Autorización Product Owner | Pendiente de acreditación |
| PR `staging` → `main` | Pendiente |
| SHA final `main` | Pendiente |
| Etiqueta y Release | Pendientes |
| Identificador de línea base | Pendiente de verificar |
