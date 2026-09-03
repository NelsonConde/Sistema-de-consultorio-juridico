package co.edu.ufps.legal_cases.business.controller.persona;

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

import co.edu.ufps.legal_cases.business.dto.persona.PersonaPageResponseDTO;
import co.edu.ufps.legal_cases.business.dto.persona.PersonaResumenDTO;
import co.edu.ufps.legal_cases.business.service.persona.PersonaService;
import co.edu.ufps.legal_cases.common.exception.handler.GlobalExceptionHandler;
import co.edu.ufps.legal_cases.common.exception.ResourceNotFoundException;

class PersonaControllerTest {

    private PersonaService personaService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        personaService = mock(PersonaService.class);
        PersonaController controller = new PersonaController(personaService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listadoDebeSerializarUnicamenteElContratoMinimoPaginado() throws Exception {
        PersonaResumenDTO persona = new PersonaResumenDTO(
                7L,
                "Ana",
                "Perez",
                "CC",
                "******3456",
                "Solicitante",
                true);
        PersonaPageResponseDTO respuesta = new PersonaPageResponseDTO(
                List.of(persona),
                2,
                10,
                21,
                3);
        when(personaService.listar("Ana", 2, 10)).thenReturn(respuesta);

        mockMvc.perform(get("/api/personas")
                        .param("search", "Ana")
                        .param("page", "2")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].id").value(7))
                .andExpect(jsonPath("$.content[0].nombres").value("Ana"))
                .andExpect(jsonPath("$.content[0].apellidos").value("Perez"))
                .andExpect(jsonPath("$.content[0].tipoDocumento").value("CC"))
                .andExpect(jsonPath("$.content[0].numeroDocumentoEnmascarado").value("******3456"))
                .andExpect(jsonPath("$.content[0].tipoPersona").value("Solicitante"))
                .andExpect(jsonPath("$.content[0].activo").value(true))
                .andExpect(jsonPath("$.content[0].numeroDocumento").doesNotExist())
                .andExpect(jsonPath("$.content[0].correo").doesNotExist())
                .andExpect(jsonPath("$.content[0].telefono").doesNotExist())
                .andExpect(jsonPath("$.content[0].direccion").doesNotExist())
                .andExpect(jsonPath("$.content[0].salario").doesNotExist())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(21))
                .andExpect(jsonPath("$.totalPages").value(3));

        verify(personaService).listar("Ana", 2, 10);
    }

    @Test
    void listadoNoDebeContenerElDocumentoReal() throws Exception {
        PersonaResumenDTO persona = new PersonaResumenDTO(
                7L,
                "Ana",
                "Perez",
                "CC",
                "******3456",
                "Solicitante",
                true);
        when(personaService.listar(null, 1, 10))
                .thenReturn(new PersonaPageResponseDTO(List.of(persona), 1, 10, 1, 1));

        mockMvc.perform(get("/api/personas"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("1090123456"))));
    }

    @Test
    void activosDebeDelegarBusquedaYPaginacionConElMismoContrato() throws Exception {
        when(personaService.listarActivos("Perez", 1, 50))
                .thenReturn(new PersonaPageResponseDTO(List.of(), 1, 50, 0, 0));

        mockMvc.perform(get("/api/personas/activos")
                        .param("search", "Perez")
                        .param("page", "1")
                        .param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.size").value(50));

        verify(personaService).listarActivos("Perez", 1, 50);
    }

    @Test
    void detalleNoDisponibleDebeResponder404SinRevelarExistencia() throws Exception {
        when(personaService.obtenerPorId(99L))
                .thenThrow(new ResourceNotFoundException("Persona no encontrada"));

        mockMvc.perform(get("/api/personas/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.estado").value(404))
                .andExpect(jsonPath("$.error").value("Recurso no encontrado"))
                .andExpect(jsonPath("$.mensaje").value("Persona no encontrada"))
                .andExpect(jsonPath("$.ruta").value("/api/personas/99"));
    }
}