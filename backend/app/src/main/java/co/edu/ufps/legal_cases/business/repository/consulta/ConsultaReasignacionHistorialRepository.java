package co.edu.ufps.legal_cases.business.repository.consulta;

import org.springframework.data.repository.Repository;

import co.edu.ufps.legal_cases.business.model.consulta.ConsultaReasignacionHistorial;

/**
 * Repositorio deliberadamente limitado a insercion. La lectura se incorporara
 * con PB-34 y no se exponen operaciones ordinarias de actualizacion o borrado.
 */
public interface ConsultaReasignacionHistorialRepository
        extends Repository<ConsultaReasignacionHistorial, Long> {

    ConsultaReasignacionHistorial save(ConsultaReasignacionHistorial historial);
}
