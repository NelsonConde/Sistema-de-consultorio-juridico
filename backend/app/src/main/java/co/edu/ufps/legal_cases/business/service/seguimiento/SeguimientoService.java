package co.edu.ufps.legal_cases.business.service.seguimiento;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import co.edu.ufps.legal_cases.business.dto.seguimiento.SeguimientoRequestDTO;
import co.edu.ufps.legal_cases.business.dto.seguimiento.SeguimientoResponseDTO;
import co.edu.ufps.legal_cases.business.dto.seguimiento.SeguimientoResumenDTO;
import co.edu.ufps.legal_cases.business.model.seguimiento.EstadoSeguimiento;
import co.edu.ufps.legal_cases.business.repository.seguimiento.SeguimientoAgendaProjection;
import co.edu.ufps.legal_cases.business.service.seguimiento.seguimiento.SeguimientoCommandService;
import co.edu.ufps.legal_cases.business.service.seguimiento.seguimiento.SeguimientoQueryService;
import co.edu.ufps.legal_cases.common.dto.PageResponseDTO;

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
    public PageResponseDTO<SeguimientoResumenDTO> buscarParaUsuarioActual(
            String search,
            int page,
            int size,
            String sortBy,
            String direction,
            EstadoSeguimiento estado,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            Long consultaId,
            Long autorId) {
        return seguimientoQueryService.buscarParaUsuarioActual(
                search,
                page,
                size,
                sortBy,
                direction,
                estado,
                fechaDesde,
                fechaHasta,
                consultaId,
                autorId);
    }

    public List<SeguimientoResponseDTO> listarPorConsulta(Long consultaId) {
        return seguimientoQueryService.listarPorConsulta(consultaId);
    }

    public List<SeguimientoResponseDTO> listarVisiblesParaEstudiantePorConsulta(Long consultaId) {
        return seguimientoQueryService.listarVisiblesParaEstudiantePorConsulta(consultaId);
    }

    public List<SeguimientoResponseDTO> listarPorAutor(Long autorId) {
        return seguimientoQueryService.listarPorAutor(autorId);
    }

    // Calendario por rango – Bloque B (SCRUM-269).
    // Reemplaza listarParaCalendario() que usaba findAll() + filtros en memoria.
    // El endpoint GET /api/seguimientos/calendario ahora requiere from y to.
    public List<SeguimientoResponseDTO> listarCalendarioPorRango(LocalDate from, LocalDate to) {
        return seguimientoQueryService.listarCalendarioPorRango(from, to);
    }

    // Para Agenda – Bloque B (SCRUM-269).
    // Resuelve scope internamente; AgendaQueryService solo pasa el rango.
    public List<SeguimientoAgendaProjection> buscarParaAgenda(LocalDate from, LocalDate to) {
        return seguimientoQueryService.buscarParaAgenda(from, to);
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

    public SeguimientoResponseDTO cambiarEstadoSeguimiento(Long id, EstadoSeguimiento estado, Long version) {
        return seguimientoCommandService.cambiarEstadoSeguimiento(id, estado, version);
    }

    public void eliminar(Long id, Long version) {
        seguimientoCommandService.eliminar(id, version);
    }
}
