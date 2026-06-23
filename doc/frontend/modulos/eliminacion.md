# Frontend - Módulo de eliminación y reactivación

## 1. Propósito del módulo

El módulo de eliminación centraliza la consulta de registros inactivos o archivados y permite reactivarlos cuando el usuario tiene permisos suficientes. En el frontend, esta pantalla funciona como un panel administrativo de recuperación de registros.

Ruta:

```text
/eliminacion
```

Componente principal:

```text
src/components/forms/EliminacionForm.jsx
```

## 2. Ruta y composición

La página se define en:

```text
src/app/(dashboard)/eliminacion/page.js
```

Renderiza `EliminacionForm` dentro de una vista con encabezado e iconografía de recuperación.

## 3. Secciones gestionadas

El componente define internamente las secciones disponibles:

| Sección | Endpoint de carga | Tipo de recuperación |
|---|---|---|
| Personas | `/personas` | Reactivación por endpoint específico. |
| Consultas | `/consultas/archivadas` | Desarchivo de consulta. |
| Usuarios del sistema | `/usuarios-sistema` | Cambio `activo=true`. |
| Administrativos | `/administrativos` | Cambio `activo=true`. |
| Asesores | `/asesores` | Cambio `activo=true`. |
| Estudiantes | `/estudiantes` | Cambio `activo=true`. |
| Monitores | `/monitores` | Cambio `activo=true`. |
| Conciliadores | `/conciliadores` | Cambio `activo=true`. |

El componente carga todas las secciones y luego filtra en cliente los elementos inactivos o archivados según corresponda.

## 4. Permisos usados

Antes de cargar la pantalla, el componente valida sesión con:

```text
GET /api/auth/me
```

Para ingresar exige un conjunto de permisos relacionados con cambio de estado y archivo:

```text
Cambiar estado personas
Cambiar estado usuarios
Cambiar estado estudiantes
Cambiar estado consultas
Archivar consultas
```

La navegación lateral muestra la ruta cuando el usuario cuenta con `Acceder eliminación`.

## 5. Endpoints de carga

El componente obtiene los datos con:

```text
GET /api/personas
GET /api/consultas/archivadas
GET /api/usuarios-sistema
GET /api/administrativos
GET /api/asesores
GET /api/estudiantes
GET /api/monitores
GET /api/conciliadores
```

Cuando un endpoint responde 403, la sección se deja vacía sin interrumpir la carga del resto de la pantalla. Esta decisión permite que el panel funcione según alcance efectivo del usuario.

## 6. Endpoints de reactivación

### 6.1 Personas

```text
PATCH /api/personas/{id}/reactivar
```

### 6.2 Consultas archivadas

```text
PATCH /api/consultas/{id}/desarchivar
```

### 6.3 Usuarios y perfiles

Para usuarios y perfiles se usa:

```text
PATCH /api/{endpoint}/{id}/activo?activo=true
```

Ejemplos:

```text
PATCH /api/usuarios-sistema/{id}/activo?activo=true
PATCH /api/administrativos/{id}/activo?activo=true
PATCH /api/asesores/{id}/activo?activo=true
PATCH /api/estudiantes/{id}/activo?activo=true
PATCH /api/monitores/{id}/activo?activo=true
PATCH /api/conciliadores/{id}/activo?activo=true
```

## 7. Filtros y visualización

La pantalla permite buscar registros por:

- identificador;
- nombre visible;
- documento;
- texto de consulta;
- detalle de perfil;
- estado.

También usa paginación cliente mediante utilidades de lista:

```text
getTotalPages
paginateItems
sortByIdAsc
DEFAULT_PAGE_SIZE_OPTIONS
```

## 8. Confirmación de reactivación

Antes de reactivar un registro, el componente abre un diálogo de confirmación. Al confirmar, ejecuta la petición correspondiente y recarga todas las secciones.

Este patrón evita cambios accidentales y mantiene la pantalla actualizada después de cada operación.

## 9. Diferencia entre inactivo y archivado

El componente diferencia:

| Caso | Criterio visual |
|---|---|
| Registro inactivo | `activo === false` o estado textual `INACTIVO`. |
| Consulta archivada | Estado `ARCHIVADO` o `ARCHIVADA`. |

Las consultas no se reactivan con `activo=true`; se desarchivan mediante endpoint específico.

## 10. Manejo de errores

El componente maneja:

- sesión expirada;
- permisos insuficientes;
- errores de carga por sección;
- errores del backend al reactivar;
- respuestas JSON o texto plano.

Las notificaciones se muestran mediante `toast`.

## 11. Relación con reglas backend

La pantalla presenta y solicita reactivaciones, pero las reglas definitivas permanecen en backend. Por ejemplo, el backend decide si un perfil puede reactivarse y aplica las validaciones asociadas a usuarios, perfiles y consultas.

## 12. Alcance de la documentación

Este documento describe la pantalla frontend de eliminación/reactivación. No documenta eliminación física, porque el flujo visible trabaja con estados, archivo y reactivación lógica.


---

## 11. Precisión sobre UsuarioSistema y perfiles reales

La pantalla permite reactivar distintos tipos de registros mediante endpoints de estado. Cuando se reactiva un perfil operativo desde su endpoint propio, el backend sincroniza el estado del `UsuarioSistema` asociado.

Cuando se reactiva directamente un `UsuarioSistema` mediante:

```text
PATCH /api/usuarios-sistema/{id}/activo?activo=true
```

se modifica la cuenta de acceso. Esa operación no reactiva automáticamente el perfil real asociado. Por esa razón, la pantalla diferencia entre reactivación de cuentas y reactivación de perfiles o registros operativos.
