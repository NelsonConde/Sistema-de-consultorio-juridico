# API - Estadísticas

> Documento ajustado contra el código fuente actual. Describe los endpoints implementados en `EstadisticasController` y las reglas aplicadas por los servicios de estadísticas.

## 1. Ruta base

```http
/api/estadisticas
```

La API de estadísticas expone reportes consolidados en JSON y PDF. El módulo trabaja con tres tipos de consulta:

- reportes globales por semestre;
- reportes globales por rango libre de fechas;
- reportes resumidos por perfil operativo para estudiante, asesor y monitor.

---

## 2. Endpoints implementados

| Método | Endpoint | Descripción | Permiso |
|---|---|---|---|
| `GET` | `/api/estadisticas/semestres` | Lista semestres disponibles para selector de periodo | `VER_REPORTES` o `VER_CONSULTAS` |
| `GET` | `/api/estadisticas/{año}/semestre/{semestre}` | Reporte institucional por semestre | `VER_REPORTES` |
| `GET` | `/api/estadisticas/{año}/semestre/{semestre}/pdf` | Descarga PDF del reporte por semestre | `VER_REPORTES` |
| `GET` | `/api/estadisticas/reporte?fechaInicio={fecha}&fechaFin={fecha}` | Reporte institucional por rango de fechas | `VER_REPORTES` |
| `GET` | `/api/estadisticas/reporte/pdf?fechaInicio={fecha}&fechaFin={fecha}` | Descarga PDF del reporte por rango | `VER_REPORTES` |
| `GET` | `/api/estadisticas/{año}/semestre/{semestre}/estudiante/{id}` | Reporte resumido por estudiante | `VER_CONSULTAS` |
| `GET` | `/api/estadisticas/{año}/semestre/{semestre}/asesor/{id}` | Reporte resumido por asesor | `VER_CONSULTAS` |
| `GET` | `/api/estadisticas/{año}/semestre/{semestre}/monitor/{id}` | Reporte resumido por monitor | `VER_CONSULTAS` |

Los reportes resumidos por perfil expuestos por el controller corresponden a estudiante, asesor y monitor.

---

## 3. Semestres disponibles

```http
GET /api/estadisticas/semestres
```

El backend construye la lista de semestres disponibles desde el año `2024` hasta el año actual. Solo incluye semestres cuya fecha de inicio ya ocurrió; por tanto, la lista funciona como catálogo de periodos consultables y no depende de que existan registros operativos en cada semestre.

Ejemplo de respuesta:

```json
[
  {
    "año": 2026,
    "semestre": 1,
    "etiqueta": "2026-1",
    "periodoInicio": "2026-01-01",
    "periodoFin": "2026-06-30"
  }
]
```

---

## 4. Validaciones de semestre

Los endpoints por semestre aceptan:

- `semestre` igual a `1` o `2`;
- `año` mayor o igual a `2024`;
- periodos cuya fecha de inicio no sea futura.

Ejemplo:

```http
GET /api/estadisticas/2026/semestre/1
```

---

## 5. Reporte por rango libre de fechas

```http
GET /api/estadisticas/reporte?fechaInicio=2026-01-01&fechaFin=2026-06-30
```

Los parámetros se reciben en formato ISO:

```text
yyyy-MM-dd
```

Reglas aplicadas por el backend:

- `fechaInicio` y `fechaFin` son obligatorias;
- `fechaInicio` no puede ser posterior a `fechaFin`;
- el año de `fechaInicio` debe ser mayor o igual a `2024`;
- `fechaInicio` no puede ser futura.

La validación temporal del rango aplica el año mínimo y el control de fecha futura sobre `fechaInicio`; `fechaFin` participa en las validaciones de obligatoriedad y orden del rango.

---

## 6. DTO de respuesta institucional

Los reportes por semestre y por rango retornan `EstadisticasSemestreDTO`.

Campos principales:

