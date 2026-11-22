# Preflight de liberación v1.3.2 — promoción consolidada

> **Estado:** candidata PATCH en preparación, **no publicada**. Este documento no certifica ejecución en `staging`, aprobación del Product Owner ni despliegue de producción.

## 1. Identificación de la candidata

- **Última versión publicada y verificada:** [v1.3.1](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/releases/tag/v1.3.1), 18/11/2026 en America/Bogota (publicación en GitHub: 19/11/2026 03:19:33 UTC).
- **Siguiente candidata:** `v1.3.2` (PATCH). No reutilizar la etiqueta `v1.3.1`.
- **Origen:** `develop`. Registrar SHA de corte inmediatamente antes de promocionar; no congelar un SHA que todavía puede incorporar esta documentación.
- **Destino:** `develop` → `staging` → `main` con PR y merge commits revisados. **Una sola promoción del mismo alcance consolidado**, no dos liberaciones parciales.
- **Versionado:** etiqueta Git y GitHub Release sobre el SHA final de `main`, después de aprobación y verificación; no modificar etiquetas históricas.
- **Migraciones:** las diferencias verificadas entre `develop` y `main` para este cambio son cinco rutas frontend/workflow; **no hay SQL de migración ni cambios nuevos de backend** en ese diferencial. La ausencia de cambios no elimina el preflight de respaldo y disponibilidad del ambiente.

## 2. Alcance técnico

