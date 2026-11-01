# SCRUM-369 — Revisión independiente de SCRUM-354 (Habilitación de conciliación)

**Estado de la revisión: PASS / VERIFICADO sobre develop.**  
Fecha inicial: 15/10/2026 (bloqueado por falta de entrega).  
**Fecha de actualización y verificación: 01/11/2026, America/Bogota.**  
Base verificada: `develop` en commit `21d752b66ed574f1be2f646cb21052fc74242bcd`.  
Entrega técnica comprobada: PR #53 (merge `6abf268` por Edwin Torres).

---

## 1. Contexto y superación del bloqueo previo

El informe preliminar del 15/10/2026 registró estado *INCONCLUSO / BLOQUEADO* debido a que SCRUM-368 no se encontraba integrada en `develop` y el código forzaba `conciliacion=false` en el alta de estudiantes.

Con la finalización formal de SCRUM-368 y la integración del PR #53 en `develop`, se levantó dicho bloqueo. La revisión estática y dinámica confirma que `EstudianteCommandService.crear` implementa la semántica correcta:

```java
Boolean conciliacionInicial = Boolean.TRUE.equals(dto.getConciliacion());
DatosEstudiante datos = prepararDatos(dto, true, conciliacionInicial);
```

---

## 2. Matriz de verificación de criterios de aceptación

| Criterio de SCRUM-354 | Verificación ejecutada | Resultado |
|---|---|---|
| **Alta autorizada con `true`** | Petición HTTP POST `/api/estudiantes` con payload `conciliacion: true`. Se contrasta respuesta (201 Created), fila real en PostgreSQL (`conciliacion = true`), endpoint GET de detalle, listado general y selector de conciliación. | **PASS** |
| **Valores `false`, `null` y propiedad ausente** | Se envían tres payloads distintos (`false`, `null` y omitiendo la clave). En los tres casos, la respuesta y la persistencia en base de datos registran `conciliacion = false`. | **PASS** |
| **Payload no autenticado o sin permiso** | Petición sin autenticación retorna 401; asesor sin rol administrativo retorna 403. Se verifica con `JdbcTemplate` en PostgreSQL que no se crea usuario ni perfil en base de datos. | **PASS** |
| **Preservación y no regresión en actualizaciones** | PUT de actualización general no altera el valor preexistente de conciliación. El endpoint específico (`PATCH /api/estudiantes/{id}/conciliacion`) modifica el valor correctamente. | **PASS** |
| **Integración con selector de conciliación** | Invocación del endpoint paginado de conciliación (`/api/estudiantes/conciliacion/paginados`) devuelve exclusivamente los estudiantes con `conciliacion = true`. | **PASS** |

---

## 3. Pruebas ejecutadas y resultados

### A. Pruebas de integración HTTP + PostgreSQL real
Ejecutadas dentro de `Sprint7LocalQaPostgreSqlTest` sobre un cluster PostgreSQL temporal y sintético:
1. `studentFlagSurvivesHttpPersistenceDetailListAndSelector("true")`: **PASS**.
2. `studentFlagSurvivesHttpPersistenceDetailListAndSelector("false")`: **PASS**.
3. `studentFlagSurvivesHttpPersistenceDetailListAndSelector("null")`: **PASS**.
4. `studentFlagSurvivesHttpPersistenceDetailListAndSelector("omitted")`: **PASS**.
5. `unauthenticatedStudentCreationHasNoPersistentEffects()`: **PASS** (401 y 0 filas).
6. `advisorCannotCreateStudentsEvenWithLegacyCreatePermission()`: **PASS** (403 y 0 filas).

### B. Pruebas unitarias y de servicios
- `EstudianteCommandServiceConciliacionTest`: 5/5 **PASS**.
- `EstudianteExcelServiceConciliacionTest`: 1/1 **PASS**.
- `EstudianteSelectorPaginadoTest`: 4/4 **PASS**.

**Total suite consolidada:** 29 pruebas ejecutadas, **0 fallos, 0 errores, 0 omitidas**.

---

## 4. Ambiente y reproducibilidad

- **Entorno:** Java 21 (OpenJDK 64-Bit), Maven 3.9.14, PostgreSQL 18.6 local.
- **Ejecución automatizada:**
  ```sh
  bash docs/testing/ops/run-sprint7-local-qa.sh
  ```
- **Aislamiento:** La base de datos es desechable (`legal_cases_qa_sprint7`), ejecutada en loopback (puerto efímero) con limpieza automática al finalizar. No utiliza ni expone credenciales ni datos de producción.

---

## 5. Conclusión y recomendación para el tablero Jira

El criterio funcional y técnico de SCRUM-354 / SCRUM-368 ha sido completamente verificado.  
Se remite a los revisores técnicos (**Edwin Torres** y **Nelson Conde**) para su conformidad y paso de **SCRUM-369 a Done**.
