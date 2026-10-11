package co.edu.ufps.legal_cases.business.service.consulta.reasignacion;

import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarTexto;

import java.time.Instant;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.model.consulta.ConsultaReasignacionHistorial;
import co.edu.ufps.legal_cases.business.repository.consulta.ConsultaReasignacionHistorialRepository;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

@Service
public class JpaConsultaReasignacionHistorialWriter
        implements ConsultaReasignacionHistorialWriter {

    private final ConsultaReasignacionHistorialRepository historialRepository;

    public JpaConsultaReasignacionHistorialWriter(
            ConsultaReasignacionHistorialRepository historialRepository) {
        this.historialRepository = historialRepository;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(RegistroReasignacion registro) {
        String motivo = validar(registro);

        historialRepository.save(new ConsultaReasignacionHistorial(
                registro.consultaId(),
                registro.tipoResponsable(),
                registro.responsableAnteriorId(),
                registro.responsableNuevoId(),
                motivo,
                registro.actorUsuarioId(),
                Instant.now()));
    }

    private String validar(RegistroReasignacion registro) {
        if (registro == null) {
            throw new BusinessException("El registro de reasignacion es obligatorio");
        }
        validarIdPositivo(registro.consultaId(), "La consulta es obligatoria");
        if (registro.tipoResponsable() == null) {
            throw new BusinessException("El tipo de responsable es obligatorio");
        }
        if (registro.responsableAnteriorId() != null) {
            validarIdPositivo(
                    registro.responsableAnteriorId(),
                    "El responsable anterior no es valido");
        }
        validarIdPositivo(
                registro.responsableNuevoId(),
                "El nuevo responsable es obligatorio");
        validarIdPositivo(
                registro.actorUsuarioId(),
                "El usuario que realiza la reasignacion es obligatorio");

        if (Objects.equals(
                registro.responsableAnteriorId(),
                registro.responsableNuevoId())) {
            throw new BusinessException("El responsable anterior y el nuevo deben ser diferentes");
        }

        String motivo = normalizarTexto(registro.motivo());
        if (motivo == null) {
            throw new BusinessException("El motivo de la reasignacion es obligatorio");
        }
        return motivo;
    }

    private void validarIdPositivo(Long id, String mensaje) {
        if (id == null || id <= 0) {
            throw new BusinessException(mensaje);
        }
    }
}
