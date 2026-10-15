# Guía de contribución — Sistema de Gestión de Casos Jurídicos

> **Base de comparación:** `main` (referencia de despliegue) y `develop` (integración en curso), revisadas el 14/10/2026. Las diferencias observadas en `develop` no se declaran liberadas en producción.

> **Borrador documental — SCRUM-360.** Preparado para revisión de la persona responsable de configuración (SCRUM-338). Este archivo resume los procedimientos vigentes; no sustituye el *Manual de Gestión de Ramas Git y GitHub*, versión 1.0, ni el *Plan de Gestión de la Configuración*, versión 1.0.

## Antes de contribuir

1. Identificar una historia, tarea, defecto, hallazgo o solicitud de cambio registrada en Jira. Confirmar alcance y responsable.
2. Consultar la documentación técnica en [`doc/`](doc/README.md), especialmente API, backend, frontend, reglas de negocio, decisiones y mantenimiento según los componentes afectados.
3. No incluir secretos, datos personales reales, contraseñas, tokens ni archivos `.env` en Git, capturas, PR o documentación.
4. Respetar las decisiones y aprobaciones previstas en los documentos de gestión del proyecto. Las configuraciones de seguridad y las liberaciones requieren la revisión correspondiente.

## Preparación del entorno

Requisitos identificados en el repositorio:

- Backend: Java 21 y Maven Wrapper incluido en `backend/app/`.
- Frontend: Node.js 22.x y npm **desde 10.9.4 (inclusive) hasta antes de 11**, según `frontend/package.json` (`>=10.9.4 <11`). La versión 10.9.4 es la mínima permitida dentro de npm 10, no la única admitida.
- Configuración funcional de base de datos, correo, JWT y servicios de archivos por variables de entorno suministradas a través de canales autorizados.

### Backend

Desde la raíz del repositorio:

```bash
cd backend/app
./mvnw spring-boot:run
```

En Windows PowerShell:

```powershell
cd backend/app
.\mvnw.cmd spring-boot:run
```

Validaciones usadas en CI:

```bash
./mvnw test
./mvnw -DskipTests package
```

La segunda instrucción empaqueta el backend sin volver a ejecutar las pruebas; no sustituye `./mvnw test`.

### Frontend

```bash
cd frontend
npm ci
npm run dev
```

Para desarrollo local, el README también documenta `npm install`. Se recomienda `npm ci` al verificar una integración con el lockfile vigente.

Comprobaciones disponibles:

```bash
npm run build
npm run lint
npm run test
```

`npm run test` ejecuta Playwright. Según la necesidad del caso también existen `npm run test:ui`, `npm run test:headed` y `npm run test:debug`. No se debe afirmar que las pruebas E2E forman parte del workflow de CI: el archivo `.github/workflows/build.yml` consultado ejecuta build y lint frontend, pero no Playwright.

## Estrategia de ramas

Las ramas permanentes cumplen estas finalidades:

| Rama | Finalidad |
| --- | --- |
| `main` | Versión estable y aprobada. |
| `develop` | Integración de cambios del Sprint. |
| `staging` | Validación integrada previa a la liberación. |

Las ramas temporales parten normalmente de `develop`, salvo `hotfix/*`, que parte de `main`:

| Patrón | Uso |
| --- | --- |
| `feature/*` | Funcionalidades. |
| `bugfix/*` | Correcciones no urgentes. |
| `hotfix/*` | Incidentes urgentes en producción. |
| `docs/*` | Documentación versionada. |
| `test/*` | Pruebas. |

Utilizar nombres técnicos en inglés, minúsculas y guiones, sin espacios ni caracteres acentuados. Incluir el identificador trazable cuando aplique. Ejemplo apropiado para esta tarea: `docs/scrum-360-contribution-changelog` (sujeto a validación por el gestor de configuración).

No desarrollar ni hacer push directo a las ramas permanentes. La ruta ordinaria de integración es **rama temporal → `develop` → `staging` → `main`**, mediante Pull Requests y revisiones. Los `hotfix/*` siguen el procedimiento especial del manual y deben sincronizarse después con las demás ramas.

## Commits

Usar `type(scope): description`, con descripción en inglés, en presente y específica. Los tipos reconocidos en el manual son `feat`, `fix`, `docs`, `test`, `refactor`, `chore` y `style`.

```text
docs(governance): add contribution and changelog drafts
fix(pagination): keep stable ordering
test(documents): cover upload flow
```

No emplear mensajes genéricos como `update`, `final` o `changes`. Mantener cada commit relacionado con una modificación coherente.

## Pull Requests y revisión

Todo PR debe documentar:

- Identificador Jira, solicitud de cambio o hallazgo de origen y propósito del cambio.
- Resumen, componentes afectados y alcance explícito.
- Pruebas o comprobaciones **realmente ejecutadas**, comandos, resultado y evidencias; indicar claramente las que no se ejecutaron.
- Impacto en configuración, migraciones, documentación y despliegue, sin publicar secretos.
- Capturas u otras evidencias visuales cuando cambie la interfaz.

