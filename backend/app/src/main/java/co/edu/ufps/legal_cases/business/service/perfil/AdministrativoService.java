package co.edu.ufps.legal_cases.business.service.perfil;

import java.util.List;

import org.springframework.stereotype.Service;

import co.edu.ufps.legal_cases.business.dto.perfil.AdministrativoDTO;
import co.edu.ufps.legal_cases.business.service.perfil.administrativo.AdministrativoCommandService;
import co.edu.ufps.legal_cases.business.service.perfil.administrativo.AdministrativoQueryService;

@Service
public class AdministrativoService {

    private final AdministrativoQueryService administrativoQueryService;
    private final AdministrativoCommandService administrativoCommandService;

    public AdministrativoService(
            AdministrativoQueryService administrativoQueryService,
            AdministrativoCommandService administrativoCommandService) {
        this.administrativoQueryService = administrativoQueryService;
        this.administrativoCommandService = administrativoCommandService;
    }

    // Fachada del módulo de administrativos.
    // El controller entra por aquí, pero lectura y escritura quedan separadas por responsabilidad.
    public List<AdministrativoDTO> listar() {
        return administrativoQueryService.listar();
    }

    public List<AdministrativoDTO> listarActivos() {
        return administrativoQueryService.listarActivos();
    }

    public List<AdministrativoDTO> listarDirectoras() {
        return administrativoQueryService.listarDirectoras();
    }

    public AdministrativoDTO obtenerPorId(Long id) {
        return administrativoQueryService.obtenerPorId(id);
    }

    public AdministrativoDTO crear(AdministrativoDTO dto) {
        return administrativoCommandService.crear(dto);
    }

    public AdministrativoDTO actualizar(Long id, AdministrativoDTO dto) {
        return administrativoCommandService.actualizar(id, dto);
    }

    public AdministrativoDTO cambiarEstado(Long id, Boolean activo) {
        return administrativoCommandService.cambiarEstado(id, activo);
    }

    public AdministrativoDTO cambiarDirectora(Long id, Boolean directora) {
        return administrativoCommandService.cambiarDirectora(id, directora);
    }

    public void eliminar(Long id) {
        administrativoCommandService.eliminar(id);
    }
}