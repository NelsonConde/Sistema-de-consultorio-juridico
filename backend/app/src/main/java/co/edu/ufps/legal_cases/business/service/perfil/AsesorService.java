package co.edu.ufps.legal_cases.business.service.perfil;

import java.util.List;

import org.springframework.stereotype.Service;

import co.edu.ufps.legal_cases.business.dto.perfil.AsesorDTO;
import co.edu.ufps.legal_cases.business.service.perfil.asesor.AsesorCommandService;
import co.edu.ufps.legal_cases.business.service.perfil.asesor.AsesorQueryService;

@Service
public class AsesorService {

    private final AsesorQueryService asesorQueryService;
    private final AsesorCommandService asesorCommandService;

    public AsesorService(
            AsesorQueryService asesorQueryService,
            AsesorCommandService asesorCommandService) {
        this.asesorQueryService = asesorQueryService;
        this.asesorCommandService = asesorCommandService;
    }

    // Fachada del módulo de asesores.
    // El controller entra por aquí, pero lectura y escritura quedan separadas por responsabilidad.
    public List<AsesorDTO> listar() {
        return asesorQueryService.listar();
    }

    public List<AsesorDTO> listarActivos() {
        return asesorQueryService.listarActivos();
    }

    public AsesorDTO obtenerPorId(Long id) {
        return asesorQueryService.obtenerPorId(id);
    }

    public AsesorDTO crear(AsesorDTO dto) {
        return asesorCommandService.crear(dto);
    }

    public AsesorDTO actualizar(Long id, AsesorDTO dto) {
        return asesorCommandService.actualizar(id, dto);
    }

    public AsesorDTO cambiarEstado(Long id, Boolean activo) {
        return asesorCommandService.cambiarEstado(id, activo);
    }

    public void eliminar(Long id) {
        asesorCommandService.eliminar(id);
    }
}