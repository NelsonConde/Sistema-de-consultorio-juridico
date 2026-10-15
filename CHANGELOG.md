# Registro de cambios — Sistema de Gestión de Casos Jurídicos

> **Borrador documental — SCRUM-360.** No constituye por sí mismo una liberación ni una línea base aprobada. Los eventos se incorporan únicamente cuando existe trazabilidad verificable a Git, PR, Jira, pruebas o línea base. Las versiones siguen la convención `vMAJOR.MINOR.PATCH` del Manual de Gestión de Ramas y el Plan de Gestión de la Configuración vigentes.

## [Unreleased]

Cambios en desarrollo, revisión o pendientes de liberar. Esta sección **no declara funcionalidades terminadas**.

- **Sprint 7 — Cambios y cierre:** seguimiento de cambio de contraseña propia (SCRUM-262), invitación de activación de cuentas (SCRUM-263), reasignación de responsables (SCRUM-302), historial de reasignaciones (SCRUM-303) y atención de hallazgos de auditoría (SCRUM-315). Su inclusión aquí expresa trabajo rastreado, **no evidencia de incorporación a `main`**.
- **Gobernanza y build:** SCRUM-337 agrupa documentos de gobierno, escaneos y checksum Maven. SCRUM-360 aporta únicamente borradores de `CONTRIBUTING.md` y `CHANGELOG.md`; SCRUM-338 mantiene la revisión e integración de archivos de gobierno. No se dan por finalizadas las subtareas técnicas por publicar este borrador.

## Cambios identificados en develop frente a main — revisión 2026-10-14

**Estado: integrados o presentes en `develop`; NO declarados liberados en `main`.** La comparación `main...develop` devolvió ramas divergentes: `develop` está 29 commits por delante y 4 por detrás de `main`. Este recuento no equivale a 29 funcionalidades ni acredita un release.

### Added / Changed (en integración)

- **Gestión de perfiles:** nuevo validador `PerfilOperacionEstadoValidator` y cambios en servicios de perfiles administrativo, asesor, monitor, conciliador y estudiante, junto con pruebas de operaciones y reactivación. Requiere evaluación de criterios de negocio y evidencias para su cierre.
- **Reasignación de responsables:** nuevos servicios, DTO, entidad y repositorio de historial de reasignaciones de consultas, con casos de prueba y cambios de controller. La presencia de código no demuestra que la característica esté desplegada ni que el flujo frontend esté completo.
- **Migración de historial:** `V20261011_01__create_consulta_reasignacion_historial.sql`, script `..._verify.sql` y `..._recovery.sql` añadidos en `develop`. La documentación indica que la recuperación no borra una tabla que ya tenga eventos.
- **Autenticación y sesiones:** ajustes de servicios de autenticación, recuperación de contraseña y documentación de cookies JWT/CSRF, con pruebas de sesión expirada y reactivación de perfiles.
- **Frontend:** actualización de la dependencia declarada de Next.js de `^16.2.6` (`main`) a `^16.4.1` (`develop`); no confundir la versión del paquete con un tag de producto.
- **CI:** `.github/workflows/build.yml` añade `permissions: contents: read` en `develop`. Es una modificación de configuración, no acreditación de escaneos o controles de seguridad aún en curso.
- **Documentación técnica:** ajustes a API, backend, reglas, decisiones, mantenimiento y evidencias para reflejar cambios en desarrollo.

**Fuente de contraste:** [comparación `main...develop`](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/compare/main...develop) y archivos individuales de cada rama. Antes de promover estas entradas bajo una versión publicada, verificar PR fusionado, pruebas, aprobación, línea base y coincidencia del commit desplegado.

## Línea base histórica de referencia — 2026-10-08

La etiqueta Git [`v1.1.0`](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/tree/v1.1.0) **existe** y apunta al commit [`c02f66d04b1baabb6a29d01e5768fbad9ed93d9c`](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/commit/c02f66d04b1baabb6a29d01e5768fbad9ed93d9c), utilizado como referencia por SCRUM-315. Se corroboró comparando el tag con ese SHA en GitHub: ambas referencias son idénticas.

**Alcance de la verificación:** la existencia de la etiqueta Git y su commit asociado están confirmados. **Siguen pendientes de comprobación documental** la aprobación de la liberación, el registro formal de línea base, la fecha de liberación aprobada y la correspondencia con el commit efectivamente desplegado. La presencia del tag, por sí sola, no acredita dichos hitos. El plan de configuración también contiene líneas base *previstas* que no deben presentarse como aprobadas sin evidencia.

## Historial por verificar y clasificar antes de publicar una versión

| Área de cambios | Fuente de trazabilidad a contrastar | Condición de registro |
| --- | --- | --- |
| Paginación y búsqueda del lado del servidor | CC-OPT-001; Sprint 5; PR y commits integrados | Asociar con el tag/commit de `main` correspondiente. |
| Gestión documental segura y vista unificada del expediente | CC-OPT-002; Sprint 6; PR y commits integrados | Distinguir cambios realmente fusionados y pruebas acreditadas. |
| Autogestión de datos de contacto y perfil propio | CC-OPT-003; Sprint 6; PR y commits integrados | No mezclar con cambio de contraseña o invitación aún en Sprint 7. |
| Ficha integral del expediente en PDF | CC-OPT-004; Sprint 6; PR y commits integrados | Identificar la versión/commit en que se incorporó. |
| Cambios de reasignación e historial | CC-OPT-005; Sprint 7 | Mantener en Unreleased hasta integración verificable. |

Esta tabla es una **lista de conciliación documental**, no una declaración de publicación de esos cambios ni una cronología definitiva. Al verificar una integración, sustituirla por entradas bajo la etiqueta `vMAJOR.MINOR.PATCH` correspondiente, con fecha real y referencias a PR/commit.

## Criterios para mantener este archivo

1. Distinguir la existencia comprobada de una etiqueta Git de la liberación formalmente aprobada. Antes de presentar una versión como liberada, acreditar su aprobación, línea base y correspondencia con el despliegue; el tag `v1.1.0` y su commit ya están verificados.
2. Describir cambios según su efecto real (funcionalidad, corrección, seguridad, documentación o mantenimiento), con enlaces a PR/Jira/commit cuando estén disponibles.
3. No equiparar «tarea cerrada en Jira», «PR abierto», «cambio en develop» y «liberación en main»: son hitos distintos.
4. No atribuir éxito a pruebas o escaneos que no se hayan ejecutado. Identificar pendientes y límites de evidencia.
5. Conservar el historial existente; no reescribir versiones anteriores para aparentar adopción retroactiva de estándares.

## Fuentes de este borrador

- SCRUM-315 y SCRUM-337 (plan y evidencias de auditoría), consultados para SCRUM-360.
- *Manual de Gestión de Ramas Git y GitHub* v1.0 y *Plan de Gestión de la Configuración* v1.0.
- [`README.md`](README.md), [`doc/README.md`](doc/README.md) y documentación técnica asociada.
- Historial de GitHub y solicitudes CC-OPT-001 a CC-OPT-005 **pendientes de conciliación exacta por versión**.

**Responsable de aceptación y publicación:** por confirmar en revisión de SCRUM-338; no presupone aprobación técnica de seguridad, licencia o liberación.
