package co.edu.ufps.legal_cases.security.controller.account;

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

import co.edu.ufps.legal_cases.common.dto.PageResponseDTO;
import co.edu.ufps.legal_cases.security.dto.account.UsuarioSistemaResumenDTO;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.service.account.UsuarioCambioPerfilService;
import co.edu.ufps.legal_cases.security.service.account.UsuarioSistemaService;

class UsuarioSistemaControllerTest {

    private UsuarioSistemaService usuarioSistemaService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        usuarioSistemaService = mock(UsuarioSistemaService.class);
        UsuarioCambioPerfilService usuarioCambioPerfilService = mock(UsuarioCambioPerfilService.class);
        UsuarioSistemaController controller = new UsuarioSistemaController(
                usuarioSistemaService,
                usuarioCambioPerfilService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .build();
    }

    @Test
    void listarSinParametrosDebePropagarDefaultsYResponderPageResponseDTO() throws Exception {
        UsuarioSistemaResumenDTO usuario = crearUsuarioResumen();
        PageResponseDTO<UsuarioSistemaResumenDTO> respuesta = new PageResponseDTO<>(
                List.of(usuario),
                1,
                10,
                1,
                1);
        when(usuarioSistemaService.buscar(null, 1, 10, "id", "desc", null, null))
                .thenReturn(respuesta);

        mockMvc.perform(get("/api/usuarios-sistema"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].id").value(5))
                .andExpect(jsonPath("$.content[0].username").value("admin@example.test"))
                .andExpect(jsonPath("$.content[0].activo").value(true))
                .andExpect(jsonPath("$.content[0].rolId").value(6))
                .andExpect(jsonPath("$.content[0].rolNombre").value("Administrador"))
                .andExpect(jsonPath("$.content[0].tipoPerfil").value("ADMINISTRATIVO"))
                .andExpect(jsonPath("$.content[0].password").doesNotExist())
                .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$.content[0].permisos").doesNotExist())
                .andExpect(jsonPath("$.content[0].token").doesNotExist())
                .andExpect(jsonPath("$.content[0].secret").doesNotExist())
                .andExpect(jsonPath("$.content[0].perfilId").doesNotExist())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(usuarioSistemaService).buscar(null, 1, 10, "id", "desc", null, null);
    }

    @Test
    void listarConParametrosExplicitosDebePropagarlosAlService() throws Exception {
        when(usuarioSistemaService.buscar(
                "admin",
                2,
                20,
                "rolNombre",
                "asc",
                true,
                TipoPerfilUsuario.ADMINISTRATIVO))
                .thenReturn(new PageResponseDTO<>(List.of(), 2, 20, 0, 0));

        mockMvc.perform(get("/api/usuarios-sistema")
                        .param("search", "admin")
                        .param("page", "2")
                        .param("size", "20")
                        .param("sortBy", "rolNombre")
                        .param("direction", "asc")
                        .param("activo", "true")
                        .param("tipoPerfil", "ADMINISTRATIVO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(20));

        verify(usuarioSistemaService).buscar(
                "admin",
                2,
                20,
                "rolNombre",
                "asc",
                true,
                TipoPerfilUsuario.ADMINISTRATIVO);
    }

    private UsuarioSistemaResumenDTO crearUsuarioResumen() {
        UsuarioSistemaResumenDTO dto = new UsuarioSistemaResumenDTO();
        dto.setId(5L);
        dto.setUsername("admin@example.test");
        dto.setActivo(true);
        dto.setRolId(6L);
        dto.setRolNombre("Administrador");
        dto.setTipoPerfil(TipoPerfilUsuario.ADMINISTRATIVO);
        return dto;
    }
}
