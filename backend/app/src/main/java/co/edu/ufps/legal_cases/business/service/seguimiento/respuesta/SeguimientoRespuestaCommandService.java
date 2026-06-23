package co.edu.ufps.legal_cases.business.service.seguimiento.respuesta;

import static co.edu.ufps.legal_cases.common.util.ComparacionUtils.equalsIgnoreCase;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.audit.aop.log.Auditable;

import co.edu.ufps.legal_cases.business.dto.seguimiento.respuesta.SeguimientoRespuestaDecisionDTO;
import co.edu.ufps.legal_cases.business.dto.seguimiento.respuesta.SeguimientoRespuestaRequestDTO;
import co.edu.ufps.legal_cases.business.dto.seguimiento.respuesta.SeguimientoRespuestaResponseDTO;
import co.edu.ufps.legal_cases.business.model.perfil.Estudiante;
import co.edu.ufps.legal_cases.business.model.seguimiento.Seguimiento;
import co.edu.ufps.legal_cases.business.model.seguimiento.respuesta.EstadoRespuestaSeguimiento;
import co.edu.ufps.legal_cases.business.model.seguimiento.respuesta.SeguimientoRespuesta;
import co.edu.ufps.legal_cases.business.repository.perfil.EstudianteRepository;
import co.edu.ufps.legal_cases.business.repository.seguimiento.SeguimientoRepository;
import co.edu.ufps.legal_cases.business.repository.seguimiento.respuesta.SeguimientoRespuestaRepository;
import co.edu.ufps.legal_cases.business.service.acceso.seguimiento.SeguimientoRespuestaAccessService;
import co.edu.ufps.legal_cases.business.service.seguimiento.seguimiento.SeguimientoEstadoService;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.repository.account.UsuarioSistemaRepository;

@Service
public class SeguimientoRespuestaCommandService {

    private final SeguimientoRespuestaRepository seguimientoRespuestaRepository;
    private final SeguimientoRepository seguimientoRepository;
    private final EstudianteRepository estudianteRepository;
    private final UsuarioSistemaRepository usuarioSistemaRepository;
    private final SeguimientoRespuestaAccessService seguimientoRespuestaAccessService;
    private final SeguimientoRespuestaValidator seguimientoRespuestaValidator;
    private final SeguimientoRespuestaMapper seguimientoRespuestaMapper;
    private final SeguimientoEstadoService seguimientoEstadoService;

    public SeguimientoRespuestaCommandService(
            SeguimientoRespuestaRepository seguimientoRespuestaRepository,
            SeguimientoRepository seguimientoRepository,
            EstudianteRepository estudianteRepository,
            UsuarioSistemaRepository usuarioSistemaRepository,
            SeguimientoRespuestaAccessService seguimientoRespuestaAccessService,
            SeguimientoRespuestaValidator seguimientoRespuestaValidator,
            SeguimientoRespuestaMapper seguimientoRespuestaMapper,
            SeguimientoEstadoService seguimientoEstadoService) {
        this.seguimientoRespuestaRepository = seguimientoRespuestaRepository;
        this.seguimientoRepository = seguimientoRepository;
        this.estudianteRepository = estudianteRepository;
        this.usuarioSistemaRepository = usuarioSistemaRepository;
        this.seguimientoRespuestaAccessService = seguimientoRespuestaAccessService;
        this.seguimientoRespuestaValidator = seguimientoRespuestaValidator;
        this.seguimientoRespuestaMapper = seguimientoRespuestaMapper;
        this.seguimientoEstadoService = seguimientoEstadoService;
    }

    @Transactional
    @Auditable(action = "CREAR_RESPUESTA_SEGUIMIENTO", entityName = "SeguimientoRespuesta")
    public SeguimientoRespuestaResponseDTO crear(Long seguimientoId, SeguimientoRespuestaRequestDTO dto) {
        seguimientoRespuestaAccessService.validarPuedeResponderSeguimiento(seguimientoId);
        seguimientoRespuestaValidator.validarCreacion(dto);

        Long estudianteActualId = seguimientoRespuestaAccessService.obtenerEstudianteActualId();

        Seguimiento seguimiento = obtenerSeguimientoActivo(seguimientoId);
        seguimientoEstadoService.validarPermiteRespuesta(seguimiento);
        validarPuedeCrearNuevoIntento(seguimientoId, estudianteActualId);

        Estudiante estudiante = obtenerEstudianteActivo(estudianteActualId);

        SeguimientoRespuesta respuesta = new SeguimientoRespuesta();
        respuesta.setSeguimiento(seguimiento);
        respuesta.setEstudiante(estudiante);
        respuesta.setContenido(seguimientoRespuestaValidator.normalizarContenido(dto.getContenido()));
        respuesta.setEstado(EstadoRespuestaSeguimiento.PENDIENTE);

        // El sistema permite responder tarde, pero deja marcado si fue fuera de plazo.
        respuesta.setFueraPlazo(estaFueraDePlazo(seguimiento));

        respuesta.setActivo(true);

        return seguimientoRespuestaMapper.convertirAResponseDTO(
                seguimientoRespuestaRepository.save(respuesta));
    }

