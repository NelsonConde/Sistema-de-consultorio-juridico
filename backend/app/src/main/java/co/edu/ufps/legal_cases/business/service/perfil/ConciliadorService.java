package co.edu.ufps.legal_cases.business.service.perfil;

import java.util.List;

import org.springframework.stereotype.Service;

import co.edu.ufps.legal_cases.business.dto.perfil.ConciliadorDTO;
import co.edu.ufps.legal_cases.business.dto.perfil.ConciliadorResumenDTO;
import co.edu.ufps.legal_cases.business.model.perfil.TipoConciliador;
import co.edu.ufps.legal_cases.business.service.perfil.conciliador.ConciliadorCommandService;
import co.edu.ufps.legal_cases.business.service.perfil.conciliador.ConciliadorQueryService;
import co.edu.ufps.legal_cases.common.dto.PageResponseDTO;

@Service
public class ConciliadorService {

    private final ConciliadorQueryService conciliadorQueryService;
    private final ConciliadorCommandService conciliadorCommandService;

    public ConciliadorService(
            ConciliadorQueryService conciliadorQueryService,
            ConciliadorCommandService conciliadorCommandService) {
        this.conciliadorQueryService = conciliadorQueryService;
        this.conciliadorCommandService = conciliadorCommandService;
    }

    // Fachada del módulo de conciliadores.
    // El controller entra por aquí, pero lectura y escritura quedan separadas por responsabilidad.
    public PageResponseDTO<ConciliadorResumenDTO> buscar(
            String search,
            int page,
            int size,
            String sortBy,
            String direction,
            Boolean activo,
            TipoConciliador tipoConciliador) {
        return conciliadorQueryService.buscar(search, page, size, sortBy, direction, activo, tipoConciliador);
    }

    public List<ConciliadorDTO> listarActivos() {
        return conciliadorQueryService.listarActivos();
    }

    public ConciliadorDTO obtenerPorId(Long id) {
        return conciliadorQueryService.obtenerPorId(id);
    }

    public ConciliadorDTO crear(ConciliadorDTO dto) {
        return conciliadorCommandService.crear(dto);
    }

    public ConciliadorDTO actualizar(Long id, ConciliadorDTO dto) {
        return conciliadorCommandService.actualizar(id, dto);
    }

    public ConciliadorDTO cambiarEstado(Long id, Boolean activo) {
        return conciliadorCommandService.cambiarEstado(id, activo);
    }

    public void eliminar(Long id) {
        conciliadorCommandService.eliminar(id);
    }
}