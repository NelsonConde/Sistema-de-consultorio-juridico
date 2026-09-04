package co.edu.ufps.legal_cases.business.controller.consulta;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import co.edu.ufps.legal_cases.business.dto.consulta.ConsultaBusquedaDTO;
import co.edu.ufps.legal_cases.business.model.consulta.EstadoConsulta;
import co.edu.ufps.legal_cases.business.service.consulta.ConsultaService;
import co.edu.ufps.legal_cases.common.dto.PageResponseDTO;
import co.edu.ufps.legal_cases.common.exception.handler.GlobalExceptionHandler;

class ConsultaControllerTest {

    private ConsultaService consultaService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        consultaService = mock(ConsultaService.class);
        ConsultaController controller = new ConsultaController(consultaService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void buscarSinParametrosDebePropagarDefaultsYResponderPageResponseDTO() throws Exception {
        ConsultaBusquedaDTO consulta = new ConsultaBusquedaDTO(
                5L,
                1L,
                "Consulta civil",
                LocalDate.of(2026, 9, 3),
                "Ana",
                "Perez",
                "1090",
                EstadoConsulta.ACTIVO);
        PageResponseDTO<ConsultaBusquedaDTO> respuesta = new PageResponseDTO<>(
                List.of(consulta),
                1,
                10,
                1,
                1);
        when(consultaService.buscarParaUsuarioActual(
                null, 1, 10, "fecha", "desc", null, null, null, null, null))
                .thenReturn(respuesta);

        mockMvc.perform(get("/api/consultas"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].id").value(5))
                .andExpect(jsonPath("$.content[0].version").value(1))
                .andExpect(jsonPath("$.content[0].consulta").value("Consulta civil"))
                .andExpect(jsonPath("$.content[0].fecha").value("2026-09-03"))
                .andExpect(jsonPath("$.content[0].nombre").value("Ana"))
                .andExpect(jsonPath("$.content[0].apellido").value("Perez"))
                .andExpect(jsonPath("$.content[0].cedula").value("1090"))
                .andExpect(jsonPath("$.content[0].estado").value("ACTIVO"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(consultaService).buscarParaUsuarioActual(
                null, 1, 10, "fecha", "desc", null, null, null, null, null);
    }

    @Test
    void buscarConParametrosExplicitosDebePropagarlosAlService() throws Exception {
        when(consultaService.buscarParaUsuarioActual(
                "Ana", 2, 20, "nombre", "asc",
                3L, EstadoConsulta.CERRADO, 4L, 5L, 6L))
                .thenReturn(new PageResponseDTO<>(List.of(), 2, 20, 0, 0));

        mockMvc.perform(get("/api/consultas")
                        .param("search", "Ana")
                        .param("page", "2")
                        .param("size", "20")
                        .param("sortBy", "nombre")
                        .param("direction", "asc")
                        .param("areaId", "3")
                        .param("estado", "CERRADO")
                        .param("asesorId", "4")
                        .param("monitorId", "5")
                        .param("estudianteId", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(20));

        verify(consultaService).buscarParaUsuarioActual(
                "Ana", 2, 20, "nombre", "asc",
                3L, EstadoConsulta.CERRADO, 4L, 5L, 6L);
    }
}
