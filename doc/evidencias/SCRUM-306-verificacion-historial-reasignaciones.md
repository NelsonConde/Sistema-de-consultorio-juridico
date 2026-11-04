# SCRUM-306 — Verificación de Historial Inmutable y Consistencia Transaccional

- **Identificador de tarea:** SCRUM-306
- **Resumen:** QA — Verificar historial inmutable y consistencia transaccional
- **Fecha de ejecución:** 01/11/2026 (America/Bogota)
- **Ejecutor / Auditor:** José Manuel Pérez Rodríguez (`josemanuelpr@ufps.edu.co`)
- **Revisor asignado:** Edwin Geanfranco Torres Zapata
- **Rama base:** `origin/develop` (`21d752b66ed574f1be2f646cb21052fc74242bcd`)
- **Entorno de verificación:** Java 21.0.12.1, PostgreSQL 18.6 (loopback sintético aislado), Maven 3.9.14.

---

## 1. Contexto y Prerrequisitos

Se validó el estado de las tareas predecesoras y artefactos integrados:
- **SCRUM-311:** Implementación backend del historial paginado (PR #60 integrado en `develop` en `21d752b`, marcado como Done por Juan Diego).
- **SCRUM-312:** Interfaz de usuario para panel de responsables y diálogo de reasignación (PR #59 integrado en `develop`, marcado como Done por Juan Diego).
- **Esquema de base de datos:** Migración versionada `V20261011_01__create_consulta_reasignacion_historial.sql` con definición DDL, función `validar_responsables_reasignacion_consulta()`, función `prevenir_modificacion_historial_reasignacion()` y triggers asociados.

---

## 2. Matriz de Comprobaciones y Criterios de Aceptación

| Criterio | Descripción de la prueba | Resultado | Evidencia |
|---|---|---|---|
| **Integridad de Migración DDL** | Ejecución de `V20261011_01__create_consulta_reasignacion_historial_verify.sql` validando 8 columnas, 8 restricciones y 2 índices. | **PASS** | Script ejecutado sin excepciones en PostgreSQL 18.6 (`test01_verifyMigrationScriptAndSchemaIntegrity`). |
| **Reasignaciones Sucesivas** | Reasignación secuencial de múltiples roles (ASESOR `a1 -> a2`, MONITOR `m1 -> m2`, ASESOR `a2 -> a3`) sobre la misma consulta. | **PASS** | 3 eventos registrados en `consulta_reasignacion_historial` con tipos, anteriores, nuevos y versiones incrementales (`test02_singleAndMultipleSequentialReassignments`). |
| **Orden y Persistencia de Metadatos** | Consulta al endpoint `GET /api/consultas/{id}/responsables/historial` verificando orden `ocurrido_en DESC, id DESC`, actor, motivo y fechas. | **PASS** | Elementos retornados en orden cronológico inverso exacto, nombres formateados y motivos validados (`test03_orderActorDateReasonAndFormatting`). |
| **Lectura Autorizada y Control de Acceso** | Verificación de seguridad en el endpoint de historial con usuarios anónimos, usuarios sin permiso y administradores. | **PASS** | Anónimo: HTTP 401 Unauthorized; Usuario sin `VER_CONSULTAS`: HTTP 403 Forbidden; Usuario con permiso: HTTP 200 OK (`test04_authorizedReadAndAccessControl`). |
| **Inmutabilidad por Trigger** | Intento de `UPDATE` y `DELETE` directo por SQL sobre registros persistidos en `consulta_reasignacion_historial`. | **PASS** | Bloqueado por trigger `trg_historial_reasignacion_inmutable` con SQLSTATE `55000` ("El historial de reasignaciones es inmutable") (`test05_historicalImmutabilityTriggerRejectsUpdateAndDelete`). |
| **Consistencia Transaccional y Rollback** | Intento de reasignación con datos inválidos / conflicto de clave foránea / error de negocio. | **PASS** | Transacción revertida atómicamente; 0 registros huérfanos insertados en historial y entidad sin cambios (`test06_transactionalConsistencyAndRollbackOnError`). |

---

## 3. Resultados de Pruebas Automatizadas

### Pruebas de Integración PostgreSQL (`ConsultaReasignacionHistorialQaPostgreSqlTest`)

```
[INFO] Running co.edu.ufps.legal_cases.business.service.consulta.reasignacion.ConsultaReasignacionHistorialQaPostgreSqlTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 11.68 s
```

### Pruebas Unitarias de Lógica y DTOs de Reasignación

```
[INFO] Running co.edu.ufps.legal_cases.common.exception.handler.GlobalExceptionHandlerReasignacionTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running co.edu.ufps.legal_cases.business.service.consulta.reasignacion.ConsultaReasignacionServiceTest
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running co.edu.ufps.legal_cases.business.service.consulta.reasignacion.ConsultaReasignacionHistorialQueryServiceTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running co.edu.ufps.legal_cases.business.service.consulta.consulta.ConsultaValidatorReasignacionTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running co.edu.ufps.legal_cases.business.service.acceso.consulta.ConsultaAccessServiceReasignacionTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running co.edu.ufps.legal_cases.business.dto.consulta.ConsultaReasignacionDTOTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0

Total pruebas ejecutadas: 35
Total fallos: 0
Total errores: 0
Estado: BUILD SUCCESS
```

---

## 4. Comando de Reproducción

Para ejecutar la verificación completa de forma reproducible en un cluster local de PostgreSQL sintético:

```bash
bash ./docs/testing/ops/run-sprint7-local-qa.sh
```

---

## 5. Dictamen y Conclusión

La implementación de auditoría del historial de reasignaciones cumple a cabalidad con los 5 criterios de aceptación requeridos en la especificación de SCRUM-306. La persistencia es estrictamente inmutable a nivel de base de datos gracias a los triggers de PostgreSQL, y las operaciones de reasignación conservan la atomicidad transaccional y el control de concurrencia optimista.

Se traslada la tarea a estado **En revisión** para la aprobación formal del revisor designado **Edwin Geanfranco Torres Zapata**.
