package co.edu.ufps.legal_cases.business.controller.agenda;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_CONCILIACIONES;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_SEGUIMIENTOS;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.ufps.legal_cases.business.dto.agenda.AgendaEventDTO;
import co.edu.ufps.legal_cases.business.service.agenda.AgendaQueryService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/agenda")
@RequiredArgsConstructor
public class AgendaController {

    private final AgendaQueryService agendaQueryService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('" + VER_SEGUIMIENTOS + "', '" + VER_CONCILIACIONES + "')")
    public List<AgendaEventDTO> listar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return agendaQueryService.listar(from, to);
    }
}
