package co.edu.ufps.legal_cases.business.controller.proceso;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_PROCESOS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_PROCESOS;
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

import co.edu.ufps.legal_cases.business.service.proceso.ProcesoService;
import co.edu.ufps.legal_cases.common.dto.PageResponseDTO;

class ProcesoControllerAuthorizationTest {

    private AnnotationConfigApplicationContext context;
    private ProcesoController procesoController;
    private ProcesoService procesoService;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext(TestConfiguration.class);
        procesoController = context.getBean(ProcesoController.class);
        procesoService = context.getBean(ProcesoService.class);
        when(procesoService.buscarParaUsuarioActual(
                null, 1, 10, "id", "desc", null, null, null))
                .thenReturn(new PageResponseDTO<>(List.of(), 1, 10, 0, 0));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        context.close();
    }

    @Test
    void usuarioConVerProcesosPuedeBuscar() {
        autenticarCon(VER_PROCESOS);

        assertDoesNotThrow(() -> procesoController.buscar(
                null, 1, 10, "id", "desc", null, null, null));
    }

    @Test
    void usuarioConGestionarProcesosPuedeBuscar() {
        autenticarCon(GESTIONAR_PROCESOS);

        assertDoesNotThrow(() -> procesoController.buscar(
                null, 1, 10, "id", "desc", null, null, null));
    }

    @Test
    void usuarioSinPermisoNoPuedeBuscar() {
        autenticarCon("Acceder procesos");

        assertThrows(
                AccessDeniedException.class,
                () -> procesoController.buscar(
                        null, 1, 10, "id", "desc", null, null, null));
    }

    @Test
    void usuarioConVerProcesosPuedeLlegarAlServiceDeDetalle() {
        autenticarCon(VER_PROCESOS);

        assertDoesNotThrow(() -> procesoController.obtenerPorId(99L));

        verify(procesoService).obtenerPorId(99L);
    }

    @Test
    void usuarioSinPermisoNoPuedeConsultarDetalle() {
        autenticarCon("Acceder procesos");

        assertThrows(
                AccessDeniedException.class,
                () -> procesoController.obtenerPorId(99L));
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
        ProcesoService procesoService() {
            return mock(ProcesoService.class);
        }

        @Bean
        ProcesoController procesoController(ProcesoService procesoService) {
            return new ProcesoController(procesoService);
        }
    }
}