| Campo | Descripción |
|---|---|
| `año` | Año del semestre consultado. En reporte por rango se envía `null`. |
| `semestre` | Semestre consultado. En reporte por rango se envía `null`. |
| `periodoInicio` | Fecha inicial del periodo reportado. |
| `periodoFin` | Fecha final del periodo reportado. |
| `consultasFinalizadas` | Consultas finalizadas del periodo. |
| `consultasPendientes` | Consultas pendientes del periodo. |
| `totalConsultas` | Total de consultas calculado a partir de finalizadas y pendientes. |
| `consultasPorEstado` | Distribución de consultas por estado en el periodo. |
| `consultasPorArea` | Distribución de consultas por área en el periodo. |
| `consultasPorTipoViolencia` | Distribución de consultas por tipo de violencia en el periodo. |
| `totalPersonasAtendidas` | Total de personas atendidas en el periodo. |
| `personasPorGenero` | Personas agrupadas por género. |
| `personasPorEstrato` | Personas agrupadas por estrato. |
| `personasPorZona` | Personas agrupadas por zona. |
| `personasPorGrupoEtnico` | Personas agrupadas por grupo étnico. |
| `personasPorMunicipio` | Personas agrupadas por municipio. |
| `personasPorCondicion` | Personas agrupadas por condición. |
| `procesosPorEstado` | Indicador complementario de procesos por estado. |
| `totalConciliaciones` | Conciliaciones del periodo. |
| `conciliacionesPorEstado` | Conciliaciones por estado en el periodo. |
| `totalSeguimientos` | Seguimientos del periodo. |
| `seguimientosPorEstado` | Seguimientos por estado en el periodo. |
| `totalEstudiantesActivos` | Total actual de estudiantes activos al momento del reporte. |
| `totalEstudiantesHabilitadosConciliacion` | Total actual de estudiantes activos habilitados para conciliación. |

---

## 7. Precisión sobre `procesosPorEstado`

El campo `procesosPorEstado` se obtiene desde agregaciones de `ProcesoRepository`.

En reportes globales por semestre o por rango, `procesosPorEstado` representa la distribución vigente de procesos, independientemente del periodo seleccionado. En reportes por perfil representa la distribución vigente asociada al estudiante, asesor o monitor indicado.

Esta precisión es importante porque los demás indicadores institucionales del reporte sí usan el periodo consultado cuando el repositorio correspondiente recibe año, semestre o rango.

---

## 8. Reportes por perfil operativo

Endpoints:

```http
GET /api/estadisticas/{año}/semestre/{semestre}/estudiante/{id}
GET /api/estadisticas/{año}/semestre/{semestre}/asesor/{id}
GET /api/estadisticas/{año}/semestre/{semestre}/monitor/{id}
```

Estos endpoints reutilizan `EstadisticasSemestreDTO`, pero entregan una vista resumida orientada al panel de inicio. Los campos calculados en esa vista son principalmente:

- año;
- semestre;
- periodoInicio;
- periodoFin;
- consultasFinalizadas;
- consultasPendientes;
- totalConsultas;
- consultasPorArea como lista vacía;
- procesosPorEstado;
- totalPersonasAtendidas.

Los endpoints por perfil calculan el subconjunto de campos descrito para la vista resumida del panel de inicio.

Los endpoints por perfil requieren `VER_CONSULTAS` y reciben el id del perfil en la ruta. En el frontend de inicio, ese id se toma del `perfilId` del usuario autenticado.

---

## 9. Descarga PDF

Endpoints:

```http
GET /api/estadisticas/2026/semestre/1/pdf
GET /api/estadisticas/reporte/pdf?fechaInicio=2026-01-01&fechaFin=2026-06-30
```

La respuesta retorna bytes con `Content-Type: application/pdf` y encabezado `Content-Disposition` para descarga.

Los reportes PDF por semestre y por rango usan la misma plantilla de `EstadisticasPdfService`. Cuando el reporte corresponde a rango libre, el DTO no contiene año ni semestre y el PDF se presenta como reporte personalizado.
