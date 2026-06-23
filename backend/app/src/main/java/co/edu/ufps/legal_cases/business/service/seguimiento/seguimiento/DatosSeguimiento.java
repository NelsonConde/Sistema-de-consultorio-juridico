package co.edu.ufps.legal_cases.business.service.seguimiento.seguimiento;

import java.time.LocalDate;

import co.edu.ufps.legal_cases.business.model.consulta.Consulta;
import co.edu.ufps.legal_cases.business.model.seguimiento.CategoriaSeguimiento;

// Agrupa los datos ya normalizados y cargados para crear o actualizar un seguimiento.
// Así el flujo no queda pasando categoría, consulta y banderas como parámetros sueltos.
public record DatosSeguimiento(
        String descripcion,
        LocalDate fechaEntrega,
        Integer diasNotificacion,
        Boolean notificarPartes,
        Boolean notificarEstudiante,
        Boolean alertaDisciplinaria,
        CategoriaSeguimiento categoria,
        Consulta consulta) {
}