package co.edu.ufps.legal_cases.business.service.conciliacion.reunion;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.audit.aop.log.Auditable;

import co.edu.ufps.legal_cases.business.dto.conciliacion.reunion.ReunionConciliacionRequestDTO;
import co.edu.ufps.legal_cases.business.dto.conciliacion.reunion.ReunionConciliacionResponseDTO;
import co.edu.ufps.legal_cases.business.model.catalogo.Sede;
import co.edu.ufps.legal_cases.business.model.conciliacion.Conciliacion;
import co.edu.ufps.legal_cases.business.model.conciliacion.EstadoConciliacion;
import co.edu.ufps.legal_cases.business.model.conciliacion.reunion.ReunionConciliacion;
import co.edu.ufps.legal_cases.business.repository.conciliacion.ConciliacionRepository;
import co.edu.ufps.legal_cases.business.repository.conciliacion.reunion.ReunionConciliacionRepository;
import co.edu.ufps.legal_cases.business.service.acceso.conciliacion.ConciliacionAccessService;
import co.edu.ufps.legal_cases.business.service.conciliacion.reunion.notificacion.ReunionConciliacionNotificacionService;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ReunionConciliacionCommandService {

    private final ConciliacionRepository conciliacionRepository;
    private final ReunionConciliacionRepository reunionConciliacionRepository;
    private final ConciliacionAccessService conciliacionAccessService;
    private final ReunionConciliacionRelacionService reunionConciliacionRelacionService;
    private final ReunionConciliacionValidator reunionConciliacionValidator;
    private final ReunionConciliacionMapper reunionConciliacionMapper;
    private final ReunionConciliacionHistorialService reunionConciliacionHistorialService;
    private final ReunionConciliacionNotificacionService reunionConciliacionNotificacionService;

    @Transactional
    @Auditable(action = "PROGRAMAR_REUNION", entityName = "ReunionConciliacion")
    public ReunionConciliacionResponseDTO programar(Long conciliacionId, ReunionConciliacionRequestDTO dto) {
        conciliacionAccessService.validarPuedeProgramarReunion(conciliacionId);

        Conciliacion conciliacion = reunionConciliacionRelacionService.obtenerConciliacionActiva(conciliacionId);
        Sede sede = reunionConciliacionRelacionService.obtenerSedeActiva(dto != null ? dto.getSedeId() : null);

        reunionConciliacionValidator.validarProgramacion(conciliacion, dto, sede);

        ReunionConciliacion reunion = new ReunionConciliacion();
        reunion.setConciliacion(conciliacion);
        reunion.setFechaReunion(dto.getFechaReunion());
        reunion.setSede(sede);
        reunion.setObservaciones(reunionConciliacionValidator.normalizarObservaciones(dto.getObservaciones()));

        ReunionConciliacion reunionGuardada = reunionConciliacionRepository.save(reunion);

        asegurarEstadoReunionProgramada(conciliacion);

        UsuarioSistema usuario = reunionConciliacionRelacionService.obtenerUsuario(
                conciliacionAccessService.obtenerUsuarioActualId());
        reunionConciliacionHistorialService.registrarProgramacion(reunionGuardada, usuario);

        // El envío de correos registra errores, pero no revierte la programación.
        reunionConciliacionNotificacionService.registrarProgramacion(reunionGuardada);

        return reunionConciliacionMapper.convertirAResponseDTO(reunionGuardada);
    }

    @Transactional
    @Auditable(action = "REPROGRAMAR_REUNION", entityName = "ReunionConciliacion")
    public ReunionConciliacionResponseDTO reprogramar(Long conciliacionId, ReunionConciliacionRequestDTO dto) {
        conciliacionAccessService.validarPuedeReprogramarReunion(conciliacionId);

        Conciliacion conciliacion = reunionConciliacionRelacionService.obtenerConciliacionActiva(conciliacionId);
        ReunionConciliacion reunion = reunionConciliacionRelacionService.obtenerReunion(conciliacionId);
        Sede sedeNueva = reunionConciliacionRelacionService.obtenerSedeActiva(dto != null ? dto.getSedeId() : null);

        reunionConciliacionValidator.validarReprogramacion(conciliacion, reunion, dto, sedeNueva);

        LocalDateTime fechaAnterior = reunion.getFechaReunion();
        Sede sedeAnterior = reunion.getSede();
        String observacionesAnteriores = reunion.getObservaciones();

        reunion.setFechaReunion(dto.getFechaReunion());
        reunion.setSede(sedeNueva);
        reunion.setObservaciones(reunionConciliacionValidator.normalizarObservaciones(dto.getObservaciones()));

        ReunionConciliacion reunionGuardada = reunionConciliacionRepository.save(reunion);

        asegurarEstadoReunionProgramada(conciliacion);

        UsuarioSistema usuario = reunionConciliacionRelacionService.obtenerUsuario(
                conciliacionAccessService.obtenerUsuarioActualId());
        reunionConciliacionHistorialService.registrarReprogramacion(
                reunionGuardada,
                fechaAnterior,
                sedeAnterior,
                observacionesAnteriores,
                usuario);

        // Al reprogramar se cancelan pendientes anteriores y se crean nuevos recordatorios.
        reunionConciliacionNotificacionService.registrarReprogramacion(reunionGuardada);

        return reunionConciliacionMapper.convertirAResponseDTO(reunionGuardada);
    }

    private void asegurarEstadoReunionProgramada(Conciliacion conciliacion) {
        EstadoConciliacion estadoReunionProgramada = reunionConciliacionRelacionService.obtenerEstadoReunionProgramada();

        if (conciliacion.getEstado() == null
                || !estadoReunionProgramada.getCodigo().equals(conciliacion.getEstado().getCodigo())) {
            conciliacion.setEstado(estadoReunionProgramada);
            conciliacionRepository.save(conciliacion);
        }
    }
}
