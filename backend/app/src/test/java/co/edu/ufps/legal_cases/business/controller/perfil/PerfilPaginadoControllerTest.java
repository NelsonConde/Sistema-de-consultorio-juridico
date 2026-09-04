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

import co.edu.ufps.legal_cases.business.dto.perfil.AdministrativoResumenDTO;
import co.edu.ufps.legal_cases.business.dto.perfil.AsesorResumenDTO;
import co.edu.ufps.legal_cases.business.dto.perfil.MonitorResumenDTO;
import co.edu.ufps.legal_cases.business.service.perfil.AdministrativoService;
import co.edu.ufps.legal_cases.business.service.perfil.AsesorService;
import co.edu.ufps.legal_cases.business.service.perfil.MonitorService;
import co.edu.ufps.legal_cases.common.dto.PageResponseDTO;

class PerfilPaginadoControllerTest {

    private AdministrativoService administrativoService;
    private AsesorService asesorService;
    private MonitorService monitorService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        administrativoService = mock(AdministrativoService.class);
        asesorService = mock(AsesorService.class);
        monitorService = mock(MonitorService.class);

        mockMvc = MockMvcBuilders.standaloneSetup(
                new AdministrativoController(administrativoService),
                new AsesorController(asesorService),
                new MonitorController(monitorService))
                .build();
    }

    @Test
    void administrativosSinParametrosDebePropagarDefaultsYResponderPageResponseDTO() throws Exception {
        AdministrativoResumenDTO administrativo = administrativoResumen();
        when(administrativoService.buscar(null, 1, 10, "id", "desc", null))
                .thenReturn(new PageResponseDTO<>(List.of(administrativo), 1, 10, 1, 1));

        mockMvc.perform(get("/api/administrativos"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].nombre").value("Administrativa A"))
                .andExpect(jsonPath("$.content[0].documento").value("DOC-A"))
                .andExpect(jsonPath("$.content[0].email").value("admin@example.test"))
                .andExpect(jsonPath("$.content[0].usuario").value("admin.a"))
                .andExpect(jsonPath("$.content[0].codigo").value("ADM-A"))
                .andExpect(jsonPath("$.content[0].activo").value(true))
                .andExpect(jsonPath("$.content[0].directora").value(false))
                .andExpect(jsonPath("$.content[0].sedeId").value(10))
                .andExpect(jsonPath("$.content[0].sedeNombre").value("Principal"))
                .andExpect(jsonPath("$.content[0].password").doesNotExist())
                .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$.content[0].permisos").doesNotExist())
                .andExpect(jsonPath("$.content[0].token").doesNotExist())
                .andExpect(jsonPath("$.content[0].secret").doesNotExist())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(administrativoService).buscar(null, 1, 10, "id", "desc", null);
    }

    @Test
    void administrativosConParametrosExplicitosDebeDelegar() throws Exception {
        when(administrativoService.buscar("ana", 2, 20, "nombre", "asc", true))
                .thenReturn(new PageResponseDTO<>(List.of(), 2, 20, 0, 0));

        mockMvc.perform(get("/api/administrativos")
                        .param("search", "ana")
                        .param("page", "2")
                        .param("size", "20")
                        .param("sortBy", "nombre")
                        .param("direction", "asc")
                        .param("activo", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(2));

        verify(administrativoService).buscar("ana", 2, 20, "nombre", "asc", true);
    }

    @Test
    void asesoresSinParametrosDebePropagarDefaultsYResponderPageResponseDTO() throws Exception {
        AsesorResumenDTO asesor = asesorResumen();
        when(asesorService.buscar(null, 1, 10, "id", "desc", null))
                .thenReturn(new PageResponseDTO<>(List.of(asesor), 1, 10, 1, 1));

        mockMvc.perform(get("/api/asesores"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].id").value(2))
                .andExpect(jsonPath("$.content[0].nombre").value("Asesor A"))
                .andExpect(jsonPath("$.content[0].areaId").value(20))
                .andExpect(jsonPath("$.content[0].areaNombre").value("Civil"))
                .andExpect(jsonPath("$.content[0].sedeId").value(10))
                .andExpect(jsonPath("$.content[0].sedeNombre").value("Principal"))
                .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$.content[0].permisos").doesNotExist())
                .andExpect(jsonPath("$.content[0].token").doesNotExist())
                .andExpect(jsonPath("$.content[0].secret").doesNotExist())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(asesorService).buscar(null, 1, 10, "id", "desc", null);
    }

    @Test
    void asesoresConParametrosExplicitosDebeDelegar() throws Exception {
        when(asesorService.buscar("civil", 2, 20, "areaNombre", "asc", false))
                .thenReturn(new PageResponseDTO<>(List.of(), 2, 20, 0, 0));

        mockMvc.perform(get("/api/asesores")
                        .param("search", "civil")
                        .param("page", "2")
                        .param("size", "20")
                        .param("sortBy", "areaNombre")
                        .param("direction", "asc")
                        .param("activo", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.size").value(20));

        verify(asesorService).buscar("civil", 2, 20, "areaNombre", "asc", false);
    }

    @Test
    void monitoresSinParametrosDebePropagarDefaultsYResponderPageResponseDTO() throws Exception {
        MonitorResumenDTO monitor = monitorResumen();
        when(monitorService.buscar(null, 1, 10, "id", "desc", null))
                .thenReturn(new PageResponseDTO<>(List.of(monitor), 1, 10, 1, 1));

        mockMvc.perform(get("/api/monitores"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].id").value(3))
                .andExpect(jsonPath("$.content[0].nombre").value("Monitor A"))
                .andExpect(jsonPath("$.content[0].sedeId").value(10))
                .andExpect(jsonPath("$.content[0].sedeNombre").value("Principal"))
                .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$.content[0].permisos").doesNotExist())
                .andExpect(jsonPath("$.content[0].token").doesNotExist())
                .andExpect(jsonPath("$.content[0].secret").doesNotExist())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(monitorService).buscar(null, 1, 10, "id", "desc", null);
    }

    @Test
    void monitoresConParametrosExplicitosDebeDelegar() throws Exception {
        when(monitorService.buscar("turno", 2, 20, "sedeNombre", "asc", true))
                .thenReturn(new PageResponseDTO<>(List.of(), 2, 20, 0, 0));

        mockMvc.perform(get("/api/monitores")
                        .param("search", "turno")
                        .param("page", "2")
                        .param("size", "20")
                        .param("sortBy", "sedeNombre")
                        .param("direction", "asc")
                        .param("activo", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(2));

        verify(monitorService).buscar("turno", 2, 20, "sedeNombre", "asc", true);
    }

    private AdministrativoResumenDTO administrativoResumen() {
        AdministrativoResumenDTO dto = new AdministrativoResumenDTO();
        dto.setId(1L);
        dto.setNombre("Administrativa A");
        dto.setDocumento("DOC-A");
        dto.setEmail("admin@example.test");
        dto.setUsuario("admin.a");
        dto.setCodigo("ADM-A");
        dto.setActivo(true);
        dto.setDirectora(false);
        dto.setSedeId(10L);
        dto.setSedeNombre("Principal");
        return dto;
    }

    private AsesorResumenDTO asesorResumen() {
        AsesorResumenDTO dto = new AsesorResumenDTO();
        dto.setId(2L);
        dto.setNombre("Asesor A");
        dto.setDocumento("DOC-B");
        dto.setEmail("asesor@example.test");
        dto.setUsuario("asesor.a");
        dto.setCodigo("ASE-A");
        dto.setActivo(true);
        dto.setAreaId(20L);
        dto.setAreaNombre("Civil");
        dto.setSedeId(10L);
        dto.setSedeNombre("Principal");
        return dto;
    }

    private MonitorResumenDTO monitorResumen() {
        MonitorResumenDTO dto = new MonitorResumenDTO();
        dto.setId(3L);
        dto.setNombre("Monitor A");
        dto.setDocumento("DOC-C");
        dto.setEmail("monitor@example.test");
        dto.setUsuario("monitor.a");
        dto.setCodigo("MON-A");
        dto.setActivo(true);
        dto.setSedeId(10L);
        dto.setSedeNombre("Principal");
        return dto;
    }
}
