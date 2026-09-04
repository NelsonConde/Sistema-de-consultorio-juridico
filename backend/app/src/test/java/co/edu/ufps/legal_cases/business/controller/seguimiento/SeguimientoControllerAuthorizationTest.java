package co.edu.ufps.legal_cases.business.controller.seguimiento;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_SEGUIMIENTOS;
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

import co.edu.ufps.legal_cases.business.service.seguimiento.SeguimientoService;
import co.edu.ufps.legal_cases.common.dto.PageResponseDTO;

class SeguimientoControllerAuthorizationTest {

    private AnnotationConfigApplicationContext context;
    private SeguimientoController seguimientoController;
    private SeguimientoService seguimientoService;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext(TestConfiguration.class);
        seguimientoController = context.getBean(SeguimientoController.class);
        seguimientoService = context.getBean(SeguimientoService.class);
        when(seguimientoService.buscarParaUsuarioActual(
                null, 1, 10, "id", "desc", null, null, null, null, null))
                .thenReturn(new PageResponseDTO<>(List.of(), 1, 10, 0, 0));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        context.close();
    }

    @Test
    void usuarioConVerSeguimientosPuedeBuscar() {
        autenticarCon(VER_SEGUIMIENTOS);

        assertDoesNotThrow(() -> seguimientoController.buscar(
                null, 1, 10, "id", "desc", null, null, null, null, null));
    }

    @Test
    void usuarioSinPermisoNoPuedeBuscar() {
        autenticarCon("Acceder seguimientos");

        assertThrows(
                AccessDeniedException.class,
                () -> seguimientoController.buscar(
                        null, 1, 10, "id", "desc", null, null, null, null, null));
    }

    @Test
    void usuarioConVerSeguimientosPuedeListarCalendarioPorRango() {
        autenticarCon(VER_SEGUIMIENTOS);

        assertDoesNotThrow(() -> seguimientoController.listarCalendarioPorRango(
                java.time.LocalDate.now(), java.time.LocalDate.now().plusDays(1)));
    }

    @Test
    void usuarioSinPermisoNoPuedeListarCalendarioPorRango() {
        autenticarCon("OTRO_PERMISO");

        assertThrows(
                AccessDeniedException.class,
                () -> seguimientoController.listarCalendarioPorRango(
                        java.time.LocalDate.now(), java.time.LocalDate.now().plusDays(1)));
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
        SeguimientoService seguimientoService() {
            return mock(SeguimientoService.class);
        }

        @Bean
        SeguimientoController seguimientoController(SeguimientoService seguimientoService) {
            return new SeguimientoController(seguimientoService);
        }
    }
}
