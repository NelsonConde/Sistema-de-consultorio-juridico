# Diagnóstico de reactivación de perfiles operativos

## Estado histórico del diagnóstico

Este documento conserva el diagnóstico técnico realizado sobre la versión indicada en la sección siguiente. Las pruebas P01 a P12 demuestran el comportamiento que existía en esa versión, pero un resultado PASS no significa que ese comportamiento fuera funcionalmente correcto.

En el momento del diagnóstico, `RF-P01` a `RF-P06` eran una propuesta preliminar y todavía no estaban implementadas. Su aprobación e implementación posteriores se registran al final sin alterar la evidencia original.

## Contexto

Se investigó el reporte según el cual, después de reactivar un perfil estudiante histórico, `GET /api/auth/me` seguía mostrando el perfil ASESOR y sus permisos. No se asumió que los perfiles fueran mutuamente excluyentes ni que una reactivación debiera cambiar automáticamente el perfil actual.

El análisis distinguió dos operaciones:

- cambiar el estado lógico de un estudiante;
- cambiar explícitamente el tipo de perfil operativo de una cuenta.

La prueba dinámica determinó sus efectos reales sobre perfiles, cuenta, rol, permisos, autenticación, autorización e historial.

## Versión analizada

| Dato | Valor |
|---|---|
| Rama | `develop` |
| Commit | `aca0b1e3b63b79eed112e231203a2bf1603e222f` |
| Base de datos | PostgreSQL 16 mediante Testcontainers |
| Ejecución | Local y focalizada; no corresponde a CI ni a la suite completa |

## Endpoints involucrados

| Método y ruta | Uso en el diagnóstico |
|---|---|
| `PATCH /api/estudiantes/{id}/activo?activo=true|false` | Activación o desactivación directa del estudiante. |
| `PATCH /api/asesores/{id}/activo?activo=true|false` | Activación o desactivación directa del asesor. |
| `PATCH /api/usuarios-sistema/{id}/perfil/estudiante` | Transición explícita hacia ESTUDIANTE. |
| `PATCH /api/usuarios-sistema/{id}/activo?activo=true|false` | Control directo del estado de la cuenta, considerado al analizar estados alcanzables. |
| `GET /api/auth/me` | Resolución del usuario, perfil actual, rol y permisos. |
| `GET /api/asesores/{id}` | Autorización representativa del perfil asesor. |
| `GET /api/estudiantes/{id}` | Permiso y alcance sobre estudiantes. |
| `GET /api/usuarios-sistema` | Rechazo por falta de permiso administrativo. |
| `GET /api/auth/csrf` | Obtención del CSRF para solicitudes mutables. |

## Identidad y relaciones

`UsuarioSistema` representa la cuenta de acceso y conserva `activo`, `tipoPerfilActual`, rol y permisos. `Estudiante` y `Asesor` representan perfiles operativos y cada tabla contiene `usuario_sistema_id`. Una cuenta puede conservar registros históricos en ambas tablas.

La identidad compartida se comprobó comparando esas claves foráneas; no se dedujo de nombres, correos o documentos. Debe distinguirse de `Estudiante.asesor`, que representa la asignación operativa o académica del estudiante y no demuestra identidad de acceso compartida.

También se crearon perfiles sintéticos semejantes enlazados a cuentas diferentes para comprobar el aislamiento entre identidades.

## Método

`PerfilEstudianteReactivacionPostgreSqlTest` usa:

- contexto completo de Spring Boot;
- `MockMvc` sobre la cadena real de Spring Security;
- JWT generado por `JwtService` en la cookie `access_token`;
- CSRF obtenido mediante `GET /api/auth/csrf`;
- JPA y consultas directas de comprobación;
- PostgreSQL 16 efímero mediante Testcontainers;
- datos sintéticos independientes por caso;
- verificación de claves foráneas, estados, `tipo_perfil_actual`, `rol_id` e historial.

No se utilizó `@WithMockUser`. Los estados especiales de seguridad se prepararon como fixtures controlados y se diferenciaron de las transiciones ejecutadas mediante API.

## Matriz P01-P12

