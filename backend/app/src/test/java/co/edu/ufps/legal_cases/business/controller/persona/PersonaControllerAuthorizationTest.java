package co.edu.ufps.legal_cases.business.controller.persona;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_PERSONAS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_PERSONAS;
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

import co.edu.ufps.legal_cases.common.dto.PageResponseDTO;
import co.edu.ufps.legal_cases.business.service.persona.PersonaService;

class PersonaControllerAuthorizationTest {

    private AnnotationConfigApplicationContext context;
    private PersonaController personaController;
    private PersonaService personaService;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext(TestConfiguration.class);
        personaController = context.getBean(PersonaController.class);
        personaService = context.getBean(PersonaService.class);
        when(personaService.listar(null, 1, 10))
                .thenReturn(new PageResponseDTO<>(List.of(), 1, 10, 0, 0));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        context.close();
    }

    @Test
    void usuarioConVerPersonasPuedeBuscar() {
        autenticarCon(VER_PERSONAS);

        assertDoesNotThrow(() -> personaController.listar(null, 1, 10));
    }

    @Test
    void usuarioConGestionarPersonasPuedeBuscar() {
        autenticarCon(GESTIONAR_PERSONAS);

        assertDoesNotThrow(() -> personaController.listar(null, 1, 10));
    }

    @Test
    void usuarioSinPermisoNoPuedeBuscar() {
        autenticarCon("Acceder personas");

        assertThrows(
                AccessDeniedException.class,
                () -> personaController.listar(null, 1, 10));
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
        PersonaService personaService() {
            return mock(PersonaService.class);
        }

        @Bean
        PersonaController personaController(PersonaService personaService) {
            return new PersonaController(personaService);
        }
    }
}