# Frontend - Módulo de catálogos

## 1. Propósito del módulo

El módulo de catálogos permite administrar parámetros base usados por las consultas jurídicas y otros formularios del sistema. En el frontend actual, la administración visible de catálogos se concentra en la ruta `/admin`, mediante pestañas para área, tema y tipo.

Los catálogos documentados en esta vista son los que tienen formularios CRUD visibles en la ruta `/admin`:

```text
Área
Tema
Tipo
```

La jerarquía funcional es:

```text
Área → Tema → Tipo
```

## 2. Ruta y componentes

La ruta es:

```text
/admin
```

La página se define en:

```text
src/app/(dashboard)/admin/page.js
```

y renderiza los siguientes componentes:

```text
src/components/forms/catalogos/AreaForm.jsx
src/components/forms/catalogos/TemaForm.jsx
src/components/forms/catalogos/TipoForm.jsx
```

## 3. Organización visual

Los catálogos se muestran en pestañas dentro de la pantalla de administración:

| Pestaña | Componente | Recurso |
|---|---|---|
| Área | `AreaForm` | Áreas jurídicas. |
| Tema | `TemaForm` | Temas asociados a áreas. |
| Tipo | `TipoForm` | Tipos asociados a temas. |

La misma pantalla también contiene pestañas administrativas de permisos, cambio de rol y auditoría.

## 4. Permisos usados

| Permiso | Uso frontend |
|---|---|
| `Acceder administración` | Permite entrar a la pantalla `/admin`. |
| `Ver catálogos` | Permite consultar catálogos cuando el componente lo requiere. |
| `Gestionar catálogos` | Permite crear, editar y desactivar áreas, temas y tipos. |

Cada formulario consulta `/api/auth/me` para validar sesión y permisos antes de cargar o permitir operaciones.

## 5. Formulario de áreas

Componente:

```text
AreaForm.jsx
```

Endpoints consumidos:

```text
GET /api/areas
POST /api/areas
PUT /api/areas/{id}
PATCH /api/areas/{id}/activo?activo=false
```

El formulario permite:

- listar áreas;
- crear área;
- editar área existente;
- desactivar área con confirmación;
- mostrar errores del backend mediante toast.

## 6. Formulario de temas

Componente:

```text
TemaForm.jsx
```

Endpoints consumidos:

```text
GET /api/areas
GET /api/temas
POST /api/temas
PUT /api/temas/{id}
PATCH /api/temas/{id}/activo?activo=false
```

El selector de área permite asociar cada tema a un área. El frontend carga áreas antes de permitir la gestión de temas.

## 7. Formulario de tipos

Componente:

```text
TipoForm.jsx
```

Endpoints consumidos:

```text
GET /api/temas
GET /api/tipos
POST /api/tipos
PUT /api/tipos/{id}
PATCH /api/tipos/{id}/activo?activo=false
```

El selector de tema permite asociar cada tipo a un tema.

## 8. Patrón común de formularios

Los tres formularios comparten un patrón:

```text
1. Validan sesión con /api/auth/me.
2. Validan permiso de gestión.
3. Cargan datos existentes.
4. Presentan formulario de creación.
5. Permiten seleccionar un registro para edición.
6. Envían POST o PUT según modo.
7. Usan PATCH para desactivar.
8. Recargan la lista después de operaciones exitosas.
```

## 9. Confirmación de desactivación

La desactivación se realiza mediante `ConfirmActionDialog`. Esto evita cambiar el estado de un catálogo por accidente.

El frontend no elimina físicamente los registros; usa endpoints de cambio de estado activo.

## 10. Manejo de errores

Los formularios capturan:

- sesión expirada;
- permisos insuficientes;
- errores de red;
- errores de validación del backend;
- duplicados o relaciones inválidas reportadas por el backend.

Los mensajes se muestran mediante `toast` y estados locales.

## 11. Relación con otros módulos

Áreas, temas y tipos alimentan otros formularios, especialmente:

- nueva consulta;
- consulta jurídica;
- procesos, cuando dependen de catálogos de apoyo;
- estadísticas, porque agrupa resultados por área.

## 12. Alcance de la documentación

Este documento describe los formularios visibles de administración implementados en `/admin` para áreas, temas y tipos.

Sedes, tipos de documento, órganos de control y especialidades se consumen como datos auxiliares en los formularios de los módulos que los requieren.

Los formularios frontend de área, tema y tipo administran su estado mediante:

```text
PATCH /api/areas/{id}/activo?activo=
PATCH /api/temas/{id}/activo?activo=
PATCH /api/tipos/{id}/activo?activo=
```