| Caso | Escenario | HTTP observado | Persistencia y autorización observadas | Resultado técnico |
|---|---|---|---|---|
| P01 | Comparar perfiles de una cuenta compartida con perfiles de cuentas distintas. | No aplica. | Asesor y estudiante compartidos tuvieron el mismo `usuario_sistema_id`; los independientes tuvieron cuentas diferentes. | PASS: relación comprobada por FK. |
| P02 | Cuenta activa, ASESOR actual, rol asesor, asesor activo y estudiante inactivo; consultar sesión. | `/api/auth/me` `200`. | Devolvió ASESOR, id del asesor, rol y permisos de asesor. | PASS: estado inicial caracterizado. |
| P03 | Reactivar directamente el estudiante histórico con administrador autorizado. | `200`. | Cuenta activa; asesor y estudiante activos; tipo y rol ASESOR; historial de cambio de perfil `0`. | PASS: coexistencia reproducida. |
| P04 | Consultar sesión después de P03. | Reactivación `200`; sesión `200`. | Continuó ASESOR con permisos del rol asesor. | PASS: conservación reproducida. |
| P05 | Invocar endpoints protegidos después de la reactivación. | Asesor `200`; estudiante `403`; usuarios `403`. | Acceso de asesor conservado; estudiante rechazado por alcance; usuarios rechazado por falta de permiso. | PASS: permiso y alcance diferenciados. |
| P06 | Cambio explícito ASESOR a ESTUDIANTE. | Cambio `200`; sesión `200`. | Misma cuenta; asesor inactivo; estudiante activo; tipo, rol y permisos de estudiante; historial `1`. | PASS: transición coherente. |
| P07 | Con ambos perfiles activos y ASESOR actual, desactivar el estudiante no actual. | Activación `200`; desactivación `200`; sesión `401`. | Cuenta inactiva; asesor activo; estudiante inactivo; tipo y rol ASESOR. | PASS: pérdida de acceso reproducida. |
| P08 | Con ambos activos y ASESOR actual, desactivar el asesor sin consultas operativas. | Activación `200`; desactivación `200`; sesión `401`. | Cuenta y asesor inactivos; estudiante todavía activo. | PASS: el perfil alterno no mantuvo acceso. |
| P09 | Cuenta inactiva, ASESOR actual y activo, estudiante histórico inactivo; reactivar estudiante. | Sesión inicial `401`; reactivación `200`; sesión posterior `200`. | Cuenta habilitada; ambos perfiles activos; acceso recuperado como ASESOR. | PASS: rehabilitación indirecta reproducida. |
| P10 | Cuenta activa, estudiante activo, asesor inactivo y ASESOR actual. | Sesión `401`. | El estudiante activo no sustituyó al perfil actual inactivo. | PASS: resolución exclusiva confirmada. |
| P11 | Cambiar explícitamente a ESTUDIANTE con consulta operativa del asesor. | `400`. | Rollback: asesor activo, estudiante inactivo, tipo y rol ASESOR, historial `0`. | PASS: restricción y atomicidad confirmadas. |
| P12 | Reactivar el estudiante compartido manteniendo otra cuenta semejante. | `200`. | La otra cuenta y sus perfiles conservaron sus estados. | PASS: aislamiento confirmado. |

PASS indica que se obtuvo reproduciblemente el resultado esperado para caracterizar la implementación actual. No expresa conformidad con una política de negocio todavía no formalizada.

## Evidencia de ejecución

| Grupo | Pruebas | Fallos | Errores | Omitidas |
|---|---:|---:|---:|---:|
| `PerfilEstudianteReactivacionPostgreSqlTest` | 12 | 0 | 0 | 0 |
| Unitarias relacionadas con estado, resolución y cambio de perfil | 24 | 0 | 0 | 0 |
| `EstudianteSesionExpiradaPostgreSqlTest` | 7 | 0 | 0 | 0 |
| **Total local** | **43** | **0** | **0** | **0** |

Comando focalizado comprobado desde `backend/app`:

```powershell
.\mvnw.cmd '-Dtest=PerfilEstudianteReactivacionPostgreSqlTest' test
```

Resultado de la nueva clase:

