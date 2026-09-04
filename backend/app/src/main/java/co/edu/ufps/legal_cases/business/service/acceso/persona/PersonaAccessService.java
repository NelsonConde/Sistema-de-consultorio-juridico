package co.edu.ufps.legal_cases.business.service.acceso.persona;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.CAMBIAR_ESTADO_PERSONAS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.CREAR_PERSONAS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.EDITAR_PERSONAS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_PERSONAS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_PERSONAS;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.model.consulta.EstadoConsulta;
import co.edu.ufps.legal_cases.business.repository.persona.PersonaConsultaScopeRepository;
import co.edu.ufps.legal_cases.common.exception.ResourceNotFoundException;
import co.edu.ufps.legal_cases.security.dto.account.PerfilUsuarioActual;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.service.context.UsuarioActualService;

// Este servicio valida permisos de acceso a funcionalidades relacionadas con actores
// que manejan la informacion de personas como estudiantes y asesores
@Service
public class PersonaAccessService {

    private final UsuarioActualService usuarioActualService;
    private final PersonaConsultaScopeRepository personaConsultaScopeRepository;

    public PersonaAccessService(
            UsuarioActualService usuarioActualService,
            PersonaConsultaScopeRepository personaConsultaScopeRepository) {
        this.usuarioActualService = usuarioActualService;
        this.personaConsultaScopeRepository = personaConsultaScopeRepository;
    }

    // El buscador es global para quien tiene la capacidad funcional. La privacidad
    // se garantiza con el contrato minimo, no ocultando personas aun no vinculadas.
    public void validarPuedeBuscarPersonas() {
        if (!usuarioActualService.tieneAlgunPermiso(VER_PERSONAS, GESTIONAR_PERSONAS)) {
            throw new AccessDeniedException("No tiene permisos para consultar personas");
        }
    }

    public AlcanceLecturaPersonas obtenerAlcanceLecturaPersonas() {
        validarPuedeBuscarPersonas();

        if (usuarioActualService.tienePermiso(GESTIONAR_PERSONAS)) {
            return AlcanceLecturaPersonas.global();
        }

        PerfilUsuarioActual perfil = usuarioActualService.obtenerPerfilActual();
        if (perfil == null || perfil.getPerfilId() == null || perfil.getTipoPerfil() == null) {
            // Fail closed: sin perfil válido no se expone ningún dato.
            return AlcanceLecturaPersonas.restringido(null, null);
        }

        if (perfil.getTipoPerfil() == TipoPerfilUsuario.ADMINISTRATIVO) {
            return AlcanceLecturaPersonas.global();
        }

        return AlcanceLecturaPersonas.restringido(perfil.getTipoPerfil(), perfil.getPerfilId());
    }

    @Transactional(readOnly = true)
    public void validarPuedeVerDetallePersona(Long personaId) {
        // Primero verificar permiso: garantiza 403 antes que 404,
        // incluso cuando personaId es null.
        AlcanceLecturaPersonas alcance = obtenerAlcanceLecturaPersonas();

        if (personaId == null) {
            throw personaNoDisponible();
        }

        if (alcance.esGlobal()) {
            return;
        }

        boolean tieneAlcance = tieneAlcanceOperativo(
                alcance.tipoPerfil(), alcance.perfilId(), personaId);

        if (!tieneAlcance) {
            // Mismo resultado para un id inexistente y uno fuera de alcance. De esta
            // forma el detalle no funciona como oraculo de enumeracion.
            throw personaNoDisponible();
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

    private boolean tieneAlcanceOperativo(
            TipoPerfilUsuario tipoPerfil,
            Long perfilId,
            Long personaId) {
        if (tipoPerfil == null || perfilId == null) {
            return false;
        }

        return switch (tipoPerfil) {
            case ESTUDIANTE -> personaConsultaScopeRepository.existsPersonaEnConsultaDeEstudiante(
                    personaId,
                    perfilId,
                    EstadoConsulta.ARCHIVADO);
            case ASESOR -> personaConsultaScopeRepository.existsPersonaEnConsultaDeAsesor(
                    personaId,
                    perfilId,
                    EstadoConsulta.ARCHIVADO);
            case MONITOR -> personaConsultaScopeRepository.existsPersonaEnConsultaDeMonitor(
                    personaId,
                    perfilId,
                    EstadoConsulta.ARCHIVADO);
            case CONCILIADOR -> personaConsultaScopeRepository.existsPersonaEnConciliacionDeConciliador(
                    personaId,
                    perfilId,
                    EstadoConsulta.ARCHIVADO);
            case ADMINISTRATIVO -> true;
        };
    }

    private ResourceNotFoundException personaNoDisponible() {
        return new ResourceNotFoundException("Persona no encontrada");
    }
}