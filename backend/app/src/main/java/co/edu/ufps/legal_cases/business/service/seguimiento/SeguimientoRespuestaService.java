package co.edu.ufps.legal_cases.business.service.seguimiento;

import java.util.List;

import org.springframework.stereotype.Service;

import co.edu.ufps.legal_cases.business.dto.seguimiento.respuesta.SeguimientoRespuestaDecisionDTO;
import co.edu.ufps.legal_cases.business.dto.seguimiento.respuesta.SeguimientoRespuestaRequestDTO;
import co.edu.ufps.legal_cases.business.dto.seguimiento.respuesta.SeguimientoRespuestaResponseDTO;
import co.edu.ufps.legal_cases.business.service.seguimiento.respuesta.SeguimientoRespuestaCommandService;
import co.edu.ufps.legal_cases.business.service.seguimiento.respuesta.SeguimientoRespuestaQueryService;

@Service
public class SeguimientoRespuestaService {

    private final SeguimientoRespuestaQueryService seguimientoRespuestaQueryService;
    private final SeguimientoRespuestaCommandService seguimientoRespuestaCommandService;

    public SeguimientoRespuestaService(
            SeguimientoRespuestaQueryService seguimientoRespuestaQueryService,
            SeguimientoRespuestaCommandService seguimientoRespuestaCommandService) {
        this.seguimientoRespuestaQueryService = seguimientoRespuestaQueryService;
        this.seguimientoRespuestaCommandService = seguimientoRespuestaCommandService;
    }

    public SeguimientoRespuestaResponseDTO obtenerPorId(Long id) {
        return seguimientoRespuestaQueryService.obtenerPorId(id);
    }

    public List<SeguimientoRespuestaResponseDTO> listarPorSeguimiento(Long seguimientoId) {
        return seguimientoRespuestaQueryService.listarPorSeguimiento(seguimientoId);
    }

    public List<SeguimientoRespuestaResponseDTO> listarPendientes() {
        return seguimientoRespuestaQueryService.listarPendientes();
    }

    public SeguimientoRespuestaResponseDTO crear(Long seguimientoId, SeguimientoRespuestaRequestDTO dto) {
        return seguimientoRespuestaCommandService.crear(seguimientoId, dto);
    }

    public SeguimientoRespuestaResponseDTO actualizar(Long id, SeguimientoRespuestaRequestDTO dto) {
        return seguimientoRespuestaCommandService.actualizar(id, dto);
    }

    public SeguimientoRespuestaResponseDTO decidir(Long id, SeguimientoRespuestaDecisionDTO dto) {
        return seguimientoRespuestaCommandService.decidir(id, dto);
    }
}