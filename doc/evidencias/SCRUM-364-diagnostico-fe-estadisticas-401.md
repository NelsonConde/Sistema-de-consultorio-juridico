# SCRUM-364 — Diagnóstico frontend del 401 en estadísticas

Fecha de revisión: 2026-11-02 (America/Bogota).

## Resultado

**DEFECTO FRONTEND RESPALDADO POR INSPECCIÓN ESTÁTICA; EJECUCIÓN E2E PENDIENTE.**

El módulo no aplica un tratamiento uniforme a las respuestas `401`. La
comprobación inicial de sesión redirige a `/`, pero las solicitudes propias de
estadísticas permanecen en la vista y muestran un estado silencioso, un error
genérico, el mensaje recibido del servidor o un `alert`, según la operación.

Este diagnóstico no determina por qué el backend produjo el `401` observado en
CP-23. Ausencia, expiración o invalidez de sesión son hipótesis que requieren la
traza backend coordinada por SCRUM-323. Tampoco se declara una nueva ejecución
de CP-23 ni se invalida su resultado histórico.

## Línea base y método

| Campo | Valor |
|---|---|
| Repositorio | `NelsonConde/Sistema-de-consultorio-juridico` |
| Rama | `develop` |
| SHA revisado | `26e42a6f1a4386d01d0858f632af62cdcdb53b91` |
| Método | Inspección del código actual y creación de escenarios Playwright con respuestas sintéticas |
| Ambiente de datos | Sin cookies, tokens, credenciales ni datos jurídicos reales |
| Alcance | Exclusivamente frontend de estadísticas |

La numeración histórica RF96/RF97 citada por el Anexo F no coincide con la
numeración RF56/RF57 del SRS/EP5 vigente. La diferencia documental no explica
un `401`, no cambia el contrato HTTP y no invalida CP-23.

## Peticiones identificadas

Todas las solicitudes usan `apiClient`, que configura
`credentials: "include"`. El JWT no se lee desde el componente.

| Momento | Método y endpoint | Propósito |
|---|---|---|
| Apertura de la vista | `GET /api/auth/me` | Validar sesión y permiso `Ver reportes`. |
| Apertura posterior | `GET /api/estadisticas/semestres` | Cargar periodos disponibles. |
| Selección semestral | `GET /api/estadisticas/{año}/semestre/{semestre}` | Obtener estadísticas del semestre. |
| Consulta por rango | `GET /api/estadisticas/reporte?fechaInicio=...&fechaFin=...` | Obtener estadísticas del rango. |
| Exportación semestral | `GET /api/estadisticas/{año}/semestre/{semestre}/pdf` | Descargar PDF semestral. |
| Exportación por rango | `GET /api/estadisticas/reporte/pdf?fechaInicio=...&fechaFin=...` | Descargar PDF del rango. |

## Manejo real del 401

| Petición que responde 401 | Estado frontend observado en código | Redirección | Mensaje visible | CorrelationId |
|---|---|---|---|---|
| `/api/auth/me` | Finaliza la inicialización y ejecuta `router.replace("/")`. | Sí, a login. | No se presenta en el componente. | Leído por `apiResponse`, pero ignorado. |
| `/api/estadisticas/semestres` | No entra en `resSem.ok`; deja la lista vacía. | No. | Ningún mensaje específico de sesión o error. | Ignorado. |
| Reporte semestral | Entra en `!res.ok`, limpia estadísticas. | No. | `Error al cargar estadísticas.` | Ignorado. |
| Reporte por rango | Entra en `!res.ok`, limpia estadísticas. | No. | Usa `mensaje`/`message` del cuerpo si existe. | Ignorado aunque venga en header o cuerpo. |
| PDF semestral o por rango | Entra en `!res.ok`. | No. | `El PDF no está disponible aún.` mediante `alert`. | No se procesa. |

La misma sesión vencida puede producir cinco presentaciones distintas. Solo la
verificación inicial reconoce explícitamente `401`; los endpoints funcionales
solo distinguen `403` o agrupan cualquier respuesta no exitosa.

## Trazas controladas y sanitizadas

Los escenarios definidos en
`frontend/e2e/statistics-401-diagnosis.spec.js` usan exclusivamente:

```text
X-Request-ID: diag-statistics-401
HTTP/1.1 401 Unauthorized
Content-Type: application/json
```

```json
{
  "estado": 401,
  "mensaje": "Debe iniciar sesión para acceder a este recurso",
  "correlacionId": "diag-statistics-401"
}
```

La traza de cada petición conserva únicamente método, ruta y query string. No
registra headers de autenticación, cookies, JWT, CSRF, cuerpos jurídicos ni
identificadores de personas.

## Escenarios automatizados preparados

| Escenario | Resultado esperado por caracterización |
|---|---|
| `401` en `/auth/me` | Redirige a `/` y no consulta estadísticas. |
| `401` en `/estadisticas/semestres` | Permanece en `/estadisticas`, sin mensaje específico. |
| `401` en reporte semestral | Permanece y presenta error genérico. |
| `401` en reporte por rango | Permanece y presenta el mensaje del cuerpo. |
| `401` en descarga PDF | Permanece y presenta el aviso genérico del PDF. |

Estos casos caracterizan el comportamiento actual; no lo corrigen ni lo
declaran deseable.

## Evidencia y límites

- La inspección estática sobre el SHA indicado confirma rutas y ramas de manejo.
- La suite focalizada queda preparada para ejecución con respuestas sintéticas.
- Biome revisó el archivo E2E focalizado sin diagnósticos: **PASS**.
- Playwright descubrió los cinco escenarios: **PASS de descubrimiento**.
- La ejecución local focalizada fue intentada el 2026-11-02, pero Next.js no
  pudo iniciar el `webServer` por `uv_interface_addresses: Unknown system error
  1`; el proceso se detuvo de forma controlada por tiempo límite con código
  `124`. Ningún escenario alcanzó Chromium y no se contabiliza como prueba
  funcional ejecutada.
- El resultado funcional E2E permanece **INCONCLUSO** hasta ejecutar los cinco
  escenarios en CI o en un host compatible.
- No se realizó una petición contra backend desplegado, no se usaron sesiones
  reales y no se atribuye el `401` a autenticación, permisos, CORS o datos.
- El diagnóstico backend y la consolidación de causa pertenecen a SCRUM-323.

## Conclusión para el registro compartido

En la línea base revisada, el frontend de estadísticas consulta primero
`GET /api/auth/me` y después los endpoints bajo `/api/estadisticas`. Un `401` en
la verificación inicial redirige al login; el mismo estado en semestres,
reportes o PDF no redirige y se presenta de forma inconsistente. El frontend
descarta cualquier `correlationId` recibido, por lo que no permite enlazar el
fallo visible con la traza backend. Esto respalda un defecto de manejo frontend,
pero no demuestra la causa del `401` histórico de CP-23. La discrepancia de
numeración RF96/RF97 frente a RF56/RF57 es documental y no causal.

## Trabajo posterior separado

Una corrección futura debe registrarse y estimarse de forma independiente. La
opción técnica a evaluar es centralizar el tratamiento de `401` para todas las
peticiones del módulo, conservar una referencia de correlación sanitizada y
mantener `403`, errores funcionales y fallos de red como estados distintos. Este
documento no implementa esa corrección.
