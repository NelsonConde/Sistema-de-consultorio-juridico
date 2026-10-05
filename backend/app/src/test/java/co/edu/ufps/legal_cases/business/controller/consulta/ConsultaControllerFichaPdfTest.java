package co.edu.ufps.legal_cases.business.controller.consulta;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import co.edu.ufps.legal_cases.business.dto.consulta.ficha.FichaExpedienteDTO;
import co.edu.ufps.legal_cases.business.dto.consulta.ficha.FichaExpedienteDTO.ClasificacionFichaDTO;
import co.edu.ufps.legal_cases.business.dto.consulta.ficha.FichaExpedienteDTO.EstadoResultadoFichaDTO;
import co.edu.ufps.legal_cases.business.dto.consulta.ficha.FichaExpedienteDTO.IdentificacionFichaDTO;
import co.edu.ufps.legal_cases.business.dto.consulta.ficha.FichaExpedienteDTO.PartesFichaDTO;
import co.edu.ufps.legal_cases.business.dto.consulta.ficha.FichaExpedienteDTO.PersonaDetalleDTO;
import co.edu.ufps.legal_cases.business.dto.consulta.ficha.FichaExpedienteDTO.ResponsableDetalleDTO;
import co.edu.ufps.legal_cases.business.dto.consulta.ficha.FichaExpedienteDTO.ResponsablesFichaDTO;
import co.edu.ufps.legal_cases.business.service.consulta.ConsultaService;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.common.exception.handler.GlobalExceptionHandler;

class ConsultaControllerFichaPdfTest {

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
    @DisplayName("GET /api/consultas/{id}/pdf retorna 200 OK con Content-Type application/pdf y cabecera de descarga")
    void debeDescargarFichaPdfExitosamente() throws Exception {
        Long consultaId = 1L;
        byte[] fakePdfBytes = "%PDF-1.7 Test PDF Content".getBytes(StandardCharsets.US_ASCII);

        when(consultaService.generarFichaPdf(consultaId)).thenReturn(fakePdfBytes);

        mockMvc.perform(get("/api/consultas/{id}/pdf", consultaId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"ficha-expediente-1.pdf\""))
                .andExpect(content().bytes(fakePdfBytes));

        verify(consultaService).generarFichaPdf(consultaId);
    }

    @Test
    @DisplayName("GET /api/consultas/{id}/ficha-pdf (alias) retorna 200 OK con Content-Type application/pdf")
    void debeDescargarFichaPdfPorRutaAlias() throws Exception {
        Long consultaId = 5L;
        byte[] fakePdfBytes = "%PDF-1.7 Alias Test Content".getBytes(StandardCharsets.US_ASCII);

        when(consultaService.generarFichaPdf(consultaId)).thenReturn(fakePdfBytes);

        mockMvc.perform(get("/api/consultas/{id}/ficha-pdf", consultaId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(fakePdfBytes));

        verify(consultaService).generarFichaPdf(consultaId);
    }

    @Test
    @DisplayName("GET /api/consultas/{id}/ficha retorna 200 OK con datos consolidados JSON de PB-35")
    void debeConsultarDatosVigentesFichaJson() throws Exception {
        Long consultaId = 10L;
        FichaExpedienteDTO dto = FichaExpedienteDTO.builder()
                .identificacion(IdentificacionFichaDTO.builder()
                        .consultaId(consultaId)
                        .fechaRadicacion(LocalDate.of(2026, 9, 15))
                        .tramite("Asesoría Civil")
                        .descripcion("Caso de prueba")
                        .build())
                .estadoResultado(EstadoResultadoFichaDTO.builder()
                        .estado("EN_TRAMITE")
                        .resultado("Pendiente")
                        .build())
                .partes(PartesFichaDTO.builder()
                        .solicitantePrincipal(PersonaDetalleDTO.builder()
                                .id(101L)
                                .nombreCompleto("María Rodríguez")
                                .tipoDocumento("CC")
                                .numeroDocumento("1090123456")
                                .build())
                        .build())
                .clasificacion(ClasificacionFichaDTO.builder()
                        .area("Civil")
                        .tema("Sucesiones")
                        .build())
                .responsables(ResponsablesFichaDTO.builder()
                        .estudiante(ResponsableDetalleDTO.builder()
                                .nombre("Juan Estudiante")
                                .codigo("1151234")
                                .build())
                        .build())
                .build();

        when(consultaService.obtenerFichaExpediente(consultaId)).thenReturn(dto);

        mockMvc.perform(get("/api/consultas/{id}/ficha", consultaId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.identificacion.consultaId").value(10))
                .andExpect(jsonPath("$.identificacion.tramite").value("Asesoría Civil"))
                .andExpect(jsonPath("$.estadoResultado.estado").value("EN_TRAMITE"))
                .andExpect(jsonPath("$.partes.solicitantePrincipal.nombreCompleto").value("María Rodríguez"))
                .andExpect(jsonPath("$.responsables.estudiante.codigo").value("1151234"));

        verify(consultaService).obtenerFichaExpediente(consultaId);
    }

    @Test
    @DisplayName("Rechazo de intento de tercero ajeno: retorna HTTP 403 Forbidden")
    void debeRetornar403CuandoTerceroIntentaAcceder() throws Exception {
        Long consultaId = 42L;

        when(consultaService.generarFichaPdf(consultaId))
                .thenThrow(new AccessDeniedException("No tiene permisos para ver esta consulta"));

        mockMvc.perform(get("/api/consultas/{id}/pdf", consultaId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.estado").value(403))
                .andExpect(jsonPath("$.error").value("No autorizado"));
    }

    @Test
    @DisplayName("Consulta inexistente: retorna HTTP 400 Bad Request según regla de negocio")
    void debeRetornar400CuandoConsultaNoExiste() throws Exception {
        Long consultaId = 999L;

        when(consultaService.generarFichaPdf(consultaId))
                .thenThrow(new BusinessException("Consulta no encontrada con id: " + consultaId));

        mockMvc.perform(get("/api/consultas/{id}/pdf", consultaId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400))
                .andExpect(jsonPath("$.error").value("Error de negocio"));
    }
}
