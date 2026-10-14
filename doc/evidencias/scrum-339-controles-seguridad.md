# SCRUM-339 — Evidencia de controles de seguridad

## Alcance del cambio

Rama de trabajo: `chore/scrum-339-security-controls`, basada en `develop`.

| Control | Archivo | Resultado documental |
| --- | --- | --- |
| Dependencias Maven, npm y GitHub Actions | `.github/dependabot.yml` | Configuración propuesta; GitHub la consume desde la rama predeterminada `main`, por lo que su activación no queda acreditada con un PR contra `develop`. |
| Análisis estático Java y JavaScript/TypeScript | `.github/workflows/codeql.yml` | Workflow agregado; falta verificar la ejecución y las alertas en GitHub Actions/Code Scanning. El modo sin build tiene limitaciones de cobertura. |
| Revisión de cambios de dependencias | `.github/workflows/dependency-review.yml` | Workflow agregado para PR; falta confirmar ejecución satisfactoria y disponibilidad de dependency graph. |
| Detección preventiva de secretos en cambios de PR | `.github/workflows/secret-scan.yml`, `.github/scripts/scan_changed_secrets.py` | Workflow y script propuestos; falta confirmar ejecución real. El script no publica detalles ni valores encontrados. |

## Límites y condiciones de seguridad

- El control de secretos escanea solo archivos elegibles modificados por el PR: **no es auditoría histórica ni revisión completa del repositorio**.
- La configuración protegida del backend `application.properties` queda expresamente excluida. No se consulta ni reproduce su contenido.
- El escáner devuelve únicamente número de archivos y cantidad de hallazgos potenciales. No publica una línea, valor, ruta de secreto ni artefactos con resultados.
- Ningún resultado sustituye la revisión por el responsable de seguridad, la rotación por operador autorizado ni la comprobación de las copias históricas.
- La configuración de Dependabot debe llegar a la rama predeterminada `main` para ser interpretada por GitHub. Las actualizaciones de versión pueden dirigirse a `develop`; los parches de seguridad de Dependabot se gestionan según las reglas de la rama predeterminada.
- El estado efectivo de Dependabot Alerts, secret scanning/push protection y Code Scanning no pudo consultarse con la conexión actual de GitHub; debe contrastarse en **Settings → Security / Advanced Security** por alguien con acceso administrativo.
- La evidencia pasada de auditoría de npm, registrada en `doc/02-configuracion-seguridad.md`, no equivale a una auditoría recién ejecutada.
- Los workflows nuevos no acreditan por sí solos su funcionamiento: adjuntar ejecuciones reales, registrar PASS/FAIL/inconcluso y analizar hallazgos antes de cerrar SCRUM-339.

## Validación y cierre pendientes

1. Abrir el PR hacia `develop` y verificar el resultado individual de CodeQL (Java y JS/TS), Dependency Review y Secret Scan, además de la CI Build.
2. Si un workflow no se ejecuta por permisos o por capacidades del plan, registrar la limitación técnica y una alternativa aprobada, sin silenciar fallos.
3. Comprobar en GitHub Security el estado efectivo de alertas, escaneo y protección de secretos. Registrar únicamente cantidades/severidades y enlaces a evidencias de acceso restringido, sin valores sensibles.
4. Asignar los hallazgos confirmados con responsable, estado y acción; registrar fecha, ambiente y SHA del PR analizado.
5. Obtener revisión independiente de Edwin o José Manuel y registrar evidencia verificable en SCRUM-339.

**Estado de esta acta:** propuesta técnica. No declara aprobaciones, escaneos ejecutados ni cierre Jira.
