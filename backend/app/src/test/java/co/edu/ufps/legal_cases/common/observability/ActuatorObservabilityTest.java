package co.edu.ufps.legal_cases.common.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.ufps.legal_cases.config.cors.CorsConfig;
import co.edu.ufps.legal_cases.config.cors.CorsProperties;
import co.edu.ufps.legal_cases.config.security.SecurityConfig;
import co.edu.ufps.legal_cases.config.security.SecurityExceptionHandler;
import co.edu.ufps.legal_cases.security.filter.jwt.JwtAuthenticationFilter;
import co.edu.ufps.legal_cases.security.repository.account.UsuarioSistemaRepository;
import co.edu.ufps.legal_cases.security.service.account.perfil.PerfilUsuarioResolverService;
import co.edu.ufps.legal_cases.security.service.jwt.JwtService;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(
        classes = ActuatorObservabilityTest.TestApplication.class,
        properties = {
                "management.endpoints.web.exposure.include=health,metrics",
                "management.endpoint.health.show-details=never",
                "management.metrics.distribution.percentiles.http.server.requests=0.5,0.95",
                "app.cors.allowed-origins=http://localhost:3000",
                "spring.autoconfigure.exclude="
                        + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                        + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration"
        })
@AutoConfigureMockMvc
class ActuatorObservabilityTest {

    private static final Path APPLICATION_PROPERTIES =
            Path.of("src/main/resources/application.properties");

    private final MockMvc mockMvc;
    private final MeterRegistry meterRegistry;
    private final ApplicationContext applicationContext;
    private final FilterChainProxy filterChainProxy;

    @Autowired
    ActuatorObservabilityTest(
            MockMvc mockMvc,
            MeterRegistry meterRegistry,
            ApplicationContext applicationContext,
            FilterChainProxy filterChainProxy) {

        this.mockMvc = mockMvc;
        this.meterRegistry = meterRegistry;
        this.applicationContext = applicationContext;
        this.filterChainProxy = filterChainProxy;
    }

    @Test
    void healthShouldBePublicAndHideDetails() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(header().exists(CorrelationIdContext.HEADER_NAME))
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    void metricsShouldRequireAuthentication() throws Exception {
        MvcResult result = mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists(CorrelationIdContext.HEADER_NAME))
                .andReturn();

        String correlationId =
                result.getResponse().getHeader(CorrelationIdContext.HEADER_NAME);

        assertEquals(correlationId, UUID.fromString(correlationId).toString());
        assertTrue(result.getResponse().getContentAsString()
                .contains("\"correlacionId\":\"" + correlationId + "\""));
    }

    @Test
    void authenticatedUserShouldAccessMetrics() throws Exception {
        String incomingCorrelationId =
                UUID.randomUUID().toString();

        mockMvc.perform(get("/actuator/metrics")
                        .header(CorrelationIdContext.HEADER_NAME, incomingCorrelationId)
                        .header(TestJwtAuthenticationFilter.TEST_USER_HEADER, "auditor"))
                .andExpect(status().isOk())
                .andExpect(header().string(CorrelationIdContext.HEADER_NAME, incomingCorrelationId));
    }

    @Test
    void httpServerRequestsMetricShouldExistAfterRequest() throws Exception {
        mockMvc.perform(get("/api/test-observability")
                        .header(TestJwtAuthenticationFilter.TEST_USER_HEADER, "auditor"))
                .andExpect(status().isOk());

        assertNotNull(meterRegistry.find("http.server.requests").timer());
    }

    @Test
    void applicationPropertiesShouldConfigureHttpRequestPercentiles() throws IOException {
        String properties = Files.readString(APPLICATION_PROPERTIES);

        assertTrue(properties.contains("management.endpoints.web.exposure.include=health,metrics"));
        assertTrue(properties.contains("management.endpoint.health.show-details=never"));
        assertTrue(properties.contains(
                "management.metrics.distribution.percentiles.http.server.requests=0.5,0.95"));
    }

    @Test
    void correlationIdFilterShouldNotBeRegisteredAsIndependentServletFilter() {
        Map<String, FilterRegistrationBean> registrations =
                applicationContext.getBeansOfType(FilterRegistrationBean.class);

        FilterRegistrationBean<?> registration =
                registrations.get("correlationIdFilterRegistration");

        assertNotNull(registration);
        assertFalse(registration.isEnabled());
        assertTrue(registration.getFilter() instanceof CorrelationIdFilter);
    }

    @Test
    void correlationIdFilterShouldStayBeforeSecurityContextHolderFilter() {
        List<jakarta.servlet.Filter> filters =
                filterChainProxy.getFilters("/actuator/health");

        int correlationIndex = indexOf(filters, CorrelationIdFilter.class);
        int securityContextHolderIndex = indexOf(filters, SecurityContextHolderFilter.class);

        assertTrue(correlationIndex >= 0);
        assertTrue(securityContextHolderIndex >= 0);
        assertTrue(correlationIndex < securityContextHolderIndex);
    }

    private int indexOf(
            List<jakarta.servlet.Filter> filters,
            Class<?> filterType) {

        for (int index = 0; index < filters.size(); index++) {
            if (filterType.isInstance(filters.get(index))) {
                return index;
            }
        }

        return -1;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({
            SecurityConfig.class,
            CorsConfig.class,
            SecurityExceptionHandler.class,
            CorrelationIdFilter.class,
            TestController.class
    })
    static class TestApplication {

        @Bean
        JsonMapper jsonMapper() {
            return JsonMapper.builder().findAndAddModules().build();
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

    @RestController
    static class TestController {

        @GetMapping("/api/test-observability")
        String ok() {
            return "ok";
        }
    }

    static class TestJwtAuthenticationFilter extends JwtAuthenticationFilter {

        static final String TEST_USER_HEADER = "X-Test-User";

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
                FilterChain filterChain) throws ServletException, IOException {

            String username = request.getHeader(TEST_USER_HEADER);

            if (username != null) {
                SecurityContextHolder.getContext()
                        .setAuthentication(new UsernamePasswordAuthenticationToken(
                                username,
                                null,
                                List.of(new SimpleGrantedAuthority("TEST"))));
            }

            try {
                filterChain.doFilter(request, response);
            } finally {
                SecurityContextHolder.clearContext();
            }
        }
    }
}
