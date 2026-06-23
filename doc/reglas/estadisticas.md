# Reglas de negocio - Estadísticas y reportes

> Documento ajustado contra el código fuente actual. Describe las reglas implementadas en los servicios de estadísticas.

## 1. Regla general

Las estadísticas se calculan dinámicamente a partir de información operativa persistida en consultas, personas, procesos, seguimientos, conciliaciones y estudiantes.

---

## 2. Periodos permitidos

El sistema maneja:

- reportes por semestre;
- reportes por rango libre de fechas;
- reportes resumidos por perfil dentro de un semestre.

Los semestres disponibles se construyen desde 2024 hasta el año actual, excluyendo semestres que aún no han iniciado.

---

## 3. Validación de semestre

Para reportes por semestre:

- el semestre debe ser `1` o `2`;
- el año debe ser mayor o igual a `2024`;
- el semestre no puede tener fecha de inicio futura.

---

## 4. Validación de rango

Para reportes por rango:

- fecha de inicio y fecha fin son obligatorias;
- la fecha de inicio no puede ser posterior a la fecha fin;
- la fecha de inicio no puede ser futura;
- el año de la fecha de inicio debe ser mayor o igual a 2024.

---

## 5. Indicadores institucionales

El reporte institucional incluye indicadores de:

- consultas finalizadas, pendientes y totales;
- consultas por estado, área y tipo de violencia;
- personas atendidas y distribuciones por atributos sociodemográficos;
- conciliaciones por periodo y estado;
- seguimientos por periodo y estado;
- estudiantes activos y estudiantes habilitados para conciliación.

Los campos de estudiantes activos representan el estado actual al momento de generar el reporte.

---

## 6. Procesos por estado

El indicador `procesosPorEstado` se calcula desde las agregaciones disponibles en `ProcesoRepository`.

En reportes globales, funciona como distribución complementaria vigente de procesos por estado. En reportes por perfil, representa la distribución vigente asociada al estudiante, asesor o monitor indicado. Esta regla debe conservarse al interpretar el reporte.

---

## 7. Reportes por perfil

Los reportes resumidos por perfil implementados corresponden a estudiante, asesor y monitor. Reutilizan el DTO de estadísticas semestrales y entregan para el panel de inicio los campos construidos por `EstadisticasPerfilQueryService`.

---

## 8. Reglas de acceso

- Los reportes institucionales y PDFs requieren `VER_REPORTES`.
- Los semestres disponibles pueden consultarse con `VER_REPORTES` o `VER_CONSULTAS`.
- Las estadísticas por estudiante, asesor y monitor requieren `VER_CONSULTAS`.

---

## 9. PDF

Los PDF se generan en backend mediante `EstadisticasPdfService`. Los reportes por semestre y por rango usan la misma plantilla. Cuando el reporte es por rango libre, el PDF se identifica como reporte personalizado.
