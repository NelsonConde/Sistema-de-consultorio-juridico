# Guía de contribución — Sistema de Gestión de Casos Jurídicos

Esta guía describe los requisitos técnicos y el procedimiento para contribuir al Sistema de Gestión de Casos Jurídicos, incluyendo preparación del entorno, estrategia de ramas, commits, Pull Requests, pruebas y mantenimiento.

## Antes de contribuir

1. Confirmar el alcance del cambio y su registro en el mecanismo de trazabilidad autorizado.
2. Consultar la documentación técnica en [`doc/`](doc/README.md), especialmente los contratos, reglas de negocio, decisiones y procedimientos de mantenimiento del componente afectado.
3. No incluir secretos, datos personales reales, credenciales, tokens, archivos `.env`, respaldos ni documentos de usuarios en Git, incidencias, Pull Requests o evidencias.
4. Separar los cambios sin relación funcional o técnica.
5. Informar únicamente pruebas y verificaciones realmente ejecutadas.

## Preparación del entorno

Requisitos comprobados en el repositorio:

- Backend: Java 21 y Maven Wrapper incluido en `backend/app/`.
- Frontend: Node.js `>=22 <23` y npm `>=10.9.4 <11`, según `frontend/package.json`.
- Configuración local: usar [`backend/app/.env.example`](backend/app/.env.example) y [`frontend/.env.example`](frontend/.env.example) como plantillas de referencia. Estos archivos no configuran automáticamente el entorno; las variables deben definirse en el entorno de ejecución correspondiente, sin versionar archivos `.env` con valores reales.

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

Validaciones ejecutadas por el workflow de integración continua:

```bash
./mvnw test
./mvnw -DskipTests package
```

El segundo comando empaqueta el backend sin volver a ejecutar las pruebas y no sustituye el primero.

### Frontend

```bash
cd frontend
npm ci
npm run dev
```

Comprobaciones disponibles:

```bash
npm run build
npm run lint
npm run test
```

`npm run test` ejecuta Playwright. También existen `npm run test:ui`, `npm run test:headed` y `npm run test:debug`. El workflow `.github/workflows/build.yml` ejecuta build y lint del frontend, pero no ejecuta Playwright.

## Estrategia de ramas

Las ramas permanentes tienen estas finalidades:

| Rama | Finalidad |
| --- | --- |
| `main` | Conservar la versión estable aprobada. |
| `develop` | Integrar cambios antes de la validación de liberación. |
| `staging` | Validar de forma integrada una versión candidata. |

Las ramas temporales parten normalmente de `develop`; las ramas `hotfix/*` parten de `main`:

| Patrón | Uso |
| --- | --- |
| `feature/*` | Funcionalidades o cambios funcionales. |
| `bugfix/*` | Correcciones no urgentes. |
| `hotfix/*` | Correcciones urgentes de producción. |
| `docs/*` | Documentación versionada. |
| `test/*` | Pruebas sin una funcionalidad asociada. |

Los nombres técnicos se escriben en inglés, en minúsculas y con guiones, sin espacios, tildes, `ñ` ni nombres personales. Ejemplo: `docs/update-governance-files`.

No se desarrolla ni se hace push directo a ramas permanentes. La ruta ordinaria es **rama temporal → `develop` → `staging` → `main`**, mediante Pull Requests y revisión. Las correcciones de `hotfix/*` deben sincronizarse posteriormente con las ramas de integración correspondientes.

## Commits

Usar `type(scope): description`, con una descripción específica en inglés y en presente. Los tipos definidos son `feat`, `fix`, `docs`, `test`, `refactor`, `chore` y `style`.

```text
docs(governance): document contribution workflow
fix(pagination): keep stable ordering
test(documents): cover upload flow
```

No utilizar mensajes genéricos como `update`, `final` o `changes`. Cada commit debe representar una modificación coherente y no debe contener secretos ni datos sensibles.

## Pull Requests y revisión

Todo Pull Request debe indicar:

