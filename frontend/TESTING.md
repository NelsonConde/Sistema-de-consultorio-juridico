# Testing - Frontend

Documento de referencia para la ejecución y mantenimiento de pruebas automatizadas del frontend.

El frontend utiliza Playwright para pruebas end-to-end sobre los flujos principales de la aplicación.

## Herramienta

- Playwright.
- Navegador principal configurado: Chromium.
- Configuración central: `playwright.config.js`.
- Carpeta de pruebas: `e2e/`.

## Requisitos

Antes de ejecutar pruebas, asegúrate de tener instaladas las dependencias del frontend:

```bash
npm install
```

Instalar navegadores de Playwright cuando sea necesario:

```bash
npx playwright install
```

## Comandos disponibles

Ejecutar pruebas en modo headless:

```bash
npm run test
```

Ejecutar pruebas con interfaz de Playwright:

```bash
npm run test:ui
```

Ejecutar pruebas con navegador visible:

```bash
npm run test:headed
```

Ejecutar pruebas en modo depuración:

```bash
npm run test:debug
```

## Configuración general

La configuración de Playwright se encuentra en:

```text
playwright.config.js
```

Aspectos principales de la configuración:

- `testDir`: define la carpeta `e2e/` como ubicación de las pruebas.
- `baseURL`: define la URL base del frontend.
- `webServer`: permite iniciar el servidor de desarrollo antes de correr pruebas.
- `reporter`: genera reporte HTML de resultados.
- `trace`: conserva trazas en reintentos para facilitar diagnóstico.

La URL base puede configurarse mediante variable de entorno:

```text
BASE_URL
```

## Estructura recomendada

Las pruebas end-to-end deben organizarse por flujo funcional.

Ejemplo de estructura:

```text
frontend/
  e2e/
    auth.spec.js
    usuarios-roles.spec.js
    consultas.spec.js
    conciliaciones.spec.js
```

Cada archivo debe agrupar pruebas relacionadas con un módulo o flujo de usuario.

## Datos y usuarios de prueba

No se deben documentar ni versionar credenciales reales.

Los usuarios, contraseñas y datos de prueba deben configurarse de forma controlada mediante:

- variables de entorno locales;
- datos semilla definidos por el equipo;
- usuarios de prueba creados específicamente para ambientes de desarrollo o testing.

Ejemplo de variables esperadas para pruebas:

```text
E2E_USER_EMAIL
E2E_USER_PASSWORD
E2E_ADMIN_EMAIL
E2E_ADMIN_PASSWORD
```

Los valores reales deben configurarse fuera del repositorio.

## Buenas prácticas

- No usar usuarios reales en pruebas automatizadas.
- No incluir contraseñas, tokens, llaves ni secretos en archivos versionados.
- No depender de datos personales reales.
- No hardcodear URLs del backend.
- Usar datos de prueba controlados.
- Mantener las pruebas alineadas con los permisos y reglas reales del backend.
- Evitar pruebas que dependan del orden de ejecución.
- Limpiar o aislar datos creados durante las pruebas cuando sea necesario.

## Reportes y resultados

Playwright puede generar reportes locales en:

```text
playwright-report/
```

También puede generar resultados temporales en:

```text
test-results/
```

Estas carpetas son artefactos generados y no deben versionarse en Git.

Para visualizar el reporte local después de una ejecución:

```bash
npx playwright show-report
```

## Autenticación en pruebas

Las pruebas que consuman rutas protegidas deben contemplar que el backend maneja sesión mediante cookie.

Cuando una prueba realice peticiones directas con `fetch` o `request`, debe respetar el mecanismo de autenticación configurado en el sistema.

En flujos desde navegador, el login debe realizarse mediante la pantalla o flujo oficial de autenticación del frontend.

## Pruebas por módulo

### Autenticación

Validaciones recomendadas:

- carga correcta de pantalla de login;
- login con credenciales válidas de ambiente de prueba;
- manejo de credenciales inválidas;
- validación de campos requeridos;
- cierre de sesión.

### Usuarios, roles y permisos

Validaciones recomendadas:

- carga de pantalla de usuarios;
- asignación de rol según permisos;
- visualización de campos específicos según perfil;
- validaciones de formulario;
- manejo de respuestas de error.

### Consultas jurídicas

Validaciones recomendadas:

- consulta de listados;
- creación o edición según permisos;
- validaciones de formulario;
- manejo de estados de consulta;
- restricciones por rol y alcance.

### Conciliaciones

Validaciones recomendadas:

- listado de conciliaciones visibles para el usuario;
- detalle de conciliación;
- creación desde consulta con solicitud PDF;
- asignación de estudiante;
- asignación de conciliador;
- cambio de estado operativo;
- finalización con acta PDF;
- bloqueo de acciones según permisos y alcance.

## Manejo de errores en pruebas

Las pruebas deben validar respuestas esperadas para:

- `400 Bad Request`: error de validación o negocio;
- `401 Unauthorized`: sesión no válida;
- `403 Forbidden`: falta de permiso o alcance;
- `404 Not Found`: recurso inexistente;
- `500 Internal Server Error`: error no controlado.

## Integración con CI

En ambientes de integración continua se recomienda:

- ejecutar pruebas con `npm run test`;
- usar `CI=true`;
- conservar reportes como artefactos del pipeline;
- no subir reportes generados al repositorio;
- configurar variables de prueba desde el entorno seguro del pipeline.

## Criterios de mantenimiento

- Toda prueba debe corresponder a un flujo vigente del sistema.
- Si cambia un endpoint, permiso o regla de negocio, deben actualizarse las pruebas relacionadas.
- Si cambia el flujo de autenticación, deben actualizarse las pruebas de sesión.
- Si cambia la estructura visual de una pantalla, deben revisarse los selectores usados por las pruebas.