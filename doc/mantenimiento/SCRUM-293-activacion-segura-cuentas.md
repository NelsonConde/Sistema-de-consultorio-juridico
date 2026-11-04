# SCRUM-293 — Activación segura de cuentas

## Alcance

SCRUM-293 elimina la contraseña inicial derivada del documento de identidad e incorpora un estado explícito de activación de cuenta.

La migración asociada es:

`V20261102_02__add_account_activation.sql`

Los cambios estructurales son:

- `usuario_sistema.password_hash` pasa a admitir `NULL` únicamente durante el estado pendiente de activación.
- Se agrega `usuario_sistema.estado_activacion` con valores `PENDIENTE` y `ACTIVA`.
- Las cuentas existentes al momento de aplicar la migración se conservan como `ACTIVA`; no se modifica su contraseña.
- Se crea `account_activation_token`.
- El token real de activación no se persiste; únicamente se almacena su hash SHA-256.
- Un índice único parcial garantiza como máximo una invitación no usada y no revocada por usuario.

## Compatibilidad

La migración mantiene las cuentas existentes sin cambios funcionales:

- las cuentas previamente existentes quedan con `estado_activacion = 'ACTIVA'`;
- sus valores de `password_hash` permanecen sin modificación;
- no se obliga a restablecer contraseñas existentes;
- las cuentas creadas por el nuevo flujo se almacenan inicialmente como `PENDIENTE` y con `password_hash = NULL`.

## Validación previa al despliegue

Antes de aplicar la migración se debe disponer de un respaldo verificable de PostgreSQL conforme al procedimiento general de despliegue y recuperación del proyecto.

El archivo `migrations.sha256` debe contener el SHA-256 revisado de la migración y no se debe modificar una migración que ya haya sido aplicada en un ambiente compartido.

La migración debe validarse mediante Flyway y mediante las pruebas PostgreSQL automatizadas del proyecto.

## Estrategia de rollback

Las migraciones versionadas aplicadas no se editan ni se eliminan del historial de Flyway.

Ante un fallo durante la aplicación de `V20261102_02`, PostgreSQL y Flyway deben revertir la transacción de migración y el despliegue no debe continuar.

Si la migración fue aplicada correctamente pero posteriormente se requiere volver a la versión anterior del sistema, el procedimiento preferido es:

1. Detener el despliegue de la versión que implementa SCRUM-293.
2. Restaurar el respaldo PostgreSQL tomado inmediatamente antes de aplicar `V20261102_02`.
3. Verificar la integridad del esquema restaurado y del historial `flyway_schema_history`.
4. Desplegar nuevamente la versión anterior del backend.
5. Ejecutar las verificaciones de autenticación y acceso correspondientes antes de restablecer el servicio.

No debe ejecutarse un rollback estructural destructivo si existen cuentas nuevas con `password_hash IS NULL`, porque la versión anterior requiere una contraseña no nula y no conoce el estado `PENDIENTE`.

## Reversión estructural excepcional

Una reversión manual del esquema solo puede considerarse cuando se haya comprobado previamente que:

`SELECT COUNT(*) FROM "DB_consultorioJuridico".usuario_sistema WHERE password_hash IS NULL;`

devuelve `0`.

Bajo esa precondición, la reversión estructural equivalente consistiría en eliminar `account_activation_token`, eliminar la restricción y columna `estado_activacion` y restaurar `password_hash` como `NOT NULL`.

Este procedimiento no sustituye el respaldo y no debe utilizarse para corregir una migración ya aplicada en producción. La recuperación mediante respaldo es el mecanismo preferido cuando puedan existir datos creados por el nuevo flujo.

## Evidencia de validación

La implementación incorpora pruebas para verificar:

- migración desde una base vacía;
- adopción del baseline histórico y aplicación posterior de las migraciones;
- preservación de cuentas existentes;
- creación del estado de activación y de la tabla de tokens;
- rechazo de versiones duplicadas o fuera de orden;
- rollback transaccional ante una migración sintética fallida;
- activación de una cuenta pendiente;
- expiración, revocación y uso único del token;
- resistencia frente a consumo concurrente;
- ausencia del token y contraseña en los registros de auditoría.

En el entorno local de desarrollo la compilación y las pruebas unitarias finalizaron correctamente. Las pruebas basadas en PostgreSQL/Testcontainers requieren un motor Docker disponible y quedan preparadas para ejecución en CI cuando el entorno local no disponga de dicho motor.