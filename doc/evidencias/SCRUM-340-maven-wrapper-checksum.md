# SCRUM-340 — Verificación del checksum de Maven Wrapper

Fecha de ejecución: 2026-11-02 (America/Bogota).

## Alcance

Se configuró `distributionSha256Sum` para la distribución ya fijada por el
repositorio:

```text
https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.14/apache-maven-3.9.14-bin.zip
```

No se cambió la versión de Maven, el tipo de wrapper, Java, el `pom.xml`, las
dependencias ni los scripts `mvnw` y `mvnw.cmd`.

## Fuentes oficiales

- Distribución exacta en Maven Central:
  <https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.14/apache-maven-3.9.14-bin.zip>
- Cambio del proyecto Apache HttpComponents que fija la misma URL y el SHA-256:
  <https://apache.googlesource.com/httpcomponents-core/+/80a3ad15f6cccf665c4fae34ee17238a1a7b9a0e%5E%21/>
- SHA-512 publicado por Apache para la distribución 3.9.14:
  <https://archive.apache.org/dist/maven/maven-3/3.9.14/binaries/apache-maven-3.9.14-bin.zip.sha512>
- Notas oficiales de Maven 3.9.14:
  <https://maven.apache.org/docs/3.9.14/release-notes.html>

## Valores comprobados

```text
SHA-256: 55fadd669532a3205d5db95f490bf13971d8b0843526f407f29db0e61f074ab3
SHA-512: 4122c5e7a8794260539dd8fcd78480549511babff2f85e2b1258c8d4cf33c50af90f65d323f43c88d4959f35a8f37ced3eca802983caa6eb7cc81b16af936ab0
```

El SHA-512 calculado sobre el ZIP descargado desde la `distributionUrl`
coincidió exactamente con el valor publicado por Apache.

## Validación positiva

Se utilizó un directorio temporal vacío mediante `MAVEN_USER_HOME` para evitar
reutilizar una distribución de Maven previamente almacenada:

```bash
scrum340_maven_home="$(mktemp -d)"
MAVEN_USER_HOME="$scrum340_maven_home" ./mvnw -v
```

Resultado: **PASS**, código de salida `0`.

```text
Apache Maven 3.9.14 (996c630dbc656c76214ce58821dcc58be960875b)
Maven home: <directorio-temporal>/wrapper/dists/apache-maven-3.9.14/db91789b
```

También se descargó el artefacto exacto y se calcularon ambos resúmenes:

```bash
sha256sum apache-maven-3.9.14-bin.zip
sha512sum apache-maven-3.9.14-bin.zip
```

Resultado: **PASS**; ambos valores fueron los indicados en la sección anterior.

## Prueba negativa controlada

Se sustituyó temporalmente `distributionSha256Sum` por 64 ceros y se ejecutó el
wrapper con otro `MAVEN_USER_HOME` vacío.

Resultado esperado y observado: **PASS**, el wrapper rechazó la distribución y
terminó con código `1`:

```text
Error: Failed to validate Maven distribution SHA-256, your Maven distribution might be compromised.
If you updated your Maven version, you need to update the specified distributionSha256Sum property.
```

El valor de prueba se retiró inmediatamente. El archivo candidato conserva
únicamente el SHA-256 oficial correcto.

## Java 21 y límites

El entorno local disponible expuso OpenJDK 17, por lo que `./mvnw -v` acredita
la descarga y validación criptográfica del wrapper, pero no se presenta como
una compilación con Java 21. El proyecto conserva `<java.version>21</java.version>`
en `backend/app/pom.xml` y el workflow `Build` configura Temurin 21 antes de
ejecutar `./mvnw test` y `./mvnw -DskipTests package`.

La aceptación definitiva con Java 21 queda pendiente del resultado real del
workflow de la Pull Request. No se ejecutaron bases operativas ni se utilizaron
credenciales o datos reales.
