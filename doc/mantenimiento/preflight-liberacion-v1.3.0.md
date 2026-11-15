# Preparación de la siguiente liberación — candidata v1.3.0

> Estado: **candidata, no liberada**. Fuente: `develop` en `79f9658c6a01988ceca96852946ea5729b2c02fc` (15/11/2026). La versión `v1.3.0` es una propuesta MINOR por funcionalidades compatibles; no crear la etiqueta ni afirmar el commit de `main` hasta finalizar el procedimiento.

## Alcance verificado respecto de v1.2.1

- PR #105: invitación inicial de activación y administración de cuentas pendientes.
- PR #106: primera reunión de conciliación.
- PR #107: plantilla XLSX real de importación de estudiantes.
- PR #108: errores de encabezados XLSX tratados como solicitud inválida.
- PR #109: tareas pendientes de Inicio obtenidas directamente de seguimientos.
- PR #110: fechas predeterminadas de estadísticas según calendario local.
- PR #111: documentos de procesos (carga, versiones, eliminación, permisos) y etiquetas de estadísticas/PDF.

La identificación de estos PR no certifica QA independiente ni despliegue. Conservar los enlaces, commits, pruebas y aprobaciones de cada PR como evidencia.

## Verificaciones antes del PR develop → staging

- [ ] Asegurar que las revisiones y gates CI de todos los cambios destinados a la liberación estén aprobados.
- [ ] Verificar que `develop` incluya el Dockerfile corregido de backend, con `unzip` en la etapa builder; no suprimir verificación de checksum del Maven Wrapper.
- [ ] Pasar el nuevo gate CI **Backend Docker image**: build real desde `backend/app`, comprobar ambos JAR en la imagen y Java 21. El gate NO equivale a arranque integrado ni a deploy de Railway.
- [ ] Validar `./mvnw clean verify`, empaquetado del migrador, prueba aislada de migraciones, `npm ci`, build y lint frontend, E2E y revisión de secretos.
- [ ] Verificar que Vercel tenga como Root Directory `frontend`; Railway debe construir `backend/app/Dockerfile` con contexto `backend/app`. Corroborar configuración efectiva en los proveedores: no se infiere de Git.
- [ ] Verificar que `NEXT_PUBLIC_API_URL_BASE` o `NEXT_PUBLIC_API_URL` estén definidos **durante la construcción** del frontend. El cliente agrega `/api`; no duplicar el sufijo. Los valores `NEXT_PUBLIC_*` son públicos, no secretos.
- [ ] Comprobar variables de Railway sin revelar valores: `PORT`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS`, `FRONTEND_URL`, Brevo y Supabase Storage. Verificar atributos de cookies `Secure` y `SameSite` apropiados al ambiente.
- [ ] Revisar `flyway_schema_history` y las migraciones existentes, incluidas `V20261015_01` y `V20261102_01`. El JAR del backend únicamente valida; el JAR de migraciones es separado. No aplicar `baseline` ni DDL sin inspección, respaldo y autorización.
- [ ] Comprobar coherencia de la documentación oficial de Google Drive (manual de despliegue, Product Backlog, incrementos funcionales, casos de uso y diccionario si corresponden), con revisión documental y evidencias. No afirmar actualizaciones no efectuadas.
- [ ] Registrar el commit candidato exacto de 40 caracteres y comparar con `origin/develop`. No copiar el SHA de un merge futuro en documentos antes de que exista.

## Promoción y validación obligatorias

1. **PR develop → staging**: no hacer push directo. Revisar diferencias y conflictos; conservar commits de merge.
2. **Staging**: verificar despliegue real de Railway y Vercel, hash/commit efectivo, inicio de Spring Boot, estado de salud, validación Flyway, conexión PostgreSQL y almacenamiento privado. Comprobar también autenticación, CSRF/CORS, correos e invitaciones.
3. **Pruebas funcionales integradas**: ejercitar los siete hallazgos originales y regresiones relevantes con personas/perfiles y datos de prueba autorizados; incluir primer encuentro, importación XLSX y documentos de proceso con URLs firmadas reales. Registrar PASS/FAIL, ambiente, fecha y versión. La aprobación de CI no sustituye estas pruebas.
4. **Defectos de staging**: corregir mediante una rama temporal desde `develop`, PR a `develop` y nueva promoción a `staging`; nunca modificar directamente `staging` ni `main`.
5. **Autorizaciones**: revisión técnica independiente, consolidación de evidencia y aprobación expresa del Product Owner; aceptación del representante del consultorio cuando corresponda.
6. **PR staging → main**: promover solo el candidato validado. Antes de fusionar, comparar archivos de `staging` y `main` y verificar configuración y documentación; ejecutar los controles requeridos.
7. **Después de la fusión**: registrar commit de `main` exacto y comprobar el desplegado. Crear `v1.3.0` solo con aprobación y si sigue siendo la versión correcta, apuntando al commit aprobado en `main`. Registrar línea base `LB-xx` (identificador siguiente **por verificar**), fecha, responsables, PR, pruebas y despliegue.
8. **Después de la liberación**: publicar notas definitivas y actualizar las fuentes oficiales de estado/configuración. No dar por realizados pasos sin evidencia.

## Precauciones específicas

- La diferencia de historia entre `develop`, `staging` y `main` contiene commits de promoción independientes; no usar force-push, reset o rebase destructivo. Revisar tanto las diferencias de archivos como las de commits.
- `staging` y `main` presentan contenido de árbol equivalente al momento de este preflight, pero no idéntico SHA. Revisar nuevamente antes de cada PR.
- El `backend/app/Dockerfile` actual instala `unzip` antes de invocar Maven Wrapper y copia `legal-cases-backend.jar` y `legal-cases-backend-migrations.jar`. El gate Docker detecta regresiones de construcción; el arranque y migraciones contra servicios reales solo se validan en staging.
- No cambiar la versión Maven `0.0.1-SNAPSHOT` ni la versión NPM `0.1.0` automáticamente: son metadatos de artefactos que requieren una política de sincronización explícita. La etiqueta Git identifica la liberación; si el equipo decide alinearlos, hacerlo con lockfile, pruebas y revisión.
- No reetiquetar `v1.2.1`, no editar su release histórica ni completar datos de la nueva línea base anticipadamente.

## Registro pendiente de liberación

| Campo | Valor |
| --- | --- |
| Versión candidata | `v1.3.0` (propuesta, no aprobada) |
| Base de preparación | `develop` `79f9658c6a01988ceca96852946ea5729b2c02fc` |
| PR develop → staging | Pendiente |
| Commit y despliegue de staging | Pendientes |
| Evidencia QA funcional de staging | Pendiente |
| Aprobación del Product Owner | Pendiente |
| PR staging → main | Pendiente |
| Commit exacto y etiqueta de main | Pendientes |
| Identificador de línea base | Pendiente de contrastar registro oficial |
