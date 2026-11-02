# Frontend - Activación pública de cuenta

## Propósito

SCRUM-295 incorpora el flujo público que permite crear la contraseña inicial a partir del enlace de invitación. La ruta no exige una sesión autenticada y no presenta datos de la cuenta asociada al token.

```text
/activar-cuenta?token=<token-de-activacion>
```

## Componentes

| Archivo | Responsabilidad |
|---|---|
| `frontend/src/app/activar-cuenta/page.js` | Declara la ruta, los metadatos sin indexación y la política `no-referrer`. |
| `frontend/src/components/auth/ActivarCuentaForm.jsx` | Retira el token de la URL, presenta el formulario y controla los estados del flujo. |
| `frontend/src/lib/account-activation.js` | Encapsula el contrato HTTP y traduce códigos funcionales a estados seguros de interfaz. |
| `frontend/e2e/account-activation.spec.js` | Cubre seguridad del token, validaciones, payload, CSRF, doble envío, estados y viewports. |

## Manejo del token

Al montar el componente se ejecuta el siguiente orden:

1. Se lee `token` desde la URL mediante la API del navegador.
2. Se conserva únicamente en una referencia en memoria del componente.
3. Se reemplaza la entrada actual del historial por `/activar-cuenta`, sin query parameters.
4. Se elimina la referencia al completar el flujo, recibir un estado terminal o desmontar la vista.

El token no se guarda en `localStorage`, `sessionStorage`, cookies creadas por el frontend, estado serializado, URL de redirección ni logs. La página declara `referrer: no-referrer` y no integra analítica ni recursos de terceros.

## Formulario y validación

El formulario contiene exclusivamente:

- nueva contraseña;
- confirmación de nueva contraseña.

La política reutiliza `passwordPolicyRule()`:

- entre 8 y 100 caracteres;
- al menos una letra mayúscula;
- al menos un número.

La confirmación se valida en el navegador con `passwordsMatchRule()` y no se transmite. Los campos admiten mostrar u ocultar el valor mediante botones con nombre accesible. El botón principal se deshabilita durante la petición y una referencia síncrona evita un segundo envío antes de que React actualice la vista.

## Contrato HTTP aislado

El único punto que conoce el endpoint y el payload es:

```text
frontend/src/lib/account-activation.js
```

Contrato consumido por esta implementación:

```http
POST /api/auth/activar-cuenta
Content-Type: application/json
X-CSRF-TOKEN: <valor obtenido de /api/auth/csrf>
```

```json
{
  "token": "token-de-activacion",
  "passwordNueva": "nueva-contraseña"
}
```

`apiClient` obtiene un CSRF nuevo antes del `POST` y envía las cookies con `credentials: "include"`. La confirmación no forma parte del payload.

La implementación backend de SCRUM-293 no está presente en la línea base inspeccionada. Por ello, nombre del endpoint, payload y códigos funcionales deben contrastarse con el contrato final antes de integrar. Si el contrato cambia, el ajuste queda limitado a `account-activation.js` y a los fixtures E2E; la vista no depende de detalles del backend.

## Estados presentados

| Estado | Señal reconocida | Resultado visible |
|---|---|---|
| Válido | Token presente y formulario sin enviar | Permite crear y confirmar la contraseña. |
| Vencido | HTTP `410` o código con `VENCID`/`EXPIRED` | Solicita una nueva invitación. |
| Usado | HTTP `409` o código con `USADO`/`UTILIZADO`/`USED` | Permite ir al inicio de sesión. |
| Revocado | Código con `REVOC`/`REVOKED` | Informa que el enlace ya no está disponible. |
| Inválido | Token ausente, HTTP `400`, `404` o `422` | Informa que el enlace es inválido o no está disponible. |
| No disponible | Red, CSRF, HTTP no clasificado o servidor | Conserva los campos y permite reintento explícito. |
| Éxito | Respuesta HTTP exitosa | Limpia token y contraseñas y ofrece ir al login. |

Los textos no incluyen correo, usuario, nombre ni mensajes arbitrarios del backend. Cuando existe, solo se presenta el identificador de correlación para soporte.

## Accesibilidad y distribución

- Etiquetas asociadas a cada campo.
- Errores vinculados mediante `aria-describedby` y anunciados con `role="alert"`.
- Foco programático al cambiar entre validación inicial y estados terminales.
- Controles operables con teclado y nombres accesibles para mostrar u ocultar contraseñas.
- Diseño apilado en móvil y panel informativo lateral desde `lg`.
- Cobertura de ausencia de scroll horizontal en 360, 768 y 1280 píxeles.
- Tema claro, oscuro o del sistema mediante el selector existente.

## Pruebas y evidencia

Archivo focalizado:

```text
frontend/e2e/account-activation.spec.js
```

La suite define nueve casos deterministas con tokens y respuestas sintéticas:

1. retirada del token de URL e historial;
2. ausencia de secretos en almacenamiento del navegador;
3. política y confirmación local sin petición prematura;
4. payload mínimo, CSRF y doble envío bloqueado;
5. estados vencido, usado y revocado;
6. error de red con datos conservados y reintento explícito;
7. enlace sin token;
8. ausencia de scroll horizontal en 360, 768 y 1280 píxeles;
9. ausencia del token y la contraseña en consola.

Validación local realizada con Node 22.22.0 y npm 10.9.4:

| Verificación | Resultado |
|---|---|
| Biome sobre los cuatro archivos frontend/E2E | PASS |
| `npm run build` | PASS; `/activar-cuenta` se prerenderiza correctamente. |
| `playwright test e2e/account-activation.spec.js --list` | PASS; 9 casos descubiertos. |
| Ejecución con navegador | INCONCLUSA en el entorno local: Next.js no pudo enumerar interfaces de red al ser iniciado por el `webServer` de Playwright. Los casos no se marcan como aprobados hasta ejecutarse en CI o en un host compatible. |

## Condición de integración

La PR frontend puede revisarse de forma independiente, pero no debe declararse integrada de extremo a extremo hasta que SCRUM-293 entregue el contrato backend definitivo y la suite E2E se ejecute con navegador. No se deben usar tokens reales en capturas, fixtures o registros.
