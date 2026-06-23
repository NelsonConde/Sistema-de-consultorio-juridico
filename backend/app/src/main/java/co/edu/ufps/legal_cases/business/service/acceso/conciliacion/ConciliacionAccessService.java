package co.edu.ufps.legal_cases.business.service.acceso.conciliacion;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.CONCLUIR_CONCILIACIONES;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_CONCILIACIONES;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.PROGRAMAR_REUNIONES_CONCILIACION;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.REPROGRAMAR_REUNIONES_CONCILIACION;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_CONCILIACIONES;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.model.conciliacion.Conciliacion;
import co.edu.ufps.legal_cases.business.model.consulta.Consulta;
import co.edu.ufps.legal_cases.business.repository.conciliacion.ConciliacionRepository;
import co.edu.ufps.legal_cases.business.repository.consulta.ConsultaRepository;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.service.context.UsuarioActualService;

// Valida permisos funcionales y delega las reglas de alcance a ConciliacionAlcanceService.
@Service
public class ConciliacionAccessService {

    private final ConciliacionRepository conciliacionRepository;
    private final ConsultaRepository consultaRepository;
    private final ConciliacionAlcanceService conciliacionAlcanceService;
    private final UsuarioActualService usuarioActualService;

    public ConciliacionAccessService(
            ConciliacionRepository conciliacionRepository,
            ConsultaRepository consultaRepository,
            ConciliacionAlcanceService conciliacionAlcanceService,
            UsuarioActualService usuarioActualService) {
        this.conciliacionRepository = conciliacionRepository;
        this.consultaRepository = consultaRepository;
        this.conciliacionAlcanceService = conciliacionAlcanceService;
        this.usuarioActualService = usuarioActualService;
    }

    @Transactional(readOnly = true)
    public void validarPuedeListarConciliaciones() {
        validarTienePermiso(VER_CONCILIACIONES);
    }

    @Transactional(readOnly = true)
    public void validarPuedeVerConciliacion(Long conciliacionId) {
        validarTienePermiso(VER_CONCILIACIONES);

        Conciliacion conciliacion = obtenerConciliacionActiva(conciliacionId);

        if (!conciliacionAlcanceService.puedeVerConciliacion(conciliacion)) {
            throw new AccessDeniedException("No tiene permisos para ver esta conciliación");
        }
    }

    @Transactional(readOnly = true)
    public void validarPuedeCrearConciliacion(Long consultaId) {
        validarTienePermiso(GESTIONAR_CONCILIACIONES);

        Consulta consulta = obtenerConsulta(consultaId);

        if (!conciliacionAlcanceService.puedeCrearConciliacion(consulta)) {
            throw new AccessDeniedException("No tiene permisos para crear una conciliación en esta consulta");
        }
    }

    @Transactional(readOnly = true)
    public void validarPuedeProgramarReunion(Long conciliacionId) {
        validarTienePermiso(PROGRAMAR_REUNIONES_CONCILIACION);

        Conciliacion conciliacion = obtenerConciliacionActiva(conciliacionId);

        if (!conciliacionAlcanceService.puedeProgramarReunion(conciliacion)) {
            throw new AccessDeniedException("No tiene permisos para programar la reunión de esta conciliación");
        }
    }

    @Transactional(readOnly = true)
    public void validarPuedeReprogramarReunion(Long conciliacionId) {
        validarTienePermiso(REPROGRAMAR_REUNIONES_CONCILIACION);

        Conciliacion conciliacion = obtenerConciliacionActiva(conciliacionId);

        if (!conciliacionAlcanceService.puedeReprogramarReunion(conciliacion)) {
            throw new AccessDeniedException("No tiene permisos para reprogramar la reunión de esta conciliación");
        }
    }

    @Transactional(readOnly = true)
    public void validarPuedeAsignarConciliador(Long conciliacionId) {
        validarTienePermiso(GESTIONAR_CONCILIACIONES);

        Conciliacion conciliacion = obtenerConciliacionActiva(conciliacionId);

        if (!conciliacionAlcanceService.puedeAsignarConciliador(conciliacion)) {
            throw new AccessDeniedException("No tiene permisos para asignar conciliador en esta conciliación");
        }
    }

    @Transactional(readOnly = true)
    public void validarPuedeAsignarEstudiante(Long conciliacionId) {
        validarTieneAlgunPermiso(GESTIONAR_CONCILIACIONES, CONCLUIR_CONCILIACIONES);

        Conciliacion conciliacion = obtenerConciliacionActiva(conciliacionId);

        if (!conciliacionAlcanceService.puedeAsignarEstudiante(conciliacion)) {
            throw new AccessDeniedException("No tiene permisos para asignar estudiante en esta conciliación");
        }
    }

    @Transactional(readOnly = true)
    public void validarPuedeCambiarEstado(Long conciliacionId, String estadoCodigo) {
        if (estadoCodigo == null || estadoCodigo.isBlank()) {
            throw new BusinessException("El estado de conciliación es obligatorio");
        }

        validarTieneAlgunPermiso(GESTIONAR_CONCILIACIONES, CONCLUIR_CONCILIACIONES);

        Conciliacion conciliacion = obtenerConciliacionActiva(conciliacionId);

        if (!conciliacionAlcanceService.puedeCambiarEstado(conciliacion, estadoCodigo)) {
            throw new AccessDeniedException("No tiene permisos para cambiar el estado de esta conciliación");
        }
    }

    @Transactional(readOnly = true)
    public void validarPuedeFinalizar(Long conciliacionId) {
        validarTieneAlgunPermiso(GESTIONAR_CONCILIACIONES, CONCLUIR_CONCILIACIONES);

        Conciliacion conciliacion = obtenerConciliacionActiva(conciliacionId);

        if (!conciliacionAlcanceService.puedeSubirActa(conciliacion)) {
            throw new AccessDeniedException("No tiene permisos para finalizar esta conciliación");
        }
    }

    @Transactional(readOnly = true)
    public void validarPuedeReemplazarSolicitud(Long conciliacionId) {
        validarTienePermiso(GESTIONAR_CONCILIACIONES);

        Conciliacion conciliacion = obtenerConciliacionActiva(conciliacionId);

        if (!conciliacionAlcanceService.puedeReemplazarSolicitud(conciliacion)) {
            throw new AccessDeniedException("No tiene permisos para reemplazar la solicitud de conciliación");
        }
    }

    @Transactional(readOnly = true)
    public void validarPuedeDesactivarConciliacion(Long conciliacionId) {
        validarTienePermiso(GESTIONAR_CONCILIACIONES);

        Conciliacion conciliacion = obtenerConciliacionActiva(conciliacionId);

        if (!conciliacionAlcanceService.puedeDesactivarConciliacion(conciliacion)) {
            throw new AccessDeniedException("No tiene permisos para desactivar esta conciliación");
        }
    }

    @Transactional(readOnly = true)
    public Long obtenerUsuarioActualId() {
        return usuarioActualService.obtenerUsuarioActualId();
    }

    private Conciliacion obtenerConciliacionActiva(Long conciliacionId) {
        if (conciliacionId == null) {
            throw new BusinessException("El id de la conciliación es obligatorio");
        }

        return conciliacionRepository.findByIdAndActivoTrue(conciliacionId)
                .orElseThrow(() -> new BusinessException(
                        "Conciliación no encontrada con id: " + conciliacionId));
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
