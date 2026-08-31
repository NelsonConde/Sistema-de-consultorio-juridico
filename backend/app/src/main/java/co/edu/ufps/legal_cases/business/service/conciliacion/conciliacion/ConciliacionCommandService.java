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
import co.edu.ufps.legal_cases.common.concurrency.ConcurrenciaOptimistaValidator;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.file_storage.model.FileAsset;
import co.edu.ufps.legal_cases.file_storage.model.FileResourceType;
import co.edu.ufps.legal_cases.file_storage.service.FileResourceService;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.repository.account.UsuarioSistemaRepository;
import jakarta.persistence.EntityManager;
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
    private final FileResourceService fileResourceService;
    private final ConciliacionValidator conciliacionValidator;
    private final ConciliacionMapper conciliacionMapper;
    private final ReunionConciliacionNotificacionService reunionConciliacionNotificacionService;
    private final ConcurrenciaOptimistaValidator concurrenciaOptimistaValidator;
    private final EntityManager entityManager;

    @Transactional
    @Auditable(
            action = "CREAR_CONCILIACION",
            entityName = "Conciliacion",
            entityId = "#result.id",
            metadata = "consultaId=#consultaId")
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

        entityManager.flush();

        FileAsset solicitudAsset = fileResourceService.storeMultipartAfterAuthorization(
                FileResourceType.CONCILIACION,
                conciliacionGuardada.getId(),
                null,
                solicitud);
        conciliacionGuardada.setDocumentoSolicitud(solicitudAsset);

        conciliacionGuardada = conciliacionRepository.save(conciliacionGuardada);

        entityManager.flush();

        return conciliacionMapper.convertirAResponseDTO(conciliacionGuardada);
    }

    @Transactional
    @Auditable(action = "ASIGNAR_ESTUDIANTE", entityName = "Conciliacion")
    public ConciliacionResponseDTO asignarEstudiante(Long id, Long estudianteId, Long versionEsperada) {
        conciliacionAccessService.validarPuedeAsignarEstudiante(id);

        Conciliacion conciliacion = conciliacionRelacionService.obtenerConciliacionActiva(id);

        concurrenciaOptimistaValidator.validarVersion(
                versionEsperada,
                conciliacion.getVersion(),
                "conciliación");

        conciliacionValidator.validarConciliacionNoFinalizada(conciliacion);
        conciliacionValidator.validarConsultaPermiteOperacionConciliacion(conciliacion.getConsulta());

        Estudiante estudiante = conciliacionRelacionService.obtenerEstudianteActivo(estudianteId);
        conciliacionValidator.validarEstudianteHabilitadoParaConciliacion(estudiante);

        conciliacion.setEstudiante(estudiante);

        // Si ya existe reunión programada, se conserva REUNION_PROGRAMADA.
        // Si no existe reunión, el estado depende de los responsables asignados.
        aplicarEstadoSegunAsignacion(conciliacion);

        Conciliacion guardada = conciliacionRepository.save(conciliacion);

        entityManager.flush();

        return conciliacionMapper.convertirAResponseDTO(guardada);
    }

    @Transactional
    @Auditable(action = "ASIGNAR_CONCILIADOR", entityName = "Conciliacion")
    public ConciliacionResponseDTO asignarConciliador(Long id, Long conciliadorId, Long versionEsperada) {
        conciliacionAccessService.validarPuedeAsignarConciliador(id);

        Conciliacion conciliacion = conciliacionRelacionService.obtenerConciliacionActiva(id);

        concurrenciaOptimistaValidator.validarVersion(
                versionEsperada,
                conciliacion.getVersion(),
                "conciliación");

        conciliacionValidator.validarConciliacionNoFinalizada(conciliacion);
        conciliacionValidator.validarConsultaPermiteOperacionConciliacion(conciliacion.getConsulta());

        Conciliador conciliador = conciliacionRelacionService.obtenerConciliadorActivo(conciliadorId);
        conciliacionValidator.validarConciliadorActivo(conciliador);

        conciliacion.setConciliador(conciliador);

        // Si ya existe reunión programada, se conserva REUNION_PROGRAMADA.
        // Si no existe reunión, el estado depende de los responsables asignados.
        aplicarEstadoSegunAsignacion(conciliacion);

        Conciliacion guardada = conciliacionRepository.save(conciliacion);

        entityManager.flush();

        return conciliacionMapper.convertirAResponseDTO(guardada);
    }

    @Transactional
    @Auditable(action = "CAMBIAR_ESTADO_CONCILIACION", entityName = "Conciliacion")
    public ConciliacionResponseDTO cambiarEstado(Long id, String estadoCodigo, Long versionEsperada) {
        conciliacionAccessService.validarPuedeCambiarEstado(id, estadoCodigo);

        Conciliacion conciliacion = conciliacionRelacionService.obtenerConciliacionActiva(id);

        concurrenciaOptimistaValidator.validarVersion(
                versionEsperada,
                conciliacion.getVersion(),
                "conciliación");

        EstadoConciliacion estadoNuevo = conciliacionRelacionService.obtenerEstadoActivoPorCodigo(estadoCodigo);

        conciliacionValidator.validarCambioEstado(conciliacion, estadoNuevo);

        conciliacion.setEstado(estadoNuevo);

        Conciliacion guardada = conciliacionRepository.save(conciliacion);

        entityManager.flush();

        return conciliacionMapper.convertirAResponseDTO(guardada);
    }

    @Transactional
    @Auditable(action = "FINALIZAR_CONCILIACION", entityName = "Conciliacion")
    public ConciliacionResponseDTO finalizar(Long id, String estadoCodigo, MultipartFile acta, Long versionEsperada) {
        conciliacionAccessService.validarPuedeFinalizar(id);

        Conciliacion conciliacion = conciliacionRelacionService.obtenerConciliacionActiva(id);

        concurrenciaOptimistaValidator.validarVersion(
                versionEsperada,
                conciliacion.getVersion(),
                "conciliación");

        EstadoConciliacion estadoFinal = conciliacionRelacionService.obtenerEstadoActivoPorCodigo(estadoCodigo);

        conciliacionValidator.validarFinalizacion(conciliacion, estadoFinal);

        // El acta es soporte obligatorio de cierre.
        // Se guarda antes de cambiar estado para no finalizar sin documento.
        FileAsset actaAsset = fileResourceService.storeMultipartAfterAuthorization(
                FileResourceType.CONCILIACION, id, null, acta);

        Conciliacion guardada;
        try {
            conciliacion.setEstado(estadoFinal);
            conciliacion.setActa(actaAsset);
            conciliacion.setFechaFinalizacion(LocalDateTime.now());

            guardada = conciliacionRepository.save(conciliacion);

            /*
             * Confirmamos primero la escritura versionada.
             * No ejecutamos efectos posteriores si existe un conflicto.
             */
            entityManager.flush();
        } catch (RuntimeException ex) {
            compensarArchivoAlmacenado(actaAsset, ex);
            throw ex;
        }

        // Al finalizar la conciliación ya no deben salir recordatorios de reunión pendientes.
        reunionConciliacionNotificacionService.cancelarPendientesPorConciliacion(id);

        return conciliacionMapper.convertirAResponseDTO(guardada);
    }

    @Transactional
    @Auditable(action = "ACTUALIZAR_SOLICITUD", entityName = "Conciliacion")
    public ConciliacionResponseDTO reemplazarSolicitud(Long id, MultipartFile solicitud, Long versionEsperada) {
        conciliacionAccessService.validarPuedeReemplazarSolicitud(id);

        Conciliacion conciliacion = conciliacionRelacionService.obtenerConciliacionActiva(id);

        concurrenciaOptimistaValidator.validarVersion(
                versionEsperada,
                conciliacion.getVersion(),
                "conciliación");

        conciliacionValidator.validarConciliacionNoFinalizada(conciliacion);
        conciliacionValidator.validarConsultaPermiteOperacionConciliacion(conciliacion.getConsulta());

        FileAsset solicitudAsset = fileResourceService.storeMultipartAfterAuthorization(
                FileResourceType.CONCILIACION, id, null, solicitud);
        Conciliacion guardada;
        try {
            conciliacion.setDocumentoSolicitud(solicitudAsset);

            guardada = conciliacionRepository.save(conciliacion);

            entityManager.flush();
        } catch (RuntimeException ex) {
            compensarArchivoAlmacenado(solicitudAsset, ex);
            throw ex;
        }

        return conciliacionMapper.convertirAResponseDTO(guardada);
    }

    @Transactional
    @Auditable(action = "DESACTIVAR_CONCILIACION", entityName = "Conciliacion")
    public void desactivar(Long id, Long versionEsperada) {
        conciliacionAccessService.validarPuedeDesactivarConciliacion(id);

        Conciliacion conciliacion = conciliacionRelacionService.obtenerConciliacionActiva(id);

        concurrenciaOptimistaValidator.validarVersion(
                versionEsperada,
                conciliacion.getVersion(),
                "conciliación");

        conciliacionValidator.validarConciliacionNoFinalizada(conciliacion);
        conciliacionValidator.validarConsultaPermiteOperacionConciliacion(conciliacion.getConsulta());

        // Desactivación lógica. No representa finalización de la conciliación.
        conciliacion.setActivo(false);

        conciliacionRepository.save(conciliacion);
        entityManager.flush();

        // Una conciliación desactivada no debe conservar recordatorios pendientes.
        reunionConciliacionNotificacionService.cancelarPendientesPorConciliacion(id);
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

    private void compensarArchivoAlmacenado(FileAsset asset, RuntimeException causaOriginal) {
        try {
            fileResourceService.discardStoredAsset(asset);
        } catch (RuntimeException cleanup) {
            causaOriginal.addSuppressed(cleanup);
        }
    }
}
