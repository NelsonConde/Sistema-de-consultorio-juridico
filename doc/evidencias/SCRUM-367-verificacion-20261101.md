# SCRUM-367 — Verificación independiente de SCRUM-353

Fecha: 01/11/2026, America/Bogota.
Base: `develop` `21d752b66ed574f1be2f646cb21052fc74242bcd`.
Entrega funcional: PR #59, merge `8311709`.

## Trabajo realizado y resultados

- **PASS, 5/5 Playwright, Chromium:** perfiles administrativo y asesor cargan
  una consulta y envían el proceso; búsqueda/paginación usan `fecha/desc`;
  resultado vacío conserva la búsqueda; error de red permite reintentar sin
  perder el departamento seleccionado.
- Se corrigió una ambigüedad del test de reintento: `getByRole("alert")` también
  encontraba el anunciador de navegación de Next.js. El selector ahora exige
  que la alerta contenga «Reintentar». No se modificó la aplicación.
- **PASS, HTTP con Spring Security y PostgreSQL real:** `/api/consultas` acepta
  `sortBy=fecha&direction=desc` y rechaza `id` y un campo inexistente con 400.
- **PASS, 12/12** `ConsultaControllerAuthorizationTest`: permisos de lectura y
  operaciones protegidas; servicio de negocio simulado en esos tests.

## Alcance de la evidencia

Playwright utiliza API simulada: sus roles y consultas ajenas son fixtures y no
acreditan filtrado real de filas ni creación persistente de un proceso. La
comprobación HTTP usa MockMvc con filtros reales y una BD sintética, pero la
consulta está vacía. No se declara un recorrido navegador–API–BD completo.

La aplicación no se probó contra producción. PostgreSQL local es 18.6, no la
versión 16 del perfil Testcontainers. Docker no resultó accesible al usuario.
Las pruebas no certifican compatibilidad con el proveedor de despliegue.

## Reproducción

Backend (Java 21, Maven 3.9.14 y herramientas PostgreSQL en PATH):

```sh
bash docs/testing/ops/run-sprint7-local-qa.sh
```

Frontend (Node 22 y npm 10.9.4; dependencias del lockfile):

```sh
cd frontend
npm ci
npx playwright test e2e/new-process-consultation-selector.spec.js --reporter=list
```

Se usaron Node 22.23.4, npm 10.9.4, Java 21.0.12.1 y Maven 3.9.14.
El runner crea y elimina un cluster propio; nunca recibe una URL de despliegue.

## Estado propuesto

Pasar SCRUM-367 de **Por hacer** a **En revisión** para entregar estos resultados.
Antes de Done, completar o aceptar expresamente el límite de cobertura: creación
persistente de proceso y exclusión real de consultas ajenas con ambos roles.
No se cambió Jira ni se declaró una aprobación independiente.
