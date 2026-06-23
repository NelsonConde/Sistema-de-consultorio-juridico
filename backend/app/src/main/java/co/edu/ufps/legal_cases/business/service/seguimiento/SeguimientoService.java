package co.edu.ufps.legal_cases.business.service.seguimiento;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import co.edu.ufps.legal_cases.business.dto.seguimiento.SeguimientoRequestDTO;
import co.edu.ufps.legal_cases.business.dto.seguimiento.SeguimientoResponseDTO;
import co.edu.ufps.legal_cases.business.model.seguimiento.EstadoSeguimiento;
import co.edu.ufps.legal_cases.business.service.seguimiento.seguimiento.SeguimientoCommandService;
import co.edu.ufps.legal_cases.business.service.seguimiento.seguimiento.SeguimientoQueryService;

@Service
public class SeguimientoService {

    private final SeguimientoQueryService seguimientoQueryService;
    private final SeguimientoCommandService seguimientoCommandService;

    public SeguimientoService(
            SeguimientoQueryService seguimientoQueryService,
            SeguimientoCommandService seguimientoCommandService) {
        this.seguimientoQueryService = seguimientoQueryService;
        this.seguimientoCommandService = seguimientoCommandService;
    }

    // Fachada del módulo: el controller sigue usando este service,
    // mientras la lectura y la escritura quedan separadas por dentro.
    public List<SeguimientoResponseDTO> listarPorConsulta(Long consultaId) {
        return seguimientoQueryService.listarPorConsulta(consultaId);
    }

    public List<SeguimientoResponseDTO> listarVisiblesParaEstudiantePorConsulta(Long consultaId) {
        return seguimientoQueryService.listarVisiblesParaEstudiantePorConsulta(consultaId);
    }

    public List<SeguimientoResponseDTO> listarPorAutor(Long autorId) {
        return seguimientoQueryService.listarPorAutor(autorId);
    }

    public List<SeguimientoResponseDTO> listarParaCalendario() {
        return seguimientoQueryService.listarParaCalendario();
    }

    public List<SeguimientoResponseDTO> listarAlertasDisciplinarias() {
        return seguimientoQueryService.listarAlertasDisciplinarias();
    }

    public List<SeguimientoResponseDTO> listarPorFechaEntrega(LocalDate fechaEntrega) {
        return seguimientoQueryService.listarPorFechaEntrega(fechaEntrega);
    }

    public SeguimientoResponseDTO obtenerPorId(Long id) {
        return seguimientoQueryService.obtenerPorId(id);
    }

    public SeguimientoResponseDTO crear(SeguimientoRequestDTO dto) {
        return seguimientoCommandService.crear(dto);
    }

    public SeguimientoResponseDTO actualizar(Long id, SeguimientoRequestDTO dto) {
        return seguimientoCommandService.actualizar(id, dto);
    }

    public SeguimientoResponseDTO cambiarEstadoSeguimiento(Long id, EstadoSeguimiento estado) {
        return seguimientoCommandService.cambiarEstadoSeguimiento(id, estado);
    }

    public void eliminar(Long id) {
        seguimientoCommandService.eliminar(id);
    }
}