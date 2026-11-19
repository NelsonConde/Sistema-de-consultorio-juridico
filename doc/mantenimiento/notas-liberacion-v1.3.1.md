# Notas de entrega — v1.3.1 (candidata)

> **Estado: preparada para revisión.** Este archivo no declara publicada la versión `v1.3.1` ni reemplaza una autorización pendiente. Antes de publicar, confirmar el SHA definitivo de `main`, pruebas, revisión, aprobación del Product Owner y fecha de publicación.

**Tipo de versión:** PATCH — corrección compatible y pruebas de regresión.  
**Versión anterior:** [v1.3.0](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/releases/tag/v1.3.0).  
**Versión candidata:** `v1.3.1`.  
**Commit de referencia de los cambios en `develop`:** `58bce3b61ac23dd2636f9850680a4557aeed7c23` (antes de agregar la preparación documental).

## Resumen

Esta entrega corrige la desactivación de personas y mejora la identificación de campos obligatorios en tareas. Amplía las pruebas funcionales y de integración del Sprint 8 y fortalece la confiabilidad de construcción Docker/CI del backend.

## Corrección funcional

- **Desactivación de personas:** consulta del detalle vigente antes de invocar el endpoint `PATCH` de desactivación y uso de la versión actual del registro, preservando el control optimista de concurrencia. No se introduce un reintento automático de una acción rechazada por conflicto.
- Trazabilidad: [PR #117 — SCRUM-386](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/117).
- **Seguimientos/tareas:** etiquetas visuales de Categoría y Descripción como campos obligatorios, sin alterar el contrato de validación ([PR #122 — SCRUM-378](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/122)).

## Pruebas y evidencias

- **INT-01 — Reasignación e historial:** integridad y concurrencia con PostgreSQL/Testcontainers.
- **INT-02 — Estadísticas y PDF:** coherencia entre datos del reporte, PDF y fecha local.
- **INT-03 — Importación XLSX:** plantilla, errores de cabecera y procesamiento de filas.
- **INT-04 — Notificaciones y correo:** selección de destinatarios y comportamiento ante indisponibilidad simulada del proveedor.
- Trazabilidad: [PR #116 — SCRUM-380](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/116) y [documento de auditoría](../evidencias/SCRUM-380-auditoria-qa-integracion-sprint-8.md).
- El informe de QA local documenta cuatro casos aprobados. No se presenta como prueba de despliegue funcional sobre un entorno remoto.
- **FUN-01 a FUN-04:** pruebas funcionales locales de consulta, proceso, seguimiento/tarea y conciliación. Reporte y capturas incorporados mediante [PR #122 — SCRUM-378](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/122). La evidencia disponible no acredita por sí misma ejecución en `staging`.

## Confiabilidad de compilación y despliegue

- El backend utiliza una imagen de construcción con Maven 3.9.14 y Java 21 ya instalados, evitando descargar la distribución de Maven Wrapper durante la construcción Docker.
- GitHub Actions almacena en caché la distribución del Maven Wrapper y reintenta su inicialización hasta tres veces, conservando la comprobación SHA-256 ([PR #119 — SCRUM-340](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/119)).
- Se mantienen las comprobaciones de presencia de los artefactos JAR de ejecución y migraciones. La construcción exitosa no garantiza disponibilidad de todos los servicios externos en Railway.

## Compatibilidad y configuración

- Cambios compatibles con la versión anterior.
- No se identificaron cambios de migraciones SQL ni de versiones internas Maven/npm en los PR de alcance funcional y técnico de esta candidata.
- No se modifican las versiones internas de los artefactos Maven/npm; el sistema se identifica mediante la etiqueta global Git.
- La corrección de construcción del [PR #119](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/119) es adicional a la del Dockerfile publicada en `v1.2.1`.

## Verificaciones disponibles

- El CI post-merge de `develop` en `58bce3b61ac23dd2636f9850680a4557aeed7c23` finalizó con éxito: Backend, Frontend, Playwright E2E, Backend Docker image y análisis de seguridad.
- El primer intento del job Backend falló al descargar Maven 3.9.14 desde Maven Central; la nueva ejecución fue satisfactoria.
- La actualización funcional de [PR #122](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/122) está fusionada en `develop`; sus controles finales, la revisión de promoción y las autorizaciones correspondientes deben corroborarse antes de publicar esta entrega.

## Trazabilidad de promoción

- Cambios funcionales, técnicos y QA: [PR #117](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/117), [PR #116](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/116), [PR #119](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/119) y [PR #122](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/122).
- Preparación del candidato: [PR #118](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/118).
- PR `develop` → `staging`: [PR #121](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/121) abierto, pendiente de aprobación independiente y verificación completa del alcance actualizado.
- PR `staging` → `main`: pendiente.
- SHA final de liberación, etiqueta y enlace de GitHub Release: pendientes de verificación y publicación.

**Recomendación para las notas definitivas:** sustituir esta cabecera y los campos pendientes únicamente con información observada, después de crear la versión aprobada sobre el SHA definitivo de `main`.
