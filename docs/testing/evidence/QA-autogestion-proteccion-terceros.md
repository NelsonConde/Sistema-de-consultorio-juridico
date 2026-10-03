# Evidencia de Pruebas QA — Autogestión y Protección contra Edición de Terceros

- **Solicitud / Épica:** CC-OPT-003 — Autogestión de perfil, contacto, contraseña e invitación
- **Ticket Jira:** QA — Probar autogestión y protección contra edición de terceros
- **Rama:** `test/cc-opt-003-profile-security`
- **Fecha de ejecución:** 03/10/2026
- **Responsable de QA:** Equipo de Control de Calidad y Pruebas

---

## 1. Alcance y Objetivos

Verificar formalmente la seguridad, integridad y robustez de los endpoints de autogestión de perfil (`/api/mi-perfil` y `/api/mi-perfil/contacto`) y la interfaz de usuario asociada (`/mi-perfil`).

Se garantiza que:
1. Cualquier usuario autenticado en cualquiera de los cinco perfiles del sistema puede consultar su información y actualizar exclusivamente sus datos de contacto (`email` y `telefono`).
2. Los atributos protegidos (`id`, `username`, `rol`, `tipoPerfil`, `documento`, `codigo`, `sede`, `activo`, `password`) son inmutables ante cualquier intento de alteración o manipulación de payloads.
3. No es posible acceder ni modificar el perfil de terceros bajo ninguna circunstancia.
4. Las sesiones ausentes o expiradas son rechazadas inmediatamente con código HTTP 401.
5. Los eventos probatorios de auditoría se registran adecuadamente tanto en actualizaciones exitosas como en accesos denegados.
6. La suite E2E y el pipeline de integración continua (CI) se encuentran completamente en verde.

---

## 2. Matriz de Pruebas y Resultados

| Caso / Escenario | Descripción | Componente | Resultado | Evidencia |
| :--- | :--- | :--- | :---: | :--- |
| **MP-01: Cinco perfiles (Lectura)** | Consulta de perfil propio para `ADMINISTRATIVO`, `ASESOR`, `MONITOR`, `ESTUDIANTE` y `CONCILIADOR`. | Backend / Frontend | **PASSED** | `MiPerfilServiceTest.obtenerMiPerfil_cincoPerfiles`<br>`MiPerfilControllerTest.obtenerMiPerfil_exitoso`<br>E2E: 5 perfiles en `mi-perfil.spec.js` |
| **MP-02: Cinco perfiles (Actualización)** | Actualización válida de correo y teléfono para los 5 perfiles. | Backend / Frontend | **PASSED** | `MiPerfilServiceTest.actualizarContacto_cincoPerfiles`<br>`MiPerfilControllerTest.actualizarContacto_validoRetorna200`<br>E2E en `mi-perfil.spec.js` |
| **MP-03: Email inválido** | Rechazo de emails vacíos, sin formato (`sin-arroba`, `correo@`, `@dominio`), o superiores a 120 caracteres. | Backend (Validation) / Frontend | **PASSED** | `MiPerfilControllerTest.actualizarContacto_emailInvalidoRetorna400`<br>`MiPerfilControllerTest.actualizarContacto_emailMayor120Retorna400`<br>E2E: `Validación de formulario` |
| **MP-04: Teléfono inválido** | Rechazo de teléfonos con letras (`abc`), formato inválido o longitud inferior a 7 caracteres. | Backend (Validation) / Frontend | **PASSED** | `MiPerfilControllerTest.actualizarContacto_telefonoInvalidoRetorna400`<br>E2E: `Validación de formulario` |
| **MP-05: Contacto duplicado** | Rechazo controlado cuando email o teléfono ya existen en otro usuario (restricción unique). No se filtran trazas SQL. | Backend (`BusinessException`) | **PASSED** | `MiPerfilServiceTest.actualizarContacto_duplicadoLanzaBusinessException`<br>`MiPerfilControllerTest.actualizarContacto_duplicadoRetorna400` |
| **MP-06: Protección Mass Assignment** | Envío de payload JSON con campos protegidos (`id`, `usuarioSistemaId`, `rol`, `tipoPerfil`, `documento`, `codigo`, `sede`, `activo`, `password`). Ningún campo protegido se altera. | Backend (`ActualizarContactoDTO`) / Frontend | **PASSED** | `MiPerfilControllerTest.actualizarContacto_massAssignmentProtegido`<br>E2E: `Protección de atributos: los campos protegidos no tienen inputs editables` |
| **MP-07: Sesión ausente** | Petición sin cookies ni header JWT a `/api/mi-perfil` o `/api/mi-perfil/contacto` retorna 401 Unauthorized y audita denegación. | Backend (`SecurityExceptionHandler`) | **PASSED** | `MiPerfilSecurityTest.peticionSinSesionRetorna401YAudita` |
| **MP-08: Sesión expirada** | Petición con JWT expirado es interceptada por el filtro, limpiando el contexto y retornando 401 Unauthorized con evento de denegación. | Backend (`JwtAuthenticationFilter`) | **PASSED** | `MiPerfilSecurityTest.peticionConTokenExpiradoLimpiaContexto` |
| **MP-09: Intento sobre tercero** | No existe endpoint que reciba ID de tercero (`/api/mi-perfil/{id}` retorna 404). Parámetros o IDs en body son ignorados, resolviendo siempre el usuario de sesión. | Backend / Spring Security | **PASSED** | `MiPerfilControllerTest.intentoSobreTerceroEnRutaRetorna404` |
| **MP-10: Auditoría de eventos** | Actualización de contacto genera evento de auditoría (`ACTUALIZAR_CONTACTO_PROPIO`, entidad `UsuarioSistema`, actor, timestamp, outcome `SUCCESS`). | Backend (`@Auditable`, `AuditAspect`) | **PASSED** | `MiPerfilServiceTest.actualizarContacto_tieneAnotacionAuditable`<br>`MiPerfilServiceTest.actualizarContacto_interceptadoPorAuditAspectRegistraExito` |

