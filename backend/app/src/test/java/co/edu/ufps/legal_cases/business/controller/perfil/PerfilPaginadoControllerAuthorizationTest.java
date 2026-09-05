package co.edu.ufps.legal_cases.business.controller.perfil;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_ADMINISTRADORES;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_ASESORES_MONITORES;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_CONCILIADORES;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_USUARIOS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_ADMINISTRADORES;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_ASESORES_MONITORES;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_CONCILIADORES;
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

import co.edu.ufps.legal_cases.business.service.perfil.AdministrativoService;
import co.edu.ufps.legal_cases.business.service.perfil.AsesorService;
import co.edu.ufps.legal_cases.business.service.perfil.ConciliadorService;
import co.edu.ufps.legal_cases.business.service.perfil.MonitorService;
import co.edu.ufps.legal_cases.common.dto.PageResponseDTO;

class PerfilPaginadoControllerAuthorizationTest {

    private AnnotationConfigApplicationContext context;
    private AdministrativoController administrativoController;
    private AsesorController asesorController;
    private MonitorController monitorController;
    private ConciliadorController conciliadorController;
    private AdministrativoService administrativoService;
    private AsesorService asesorService;
    private MonitorService monitorService;
    private ConciliadorService conciliadorService;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext(TestConfiguration.class);
        administrativoController = context.getBean(AdministrativoController.class);
        asesorController = context.getBean(AsesorController.class);
        monitorController = context.getBean(MonitorController.class);
        conciliadorController = context.getBean(ConciliadorController.class);
        administrativoService = context.getBean(AdministrativoService.class);
        asesorService = context.getBean(AsesorService.class);
        monitorService = context.getBean(MonitorService.class);
        conciliadorService = context.getBean(ConciliadorService.class);

        when(administrativoService.buscar(null, 1, 10, "id", "desc", null))
                .thenReturn(new PageResponseDTO<>(List.of(), 1, 10, 0, 0));
        when(asesorService.buscar(null, 1, 10, "id", "desc", null))
                .thenReturn(new PageResponseDTO<>(List.of(), 1, 10, 0, 0));
        when(monitorService.buscar(null, 1, 10, "id", "desc", null))
                .thenReturn(new PageResponseDTO<>(List.of(), 1, 10, 0, 0));
        when(conciliadorService.buscar(null, 1, 10, "id", "desc", null, null))
                .thenReturn(new PageResponseDTO<>(List.of(), 1, 10, 0, 0));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        context.close();
    }

    @Test
    void administrativoPermiteAuthoritiesReales() {
        autenticarCon(VER_ADMINISTRADORES);
        assertDoesNotThrow(() -> administrativoController.listar(null, 1, 10, "id", "desc", null));

        autenticarCon(GESTIONAR_ADMINISTRADORES);
        assertDoesNotThrow(() -> administrativoController.listar(null, 1, 10, "id", "desc", null));

        autenticarCon(GESTIONAR_USUARIOS);
        assertDoesNotThrow(() -> administrativoController.listar(null, 1, 10, "id", "desc", null));
    }

    @Test
    void administrativoRechazaAutoridadIrrelevante() {
        autenticarCon("OTRO_PERMISO");

        assertThrows(
                AccessDeniedException.class,
                () -> administrativoController.listar(null, 1, 10, "id", "desc", null));
    }

    @Test
    void asesorPermiteAuthoritiesReales() {
        autenticarCon(VER_ASESORES_MONITORES);
        assertDoesNotThrow(() -> asesorController.listar(null, 1, 10, "id", "desc", null));

        autenticarCon(GESTIONAR_ASESORES_MONITORES);
        assertDoesNotThrow(() -> asesorController.listar(null, 1, 10, "id", "desc", null));

        autenticarCon(GESTIONAR_USUARIOS);
        assertDoesNotThrow(() -> asesorController.listar(null, 1, 10, "id", "desc", null));
    }

    @Test
    void asesorRechazaAutoridadIrrelevante() {
        autenticarCon("OTRO_PERMISO");

        assertThrows(
                AccessDeniedException.class,
                () -> asesorController.listar(null, 1, 10, "id", "desc", null));
    }

    @Test
    void monitorPermiteAuthoritiesReales() {
        autenticarCon(VER_ASESORES_MONITORES);
        assertDoesNotThrow(() -> monitorController.listar(null, 1, 10, "id", "desc", null));

        autenticarCon(GESTIONAR_ASESORES_MONITORES);
        assertDoesNotThrow(() -> monitorController.listar(null, 1, 10, "id", "desc", null));

        autenticarCon(GESTIONAR_USUARIOS);
        assertDoesNotThrow(() -> monitorController.listar(null, 1, 10, "id", "desc", null));
    }

    @Test
    void monitorRechazaAutoridadIrrelevante() {
        autenticarCon("OTRO_PERMISO");

        assertThrows(
                AccessDeniedException.class,
                () -> monitorController.listar(null, 1, 10, "id", "desc", null));
    }

    @Test
    void conciliadorPermiteAuthoritiesReales() {
        autenticarCon(VER_CONCILIADORES);
        assertDoesNotThrow(() -> conciliadorController.listar(null, 1, 10, "id", "desc", null, null));

        autenticarCon(GESTIONAR_CONCILIADORES);
        assertDoesNotThrow(() -> conciliadorController.listar(null, 1, 10, "id", "desc", null, null));

        autenticarCon(GESTIONAR_USUARIOS);
        assertDoesNotThrow(() -> conciliadorController.listar(null, 1, 10, "id", "desc", null, null));
    }

    @Test
    void conciliadorRechazaAutoridadIrrelevante() {
        autenticarCon("OTRO_PERMISO");

        assertThrows(
                AccessDeniedException.class,
                () -> conciliadorController.listar(null, 1, 10, "id", "desc", null, null));
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
        AdministrativoService administrativoService() {
            return mock(AdministrativoService.class);
        }

        @Bean
        AsesorService asesorService() {
            return mock(AsesorService.class);
        }

        @Bean
        MonitorService monitorService() {
            return mock(MonitorService.class);
        }

        @Bean
        ConciliadorService conciliadorService() {
            return mock(ConciliadorService.class);
        }

        @Bean
        AdministrativoController administrativoController(AdministrativoService administrativoService) {
            return new AdministrativoController(administrativoService);
        }

        @Bean
        AsesorController asesorController(AsesorService asesorService) {
            return new AsesorController(asesorService);
        }

        @Bean
        MonitorController monitorController(MonitorService monitorService) {
            return new MonitorController(monitorService);
        }

        @Bean
        ConciliadorController conciliadorController(ConciliadorService conciliadorService) {
            return new ConciliadorController(conciliadorService);
        }
    }
}
