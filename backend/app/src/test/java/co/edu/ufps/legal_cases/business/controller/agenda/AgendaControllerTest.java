package co.edu.ufps.legal_cases.business.controller.agenda;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_CONCILIACIONES;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_SEGUIMIENTOS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import co.edu.ufps.legal_cases.business.dto.agenda.AgendaEventDTO;
import co.edu.ufps.legal_cases.business.service.agenda.AgendaQueryService;

class AgendaControllerTest {

    private AgendaQueryService agendaQueryService;
    private AgendaController agendaController;

    @BeforeEach
    void setUp() {
        agendaQueryService = mock(AgendaQueryService.class);
        agendaController = new AgendaController(agendaQueryService);
    }

    @Test
    void verificaAnotacionesDeSeguridadYContrato() throws Exception {
        Method listarMethod = AgendaController.class.getDeclaredMethod("listar", LocalDate.class, LocalDate.class);

        PreAuthorize preAuthorize = listarMethod.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, "El endpoint /api/agenda debe estar protegido con @PreAuthorize");
        assertTrue(preAuthorize.value().contains(VER_SEGUIMIENTOS));
        assertTrue(preAuthorize.value().contains(VER_CONCILIACIONES));

        GetMapping getMapping = listarMethod.getAnnotation(GetMapping.class);
        assertNotNull(getMapping, "El método listar debe tener @GetMapping");

        RequestParam fromParam = listarMethod.getParameters()[0].getAnnotation(RequestParam.class);
        RequestParam toParam = listarMethod.getParameters()[1].getAnnotation(RequestParam.class);
        assertNotNull(fromParam);
        assertNotNull(toParam);

        DateTimeFormat fromFormat = listarMethod.getParameters()[0].getAnnotation(DateTimeFormat.class);
        DateTimeFormat toFormat = listarMethod.getParameters()[1].getAnnotation(DateTimeFormat.class);
        assertNotNull(fromFormat);
        assertNotNull(toFormat);
        assertEquals(DateTimeFormat.ISO.DATE, fromFormat.iso());
        assertEquals(DateTimeFormat.ISO.DATE, toFormat.iso());
    }

    @Test
    void delegaConsultaAlServicioDeAgenda() {
        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = LocalDate.of(2026, 9, 30);
        List<AgendaEventDTO> mockEventos = List.of();

        when(agendaQueryService.listar(from, to)).thenReturn(mockEventos);

        List<AgendaEventDTO> resultado = agendaController.listar(from, to);

        assertEquals(mockEventos, resultado);
        verify(agendaQueryService).listar(from, to);
    }
}
