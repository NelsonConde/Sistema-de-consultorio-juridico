package co.edu.ufps.legal_cases.business.controller.seguimiento;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.APROBAR_RESPUESTAS_SEGUIMIENTO;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import co.edu.ufps.legal_cases.business.service.seguimiento.SeguimientoRespuestaService;
import co.edu.ufps.legal_cases.common.dto.PageResponseDTO;

class SeguimientoRespuestaControllerAuthorizationTest {

    private AnnotationConfigApplicationContext context;
    private SeguimientoRespuestaController seguimientoRespuestaController;
    private SeguimientoRespuestaService seguimientoRespuestaService;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext(TestConfiguration.class);
        seguimientoRespuestaController = context.getBean(SeguimientoRespuestaController.class);
        seguimientoRespuestaService = context.getBean(SeguimientoRespuestaService.class);
        when(seguimientoRespuestaService.listarPendientes(
                null, 1, 10, "fechaCreacion", "desc", null, null))
                .thenReturn(new PageResponseDTO<>(List.of(), 1, 10, 0, 0));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        context.close();
    }

    @Test
    void usuarioConAprobarRespuestasSeguimientoPuedeListarPendientes() {
        autenticarCon(APROBAR_RESPUESTAS_SEGUIMIENTO);

        assertDoesNotThrow(() -> seguimientoRespuestaController.listarPendientes(
                null, 1, 10, "fechaCreacion", "desc", null, null));
    }

    @Test
    void usuarioSinPermisoNoPuedeListarPendientes() {
        autenticarCon("OTRO_PERMISO");

        assertThrows(
                AccessDeniedException.class,
                () -> seguimientoRespuestaController.listarPendientes(
                        null, 1, 10, "fechaCreacion", "desc", null, null));
    }

    private void autenticarCon(String authority) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "usuario.prueba",
                        "N/A",
                        List.of(new SimpleGrantedAuthority(authority))));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class TestConfiguration {

        @Bean
        SeguimientoRespuestaService seguimientoRespuestaService() {
            return mock(SeguimientoRespuestaService.class);
        }

        @Bean
        SeguimientoRespuestaController seguimientoRespuestaController(
                SeguimientoRespuestaService seguimientoRespuestaService) {
            return new SeguimientoRespuestaController(seguimientoRespuestaService);
        }
    }
}
