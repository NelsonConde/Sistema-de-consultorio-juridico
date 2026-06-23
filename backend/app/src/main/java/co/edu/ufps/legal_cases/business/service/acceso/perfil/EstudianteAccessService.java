package co.edu.ufps.legal_cases.business.service.acceso.perfil;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.CAMBIAR_ESTADO_ESTUDIANTES;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_USUARIOS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_ESTUDIANTES;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_PERFILES_AUXILIARES;

import java.util.Objects;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.model.perfil.Estudiante;
import co.edu.ufps.legal_cases.business.repository.perfil.EstudianteRepository;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.dto.account.PerfilUsuarioActual;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.service.context.UsuarioActualService;

// Este servicio valida permisos relacionados con estudiantes
// Al igual que en consulta donde esta comentado el paso a paso de validacion
@Service
public class EstudianteAccessService {

    private final UsuarioActualService usuarioActualService;
    private final EstudianteRepository estudianteRepository;

    public EstudianteAccessService(
            UsuarioActualService usuarioActualService,
            EstudianteRepository estudianteRepository) {
        this.usuarioActualService = usuarioActualService;
        this.estudianteRepository = estudianteRepository;
    }

    // Valida que el usuario pueda consultar estudiantes.
    // El alcance real se aplica luego en el QueryService.
    @Transactional(readOnly = true)
    public void validarPuedeListarEstudiantes() {
        if (!usuarioActualService.tieneAlgunPermiso(
                VER_ESTUDIANTES,
                VER_PERFILES_AUXILIARES,
                GESTIONAR_USUARIOS)) {
            throw new AccessDeniedException("No tiene permisos para consultar estudiantes");
        }
    }

    @Transactional(readOnly = true)
    public void validarPuedeListarEstudiantesPorAsesor(Long asesorId) {
        validarPuedeListarEstudiantes();

        if (asesorId == null) {
            throw new BusinessException("El id del asesor es obligatorio");
        }

        if (usuarioActualService.esRolAdministrador()) {
            return;
        }

        if (usuarioActualService.esAsesor()
                && Objects.equals(obtenerPerfilActualId(), asesorId)) {
            return;
        }

        throw new AccessDeniedException("No tiene permisos para consultar estudiantes de este asesor");
    }

    @Transactional(readOnly = true)
    public void validarPuedeVerEstudiante(Long estudianteId) {
        validarPuedeListarEstudiantes();

        Estudiante estudiante = obtenerEstudiante(estudianteId);

        if (!puedeVerEstudiante(estudiante)) {
            throw new AccessDeniedException("No tiene permisos para consultar este estudiante");
        }
    }

    // La gestión real de estudiantes queda restringida al administrador.
    // Esto evita que permisos viejos temporales den acceso indebido.
    @Transactional(readOnly = true)
    public void validarPuedeGestionarEstudiantes() {
        if (!usuarioActualService.esRolAdministrador()) {
            throw new AccessDeniedException("Solo el administrador puede gestionar estudiantes");
        }
    }

    @Transactional(readOnly = true)
    public void validarPuedeCambiarEstadoEstudiante() {
        if (!usuarioActualService.tieneAlgunPermiso(
                CAMBIAR_ESTADO_ESTUDIANTES,
                GESTIONAR_USUARIOS)) {
            throw new AccessDeniedException("No tiene permisos para cambiar el estado de estudiantes");
        }

        if (!usuarioActualService.esRolAdministrador()) {
            throw new AccessDeniedException("Solo el administrador puede cambiar el estado de estudiantes");
        }
    }

    @Transactional(readOnly = true)
    public boolean puedeVerTodosLosEstudiantes() {
        return usuarioActualService.esRolAdministrador();
    }

    @Transactional(readOnly = true)
    public boolean usuarioEsAsesor() {
        return usuarioActualService.esAsesor();
    }

    @Transactional(readOnly = true)
    public Long obtenerAsesorActualId() {
        PerfilUsuarioActual perfil = usuarioActualService.obtenerPerfilActual();

        if (perfil.getTipoPerfil() != TipoPerfilUsuario.ASESOR) {
            throw new AccessDeniedException("El usuario actual no es asesor");
        }

        return perfil.getPerfilId();
    }

    @Transactional(readOnly = true)
    public boolean puedeVerEstudiante(Estudiante estudiante) {
        if (estudiante == null) {
            return false;
        }

        if (usuarioActualService.esRolAdministrador()) {
            return true;
        }

        if (usuarioActualService.esAsesor()) {
            Long asesorActualId = obtenerPerfilActualId();

            return estudiante.getAsesor() != null
                    && Objects.equals(estudiante.getAsesor().getId(), asesorActualId);
        }

        return false;
    }

    private Long obtenerPerfilActualId() {
        return usuarioActualService.obtenerPerfilActual().getPerfilId();
    }

    private Estudiante obtenerEstudiante(Long estudianteId) {
        if (estudianteId == null) {
            throw new BusinessException("El id del estudiante es obligatorio");
        }

        return estudianteRepository.findById(estudianteId)
                .orElseThrow(() -> new BusinessException("Estudiante no encontrado con id: " + estudianteId));
    }
}