```text
Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Las cifras proceden de ejecuciones locales separadas, no de CI ni de una ejecución completa del repositorio.

## Aislamiento

Antes de insertar datos se comprobó que:

- el contenedor estaba ejecutándose;
- la URL JDBC activa coincidía exactamente con la entregada por Testcontainers;
- el catálogo era `legal_cases_test`;
- el producto era PostgreSQL;
- la versión mayor era 16.

El datasource se configuró dinámicamente, Hibernate creó y eliminó el esquema de prueba y al finalizar no quedaron contenedores activos. No se usaron datos, credenciales, servicios de correo ni bases compartidas.

## Comportamientos respaldados por reglas existentes

La documentación y el código vigentes respaldan que:

- cambiar el estado desde un perfil sincroniza `UsuarioSistema.activo`;
- `/api/auth/me` resuelve el perfil indicado por `tipoPerfilActual`;
- los permisos proceden del rol y el alcance se valida adicionalmente por perfil;
- el cambio explícito desactiva el perfil anterior y crea, actualiza o reactiva el destino;
- el cambio explícito conserva la cuenta, actualiza rol y perfil actual, y registra historial;
- un asesor con consultas operativas no puede desactivarse durante la transición;
- un error impide confirmar cambios parciales.

Estas reglas explican el resultado técnico, pero no determinan si debe activarse directamente un perfil histórico diferente del actual.

## Riesgos reproducidos

- **Coexistencia:** ESTUDIANTE y ASESOR pueden quedar activos para el mismo usuario.
- **Pérdida de acceso:** desactivar el estudiante no actual puede desactivar toda la cuenta aunque el asesor actual siga activo.
- **Rehabilitación indirecta:** reactivar el estudiante histórico puede habilitar la cuenta y recuperar acceso con el perfil y rol ASESOR.
- **Dependencia del perfil actual:** otro perfil activo no sustituye a un perfil actual inactivo.
- **Ausencia de historial de transición:** la reactivación directa no genera `UsuarioCambioPerfilHistorial`.

No se observó mezcla automática de permisos ni evidencia de escalada de privilegios. El riesgo comprobado consiste en que una operación sobre un perfil no actual puede modificar la disponibilidad de una cuenta que conserva otro perfil y rol actuales.

## Vacíos de requisitos

No se encontró una regla vigente que determine:

- si una cuenta puede tener varios perfiles operativos activos;
- si activar un perfil histórico debe permitirse, rechazarse o convertirse en cambio explícito;
- qué perfil gobierna `UsuarioSistema.activo` cuando existen varios registros históricos;
- si desactivar un perfil no actual puede suspender la cuenta;
- si reactivar un perfil no actual puede rehabilitar una cuenta suspendida;
- qué código HTTP corresponde a una activación incompatible;
- si una activación directa debe registrar una transición.

## Propuesta funcional pendiente

| Regla | Propuesta, todavía no implementada |
|---|---|
| RF-P01 — Un único perfil operativo activo | Cada cuenta debe tener un único perfil operativo activo; los históricos se conservan inactivos. |
| RF-P02 — Sin cambios de rol implícitos | Activar o reactivar no debe modificar automáticamente `tipoPerfilActual`, rol ni permisos. |
| RF-P03 — Rechazo de activación histórica | Activar directamente un perfil histórico diferente del actual debe responder `409 Conflict`, no modificar datos e indicar que corresponde usar el cambio explícito. |
| RF-P04 — Protección al desactivar | No debe permitirse que desactivar un perfil histórico diferente del actual suspenda la cuenta o altere el perfil vigente; la operación incompatible debe rechazarse sin efectos parciales. |
| RF-P05 — Habilitación explícita de cuenta | Un perfil histórico no puede rehabilitar indirectamente una cuenta suspendida. Cuenta, rol y perfil actual deben estar habilitados para autenticar. |
| RF-P06 — Transiciones explícitas | Los cambios de tipo deben usar el mecanismo existente, conservando cuenta, validaciones, historial y transaccionalidad. |

Las reglas propuestas cambian expectativas observadas en P03, P07 y P09. Cuando se implementen, esas caracterizaciones deberán convertirse en regresiones que esperen rechazo y ausencia de mutaciones.

## Consecuencias técnicas esperadas si se aprueba la propuesta

Deberá evaluarse:

- validar en los servicios de comando si el perfil modificado corresponde a `tipoPerfilActual`;
- retornar `409 Conflict` con un contrato de error estable;
- evitar efectos parciales sobre `UsuarioSistema.activo`;
- separar explícitamente el ciclo de vida de cuenta y perfiles históricos;
- conservar el cambio explícito como única transición entre tipos;
- revisar permisos para activar perfiles, habilitar cuentas y cambiar perfil;
- diferenciar en auditoría rechazos, estados y transiciones;
- actualizar pruebas de controller, service, seguridad e integración;
- agregar concurrencia entre activación y cambio de perfil;
- evaluar integridad en base de datos si se decide reforzar allí la exclusividad.

Este listado es un mapa de impacto, no una corrección aprobada o implementada.

## Revisión previa de datos existentes

Antes de aplicar exclusividad debe identificarse cualquier cuenta con más de un perfil activo. La revisión debería:

1. agrupar perfiles activos por `usuario_sistema_id` entre todas las tablas de perfil;
2. contrastarlos con `tipo_perfil_actual`, `rol_id` y `UsuarioSistema.activo`;
3. identificar consultas operativas u otras relaciones que impidan desactivar perfiles;
4. definir el tratamiento funcional antes de modificar datos;
5. conservar respaldo y trazabilidad de toda regularización.

No debe desactivarse automáticamente un perfil solo por introducir la nueva política. Elegir el perfil vigente y resolver responsabilidades operativas exige una decisión funcional y, si corresponde, una migración aprobada.

## Limitaciones

- No se ejecutó la suite completa.
- No hubo prueba visual E2E en navegador.
- La matriz se concentró en ESTUDIANTE y ASESOR.
- No se probó concurrencia entre activación y cambio explícito.
- No se evaluó ni implementó una restricción de exclusividad en base de datos.
- No se consultaron datos de producción o entornos compartidos.
- La evidencia no acredita aprobación funcional, revisión de seguridad formal ni resultados de CI.
- Las pruebas caracterizan el estado actual; algunas expectativas deberán cambiar al implementar la propuesta.

## Estado de la decisión

El comportamiento actual quedó reproducido y documentado. `RF-P01` a `RF-P06` permanecen pendientes de formalización funcional y de implementación.

Antes de corregir debe definirse formalmente la autoridad sobre la cuenta, el tratamiento de perfiles históricos, la semántica de activación y desactivación, el contrato `409`, la estrategia para datos existentes y los criterios de aceptación de la nueva política.

## Seguimiento posterior a la evidencia original

Las frases anteriores describen el estado de la decisión al cerrar el diagnóstico sobre `develop` y el commit documentado. Posteriormente se comunicó la aprobación funcional de `RF-P01` a `RF-P06` y una séptima regla: el perfil actual tampoco puede desactivarse directamente; para impedir el acceso se suspende la cuenta y para cambiar de cargo se usa el cambio explícito de perfil.

La implementación posterior aplica estas reglas a ESTUDIANTE, ASESOR, MONITOR, CONCILIADOR y ADMINISTRATIVO:

1. una cuenta conserva un único perfil operativo activo y mantiene los anteriores como históricos inactivos;
2. las operaciones directas de perfil no cambian rol, permisos, `tipoPerfilActual` ni `UsuarioSistema.activo`;
3. activar un perfil histórico se rechaza sin modificaciones;
4. desactivar o eliminar lógicamente un perfil histórico se rechaza sin afectar la cuenta o el perfil vigente;
5. la habilitación o suspensión de la cuenta se administra por separado;
6. las transiciones usan el cambio explícito, con validaciones, transacción e historial;
7. desactivar o eliminar lógicamente el perfil actual se rechaza.

El rechazo funcional responde `409 Conflict` con `detalles.codigo = CONFLICTO_OPERACION_PERFIL`. Las operaciones directas y los cambios explícitos se serializan mediante bloqueo pesimista de `UsuarioSistema`; el cambio explícito valida la exclusividad antes de modificar perfiles, rol, tipo actual e historial. Los estados preexistentes con varios perfiles activos se rechazan y no se sanean automáticamente.

La regresión de perfiles contiene 50 escenarios de integración: P45 a P49 usan transacciones concurrentes coordinadas y P50 verifica el rechazo de múltiples perfiles activos preexistentes. En validaciones posteriores se reportaron 50/50 aprobadas para esa clase y 116/116 para una selección focalizada de backend. La ejecución ampliada posterior contabilizó 529 pruebas: 526 aprobadas, cero fallos de aserción y tres errores de entorno relacionados con variables de almacenamiento ausentes y LocalStack no disponible. Las pruebas operacionales y `LoggingSafetyConfigurationTest` se aprobaron luego en ejecuciones aisladas; esta última usó configuración sintética y no valida la configuración privada. Estos resultados no equivalen a una única suite Maven completa aprobada.

En frontend se aprobaron 26 de 26 pruebas Playwright y el build con Node.js 22 y Next.js 16.3.8. En una ejecución previa, el lint global se reportó como no satisfactorio y no se presentó como validación aprobada. Una validación posterior con Node.js 22 y npm 10.9.4 confirmó que `npm run lint` finaliza con código 0, sin errores, con 186 advertencias preexistentes y 5 diagnósticos informativos. La comparación con `HEAD` no identificó diagnósticos nuevos; por tanto, el lint no bloquea el workflow actual de CI. Esta comprobación aislada no equivale a una ejecución integral aprobada de Maven o de CI.

Esta actualización técnica no acredita que la documentación oficial externa del responsable funcional haya sido actualizada ni representa un resultado de CI. El detalle vigente y las limitaciones se mantienen en `doc/backend/perfiles.md`, `doc/api/perfiles.md`, `doc/04-permisos-roles-alcance.md`, `doc/decisiones/estrategia-perfiles.md` y `doc/06-pruebas.md`.
