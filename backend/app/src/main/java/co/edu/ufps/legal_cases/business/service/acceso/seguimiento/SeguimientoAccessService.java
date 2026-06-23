package co.edu.ufps.legal_cases.business.service.acceso.seguimiento;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.CREAR_SEGUIMIENTOS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.EDITAR_SEGUIMIENTOS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.ELIMINAR_SEGUIMIENTOS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_ALERTAS_DISCIPLINARIAS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_SEGUIMIENTOS;

import java.util.Objects;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.model.seguimiento.Seguimiento;
import co.edu.ufps.legal_cases.business.repository.seguimiento.SeguimientoRepository;
import co.edu.ufps.legal_cases.business.service.acceso.consulta.ConsultaAccessService;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.service.context.UsuarioActualService;
import lombok.AllArgsConstructor;

// Este servicio se encarga de validar los permisos de acceso de seguimientos
// Al igual que en consulta donde esta comentado el paso a paso de validacion
@Service
@AllArgsConstructor
public class SeguimientoAccessService {

    private final SeguimientoRepository seguimientoRepository;
    private final ConsultaAccessService consultaAccessService;
    private final UsuarioActualService usuarioActualService;

    @Transactional(readOnly = true)
    public void validarPuedeListarSeguimientosDeConsulta(Long consultaId) {
        validarTienePermiso(VER_SEGUIMIENTOS);

        if (usuarioActualService.esEstudiante()) {
            throw new AccessDeniedException("El estudiante debe consultar solo los seguimientos visibles");
        }

        if (!consultaAccessService.puedeAccederAConsulta(consultaId)) {
            throw new AccessDeniedException("No tiene permisos para ver seguimientos de esta consulta");
        }
    }

    @Transactional(readOnly = true)
    public void validarPuedeListarSeguimientosPorFechaEntrega() {
        validarTienePermiso(VER_SEGUIMIENTOS);
    }

    @Transactional(readOnly = true)
    public void validarPuedeListarSeguimientosVisiblesParaEstudiante(Long consultaId) {
        validarTienePermiso(VER_SEGUIMIENTOS);

        if (!consultaAccessService.puedeAccederAConsulta(consultaId)) {
            throw new AccessDeniedException("No tiene permisos para ver seguimientos de esta consulta");
        }
    }

    @Transactional(readOnly = true)
    public void validarPuedeVerSeguimiento(Long seguimientoId) {
        validarTienePermiso(VER_SEGUIMIENTOS);

        Seguimiento seguimiento = obtenerSeguimiento(seguimientoId);

        if (!puedeVerSeguimiento(seguimiento)) {
            throw new AccessDeniedException("No tiene permisos para ver este seguimiento");
        }
    }

    @Transactional(readOnly = true)
    public void validarPuedeCrearSeguimiento(Long consultaId) {
        validarTienePermiso(CREAR_SEGUIMIENTOS);

        if (usuarioActualService.esEstudiante()) {
            throw new AccessDeniedException("El estudiante no puede crear seguimientos");
        }

        if (usuarioActualService.esConciliador()) {
            throw new AccessDeniedException("El conciliador no puede crear seguimientos");
        }

        if (!consultaAccessService.puedeAccederAConsulta(consultaId)) {
            throw new AccessDeniedException("No tiene permisos para crear seguimientos en esta consulta");
        }
    }

    @Transactional(readOnly = true)
    public void validarPuedeEditarSeguimiento(Long seguimientoId) {
        validarTienePermiso(EDITAR_SEGUIMIENTOS);

        Seguimiento seguimiento = obtenerSeguimiento(seguimientoId);

        if (!puedeModificarSeguimiento(seguimiento)) {
            throw new AccessDeniedException("No tiene permisos para editar este seguimiento");
        }
    }

