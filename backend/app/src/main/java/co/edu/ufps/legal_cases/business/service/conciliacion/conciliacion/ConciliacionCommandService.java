package co.edu.ufps.legal_cases.business.service.conciliacion.conciliacion;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import co.edu.ufps.legal_cases.audit.aop.log.Auditable;

import co.edu.ufps.legal_cases.business.dto.conciliacion.ConciliacionResponseDTO;
import co.edu.ufps.legal_cases.business.model.conciliacion.Conciliacion;
import co.edu.ufps.legal_cases.business.model.conciliacion.EstadoConciliacion;
import co.edu.ufps.legal_cases.business.model.consulta.Consulta;
import co.edu.ufps.legal_cases.business.model.perfil.Conciliador;
import co.edu.ufps.legal_cases.business.model.perfil.Estudiante;
import co.edu.ufps.legal_cases.business.repository.conciliacion.ConciliacionRepository;
import co.edu.ufps.legal_cases.business.service.acceso.conciliacion.ConciliacionAccessService;
import co.edu.ufps.legal_cases.business.service.conciliacion.reunion.notificacion.ReunionConciliacionNotificacionService;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.repository.account.UsuarioSistemaRepository;
import lombok.AllArgsConstructor;

// Maneja cambios de escritura del módulo de conciliación.
@Service
@AllArgsConstructor
public class ConciliacionCommandService {

    private final ConciliacionRepository conciliacionRepository;
    private final UsuarioSistemaRepository usuarioSistemaRepository;
    private final ConciliacionAccessService conciliacionAccessService;
    private final ConciliacionRelacionService conciliacionRelacionService;
    private final ConciliacionAsignacionService conciliacionAsignacionService;
    private final ConciliacionDocumentoService conciliacionDocumentoService;
    private final ConciliacionValidator conciliacionValidator;
    private final ConciliacionMapper conciliacionMapper;
    private final ReunionConciliacionNotificacionService reunionConciliacionNotificacionService;

    @Transactional
    @Auditable(action = "CREAR_CONCILIACION", entityName = "Conciliacion")
    public ConciliacionResponseDTO crearDesdeConsulta(Long consultaId, MultipartFile solicitud) {
        conciliacionAccessService.validarPuedeCrearConciliacion(consultaId);

        Consulta consulta = conciliacionRelacionService.obtenerConsulta(consultaId);

        conciliacionValidator.validarConsultaPermiteConciliacion(consulta);
        conciliacionValidator.validarNoExisteConciliacionActivaNoFinalizada(consulta.getId());

        Estudiante estudiante = conciliacionAsignacionService
                .seleccionarEstudianteParaNuevaConciliacion(consulta);

        Conciliador conciliador = conciliacionAsignacionService
                .seleccionarConciliadorParaNuevaConciliacion();

        UsuarioSistema solicitante = obtenerSolicitanteActual();

        Conciliacion conciliacion = new Conciliacion();
        conciliacion.setConsulta(consulta);
        conciliacion.setEstudiante(estudiante);
        conciliacion.setConciliador(conciliador);
        conciliacion.setSolicitadoPor(solicitante);
        conciliacion.setActivo(true);

        // Si falta estudiante o conciliador queda EN_ESPERA.
        // Si tiene ambos queda ESPERANDO_REUNION.
        aplicarEstadoSegunAsignacion(conciliacion);

        Conciliacion conciliacionGuardada = conciliacionRepository.save(conciliacion);

        // Se guarda después del primer save porque la ruta depende del id:
        // conciliacion/{id}/solicitud.pdf
        String solicitudPath = conciliacionDocumentoService.guardarSolicitud(
                conciliacionGuardada.getId(),
                solicitud);

        conciliacionGuardada.setDocumentoSolicitudPath(solicitudPath);

        return conciliacionMapper.convertirAResponseDTO(
                conciliacionRepository.save(conciliacionGuardada));
    }

    @Transactional
    @Auditable(action = "ASIGNAR_ESTUDIANTE", entityName = "Conciliacion")
    public ConciliacionResponseDTO asignarEstudiante(Long id, Long estudianteId) {
        conciliacionAccessService.validarPuedeAsignarEstudiante(id);

        Conciliacion conciliacion = conciliacionRelacionService.obtenerConciliacionActiva(id);
        conciliacionValidator.validarConciliacionNoFinalizada(conciliacion);
        conciliacionValidator.validarConsultaPermiteOperacionConciliacion(conciliacion.getConsulta());

        Estudiante estudiante = conciliacionRelacionService.obtenerEstudianteActivo(estudianteId);
        conciliacionValidator.validarEstudianteHabilitadoParaConciliacion(estudiante);

        conciliacion.setEstudiante(estudiante);

        // Si ya existe reunión programada, se conserva REUNION_PROGRAMADA.
        // Si no existe reunión, el estado depende de los responsables asignados.
        aplicarEstadoSegunAsignacion(conciliacion);

        return conciliacionMapper.convertirAResponseDTO(
                conciliacionRepository.save(conciliacion));
    }

