
# SCRUM-318 — QA de sesión vencida y cambio de identidad en nueva consulta

## 1. Identificación y alcance

- **Incidencia:** [SCRUM-318](https://gestion-casos-juridicos.atlassian.net/browse/SCRUM-318).
- **Origen:** CP-26 (Anexo F), CICLO-01 y hallazgo J01; incidencia principal SCRUM-316.
- **Dependencia funcional:** SCRUM-317.
- **Objetivo:** verificar con Playwright el manejo de `401` en `POST /api/consultas`, la conservación temporal del borrador, la revalidación y el reenvío explícito, el control de `403`, la confirmación de salida y la eliminación de información al cambiar de identidad.
- **Alcance de ejecución:** frontend local con interceptación de API mediante `page.route`; no se empleó backend real, datos personales reales ni almacenamiento remoto.

## 2. Hallazgo detectado y corrección propuesta

Durante la ampliación de las pruebas se simuló una consulta en la que el primer `POST /api/consultas` respondía `401` y, al revalidar `GET /api/auth/me`, la identidad cambiaba del usuario sintético `1` al `2`.

**Resultado anterior a la corrección: FAIL.** La descripción del borrador del usuario inicial permanecía en el formulario después de revalidar con otra cuenta. El fallo quedó localizado en la aserción de Playwright que esperaba la eliminación del contenido.

**Causa identificada en el componente:** `NuevaConsultaForm.jsx` revalidaba permisos, pero no comparaba la identidad autenticada con la identidad registrada al cargar la vista.

**Cambio implementado para revisión:** registrar el identificador inicial en una referencia de React; comparar ese identificador al revalidar; ante un cambio, vaciar los datos del formulario, las selecciones, los archivos adjuntos seleccionados y los datos auxiliares vinculados al borrador; informar el cambio de cuenta. El nuevo usuario debe completar un formulario nuevo, y la revalidación no ejecuta automáticamente otro `POST`.

Esta corrección funcional fue detectada durante QA de SCRUM-318 y afecta un criterio relacionado con SCRUM-317. **Requiere trazabilidad del defecto mediante un Bug vinculado y revisión independiente antes de integrar.** No se considera cerrada esa aprobación por el solo hecho de superar las pruebas locales.

## 3. Casos ejecutados

| Caso | Comportamiento verificado | Resultado |
|---|---|---|
| CP-26 / 401 y reintento manual | El primer `POST` devuelve `401`; se muestra aviso, permanece la ruta y se conserva el borrador; revalidación fallida no reenvía; revalidación exitosa de la misma identidad permite un segundo `POST` únicamente por acción explícita | PASS |
| CP-26 / 403 | El rechazo `403` no habilita la creación indebida | PASS |
| CP-26 / validaciones | Un formulario incompleto no se envía | PASS |
| CP-26 / confirmación de salida | Se exige confirmación antes de descartar el borrador; la navegación se verifica usando permisos coherentes con la página de destino | PASS |
| CP-26 / cambio de identidad | La revalidación con otra identidad informa el cambio, elimina los campos jurídicos y selecciones comprobadas, retira el archivo ficticio seleccionado, mantiene la ruta y no reenvía el `POST` anterior; el formulario vacío no se envía | PASS |

En los datos de prueba se utilizaron exclusivamente identidades, contenidos y archivos ficticios. El caso de cambio de identidad verifica el archivo seleccionado en la interfaz **antes de cualquier subida**, no la cancelación de una carga previamente iniciada.

## 4. Ambiente y comandos reproducibles

- **Entorno:** frontend local en Windows / PowerShell, Next.js 16.4.1, proyecto Playwright `chromium`, un worker.
- **Mocks:** `GET /api/auth/me`, CSRF, catálogos/personas y `POST /api/consultas`, controlados por `page.route("**/api/**", ...)`.
- **Archivo de pruebas:** `frontend/e2e/new-consultation-session-expiry.spec.js`.
- **Archivo funcional:** `frontend/src/components/forms/consulta/NuevaConsultaForm.jsx`.

Ejecutados desde la carpeta `frontend`:

```powershell
npx playwright test e2e/new-consultation-session-expiry.spec.js --project=chromium --workers=1 --grep "descarta el borrador cuando cambia" --reporter=line
npx playwright test e2e/new-consultation-session-expiry.spec.js --project=chromium --workers=1 --reporter=line
git diff --check
npx biome lint src/components/forms/consulta/NuevaConsultaForm.jsx e2e/new-consultation-session-expiry.spec.js
npm run build
```

## 5. Resultados observados

| Verificación | Resultado real comunicado |
|---|---|
| Playwright, cambio de identidad (ejecución final aislada) | **1 passed (18.3s)** |
| Playwright, suite completa CP-26 | **5 passed (22.2s)** |
| `git diff --check` | Sin salida; sin errores de whitespace |
| Biome, dos archivos | **0 errores; 3 advertencias existentes en el código de base; sin correcciones automáticas** |
| `npm run build` | **PASS**; compilación optimizada exitosa; **23/23** páginas estáticas generadas |

Las tres advertencias informadas por Biome corresponden a `lint/a11y/noLabelWithoutControl`, `lint/correctness/noUnusedImports` y `lint/correctness/useExhaustiveDependencies` en `NuevaConsultaForm.jsx`. Están presentes en la versión de referencia de `develop` y no se modificaron en este alcance.

Se produjo una falla intermitente anterior al localizar el campo `fecha` durante la carga inicial; una ejecución posterior aislada y la suite final pasaron. Una corrida de la suite también falló inicialmente al navegar al listado porque el mock no contemplaba el permiso requerido por la página destino; se corrigió el permiso del usuario **simulado**, sin modificar la autorización real.

## 6. Seguridad, privacidad y limitaciones

- Los campos jurídicos sensibles permanecen en memoria durante la recuperación **de la misma identidad**, sin persistir el borrador en `localStorage`, `sessionStorage` ni parámetros de URL según la cobertura instrumentada.
- La prueba de cambio de identidad valida el descarte de datos y de un archivo ficticio seleccionado; no representa un ensayo de cancelación de upload en curso.
- `401` y `403` provienen de **respuestas simuladas**. Esta evidencia **no acredita** el comportamiento del proveedor de identidad ni del backend en un entorno real.
- No se ha ejecutado ni acreditado aquí una CI remota posterior al cambio; los resultados corresponden al entorno local reportado.
- La presencia de un resultado PASS no sustituye revisión de código, validación independiente ni seguimiento del defecto detectado.

## 7. Trazabilidad y aprobación pendiente

- **Branch de trabajo:** `test/scrum-318-new-consultation-session-qa`.
- **Commit:** pendiente al preparar esta evidencia; registrar SHA después de crearlo.
- **Pull request:** pendiente; registrar URL después de abrirla.
- **Bug asociado a criterio de SCRUM-317:** pendiente de registrar y vincular.
- **Revisión independiente:** pendiente, según los revisores indicados en SCRUM-318 (Nelson Enrique Conde Tarazona / José Manuel Pérez Rodríguez).
- **Estado de QA local:** PASS para los cinco casos instrumentados.
- **Estado de integración/cierre:** pendiente de commit, PR, vinculación del defecto y aprobación independiente.
