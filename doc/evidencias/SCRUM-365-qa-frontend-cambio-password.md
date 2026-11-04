# SCRUM-365 — QA frontend del cambio de contraseña

Fecha de revisión: 2026-11-02, America/Bogota.

SCRUM-290 ya fue integrado en
`develop` mediante el PR #77 y su contrato frontend quedó disponible. La
cobertura automatizada fue actualizada para aceptar el `204 No Content` real;

## Alcance revisado

La revisión se limitó al consumidor frontend de:

```http
PATCH /api/auth/cambiar-password
```

Se inspeccionaron:

- `frontend/src/components/forms/perfil/CambiarPasswordForm.jsx`;
- `frontend/src/components/miPerfil/MiPerfilView.jsx`;
- `frontend/src/hooks/useApiForm.js`;
- `frontend/src/lib/apiClient.js`;
- `frontend/src/lib/form-validation.js`;
- `frontend/e2e/mi-perfil.spec.js`.

La referencia remota fue `develop` en
`8f684a56aa0cb80d9132bd528d18835294b0046c`, consultada el 2026-11-02. Ese
commit integra el PR #77 de SCRUM-290. La revisión acredita el contenido del
repositorio, no un despliegue operativo.

## Hechos comprobados por inspección

| Control | Evidencia estática | Resultado |
|---|---|---|
| Confirmación | `passwordsMatchRule` exige confirmación y compara contra la nueva contraseña. | Conforme en código |
| Payload | `onSubmit` construye exclusivamente `passwordActual` y `passwordNueva`. | Conforme en código |
| Confirmación no enviada | `confirmarPassword` se valida localmente y no se incorpora al objeto enviado. | Conforme en código |
| Método y endpoint | `useApiForm` usa `PATCH` sobre `/api/auth/cambiar-password`. | Conforme en código |
| CSRF | `apiClient` obtiene token antes de métodos no seguros y agrega el header indicado por `/api/auth/csrf`. | Conforme en código |
| 401 | `useApiForm` muestra sesión expirada y redirige a `/`. | Conforme en código |
| Mensajes | El componente presenta éxito mediante `role="status"` y error mediante `role="alert"`. | Conforme en código |
| Mostrar/ocultar | Los tres campos tienen botones con etiquetas accesibles y alternan `password`/`text`. | Conforme en código |
| Limpieza | Los campos se limpian tras éxito, al salir de Seguridad y al desmontar. | Conforme en código |
| Persistencia | No se observó escritura de contraseñas en URL, `localStorage` ni `sessionStorage`. | Conforme en código |


## Cobertura añadida

Se agregó `frontend/e2e/password-change-qa.spec.js` con seis escenarios y
fixtures completamente sintéticos:

1. confirmación diferente bloquea el envío y no solicita CSRF;
2. mostrar/ocultar funciona mediante teclado y mantiene etiquetas accesibles;
3. el PATCH contiene únicamente `passwordActual` y `passwordNueva`, junto al
   header CSRF, acepta el `204 No Content` contractual y limpia los campos tras
   éxito;
4. un rechazo `400` presenta mensaje controlado y conserva los valores para
   corrección;
5. un `401` produce una sola petición y redirección controlada;
6. un fallo al obtener CSRF impide el PATCH y conserva el formulario.

Las aserciones también comprueban que los valores sintéticos no se copian a
storage, URL ni mensajes de consola. El cuerpo HTTP se conserva únicamente en
memoria dentro del fixture para comprobar el contrato y no se imprime en la
evidencia.

## Comandos y resultados reales

Ejecutados desde `frontend`:

```bash
npx biome format --write e2e/password-change-qa.spec.js
npx biome lint e2e/password-change-qa.spec.js
npx playwright test e2e/password-change-qa.spec.js --list
npm run build
npx playwright test e2e/password-change-qa.spec.js --reporter=line
npx playwright install chromium
```

| Verificación | Resultado real |
|---|---|
| Formato Biome | PASS; un archivo formateado. |
| Lint Biome focalizado | PASS; un archivo revisado sin diagnósticos. |
| Descubrimiento Playwright | PASS; seis pruebas reconocidas. |
| Build Next.js 16.4.1 | PASS tras apartar una caché local corrupta de Turbopack; 23 páginas estáticas generadas. |
| Inicio de ejecución Playwright | INCONCLUSO; el servidor Next.js inició y Playwright programó los seis casos, pero ninguno alcanzó sus aserciones por ausencia de Chromium. |
| Instalación de Chromium | FAIL de ambiente; la descarga retornó un archivo vacío/truncado que no era un ZIP válido. |
| Ejecución E2E | NO ACREDITADA; los seis casos fallaron antes de abrir navegador por falta del ejecutable. |

