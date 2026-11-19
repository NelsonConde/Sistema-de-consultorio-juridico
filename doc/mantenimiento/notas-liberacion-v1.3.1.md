# Notas de entrega — v1.3.1 (candidata)

> **Estado: preparada para revisión.** Este archivo no declara publicada la versión `v1.3.1` ni reemplaza una autorización pendiente. Antes de publicar, confirmar el SHA definitivo de `main`, pruebas, revisión, aprobación del Product Owner y fecha de publicación.

**Tipo de versión:** PATCH — corrección compatible y pruebas de regresión.  
**Versión anterior:** [v1.3.0](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/releases/tag/v1.3.0).  
**Versión candidata:** `v1.3.1`.  
**Commit de referencia de los cambios en `develop`:** `58bce3b61ac23dd2636f9850680a4557aeed7c23` (antes de agregar la preparación documental).

## Resumen

Esta entrega corrige la desactivación de personas cuando el frontend necesita actualizar la versión del registro antes de enviar la operación, y amplía la cobertura de pruebas y evidencias de integración del Sprint 8.

## Corrección funcional

- **Desactivación de personas:** consulta del detalle vigente antes de invocar el endpoint `PATCH` de desactivación y uso de la versión actual del registro, preservando el control optimista de concurrencia. No se introduce un reintento automático de una acción rechazada por conflicto.
- Trazabilidad: [PR #117 — SCRUM-386](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/117).

## Pruebas y evidencias

- **INT-01 — Reasignación e historial:** integridad y concurrencia con PostgreSQL/Testcontainers.
- **INT-02 — Estadísticas y PDF:** coherencia entre datos del reporte, PDF y fecha local.
- **INT-03 — Importación XLSX:** plantilla, errores de cabecera y procesamiento de filas.
- **INT-04 — Notificaciones y correo:** selección de destinatarios y comportamiento ante indisponibilidad simulada del proveedor.
- Trazabilidad: [PR #116 — SCRUM-380](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/116) y [documento de auditoría](../evidencias/SCRUM-380-auditoria-qa-integracion-sprint-8.md).
- El informe de QA local documenta cuatro casos aprobados. No se presenta como prueba de despliegue funcional sobre un entorno remoto.

## Compatibilidad y configuración

- Cambios compatibles con la versión anterior.
- Los PR #116 y #117 no incorporan migraciones SQL ni modificaciones a dependencias, archivos de entorno o credenciales.
- No se modifican las versiones internas de los artefactos Maven/npm; el sistema se identifica mediante la etiqueta global Git.
- El backend conserva la corrección de Docker/Maven Wrapper publicada previamente.

## Verificaciones disponibles

- El CI post-merge de `develop` en `58bce3b61ac23dd2636f9850680a4557aeed7c23` finalizó con éxito: Backend, Frontend, Playwright E2E, Backend Docker image y análisis de seguridad.
- El primer intento del job Backend falló al descargar Maven 3.9.14 desde Maven Central; la nueva ejecución fue satisfactoria.
- Los controles de los PR de preparación y promoción y sus autorizaciones deberán corroborarse antes de publicar esta entrega.

## Trazabilidad de promoción

- Cambios funcionales y QA: [PR #117](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/117) y [PR #116](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/116).
- Preparación del candidato: [PR #118](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/118).
- PR `develop` → `staging`: pendiente.
- PR `staging` → `main`: pendiente.
- SHA final de liberación, etiqueta y enlace de GitHub Release: pendientes de verificación y publicación.

**Recomendación para las notas definitivas:** sustituir esta cabecera y los campos pendientes únicamente con información observada, después de crear la versión aprobada sobre el SHA definitivo de `main`.
