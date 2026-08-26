package co.edu.ufps.legal_cases.business.service.perfil.administrativo;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import co.edu.ufps.legal_cases.business.repository.catalogo.SedeRepository;
import co.edu.ufps.legal_cases.business.repository.catalogo.TipoDocumentoRepository;
import co.edu.ufps.legal_cases.business.repository.perfil.AdministrativoRepository;
import co.edu.ufps.legal_cases.business.service.acceso.perfil.AdministrativoAccessService;
import co.edu.ufps.legal_cases.common.exception.AdministracionInvariantException;
import co.edu.ufps.legal_cases.security.service.account.usuario.UsuarioSistemaPerfilEstadoService;
import co.edu.ufps.legal_cases.security.service.account.usuario.UsuarioSistemaRegistroService;
import co.edu.ufps.legal_cases.security.service.invariant.administracion.AdministracionInvariantService;

class AdministrativoCommandServiceInvariantTest {

    private AdministrativoRepository administrativoRepository;
    private AdministrativoAccessService accessService;
    private AdministracionInvariantService invariantService;

    private AdministrativoCommandService service;

    @BeforeEach
    void setUp() {
        administrativoRepository =
                mock(AdministrativoRepository.class);

        TipoDocumentoRepository tipoDocumentoRepository =
                mock(TipoDocumentoRepository.class);

        SedeRepository sedeRepository =
                mock(SedeRepository.class);

        UsuarioSistemaRegistroService registroService =
                mock(UsuarioSistemaRegistroService.class);

        UsuarioSistemaPerfilEstadoService perfilEstadoService =
                mock(UsuarioSistemaPerfilEstadoService.class);

        accessService =
                mock(AdministrativoAccessService.class);

        AdministrativoValidator validator =
                mock(AdministrativoValidator.class);

        AdministrativoMapper mapper =
                mock(AdministrativoMapper.class);

        invariantService =
                mock(AdministracionInvariantService.class);

        service = new AdministrativoCommandService(
                administrativoRepository,
                tipoDocumentoRepository,
                sedeRepository,
                registroService,
                perfilEstadoService,
                accessService,
                validator,
                mapper,
                invariantService);
    }

    @Test
    void ejecutaGuardAntesDeConsultarAdministrativoAlCambiarEstado() {
        Long id = 10L;

        doThrow(
                new AdministracionInvariantException(
                        "operación bloqueada"))
                .when(invariantService)
                .validarCambioEstadoAdministrativo(
                        id,
                        false);

        assertThrows(
                AdministracionInvariantException.class,
                () -> service.cambiarEstado(
                        id,
                        false));

        verify(accessService)
                .validarPuedeGestionarAdministradores();

        verify(invariantService)
                .validarCambioEstadoAdministrativo(
                        id,
                        false);

        verifyNoInteractions(
                administrativoRepository);
    }

    @Test
    void ejecutaGuardAntesDeConsultarAdministrativoAlQuitarDirectora() {
        Long id = 10L;

        doThrow(
                new AdministracionInvariantException(
                        "operación bloqueada"))
                .when(invariantService)
                .validarCambioDirectora(
                        id,
                        false);

        assertThrows(
                AdministracionInvariantException.class,
                () -> service.cambiarDirectora(
                        id,
                        false));

        verify(invariantService)
                .validarCambioDirectora(
                        id,
                        false);

        verifyNoInteractions(
                administrativoRepository);
    }

    @Test
    void ejecutaGuardAntesDeConsultarAdministrativoAlEliminar() {
        Long id = 10L;

        doThrow(
                new AdministracionInvariantException(
                        "operación bloqueada"))
                .when(invariantService)
                .validarEliminacionAdministrativo(id);

        assertThrows(
                AdministracionInvariantException.class,
                () -> service.eliminar(id));

        verify(invariantService)
                .validarEliminacionAdministrativo(id);

        verifyNoInteractions(
                administrativoRepository);
    }
}