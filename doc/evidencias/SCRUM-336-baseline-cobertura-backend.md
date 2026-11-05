# SCRUM-336 — Línea base de cobertura backend y gate JaCoCo

**Estado del documento:** Línea base local y validación del gate en GitHub Actions registradas para la PR #89. Los umbrales configurados están sujetos a ratificación independiente por el equipo.

**Fecha de medición:** 2026-11-05 (America/Bogota).

**Jira:** SCRUM-336 — QA/CI: Medir cobertura y configurar umbrales aprobados de calidad.

**Repositorio:** `NelsonConde/Sistema-de-consultorio-juridico`.

**Rama de trabajo:** `chore/scrum-336-jacoco-baseline`.

**Base de la PR:** `fa3142e0d13673f93554850d2d80f202af83b20f` en `develop`. **Commit de implementación verificado:** [`a6be09b7f881a60fc4dc202efdc4f64e91c44b49`](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/commit/a6be09b7f881a60fc4dc202efdc4f64e91c44b49). La referencia `0 0` de `git rev-list --left-right --count HEAD...origin/develop` corresponde a la preparación local anterior a esta PR.

**Trazabilidad:** [PR #89](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/89) abierta hacia `develop`; [Build #371](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/actions/runs/42028759690) completó `Backend`, `Frontend` y `Playwright E2E` satisfactoriamente en el commit de implementación.

---

## 1. Objetivo y alcance

Medir la cobertura del backend por líneas y ramas utilizando JaCoCo, documentar una línea base reproducible y establecer un control automático de calidad mediante umbrales mínimos integrados en el workflow de GitHub Actions.

La implementación comprende:

- Instrumentación del backend mediante JaCoCo Maven Plugin 0.8.16.
- Generación de reportes HTML, XML y CSV.
- Medición global y por módulos.
- Configuración de umbrales mínimos de cobertura.
- Validación positiva y negativa del gate.
- Integración del gate en el ciclo Maven `verify`.
- Configuración de publicación de reportes en GitHub Actions.
- Validación estática del workflow mediante `actionlint`.

La cobertura corresponde exclusivamente al código Java instrumentado del backend.

No representa cobertura funcional integral de los requisitos, de la interfaz Next.js ni de las pruebas E2E de Playwright.

El gate se configura sobre el **bundle global del backend** (`BUNDLE`). Las métricas por módulo son informativas y permiten identificar áreas prioritarias de mejora.

No se añadieron exclusiones explícitas de paquetes o clases en la configuración JaCoCo.

## 2. Ambiente de ejecución

| Componente | Configuración |
|---|---|
| Sistema operativo local | Windows |
| Terminal | PowerShell desde IntelliJ IDEA |
| Directorio de ejecución | `backend/app` |
| Java | 21 |
| Spring Boot | 4.0.6 |
| Build | Maven Wrapper |
| JaCoCo | 0.8.16 |
| Base de datos de pruebas | PostgreSQL 16 |
| Contenedores | Docker Desktop y Testcontainers |
| CI verificado | GitHub Actions, Build #371 |
| Runner del backend | `ubuntu-24.04` |

### 2.1. Comando de validación integral

```powershell
.\mvnw.cmd clean verify
```

Este comando ejecuta las pruebas automatizadas, genera los artefactos del backend, produce los informes de cobertura y aplica los umbrales configurados.

### 2.2. Reportes generados

Los archivos principales son:

- `backend/app/target/jacoco.exec`
- `backend/app/target/site/jacoco/index.html`
- `backend/app/target/site/jacoco/jacoco.xml`
- `backend/app/target/site/jacoco/jacoco.csv`

El archivo XML permite extraer los contadores utilizados en este documento.

## 3. Línea base de cobertura

La ejecución local final de `clean verify`, realizada el 2026-11-05, terminó correctamente.

### 3.1. Resultados de pruebas

| Indicador | Resultado |
|---|---:|
| Pruebas registradas | 659 |
| Pruebas no omitidas | 649 |
| Fallos | 0 |
| Errores | 0 |
| Pruebas omitidas | 10 |
| Clases analizadas por JaCoCo | 451 |
| Resultado Maven | BUILD SUCCESS |

La salida final confirmó:

```text
jacoco:0.8.16:report (coverage-report)
jacoco:0.8.16:check (coverage-check)
All coverage checks have been met.
BUILD SUCCESS
```

**Estado:** PASS local.

La ejecución se realizó con un ajuste temporal de compatibilidad POSIX/Windows aplicado a una prueba de migración. Dicho ajuste fue posteriormente separado del alcance de SCRUM-336 y conservado en un stash independiente.

Por tanto, esta evidencia acredita la ejecución local observada, pero no debe presentarse como validación de la suite completa de Windows sobre el árbol exacto preparado para la PR.

### 3.2. Cobertura global

| Métrica | Cubierto | Total | Cobertura | Mínimo | Estado |
|---|---:|---:|---:|---:|---|
| Líneas (`LINE`) | 5.430 | 13.011 | 41,73 % | 40 % | PASS |
| Ramas (`BRANCH`) | 1.435 | 5.240 | 27,39 % | 25 % | PASS |

Los porcentajes provienen de los contadores globales del reporte XML generado por JaCoCo.

No corresponden al promedio aritmético de los porcentajes por módulo.

### 3.3. Cobertura por módulos

| Módulo | Líneas | Ramas |
|---|---:|---:|
| `audit` | 74,59 % | 54,55 % |
| `business` | 27,66 % | 17,21 % |
| `common` | 65,58 % | 35,87 % |
| `config` | 92,98 % | 85,71 % |
| `file_storage` | 57,71 % | 44,85 % |
| `migration` | 82,86 % | 63,08 % |
| `root` | 33,33 % | N/A |
| `security` | 73,46 % | 49,58 % |

### 3.4. Interpretación de resultados

El módulo `business` presenta la cobertura más baja de los módulos principales:

- Líneas: 27,66 %.
- Ramas: 17,21 %.

Se identifica como un área prioritaria para fortalecer las pruebas automatizadas en actividades posteriores.

Por otra parte, `migration`, `security` y `audit` presentan coberturas superiores al promedio global de líneas.

En `root`, el resultado de ramas es `N/A` debido a que JaCoCo no contabiliza ramas para ese paquete.

El gate global no exige que cada módulo supere individualmente el 40 % de líneas o el 25 % de ramas.

## 4. Configuración de umbrales y gate de calidad

Se establecieron los siguientes mínimos para la implementación de SCRUM-336:

```properties
coverage.line.minimum=0.40
coverage.branch.minimum=0.25
```

Los valores fueron definidos por el ejecutor tomando como referencia la cobertura medida y quedaron aplicados en la PR. La conformidad del equipo con estos umbrales concretos requiere una constancia trazable de revisión independiente; esta medición y la ejecución satisfactoria del gate no constituyen por sí solas dicha constancia.

### 4.1. Configuración del gate

La configuración JaCoCo utiliza:

- Elemento: `BUNDLE`.
- Contador de líneas: `LINE`.
- Contador de ramas: `BRANCH`.
- Tipo de comparación: `COVEREDRATIO`.
- Mínimo de líneas: `0.40`.
- Mínimo de ramas: `0.25`.

El plugin ejecuta:

- `prepare-agent`, para instrumentar las pruebas.
- `report`, durante `verify`, para generar los reportes.
- `check`, durante `verify`, para comprobar los umbrales.

Si cualquiera de los mínimos no se cumple, JaCoCo rechaza la ejecución Maven.

### 4.2. Prueba positiva del gate

**Comando ejecutado:**

```powershell
.\mvnw.cmd jacoco:check
```

**Resultado observado:**

```text
All coverage checks have been met.
BUILD SUCCESS
```

**Estado:** PASS.

El gate aceptó los resultados registrados porque la cobertura global superó ambos mínimos.

### 4.3. Prueba negativa de cobertura de líneas

**Comando ejecutado:**

```powershell
.\mvnw.cmd "-Dcoverage.line.minimum=0.99" jacoco:check
```

Se exigió temporalmente un mínimo de 99 % de líneas.

**Resultado observado:**

```text
Rule violated for bundle legal_cases:
lines covered ratio is 0.41, but expected minimum is 0.99

BUILD FAILURE
```

**Estado:** PASS — rechazo esperado.

La ejecución demuestra que el gate detecta el incumplimiento del mínimo de líneas.

El valor de 99 % solo se aplicó durante esta prueba y no modificó permanentemente la configuración del proyecto.

### 4.4. Prueba negativa de cobertura de ramas

**Comando ejecutado:**

```powershell
.\mvnw.cmd "-Dcoverage.branch.minimum=0.99" jacoco:check
```

Se exigió temporalmente un mínimo de 99 % de ramas.

**Resultado observado:**

```text
Rule violated for bundle legal_cases:
branches covered ratio is 0.27, but expected minimum is 0.99

BUILD FAILURE
```

**Estado:** PASS — rechazo esperado.

La ejecución demuestra que el gate también detecta incumplimientos del mínimo de ramas.

### 4.5. Validación integral con el gate activado

**Comando ejecutado:**

```powershell
.\mvnw.cmd clean verify
```

**Resultado observado:**

```text
jacoco:0.8.16:report (coverage-report)
jacoco:0.8.16:check (coverage-check)
All coverage checks have been met.
BUILD SUCCESS
```

**Estado:** PASS local.

Las pruebas negativas utilizaron el archivo de ejecución generado previamente.

Posteriormente, `clean verify` regeneró los datos de cobertura, ejecutó la suite completa y confirmó que ambos mínimos se cumplían dentro del ciclo Maven.

## 5. Integración con GitHub Actions

La PR #89 modifica el archivo:

`.github/workflows/build.yml`

En el job `backend`, los pasos independientes de pruebas y empaquetado se sustituyeron por:

```yaml
- name: Verify backend and generate coverage
  working-directory: backend/app
  run: ./mvnw clean verify
```

Este comando incorpora la ejecución de pruebas, el empaquetado y las verificaciones JaCoCo.

### 5.1. Validación de artefactos

Se agregó el siguiente paso:

```yaml
- name: Validate JaCoCo artifacts
  working-directory: backend/app
  run: |
    test -s target/jacoco.exec
    test -s target/site/jacoco/jacoco.xml
```

Su propósito es comprobar que los dos archivos requeridos existen y no están vacíos.

Si falta alguno, el comando finaliza con un código distinto de cero y el paso falla.

Este mecanismo complementa la ejecución del gate JaCoCo, evitando aceptar una ejecución exitosa sin evidencia básica de cobertura.

**Estado:** Ejecutado satisfactoriamente en el job `Backend` de [Build #371](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/actions/runs/42028759690), paso `Validate JaCoCo artifacts`.

### 5.2. Publicación del reporte

Se configuró la publicación mediante `actions/upload-artifact@v4`:

```yaml
- name: Upload JaCoCo coverage report
  if: always()
  uses: actions/upload-artifact@v4
  with:
    name: backend-jacoco-report
    path: |
      backend/app/target/site/jacoco/
      backend/app/target/jacoco.exec
    if-no-files-found: error
    retention-days: 7
```

La configuración permite adjuntar los reportes de cobertura como artefactos del workflow.

La propiedad `if-no-files-found: error` hace que la ausencia de archivos coincidentes se trate como un error y no como una advertencia.

La condición `if: always()` permite intentar publicar los reportes incluso cuando un paso anterior falla, facilitando el diagnóstico cuando existen artefactos disponibles. Si no existen, el paso de publicación también puede fallar.

La retención configurada es de siete días.

El workflow mantiene el paso independiente de verificación del ejecutable de migraciones.

### 5.3. Validación estática del workflow

Se ejecutó `actionlint` mediante Docker:

```powershell
$repoRoot = (Resolve-Path ../..).Path

docker run --rm `
    --mount "type=bind,source=$repoRoot,target=/repo,readonly" `
    --workdir /repo `
    rhysd/actionlint:latest `
    .github/workflows/build.yml
```

**Resultado observado:**

```text
Exit code: 0
```

**Estado:** PASS.

La validación estática no reportó errores en el workflow modificado.

Adicionalmente, se comprobó localmente la existencia de los archivos:

```powershell
Test-Path .\target\jacoco.exec
Test-Path .\target\site\jacoco\jacoco.xml
```

**Resultado observado:**

```text
True
True
```

Estas comprobaciones locales acreditan existencia, no ejecución del paso `test -s` dentro de GitHub Actions.

La ejecución remota y la publicación del artefacto `backend-jacoco-report` se verificaron en [Build #371](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/actions/runs/42028759690), commit `a6be09b7f881a60fc4dc202efdc4f64e91c44b49`. El artefacto [#13415461340](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/actions/runs/42028759690/artifacts/13415461340) contiene el reporte XML; sus contadores globales confirman 5.430/13.011 líneas (41,73 %) y 1.435/5.240 ramas (27,39 %), iguales a los de la medición local. El job ejecutó `clean verify`, superó el gate y validó la existencia de `jacoco.exec` y `jacoco.xml`.

## 6. Alcance de los cambios

Los archivos de la PR #89 correspondientes a SCRUM-336 son:

| Archivo | Cambio | Relación con SCRUM-336 |
|---|---|---|
| `.github/workflows/build.yml` | Ejecución de `verify`, validación y publicación de JaCoCo | Directa |
| `backend/app/pom.xml` | Instrumentación, reporte y gate de cobertura | Directa |
| `doc/evidencias/SCRUM-336-baseline-cobertura-backend.md` | Documentación de mediciones y verificaciones | Directa |

### 6.1. Corrección de portabilidad separada

Durante las pruebas locales se identificó una incompatibilidad de `DatabaseMigrationPostgreSqlTest.java` con Windows.

La prueba original utiliza `Files.setPosixFilePermissions`, que no está disponible en los sistemas de archivos Windows convencionales.

Se aplicó temporalmente un ajuste para permitir completar las pruebas locales.

Después, dicho ajuste fue retirado del directorio de trabajo y guardado por separado:

```text
stash@{0}: SCRUM-332 ajuste local de compatibilidad POSIX Windows
```

El cambio no forma parte de los archivos preparados para SCRUM-336.

Se mantiene un respaldo anterior:

```text
stash@{1}: SCRUM-336 respaldo antes de actualizar develop
```

Las referencias corresponden al estado observado durante la auditoría y pueden cambiar si se crean nuevos stashes.

### 6.2. Scripts SQL

Las tres migraciones SQL se verificaron con saltos de línea LF.

Después de sincronizar la rama, no quedaron cambios pendientes en los scripts de migración.

No se modificó el esquema de producción ni se ejecutaron migraciones contra bases remotas.

## 7. Observaciones y limitaciones

### 7.1. Advertencias de HikariCP y Surefire

Durante la finalización de la suite se observaron:

- Intentos fallidos de reconexión de HikariCP hacia un contenedor PostgreSQL que ya no respondía.
- El mensaje `Surefire is going to kill self fork JVM`.

Estas advertencias no impidieron que Maven terminara con `BUILD SUCCESS`.

No se ha demostrado su causa exacta ni se declara resuelta.

Se recomienda realizar seguimiento si se reproducen en CI o afectan otras ejecuciones.

### 7.2. Pruebas omitidas

La suite registró 10 pruebas omitidas.

No se ha revisado todavía la justificación individual de cada omisión.

Por tanto, el resultado no debe interpretarse como ejecución efectiva de todos los casos definidos.

### 7.3. Cobertura funcional y E2E

JaCoCo mide cobertura del código Java instrumentado durante las pruebas del backend.

No demuestra automáticamente:

- Cobertura de todos los requisitos funcionales y no funcionales.
- Validación completa de la interfaz Next.js.
- Ejecución de recorridos reales de usuarios.
- Cobertura de todos los flujos E2E.
- Ausencia de vulnerabilidades.
- Ausencia de defectos funcionales.

El repositorio dispone de un job Playwright independiente.

El job `Playwright E2E` de Build #371 finalizó satisfactoriamente en el commit de implementación de SCRUM-336. Su resultado no equivale a cobertura de interfaz medida por JaCoCo ni a cobertura integral de requisitos.

### 7.4. Umbrales globales

Los mínimos se aplican al backend completo.

Un módulo puede permanecer por debajo de los valores establecidos sin provocar un fallo del gate global.

El módulo `business` requiere especial atención en una estrategia futura de ampliación de pruebas.

No se introdujeron exclusiones para incrementar artificialmente los porcentajes.

### 7.5. Limitaciones de la validación local

La suite completa se ejecutó en Windows con una corrección temporal aplicada a una prueba de migración.

Esa corrección quedó excluida del cambio preparado para SCRUM-336.

El backend se ejecutó en Ubuntu en Build #371 y superó pruebas, empaquetado y el gate de cobertura en el commit de implementación. La observación sobre el ajuste temporal de Windows se conserva para interpretar correctamente la medición local.

### 7.6. Seguridad de artefactos

El reporte JaCoCo se genera a partir del código instrumentado.

La revisión de contenido antes de integrar debe descartar secretos, credenciales, archivos sensibles y datos personales innecesarios. El reporte de cobertura publicado corresponde al código instrumentado del backend.

La comprobación de nombres de archivos realizada en la auditoría no encontró coincidencias con patrones de secretos, pero no sustituye una revisión de contenido.

### 7.7. Revisión independiente

La conformidad independiente de los responsables designados, Nelson Enrique Conde Tarazona o José Manuel Pérez Rodríguez, con los umbrales de 40 % de líneas y 25 % de ramas no está documentada en esta evidencia. La configuración y el resultado satisfactorio del gate se presentan como hechos técnicos separados de esa decisión.

## 8. Matriz de aceptación de SCRUM-336

| Criterio Jira | Implementación y evidencia | Estado |
|---|---|---|
| 1. Reportar cobertura de líneas y ramas por módulo con exclusiones justificadas | JaCoCo XML/HTML y tabla por módulos; sin exclusiones explícitas adicionales; XML de CI contrastado con la medición local | Verificado |
| 2. Registrar línea base y umbrales acordados | Baseline local y CI 41,73 % / 27,39 %; mínimos 40 % / 25 % configurados; conformidad del equipo pendiente de constancia | Medición y configuración verificadas; acuerdo por acreditar |
| 3. Aplicar umbral y demostrar aceptación y rechazo | PASS local y CI Build #371; fallos intencionales de líneas y ramas documentados localmente | Verificado en el alcance indicado |
| 4. Documentar limitaciones E2E/UI | Limitaciones descritas en la sección 7; job Playwright E2E de Build #371 satisfactorio | Documentado |

## 9. Trazabilidad de verificaciones

### 9.1. Preparación y comprobaciones locales realizadas

- Consulta de los criterios de aceptación de SCRUM-336 en Jira.
- Identificación de la dependencia SCRUM-335, registrada como finalizada.
- Sincronización de la rama de trabajo con `origin/develop`.
- Verificación de ausencia de commits propios antes de preparar los cambios.
- Separación del ajuste de compatibilidad de SCRUM-332.
- Generación de reportes JaCoCo.
- Obtención de cobertura global y por módulos.
- Ejecución de pruebas positivas del gate.
- Ejecución de pruebas negativas de líneas y ramas.
- Ejecución integral de `clean verify`.
- Configuración de publicación del reporte en GitHub Actions.
- Validación estática de `.github/workflows/build.yml` mediante `actionlint`: PASS, código de salida `0`.
- Verificación local de existencia de `jacoco.exec` y `jacoco.xml`: ambos presentes.
- Ejecución de `git diff --check` y `git diff --cached --check`: sin observaciones.
- Verificación del staging limitado a los archivos de SCRUM-336.
- Comprobación de nombres de archivos preparados contra patrones básicos de secretos: sin coincidencias.

### 9.2. Comprobaciones posteriores a la publicación de la PR

1. **Workflow, gate y artefactos:** verificados en [Build #371](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/actions/runs/42028759690) del commit `a6be09b`; el reporte `backend-jacoco-report` fue publicado.
2. **Revisión de seguridad del contenido:** el control local de nombres de archivos se registró en la sección 9.1; la revisión independiente del contenido preparado forma parte de la revisión de la PR.
3. **Coherencia documental con el diff:** esta actualización distingue las pruebas locales, la configuración del gate y el resultado remoto. La versión incorporada a la PR queda sujeta a la revisión del commit documental correspondiente.
4. **Sincronización de `develop`:** la base de integración se identificó en `fa3142e0`; la comprobación de nuevos cambios corresponde al momento de integrar.
5. **Conformidad con los umbrales:** se requiere constancia trazable del acuerdo del equipo sobre 40 % de líneas y 25 % de ramas. No se atribuye una aprobación inexistente a los revisores.
6. **CI del commit publicado:** Build #371 verificó el commit de implementación `a6be09b`. Toda actualización documental posterior se verifica en el CI de su propio commit antes de integrar.
7. **Observaciones de revisión:** se incorporarán las correcciones técnicas que resulten de la revisión independiente; esta verificación no sustituye la decisión del revisor.

## 10. Resultado técnico y control de decisión

La instrumentación JaCoCo, la línea base global y por módulos, las pruebas positivas y negativas del gate y la validación estática del workflow cuentan con los resultados registrados. Build #371 confirma la ejecución remota de las pruebas del backend, el cumplimiento de los mínimos configurados y la publicación del reporte; el XML remoto coincide con la medición local de 41,73 % de líneas y 27,39 % de ramas.

La decisión de adoptar formalmente los umbrales de 40 % y 25 % corresponde al equipo y se registra por el canal de revisión autorizado. Esta constancia es independiente del resultado técnico satisfactorio del gate. La revisión de la PR y el CI de cualquier commit documental posterior completan la trazabilidad de integración, sin modificar las mediciones ni los hechos históricos conservados en este documento.