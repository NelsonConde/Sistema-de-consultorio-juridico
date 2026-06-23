package co.edu.ufps.legal_cases.business.service.acceso.seguimiento;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.APROBAR_RESPUESTAS_SEGUIMIENTO;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.RESPONDER_SEGUIMIENTOS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_SEGUIMIENTOS;

import java.util.Objects;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.model.seguimiento.Seguimiento;
import co.edu.ufps.legal_cases.business.model.seguimiento.respuesta.EstadoRespuestaSeguimiento;
import co.edu.ufps.legal_cases.business.model.seguimiento.respuesta.SeguimientoRespuesta;
import co.edu.ufps.legal_cases.business.repository.seguimiento.SeguimientoRepository;
import co.edu.ufps.legal_cases.business.repository.seguimiento.respuesta.SeguimientoRespuestaRepository;
import co.edu.ufps.legal_cases.business.service.acceso.consulta.ConsultaAccessService;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.dto.account.PerfilUsuarioActual;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.service.context.UsuarioActualService;
import lombok.AllArgsConstructor;

// Valida permisos y alcance para respuestas de estudiantes a seguimientos.
@Service
@AllArgsConstructor
public class SeguimientoRespuestaAccessService {

    private final SeguimientoRepository seguimientoRepository;
    private final SeguimientoRespuestaRepository seguimientoRespuestaRepository;
    private final ConsultaAccessService consultaAccessService;
    private final UsuarioActualService usuarioActualService;

    @Transactional(readOnly = true)
    public void validarPuedeResponderSeguimiento(Long seguimientoId) {
        validarTienePermiso(RESPONDER_SEGUIMIENTOS);

        if (!usuarioActualService.esEstudiante()) {
            throw new AccessDeniedException("Solo el estudiante puede responder seguimientos");
        }

        Seguimiento seguimiento = obtenerSeguimientoActivo(seguimientoId);

        if (!Boolean.TRUE.equals(seguimiento.getNotificarEstudiante())) {
            throw new AccessDeniedException("Este seguimiento no está habilitado para respuesta del estudiante");
        }

        if (!consultaAccessService.puedeAccederAConsulta(seguimiento.getConsulta())) {
            throw new AccessDeniedException("No tiene permisos para responder este seguimiento");
        }
    }

    @Transactional(readOnly = true)
    public void validarPuedeEditarRespuesta(Long respuestaId) {
        validarTienePermiso(RESPONDER_SEGUIMIENTOS);

        if (!usuarioActualService.esEstudiante()) {
            throw new AccessDeniedException("Solo el estudiante puede editar su respuesta");
        }

        SeguimientoRespuesta respuesta = obtenerRespuestaActiva(respuestaId);

        if (!esRespuestaDelEstudianteActual(respuesta)) {
            throw new AccessDeniedException("No tiene permisos para editar esta respuesta");
        }

        if (respuesta.getEstado() != EstadoRespuestaSeguimiento.PENDIENTE) {
            throw new BusinessException("Solo se pueden editar respuestas pendientes");
        }

        if (!puedeVerSeguimientoComoEstudiante(respuesta.getSeguimiento())) {
            throw new AccessDeniedException("No tiene permisos para editar esta respuesta");
        }
    }

    @Transactional(readOnly = true)
    public void validarPuedeListarRespuestasDeSeguimiento(Long seguimientoId) {
        validarTienePermiso(VER_SEGUIMIENTOS);

        Seguimiento seguimiento = obtenerSeguimientoActivo(seguimientoId);

        if (!consultaAccessService.puedeAccederAConsulta(seguimiento.getConsulta())) {
            throw new AccessDeniedException("No tiene permisos para ver respuestas de este seguimiento");
        }

        if (usuarioActualService.esEstudiante() && !Boolean.TRUE.equals(seguimiento.getNotificarEstudiante())) {
            throw new AccessDeniedException("El estudiante no puede ver respuestas de este seguimiento");
        }
    }

    @Transactional(readOnly = true)
    public void validarPuedeListarRespuestasPendientes() {
        validarTienePermiso(APROBAR_RESPUESTAS_SEGUIMIENTO);

        if (usuarioActualService.esEstudiante() || usuarioActualService.esConciliador()) {
            throw new AccessDeniedException("No tiene permisos para revisar respuestas de seguimiento");
        }
    }

