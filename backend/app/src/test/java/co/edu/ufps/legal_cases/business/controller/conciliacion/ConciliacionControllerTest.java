package co.edu.ufps.legal_cases.business.controller.conciliacion;

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

import co.edu.ufps.legal_cases.business.dto.conciliacion.ConciliacionResumenDTO;
import co.edu.ufps.legal_cases.business.service.conciliacion.ConciliacionService;
import co.edu.ufps.legal_cases.common.dto.PageResponseDTO;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.common.exception.handler.GlobalExceptionHandler;

class ConciliacionControllerTest {

    private ConciliacionService conciliacionService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        conciliacionService = mock(ConciliacionService.class);
        ConciliacionController controller = new ConciliacionController(conciliacionService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void buscarSinParametrosDebePropagarDefaultsYResponderPageResponseDTO() throws Exception {
        ConciliacionResumenDTO conciliacion = new ConciliacionResumenDTO(
                5L,
                1L,
                2L,
                "Consulta civil",
                "EN_ESPERA",
                "En espera",
                3L,
                "Estudiante A",
                4L,
                "Conciliador A",
                LocalDateTime.of(2026, 9, 4, 10, 30),
                LocalDateTime.of(2026, 9, 5, 10, 30),
                null,
                true);
        PageResponseDTO<ConciliacionResumenDTO> respuesta = new PageResponseDTO<>(
                List.of(conciliacion),
                1,
                10,
                1,
                1);
        when(conciliacionService.buscarParaUsuarioActual(
                null, 1, 10, "id", "desc", null, null, null))
                .thenReturn(respuesta);

        mockMvc.perform(get("/api/conciliaciones"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].id").value(5))
                .andExpect(jsonPath("$.content[0].version").value(1))
                .andExpect(jsonPath("$.content[0].consultaId").value(2))
                .andExpect(jsonPath("$.content[0].consulta").value("Consulta civil"))
                .andExpect(jsonPath("$.content[0].estadoCodigo").value("EN_ESPERA"))
                .andExpect(jsonPath("$.content[0].estadoNombre").value("En espera"))
                .andExpect(jsonPath("$.content[0].estudianteId").value(3))
                .andExpect(jsonPath("$.content[0].estudianteNombre").value("Estudiante A"))
                .andExpect(jsonPath("$.content[0].conciliadorId").value(4))
                .andExpect(jsonPath("$.content[0].conciliadorNombre").value("Conciliador A"))
                .andExpect(jsonPath("$.content[0].fechaCreacion").value("2026-09-04T10:30:00"))
                .andExpect(jsonPath("$.content[0].fechaConciliacion").value("2026-09-05T10:30:00"))
                .andExpect(jsonPath("$.content[0].activo").value(true))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(conciliacionService).buscarParaUsuarioActual(
                null, 1, 10, "id", "desc", null, null, null);
    }

    @Test
    void buscarConParametrosExplicitosDebePropagarlosAlService() throws Exception {
        when(conciliacionService.buscarParaUsuarioActual(
                "consulta", 2, 20, "fechaCreacion", "asc", "EN_ESPERA",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30)))
                .thenReturn(new PageResponseDTO<>(List.of(), 2, 20, 0, 0));

        mockMvc.perform(get("/api/conciliaciones")
                        .param("search", "consulta")
                        .param("page", "2")
                        .param("size", "20")
                        .param("sortBy", "fechaCreacion")
                        .param("direction", "asc")
                        .param("estado", "EN_ESPERA")
                        .param("fechaDesde", "2026-09-01")
                        .param("fechaHasta", "2026-09-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(20));

        verify(conciliacionService).buscarParaUsuarioActual(
                "consulta", 2, 20, "fechaCreacion", "asc", "EN_ESPERA",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30));
    }

    @Test
    void businessExceptionEnListadoDebeResponder400() throws Exception {
        when(conciliacionService.buscarParaUsuarioActual(
                null, 1, 51, "id", "desc", null, null, null))
                .thenThrow(new BusinessException("El tamano de pagina debe estar entre 1 y 50"));

        mockMvc.perform(get("/api/conciliaciones")
                        .param("size", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400))
                .andExpect(jsonPath("$.error").value("Error de negocio"))
                .andExpect(jsonPath("$.mensaje").value("El tamano de pagina debe estar entre 1 y 50"));
    }

    @Test
    void accessDeniedEnListadoDebeResponder403() throws Exception {
        when(conciliacionService.buscarParaUsuarioActual(
                null, 1, 10, "id", "desc", null, null, null))
                .thenThrow(new AccessDeniedException("Acceso denegado"));

        mockMvc.perform(get("/api/conciliaciones"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.estado").value(403))
                .andExpect(jsonPath("$.error").value("No autorizado"))
                .andExpect(jsonPath("$.mensaje")
                        .value("No tiene permisos para acceder a este recurso"))
                .andExpect(jsonPath("$.ruta").value("/api/conciliaciones"));
    }
}
