# Pruebas del sistema

## Propósito

El backend incluye pruebas automatizadas para carga del contexto de aplicación, reglas críticas de negocio, validadores y estrategias de seguridad. Estas pruebas sirven como soporte técnico para validar comportamientos funcionales importantes del sistema.

## Ubicación

Las pruebas backend se encuentran en:

```text
backend/app/src/test/java
```

El frontend cuenta con configuración de Playwright en:

```text
frontend/playwright.config.js
```

y scripts asociados en `frontend/package.json`.

## Pruebas backend identificadas

| Prueba | Propósito |
|---|---|
| `ConsultaEstadoServiceTest` | Valida cierre de consulta, pendientes operativos, archivo, desarchivo y bloqueo de operaciones en consultas cerradas o archivadas. |
| `ConsultaCambioEstructuralValidatorTest` | Valida protección de datos estructurales cuando una consulta ya tiene actividad asociada. |
| `ConsultaResponsableOperacionServiceTest` | Valida bloqueo de desactivación de asesor, estudiante o monitor con consultas operativas. |
| `ProcesoValidatorTest` | Valida radicado condicional: permitido en pendiente y obligatorio en estados finales. |
| `SeguimientoValidatorTest` | Valida que `notificarEstudiante=true` requiera estudiante asignado y activo. |
| `SeguimientoRespuestaValidatorTest` | Valida decisiones sobre respuestas, incluyendo observación obligatoria al rechazar. |
| `PerfilEstadoServiceTest` | Valida delegación por Strategy para desactivar perfil actual. |
| `PerfilUsuarioResolverServiceTest` | Valida delegación por Strategy para resolver perfil activo del usuario. |
| `PerfilEstadoHandlerRegistryTest` | Valida registro y resolución de handlers de estado de perfil. |
| `PerfilUsuarioActivoResolverRegistryTest` | Valida registro y resolución de resolvers de perfil activo. |
| `UsuarioSistemaPerfilEstadoServiceTest` | Valida sincronización entre perfil y usuario del sistema. |
| `LegalCasesApplicationTests.contextLoads()` | Verifica que el contexto principal de Spring Boot pueda cargarse durante la ejecución de pruebas. |

`LegalCasesApplicationTests.contextLoads()` utiliza `@SpringBootTest` para validar la carga del contexto de la aplicación. Las demás pruebas listadas se orientan a reglas funcionales, validadores y estrategias del backend.

## Reglas críticas cubiertas

Las pruebas actuales cubren reglas como:

- una consulta no se cierra sin resultado;
- una consulta no se cierra con procesos, seguimientos, respuestas, notificaciones o conciliaciones pendientes;
- una consulta cerrada o archivada no permite operaciones operativas;
- una consulta con actividad asociada protege sus datos estructurales;
- un proceso pendiente puede existir sin radicado;
- un proceso final requiere radicado;
- una respuesta rechazada requiere observación;
- un seguimiento visible para estudiante requiere estudiante asignado y activo;
- asesor, estudiante y monitor no se desactivan si tienen consultas operativas;
- el cambio y resolución de perfil usan estrategias registradas;
- el estado de perfil y usuario del sistema se sincroniza.

## Ejecución de pruebas backend

Desde la carpeta del backend:

```bash
cd backend/app
./mvnw test
```

En Windows:

```powershell
cd backend/app
.\mvnw.cmd test
```

## Ejecución frontend

El frontend define scripts de prueba en `package.json`:

```bash
npm run test
npm run test:ui
npm run test:debug
npm run test:headed
```

## Criterio documental

La documentación de pruebas se limita a pruebas presentes en el código fuente o configuraciones existentes. Los documentos de detalle pueden ampliar escenarios manuales o de integración cuando existan como archivos del proyecto.