    @Transactional
    @Auditable(action = "ACTUALIZAR_RESPUESTA_SEGUIMIENTO", entityName = "SeguimientoRespuesta")
    public SeguimientoRespuestaResponseDTO actualizar(Long id, SeguimientoRespuestaRequestDTO dto) {
        seguimientoRespuestaAccessService.validarPuedeEditarRespuesta(id);
        seguimientoRespuestaValidator.validarActualizacion(id, dto);

        SeguimientoRespuesta respuesta = obtenerRespuestaActiva(id);
        seguimientoEstadoService.validarPermiteRespuesta(respuesta.getSeguimiento());
        String contenido = seguimientoRespuestaValidator.normalizarContenido(dto.getContenido());

        if (equalsIgnoreCase(respuesta.getContenido(), contenido)) {
            throw new BusinessException("No hay cambios para actualizar");
        }

        respuesta.setContenido(contenido);

        // Si se edita después del plazo, queda marcada como fuera de plazo.
        // Si ya estaba marcada, se conserva.
        respuesta.setFueraPlazo(
                Boolean.TRUE.equals(respuesta.getFueraPlazo())
                        || estaFueraDePlazo(respuesta.getSeguimiento()));

        return seguimientoRespuestaMapper.convertirAResponseDTO(
                seguimientoRespuestaRepository.save(respuesta));
    }

    @Transactional
    @Auditable(action = "APROBAR/RECHAZAR_RESPUESTA", entityName = "SeguimientoRespuesta")
    public SeguimientoRespuestaResponseDTO decidir(Long id, SeguimientoRespuestaDecisionDTO dto) {
        seguimientoRespuestaAccessService.validarPuedeRevisarRespuesta(id);
        seguimientoRespuestaValidator.validarDecision(dto);

        SeguimientoRespuesta respuesta = obtenerRespuestaActiva(id);
        seguimientoEstadoService.validarPermiteRespuesta(respuesta.getSeguimiento());

        UsuarioSistema revisor = obtenerUsuarioActual();

        respuesta.setEstado(dto.getEstado());
        respuesta.setObservacionRevision(
                seguimientoRespuestaValidator.normalizarObservacionRevision(dto.getObservacionRevision()));
        respuesta.setRevisadoPor(revisor);
        respuesta.setFechaDecision(LocalDateTime.now());

        SeguimientoRespuesta respuestaGuardada = seguimientoRespuestaRepository.save(respuesta);

        if (EstadoRespuestaSeguimiento.APROBADA.equals(respuestaGuardada.getEstado())) {
            seguimientoEstadoService.completarPorRespuestaAprobada(respuestaGuardada);
        }

        return seguimientoRespuestaMapper.convertirAResponseDTO(respuestaGuardada);
    }

    private void validarPuedeCrearNuevoIntento(Long seguimientoId, Long estudianteId) {
        seguimientoRespuestaRepository
                .findFirstBySeguimiento_IdAndEstudiante_IdAndActivoTrueOrderByFechaCreacionDescIdDesc(
                        seguimientoId,
                        estudianteId)
                .ifPresent(ultimaRespuesta -> {
                    if (ultimaRespuesta.getEstado() == EstadoRespuestaSeguimiento.PENDIENTE) {
                        throw new BusinessException("Ya existe una respuesta pendiente para este seguimiento");
                    }

                    if (ultimaRespuesta.getEstado() == EstadoRespuestaSeguimiento.APROBADA) {
                        throw new BusinessException(
                                "La respuesta ya fue aprobada y no se pueden enviar más respuestas");
                    }

                    // Si la última respuesta fue RECHAZADA, se permite crear un nuevo intento.
                    // Si está fuera de plazo, el nuevo intento se crea igual, pero queda marcado.
                });
    }

    private boolean estaFueraDePlazo(Seguimiento seguimiento) {
        if (seguimiento == null || seguimiento.getFechaEntrega() == null) {
            return false;
        }

        return LocalDateTime.now()
                .toLocalDate()
                .isAfter(seguimiento.getFechaEntrega());
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
                .orElseThrow(() -> new BusinessException(
                        "Respuesta de seguimiento no encontrada con id: " + respuestaId));
    }

    private Estudiante obtenerEstudianteActivo(Long estudianteId) {
        if (estudianteId == null) {
            throw new BusinessException("El id del estudiante es obligatorio");
        }

        return estudianteRepository.findByIdAndActivoTrue(estudianteId)
                .orElseThrow(() -> new BusinessException(
                        "Estudiante no encontrado o inactivo con id: " + estudianteId));
    }

    private UsuarioSistema obtenerUsuarioActual() {
        Long usuarioActualId = seguimientoRespuestaAccessService.obtenerUsuarioActualId();

        return usuarioSistemaRepository.findById(usuarioActualId)
                .orElseThrow(() -> new BusinessException("Usuario revisor no encontrado con id: " + usuarioActualId));
    }
}