Los resultados del intento local se conservan en la tabla anterior. La ejecución posterior en Chromium consta en la sección «Ejecución posterior en GitHub Actions» y está respaldada por el [CI del commit revisado, Build #357](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/actions/runs/41918440117).

## Observaciones y límites

- El error al obtener CSRF se presenta actualmente como `Error de conexión`.
  Es un fallo controlado y evita el PATCH, pero el mensaje no distingue CSRF de
  una caída de red. Si el criterio exige una explicación específica de CSRF,
  debe registrarse como defecto independiente antes de modificar aplicación.
- No se usaron cuentas, contraseñas, cookies, tokens ni datos personales reales.
- No se enviaron peticiones a un backend operativo ni se consultó producción.
- No se verificaron contra un backend operativo el hash, la contraseña anterior,
  la renovación de autenticación, la auditoría, la invalidación de sesiones ni
  la política 8-100; su consolidación corresponde a SCRUM-292. SCRUM-290 aporta
  el contrato y sus pruebas backend, pero esta evidencia no las reutiliza como
  ejecución nueva.
- No se generaron capturas antes/después porque no hubo ejecución de navegador.
- La cobertura existente de `mi-perfil.spec.js` no se reutiliza como evidencia
  de ejecución nueva.
- El checkout local es una copia extraída sin metadatos `.git` y contiene otros
  cambios frontend preparados en la misma sesión. El build acredita
  compatibilidad del conjunto local, no un commit aislado de SCRUM-365. La rama
  limpia debe repetir build y pruebas antes de crear la PR.

## Ejecución posterior en GitHub Actions — 04/11/2026

La ejecución local original descrita en la tabla anterior permanece como
**INCONCLUSA** por falta del navegador. Posteriormente, Chromium se instaló
correctamente en el runner de GitHub Actions y se ejecutaron las pruebas reales
de interfaz en un servidor Next.js local del workflow, usando peticiones
interceptadas y credenciales sintéticas. No se utilizó producción.

- **Primer intento de CI (commit `bba60f6e`):** [Build #355, ejecución 41917195932](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/actions/runs/41917195932). Resultado **FAIL**: 64 pruebas aprobadas y seis casos SCRUM-365 fallidos. El error compartido fue `strict mode violation` al utilizar `getByLabel("Contraseña actual")`, que coincidía tanto con el `input` como con el botón de visibilidad. La causa fue un selector ambiguo en la prueba, no un rechazo del endpoint.
- **Corrección focalizada (commit `1465af86f2c52938d740dd13da371370650385f8`):** se sustituyeron únicamente los localizadores de los campos por selectores específicos `input[name="passwordActual"]`, `input[name="passwordNueva"]` e `input[name="confirmarPassword"]`. No se modificaron el formulario, la política, los permisos ni el contrato de autenticación.
- **Segunda ejecución de CI:** [Build #356, ejecución 41918007858](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/actions/runs/41918007858), job **Playwright E2E**. Comando del workflow ejecutado desde `frontend/`:

  ```bash
  npm run test -- --project=chromium --workers=1 --retries=0 --reporter=line,html
  ```

  **Resultado real: PASS — 70 pruebas aprobadas (1.3 min), cero fallidas**. El log del job muestra los seis casos de `e2e/password-change-qa.spec.js` ejecutados consecutivamente (posiciones 46–51/70), sin errores, y el job finalizó exitosamente.

| Caso SCRUM-365 | Resultado en segunda CI |
|---|---|
| Validación de confirmación sin petición adicional | PASS |
| Mostrar y ocultar campos mediante controles accesibles | PASS |
| Payload permitido, CSRF, `204 No Content` y limpieza | PASS |
| Respuesta `400` controlada y conservación de campos | PASS |
| Respuesta `401` con única petición y redirección | PASS |
| Fallo al obtener CSRF sin enviar PATCH | PASS |

**Evidencia sanitizada:** URL de la ejecución y registro del job Playwright E2E
de GitHub Actions, que identifica los títulos de los seis casos y el resumen
`70 passed`. No se adjuntan cuerpos de solicitudes ni capturas con contraseñas,
cookies o tokens. Las comprobaciones son de frontend con mocks; no validan el
hash real del backend, su persistencia o políticas fuera del alcance de SCRUM-365.

**Conclusión actualizada:** la ejecución funcional de los seis escenarios
SCRUM-365 quedó acreditada como **PASS en CI** para el commit
`1465af86f2c52938d740dd13da371370650385f8`; la limitación del
primer intento local se mantiene como antecedente histórico y ya no bloquea
esta validación de navegador.
