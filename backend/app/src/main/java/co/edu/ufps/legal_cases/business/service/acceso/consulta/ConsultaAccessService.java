package co.edu.ufps.legal_cases.business.service.acceso.consulta;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.ARCHIVAR_CONSULTAS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.ASIGNAR_RESPONSABLES_CONSULTA;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.CAMBIAR_ESTADO_CONSULTAS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.CREAR_CONSULTAS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.EDITAR_CONSULTAS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_CONSULTAS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_CONSULTAS;

import java.util.Objects;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.model.consulta.Consulta;
import co.edu.ufps.legal_cases.business.model.consulta.EstadoConsulta;
import co.edu.ufps.legal_cases.business.repository.consulta.ConsultaRepository;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.dto.account.PerfilUsuarioActual;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.service.context.UsuarioActualService;

@Service
public class ConsultaAccessService {

    private final UsuarioActualService usuarioActualService;
    private final ConsultaRepository consultaRepository;

    public ConsultaAccessService(
            UsuarioActualService usuarioActualService,
            ConsultaRepository consultaRepository) {
        this.usuarioActualService = usuarioActualService;
        this.consultaRepository = consultaRepository;
    }

    // Replica el acceso del buscador de consultas y luego QueryService filtra por
    // alcance.
    @Transactional(readOnly = true)
    public void validarPuedeBuscarConsultas() {
        validarTieneAlgunPermiso(VER_CONSULTAS, GESTIONAR_CONSULTAS);
    }

    @Transactional(readOnly = true)
    public void validarPuedeVerConsulta(Long consultaId) {
        validarTieneAlgunPermiso(VER_CONSULTAS, GESTIONAR_CONSULTAS);

        Consulta consulta = obtenerConsulta(consultaId);

        if (!puedeAccederAConsulta(consulta)) {
            throw new AccessDeniedException("No tiene permisos para ver esta consulta");
        }
    }

    @Transactional(readOnly = true)
    public void validarPuedeCrearConsulta() {
        validarTieneAlgunPermiso(CREAR_CONSULTAS, GESTIONAR_CONSULTAS);
    }

    @Transactional(readOnly = true)
    public void validarPuedeEditarConsulta(Long consultaId) {
        validarTieneAlgunPermiso(EDITAR_CONSULTAS, GESTIONAR_CONSULTAS);

        Consulta consulta = obtenerConsulta(consultaId);

        if (!puedeAccederAConsulta(consulta)) {
            throw new AccessDeniedException("No tiene permisos para editar esta consulta");
        }
    }

    @Transactional(readOnly = true)
    public void validarPuedeCambiarEstadoConsulta(Long consultaId) {
        validarTieneAlgunPermiso(CAMBIAR_ESTADO_CONSULTAS, GESTIONAR_CONSULTAS);

        Consulta consulta = obtenerConsulta(consultaId);

        if (!puedeAccederAConsulta(consulta)) {
            throw new AccessDeniedException("No tiene permisos para cambiar el estado de esta consulta");
        }

        if (usuarioActualService.esEstudiante()) {
            throw new AccessDeniedException("El estudiante no puede cambiar el estado de la consulta");
        }
    }

    @Transactional(readOnly = true)
    public void validarPuedeCambiarEstadoConsulta(Long consultaId, EstadoConsulta estadoNuevo) {
        if (estadoNuevo == null) {
            throw new BusinessException("El estado es obligatorio");
        }

        if (EstadoConsulta.ARCHIVADO.equals(estadoNuevo)) {
            validarPuedeArchivarConsulta(consultaId);
            return;
        }

        validarPuedeCambiarEstadoConsulta(consultaId);
    }

    @Transactional(readOnly = true)
    public void validarPuedeArchivarConsulta(Long consultaId) {
        validarTienePermiso(ARCHIVAR_CONSULTAS);

        if (!usuarioActualService.esRolAdministrador()) {
            throw new AccessDeniedException("Solo el administrador puede archivar consultas");
        }

        obtenerConsulta(consultaId);
    }

    @Transactional(readOnly = true)
    public void validarPuedeDesarchivarConsulta(Long consultaId) {
        // Desarchivar usa la misma política de permisos que archivar:
        // permiso ARCHIVAR_CONSULTAS y rol administrador.
        validarPuedeArchivarConsulta(consultaId);
    }

    @Transactional(readOnly = true)
    public void validarPuedeListarConsultasArchivadas() {
        validarTienePermiso(ARCHIVAR_CONSULTAS);

        if (!usuarioActualService.esRolAdministrador()) {
            throw new AccessDeniedException("Solo el administrador puede consultar consultas archivadas");
        }
    }

