package co.edu.ufps.legal_cases.business.service.seguimiento.respuesta;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.dto.seguimiento.respuesta.SeguimientoRespuestaResponseDTO;
import co.edu.ufps.legal_cases.business.model.consulta.EstadoConsulta;
import co.edu.ufps.legal_cases.business.model.seguimiento.respuesta.EstadoRespuestaSeguimiento;
import co.edu.ufps.legal_cases.business.model.seguimiento.respuesta.SeguimientoRespuesta;
import co.edu.ufps.legal_cases.business.repository.seguimiento.respuesta.SeguimientoRespuestaRepository;
import co.edu.ufps.legal_cases.business.service.acceso.seguimiento.SeguimientoRespuestaAccessService;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

@Service
public class SeguimientoRespuestaQueryService {

    private static final EstadoConsulta ESTADO_ARCHIVADO = EstadoConsulta.ARCHIVADO;

    private final SeguimientoRespuestaRepository seguimientoRespuestaRepository;
    private final SeguimientoRespuestaAccessService seguimientoRespuestaAccessService;
    private final SeguimientoRespuestaMapper seguimientoRespuestaMapper;

    public SeguimientoRespuestaQueryService(
            SeguimientoRespuestaRepository seguimientoRespuestaRepository,
            SeguimientoRespuestaAccessService seguimientoRespuestaAccessService,
            SeguimientoRespuestaMapper seguimientoRespuestaMapper) {
        this.seguimientoRespuestaRepository = seguimientoRespuestaRepository;
        this.seguimientoRespuestaAccessService = seguimientoRespuestaAccessService;
        this.seguimientoRespuestaMapper = seguimientoRespuestaMapper;
    }

    @Transactional(readOnly = true)
    public SeguimientoRespuestaResponseDTO obtenerPorId(Long id) {
        SeguimientoRespuesta respuesta = buscarRespuestaActiva(id);

        if (!seguimientoRespuestaAccessService.puedeVerRespuesta(respuesta)) {
            throw new AccessDeniedException("No tiene permisos para ver esta respuesta");
        }

        return seguimientoRespuestaMapper.convertirAResponseDTO(respuesta);
    }

    @Transactional(readOnly = true)
    public List<SeguimientoRespuestaResponseDTO> listarPorSeguimiento(Long seguimientoId) {
        seguimientoRespuestaAccessService.validarPuedeListarRespuestasDeSeguimiento(seguimientoId);

        return seguimientoRespuestaRepository
                .findBySeguimiento_IdAndActivoTrueAndSeguimiento_ActivoTrueAndSeguimiento_Consulta_EstadoNotOrderByFechaCreacionDesc(
                        seguimientoId,
                        ESTADO_ARCHIVADO)
                .stream()
                .filter(seguimientoRespuestaAccessService::puedeVerRespuesta)
                .map(seguimientoRespuestaMapper::convertirAResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SeguimientoRespuestaResponseDTO> listarPendientes() {
        seguimientoRespuestaAccessService.validarPuedeListarRespuestasPendientes();

        return seguimientoRespuestaRepository
                .findByEstadoAndActivoTrueAndSeguimiento_ActivoTrueAndSeguimiento_Consulta_EstadoNotOrderByFechaCreacionDesc(
                        EstadoRespuestaSeguimiento.PENDIENTE,
                        ESTADO_ARCHIVADO)
                .stream()
                .filter(seguimientoRespuestaAccessService::puedeRevisarRespuesta)
                .map(seguimientoRespuestaMapper::convertirAResponseDTO)
                .toList();
    }

    private SeguimientoRespuesta buscarRespuestaActiva(Long id) {
        if (id == null) {
            throw new BusinessException("El id de la respuesta es obligatorio");
        }

        return seguimientoRespuestaRepository
                .findByIdAndActivoTrueAndSeguimiento_ActivoTrueAndSeguimiento_Consulta_EstadoNot(
                        id,
                        ESTADO_ARCHIVADO)
                .orElseThrow(() -> new BusinessException("Respuesta de seguimiento no encontrada con id: " + id));
    }
}