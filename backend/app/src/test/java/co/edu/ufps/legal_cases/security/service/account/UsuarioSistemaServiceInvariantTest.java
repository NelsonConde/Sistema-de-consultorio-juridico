package co.edu.ufps.legal_cases.security.service.account;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import co.edu.ufps.legal_cases.common.exception.AdministracionInvariantException;
import co.edu.ufps.legal_cases.security.repository.account.UsuarioSistemaRepository;
import co.edu.ufps.legal_cases.security.service.account.usuario.UsuarioSistemaMapper;
import co.edu.ufps.legal_cases.security.service.account.usuario.UsuarioSistemaValidator;
import co.edu.ufps.legal_cases.security.service.invariant.administracion.AdministracionInvariantService;

class UsuarioSistemaServiceInvariantTest {

    private UsuarioSistemaRepository usuarioSistemaRepository;
    private UsuarioSistemaValidator usuarioSistemaValidator;
    private AdministracionInvariantService invariantService;

    private UsuarioSistemaService service;

    @BeforeEach
    void setUp() {
        usuarioSistemaRepository =
                mock(UsuarioSistemaRepository.class);

        UsuarioSistemaMapper mapper =
                mock(UsuarioSistemaMapper.class);

        usuarioSistemaValidator =
                mock(UsuarioSistemaValidator.class);

        invariantService =
                mock(AdministracionInvariantService.class);

        service = new UsuarioSistemaService(
                usuarioSistemaRepository,
                mapper,
                usuarioSistemaValidator,
                invariantService);
    }

    @Test
    void ejecutaGuardAntesDeConsultarOMutarUsuario() {
        Long usuarioId = 10L;

        doThrow(
                new AdministracionInvariantException(
                        "operación bloqueada"))
                .when(invariantService)
                .validarCambioEstadoUsuario(
                        usuarioId,
                        false);

        assertThrows(
                AdministracionInvariantException.class,
                () -> service.cambiarEstado(
                        usuarioId,
                        false));

        verify(usuarioSistemaValidator)
                .validarIdObligatorio(usuarioId);

        verify(usuarioSistemaValidator)
                .validarEstadoObligatorio(false);

        verify(invariantService)
                .validarCambioEstadoUsuario(
                        usuarioId,
                        false);

        verifyNoInteractions(
                usuarioSistemaRepository);
    }
}