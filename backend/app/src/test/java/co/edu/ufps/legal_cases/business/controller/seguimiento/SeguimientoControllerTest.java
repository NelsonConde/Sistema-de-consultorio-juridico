package co.edu.ufps.legal_cases.business.controller.seguimiento;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import co.edu.ufps.legal_cases.business.dto.seguimiento.SeguimientoResumenDTO;
import co.edu.ufps.legal_cases.business.model.seguimiento.EstadoSeguimiento;
import co.edu.ufps.legal_cases.business.service.seguimiento.SeguimientoService;
import co.edu.ufps.legal_cases.common.dto.PageResponseDTO;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.common.exception.handler.GlobalExceptionHandler;

class SeguimientoControllerTest {

    private SeguimientoService seguimientoService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        seguimientoService = mock(SeguimientoService.class);
        SeguimientoController controller = new SeguimientoController(seguimientoService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void buscarSinParametrosDebePropagarDefaultsYResponderPageResponseDTO() throws Exception {
        SeguimientoResumenDTO seguimiento = new SeguimientoResumenDTO(
                5L,
                1L,
                "Seguimiento civil",
                LocalDate.of(2026, 9, 20),
                3,
                true,
                false,
                true,
                EstadoSeguimiento.PENDIENTE,
                2L,
                "Audiencia",
                3L,
                4L,
                "autor@example.test",
                LocalDateTime.of(2026, 9, 4, 10, 30),
                LocalDateTime.of(2026, 9, 5, 11, 30));
        PageResponseDTO<SeguimientoResumenDTO> respuesta = new PageResponseDTO<>(
                List.of(seguimiento),
                1,
                10,
                1,
                1);
        when(seguimientoService.buscarParaUsuarioActual(
                null, 1, 10, "id", "desc", null, null, null, null, null))
                .thenReturn(respuesta);

        mockMvc.perform(get("/api/seguimientos"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].id").value(5))
                .andExpect(jsonPath("$.content[0].version").value(1))
                .andExpect(jsonPath("$.content[0].descripcion").value("Seguimiento civil"))
                .andExpect(jsonPath("$.content[0].fechaEntrega").value("2026-09-20"))
                .andExpect(jsonPath("$.content[0].diasNotificacion").value(3))
                .andExpect(jsonPath("$.content[0].notificarPartes").value(true))
                .andExpect(jsonPath("$.content[0].notificarEstudiante").value(false))
                .andExpect(jsonPath("$.content[0].alertaDisciplinaria").value(true))
                .andExpect(jsonPath("$.content[0].estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.content[0].categoriaSeguimientoId").value(2))
                .andExpect(jsonPath("$.content[0].categoriaSeguimientoNombre").value("Audiencia"))
                .andExpect(jsonPath("$.content[0].consultaId").value(3))
                .andExpect(jsonPath("$.content[0].autorId").value(4))
                .andExpect(jsonPath("$.content[0].autorUsername").value("autor@example.test"))
                .andExpect(jsonPath("$.content[0].fechaCreacion").value("2026-09-04T10:30:00"))
                .andExpect(jsonPath("$.content[0].fechaActualizacion").value("2026-09-05T11:30:00"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(seguimientoService).buscarParaUsuarioActual(
                null, 1, 10, "id", "desc", null, null, null, null, null);
    }

    @Test
    void buscarConParametrosExplicitosDebePropagarlosAlService() throws Exception {
        when(seguimientoService.buscarParaUsuarioActual(
                "seguimiento", 2, 20, "fechaCreacion", "asc", EstadoSeguimiento.COMPLETADO,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30),
                7L,
                8L))
                .thenReturn(new PageResponseDTO<>(List.of(), 2, 20, 0, 0));

        mockMvc.perform(get("/api/seguimientos")
                        .param("search", "seguimiento")
                        .param("page", "2")
                        .param("size", "20")
                        .param("sortBy", "fechaCreacion")
                        .param("direction", "asc")
                        .param("estado", "COMPLETADO")
                        .param("fechaDesde", "2026-09-01")
                        .param("fechaHasta", "2026-09-30")
                        .param("consultaId", "7")
                        .param("autorId", "8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(20));

        verify(seguimientoService).buscarParaUsuarioActual(
                "seguimiento", 2, 20, "fechaCreacion", "asc", EstadoSeguimiento.COMPLETADO,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30),
                7L,
                8L);
    }

    @Test
    void businessExceptionEnListadoDebeResponder400() throws Exception {
        when(seguimientoService.buscarParaUsuarioActual(
                null, 1, 51, "id", "desc", null, null, null, null, null))
                .thenThrow(new BusinessException("El tamano de pagina debe estar entre 1 y 50"));

        mockMvc.perform(get("/api/seguimientos")
                        .param("size", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400))
                .andExpect(jsonPath("$.error").value("Error de negocio"))
                .andExpect(jsonPath("$.mensaje").value("El tamano de pagina debe estar entre 1 y 50"));
    }

    @Test
    void accessDeniedEnListadoDebeResponder403() throws Exception {
        when(seguimientoService.buscarParaUsuarioActual(
                null, 1, 10, "id", "desc", null, null, null, null, null))
                .thenThrow(new AccessDeniedException("Acceso denegado"));

        mockMvc.perform(get("/api/seguimientos"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.estado").value(403))
                .andExpect(jsonPath("$.error").value("No autorizado"))
                .andExpect(jsonPath("$.mensaje")
                        .value("No tiene permisos para acceder a este recurso"))
                .andExpect(jsonPath("$.ruta").value("/api/seguimientos"));
    }
}
