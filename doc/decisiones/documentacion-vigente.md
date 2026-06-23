# Decisión técnica - Documentación vigente

## Contexto

El proyecto requiere documentación técnica, funcional y de API que acompañe el código. La documentación debe servir para comprender arquitectura, integrar frontend y backend, explicar reglas de negocio, defender decisiones técnicas y evitar análisis basados en información desactualizada.

## Decisión

La documentación oficial del repositorio describe el estado vigente del código fuente. No funciona como histórico informal de cambios ni como listado de críticas.

Cuando una regla, endpoint, DTO, permiso, entidad o estado cambia, se actualizan los documentos afectados.

## Fuente de verdad

Orden de prioridad:

1. Código fuente actual.
2. Configuración actual.
3. Entidades y modelos actuales.
4. Tests existentes.
5. Documentación vigente.

La documentación no debe contradecir el código.

## Estructura documental

| Carpeta | Propósito |
|---|---|
| `doc/backend` | Implementación backend por módulo. |
| `doc/api` | Contratos de endpoints para frontend y pruebas. |
| `doc/reglas` | Reglas de negocio vigentes. |
| `doc/base-datos` | Entidades, relaciones, estados y catálogos. |
| `doc/frontend` | Rutas, formularios, configuración y módulos frontend. |
| `doc/decisiones` | Justificación de decisiones técnicas. |
| `doc/mantenimiento` | Guías de actualización documental. |

## Qué se documenta

Se documenta:

- comportamiento vigente;
- endpoints reales;
- DTOs reales;
- permisos reales;
- reglas implementadas;
- relaciones principales;
- criterios de seguridad;
- decisiones técnicas relevantes;
- rutas y componentes frontend existentes;
- pruebas existentes cuando corresponda.

## Qué no se documenta como estado vigente

No se documentan como si estuvieran implementados:

- reglas futuras;
- endpoints no implementados;
- pantallas inexistentes;
- cambios no integrados;
- valores reales de configuración sensible;
- comportamientos inferidos sin respaldo del código.

## Cambios y documentos a revisar

| Cambio | Documentos a revisar |
|---|---|
| Cambio en cierre de consulta | `backend/consultas.md`, `api/consultas.md`, `reglas/consultas.md`. |
| Cambio en radicado de proceso | `backend/procesos.md`, `api/procesos.md`, `reglas/procesos.md`, `base-datos/entidades-principales.md`. |
| Cambio en seguimiento o respuesta | `backend/seguimientos.md`, `api/seguimientos.md`, `reglas/seguimientos.md`. |
| Cambio en conciliación | `backend/conciliaciones.md`, `api/conciliaciones.md`, `reglas/conciliaciones.md`, `decisiones/conciliacion.md`. |
| Cambio en perfil o Strategy | `backend/perfiles.md`, `api/perfiles.md`, `api/usuarios-roles-permisos.md`, `decisiones/estrategia-perfiles.md`. |
| Cambio en estadísticas | `backend/estadisticas.md`, `api/estadisticas.md`, `reglas/estadisticas.md`, `frontend/modulos/estadisticas.md`. |
| Cambio en frontend | `doc/frontend` y módulo específico. |

## Criterio de redacción

La documentación se redacta en tono profesional y descriptivo:

```text
El sistema implementa...
El backend valida...
El frontend consume...
El módulo permite...
```

No se redacta como lista de carencias.
