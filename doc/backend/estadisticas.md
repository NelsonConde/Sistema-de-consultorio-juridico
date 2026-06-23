# Backend - Estadísticas y reportes

> Documento ajustado contra el código fuente actual. Describe la implementación real del módulo de estadísticas en backend.

## 1. Propósito del módulo

El módulo de estadísticas consolida información operativa del consultorio jurídico para generar reportes institucionales por semestre, reportes por rango libre de fechas y vistas resumidas por perfil operativo.

La entrada REST se encuentra en `EstadisticasController` y la lógica se distribuye en servicios especializados de consulta, mapeo y generación PDF.

---

## 2. Componentes principales

| Clase | Responsabilidad |
|---|---|
| `EstadisticasController` | Expone endpoints bajo `/api/estadisticas`. |
| `EstadisticasService` | Fachada del módulo usada por el controller. |
| `EstadisticasQueryService` | Construye reportes institucionales por semestre. |
| `EstadisticasRangoQueryService` | Construye reportes institucionales por rango libre de fechas. |
| `EstadisticasPerfilQueryService` | Construye vistas resumidas por estudiante, asesor o monitor. |
| `EstadisticasMapperService` | Convierte resultados agregados de repositorios a `ConteoDTO`. |
| `EstadisticasPdfService` | Genera reportes PDF a partir de `EstadisticasSemestreDTO`. |
| `EstadisticasSemestreDTO` | DTO principal de salida. |
| `SemestreDTO` | DTO usado para el selector de semestres. |
| `ConteoDTO` | DTO genérico para pares nombre-cantidad. |

---

## 3. Semestres disponibles

`EstadisticasQueryService.listarSemestresDisponibles()` construye los semestres desde el año mínimo `2024` hasta el año actual. Incluye únicamente semestres cuya fecha de inicio no sea futura.

La lista funciona como selector de periodos para frontend y se construye por calendario, independientemente de la presencia de registros operativos en cada semestre.

---

## 4. Reglas de validación temporal

### 4.1 Reportes por semestre

Para obtener estadísticas por semestre, el backend valida:

- semestre `1` o `2`;
- año mayor o igual a `2024`;
- fecha de inicio del semestre no futura.

### 4.2 Reportes por rango

Para obtener estadísticas por rango, el backend valida:

- `fechaInicio` obligatoria;
- `fechaFin` obligatoria;
- `fechaInicio` menor o igual a `fechaFin`;
- año de `fechaInicio` mayor o igual a `2024`;
- `fechaInicio` no futura.

---

## 5. Reporte institucional por semestre

El reporte por semestre usa agregaciones de repositorios para obtener indicadores de consultas, personas, conciliaciones, seguimientos y estudiantes.

Los datos del periodo se obtienen principalmente desde:

- `ConsultaRepository`;
- `ConciliacionRepository`;
- `SeguimientoRepository`;
- `ProcesoRepository`;
- `EstudianteRepository`.

`totalEstudiantesActivos` y `totalEstudiantesHabilitadosConciliacion` representan el estado actual de estudiantes activos al momento de generar el reporte.

---

## 6. Reporte institucional por rango

El reporte por rango usa una estructura equivalente a la del semestre, pero con `fechaInicio` y `fechaFin` recibidas por parámetro.

En el DTO de salida, `año` y `semestre` se envían como `null`, porque el periodo no corresponde a un semestre predefinido. La plantilla PDF interpreta este caso como reporte personalizado.

---

## 7. Indicador `procesosPorEstado`

`procesosPorEstado` se obtiene desde `ProcesoRepository`.

En reportes globales por semestre y por rango, este indicador representa la distribución vigente de procesos por estado, independientemente del periodo seleccionado. En reportes por perfil representa la distribución vigente asociada al estudiante, asesor o monitor indicado.

La documentación del reporte debe distinguir este indicador de otros agregados que sí se calculan con periodo, como consultas, personas, conciliaciones y seguimientos.

---

## 8. Reportes por perfil

`EstadisticasPerfilQueryService` implementa reportes para:

- estudiante;
- asesor;
- monitor.

Estos endpoints están orientados al panel de inicio. Reutilizan `EstadisticasSemestreDTO` y construyen una vista resumida que calcula principalmente:

- periodo;
- consultas finalizadas;
- consultas pendientes;
- total de consultas;
- total de personas atendidas;
- procesos por estado;
- `consultasPorArea` como lista vacía.

Los servicios de estadísticas por perfil implementados corresponden a estudiante, asesor y monitor.

---

## 9. Seguridad

El controller diferencia dos permisos:

- `VER_REPORTES`: reportes institucionales globales y PDFs;
- `VER_CONSULTAS`: listado de semestres y reportes resumidos por perfil.

Los endpoints por perfil reciben el id del perfil como parámetro de ruta. En el frontend, el panel de inicio usa el `perfilId` del usuario autenticado al construir la URL.

---

## 10. Generación PDF

`EstadisticasPdfService` genera el documento PDF con iText. La plantilla incluye:

- encabezado institucional;
- periodo reportado;
- identificación de semestre o reporte personalizado;
- resumen general;
- secciones de conteos solo cuando existen datos para mostrarlas;
- fecha de generación.

Los endpoints PDF retornan `ResponseEntity<byte[]>` con `Content-Type: application/pdf`.
