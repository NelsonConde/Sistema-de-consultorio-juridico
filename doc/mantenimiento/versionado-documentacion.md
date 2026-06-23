# Versionado de documentación

Este documento define criterios para versionar y actualizar la documentación técnica del sistema.

## Principio general

La documentación debe evolucionar junto con el código fuente. Cada cambio funcional, contractual o estructural debe reflejarse en los documentos correspondientes.

## Tipos de actualización

| Tipo | Descripción | Ejemplo |
|---|---|---|
| Corrección técnica | Ajuste de un dato documental para alinearlo con el código. | Método HTTP de un endpoint. |
| Ampliación funcional | Documentación de comportamiento ya implementado. | Nuevo módulo de estadísticas. |
| Actualización de contrato | Cambio en request, response, query param o path. | Nuevo campo en DTO. |
| Actualización de regla | Cambio en validación de negocio. | Regla de cierre de consulta. |
| Actualización de estructura | Cambio en carpetas, rutas o componentes. | Nueva ruta frontend. |
| Decisión técnica | Registro de criterio de diseño transversal. | Strategy de perfiles. |

## Recomendaciones de commits

Para cambios documentales generales:

```bash
git add doc
git commit -m "docs: actualizar documentacion tecnica"
```

Para cambios de un módulo específico:

```bash
git add doc/backend/consultas.md doc/api/consultas.md doc/reglas/consultas.md
git commit -m "docs: actualizar documentacion de consultas"
```

Para cambios de frontend:

```bash
git add doc/frontend
git commit -m "docs: actualizar documentacion frontend"
```

## Trazabilidad documental

Cada actualización debe mantener coherencia entre:

- backend;
- API;
- reglas de negocio;
- frontend;
- base de datos;
- decisiones técnicas;
- mantenimiento.

## Revisión previa a entrega

Antes de entregar una versión documental:

- confirmar que los documentos nuevos están enlazados en los índices;
- confirmar que no hay referencias a archivos inexistentes;
- confirmar que los endpoints documentados existen en controllers;
- confirmar que los DTOs documentados existen o coinciden con código;
- confirmar que no se exponen secretos o datos reales;
- confirmar que la documentación describe el sistema implementado.
