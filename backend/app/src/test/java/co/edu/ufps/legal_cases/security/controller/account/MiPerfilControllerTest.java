package co.edu.ufps.legal_cases.security.controller.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.common.exception.handler.GlobalExceptionHandler;
import co.edu.ufps.legal_cases.security.dto.account.ActualizarContactoDTO;
import co.edu.ufps.legal_cases.security.dto.account.MiPerfilDTO;
import co.edu.ufps.legal_cases.security.service.account.MiPerfilService;

class MiPerfilControllerTest {

    private MiPerfilService miPerfilService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        miPerfilService = mock(MiPerfilService.class);
        MiPerfilController controller = new MiPerfilController(miPerfilService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/mi-perfil retorna 200 con DTO completo y documento enmascarado")
    void obtenerMiPerfil_exitoso() throws Exception {
        MiPerfilDTO dto = new MiPerfilDTO();

        dto.setUsername("estudiante.demo");
        dto.setRolNombre("Estudiante Consultorio");
        dto.setTipoPerfil("ESTUDIANTE");
        dto.setNombre("Carlos Perez");
        dto.setDocumentoEnmascarado("******3456");
        dto.setEmail("carlos@ufps.edu.co");
        dto.setTelefono("+57 300 111 2233");
        dto.setSede("Sede Central");
        dto.setCodigo("1150001");

        when(miPerfilService.obtenerMiPerfil()).thenReturn(dto);

        mockMvc.perform(get("/api/mi-perfil")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("estudiante.demo"))
                .andExpect(jsonPath("$.rolNombre").value("Estudiante Consultorio"))
                .andExpect(jsonPath("$.tipoPerfil").value("ESTUDIANTE"))
                .andExpect(jsonPath("$.nombre").value("Carlos Perez"))
                .andExpect(jsonPath("$.documentoEnmascarado").value("******3456"))
                .andExpect(jsonPath("$.email").value("carlos@ufps.edu.co"))
                .andExpect(jsonPath("$.telefono").value("+57 300 111 2233"))
                .andExpect(jsonPath("$.sede").value("Sede Central"))
                .andExpect(jsonPath("$.codigo").value("1150001"))
                .andExpect(jsonPath("$.documento").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    @DisplayName("PATCH /api/mi-perfil/contacto con datos válidos retorna 200 y actualiza solo contacto")
    void actualizarContacto_validoRetorna200() throws Exception {
        MiPerfilDTO actualizado = new MiPerfilDTO();

        actualizado.setUsername("estudiante.demo");
        actualizado.setRolNombre("Estudiante Consultorio");
        actualizado.setTipoPerfil("ESTUDIANTE");
        actualizado.setNombre("Carlos Perez");
        actualizado.setDocumentoEnmascarado("******3456");
        actualizado.setEmail("carlos.nuevo@ufps.edu.co");
        actualizado.setTelefono("+57 311 222 3344");
        actualizado.setSede("Sede Central");
        actualizado.setCodigo("1150001");

        when(miPerfilService.actualizarContacto(any(ActualizarContactoDTO.class)))
                .thenReturn(actualizado);

        String jsonPayload = """
                {
                    "email": "carlos.nuevo@ufps.edu.co",
                    "telefono": "+57 311 222 3344"
                }
                """;

        mockMvc.perform(patch("/api/mi-perfil/contacto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("carlos.nuevo@ufps.edu.co"))
                .andExpect(jsonPath("$.telefono").value("+57 311 222 3344"))
                .andExpect(jsonPath("$.documentoEnmascarado").value("******3456"))
                .andExpect(jsonPath("$.codigo").value("1150001"))
                .andExpect(jsonPath("$.sede").value("Sede Central"))
                .andExpect(jsonPath("$.documento").doesNotExist());

        ArgumentCaptor<ActualizarContactoDTO> captor =
                ArgumentCaptor.forClass(ActualizarContactoDTO.class);

        verify(miPerfilService).actualizarContacto(captor.capture());

        assertEquals(
                "carlos.nuevo@ufps.edu.co",
                captor.getValue().getEmail());

        assertEquals(
                "+57 311 222 3344",
                captor.getValue().getTelefono());
    }

    @ParameterizedTest(name = "Email inválido: {0}")
    @ValueSource(strings = {"", "   ", "sin-arroba", "correo@", "@dominio.com"})
    @DisplayName("Email inválido o vacío es rechazado con 400 Bad Request")
    void actualizarContacto_emailInvalidoRetorna400(String emailInvalido) throws Exception {
        String jsonPayload = """
                {
                    "email": "%s",
                    "telefono": "+57 300 123 4567"
                }
                """.formatted(emailInvalido);

        mockMvc.perform(patch("/api/mi-perfil/contacto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400));

        verify(miPerfilService, never()).actualizarContacto(any());
    }

    @Test
    @DisplayName("Email mayor a 120 caracteres es rechazado con 400 Bad Request")
    void actualizarContacto_emailMayor120Retorna400() throws Exception {
        String emailLargo = "a".repeat(115) + "@prueba.com";

        String jsonPayload = """
                {
                    "email": "%s",
                    "telefono": "+57 300 123 4567"
                }
                """.formatted(emailLargo);

        mockMvc.perform(patch("/api/mi-perfil/contacto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400));

        verify(miPerfilService, never()).actualizarContacto(any());
    }

    @ParameterizedTest(name = "Teléfono inválido: {0}")
    @ValueSource(strings = {"", "   ", "123", "abcdefg", "+57 300 texto"})
    @DisplayName("Teléfono inválido o con formato erróneo es rechazado con 400 Bad Request")
    void actualizarContacto_telefonoInvalidoRetorna400(String telefonoInvalido) throws Exception {
        String jsonPayload = """
                {
                    "email": "valido@ufps.edu.co",
                    "telefono": "%s"
                }
                """.formatted(telefonoInvalido);

        mockMvc.perform(patch("/api/mi-perfil/contacto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400));

        verify(miPerfilService, never()).actualizarContacto(any());
    }

    @Test
    @DisplayName("Contacto duplicado lanza BusinessException y retorna 400 sin exponer detalles de BD")
    void actualizarContacto_duplicadoRetorna400() throws Exception {
        when(miPerfilService.actualizarContacto(any(ActualizarContactoDTO.class)))
                .thenThrow(
                        new BusinessException(
                                "El correo o telefono ya esta en uso por otro usuario"));

        String jsonPayload = """
                {
                    "email": "yaexiste@ufps.edu.co",
                    "telefono": "+57 300 999 8888"
                }
                """;

        mockMvc.perform(patch("/api/mi-perfil/contacto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.mensaje")
                                .value(
                                        "El correo o telefono ya esta en uso por otro usuario"));
    }

    @Test
    @DisplayName("Protección contra Mass Assignment: atributos protegidos no pueden modificarse")
    void actualizarContacto_massAssignmentProtegido() throws Exception {
        MiPerfilDTO dto = new MiPerfilDTO();

        dto.setUsername("victima");
        dto.setRolNombre("Rol Estudiante");
        dto.setTipoPerfil("ESTUDIANTE");
        dto.setNombre("Victima Inmutable");
        dto.setDocumentoEnmascarado("******1234");
        dto.setEmail("nuevo@prueba.local");
        dto.setTelefono("+57 300 123 4567");
        dto.setSede("Sede Central Original");
        dto.setCodigo("COD-ORIGINAL");

        when(miPerfilService.actualizarContacto(any(ActualizarContactoDTO.class)))
                .thenReturn(dto);

        String payloadMalicioso = """
                {
                    "id": 999,
                    "usuarioSistemaId": 888,
                    "rol": "ROLE_ADMIN",
                    "rolNombre": "Administrador",
                    "tipoPerfil": "ADMINISTRATIVO",
                    "perfil": "ADMIN",
                    "documento": "99999999",
                    "documentoEnmascarado": "HACK",
                    "codigo": "HACK-001",
                    "sede": "Sede Hacked",
                    "activo": false,
                    "password": "hackedPassword123",
                    "email": "nuevo@prueba.local",
                    "telefono": "+57 300 123 4567"
                }
                """;

        mockMvc.perform(patch("/api/mi-perfil/contacto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadMalicioso))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("nuevo@prueba.local"))
                .andExpect(jsonPath("$.telefono").value("+57 300 123 4567"))
                .andExpect(jsonPath("$.documentoEnmascarado").value("******1234"))
                .andExpect(jsonPath("$.codigo").value("COD-ORIGINAL"))
                .andExpect(jsonPath("$.sede").value("Sede Central Original"))
                .andExpect(jsonPath("$.rolNombre").value("Rol Estudiante"))
                .andExpect(jsonPath("$.tipoPerfil").value("ESTUDIANTE"))
                .andExpect(jsonPath("$.documento").doesNotExist());

        ArgumentCaptor<ActualizarContactoDTO> captor =
                ArgumentCaptor.forClass(ActualizarContactoDTO.class);

        verify(miPerfilService).actualizarContacto(captor.capture());

        ActualizarContactoDTO capturado = captor.getValue();

        assertEquals(
                "nuevo@prueba.local",
                capturado.getEmail());

        assertEquals(
                "+57 300 123 4567",
                capturado.getTelefono());
    }

    @Test
    @DisplayName("Intento de acceder a autogestión con identificador de tercero en ruta retorna 404")
    void intentoSobreTerceroEnRutaRetorna404() throws Exception {
        mockMvc.perform(get("/api/mi-perfil/999"))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/mi-perfil/contacto/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());

        verify(miPerfilService, never()).obtenerMiPerfil();
        verify(miPerfilService, never()).actualizarContacto(any());
    }
}