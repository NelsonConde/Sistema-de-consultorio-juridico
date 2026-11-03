# SCRUM-292 — Verificación de cambio de contraseña, CSRF y política de sesión sin exposición de secretos

## Identificación

- **Incidencia:** [SCRUM-292](https://gestion-casos-juridicos.atlassian.net/browse/SCRUM-292)
- **Historia / Marco de trabajo:** Sprint 7 ([SCRUM-315](https://gestion-casos-juridicos.atlassian.net/browse/SCRUM-315))
- **Predecesoras verificadas:**
  - [SCRUM-290](https://gestion-casos-juridicos.atlassian.net/browse/SCRUM-290) (Edwin Torres: Política de sesión e invalidación de contraseñas, mergeada en `develop` commit `8f684a5`).
  - [SCRUM-365](https://gestion-casos-juridicos.atlassian.net/browse/SCRUM-365) / [SCRUM-352](https://gestion-casos-juridicos.atlassian.net/browse/SCRUM-352) (Juan Diego Sarmiento: Cobertura QA frontend en Mi Perfil, mergeada en `develop` commit `ac16f63`).
- **Rama de verificación:** `test/scrum-292-password-change-csrf-qa`
- **Responsable:** José Manuel Pérez Rodríguez (Operación y QA)
- **Fecha de ejecución:** 03/11/2026, zona `America/Bogota`
- **Ambiente:** Linux x86_64; OpenJDK 21; Suite de verificación automatizada JUnit 5, Mockito, Spring Test Context y validación estática de trazas.
- **Revisores designados:** Nelson Enrrique Conde Tarazona; Juan Diego Sarmiento Barroyeta.

---

## Alcance y criterios evaluados

Conforme a la definición de [SCRUM-292](https://gestion-casos-juridicos.atlassian.net/browse/SCRUM-292), se ejecutó la suite de aseguramiento de calidad sobre el flujo de cambio de contraseña, la política de invalidación de sesión y la protección CSRF:

1. **Contraseña actual:** Verificación con credencial correcta (permite cambio) frente a incorrecta (rechazo con mensaje controlado sin enumeración ni detalles internos).
2. **Nueva contraseña:** Rechazo de contraseñas idénticas a la actual, rechazo de contraseñas débiles o fuera de rango (longitud < 8 o > 100 caracteres, vacías o en blanco) y aceptación de nuevas contraseñas válidas.
3. **Confirmación de contraseña:** Control de discrepancia entre contraseña nueva y su confirmación.
4. **Estado de sesión y auth_version:** Detección y rechazo de solicitudes con sesión ausente, expirada o cuyo JWT contenga un `auth_version` revocado por un cambio anterior.
5. **Ciclo de vida CSRF:**
   - Invalidación forzada de la cookie CSRF previa tras el cambio exitoso de contraseña mediante `csrfTokenRepository.saveToken(null, request, response)`.
   - Renovación simultánea de la cookie de sesión JWT con la nueva versión.
   - En caso de fallo o excepción de negocio, la sesión previa no se altera ni se emiten cookies erróneas.
6. **Contraseña vieja vs. nueva tras el cambio:**
   - La contraseña antigua deja de autenticar inmediatamente.
   - La nueva contraseña encriptada con BCrypt autentica satisfactoriamente.
   - El contador `auth_version` del usuario incrementa de manera atómica (de $N$ a $N+1$).
7. **Inspección de trazas y auditoría (Cero exposición de secretos):**
   - La acción auditada `@Auditable(action = "CAMBIAR_PASSWORD_PROPIA")` no declara campos sensibles ni metadatos (`trackedFields = {}`, `metadata = {}`), evitando captura por reflexión de contraseñas en texto plano o hashes.
   - Verificación de filtros de ofuscación (`LoggingSafetyConfiguration`) que suprimen cabeceras `Authorization`, `Cookie`, tokens y contraseñas.
8. **Continuidad de login y recuperación:** Los flujos de autenticación inicial y recuperación de contraseña continúan 100% operativos tras cualquier cambio de credencial.

---

## Matriz de casos ejecutados

| ID | Caso de prueba | Criterio de aceptación | Resultado obtenido | Estado |
|---|---|---|---|:---:|
| **QA-PWD-01** | Contraseña actual correcta | Validación exitosa sin excepciones ante hash almacenado | Validación conforme, permite continuar flujo | **PASS** |
| **QA-PWD-02** | Contraseña actual incorrecta | Rechazo con `BusinessException("La contraseña actual no es correcta")` | Excepción capturada, mensaje exacto y controlado | **PASS** |
| **QA-PWD-03** | Nueva contraseña igual a actual | Rechazo con `BusinessException("La nueva contraseña no puede ser igual a la actual")` | Rechazo inmediato antes de persistir o emitir tokens | **PASS** |
| **QA-PWD-04** | Nueva contraseña corta o vacía (< 8 car.) | Rechazo con mensaje de longitud (entre 8 y 100 caracteres) | Probado con cadenas vacías, espacios, 'short' y '1234567'; todas rechazadas | **PASS** |
| **QA-PWD-05** | Nueva contraseña válida | Aceptación de contraseña robusta cumpliendo política | Pasa validación sin excepciones | **PASS** |
| **QA-PWD-06** | Confirmación distinta | Rechazo con `BusinessException("La nueva contraseña y su confirmación no coinciden")` | Control de integridad de confirmación validado | **PASS** |
| **QA-PWD-07** | Sesión con `auth_version` revocado | Token anterior con versión $N$ cuando la BD está en $N+1$ debe ser rechazado | Rechazo con `"Token inválido o expirado"`; no toca BD | **PASS** |
| **QA-PWD-08** | Invalidación CSRF tras cambio exitoso | Invalida token CSRF anterior y emite cookie JWT renovada | `csrfTokenRepository.saveToken(null)` verificado; cookie renovada | **PASS** |
| **QA-PWD-09** | Fallo de cambio no altera sesión | Ante fallo de validación, no se invalidan cookies válidas ni se emite nuevo JWT | Verificado: cero llamadas a invalidar y sin cookies de Set-Cookie | **PASS** |
| **QA-PWD-10** | Incremento atómico de `auth_version` | Usuario pasa a $N+1$, hash antiguo no valida y nuevo hash valida en BCrypt | `authVersion` incrementado; `passwordEncoder.matches` validado | **PASS** |
| **QA-PWD-11** | Cero secretos en auditoría | Anotación `@Auditable` con `trackedFields` y `metadata` vacíos | Inspección por reflexión: 0 campos y 0 metadatos sensibles expuestos | **PASS** |
| **QA-PWD-12** | Login y recuperación funcionales | Usuario con nueva credencial puede iniciar sesión y solicitar recuperación | Autenticación con nueva contraseña exitosa; validador de recuperación OK | **PASS** |
| **QA-PWD-13** | Invalidación CSRF en login/logout | Invalida CSRF previo en login exitoso y en cierre de sesión | Verificado en `AuthControllerCsrfTest` (5/5 pruebas) | **PASS** |
| **QA-PWD-14** | Incremento `auth_version` en reseteo | Flujo de restablecimiento de contraseña incrementa `auth_version` e invalida sesiones activas | Verificado en `PasswordResetServiceAuthVersionTest` (2/2 pruebas) | **PASS** |
| **QA-PWD-15** | Sanitización y enmascaramiento de logs | Trazas del sistema enmascaran contraseñas, tokens y cabeceras de autorización | Verificado en `LoggingSafetyConfigurationTest` (3/3 pruebas) | **PASS** |

**Resumen de ejecución:** 15 PASS, 0 FAIL, 0 SKIPPED (Total de 31 pruebas en la suite de seguridad ejecutada).

---

## Verificación de no exposición de secretos

### 1. Auditoría AOP sin fuga de credenciales
La anotación de auditoría en `AuthService.cambiarPassword` está definida de la siguiente manera:
```java
@Auditable(
        action = "CAMBIAR_PASSWORD_PROPIA",
        entityName = "UsuarioSistema",
        entityId = "'self'")
public String cambiarPassword(String tokenActual, CambiarPasswordRequestDTO dto)
```
- No utiliza `trackedFields` (evita que el interceptor AOP serialice el objeto DTO que contiene `passwordActual` y `passwordNueva`).
- No utiliza `metadata` con expresiones SpEL que extraigan valores de parámetros.
- Registra únicamente la acción efectuada y el ID simbólico `'self'`.

### 2. Filtros y trazas de observabilidad
`LoggingSafetyConfiguration` aplica reglas estrictas sobre los filtros de logging HTTP:
- Cabecera `Authorization`: enmascarada / suprimida.
- Cabecera `Cookie` (que transporta `access_token` y `XSRF-TOKEN`): enmascarada.
- Campos de payload coincidentes con patrones `*password*`, `*token*`, `*hash*`: sanitizados antes de emitirse a los appenders.

---

## Instrucciones de reproducibilidad

Para ejecutar la verificación automatizada en cualquier entorno:

```bash
chmod +x ./docs/testing/ops/run-scrum-292-password-csrf-qa.sh
./docs/testing/ops/run-scrum-292-password-csrf-qa.sh
```

El script invoca Maven Wrapper ejecutando las 31 pruebas unitarias y de integración de la política de sesión, CSRF y sanitización de credenciales.
