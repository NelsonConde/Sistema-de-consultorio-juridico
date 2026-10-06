package co.edu.ufps.legal_cases.business.controller.consulta;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.ARCHIVAR_CONSULTAS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_CONSULTAS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_CONSULTAS;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import co.edu.ufps.legal_cases.business.dto.consulta.ficha.FichaExpedienteDTO;
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
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        context.close();
    }

    @Test
    @DisplayName("Usuario con permiso VER_CONSULTAS puede invocar descarga de PDF y ficha JSON")
    void usuarioConVerConsultasPuedeAccederAFichaYPdf() {
        autenticarCon(VER_CONSULTAS);

        when(consultaService.generarFichaPdf(1L)).thenReturn(new byte[]{1, 2, 3});
        when(consultaService.obtenerFichaExpediente(1L)).thenReturn(new FichaExpedienteDTO());

        assertAll(
                () -> assertDoesNotThrow(() -> consultaController.descargarFichaPdf(1L)),
                () -> assertDoesNotThrow(() -> consultaController.obtenerFichaExpediente(1L))
        );

        verify(consultaService).generarFichaPdf(1L);
        verify(consultaService).obtenerFichaExpediente(1L);
    }

    @Test
    @DisplayName("Usuario con permiso GESTIONAR_CONSULTAS puede invocar descarga de PDF y ficha JSON")
    void usuarioConGestionarConsultasPuedeAccederAFichaYPdf() {
        autenticarCon(GESTIONAR_CONSULTAS);

        when(consultaService.generarFichaPdf(2L)).thenReturn(new byte[]{4, 5, 6});
        when(consultaService.obtenerFichaExpediente(2L)).thenReturn(new FichaExpedienteDTO());

        assertAll(
                () -> assertDoesNotThrow(() -> consultaController.descargarFichaPdf(2L)),
                () -> assertDoesNotThrow(() -> consultaController.obtenerFichaExpediente(2L))
        );

        verify(consultaService).generarFichaPdf(2L);
        verify(consultaService).obtenerFichaExpediente(2L);
    }

    @Test
    @DisplayName("Usuario sin los permisos requeridos es rechazado con AccessDeniedException")
    void usuarioSinPermisosEsRechazado() {
        autenticarCon("VER_REPORTES");

        assertAll(
                () -> assertThrows(AccessDeniedException.class, () -> consultaController.descargarFichaPdf(1L)),
                () -> assertThrows(AccessDeniedException.class, () -> consultaController.obtenerFichaExpediente(1L))
        );

        verifyNoInteractions(consultaService);
    }

    @Test
    @DisplayName("Petición sin autenticación es rechazada por seguridad")
    void peticionSinAutenticacionEsRechazada() {
        SecurityContextHolder.clearContext();

        assertAll(
                () -> assertThrows(AuthenticationCredentialsNotFoundException.class, () -> consultaController.descargarFichaPdf(1L)),
                () -> assertThrows(AuthenticationCredentialsNotFoundException.class, () -> consultaController.obtenerFichaExpediente(1L))
        );

        verifyNoInteractions(consultaService);
    }

    @Test
    void soloUsuarioConPermisoArchivarPuedeListarConsultasArchivadas() {
        autenticarCon(ARCHIVAR_CONSULTAS);
        when(consultaService.listarArchivadas(null, 1, 10, "fecha", "desc"))
                .thenReturn(new PageResponseDTO<>(List.of(), 1, 10, 0, 0));

        assertDoesNotThrow(() -> consultaController.listarArchivadas(null, 1, 10, "fecha", "desc"));

        verify(consultaService).listarArchivadas(null, 1, 10, "fecha", "desc");
    }

    @Test
    void usuarioSinPermisoArchivarNoPuedeListarConsultasArchivadas() {
        autenticarCon(VER_CONSULTAS);

        assertThrows(
                AccessDeniedException.class,
                () -> consultaController.listarArchivadas(null, 1, 10, "fecha", "desc"));

        verifyNoInteractions(consultaService);
    }

    private void autenticarCon(String authority) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "usuario.autorizado",
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
