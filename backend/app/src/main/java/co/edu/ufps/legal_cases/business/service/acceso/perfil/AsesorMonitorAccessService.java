package co.edu.ufps.legal_cases.business.service.acceso.perfil;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_ASESORES_MONITORES;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_USUARIOS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_ASESORES_MONITORES;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_PERFILES_AUXILIARES;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import co.edu.ufps.legal_cases.security.service.context.UsuarioActualService;

@Service
public class AsesorMonitorAccessService {

    private final UsuarioActualService usuarioActualService;

    public AsesorMonitorAccessService(UsuarioActualService usuarioActualService) {
        this.usuarioActualService = usuarioActualService;
    }

    // Replica el acceso de los listados administrativos de asesores y monitores.
    public void validarPuedeListarAsesoresYMonitores() {
        if (!usuarioActualService.tieneAlgunPermiso(
                VER_ASESORES_MONITORES,
                GESTIONAR_ASESORES_MONITORES,
                GESTIONAR_USUARIOS)) {
            throw new AccessDeniedException("No tiene permisos para consultar asesores y monitores");
        }
    }

    // Este flujo también se usa para combos, por eso acepta el permiso de perfiles auxiliares.
    public void validarPuedeListarAsesoresYMonitoresActivos() {
        if (!usuarioActualService.tieneAlgunPermiso(
                VER_PERFILES_AUXILIARES,
                VER_ASESORES_MONITORES,
                GESTIONAR_ASESORES_MONITORES,
                GESTIONAR_USUARIOS)) {
            throw new AccessDeniedException("No tiene permisos para consultar asesores y monitores activos");
        }
    }

    // Replica los permisos actuales para crear, editar, cambiar estado o eliminar.
    public void validarPuedeGestionarAsesoresYMonitores() {
        if (!usuarioActualService.tieneAlgunPermiso(
                GESTIONAR_ASESORES_MONITORES,
                GESTIONAR_USUARIOS)) {
            throw new AccessDeniedException("No tiene permisos para gestionar asesores y monitores");
        }
    }
}