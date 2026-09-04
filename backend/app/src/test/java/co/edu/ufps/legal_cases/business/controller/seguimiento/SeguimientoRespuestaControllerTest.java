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

import co.edu.ufps.legal_cases.business.dto.seguimiento.respuesta.SeguimientoRespuestaResponseDTO;
import co.edu.ufps.legal_cases.business.model.seguimiento.respuesta.EstadoRespuestaSeguimiento;
import co.edu.ufps.legal_cases.business.service.seguimiento.SeguimientoRespuestaService;
import co.edu.ufps.legal_cases.common.dto.PageResponseDTO;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.common.exception.handler.GlobalExceptionHandler;

class SeguimientoRespuestaControllerTest {

    private SeguimientoRespuestaService seguimientoRespuestaService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        seguimientoRespuestaService = mock(SeguimientoRespuestaService.class);
        SeguimientoRespuestaController controller = new SeguimientoRespuestaController(seguimientoRespuestaService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listarPendientesSinParametrosDebePropagarDefaultsYResponderPageResponseDTO() throws Exception {
        SeguimientoRespuestaResponseDTO respuestaPendiente = crearRespuesta();
        PageResponseDTO<SeguimientoRespuestaResponseDTO> respuesta = new PageResponseDTO<>(
                List.of(respuestaPendiente),
                1,
                10,
                1,
                1);
        when(seguimientoRespuestaService.listarPendientes(
                null, 1, 10, "fechaCreacion", "desc", null, null))
                .thenReturn(respuesta);

        mockMvc.perform(get("/api/seguimientos/respuestas/pendientes"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].id").value(5))
                .andExpect(jsonPath("$.content[0].version").value(1))
                .andExpect(jsonPath("$.content[0].seguimientoId").value(6))
                .andExpect(jsonPath("$.content[0].consultaId").value(7))
                .andExpect(jsonPath("$.content[0].estudianteId").value(8))
                .andExpect(jsonPath("$.content[0].estudianteNombre").value("Estudiante A"))
                .andExpect(jsonPath("$.content[0].contenido").value("Respuesta pendiente"))
                .andExpect(jsonPath("$.content[0].estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.content[0].fueraPlazo").value(false))
                .andExpect(jsonPath("$.content[0].observacionRevision").value("Pendiente de revision"))
                .andExpect(jsonPath("$.content[0].revisadoPorId").value(9))
                .andExpect(jsonPath("$.content[0].revisadoPorUsername").value("revisor@example.test"))
                .andExpect(jsonPath("$.content[0].activo").value(true))
                .andExpect(jsonPath("$.content[0].fechaCreacion").value("2026-09-04T10:30:00"))
                .andExpect(jsonPath("$.content[0].fechaActualizacion").value("2026-09-05T11:30:00"))
                .andExpect(jsonPath("$.content[0].fechaDecision").value("2026-09-06T12:30:00"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(seguimientoRespuestaService).listarPendientes(
                null, 1, 10, "fechaCreacion", "desc", null, null);
    }

    @Test
    void listarPendientesConParametrosExplicitosDebePropagarlosAlService() throws Exception {
        when(seguimientoRespuestaService.listarPendientes(
                "respuesta",
                2,
                20,
                "estudianteNombre",
                "asc",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30)))
                .thenReturn(new PageResponseDTO<>(List.of(), 2, 20, 0, 0));

        mockMvc.perform(get("/api/seguimientos/respuestas/pendientes")
                        .param("search", "respuesta")
                        .param("page", "2")
                        .param("size", "20")
                        .param("sortBy", "estudianteNombre")
                        .param("direction", "asc")
                        .param("fechaDesde", "2026-09-01")
                        .param("fechaHasta", "2026-09-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(20));

        verify(seguimientoRespuestaService).listarPendientes(
                "respuesta",
                2,
                20,
                "estudianteNombre",
                "asc",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30));
    }

    @Test
    void businessExceptionEnListadoPendientesDebeResponder400() throws Exception {
        when(seguimientoRespuestaService.listarPendientes(
                null, 1, 51, "fechaCreacion", "desc", null, null))
                .thenThrow(new BusinessException("El tamano de pagina debe estar entre 1 y 50"));

        mockMvc.perform(get("/api/seguimientos/respuestas/pendientes")
                        .param("size", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400))
                .andExpect(jsonPath("$.error").value("Error de negocio"))
                .andExpect(jsonPath("$.mensaje").value("El tamano de pagina debe estar entre 1 y 50"));
    }

    @Test
    void accessDeniedEnListadoPendientesDebeResponder403() throws Exception {
        when(seguimientoRespuestaService.listarPendientes(
                null, 1, 10, "fechaCreacion", "desc", null, null))
                .thenThrow(new AccessDeniedException("Acceso denegado"));

        mockMvc.perform(get("/api/seguimientos/respuestas/pendientes"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.estado").value(403))
                .andExpect(jsonPath("$.error").value("No autorizado"))
                .andExpect(jsonPath("$.mensaje")
                        .value("No tiene permisos para acceder a este recurso"))
                .andExpect(jsonPath("$.ruta").value("/api/seguimientos/respuestas/pendientes"));
    }

    private SeguimientoRespuestaResponseDTO crearRespuesta() {
        SeguimientoRespuestaResponseDTO dto = new SeguimientoRespuestaResponseDTO();
        dto.setId(5L);
        dto.setVersion(1L);
        dto.setSeguimientoId(6L);
        dto.setConsultaId(7L);
        dto.setEstudianteId(8L);
        dto.setEstudianteNombre("Estudiante A");
        dto.setContenido("Respuesta pendiente");
        dto.setEstado(EstadoRespuestaSeguimiento.PENDIENTE);
        dto.setFueraPlazo(false);
        dto.setObservacionRevision("Pendiente de revision");
        dto.setRevisadoPorId(9L);
        dto.setRevisadoPorUsername("revisor@example.test");
        dto.setActivo(true);
        dto.setFechaCreacion(LocalDateTime.of(2026, 9, 4, 10, 30));
        dto.setFechaActualizacion(LocalDateTime.of(2026, 9, 5, 11, 30));
        dto.setFechaDecision(LocalDateTime.of(2026, 9, 6, 12, 30));
        return dto;
    }
}