    @Transactional(readOnly = true)
    public void validarPuedeAsignarResponsablesConsulta() {
        validarTienePermiso(ASIGNAR_RESPONSABLES_CONSULTA);

        if (usuarioActualService.esEstudiante()) {
            throw new AccessDeniedException("El estudiante no puede asignar responsables de consulta");
        }
    }

    @Transactional(readOnly = true)
    public void validarPuedeAsignarResponsablesConsultaSiAplica(boolean solicitaAsignacionResponsables) {
        if (solicitaAsignacionResponsables) {
            validarPuedeAsignarResponsablesConsulta();
        }
    }

    @Transactional(readOnly = true)
    public boolean puedeVerTodasLasConsultas() {
        return usuarioActualService.esRolAdministrador();
    }

    @Transactional(readOnly = true)
    public boolean puedeAccederAConsulta(Long consultaId) {
        Consulta consulta = obtenerConsulta(consultaId);
        return puedeAccederAConsulta(consulta);
    }

    @Transactional(readOnly = true)
    public boolean puedeAccederAConsulta(Consulta consulta) {
        if (consulta == null) {
            return false;
        }

        // Administrador ve todas las consultas; los demás dependen de relación con el
        // caso.
        if (usuarioActualService.esRolAdministrador()) {
            return true;
        }

        PerfilUsuarioActual perfil = usuarioActualService.obtenerPerfilActual();
        Long perfilId = perfil.getPerfilId();

        if (perfil.getTipoPerfil() == TipoPerfilUsuario.ESTUDIANTE) {
            return consulta.getEstudiante() != null
                    && Objects.equals(consulta.getEstudiante().getId(), perfilId);
        }

        if (perfil.getTipoPerfil() == TipoPerfilUsuario.ASESOR) {
            boolean asesorAsignado = consulta.getAsesor() != null
                    && Objects.equals(consulta.getAsesor().getId(), perfilId);

            boolean estudianteDelAsesor = consulta.getEstudiante() != null
                    && consulta.getEstudiante().getAsesor() != null
                    && Objects.equals(consulta.getEstudiante().getAsesor().getId(), perfilId);

            return asesorAsignado || estudianteDelAsesor;
        }

        if (perfil.getTipoPerfil() == TipoPerfilUsuario.MONITOR) {
            return consulta.getMonitor() != null
                    && Objects.equals(consulta.getMonitor().getId(), perfilId);
        }

        if (perfil.getTipoPerfil() == TipoPerfilUsuario.CONCILIADOR) {
            // Cuando conciliaciones tenga alcance real, aquí se habilitarán consultas
            // asociadas.
            return false;
        }

        return false;
    }

    public boolean usuarioPuedeAsignarResponsables() {
        return !usuarioActualService.esEstudiante()
                && usuarioActualService.tienePermiso(ASIGNAR_RESPONSABLES_CONSULTA);
    }

    public boolean usuarioPuedeCambiarEstado() {
        return usuarioActualService.tieneAlgunPermiso(CAMBIAR_ESTADO_CONSULTAS, GESTIONAR_CONSULTAS)
                && !usuarioActualService.esEstudiante();
    }

    public boolean usuarioEsEstudiante() {
        return usuarioActualService.esEstudiante();
    }

    public boolean usuarioEsAsesor() {
        return usuarioActualService.esAsesor();
    }

    public boolean usuarioEsMonitor() {
        return usuarioActualService.esMonitor();
    }

    public boolean usuarioEsAdministrador() {
        return usuarioActualService.esRolAdministrador();
    }

    @Transactional(readOnly = true)
    public PerfilUsuarioActual obtenerPerfilActual() {
        return usuarioActualService.obtenerPerfilActual();
    }

    private Consulta obtenerConsulta(Long consultaId) {
        if (consultaId == null) {
            throw new BusinessException("La consulta es obligatoria");
        }

        return consultaRepository.findById(consultaId)
                .orElseThrow(() -> new BusinessException("Consulta no encontrada con id: " + consultaId));
    }

    private void validarTienePermiso(String permiso) {
        if (!usuarioActualService.tienePermiso(permiso)) {
            throw new AccessDeniedException("No tiene el permiso requerido: " + permiso);
        }
    }

    private void validarTieneAlgunPermiso(String... permisos) {
        if (!usuarioActualService.tieneAlgunPermiso(permisos)) {
            throw new AccessDeniedException("No tiene permisos para realizar esta acción");
        }
    }
}