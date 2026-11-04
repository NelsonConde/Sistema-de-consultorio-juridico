# SCRUM-331 — Revalidación de la propuesta

Fecha: 01/11/2026, America/Bogota.
Base actual: `21d752b66ed574f1be2f646cb21052fc74242bcd` (`develop`).
Propuesta: `a75bc7b338567207ec9ea91350bb549f843f69df`.
PR: https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/48

## Comprobaciones realizadas

- El PR no tiene revisiones enviadas ni hilos de revisión al momento de consulta.
  No se encontró una aprobación independiente que permita aceptar la estrategia.
- Los 13 archivos SQL del inventario de la propuesta conservan su SHA-256 sobre
  el `develop` inspeccionado: **13 OK**.
- El POM actual sigue sin declarar Flyway/Liquibase. El runtime conserva
  `spring.jpa.hibernate.ddl-auto=${DB_DDL_AUTO:validate}`; la implementación
  propuesta en otra rama no se presenta como integración ya desplegada.
- La propuesta conserva control de orden, historial, checksum, baseline explícito,
  compatibilidad, respaldo y reversión. No se hallaron observaciones del PR que
  atender ni fundamento para sustituir la decisión propuesta unilateralmente.

## Reproducción

Extraer `doc/evidencias/SCRUM-331-sql-inventory.sha256` de la rama
`docs/scrum-331-migration-strategy`, y ejecutar desde la raíz de la base indicada:

```sh
sha256sum -c /ruta/al/inventario.sha256
```

## Entrega pendiente

Mantener SCRUM-331 en **En revisión**. Responsable BD y Nelson/Edwin deben
validar la decisión y registrar la aceptación identificable en el PR o Jira.
No se suplantan sus identidades ni se toma la falta de comentarios como aceptación.
SCRUM-332 puede inspeccionarse y prepararse, pero no aceptarse como ejecución de
una estrategia aprobada mientras falte esa decisión.
