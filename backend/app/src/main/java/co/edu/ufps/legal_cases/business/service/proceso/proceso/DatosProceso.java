package co.edu.ufps.legal_cases.business.service.proceso.proceso;

import co.edu.ufps.legal_cases.business.model.catalogo.Departamento;
import co.edu.ufps.legal_cases.business.model.consulta.Consulta;
import co.edu.ufps.legal_cases.business.model.proceso.Especialidad;
import co.edu.ufps.legal_cases.business.model.proceso.OrganoControl;


// Agrupa los datos que ya fueron cargados y validados para construir o actualizar un proceso.
// Lo usamos para no pasar las entidades relacionadas como parámetros sueltos en todo el flujo.
public record DatosProceso(
        String numeroRadicado,
        Departamento departamento,
        Consulta consulta,
        OrganoControl organoControl,
        Especialidad especialidad) {
}