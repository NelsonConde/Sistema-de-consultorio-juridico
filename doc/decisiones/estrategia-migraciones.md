# SCRUM-331 — Estrategia de migraciones de PostgreSQL

Estado: **propuesta técnica pendiente de validación del responsable BD y del revisor**.
Fecha: 15/10/2026, America/Bogota. Base inspeccionada: `develop`,
`9fbd16320dbd944804d303b6cb1af9b607d74674`.
Esta propuesta no autoriza SCRUM-332 ni acredita ejecución en una base de datos.

## Situación comprobada

El POM usa Spring Boot 4.0.6 y no declara Flyway ni Liquibase. El valor
predeterminado de `spring.jpa.hibernate.ddl-auto` es `${DB_DDL_AUTO:validate}`.
Los SQL operativos están en `backend/app/db/migration/` y `backend/db/`;
no hay un esquema inicial completo versionado para una base vacía.
La configuración de arranque no aplica esos SQL automáticamente.

El manifiesto [SCRUM-331-sql-inventory.sha256](../evidencias/SCRUM-331-sql-inventory.sha256)
fija los bytes inspeccionados. No certifica qué scripts se aplicaron en un
despliegue. Las rutas históricas V23/V25/V26/U26 y la afirmación de Flyway ya
integrado no corresponden con esta versión del repositorio.

## Decisión propuesta

Adoptar **Flyway**, conservando Hibernate en `validate`. La alternativa manual
necesita una excepción aprobada y una bitácora con exclusión mutua, versiones,
hashes y prevención de doble aplicación; mantener instrucciones sueltas no
satisface esos controles. Liquibase también puede cubrirlos, pero introducir
changelogs adicionales no aporta una ventaja identificada frente al SQL
existente. No se aprueba por defecto la excepción manual.

En SCRUM-332, una vez validada esta decisión:

- Declarar `org.springframework.boot:spring-boot-starter-flyway` y
  `org.flywaydb:flyway-database-postgresql`. Usar las versiones administradas
  por el BOM de Spring Boot 4.0.6 y registrar las versiones resueltas en la
  evidencia de ensayo. No elegir una versión flotante.
- Usar exclusivamente `classpath:db/migration`, correspondiente a
  `backend/app/src/main/resources/db/migration/`, como ubicación automática.
  Este directorio se creará en 332; **todavía no existe**.
- Configurar el esquema exacto `DB_consultorioJuridico` y comprobar su
  tratamiento con mayúsculas en PostgreSQL. Usar `flyway_schema_history`
  en ese esquema, `validate-on-migrate=true`, `baseline-on-migrate=false`,
  `clean-disabled=true` y `out-of-order=false`.
- Ejecutar con un único operador/job de despliegue y una credencial de DDL
  separada de la cuenta de aplicación. La configuración concreta debe
  demostrar que la migración termina antes de iniciar tráfico y antes de
  la validación de Hibernate. El runtime no debe requerir permisos de DDL.

## Requisitos y evidencia exigida

| Requisito | Control y criterio de aceptación de SCRUM-332 |
|---|---|
| Orden | Versión única y creciente; migraciones fuera de orden y versiones duplicadas impiden continuar. |
| Historial aplicado | `flyway_schema_history` registra versión, script, resultado y checksum; el acta añade commit, SHA-256, ambiente y operador. Repetir `migrate` no repite efectos. |
| Integridad | Verificar SHA-256 del artefacto contra el commit revisado y ejecutar `validate`. Un cambio de checksum impide desplegar; no usar `repair` para ocultarlo. |
| Compatibilidad | PostgreSQL 16 sintético, esquema vacío y copia legada controlada; Hibernate `validate`, verificadores SQL y regresión del backend con el mismo artefacto. |
| Respaldo | Antes de tocar una copia legada, respaldo verificable, manifiesto, prueba de lectura y plan de restauración. Para datos con objetos, coordinar con SCRUM-343/344. |
| Reversión | Ensayo de fallo transaccional y restauración/compensación; comprobar filas, relaciones y hashes. No borrar eventos históricos ni omitir pérdida de datos. |

## Baseline y transición

Se reserva `20261015_01` como versión de baseline para el ensayo, sujeta a
confirmación en la revisión. La versión no demuestra equivalencia del esquema.

1. **Base vacía:** producir y revisar un SQL completo
   `V20261015_01__baseline_current_schema.sql`, sin datos personales ni secretos.
   Debe incluir secuencias, constraints, índices, funciones y triggers que
   Hibernate no crea. Aplicarlo mediante Flyway desde cero; contrastar catálogo
   y ejecutar `validate` de Hibernate. No convertir `ddl-auto=update` en un
   procedimiento de producción ni confundir el esquema generado para un test
   con un baseline aprobado.