| PR | Tipo | Alcance |
|---|---|---|
| [#127](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/127) — referencia histórica SCRUM-232 | FIX/TEST | Corrige Solicitud/Acta PDF en Conciliaciones usando `fileApi.download(file, resource)`; incluye test de contrato y paso CI. |
| [#128](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/128) — referencia histórica SCRUM-112 | FIX/TEST/CONFIG | Proxy API same-origin **opt-in** en `next.config.mjs`, validación de upstream HTTPS, test de compatibilidad y paso CI. |

Ambas PR están integradas en `develop` después de revisión independiente. Los identificadores SCRUM son **referencias funcionales históricas**, no tickets reabiertos ni sprints nuevos.

**Diferencias de código frente a `main` observadas antes de esta preparación:** `.github/workflows/build.yml`, `frontend/next.config.mjs`, `frontend/src/components/forms/conciliacion/ConciliacionesForm.jsx`, `frontend/tests/api-proxy.test.mjs`, `frontend/tests/conciliacion-download-contract.test.mjs`.

**Documentación que debe acompañar el mismo corte:** `CHANGELOG.md`, `frontend/README.md`, `doc/mantenimiento/README.md` y los dos documentos `v1.3.2` de esta carpeta. Mantener los archivos históricos de `v1.3.1` como registros de preparación, sin tratarlos como estado vigente.

## 3. Evidencia disponible y límites

- CI de las PR #127 y #128: GitHub Actions `Build` y `PR Title Convention` completados satisfactoriamente; Vercel reportó éxito en ambas ramas.
- Retest en Preview con Edge InPrivate, **reportado por el usuario**: login, Consultas, Personas, descarga Solicitud/Acta PDF, Estadísticas con PDF y archivos de consultas/procesos. Las capturas del login verificaron solicitudes de autenticación HTTP 200.
- La prueba de Preview **no acredita** despliegue de la candidata `v1.3.2` en `staging` ni en `main`; tampoco demuestra funcionamiento de las variables de Vercel Production.
- No adjuntar capturas con tokens, cookies, claves, datos jurídicos ni documentos de usuarios.

## 4. Configuración de frontend y backend

**Hallazgo real en Vercel (inspección de nombres, destinos y ramas; sin consultar valores):**

| Variable | Estado verificado | Acción antes de producción |
|---|---|---|
| `API_PROXY_ORIGIN` | Existe para **Preview, restringida a** `bugfix/incognito-api-proxy-validation`; **no aparece para Production** | Configurar origen HTTPS real del backend para el entorno objetivo y verificar que no contenga ruta, credenciales ni query. |
| `NEXT_PUBLIC_API_URL_BASE` | Existe para **Preview, restringida a** esa rama, y otra entrada en **Production** | Para usar el proxy, verificar/ajustar el valor de Production a la URL absoluta del propio dominio frontend seguida de `/api`, y **reconstruir** el frontend. |
| `CORS_ALLOWED_ORIGINS`, `FRONTEND_URL`, política de cookies | Configuración de Railway no acreditada en esta revisión; el Preview había usado una autorización de origen adicional | Revisar orígenes autorizados y sesiones reales por ambiente, sin publicar valores sensibles. |

- `API_PROXY_ORIGIN` se evalúa en `next.config.mjs`; si no está definida, el rewrite no se activa.
- `NEXT_PUBLIC_API_URL_BASE` es pública y de build; no contiene secretos. Según `src/lib/config.js`, requiere URL absoluta y se normaliza para terminar en `/api`.
- No realizar escrituras en secretos/variables de Production sin autorización específica; probar la configuración antes de asignar dominio principal.
- Confirmar rama productiva y SHA del despliegue en Vercel, rama e imagen de Railway, backups y compatibilidad con almacenamiento privado; este documento no presupone correspondencia automática entre Git y proveedores.

## 5. Gates para promoción única

### Integridad del corte `develop` (antes de crear PR de promoción)

- [ ] PR documental de `v1.3.2` integrada en `develop`, con revisión independiente.
- [ ] Registrar SHA exacto de `develop`; verificar su diferencia contra `staging` y `main` y que sólo contiene alcance autorizado.
- [ ] CI del SHA definitivo completo y satisfactorio; incluir frontend, tests de proxy/descarga, backend, Playwright y Docker según workflows aplicables.
- [ ] Revisar que documentación, changelog, notas y política de rollback describen la misma versión.

### Validación en `staging`

- [ ] Crear PR `develop` → `staging`, revisión independiente y CI del head vigente; fusionar mediante merge commit autorizado.
- [ ] Verificar que el SHA y los archivos de `staging` corresponden al corte; comprobar variables/URL del entorno.
- [ ] Repetir login en Edge InPrivate, `/auth/me`, logout y CSRF; probar Consultas, Personas, Conciliaciones (Solicitud/Acta PDF), Estadísticas y archivos, respetando permisos.
- [ ] Registrar evidencias sanitizadas, resultados esperados/observados y responsables; si falla algo, corregir en rama temporal y repetir el gate afectado.
- [ ] Verificar disponibilidad de backend, almacenamiento y plan de recuperación antes de promover.

### Validación de producción

- [ ] Obtener aprobación expresa del Product Owner y autorizaciones de release según el manual del repositorio.
- [ ] Crear PR `staging` → `main`, revisión independiente y checks completos; merge commit autorizado.
- [ ] Verificar configuración Production de Vercel, dominios, URL pública, configuración de backend y despliegue con el SHA esperado.
- [ ] Comprobar login privado y operaciones representativas, incluyendo descarga de PDFs y documentos; registrar rollback posible.
- [ ] Sólo tras confirmar la versión final, crear y publicar etiqueta/Release **`v1.3.2`** en `main` con el SHA real y conservar `v1.3.1` sin cambios.

## 6. Registro final de la liberación (pendiente de datos verificables)

| Dato | Estado |
|---|---|
| SHA de corte `develop` | Pendiente de integrar documentación y fijar corte |
| PR `develop` → `staging` | Pendiente |
| Resultado de validación funcional `staging` | No ejecutado en este corte |
| Aprobación Product Owner / cliente | Pendiente de acreditar |
| PR `staging` → `main` | Pendiente |
| SHA final `main` y artefacto desplegado | Pendiente |
| Etiqueta Git y GitHub Release `v1.3.2` | No creadas |
| Fecha efectiva de publicación | Pendiente |

La preparación no altera Jira, backlog, registros históricos de sprint ni los datos de Production.
