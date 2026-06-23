# Mantenimiento documental

Esta carpeta define lineamientos para conservar la documentación alineada con el código fuente del sistema.

Su propósito es facilitar revisiones futuras cuando cambien endpoints, DTOs, servicios, entidades, permisos, rutas frontend, validaciones o decisiones técnicas.

## Documentos

| Documento | Propósito |
|---|---|
| `checklist-cambios.md` | Lista de verificación para actualizar documentación ante cambios de código. |
| `estado-cobertura-documental.md` | Mapa de cobertura documental vigente por área del sistema. |
| `matriz-actualizacion-documental.md` | Relación entre tipo de cambio y documentos que deben revisarse. |
| `versionado-documentacion.md` | Criterios para versionar y registrar cambios documentales. |

## Fuente de verdad

La fuente principal es el código fuente vigente:

1. Controllers y rutas.
2. DTOs.
3. Entidades y enumeraciones.
4. Services, CommandServices y QueryServices.
5. Validators y AccessServices.
6. Repositories.
7. Componentes y rutas frontend.
8. Pruebas unitarias.
9. Documentación vigente.

## Reglas de mantenimiento

- Si cambia un endpoint, se revisa `api/` y el módulo frontend relacionado.
- Si cambia un DTO, se revisa API, backend y formularios frontend.
- Si cambia una regla de negocio, se revisa `reglas/`, `backend/` y `api/`.
- Si cambia un permiso, se revisa permisos, frontend de navegación y módulos afectados.
- Si cambia una entidad o relación, se revisa `base-datos/`.
- Si cambia una decisión transversal, se revisa `decisiones/`.

## Seguridad documental

La documentación no debe incluir valores reales de:

- contraseñas;
- tokens;
- secretos JWT;
- API keys;
- cadenas de conexión con credenciales;
- datos personales reales;
- usuarios reales de prueba;
- rutas privadas del equipo.

Se documentan nombres de variables y propósito funcional, no valores sensibles.
