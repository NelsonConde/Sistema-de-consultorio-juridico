package co.edu.ufps.legal_cases.business.service.agenda;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_CONCILIACIONES;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_SEGUIMIENTOS;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.dto.agenda.AgendaEventDTO;
import co.edu.ufps.legal_cases.business.model.conciliacion.Conciliacion;
import co.edu.ufps.legal_cases.business.model.consulta.EstadoConsulta;
import co.edu.ufps.legal_cases.business.repository.conciliacion.reunion.ReunionConciliacionRepository;
import co.edu.ufps.legal_cases.business.service.acceso.conciliacion.ConciliacionAccessService;
import co.edu.ufps.legal_cases.business.service.acceso.conciliacion.ConciliacionAlcanceService;
import co.edu.ufps.legal_cases.business.service.seguimiento.SeguimientoService;
import co.edu.ufps.legal_cases.business.dto.seguimiento.SeguimientoResponseDTO;
import co.edu.ufps.legal_cases.security.service.context.UsuarioActualService;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AgendaQueryService {

    private static final String FOLLOW_UP = "FOLLOW_UP";
    private static final String CONCILIATION_MEETING = "CONCILIATION_MEETING";

    private final SeguimientoService seguimientoService;
    private final ReunionConciliacionRepository reunionRepository;
    private final ConciliacionAccessService conciliacionAccessService;
    private final ConciliacionAlcanceService conciliacionAlcanceService;
    private final UsuarioActualService usuarioActualService;
    private final ZoneId institutionalTimeZone;

    @Transactional(readOnly = true)
    public List<AgendaEventDTO> listar(LocalDate from, LocalDate to) {
        validarRango(from, to);
        boolean puedeVerSeguimientos = usuarioActualService.tienePermiso(VER_SEGUIMIENTOS);
        boolean puedeVerConciliaciones = usuarioActualService.tienePermiso(VER_CONCILIACIONES);

        if (!puedeVerSeguimientos && !puedeVerConciliaciones) {
            throw new AccessDeniedException("No tiene permisos para consultar la agenda");
        }

        List<AgendaEventDTO> eventos = new ArrayList<>();
        if (puedeVerSeguimientos) {
            eventos.addAll(mapSeguimientos(seguimientoService.listarParaCalendario(), from, to));
        }
        if (puedeVerConciliaciones) {
            conciliacionAccessService.validarPuedeListarConciliaciones();
            eventos.addAll(mapReuniones(from, to));
        }

        return eventos.stream()
                .sorted(Comparator.comparing(AgendaEventDTO::start))
                .toList();
    }

    private List<AgendaEventDTO> mapSeguimientos(
            List<SeguimientoResponseDTO> seguimientos, LocalDate from, LocalDate to) {
        return seguimientos.stream()
                .filter(seg -> seg.getFechaEntrega() != null)
                .filter(seg -> !seg.getFechaEntrega().isBefore(from) && seg.getFechaEntrega().isBefore(to))
                .map(seg -> {
                    OffsetDateTime start = atStartOfDay(seg.getFechaEntrega());
                    return new AgendaEventDTO(
                            "seguimiento-" + seg.getId(),
                            FOLLOW_UP,
                            seg.getDescripcion() != null ? seg.getDescripcion() : "Seguimiento",
                            start,
                            start.plusDays(1),
                            true,
                            seg.getId(),
                            seg.getConsultaId(),
                            seg.getEstado() != null ? seg.getEstado().name() : null,
                            seg.getFechaEntrega().isBefore(LocalDate.now(institutionalTimeZone)));
                })
                .toList();
    }

    private List<AgendaEventDTO> mapReuniones(LocalDate from, LocalDate to) {
        return reunionRepository.findAll().stream()
                .filter(reunion -> reunion.getFechaReunion() != null)
                .filter(reunion -> reunion.getConciliacion() != null)
                .filter(reunion -> reunion.getConciliacion().getConsulta() != null)
                .filter(reunion -> reunion.getConciliacion().getConsulta().getEstado() != EstadoConsulta.ARCHIVADO)
                .filter(reunion -> conciliacionAlcanceService.puedeVerConciliacion(reunion.getConciliacion()))
                .map(reunion -> {
                    Conciliacion conciliacion = reunion.getConciliacion();
                    OffsetDateTime start = reunion.getFechaReunion().atZone(institutionalTimeZone).toOffsetDateTime();
                    return new AgendaEventDTO(
                            "reunion-" + reunion.getConciliacionId(),
                            CONCILIATION_MEETING,
                            "Reunión de conciliación",
                            start,
                            start.plusHours(1),
                            false,
                            reunion.getConciliacionId(),
                            conciliacion.getConsulta().getId(),
                            conciliacion.getEstado() != null ? conciliacion.getEstado().getCodigo() : null,
                            start.isBefore(OffsetDateTime.now(institutionalTimeZone)));
                })
                .filter(event -> !event.start().toLocalDate().isBefore(from)
                        && event.start().toLocalDate().isBefore(to))
                .toList();
    }

    private OffsetDateTime atStartOfDay(LocalDate date) {
        return date.atStartOfDay(institutionalTimeZone).toOffsetDateTime();
    }

    private void validarRango(LocalDate from, LocalDate to) {
        if (from == null || to == null || !from.isBefore(to)) {
            throw new BusinessException("El rango de agenda debe ser válido y no vacío");
        }
        if (from.plusMonths(3).isBefore(to)) {
            throw new BusinessException("El rango de agenda no puede superar tres meses");
        }
    }
}
