# SCRUM-292 — QA de cambio de contraseña, sesión, CSRF y exposición de secretos

## Identificación y control de alcance

- **Jira:** [SCRUM-292](https://gestion-casos-juridicos.atlassian.net/browse/SCRUM-292).
- **PR:** [#83](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/83), rama `test/scrum-292-password-change-csrf-qa`.
- **Origen:** SCRUM-290 (política backend), SCRUM-365 (QA frontend; su PR #79 debe verificarse independientemente).
- **Revisión original:** 03/11/2026; corrección de trazabilidad: 04/11/2026.
- **Base de inspección original:** `a179068e9af2ef0546e78b22713f0740984a7126` (commit anterior a esta corrección).
- **Tipo:** pruebas y evidencias, sin cambios funcionales, de base de datos, migración, configuración de seguridad ni credenciales.
- **Revisión independiente:** pendiente de revalidación de la PR y de la evidencia de CI del nuevo commit.

## Criterios de aceptación de Jira y evidencia verificable

La incidencia exige verificar contraseña actual correcta/incorrecta, nueva igual/débil/válida, confirmación distinta, sesión ausente/expirada, CSRF ausente/inválido, credenciales vieja/nueva tras cambio, ausencia de secretos en trazas y continuidad de login y recuperación.

Se distinguen **prueba de comportamiento**, **inspección estática** y **no verificado**; que el comando Maven termine en verde no convierte una inspección estática o un mock en integración de sistema.

| ID | Comprobación | Prueba o fuente concreta | Alcance del resultado |
|---|---|---|---|
| QA-PWD-01 | Contraseña actual correcta | `PasswordChangeCsrfPolicyQaTest.testActualCorrecta_PermiteValidacion` | Validación real del validador BCrypt |
| QA-PWD-02 | Contraseña actual incorrecta | `testActualIncorrecta_RechazaConMensajeSeguro` | Excepción y mensaje del validador |
| QA-PWD-03 | Nueva igual a la actual | `testNuevaIgualAActual_Rechaza` | Rechazo de reutilización |
| QA-PWD-04 | Nueva vacía, corta o de más de 100 caracteres | `testNuevaDebil_Rechaza`, `testNuevaDemasiadoLarga_Rechaza` | Validación de longitud; **no** análisis de fortaleza por diccionario |
| QA-PWD-05 | Nueva válida | `testNuevaValida_PasaValidacion` | Aceptación por validador |
| QA-PWD-06 | Confirmación diferente | `testConfirmacionDistinta_Rechaza` | Validación del DTO de recuperación |
| QA-PWD-07 | Sesión revocada, ausente y JWT expirado | `testSesionRevocada_RechazaCambioPassword`, `testSesionAusente_RechazaSinPersistir`, `testSesionExpirada_RechazaSinPersistir` | Servicio y comprobación de JWT firmado/caducado; sin afirmar respuesta HTTP integrada |
| QA-PWD-08 | CSRF ausente/inválido; rotación tras cambio | `testCsrfPatchAusenteOInvalido_Rechaza`, `testCsrf_InvalidaCookieYEmiteNuevoJwt`; `CsrfFilterSecurityTest` | Filtro CSRF real para PATCH y comportamiento del controlador con dependencia simulada |
| QA-PWD-09 | Fallo no invalida CSRF ni sesión | `testCsrf_FalloNoAlteraSesion` | Controlador con servicio simulado |
| QA-PWD-10 | Actualización de hash y `auth_version` | `testPoliticaSesion_IncrementaAuthVersionYActualizaHash` | Servicio y persistencia simulada; **no** demuestra atomicidad transaccional en PostgreSQL |
| QA-PWD-11 | Auditoría sin campos secretos | `testCeroSecretosEnAuditoria`, `testEventoAuditoriaNoContieneSecretos`; `AuditAspectTest` | Anotación y evento AOP generado con datos sintéticos; no revisión universal de todos los logs |
| QA-PWD-12 | Login posterior al cambio | `testLoginYRecuperacionContinuanFuncionales` | `AuthService.login` y `LoginValidator` reales con repositorio y emisión JWT simulados; clave anterior rechazada, nueva aceptada |
| QA-PWD-13 | Invalidación CSRF en login/logout | `AuthControllerCsrfTest` | Prueba unitaria de controlador; no HTTP de producción |
| QA-PWD-14 | Restablecimiento incrementa versión | `PasswordResetServiceAuthVersionTest` | Prueba de servicio del backend |
| QA-PWD-15 | Configuración de logging | `LoggingSafetyConfigurationTest` | **Solo inspección estática** de propiedades y patrones del código; no demuestra enmascaramiento dinámico de cabeceras o payload |

## Ejecución y resultados

**Ejecución anterior (commit `a179068e`):** [GitHub Actions del commit anterior](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/commit/a179068e9af2ef0546e78b22713f0740984a7126). La ejecución de pruebas fue satisfactoria, pero el informe original sobreinterpretó el alcance de varias aserciones. Ese resultado no se reutiliza como PASS para los casos nuevos.

**Ejecución de esta corrección:** **PENDIENTE DE CI**. Una vez concluida, debe registrarse el commit exacto, la URL del workflow, el número de pruebas ejecutadas y cada fallo si lo hay. No declarar `15 PASS` hasta verificar los métodos y sus resultados.

Comando reproducible desde la raíz:

```bash
bash docs/testing/ops/run-scrum-292-password-csrf-qa.sh
```

La suite invoca `./mvnw` de `backend/app`, sin depender de una instalación absoluta bajo `/tmp`. La evidencia formal es el resultado de GitHub Actions y, para la ejecución focalizada, los reportes Surefire generados por Maven (no se agregan ni publican logs sensibles).

## Alcance de seguridad y limitaciones

- Los valores de usuario, contraseña y JWT de las pruebas son sintéticos; no se utilizan cuentas ni credenciales institucionales.
- Las comprobaciones de CSRF ejercitan el filtro con una solicitud `PATCH`; los tests aislados del controlador por sí solos **no** verifican el filtro.
- La validación de login se realiza a nivel de servicio con validador real, pero repositorio simulado; no constituye una prueba HTTP/BD de extremo a extremo.
- La inspección de `@Auditable` y del evento AOP evita afirmar que todos los logs operativos se hayan examinado. **La ausencia universal de secretos en logs de producción queda NO VERIFICADA.** No se dispone en esta entrega de una captura dinámica de todos los appenders, configuraciones de plataforma y proxies.
- La recuperación posterior se comprueba con el validador y las pruebas de `PasswordResetServiceAuthVersionTest`; no se ejecutó el flujo completo de correo, enlace y navegador en un ambiente desplegado.
- El incremento de `auth_version` se verifica a nivel de servicio; la atomicidad con transacciones reales se debe acreditar con una prueba de integración de PostgreSQL si es exigida por el revisor.
- No se actualizan Jira, Drive ni líneas base. La publicación oficial y la aprobación pertenecen a los roles establecidos en el Manual de Gestión de Ramas y el Plan de Gestión de la Configuración.
