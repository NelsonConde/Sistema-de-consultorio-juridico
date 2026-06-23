package co.edu.ufps.legal_cases.business.service.consulta;

import java.util.List;

import org.springframework.stereotype.Service;

import co.edu.ufps.legal_cases.business.dto.consulta.ConsultaBusquedaDTO;
import co.edu.ufps.legal_cases.business.dto.consulta.ConsultaDTO;
import co.edu.ufps.legal_cases.business.model.consulta.EstadoConsulta;
import co.edu.ufps.legal_cases.business.service.consulta.consulta.ConsultaCommandService;
import co.edu.ufps.legal_cases.business.service.consulta.consulta.ConsultaQueryService;

// Fachada del módulo de consultas.
// El controller entra por aquí, pero lectura y escritura quedan separadas por responsabilidad.
@Service
public class ConsultaService {

    private final ConsultaQueryService consultaQueryService;
    private final ConsultaCommandService consultaCommandService;

    public ConsultaService(
            ConsultaQueryService consultaQueryService,
            ConsultaCommandService consultaCommandService) {
        this.consultaQueryService = consultaQueryService;
        this.consultaCommandService = consultaCommandService;
    }

    public List<ConsultaBusquedaDTO> buscarParaUsuarioActual(String search) {
        return consultaQueryService.buscarParaUsuarioActual(search);
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

    public List<ConsultaBusquedaDTO> listarArchivadas() {
        return consultaQueryService.listarArchivadas();
    }

    public ConsultaDTO crear(ConsultaDTO dto) {
        return consultaCommandService.crear(dto);
    }

    public ConsultaDTO actualizar(Long id, ConsultaDTO dto) {
        return consultaCommandService.actualizar(id, dto);
    }

    public ConsultaDTO cambiarEstado(Long id, EstadoConsulta estado) {
        return consultaCommandService.cambiarEstado(id, estado);
    }

    public void eliminar(Long id) {
        consultaCommandService.eliminar(id);
    }

    public ConsultaDTO archivar(Long id) {
        return consultaCommandService.archivar(id);
    }

    public ConsultaDTO desarchivar(Long id) {
        return consultaCommandService.desarchivar(id);
    }
}