2. **Copia legada equivalente:** inventariar tablas, columnas, tipos, claves,
   índices, triggers y datos sintéticos; comparar contra el baseline aprobado.
   Solo si son equivalentes se permite un `baseline` explícito en `20261015_01`,
   después del respaldo y de la revisión. Registrar hashes y comparación.
   `baseline` registra el punto de partida; no crea ni repara ese esquema.
3. **Copia legada divergente:** detener el procedimiento. Preparar una
   transición nueva con precondiciones para ese estado, ensayarla y revisarla;
   no marcar como aplicada una migración ausente. El orden de versiones
   anteriores no garantiza compatibilidad con cualquier copia.
4. **Cambios futuros:** solo versiones mayores al baseline, con SQL nuevo,
   verificadores fuera de la ubicación automática y compatibilidad con la
   versión de aplicación que todavía esté sirviendo tráfico.

Los SQL históricos se conservan byte por byte en sus rutas actuales. No se
copiará el directorio histórico completo a la ubicación de Flyway. Esto evita
que `*_verify.sql`, `*_rollback.sql` y `*_recovery.sql`, cuyos nombres pueden
parecer migraciones, entren en el descubrimiento automático.

## Inventario operativo real

| Ruta relativa | Clase |
|---|---|
| `backend/app/db/migration/V20260829_01__expand_file_asset_status_constraint.sql` | Transición histórica |
| `backend/app/db/migration/V20260830_01__restructure_audit_log.sql` | Transición histórica de auditoría |
| `backend/app/db/migration/V20260905_01__version_document_model.sql` | Transición histórica documental |
| `backend/app/db/migration/V20261005_01__add_process_creation_timestamp.sql` | Transición histórica de procesos |
| `backend/app/db/migration/V20261011_01__create_consulta_reasignacion_historial.sql` | Transición histórica de reasignaciones |
| `backend/app/db/migration/*_verify.sql` | Verificación explícita; nunca migración automática |
| `backend/app/db/migration/*_rollback.sql`, `*_recovery.sql` | Recuperación explícita y revisada; nunca migración automática |
| `backend/db/optimistic_locking.sql`, `role_base_identity.sql` | SQL operativo adicional que debe incluirse en el análisis del baseline |
| `backend/db/optimistic_locking_verify.sql`, `optimistic_locking_rollback.sql` | Verificación/recuperación fuera del descubrimiento automático |

`V20260905_01` convierte estados a `VIGENTE` antes de ampliar su CHECK: una
copia legada con un CHECK restrictivo necesita un ensayo específico. Este
hallazgo estático no acredita un fallo del despliegue actual. No modificar un
script que pudo ser aplicado para resolverlo; preservar su hash y preparar la
transición correctiva nueva que corresponda al esquema de partida.

## Recuperación y límites

Los SQL con `BEGIN`/`COMMIT` existentes no se reutilizarán a ciegas dentro de
transacciones administradas por el motor. Las nuevas migraciones deben tener
una política transaccional comprobada. Prohibir `clean` y la eliminación manual
del historial como mecanismo de rollback.

La recuperación documental histórica elimina metadatos de versiones: solo es
admisible en un ensayo desechable que registre esa pérdida o mediante
restauración verificada. La recuperación de reasignaciones aborta si hay
eventos; conservarlos y emplear una corrección hacia adelante.

SCRUM-331 no aplica SQL, no cambia dependencias ni configuración de arranque,
no modifica los SQL históricos y no declara un respaldo restaurado.

## Validación y aprobación

- **PASS, inspección estática:** inventario y SHA-256, ausencia de integración
  Flyway/Liquibase en POM, ubicación SQL real y configuración Hibernate.
- **Pendiente:** validación de la decisión por José Manuel Pérez Rodríguez
  (Backend/BD) y revisión independiente por Nelson Enrrique Conde Tarazona
  o Edwin Geanfranco Torres Zapata. No se suplantan sus identidades.
- **INCONCLUSO, aceptación completa:** sin esas validaciones SCRUM-331 no está
  cerrada y SCRUM-332 no puede implementar una estrategia supuestamente aprobada.
- El inventario no sustituye la evidencia real de SCRUM-332. La aceptación
  incluye PR, commit y aprobación independiente, sin atribuirse una CI anterior.

## Fuentes técnicas

- [Spring Boot 4.0: cambio de módulos y starter Flyway](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide).
- [Flyway: baseline explícito](https://documentation.red-gate.com/fd/baseline-277578867.html).
- [Flyway: baselineOnMigrate y su protección por defecto](https://documentation.red-gate.com/flyway/reference/configuration/flyway-namespace/flyway-baseline-on-migrate-setting).

Las fuentes explican el mecanismo propuesto; no prueban su integración en este repositorio.