    @Transactional(readOnly = true)
    public void validarPuedeEliminarSeguimiento(Long seguimientoId) {
        validarTienePermiso(ELIMINAR_SEGUIMIENTOS);

        Seguimiento seguimiento = obtenerSeguimiento(seguimientoId);

        if (!puedeModificarSeguimiento(seguimiento)) {
            throw new AccessDeniedException("No tiene permisos para eliminar este seguimiento");
        }
    }

    @Transactional(readOnly = true)
    public void validarPuedeListarSeguimientosPorAutor(Long autorId) {
        validarTienePermiso(VER_SEGUIMIENTOS);

        if (autorId == null) {
            throw new BusinessException("El id del autor es obligatorio");
        }

        if (usuarioActualService.esRolAdministrador()) {
            return;
        }

        Long usuarioActualId = usuarioActualService.obtenerUsuarioActualId();

        if (!Objects.equals(usuarioActualId, autorId)) {
            throw new AccessDeniedException("Solo puede consultar seguimientos creados por su usuario");
        }
    }

    @Transactional(readOnly = true)
    public void validarPuedeListarAlertasDisciplinarias() {
        validarTienePermiso(VER_ALERTAS_DISCIPLINARIAS);
    }

    // Valida solo que tenga el permiso de ver seguimientos, sin restricción de alcance.
    // El filtro de alcance se aplica después con puedeVerSeguimiento en el stream.
    @Transactional(readOnly = true)
    public void validarTienePermisoVerSeguimientos() {
        validarTienePermiso(VER_SEGUIMIENTOS);
    }

    @Transactional(readOnly = true)
    public boolean puedeVerSeguimiento(Seguimiento seguimiento) {
        if (seguimiento == null || !Boolean.TRUE.equals(seguimiento.getActivo())) {
            return false;
        }

        if (usuarioActualService.esRolAdministrador()) {
            return true;
        }

        if (!consultaAccessService.puedeAccederAConsulta(seguimiento.getConsulta())) {
            return false;
        }

        if (usuarioActualService.esEstudiante()) {
            return Boolean.TRUE.equals(seguimiento.getNotificarEstudiante());
        }

        if (usuarioActualService.esConciliador()) {
            // Cuando exista relación de conciliaciones, aquí se permitirá ver seguimientos
            // asociados a consultas de sus conciliaciones.
            return false;
        }

        return true;
    }

    @Transactional(readOnly = true)
    public boolean puedeModificarSeguimiento(Seguimiento seguimiento) {
        if (seguimiento == null || !Boolean.TRUE.equals(seguimiento.getActivo())) {
            return false;
        }

        if (usuarioActualService.esRolAdministrador()) {
            return true;
        }

        if (usuarioActualService.esEstudiante() || usuarioActualService.esConciliador()) {
            return false;
        }

        if (!consultaAccessService.puedeAccederAConsulta(seguimiento.getConsulta())) {
            return false;
        }

        // Asesor y monitor pueden modificar solo seguimientos creados por ellos.
        return seguimiento.getAutor() != null
                && Objects.equals(seguimiento.getAutor().getId(), usuarioActualService.obtenerUsuarioActualId());
    }

    @Transactional(readOnly = true)
    public boolean puedeVerSeguimientosDeConsulta(Long consultaId) {
        if (usuarioActualService.esEstudiante()) {
            return false;
        }

        return consultaAccessService.puedeAccederAConsulta(consultaId);
    }

    @Transactional(readOnly = true)
    public Long obtenerUsuarioActualId() {
        return usuarioActualService.obtenerUsuarioActualId();
    }

    private Seguimiento obtenerSeguimiento(Long seguimientoId) {
        if (seguimientoId == null) {
            throw new BusinessException("El id del seguimiento es obligatorio");
        }

        return seguimientoRepository.findByIdAndActivoTrue(seguimientoId)
                .orElseThrow(() -> new BusinessException("Seguimiento no encontrado con id: " + seguimientoId));
    }

    private void validarTienePermiso(String permiso) {
        if (!usuarioActualService.tienePermiso(permiso)) {
            throw new AccessDeniedException("No tiene el permiso requerido: " + permiso);
        }
    }
}