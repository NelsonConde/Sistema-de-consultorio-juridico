package co.edu.ufps.legal_cases.business.service.consulta.reasignacion;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.model.consulta.TipoResponsableConsulta;

/**
 * Puerto de integracion con el historial auditable de PB-34.
 *
 * La implementacion debe persistir el registro en la misma transaccion iniciada
 * por la reasignacion. MANDATORY impide usarla accidentalmente fuera de ella.
 */
@FunctionalInterface
public interface ConsultaReasignacionHistorialWriter {

    @Transactional(propagation = Propagation.MANDATORY)
    void registrar(RegistroReasignacion registro);

    record RegistroReasignacion(
            Long consultaId,
            TipoResponsableConsulta tipoResponsable,
            Long responsableAnteriorId,
            Long responsableNuevoId,
            String motivo,
            Long actorUsuarioId) {
    }
}
