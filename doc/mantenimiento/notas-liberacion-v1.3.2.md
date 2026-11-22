# Notas de entrega — v1.3.2 (candidata)

> **Estado:** preparada para revisión; **no publicada**. No declarar despliegue ni fecha de salida hasta verificar el SHA de `main`, los controles, el entorno y la aprobación del Product Owner.

**Tipo:** PATCH (correcciones compatibles).  
**Versión anterior:** [v1.3.1](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/releases/tag/v1.3.1) — publicada el 18/11/2026 (America/Bogota).  
**Versión candidata:** `v1.3.2`.  
**Procedimiento:** una promoción consolidada `develop` → `staging` → `main`.

## Resumen

Esta candidata integra **dos correcciones ya desarrolladas y probadas conjuntamente en Preview**:

1. La descarga de la Solicitud y del Acta PDF en Conciliaciones vuelve a utilizar el contrato existente del servicio de archivos.
2. La autenticación puede usar un proxy opcional del mismo origen en Next.js, para funcionar en navegación privada sin depender de cookies de terceros entre el frontend y la API.

Las dos correcciones están integradas en `develop` y cuentan con pruebas de regresión y controles CI. No se modifican endpoints backend, migraciones SQL ni el modelo de permisos.

## Cambios incluidos

### Conciliaciones — documentos PDF

- `ConciliacionesForm.jsx` deja de invocar el método inexistente `fileApi.downloadById`.
- Usa `fileApi.download(file, { type: "conciliacion", id: conciliacionId })` sobre el descriptor obtenido del listado.
- Se incorpora `conciliacion-download-contract.test.mjs` a la verificación automática.
- **Trazabilidad:** [PR #127](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/127); referencia histórica SCRUM-232.

### Autenticación — proxy de API

- `next.config.mjs` añade un rewrite opcional `/api/:path*` hacia un origen HTTPS de backend validado.
- No se activa cuando `API_PROXY_ORIGIN` no está configurada.
- Se añade `api-proxy.test.mjs` y el paso correspondiente al CI.
- **Trazabilidad:** [PR #128](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/128); referencia histórica SCRUM-112.

## Verificación disponible

| Validación | Resultado | Limitación |
|---|---|---|
| CI de PR #127 y #128 | PASS | No sustituye verificación de la versión consolidada en staging |
| Vercel Preview | READY/success | No confirma despliegue de Production |
| Login Edge InPrivate, consultas, personas, estadísticas | PASS, informado por el usuario | Prueba manual de Preview |
| Descarga de Solicitud/Acta PDF | PASS, informado por el usuario | Prueba manual de Preview |
| Listado, carga y descarga de archivos de consultas/procesos | PASS, informado por el usuario | Prueba manual de Preview |
| Entorno staging y dominio oficial | Pendiente | Debe ejecutarse tras la promoción correspondiente |

## Configuración y compatibilidad

Para habilitar el proxy en un entorno:

- `API_PROXY_ORIGIN`: origen HTTPS del backend, **sin ruta ni credenciales**, como variable no pública de Next.js.
- `NEXT_PUBLIC_API_URL_BASE`: URL **absoluta** del dominio frontend del mismo entorno con ruta `/api`. La variable se incorpora durante la compilación.
- Verificar en el backend `CORS_ALLOWED_ORIGINS`, `FRONTEND_URL` y las opciones de cookie/sesión aplicables.

**Estado comprobado antes de esta candidata:** `API_PROXY_ORIGIN` sólo estaba definida para Preview restringido a la rama de prueba; aún no figuraba como variable de Production. La entrada Production de `NEXT_PUBLIC_API_URL_BASE` existía, pero su valor no fue inspeccionado. Estas condiciones deben verificarse y ajustarse antes de publicar.

El comportamiento sin proxy se conserva cuando `API_PROXY_ORIGIN` no está definida, aunque el inicio de sesión en navegación privada podría seguir sujeto a limitaciones de cookies de terceros.

## Despliegue, recuperación y trazabilidad

- La promoción a `staging` y `main` requiere PR, CI, revisión independiente y aprobación de los responsables; consultar [preflight v1.3.2](preflight-liberacion-v1.3.2.md).
- Confirmar respaldo, disponibilidad del backend y almacenamiento, cookies, CSRF y descarga de archivos en el entorno final.
- Si el proxy produce una regresión, deshabilitar `API_PROXY_ORIGIN` y volver a desplegar la versión previamente validada, entendiendo que esto restaura las limitaciones anteriores para navegación privada; si falla una descarga, revertir mediante el flujo de ramas autorizado.
- No se incluyen cambios nuevos en DB, migraciones SQL ni endpoints backend.
- No modificar registros históricos de Jira o de los sprints finalizados.

## Datos a completar después de la publicación

- PR y SHA de promoción `develop` → `staging`: **pendiente**.
- Evidencia de validación real en `staging`: **pendiente**.
- Aprobación del Product Owner: **pendiente de acreditar**.
- PR y SHA de promoción `staging` → `main`: **pendiente**.
- SHA final, deployment Production, etiqueta Git y Release `v1.3.2`: **pendientes**.
- Fecha de publicación efectiva: **pendiente**.

La versión `v1.3.1` permanece publicada e inalterada.