    @Transactional
    @Auditable(action = "ASIGNAR_CONCILIADOR", entityName = "Conciliacion")
    public ConciliacionResponseDTO asignarConciliador(Long id, Long conciliadorId) {
        conciliacionAccessService.validarPuedeAsignarConciliador(id);

        Conciliacion conciliacion = conciliacionRelacionService.obtenerConciliacionActiva(id);
        conciliacionValidator.validarConciliacionNoFinalizada(conciliacion);
        conciliacionValidator.validarConsultaPermiteOperacionConciliacion(conciliacion.getConsulta());

        Conciliador conciliador = conciliacionRelacionService.obtenerConciliadorActivo(conciliadorId);
        conciliacionValidator.validarConciliadorActivo(conciliador);

        conciliacion.setConciliador(conciliador);

        // Si ya existe reunión programada, se conserva REUNION_PROGRAMADA.
        // Si no existe reunión, el estado depende de los responsables asignados.
        aplicarEstadoSegunAsignacion(conciliacion);

        return conciliacionMapper.convertirAResponseDTO(
                conciliacionRepository.save(conciliacion));
    }

    @Transactional
    @Auditable(action = "CAMBIAR_ESTADO_CONCILIACION", entityName = "Conciliacion")
    public ConciliacionResponseDTO cambiarEstado(Long id, String estadoCodigo) {
        conciliacionAccessService.validarPuedeCambiarEstado(id, estadoCodigo);

        Conciliacion conciliacion = conciliacionRelacionService.obtenerConciliacionActiva(id);
        EstadoConciliacion estadoNuevo = conciliacionRelacionService.obtenerEstadoActivoPorCodigo(estadoCodigo);

        conciliacionValidator.validarCambioEstado(conciliacion, estadoNuevo);

        conciliacion.setEstado(estadoNuevo);

        return conciliacionMapper.convertirAResponseDTO(
                conciliacionRepository.save(conciliacion));
    }

    @Transactional
    @Auditable(action = "FINALIZAR_CONCILIACION", entityName = "Conciliacion")
    public ConciliacionResponseDTO finalizar(Long id, String estadoCodigo, MultipartFile acta) {
        conciliacionAccessService.validarPuedeFinalizar(id);

        Conciliacion conciliacion = conciliacionRelacionService.obtenerConciliacionActiva(id);
        EstadoConciliacion estadoFinal = conciliacionRelacionService.obtenerEstadoActivoPorCodigo(estadoCodigo);

        conciliacionValidator.validarFinalizacion(conciliacion, estadoFinal);

        // El acta es soporte obligatorio de cierre.
        // Se guarda antes de cambiar estado para no finalizar sin documento.
        String actaPath = conciliacionDocumentoService.guardarActa(id, acta);

        conciliacion.setEstado(estadoFinal);
        conciliacion.setActaPath(actaPath);
        conciliacion.setFechaFinalizacion(LocalDateTime.now());

        // Al finalizar la conciliación ya no deben salir recordatorios de reunión pendientes.
        reunionConciliacionNotificacionService.cancelarPendientesPorConciliacion(id);

        return conciliacionMapper.convertirAResponseDTO(
                conciliacionRepository.save(conciliacion));
    }

    @Transactional
    @Auditable(action = "ACTUALIZAR_SOLICITUD", entityName = "Conciliacion")
    public ConciliacionResponseDTO reemplazarSolicitud(Long id, MultipartFile solicitud) {
        conciliacionAccessService.validarPuedeReemplazarSolicitud(id);

        Conciliacion conciliacion = conciliacionRelacionService.obtenerConciliacionActiva(id);
        conciliacionValidator.validarConciliacionNoFinalizada(conciliacion);
        conciliacionValidator.validarConsultaPermiteOperacionConciliacion(conciliacion.getConsulta());

        String solicitudPath = conciliacionDocumentoService.guardarSolicitud(id, solicitud);
        conciliacion.setDocumentoSolicitudPath(solicitudPath);

        return conciliacionMapper.convertirAResponseDTO(
                conciliacionRepository.save(conciliacion));
    }

    @Transactional
    @Auditable(action = "DESACTIVAR_CONCILIACION", entityName = "Conciliacion")
    public void desactivar(Long id) {
        conciliacionAccessService.validarPuedeDesactivarConciliacion(id);

        Conciliacion conciliacion = conciliacionRelacionService.obtenerConciliacionActiva(id);
        conciliacionValidator.validarConciliacionNoFinalizada(conciliacion);
        conciliacionValidator.validarConsultaPermiteOperacionConciliacion(conciliacion.getConsulta());

        // Desactivación lógica. No representa finalización de la conciliación.
        conciliacion.setActivo(false);

        // Una conciliación desactivada no debe conservar recordatorios pendientes.
        reunionConciliacionNotificacionService.cancelarPendientesPorConciliacion(id);

        conciliacionRepository.save(conciliacion);
    }

    private void aplicarEstadoSegunAsignacion(Conciliacion conciliacion) {
        String codigoEstado = conciliacionValidator.calcularCodigoEstadoDespuesDeAsignacion(conciliacion);
        EstadoConciliacion estado = conciliacionRelacionService.obtenerEstadoActivoPorCodigo(codigoEstado);
        conciliacion.setEstado(estado);
    }

    private UsuarioSistema obtenerSolicitanteActual() {
        Long usuarioActualId = conciliacionAccessService.obtenerUsuarioActualId();

        return usuarioSistemaRepository.findById(usuarioActualId)
                .orElseThrow(() -> new BusinessException(
                        "Usuario solicitante no encontrado con id: " + usuarioActualId));
    }
}