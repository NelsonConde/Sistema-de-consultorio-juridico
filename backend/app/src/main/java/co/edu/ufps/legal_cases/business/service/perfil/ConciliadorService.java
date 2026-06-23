package co.edu.ufps.legal_cases.business.service.perfil;

import java.util.List;

import org.springframework.stereotype.Service;

import co.edu.ufps.legal_cases.business.dto.perfil.ConciliadorDTO;
import co.edu.ufps.legal_cases.business.service.perfil.conciliador.ConciliadorCommandService;
import co.edu.ufps.legal_cases.business.service.perfil.conciliador.ConciliadorQueryService;

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
    public List<ConciliadorDTO> listar() {
        return conciliadorQueryService.listar();
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