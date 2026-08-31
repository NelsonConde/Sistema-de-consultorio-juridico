package co.edu.ufps.legal_cases.business.service.proceso;

import java.util.List;

import org.springframework.stereotype.Service;

import co.edu.ufps.legal_cases.business.dto.proceso.ProcesoDTO;
import co.edu.ufps.legal_cases.business.model.proceso.EstadoProceso;
import co.edu.ufps.legal_cases.business.service.proceso.proceso.ProcesoCommandService;
import co.edu.ufps.legal_cases.business.service.proceso.proceso.ProcesoQueryService;

@Service
public class ProcesoService {

    private final ProcesoQueryService procesoQueryService;
    private final ProcesoCommandService procesoCommandService;

    public ProcesoService(
            ProcesoQueryService procesoQueryService,
            ProcesoCommandService procesoCommandService) {
        this.procesoQueryService = procesoQueryService;
        this.procesoCommandService = procesoCommandService;
    }

    // Fachada del módulo: el controller sigue entrando por aquí,
    // aunque por dentro lectura y escritura ya estén separadas.
    public List<ProcesoDTO> listar() {
        return procesoQueryService.listar();
    }

    public ProcesoDTO obtenerPorId(Long id) {
        return procesoQueryService.obtenerPorId(id);
    }

    public ProcesoDTO crear(ProcesoDTO dto) {
        return procesoCommandService.crear(dto);
    }

    public ProcesoDTO actualizar(Long id, ProcesoDTO dto) {
        return procesoCommandService.actualizar(id, dto);
    }

    public ProcesoDTO cambiarEstado(Long id, Boolean activo) {
        return procesoCommandService.cambiarEstado(id, activo);
    }

    public ProcesoDTO cambiarEstadoProceso(Long id, EstadoProceso estado) {
        return procesoCommandService.cambiarEstadoProceso(id, estado);
    }

    public void eliminar(Long id) {
        procesoCommandService.eliminar(id);
    }
}