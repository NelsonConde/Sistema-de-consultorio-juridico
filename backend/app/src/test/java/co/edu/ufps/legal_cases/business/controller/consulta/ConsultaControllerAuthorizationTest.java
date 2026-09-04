package co.edu.ufps.legal_cases.business.controller.consulta;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_CONSULTAS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_CONSULTAS;
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

import co.edu.ufps.legal_cases.business.service.consulta.ConsultaService;
import co.edu.ufps.legal_cases.common.dto.PageResponseDTO;

class ConsultaControllerAuthorizationTest {

    private AnnotationConfigApplicationContext context;
    private ConsultaController consultaController;
    private ConsultaService consultaService;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext(TestConfiguration.class);
        consultaController = context.getBean(ConsultaController.class);
        consultaService = context.getBean(ConsultaService.class);
        when(consultaService.buscarParaUsuarioActual(
                null, 1, 10, "fecha", "desc", null, null, null, null, null))
                .thenReturn(new PageResponseDTO<>(List.of(), 1, 10, 0, 0));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        context.close();
    }

    @Test
    void usuarioConVerConsultasPuedeBuscar() {
        autenticarCon(VER_CONSULTAS);

        assertDoesNotThrow(() -> consultaController.buscar(
                null, 1, 10, "fecha", "desc", null, null, null, null, null));
    }

    @Test
    void usuarioConGestionarConsultasPuedeBuscar() {
        autenticarCon(GESTIONAR_CONSULTAS);

        assertDoesNotThrow(() -> consultaController.buscar(
                null, 1, 10, "fecha", "desc", null, null, null, null, null));
    }

    @Test
    void usuarioSinPermisoNoPuedeBuscar() {
        autenticarCon("Acceder consultas");

        assertThrows(
                AccessDeniedException.class,
                () -> consultaController.buscar(
                        null, 1, 10, "fecha", "desc", null, null, null, null, null));
    }

    @Test
    void usuarioConVerConsultasPuedeLlegarAlServiceDeDetalle() {
        autenticarCon(VER_CONSULTAS);

        assertDoesNotThrow(() -> consultaController.obtenerPorId(99L));

        verify(consultaService).obtenerPorId(99L);
    }

    @Test
    void usuarioSinPermisoNoPuedeConsultarDetalle() {
        autenticarCon("Acceder consultas");

        assertThrows(
                AccessDeniedException.class,
                () -> consultaController.obtenerPorId(99L));
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
        ConsultaService consultaService() {
            return mock(ConsultaService.class);
        }

        @Bean
        ConsultaController consultaController(ConsultaService consultaService) {
            return new ConsultaController(consultaService);
        }
    }
}
