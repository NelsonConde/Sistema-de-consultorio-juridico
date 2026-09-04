package co.edu.ufps.legal_cases.business.controller.perfil;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_USUARIOS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_ESTUDIANTES;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_PERFILES_AUXILIARES;
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

import co.edu.ufps.legal_cases.business.service.perfil.EstudianteService;
import co.edu.ufps.legal_cases.business.service.perfil.estudiante.EstudianteExcelService;
import co.edu.ufps.legal_cases.common.dto.PageResponseDTO;

class EstudiantePaginadoControllerAuthorizationTest {

    private AnnotationConfigApplicationContext context;
    private EstudianteController estudianteController;
    private EstudianteService estudianteService;
    private EstudianteExcelService estudianteExcelService;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext(TestConfiguration.class);
        estudianteController = context.getBean(EstudianteController.class);
        estudianteService = context.getBean(EstudianteService.class);
        estudianteExcelService = context.getBean(EstudianteExcelService.class);

        when(estudianteService.buscar(null, 1, 10, "id", "desc", null))
                .thenReturn(new PageResponseDTO<>(List.of(), 1, 10, 0, 0));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        context.close();
    }

    @Test
    void estudiantePermiteAuthoritiesReales() {
        autenticarCon(VER_ESTUDIANTES);
        assertDoesNotThrow(() -> estudianteController.buscar(null, 1, 10, "id", "desc", null));

        autenticarCon(VER_PERFILES_AUXILIARES);
        assertDoesNotThrow(() -> estudianteController.buscar(null, 1, 10, "id", "desc", null));

        autenticarCon(GESTIONAR_USUARIOS);
        assertDoesNotThrow(() -> estudianteController.buscar(null, 1, 10, "id", "desc", null));
    }

    @Test
    void estudianteRechazaAutoridadIrrelevante() {
        autenticarCon("OTRO_PERMISO");

        assertThrows(
                AccessDeniedException.class,
                () -> estudianteController.buscar(null, 1, 10, "id", "desc", null));
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
        EstudianteService estudianteService() {
            return mock(EstudianteService.class);
        }

        @Bean
        EstudianteExcelService estudianteExcelService() {
            return mock(EstudianteExcelService.class);
        }

        @Bean
        EstudianteController estudianteController(
                EstudianteService estudianteService,
                EstudianteExcelService estudianteExcelService) {
            return new EstudianteController(estudianteService, estudianteExcelService);
        }
    }
}
