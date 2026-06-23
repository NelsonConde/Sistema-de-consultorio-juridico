# Base de datos

Esta carpeta documenta la estructura de persistencia observada en entidades JPA, relaciones, estados y catálogos del sistema.

La documentación se basa en el modelo de entidades del código fuente y en las relaciones utilizadas por repositorios, servicios y validadores.

## Documentos

| Documento | Contenido |
|---|---|
| `entidades-principales.md` | Entidades principales del dominio, atributos relevantes y propósito funcional. |
| `estados-y-catalogos.md` | Enumeraciones, catálogos y estados funcionales usados por módulos. |
| `relaciones.md` | Relaciones principales entre usuarios, perfiles, consultas, procesos, seguimientos y conciliaciones. |

## Áreas cubiertas

- Seguridad: usuarios, roles, permisos y perfil actual.
- Perfiles internos: administrativos, asesores, estudiantes, monitores y conciliadores.
- Personas: personas naturales y empresas.
- Catálogos: sedes, áreas, temas, tipos, órganos de control, especialidades y categorías.
- Consultas jurídicas.
- Procesos asociados.
- Seguimientos y respuestas.
- Conciliaciones, reuniones, historial y notificaciones.
- Auditoría.
- Archivos.
- Estadísticas derivadas de entidades operativas.

## Criterios documentales

- Se documentan entidades y relaciones observadas en código.
- Se describe la intención funcional de los campos relevantes.
- Se diferencia entre estado funcional y activo lógico.
- Se evitan datos reales, credenciales o valores sensibles.
- Se describe la estructura como apoyo técnico, no como script de migración.
