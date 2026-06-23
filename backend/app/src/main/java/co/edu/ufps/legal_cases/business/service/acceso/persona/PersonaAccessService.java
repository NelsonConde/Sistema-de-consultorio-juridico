package co.edu.ufps.legal_cases.business.service.acceso.persona;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.CAMBIAR_ESTADO_PERSONAS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.CREAR_PERSONAS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.EDITAR_PERSONAS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_PERSONAS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_PERSONAS;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import co.edu.ufps.legal_cases.security.service.context.UsuarioActualService;

// Este servicio valida permisos de acceso a funcionalidades relacionadas con actores 
// que manejan la informacion de personas como estudiantes y asesores
@Service
public class PersonaAccessService {

    private final UsuarioActualService usuarioActualService;

    public PersonaAccessService(UsuarioActualService usuarioActualService) {
        this.usuarioActualService = usuarioActualService;
    }

    // Por ahora personas no tiene propietario, autor ni relación directa de alcance.
    // Entonces el control se hace por permiso funcional.
    public void validarPuedeVerPersonas() {
        if (!usuarioActualService.tieneAlgunPermiso(VER_PERSONAS, GESTIONAR_PERSONAS)) {
            throw new AccessDeniedException("No tiene permisos para consultar personas");
        }
    }

    public void validarPuedeCrearPersonas() {
        if (!usuarioActualService.tieneAlgunPermiso(CREAR_PERSONAS, GESTIONAR_PERSONAS)) {
            throw new AccessDeniedException("No tiene permisos para crear personas");
        }
    }

    public void validarPuedeEditarPersonas() {
        if (!usuarioActualService.tieneAlgunPermiso(EDITAR_PERSONAS, GESTIONAR_PERSONAS)) {
            throw new AccessDeniedException("No tiene permisos para editar personas");
        }
    }

    public void validarPuedeCambiarEstadoPersonas() {
        if (!usuarioActualService.tieneAlgunPermiso(CAMBIAR_ESTADO_PERSONAS, GESTIONAR_PERSONAS)) {
            throw new AccessDeniedException("No tiene permisos para cambiar el estado de personas");
        }
    }
}