package co.edu.ufps.legal_cases.business.service.conciliacion;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import co.edu.ufps.legal_cases.business.dto.conciliacion.ConciliacionDetalleResponseDTO;
import co.edu.ufps.legal_cases.business.dto.conciliacion.ConciliacionResponseDTO;
import co.edu.ufps.legal_cases.business.dto.conciliacion.ConciliacionResumenDTO;
import co.edu.ufps.legal_cases.business.dto.conciliacion.reunion.ReunionConciliacionRequestDTO;
import co.edu.ufps.legal_cases.business.dto.conciliacion.reunion.ReunionConciliacionResponseDTO;
import co.edu.ufps.legal_cases.business.service.conciliacion.conciliacion.ConciliacionCommandService;
import co.edu.ufps.legal_cases.business.service.conciliacion.conciliacion.ConciliacionQueryService;
import co.edu.ufps.legal_cases.business.service.conciliacion.reunion.ReunionConciliacionService;
import co.edu.ufps.legal_cases.common.dto.PageResponseDTO;
import lombok.AllArgsConstructor;

// Fachada del módulo de conciliación.
// El controller usa este service y no conoce CommandService ni QueryService.
@Service
@AllArgsConstructor
public class ConciliacionService {

    private final ConciliacionCommandService conciliacionCommandService;
    private final ConciliacionQueryService conciliacionQueryService;
    private final ReunionConciliacionService reunionConciliacionService;

    public PageResponseDTO<ConciliacionResumenDTO> buscarParaUsuarioActual(
            String search,
            int page,
            int size,
            String sortBy,
            String direction,
            String estado,
            LocalDate fechaDesde,
            LocalDate fechaHasta) {
        return conciliacionQueryService.buscarParaUsuarioActual(
                search,
                page,
                size,
                sortBy,
                direction,
                estado,
                fechaDesde,
                fechaHasta);
    }

    public List<ConciliacionResponseDTO> listarPorConsulta(Long consultaId) {
        return conciliacionQueryService.listarPorConsulta(consultaId);
    }

    public ConciliacionDetalleResponseDTO obtenerDetalle(Long id) {
        return conciliacionQueryService.obtenerDetalle(id);
    }

    public ConciliacionResponseDTO crearDesdeConsulta(Long consultaId, MultipartFile solicitud) {
        return conciliacionCommandService.crearDesdeConsulta(consultaId, solicitud);
    }

    public ReunionConciliacionResponseDTO programarReunion(Long conciliacionId, ReunionConciliacionRequestDTO dto) {
        return reunionConciliacionService.programar(conciliacionId, dto);
    }

    public ReunionConciliacionResponseDTO reprogramarReunion(Long conciliacionId, ReunionConciliacionRequestDTO dto) {
        return reunionConciliacionService.reprogramar(conciliacionId, dto);
    }

    public ConciliacionResponseDTO asignarEstudiante(Long id, Long estudianteId, Long version) {
        return conciliacionCommandService.asignarEstudiante(id, estudianteId, version);
    }

    public ConciliacionResponseDTO asignarConciliador(Long id, Long conciliadorId, Long version) {
        return conciliacionCommandService.asignarConciliador(id, conciliadorId, version);
    }

    public ConciliacionResponseDTO cambiarEstado(Long id, String estado, Long version) {
        return conciliacionCommandService.cambiarEstado(id, estado, version);
    }

    public ConciliacionResponseDTO finalizar(Long id, String estado, MultipartFile acta, Long version) {
        return conciliacionCommandService.finalizar(id, estado, acta, version);
    }

    public ConciliacionResponseDTO reemplazarSolicitud(Long id, MultipartFile solicitud, Long version) {
        return conciliacionCommandService.reemplazarSolicitud(id, solicitud, version);
    }

    public void desactivar(Long id, Long version) {
        conciliacionCommandService.desactivar(id, version);
    }
}
