package co.edu.ufps.legal_cases.business.service.perfil;

import java.util.List;

import org.springframework.stereotype.Service;

import co.edu.ufps.legal_cases.business.dto.perfil.MonitorDTO;
import co.edu.ufps.legal_cases.business.service.perfil.monitor.MonitorCommandService;
import co.edu.ufps.legal_cases.business.service.perfil.monitor.MonitorQueryService;

@Service
public class MonitorService {

    private final MonitorQueryService monitorQueryService;
    private final MonitorCommandService monitorCommandService;

    public MonitorService(
            MonitorQueryService monitorQueryService,
            MonitorCommandService monitorCommandService) {
        this.monitorQueryService = monitorQueryService;
        this.monitorCommandService = monitorCommandService;
    }

    // Fachada del módulo de monitores.
    // El controller entra por aquí, pero lectura y escritura quedan separadas por responsabilidad.
    public List<MonitorDTO> listar() {
        return monitorQueryService.listar();
    }

    public List<MonitorDTO> listarActivos() {
        return monitorQueryService.listarActivos();
    }

    public MonitorDTO obtenerPorId(Long id) {
        return monitorQueryService.obtenerPorId(id);
    }

    public MonitorDTO crear(MonitorDTO dto) {
        return monitorCommandService.crear(dto);
    }

    public MonitorDTO actualizar(Long id, MonitorDTO dto) {
        return monitorCommandService.actualizar(id, dto);
    }

    public MonitorDTO cambiarEstado(Long id, Boolean activo) {
        return monitorCommandService.cambiarEstado(id, activo);
    }

    public void eliminar(Long id) {
        monitorCommandService.eliminar(id);
    }
}