package co.edu.ufps.legal_cases.business.service.consulta.consulta;

import java.util.List;

import org.springframework.stereotype.Service;

import co.edu.ufps.legal_cases.business.model.consulta.EstadoConsulta;
import co.edu.ufps.legal_cases.business.repository.consulta.ConsultaRepository;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ConsultaResponsableOperacionService {

    private static final List<EstadoConsulta> ESTADOS_OPERATIVOS = List.of(
            EstadoConsulta.PENDIENTE,
            EstadoConsulta.ACTIVO,
            EstadoConsulta.EN_PROCESO,
            EstadoConsulta.URGENTE);

    private final ConsultaRepository consultaRepository;

    public void validarAsesorSinConsultasOperativas(Long asesorId) {
        if (asesorId == null) {
            throw new BusinessException("El id del asesor es obligatorio");
        }

        boolean tieneConsultasDirectas = consultaRepository
                .existsByAsesor_IdAndEstadoIn(asesorId, ESTADOS_OPERATIVOS);

        boolean tieneConsultasPorEstudiantes = consultaRepository
                .existsByEstudiante_Asesor_IdAndEstadoIn(asesorId, ESTADOS_OPERATIVOS);

        if (tieneConsultasDirectas || tieneConsultasPorEstudiantes) {
            throw new BusinessException(
                    "No se puede desactivar el asesor porque tiene consultas operativas asignadas o asociadas a sus estudiantes");
        }
    }

    public void validarEstudianteSinConsultasOperativas(Long estudianteId) {
        if (estudianteId == null) {
            throw new BusinessException("El id del estudiante es obligatorio");
        }

        if (consultaRepository.existsByEstudiante_IdAndEstadoIn(estudianteId, ESTADOS_OPERATIVOS)) {
            throw new BusinessException(
                    "No se puede desactivar el estudiante porque tiene consultas operativas asignadas");
        }
    }

    public void validarMonitorSinConsultasOperativas(Long monitorId) {
        if (monitorId == null) {
            throw new BusinessException("El id del monitor es obligatorio");
        }

        if (consultaRepository.existsByMonitor_IdAndEstadoIn(monitorId, ESTADOS_OPERATIVOS)) {
            throw new BusinessException(
                    "No se puede desactivar el monitor porque tiene consultas operativas asignadas");
        }
    }
}