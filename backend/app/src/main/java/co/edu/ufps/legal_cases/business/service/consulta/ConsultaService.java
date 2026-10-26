package co.edu.ufps.legal_cases.business.service.consulta;

import java.util.List;

import org.springframework.stereotype.Service;

import co.edu.ufps.legal_cases.business.dto.consulta.ConsultaBusquedaDTO;
import co.edu.ufps.legal_cases.business.dto.consulta.ConsultaDTO;
import co.edu.ufps.legal_cases.business.dto.consulta.ConsultaReasignacionHistorialDTO;
import co.edu.ufps.legal_cases.business.dto.consulta.ConsultaReasignacionDTO;
import co.edu.ufps.legal_cases.business.dto.consulta.ficha.FichaExpedienteDTO;
import co.edu.ufps.legal_cases.business.model.consulta.EstadoConsulta;
import co.edu.ufps.legal_cases.business.service.consulta.consulta.ConsultaCommandService;
import co.edu.ufps.legal_cases.business.service.consulta.consulta.ConsultaQueryService;
import co.edu.ufps.legal_cases.business.service.consulta.pdf.FichaExpedienteService;
import co.edu.ufps.legal_cases.business.service.consulta.reasignacion.ConsultaReasignacionHistorialQueryService;
import co.edu.ufps.legal_cases.business.service.consulta.reasignacion.ConsultaReasignacionService;
import co.edu.ufps.legal_cases.common.dto.PageResponseDTO;

// Fachada del módulo de consultas.
// El controller entra por aquí, pero lectura y escritura quedan separadas por responsabilidad.
@Service
public class ConsultaService {

    private final ConsultaQueryService consultaQueryService;
    private final ConsultaCommandService consultaCommandService;
    private final FichaExpedienteService fichaExpedienteService;
    private final ConsultaReasignacionService consultaReasignacionService;
    private final ConsultaReasignacionHistorialQueryService historialQueryService;

    public ConsultaService(
            ConsultaQueryService consultaQueryService,
            ConsultaCommandService consultaCommandService,
            FichaExpedienteService fichaExpedienteService,
            ConsultaReasignacionService consultaReasignacionService,
            ConsultaReasignacionHistorialQueryService historialQueryService) {
        this.consultaQueryService = consultaQueryService;
        this.consultaCommandService = consultaCommandService;
        this.fichaExpedienteService = fichaExpedienteService;
        this.consultaReasignacionService = consultaReasignacionService;
        this.historialQueryService = historialQueryService;
    }

    public PageResponseDTO<ConsultaBusquedaDTO> buscarParaUsuarioActual(
            String search,
            int page,
            int size,
            String sortBy,
            String direction,
            Long areaId,
            EstadoConsulta estado,
            Long asesorId,
            Long monitorId,
            Long estudianteId) {
        return consultaQueryService.buscarParaUsuarioActual(
                search,
                page,
                size,
                sortBy,
                direction,
                areaId,
                estado,
                asesorId,
                monitorId,
                estudianteId);
    }

    // Se conserva temporalmente para compatibilidad interna si alguna clase lo usa.
    public List<ConsultaBusquedaDTO> buscar(String search) {
        return consultaQueryService.buscar(search);
    }

    public List<ConsultaDTO> listar() {
        return consultaQueryService.listar();
    }

    public ConsultaDTO obtenerPorId(Long id) {
        return consultaQueryService.obtenerPorId(id);
    }

    public PageResponseDTO<ConsultaBusquedaDTO> listarArchivadas(
            String search,
            int page,
            int size,
            String sortBy,
            String direction) {
        return consultaQueryService.listarArchivadas(search, page, size, sortBy, direction);
    }

    public ConsultaDTO crear(ConsultaDTO dto) {
        return consultaCommandService.crear(dto);
    }

    public ConsultaDTO actualizar(Long id, ConsultaDTO dto) {
        return consultaCommandService.actualizar(id, dto);
    }

    public ConsultaDTO reasignarResponsable(Long id, ConsultaReasignacionDTO solicitud) {
        return consultaReasignacionService.reasignar(id, solicitud);
    }

    public PageResponseDTO<ConsultaReasignacionHistorialDTO> listarHistorialResponsables(
            Long consultaId,
            int page,
            int size) {
        return historialQueryService.listar(consultaId, page, size);
    }

    public ConsultaDTO cambiarEstado(Long id, EstadoConsulta estado, Long version) {
        return consultaCommandService.cambiarEstado(id, estado, version);
    }

    public void eliminar(Long id, Long version) {
        consultaCommandService.eliminar(id, version);
    }

    public ConsultaDTO archivar(Long id, Long version) {
        return consultaCommandService.archivar(id, version);
    }

    public ConsultaDTO desarchivar(Long id, Long version) {
        return consultaCommandService.desarchivar(id, version);
    }
    public FichaExpedienteDTO obtenerFichaExpediente(Long id) {
        return fichaExpedienteService.consolidarFichaExpediente(id);
    }

    public byte[] generarFichaPdf(Long id) {
        return fichaExpedienteService.generarFichaPdf(id);
    }
}