    @Transactional(readOnly = true)
    public void validarPuedeRevisarRespuesta(Long respuestaId) {
        validarTienePermiso(APROBAR_RESPUESTAS_SEGUIMIENTO);

        SeguimientoRespuesta respuesta = obtenerRespuestaActiva(respuestaId);

        if (!puedeRevisarRespuesta(respuesta)) {
            throw new AccessDeniedException("No tiene permisos para revisar esta respuesta");
        }

        if (respuesta.getEstado() != EstadoRespuestaSeguimiento.PENDIENTE) {
            throw new BusinessException("Solo se pueden revisar respuestas pendientes");
        }
    }

    @Transactional(readOnly = true)
    public boolean puedeVerRespuesta(SeguimientoRespuesta respuesta) {
        if (respuesta == null || !Boolean.TRUE.equals(respuesta.getActivo())) {
            return false;
        }

        Seguimiento seguimiento = respuesta.getSeguimiento();

        if (seguimiento == null || !Boolean.TRUE.equals(seguimiento.getActivo())) {
            return false;
        }

        if (!consultaAccessService.puedeAccederAConsulta(seguimiento.getConsulta())) {
            return false;
        }

        if (usuarioActualService.esEstudiante()) {
            return Boolean.TRUE.equals(seguimiento.getNotificarEstudiante())
                    && esRespuestaDelEstudianteActual(respuesta);
        }

        if (usuarioActualService.esConciliador()) {
            // Cuando conciliaciones tenga alcance real, aquí se habilitará si aplica.
            return false;
        }

        return true;
    }

    @Transactional(readOnly = true)
    public boolean puedeRevisarRespuesta(SeguimientoRespuesta respuesta) {
        if (respuesta == null || !Boolean.TRUE.equals(respuesta.getActivo())) {
            return false;
        }

        if (usuarioActualService.esEstudiante() || usuarioActualService.esConciliador()) {
            return false;
        }

        Seguimiento seguimiento = respuesta.getSeguimiento();

        if (seguimiento == null || !Boolean.TRUE.equals(seguimiento.getActivo())) {
            return false;
        }

        return consultaAccessService.puedeAccederAConsulta(seguimiento.getConsulta());
    }

    @Transactional(readOnly = true)
    public Long obtenerUsuarioActualId() {
        return usuarioActualService.obtenerUsuarioActualId();
    }

    @Transactional(readOnly = true)
    public Long obtenerEstudianteActualId() {
        PerfilUsuarioActual perfil = usuarioActualService.obtenerPerfilActual();

        if (perfil.getTipoPerfil() != TipoPerfilUsuario.ESTUDIANTE) {
            throw new AccessDeniedException("El usuario actual no es estudiante");
        }

        return perfil.getPerfilId();
    }

    private boolean puedeVerSeguimientoComoEstudiante(Seguimiento seguimiento) {
        return seguimiento != null
                && Boolean.TRUE.equals(seguimiento.getActivo())
                && Boolean.TRUE.equals(seguimiento.getNotificarEstudiante())
                && consultaAccessService.puedeAccederAConsulta(seguimiento.getConsulta());
    }

    private boolean esRespuestaDelEstudianteActual(SeguimientoRespuesta respuesta) {
        Long estudianteActualId = obtenerEstudianteActualId();

        return respuesta.getEstudiante() != null
                && Objects.equals(respuesta.getEstudiante().getId(), estudianteActualId);
    }

    private Seguimiento obtenerSeguimientoActivo(Long seguimientoId) {
        if (seguimientoId == null) {
            throw new BusinessException("El id del seguimiento es obligatorio");
        }

        return seguimientoRepository.findByIdAndActivoTrue(seguimientoId)
                .orElseThrow(() -> new BusinessException("Seguimiento no encontrado con id: " + seguimientoId));
    }

    private SeguimientoRespuesta obtenerRespuestaActiva(Long respuestaId) {
        if (respuestaId == null) {
            throw new BusinessException("El id de la respuesta es obligatorio");
        }

        return seguimientoRespuestaRepository.findByIdAndActivoTrue(respuestaId)
                .orElseThrow(() -> new BusinessException("Respuesta de seguimiento no encontrada con id: " + respuestaId));
    }

    private void validarTienePermiso(String permiso) {
        if (!usuarioActualService.tienePermiso(permiso)) {
            throw new AccessDeniedException("No tiene el permiso requerido: " + permiso);
        }
    }
}