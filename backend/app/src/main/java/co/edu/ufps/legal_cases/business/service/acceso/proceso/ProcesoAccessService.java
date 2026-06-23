package co.edu.ufps.legal_cases.business.service.acceso.proceso;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_PROCESOS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_PROCESOS;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.model.proceso.Proceso;
import co.edu.ufps.legal_cases.business.repository.proceso.ProcesoRepository;
import co.edu.ufps.legal_cases.business.service.acceso.consulta.ConsultaAccessService;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.service.context.UsuarioActualService;

@Service
public class ProcesoAccessService {

    private final ProcesoRepository procesoRepository;
    private final ConsultaAccessService consultaAccessService;
    private final UsuarioActualService usuarioActualService;

    public ProcesoAccessService(
            ProcesoRepository procesoRepository,
            ConsultaAccessService consultaAccessService,
            UsuarioActualService usuarioActualService) {
        this.procesoRepository = procesoRepository;
        this.consultaAccessService = consultaAccessService;
        this.usuarioActualService = usuarioActualService;
    }

    @Transactional(readOnly = true)
    public void validarPuedeListarProcesos() {
        validarTienePermiso(VER_PROCESOS);
    }

    @Transactional(readOnly = true)
    public void validarPuedeVerProceso(Long procesoId) {
        validarTienePermiso(VER_PROCESOS);

        Proceso proceso = obtenerProcesoActivo(procesoId);

        if (!puedeAccederAProceso(proceso)) {
            throw new AccessDeniedException("No tiene permisos para ver este proceso");
        }
    }

    @Transactional(readOnly = true)
    public void validarPuedeCrearProceso(Long consultaId) {
        validarTienePermiso(GESTIONAR_PROCESOS);
        validarUsuarioPuedeGestionarProcesos();

        if (!consultaAccessService.puedeAccederAConsulta(consultaId)) {
            throw new AccessDeniedException("No tiene permisos para crear procesos en esta consulta");
        }
    }

    @Transactional(readOnly = true)
    public void validarPuedeActualizarProceso(Long procesoId) {
        validarTienePermiso(GESTIONAR_PROCESOS);
        validarUsuarioPuedeGestionarProcesos();

        Proceso proceso = obtenerProcesoActivo(procesoId);

        if (!puedeGestionarProceso(proceso)) {
            throw new AccessDeniedException("No tiene permisos para actualizar este proceso");
        }
    }

    @Transactional(readOnly = true)
    public void validarPuedeDesactivarProceso(Long procesoId) {
        validarTienePermiso(GESTIONAR_PROCESOS);
        validarUsuarioPuedeGestionarProcesos();

        Proceso proceso = obtenerProcesoActivo(procesoId);

        if (!puedeGestionarProceso(proceso)) {
            throw new AccessDeniedException("No tiene permisos para desactivar este proceso");
        }
    }

    @Transactional(readOnly = true)
    public void validarPuedeCambiarEstadoProceso(Long procesoId) {
        validarTienePermiso(GESTIONAR_PROCESOS);
        validarUsuarioPuedeGestionarProcesos();

        Proceso proceso = obtenerProceso(procesoId);

        if (!puedeGestionarProcesoPorConsulta(proceso)) {
            throw new AccessDeniedException("No tiene permisos para cambiar el estado de este proceso");
        }
    }

    @Transactional(readOnly = true)
    public boolean puedeAccederAProceso(Proceso proceso) {
        if (proceso == null || !Boolean.TRUE.equals(proceso.getActivo())) {
            return false;
        }

        if (usuarioActualService.esConciliador()) {
            // En esta fase el conciliador no tiene alcance real sobre procesos.
            // Se habilitará cuando el módulo de conciliaciones defina su relación con
            // consultas/procesos.
            return false;
        }

        if (proceso.getConsulta() == null) {
            return false;
        }

        // El alcance del proceso se hereda de la consulta asociada.
        return consultaAccessService.puedeAccederAConsulta(proceso.getConsulta());
    }

    @Transactional(readOnly = true)
    public boolean puedeGestionarProceso(Proceso proceso) {
        if (usuarioActualService.esEstudiante()) {
            return false;
        }

        if (usuarioActualService.esConciliador()) {
            return false;
        }

        return puedeAccederAProceso(proceso);
    }

    private void validarUsuarioPuedeGestionarProcesos() {
        if (usuarioActualService.esEstudiante()) {
            throw new AccessDeniedException("El estudiante solo puede consultar procesos");
        }

        if (usuarioActualService.esConciliador()) {
            throw new AccessDeniedException("El conciliador no puede gestionar procesos en esta fase");
        }
    }

    private Proceso obtenerProcesoActivo(Long procesoId) {
        if (procesoId == null) {
            throw new BusinessException("El id del proceso es obligatorio");
        }

        return procesoRepository.findByIdAndActivoTrue(procesoId)
                .orElseThrow(() -> new BusinessException("Proceso no encontrado con id: " + procesoId));
    }

    private Proceso obtenerProceso(Long procesoId) {
        if (procesoId == null) {
            throw new BusinessException("El id del proceso es obligatorio");
        }

        return procesoRepository.findById(procesoId)
                .orElseThrow(() -> new BusinessException("Proceso no encontrado con id: " + procesoId));
    }

    private boolean puedeGestionarProcesoPorConsulta(Proceso proceso) {
        if (proceso == null || proceso.getConsulta() == null) {
            return false;
        }

        if (usuarioActualService.esEstudiante()) {
            return false;
        }

        if (usuarioActualService.esConciliador()) {
            return false;
        }

        return consultaAccessService.puedeAccederAConsulta(proceso.getConsulta());
    }

    private void validarTienePermiso(String permiso) {
        if (!usuarioActualService.tienePermiso(permiso)) {
            throw new AccessDeniedException("No tiene el permiso requerido: " + permiso);
        }
    }
}