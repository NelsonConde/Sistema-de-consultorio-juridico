
package co.edu.ufps.legal_cases.business.controller.estadisticas;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_CONSULTAS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_REPORTES;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.test.web.servlet.MockMvc;

import co.edu.ufps.legal_cases.audit.service.log.AuditSecurityService;
import co.edu.ufps.legal_cases.business.controller.estadisticas.EstadisticasController;
import co.edu.ufps.legal_cases.business.service.estadisticas.EstadisticasService;
import co.edu.ufps.legal_cases.common.observability.CorrelationIdContext;
import co.edu.ufps.legal_cases.common.observability.CorrelationIdFilter;
import co.edu.ufps.legal_cases.config.cors.CorsConfig;
import co.edu.ufps.legal_cases.config.cors.CorsProperties;
import co.edu.ufps.legal_cases.config.security.SecurityConfig;
import co.edu.ufps.legal_cases.config.security.SecurityExceptionHandler;
import co.edu.ufps.legal_cases.security.filter.jwt.JwtAuthenticationFilter;
import co.edu.ufps.legal_cases.security.repository.account.UsuarioSistemaRepository;
import co.edu.ufps.legal_cases.security.service.account.perfil.PerfilUsuarioResolverService;
import co.edu.ufps.legal_cases.security.service.jwt.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(
        classes = EstadisticasControllerHttpSecurityTest.TestApplication.class,
        properties = {
                "app.cors.allowed-origins=http://localhost:3000",
                "spring.autoconfigure.exclude="
                        + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                        + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration"
        })
@AutoConfigureMockMvc
class EstadisticasControllerHttpSecurityTest {

    private static final String TEST_PERMISSION_HEADER = "X-QA-Permission";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EstadisticasService estadisticasService;

    @BeforeEach
    void prepararPrueba() {
        reset(estadisticasService);
    }

    @Test
    void semestresSinAutenticacionRetorna401ConCorrelacion() throws Exception {
        var result = mockMvc.perform(get("/api/estadisticas/semestres"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists(CorrelationIdContext.HEADER_NAME))
                .andExpect(jsonPath("$.estado").value(401))
                .andExpect(jsonPath("$.ruta").value("/api/estadisticas/semestres"))
                .andExpect(jsonPath("$.correlacionId").isNotEmpty())
                .andReturn();

        String correlationId = result.getResponse()
                .getHeader(CorrelationIdContext.HEADER_NAME);

        assertTrue(result.getResponse()
                .getContentAsString()
                .contains(correlationId));
    }

    @Test
    void reporteSemestralSinAutenticacionRetorna401() throws Exception {
        mockMvc.perform(get("/api/estadisticas/2026/semestre/2"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.estado").value(401));
    }

    @Test
    void pdfSemestralSinAutenticacionRetorna401() throws Exception {
        mockMvc.perform(get("/api/estadisticas/2026/semestre/2/pdf"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.estado").value(401));
    }

    @Test
    void usuarioSinPermisoDeReportesRecibe403() throws Exception {
        mockMvc.perform(get("/api/estadisticas/2026/semestre/2")
                        .header(TEST_PERMISSION_HEADER, VER_CONSULTAS))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.estado").value(403))
                .andExpect(header().exists(CorrelationIdContext.HEADER_NAME));
    }

    @Test
    void usuarioConPermisoDeConsultasPuedeListarSemestres() throws Exception {
        when(estadisticasService.listarSemestresDisponibles())
                .thenReturn(List.of());

        mockMvc.perform(get("/api/estadisticas/semestres")
                        .header(TEST_PERMISSION_HEADER, VER_CONSULTAS))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void usuarioConPermisoDeReportesPuedeDescargarPdf() throws Exception {
        byte[] pdf = "%PDF-1.7 prueba"
                .getBytes(StandardCharsets.US_ASCII);

        when(estadisticasService.generarPdfSemestre(2026, 2))
                .thenReturn(pdf);

        mockMvc.perform(get("/api/estadisticas/2026/semestre/2/pdf")
                        .header(TEST_PERMISSION_HEADER, VER_REPORTES))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(pdf))
                .andExpect(header().string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"estadisticas-2026-s2.pdf\""));
    }


    @Test
    void reportePorRangoSinAutenticacionRetorna401() throws Exception {
        mockMvc.perform(get("/api/estadisticas/reporte")
                        .param("fechaInicio", "2026-01-01")
                        .param("fechaFin", "2026-06-30"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.estado").value(401))
                .andExpect(header().exists(CorrelationIdContext.HEADER_NAME));
    }

    @Test
    void pdfPorRangoSinAutenticacionRetorna401() throws Exception {
        mockMvc.perform(get("/api/estadisticas/reporte/pdf")
                        .param("fechaInicio", "2026-01-01")
                        .param("fechaFin", "2026-06-30"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.estado").value(401))
                .andExpect(header().exists(CorrelationIdContext.HEADER_NAME));
    }

    @Test
    void pdfPorRangoSinPermisoDeReportesRetorna403() throws Exception {
        mockMvc.perform(get("/api/estadisticas/reporte/pdf")
                        .header(TEST_PERMISSION_HEADER, VER_CONSULTAS)
                        .param("fechaInicio", "2026-01-01")
                        .param("fechaFin", "2026-06-30"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.estado").value(403))
                .andExpect(header().exists(CorrelationIdContext.HEADER_NAME));
    }


    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({
            SecurityConfig.class,
            CorsConfig.class,
            SecurityExceptionHandler.class,
            CorrelationIdFilter.class,
            EstadisticasController.class
    })
    static class TestApplication {

        @Bean
        EstadisticasService estadisticasService() {
            return mock(EstadisticasService.class);
        }

        @Bean
        JsonMapper jsonMapper() {
            return JsonMapper.builder()
                    .findAndAddModules()
                    .build();
        }

        @Bean
        AuditSecurityService auditSecurityService() {
            return mock(AuditSecurityService.class);
        }

        @Bean
        CsrfTokenRepository csrfTokenRepository() {
            return CookieCsrfTokenRepository.withHttpOnlyFalse();
        }

        @Bean
        CorsProperties corsProperties() {
            CorsProperties properties = new CorsProperties();
            properties.setAllowedOrigins(List.of("http://localhost:3000"));
            return properties;
        }

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter() {
            return new TestJwtAuthenticationFilter();
        }
    }

    static class TestJwtAuthenticationFilter extends JwtAuthenticationFilter {

        TestJwtAuthenticationFilter() {
            super(
                    mock(JwtService.class),
                    mock(UsuarioSistemaRepository.class),
                    mock(PerfilUsuarioResolverService.class));
        }

        @Override
        protected void doFilterInternal(
                HttpServletRequest request,
                HttpServletResponse response,
                FilterChain filterChain)
                throws ServletException, IOException {

            // Solo en pruebas: simula una autoridad sin crear JWT reales.
            String permission = request.getHeader(TEST_PERMISSION_HEADER);

            if (permission != null) {
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                "usuario.qa",
                                null,
                                List.of(new SimpleGrantedAuthority(permission))));
            }

            try {
                filterChain.doFilter(request, response);
            } finally {
                SecurityContextHolder.clearContext();
            }
        }
    }
}
