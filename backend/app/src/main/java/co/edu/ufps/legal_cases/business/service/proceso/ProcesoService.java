package co.edu.ufps.legal_cases.business.service.proceso;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

import co.edu.ufps.legal_cases.business.dto.proceso.ProcesoDTO;
import co.edu.ufps.legal_cases.business.dto.proceso.ProcesoResumenDTO;
import co.edu.ufps.legal_cases.business.model.proceso.EstadoProceso;
import co.edu.ufps.legal_cases.business.service.proceso.proceso.ProcesoCommandService;
import co.edu.ufps.legal_cases.business.service.proceso.proceso.ProcesoQueryService;
import co.edu.ufps.legal_cases.common.dto.PageResponseDTO;

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
    public PageResponseDTO<ProcesoResumenDTO> buscarParaUsuarioActual(
            String search,
            int page,
            int size,
            String sortBy,
            String direction,
            EstadoProceso estado,
            LocalDate fechaDesde,
            LocalDate fechaHasta) {
        return procesoQueryService.buscarParaUsuarioActual(
                search,
                page,
                size,
                sortBy,
                direction,
                estado,
                fechaDesde,
                fechaHasta);
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

    public ProcesoDTO cambiarEstado(Long id, Boolean activo, Long version) {
        return procesoCommandService.cambiarEstado(id, activo, version);
    }

    public ProcesoDTO cambiarEstadoProceso(Long id, EstadoProceso estado, Long version) {
        return procesoCommandService.cambiarEstadoProceso(id, estado, version);
    }

    public void eliminar(Long id, Long version) {
        procesoCommandService.eliminar(id, version);
    }
}
