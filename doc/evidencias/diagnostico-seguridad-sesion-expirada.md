# Diagnóstico de seguridad por expiración de sesión durante la creación de usuarios

## Contexto

Se investigó la sospecha de que un formulario de creación de usuarios pudiera permanecer abierto y completar el alta después del vencimiento del JWT administrativo. El análisis estático tomó como base el commit `e09b54f` y las pruebas automatizadas quedaron incorporadas en el commit `b9f18f2`.

El caso dinámico principal fue:

```text
POST /api/estudiantes
```

Este endpoint permite observar conjuntamente la creación del perfil de estudiante, la cuenta en `usuario_sistema` y la relación entre ambas entidades.

## Método

`EstudianteSesionExpiradaPostgreSqlTest` usa:

- contexto completo de Spring Boot;
- `MockMvc` atravesando la cadena real de Spring Security;
- JWT reales generados o firmados con la configuración exclusiva de prueba;
- CSRF obtenido mediante `GET /api/auth/csrf`;
- PostgreSQL 16 efímero mediante Testcontainers;
- datos sintéticos exclusivos de cada ejecución;
- consultas directas a PostgreSQL para comprobar perfiles, cuentas, relaciones y auditorías.

La prueba verifica antes de preparar el escenario que el datasource activo corresponde al Testcontainer y que el catálogo es `legal_cases_test`. No emplea `@WithMockUser` para sustituir la validación del JWT.

## Resultados

| Caso | Condición | HTTP observado | Persistencia observada | Resultado |
|---|---|---:|---|---|
| CP-01 | Administrador activo, JWT vigente y CSRF válido | `201 Created` | Un estudiante, una cuenta y su relación; auditoría de creación exitosa | Aprobado |
| CP-02 | Sin cookie `access_token`, con CSRF válido | `401 Unauthorized` | Sin estudiante, cuenta ni relación | Aprobado |
| CP-03 | JWT real correctamente firmado pero expirado, con CSRF válido | `401 Unauthorized` | Sin estudiante, cuenta ni relación | Aprobado |
| CP-04 | JWT inválido, con CSRF válido | `401 Unauthorized` | Sin estudiante, cuenta ni relación | Aprobado |
| CP-05 | JWT vigente, sin CSRF | `403 Forbidden` | Sin estudiante, cuenta ni relación | Aprobado |
| CP-06 | Sesión inicialmente válida; envío HTTP después de vencer el JWT, con CSRF válido | `401 Unauthorized` | Sin estudiante, cuenta ni relación | Aprobado |
| CP-07 | Comprobación agregada de efectos persistentes y auditoría | No aplica | Solo CP-01 produjo datos y auditoría de creación exitosa; CP-02 a CP-06 conservaron únicamente evidencia legítima de rechazo | Aprobado |

La ejecución local produjo:

- 7 pruebas nuevas aprobadas, sin fallos, errores ni omisiones;
- 13 pruebas existentes relacionadas aprobadas, sin fallos, errores ni omisiones.

No se atribuyen estos resultados a CI ni a una ejecución completa de la suite.

## Aislamiento

PostgreSQL se ejecutó exclusivamente en un contenedor efímero administrado por Testcontainers. Las propiedades del datasource fueron reemplazadas dinámicamente con la URL y las credenciales temporales del contenedor, Hibernate creó y eliminó el esquema de prueba, y no quedó un contenedor PostgreSQL de esta ejecución al finalizar.

No se utilizaron cuentas, correos, documentos, cookies, tokens ni credenciales reales. Tampoco se invocaron integraciones externas para correo o almacenamiento.

## Resultado del diagnóstico

La vulnerabilidad no fue reproducida en el alcance probado. Cuando el JWT ya estaba expirado, Spring Security no estableció un contexto autenticado y `POST /api/estudiantes` respondió `401`; no se creó un perfil, una cuenta ni su relación. La ausencia de CSRF produjo `403` antes de invocar el controller y tampoco generó persistencia.

Este resultado no demuestra que el sistema esté libre de toda vulnerabilidad ni extiende la conclusión a flujos no ejecutados.

## Limitaciones

- No se realizó una prueba E2E visual en navegador.
- No se probaron todos los endpoints capaces de crear perfiles o cuentas.
- No se ejecutó localmente la suite completa.
- La secuencia de formulario abierto se reprodujo a nivel HTTP con `MockMvc`, no mediante interacción visual con el frontend.

## Trazabilidad técnica

El cambio de pruebas y esta documentación se relacionan con el PR #37, abierto hacia `develop`. La referencia identifica el conjunto técnico revisable; este documento no afirma aprobaciones, resultados de CI ni integración del PR.