Abrir normalmente el PR hacia `develop`. El autor no puede ser su único revisor ni aprobarse a sí mismo. Antes de fusionar se deben resolver conflictos, revisar los archivos incluidos y obtener al menos una revisión técnica independiente junto con las pruebas aplicables. No ocultar validaciones fallidas.

La promoción a `staging` exige integración y validaciones satisfactorias; la promoción a `main` requiere validación en `staging`, evidencias, documentación y la autorización estipulada en el manual vigente. La aprobación de liberación no se presupone por la mera existencia de un PR.

## Particularidades de la rama develop (no liberadas en main)

La comparación de ramas realizada para este borrador identificó diferencias que deben tenerse en cuenta al contribuir sobre `develop`:

- **Sesión y solicitudes mutables:** el `README.md` de `develop` documenta JWT en cookie `access_token` y obtención de token CSRF mediante `GET /api/auth/csrf`, usando el encabezado señalado por la respuesta. Los cambios de autenticación deben respetar ese contrato y las verificaciones de backend. No extrapolar automáticamente estas indicaciones al despliegue de `main`.
- **Pipeline:** `.github/workflows/build.yml` en `develop` declara `permissions: contents: read` para limitar el token de GitHub Actions. Conserva pruebas y empaquetado backend, y compilación y lint frontend; **no ejecuta Playwright**.
- **Dependencias frontend:** `frontend/package.json` de `develop` declara `next: ^16.4.1`, mientras `main` declara `^16.2.6`. Ambas ramas exigen Node.js 22 y admiten npm `>=10.9.4 <11` (10.9.4 es el mínimo, no una versión exclusiva). Instalar las dependencias usando el lockfile correspondiente a la rama activa (`npm ci` cuando proceda); no trasladar versiones manualmente entre ramas.
- **Migraciones:** `backend/app/db/migration/README.md` de `develop` añade `V20261011_01__create_consulta_reasignacion_historial.sql` y scripts de verificación y recuperación controlada. Exige respaldo previo, verificación antes de desplegar y no eliminar historial ya escrito. El procedimiento debe validarse con el responsable de base de datos antes de ejecutarlo.
- **Perfiles y reasignaciones:** `develop` contiene cambios en servicios, validadores, documentación de perfiles y pruebas relacionadas con operaciones de estado de perfil y reasignación de responsables. No se presume que esos cambios estén publicados en `main` ni que cumplan todos los criterios de aceptación hasta que consten pruebas y aprobación.

## Cambios de base de datos, API y documentación

- Conservar scripts de migración versionados y sus instrucciones de ejecución/verificación; comprobar respaldos y procedimientos de recuperación antes de aplicar cambios sobre datos existentes. Véase [`backend/app/db/migration/README.md`](backend/app/db/migration/README.md).
- Cuando se modifiquen controllers, endpoints o DTO, revisar `doc/api/`, `doc/backend/` y consumidores en `doc/frontend/`.
- Cuando cambien reglas, permisos, entidades o decisiones, revisar respectivamente `doc/reglas/`, `doc/base-datos/` y `doc/decisiones/`, según la [guía de mantenimiento](doc/mantenimiento/README.md).
- Los cambios integrados deben mantener trazabilidad entre Jira, rama/commit, PR, pruebas, documentación y versión o línea base que corresponda.

## Incidencias y seguridad

Registrar defectos, incidencias o riesgos detectados mediante los canales de trabajo autorizados en Jira. Evitar datos sensibles en tickets o evidencias. El procedimiento específico de divulgación de vulnerabilidades y el contenido de `SECURITY.md` están **reservados al responsable técnico en SCRUM-338**: este documento no establece un canal público nuevo ni autoriza divulgar detalles explotables.

## Fuentes normativas y de implementación

- *Manual de Gestión de Ramas Git y GitHub* (v1.0, vigente; documentación oficial del proyecto).
- *Plan de Gestión de la Configuración* (v1.0, vigente; documentación oficial del proyecto).
- [`README.md`](README.md), [`frontend/README.md`](frontend/README.md) y [`doc/README.md`](doc/README.md).
- [`frontend/package.json`](frontend/package.json), [`backend/app/pom.xml`](backend/app/pom.xml) y [`.github/workflows/build.yml`](.github/workflows/build.yml).

## Pendiente de revisión para adopción

- Confirmar con el gestor de configuración el texto final, la ruta de publicación y las referencias a documentos oficiales accesibles al equipo.
- Verificar el estado actual de las protecciones de ramas y requisitos de CI en GitHub; la política documentada no prueba por sí sola la activación técnica de esas protecciones.
- Las decisiones sobre `LICENSE`, `SECURITY.md` y `.env.example` corresponden a SCRUM-338 y no forman parte de este borrador.
