package co.edu.ufps.legal_cases.security.controller.account;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_USUARIOS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_USUARIOS;
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
import co.edu.ufps.legal_cases.security.service.account.UsuarioCambioPerfilService;
import co.edu.ufps.legal_cases.security.service.account.UsuarioSistemaService;

class UsuarioSistemaControllerAuthorizationTest {

    private AnnotationConfigApplicationContext context;
    private UsuarioSistemaController usuarioSistemaController;
    private UsuarioSistemaService usuarioSistemaService;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext(TestConfiguration.class);
        usuarioSistemaController = context.getBean(UsuarioSistemaController.class);
        usuarioSistemaService = context.getBean(UsuarioSistemaService.class);
        when(usuarioSistemaService.buscar(null, 1, 10, "id", "desc", null, null))
                .thenReturn(new PageResponseDTO<>(List.of(), 1, 10, 0, 0));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        context.close();
    }

    @Test
    void usuarioConVerUsuariosPuedeListar() {
        autenticarCon(VER_USUARIOS);

        assertDoesNotThrow(() -> usuarioSistemaController.listar(
                null, 1, 10, "id", "desc", null, null));
    }

    @Test
    void usuarioConGestionarUsuariosPuedeListar() {
        autenticarCon(GESTIONAR_USUARIOS);

        assertDoesNotThrow(() -> usuarioSistemaController.listar(
                null, 1, 10, "id", "desc", null, null));
    }

    @Test
    void usuarioConAutoridadIrrelevanteNoPuedeListar() {
        autenticarCon("OTRO_PERMISO");

        assertThrows(
                AccessDeniedException.class,
                () -> usuarioSistemaController.listar(
                        null, 1, 10, "id", "desc", null, null));
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
        UsuarioSistemaService usuarioSistemaService() {
            return mock(UsuarioSistemaService.class);
        }

        @Bean
        UsuarioCambioPerfilService usuarioCambioPerfilService() {
            return mock(UsuarioCambioPerfilService.class);
        }

        @Bean
        UsuarioSistemaController usuarioSistemaController(
                UsuarioSistemaService usuarioSistemaService,
                UsuarioCambioPerfilService usuarioCambioPerfilService) {
            return new UsuarioSistemaController(
                    usuarioSistemaService,
                    usuarioCambioPerfilService);
        }
    }
}
