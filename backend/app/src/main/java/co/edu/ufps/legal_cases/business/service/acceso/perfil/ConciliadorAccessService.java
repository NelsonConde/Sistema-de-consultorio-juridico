package co.edu.ufps.legal_cases.business.service.acceso.perfil;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_CONCILIADORES;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_USUARIOS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_CONCILIADORES;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_PERFILES_AUXILIARES;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import co.edu.ufps.legal_cases.security.service.context.UsuarioActualService;

@Service
public class ConciliadorAccessService {

    private final UsuarioActualService usuarioActualService;

    public ConciliadorAccessService(UsuarioActualService usuarioActualService) {
        this.usuarioActualService = usuarioActualService;
    }

    // Replica el acceso de los listados administrativos de conciliadores.
    public void validarPuedeListarConciliadores() {
        if (!usuarioActualService.tieneAlgunPermiso(
                VER_CONCILIADORES,
                GESTIONAR_CONCILIADORES,
                GESTIONAR_USUARIOS)) {
            throw new AccessDeniedException("No tiene permisos para consultar conciliadores");
        }
    }

    // Este flujo también se usa para combos, por eso acepta el permiso de perfiles auxiliares.
    public void validarPuedeListarConciliadoresActivos() {
        if (!usuarioActualService.tieneAlgunPermiso(
                VER_PERFILES_AUXILIARES,
                VER_CONCILIADORES,
                GESTIONAR_CONCILIADORES,
                GESTIONAR_USUARIOS)) {
            throw new AccessDeniedException("No tiene permisos para consultar conciliadores activos");
        }
    }

    // Replica los permisos actuales para crear, editar, cambiar estado o eliminar.
    public void validarPuedeGestionarConciliadores() {
        if (!usuarioActualService.tieneAlgunPermiso(
                GESTIONAR_CONCILIADORES,
                GESTIONAR_USUARIOS)) {
            throw new AccessDeniedException("No tiene permisos para gestionar conciliadores");
        }
    }
}