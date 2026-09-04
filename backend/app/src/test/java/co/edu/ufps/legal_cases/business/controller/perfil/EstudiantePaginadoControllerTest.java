package co.edu.ufps.legal_cases.business.controller.perfil;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import co.edu.ufps.legal_cases.business.dto.perfil.EstudianteResumenDTO;
import co.edu.ufps.legal_cases.business.service.perfil.EstudianteService;
import co.edu.ufps.legal_cases.business.service.perfil.estudiante.EstudianteExcelService;
import co.edu.ufps.legal_cases.common.dto.PageResponseDTO;

class EstudiantePaginadoControllerTest {

    private EstudianteService estudianteService;
    private EstudianteExcelService estudianteExcelService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        estudianteService = mock(EstudianteService.class);
        estudianteExcelService = mock(EstudianteExcelService.class);

        mockMvc = MockMvcBuilders.standaloneSetup(
                new EstudianteController(estudianteService, estudianteExcelService))
                .build();
    }

    @Test
    void estudiantesSinParametrosDebePropagarDefaultsYResponderPageResponseDTO() throws Exception {
        EstudianteResumenDTO estudiante = estudianteResumen();
        when(estudianteService.buscar(null, 1, 10, "id", "desc", null))
                .thenReturn(new PageResponseDTO<>(List.of(estudiante), 1, 10, 1, 1));

        mockMvc.perform(get("/api/estudiantes"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].id").value(5))
                .andExpect(jsonPath("$.content[0].nombre").value("Estudiante A"))
                .andExpect(jsonPath("$.content[0].documento").value("DOC-E"))
                .andExpect(jsonPath("$.content[0].email").value("estudiante@example.test"))
                .andExpect(jsonPath("$.content[0].usuario").value("estudiante.a"))
                .andExpect(jsonPath("$.content[0].codigo").value("EST-A"))
                .andExpect(jsonPath("$.content[0].activo").value(true))
                .andExpect(jsonPath("$.content[0].sedeId").value(10))
                .andExpect(jsonPath("$.content[0].sedeNombre").value("Principal"))
                .andExpect(jsonPath("$.content[0].asesorId").value(20))
                .andExpect(jsonPath("$.content[0].asesorNombre").value("Asesor A"))
                .andExpect(jsonPath("$.content[0].conciliacion").value(true))
                .andExpect(jsonPath("$.content[0].password").doesNotExist())
                .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$.content[0].permisos").doesNotExist())
                .andExpect(jsonPath("$.content[0].token").doesNotExist())
                .andExpect(jsonPath("$.content[0].secret").doesNotExist())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(estudianteService).buscar(null, 1, 10, "id", "desc", null);
    }

    @Test
    void estudiantesConParametrosExplicitosDebeDelegar() throws Exception {
        when(estudianteService.buscar("turno", 2, 20, "sedeNombre", "asc", true))
                .thenReturn(new PageResponseDTO<>(List.of(), 2, 20, 0, 0));

        mockMvc.perform(get("/api/estudiantes")
                        .param("search", "turno")
                        .param("page", "2")
                        .param("size", "20")
                        .param("sortBy", "sedeNombre")
                        .param("direction", "asc")
                        .param("activo", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(2));

        verify(estudianteService).buscar("turno", 2, 20, "sedeNombre", "asc", true);
    }

    private EstudianteResumenDTO estudianteResumen() {
        EstudianteResumenDTO dto = new EstudianteResumenDTO();
        dto.setId(5L);
        dto.setNombre("Estudiante A");
        dto.setDocumento("DOC-E");
        dto.setEmail("estudiante@example.test");
        dto.setUsuario("estudiante.a");
        dto.setCodigo("EST-A");
        dto.setActivo(true);
        dto.setSedeId(10L);
        dto.setSedeNombre("Principal");
        dto.setAsesorId(20L);
        dto.setAsesorNombre("Asesor A");
        dto.setConciliacion(true);
        return dto;
    }
}
