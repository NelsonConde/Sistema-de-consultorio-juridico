package co.edu.ufps.legal_cases.business.controller.estadisticas;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_CONSULTAS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_REPORTES;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

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

import co.edu.ufps.legal_cases.business.service.estadisticas.EstadisticasService;

class EstadisticasControllerAuthorizationTest {

    private AnnotationConfigApplicationContext context;
    private EstadisticasController estadisticasController;
    private EstadisticasService estadisticasService;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext(
                TestConfiguration.class);

        estadisticasController =
                context.getBean(EstadisticasController.class);

        estadisticasService =
                context.getBean(EstadisticasService.class);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        context.close();
    }

    @Test
    void usuarioConVerConsultasPuedeConsultarSuPerfilActual() {
        autenticarCon(VER_CONSULTAS);

        assertDoesNotThrow(
                () -> estadisticasController
                        .obtenerPorPerfilActual(2026, 2));

        verify(estadisticasService)
                .obtenerPorPerfilActual(2026, 2);
    }

    @Test
    void usuarioConVerConsultasNoPuedeConsultarIdentificadoresArbitrarios() {
        autenticarCon(VER_CONSULTAS);

        assertAll(
                () -> assertThrows(
                        AccessDeniedException.class,
                        () -> estadisticasController
                                .obtenerPorEstudiante(
                                        2026,
                                        2,
                                        99L)),

                () -> assertThrows(
                        AccessDeniedException.class,
                        () -> estadisticasController
                                .obtenerPorAsesor(
                                        2026,
                                        2,
                                        99L)),

                () -> assertThrows(
                        AccessDeniedException.class,
                        () -> estadisticasController
                                .obtenerPorMonitor(
                                        2026,
                                        2,
                                        99L)));

        verifyNoInteractions(estadisticasService);
    }

    @Test
    void usuarioConVerReportesPuedeConsultarIdentificadoresArbitrarios() {
        autenticarCon(VER_REPORTES);

        assertAll(
                () -> assertDoesNotThrow(
                        () -> estadisticasController
                                .obtenerPorEstudiante(
                                        2026,
                                        2,
                                        11L)),

                () -> assertDoesNotThrow(
                        () -> estadisticasController
                                .obtenerPorAsesor(
                                        2026,
                                        2,
                                        22L)),

                () -> assertDoesNotThrow(
                        () -> estadisticasController
                                .obtenerPorMonitor(
                                        2026,
                                        2,
                                        33L)));

        verify(estadisticasService)
                .obtenerPorEstudiante(2026, 2, 11L);

        verify(estadisticasService)
                .obtenerPorAsesor(2026, 2, 22L);

        verify(estadisticasService)
                .obtenerPorMonitor(2026, 2, 33L);
    }

    private void autenticarCon(String authority) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "usuario.prueba",
                        "N/A",
                        List.of(
                                new SimpleGrantedAuthority(authority))));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class TestConfiguration {

        @Bean
        EstadisticasService estadisticasService() {
            return mock(EstadisticasService.class);
        }

        @Bean
        EstadisticasController estadisticasController(
                EstadisticasService estadisticasService) {
            return new EstadisticasController(estadisticasService);
        }
    }
}