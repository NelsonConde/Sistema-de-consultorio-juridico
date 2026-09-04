package co.edu.ufps.legal_cases.business.controller.proceso;

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

import co.edu.ufps.legal_cases.business.dto.proceso.ProcesoResumenDTO;
import co.edu.ufps.legal_cases.business.model.proceso.EstadoProceso;
import co.edu.ufps.legal_cases.business.service.proceso.ProcesoService;
import co.edu.ufps.legal_cases.common.dto.PageResponseDTO;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.common.exception.handler.GlobalExceptionHandler;

class ProcesoControllerTest {

    private ProcesoService procesoService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        procesoService = mock(ProcesoService.class);
        ProcesoController controller = new ProcesoController(procesoService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void buscarSinParametrosDebePropagarDefaultsYResponderPageResponseDTO() throws Exception {
        ProcesoResumenDTO proceso = new ProcesoResumenDTO(
                5L,
                1L,
                "RAD-2026-000000000001",
                2L,
                "Norte de Santander",
                3L,
                "Consulta civil",
                4L,
                "Juzgado",
                6L,
                "Familia",
                EstadoProceso.PENDIENTE,
                true,
                LocalDateTime.of(2026, 9, 4, 10, 30));
        PageResponseDTO<ProcesoResumenDTO> respuesta = new PageResponseDTO<>(
                List.of(proceso),
                1,
                10,
                1,
                1);
        when(procesoService.buscarParaUsuarioActual(
                null, 1, 10, "id", "desc", null, null, null))
                .thenReturn(respuesta);

        mockMvc.perform(get("/api/procesos"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].id").value(5))
                .andExpect(jsonPath("$.content[0].version").value(1))
                .andExpect(jsonPath("$.content[0].numeroRadicado").value("RAD-2026-000000000001"))
                .andExpect(jsonPath("$.content[0].departamentoId").value(2))
                .andExpect(jsonPath("$.content[0].departamentoNombre").value("Norte de Santander"))
                .andExpect(jsonPath("$.content[0].consultaId").value(3))
                .andExpect(jsonPath("$.content[0].consulta").value("Consulta civil"))
                .andExpect(jsonPath("$.content[0].organoControlId").value(4))
                .andExpect(jsonPath("$.content[0].organoControlNombre").value("Juzgado"))
                .andExpect(jsonPath("$.content[0].especialidadId").value(6))
                .andExpect(jsonPath("$.content[0].especialidadNombre").value("Familia"))
                .andExpect(jsonPath("$.content[0].estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.content[0].activo").value(true))
                .andExpect(jsonPath("$.content[0].fechaCreacion").value("2026-09-04T10:30:00"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(procesoService).buscarParaUsuarioActual(
                null, 1, 10, "id", "desc", null, null, null);
    }

    @Test
    void buscarConParametrosExplicitosDebePropagarlosAlService() throws Exception {
        when(procesoService.buscarParaUsuarioActual(
                "radicado", 2, 20, "numeroRadicado", "asc",
                EstadoProceso.SENTENCIA_FAVORABLE,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30)))
                .thenReturn(new PageResponseDTO<>(List.of(), 2, 20, 0, 0));

        mockMvc.perform(get("/api/procesos")
                        .param("search", "radicado")
                        .param("page", "2")
                        .param("size", "20")
                        .param("sortBy", "numeroRadicado")
                        .param("direction", "asc")
                        .param("estado", "SENTENCIA_FAVORABLE")
                        .param("fechaDesde", "2026-09-01")
                        .param("fechaHasta", "2026-09-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(20));

        verify(procesoService).buscarParaUsuarioActual(
                "radicado", 2, 20, "numeroRadicado", "asc",
                EstadoProceso.SENTENCIA_FAVORABLE,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30));
    }

    @Test
    void businessExceptionEnListadoDebeResponder400() throws Exception {
        when(procesoService.buscarParaUsuarioActual(
                null, 1, 51, "id", "desc", null, null, null))
                .thenThrow(new BusinessException("El tamaño de página debe estar entre 1 y 50"));

        mockMvc.perform(get("/api/procesos")
                        .param("size", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400))
                .andExpect(jsonPath("$.error").value("Error de negocio"))
                .andExpect(jsonPath("$.mensaje").value("El tamaño de página debe estar entre 1 y 50"));
    }

    @Test
    void accessDeniedEnListadoDebeResponder403() throws Exception {
        when(procesoService.buscarParaUsuarioActual(
                null, 1, 10, "id", "desc", null, null, null))
                .thenThrow(new AccessDeniedException("Acceso denegado"));

        mockMvc.perform(get("/api/procesos"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.estado").value(403))
                .andExpect(jsonPath("$.error").value("No autorizado"))
                .andExpect(jsonPath("$.mensaje")
                        .value("No tiene permisos para acceder a este recurso"))
                .andExpect(jsonPath("$.ruta").value("/api/procesos"));
    }
}
