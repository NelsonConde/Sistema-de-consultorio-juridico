package co.edu.ufps.legal_cases.business.service.consulta.consulta;

import org.springframework.stereotype.Service;

import co.edu.ufps.legal_cases.business.repository.conciliacion.ConciliacionRepository;
import co.edu.ufps.legal_cases.business.repository.proceso.ProcesoRepository;
import co.edu.ufps.legal_cases.business.repository.seguimiento.SeguimientoRepository;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ConsultaActividadService {

    private final ProcesoRepository procesoRepository;
    private final SeguimientoRepository seguimientoRepository;
    private final ConciliacionRepository conciliacionRepository;

    public boolean tieneActividadAsociada(Long consultaId) {
        validarConsultaId(consultaId);

        return procesoRepository.existsByConsulta_IdAndActivoTrue(consultaId)
                || seguimientoRepository.existsByConsulta_IdAndActivoTrue(consultaId)
                || conciliacionRepository.existsByConsulta_IdAndActivoTrue(consultaId);
    }

    public void validarSinActividadAsociada(Long consultaId) {
        if (tieneActividadAsociada(consultaId)) {
            throw new BusinessException(
                    "No se pueden modificar datos estructurales de la consulta porque ya tiene procesos, seguimientos o conciliaciones asociadas");
        }
    }

    private void validarConsultaId(Long consultaId) {
        if (consultaId == null) {
            throw new BusinessException("El id de la consulta es obligatorio");
        }
    }
}