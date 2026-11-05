# SCRUM-336 — Línea base de cobertura backend y gate JaCoCo

**Estado del documento:** Evidencia local pre-PR; no acredita ejecución de CI remoto ni aprobación independiente.

**Fecha de medición:** 2026-11-05 (America/Bogota).

**Jira:** SCRUM-336 — QA/CI: Medir cobertura y configurar umbrales aprobados de calidad.

**Repositorio:** `NelsonConde/Sistema-de-consultorio-juridico`.

**Rama de trabajo:** `chore/scrum-336-jacoco-baseline`.

**HEAD / base `origin/develop` verificados:** `fa3142e0d13673f93554850d2d80f202af83b20f` (`0 0` en `git rev-list --left-right --count HEAD...origin/develop` al momento de la verificación).

**Estado de Git:** Cambios locales preparados para commit. No se ha creado ni publicado una PR.

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
| CI configurado | GitHub Actions |
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

Los valores fueron adoptados por el ejecutor durante la actividad, tomando como referencia la cobertura medida.

La revisión y aprobación independiente de los responsables designados debe acreditarse antes del cierre definitivo de Jira.

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

Se modificó localmente el archivo:

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

**Estado:** Configurado y validado estáticamente. Su ejecución real en GitHub Actions sigue pendiente.

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

La ejecución remota del workflow y la publicación real del artefacto todavía no se han verificado.

## 6. Alcance de los cambios

Los archivos preparados para SCRUM-336 son:

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

La presente evidencia no acredita una nueva ejecución de ese job ni su aprobación sobre el cambio de SCRUM-336.

### 7.4. Umbrales globales

Los mínimos se aplican al backend completo.

Un módulo puede permanecer por debajo de los valores establecidos sin provocar un fallo del gate global.

El módulo `business` requiere especial atención en una estrategia futura de ampliación de pruebas.

No se introdujeron exclusiones para incrementar artificialmente los porcentajes.

### 7.5. Limitaciones de la validación local

La suite completa se ejecutó en Windows con una corrección temporal aplicada a una prueba de migración.

Esa corrección quedó excluida del cambio preparado para SCRUM-336.

La ejecución del backend sobre Ubuntu en GitHub Actions deberá comprobarse cuando el cambio se publique.

### 7.6. Seguridad de artefactos

El reporte JaCoCo se genera a partir del código instrumentado.

Antes de publicar los cambios debe revisarse el contenido preparado para descartar secretos, credenciales, archivos sensibles y datos personales innecesarios.

La comprobación de nombres de archivos realizada en la auditoría no encontró coincidencias con patrones de secretos, pero no sustituye una revisión de contenido.

### 7.7. Revisión independiente

La revisión técnica y la aprobación independiente por parte de los responsables designados, Nelson Enrique Conde Tarazona y José Manuel Pérez Rodríguez, permanecen pendientes de evidencia.

La implementación de los umbrales no equivale por sí sola a esa aprobación.

## 8. Matriz de aceptación de SCRUM-336

| Criterio Jira | Implementación y evidencia | Estado |
|---|---|---|
| 1. Reportar cobertura de líneas y ramas por módulo con exclusiones justificadas | JaCoCo XML/HTML y tabla por módulos; sin exclusiones explícitas adicionales | PASS local |
| 2. Registrar línea base y umbrales acordados | Baseline 41,73 % / 27,39 %; mínimos 40 % / 25 % configurados; revisión independiente pendiente | PARCIAL |
| 3. Aplicar umbral y demostrar aceptación y rechazo | PASS local, FAIL intencional de líneas y ramas, `clean verify` exitoso; CI remoto pendiente | PARCIAL |
| 4. Documentar limitaciones E2E/UI | Limitaciones descritas en la sección 7 | DOCUMENTADO, pendiente de revisión |

## 9. Auditoría pre-PR

### 9.1. Comprobaciones realizadas

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

### 9.2. Comprobaciones pendientes

Antes de dar por finalizada la actividad:

1. Verificar en GitHub Actions la ejecución real del workflow, el control de cobertura y la publicación de los artefactos.
2. Completar la revisión final de seguridad del contenido preparado.
3. Comprobar la coherencia definitiva del documento con el diff.
4. Verificar si `origin/develop` incorpora nuevos cambios antes de la publicación.
5. Registrar la revisión y aprobación independiente de los umbrales.
6. Comprobar los resultados de CI correspondientes al commit publicado.
7. Revisar las observaciones de los revisores y corregir cualquier hallazgo bloqueante.

Los resultados remotos de commits anteriores no sustituyen la validación del cambio actual.

## 10. Dictamen preliminar de auditoría

****Estado: APTO CON OBSERVACIONES PARA CREAR PR; NO APTO TODAVÍA PARA CIERRE DE SCRUM-336.**

La instrumentación JaCoCo, la medición de cobertura, los umbrales configurados y las pruebas locales del gate cuentan con evidencia satisfactoria.

Además, la configuración actual del workflow superó la validación estática de `actionlint`.

La revisión técnica local permite presentar los cambios mediante una PR para revisión independiente. La aprobación formal de los umbrales por los revisores designados y la ejecución satisfactoria del CI remoto siguen pendientes. No debe cerrarse SCRUM-336 ni declararse aprobado el gate  por el equipo hasta obtener dichas evidencias.

La ejecución remota de GitHub Actions se verificará después de publicar el cambio, sin presentar resultados anteriores como evidencia del nuevo commit.

Este dictamen no representa un fallo de la implementación, sino la identificación de verificaciones todavía pendientes dentro del procedimiento de auditoría pre-PR.