- origen y propósito del cambio;
- resumen, componentes afectados y límites del alcance;
- pruebas o comprobaciones realmente ejecutadas, con comandos y resultados;
- impacto sobre configuración, migraciones, documentación y despliegue;
- limitaciones, riesgos conocidos y validaciones pendientes;
- evidencias visuales cuando cambie la interfaz.

El destino ordinario es `develop`. El autor no puede ser su único revisor ni aprobar su propia integración. Antes de fusionar deben resolverse los conflictos, revisarse los archivos incluidos y obtenerse al menos una revisión técnica independiente junto con las pruebas aplicables. Las validaciones fallidas o no ejecutadas deben declararse.

La integración hacia `staging` y `main` requiere revisión, comprobaciones satisfactorias y autorización de los responsables del repositorio. La existencia de una rama, un Pull Request o una etiqueta no demuestra por sí sola que un cambio esté desplegado en producción.

### Control de título de Pull Request (SCRUM-346)

Para **nuevos** PR de ramas temporales se comprobará la forma `[SCRUM-123] type(scope): english description` o `[CC-OPT-004] type: english description`. Los tipos admitidos son `feat`, `fix`, `test`, `docs`, `refactor`, `chore` y `style`; el scope es opcional en el título del PR (pero sigue siendo obligatorio en el mensaje de commit conforme al manual). La descripción debe ser específica, en inglés, con al menos dos palabras y doce caracteres.

Ejemplo de trabajo: `[SCRUM-346] chore(ci): validate future PR titles`.

Las promociones entre ramas permanentes `develop` → `staging` y `staging` → `main` pueden usar `chore(release): promote validated build to main` sin un identificador de trabajo individual, ya que consolidan varios PR. Los demás cambios, incluidos los hotfix, mantienen el identificador en el título.

El workflow [PR Title Convention](.github/workflows/pr-title-convention.yml) valida títulos de PR al abrir, editar, reabrir, actualizar o marcar como listo para revisión, con permisos de lectura. No revisa ni reescribe commits históricos y no valida mensajes de commit de manera automática: se seleccionó el título de PR como punto de control estable porque GitHub puede crear mensajes de integración diferentes según la estrategia de merge. La revisión de commits sigue siendo responsabilidad del autor y revisor.

Verificación local: `node --test .github/scripts/validate-pr-title.test.mjs`. Un título que no cumple hace fallar el job. La configuración de **check requerido** en reglas de protección debe acreditarse por separado por un mantenedor autorizado; crear este workflow por sí solo no demuestra que GitHub impida un merge con la comprobación fallida.

## Base de datos, API y documentación

- Conservar las migraciones versionadas y sus instrucciones de ejecución, verificación y recuperación. Consultar [`backend/app/db/migration/README.md`](backend/app/db/migration/README.md).
- Confirmar respaldos y procedimientos de recuperación antes de aplicar cambios sobre datos existentes.
- Si cambian controllers, endpoints o DTO, revisar `doc/api/`, `doc/backend/` y los consumidores documentados en `doc/frontend/`.
- Si cambian reglas, permisos, entidades o decisiones, revisar `doc/reglas/`, `doc/base-datos/` y `doc/decisiones/` según la [guía de mantenimiento](doc/mantenimiento/README.md).
- Mantener la trazabilidad entre la solicitud de cambio, rama, commits, Pull Request, pruebas, documentación y versión o línea base correspondiente.

## Seguridad

Seguir [`SECURITY.md`](SECURITY.md) para el tratamiento de vulnerabilidades. No publicar detalles explotables ni información sensible en incidencias públicas. Los cambios de seguridad siguen el mismo control de alcance, revisión independiente, pruebas y promoción que los demás cambios, con divulgación restringida mientras exista riesgo de explotación.

Las reglas documentadas no demuestran por sí solas que las protecciones técnicas de ramas estén habilitadas en GitHub; ese estado debe verificarse antes de cada integración o liberación.

## Referencias técnicas

- [`README.md`](README.md), [`frontend/README.md`](frontend/README.md) y [`doc/README.md`](doc/README.md).
- [`frontend/package.json`](frontend/package.json), [`backend/app/pom.xml`](backend/app/pom.xml) y [`.github/workflows/build.yml`](.github/workflows/build.yml).
