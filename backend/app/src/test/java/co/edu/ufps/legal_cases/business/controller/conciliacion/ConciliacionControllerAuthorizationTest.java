package co.edu.ufps.legal_cases.business.controller.conciliacion;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_CONCILIACIONES;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_CONCILIACIONES;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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

import co.edu.ufps.legal_cases.business.service.conciliacion.ConciliacionService;
import co.edu.ufps.legal_cases.common.dto.PageResponseDTO;

class ConciliacionControllerAuthorizationTest {

    private AnnotationConfigApplicationContext context;
    private ConciliacionController conciliacionController;
    private ConciliacionService conciliacionService;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext(TestConfiguration.class);
        conciliacionController = context.getBean(ConciliacionController.class);
        conciliacionService = context.getBean(ConciliacionService.class);
        when(conciliacionService.buscarParaUsuarioActual(
                null, 1, 10, "id", "desc", null, null, null))
                .thenReturn(new PageResponseDTO<>(List.of(), 1, 10, 0, 0));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        context.close();
    }

    @Test
    void usuarioConVerConciliacionesPuedeBuscar() {
        autenticarCon(VER_CONCILIACIONES);

        assertDoesNotThrow(() -> conciliacionController.buscar(
                null, 1, 10, "id", "desc", null, null, null));
    }

    @Test
    void usuarioSoloConGestionarConciliacionesNoPuedeBuscar() {
        autenticarCon(GESTIONAR_CONCILIACIONES);

        assertThrows(
                AccessDeniedException.class,
                () -> conciliacionController.buscar(
                        null, 1, 10, "id", "desc", null, null, null));
    }

    @Test
    void usuarioSinPermisoNoPuedeBuscar() {
        autenticarCon("Acceder conciliaciones");

        assertThrows(
                AccessDeniedException.class,
                () -> conciliacionController.buscar(
                        null, 1, 10, "id", "desc", null, null, null));
    }

    @Test
    void usuarioConVerConciliacionesPuedeLlegarAlServiceDeDetalle() {
        autenticarCon(VER_CONCILIACIONES);

        assertDoesNotThrow(() -> conciliacionController.obtenerDetalle(99L));

        verify(conciliacionService).obtenerDetalle(99L);
    }

    @Test
    void usuarioSinPermisoNoPuedeConsultarDetalle() {
        autenticarCon("Acceder conciliaciones");

        assertThrows(
                AccessDeniedException.class,
                () -> conciliacionController.obtenerDetalle(99L));
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
        ConciliacionService conciliacionService() {
            return mock(ConciliacionService.class);
        }

        @Bean
        ConciliacionController conciliacionController(ConciliacionService conciliacionService) {
            return new ConciliacionController(conciliacionService);
        }
    }
}
