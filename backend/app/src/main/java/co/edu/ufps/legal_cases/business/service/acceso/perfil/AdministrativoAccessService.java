package co.edu.ufps.legal_cases.business.service.acceso.perfil;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.model.perfil.Administrativo;
import co.edu.ufps.legal_cases.business.repository.perfil.AdministrativoRepository;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.service.context.UsuarioActualService;

// Este servicio regula funcionalidades de gestion de administrativos, solo un director admin puede gestionar administrativos
// Al igual que en consulta donde esta comentado el paso a paso de validacion
@Service
public class AdministrativoAccessService {

    private final UsuarioActualService usuarioActualService;
    private final AdministrativoRepository administrativoRepository;

    public AdministrativoAccessService(
            UsuarioActualService usuarioActualService,
            AdministrativoRepository administrativoRepository) {
        this.usuarioActualService = usuarioActualService;
        this.administrativoRepository = administrativoRepository;
    }

    // Valida la regla especial del sistema:
    // solo una administrativa marcada como directora puede gestionar administradores.
    @Transactional(readOnly = true)
    public void validarPuedeGestionarAdministradores() {
        if (!usuarioActualService.esRolAdministrador()) {
            throw new AccessDeniedException("Solo un administrador puede gestionar administrativos");
        }

        Administrativo administrativoActual = obtenerAdministrativoActual();

        if (!Boolean.TRUE.equals(administrativoActual.getDirectora())) {
            throw new AccessDeniedException("Solo la directora puede gestionar administrativos");
        }
    }

    @Transactional(readOnly = true)
    public void validarPuedeVerAdministradores() {
        if (!usuarioActualService.esRolAdministrador()) {
            throw new AccessDeniedException("Solo un administrador puede consultar administrativos");
        }
    }

    private Administrativo obtenerAdministrativoActual() {
        Long usuarioActualId = usuarioActualService.obtenerUsuarioActualId();

        return administrativoRepository.findByUsuarioSistema_IdAndActivoTrue(usuarioActualId)
                .orElseThrow(() -> new BusinessException(
                        "El usuario actual no tiene un perfil administrativo activo"));
    }
}