---

## 3. Verificación de Criterios de Aceptación

| Criterio de Aceptación | Estado | Detalle de Cumplimiento |
| :--- | :---: | :--- |
| **Solo contacto cambia** | **CUMPLIDO** | Únicamente `email` y `telefono` se delegan al resolver y persisten en base de datos. |
| **Ningún payload altera atributos protegidos** | **CUMPLIDO** | `ActualizarContactoDTO` desconoce atributos como rol, activo, documento o código; inyecciones en JSON son descartadas sin efecto colateral. |
| **No se filtra información ajena** | **CUMPLIDO** | La respuesta `MiPerfilDTO` expone solo información propia del actor y omite contraseñas, hashes, tokens y referencias de base de datos. |
| **Eventos de auditoría correctos** | **CUMPLIDO** | `@Auditable(action = "ACTUALIZAR_CONTACTO_PROPIO", entityName = "UsuarioSistema", entityId = "#result.username")` audita cada modificación; accesos sin autenticación son auditados por `SecurityExceptionHandler`. |
| **E2E completo y CI verde** | **CUMPLIDO** | Playwright ejecuta 7/7 casos exitosos (5 perfiles, validación de formulario y verificación de atributos inmutables); backend ejecuta 31/31 pruebas unitarias/integración satisfactorias; build y lint pasan sin errores. |

---

## 4. Ejecución de Pruebas Automatizadas

### 4.1 Backend (Pruebas Unitarias, de Integración y Seguridad)

Comando ejecutado:
```bash
cd backend/app && ./mvnw test -Dtest="MiPerfil*Test"
```

Salida del comando:
```text
[INFO] Running co.edu.ufps.legal_cases.security.controller.account.MiPerfilControllerTest
[INFO] Tests run: 16, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 5.479 s -- in co.edu.ufps.legal_cases.security.controller.account.MiPerfilControllerTest
[INFO] Running co.edu.ufps.legal_cases.security.controller.account.MiPerfilSecurityTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.602 s -- in co.edu.ufps.legal_cases.security.controller.account.MiPerfilSecurityTest
[INFO] Running co.edu.ufps.legal_cases.security.service.account.MiPerfilServiceTest
[INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.440 s -- in co.edu.ufps.legal_cases.security.service.account.MiPerfilServiceTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 31, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

### 4.2 Frontend E2E (Playwright)

Comando ejecutado:
```bash
cd frontend && npx playwright test e2e/mi-perfil.spec.js --project=chromium
```

Salida del comando:
```text
Running 7 tests using 4 workers

[1/7] Perfil ADMINISTRATIVO: lectura y actualización autorizada de contacto
[2/7] Perfil ASESOR: lectura y actualización autorizada de contacto
[3/7] Perfil MONITOR: lectura y actualización autorizada de contacto
[4/7] Perfil ESTUDIANTE: lectura y actualización autorizada de contacto
[5/7] Perfil CONCILIADOR: lectura y actualización autorizada de contacto
[6/7] Protección de atributos: los campos protegidos no tienen inputs editables
[7/7] Validación de formulario: bloquea correo y teléfono inválidos antes de enviar

7 passed (21.1s)
```

### 4.3 Verificación de Compilación y Calidad de Código Frontend

Comandos ejecutados:
```bash
cd frontend && npm run build && npm run lint
```

Resultado:
- `next build`: Generación de bundle de producción optimizado en 21.0s sin errores.
- `biome lint`: Análisis estático completado satisfactoriamente (0 errores).

---

## 5. Conclusión y Recomendación

La funcionalidad de autogestión de perfil y protección contra edición de terceros cumple el 100% de la matriz de verificación y de los criterios de aceptación del ticket de QA. La solución es apta para ser promovida mediante Pull Request a la rama permanente `develop` conforme al procedimiento formal de gestión de ramas.
