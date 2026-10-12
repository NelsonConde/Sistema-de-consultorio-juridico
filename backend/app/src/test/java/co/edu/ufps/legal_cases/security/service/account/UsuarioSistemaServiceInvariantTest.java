package co.edu.ufps.legal_cases.security.service.account;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import co.edu.ufps.legal_cases.common.exception.AdministracionInvariantException;
import co.edu.ufps.legal_cases.security.dto.account.UsuarioSistemaDTO;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.repository.account.UsuarioSistemaRepository;
import co.edu.ufps.legal_cases.security.service.account.usuario.UsuarioSistemaMapper;
import co.edu.ufps.legal_cases.security.service.account.usuario.UsuarioSistemaValidator;
import co.edu.ufps.legal_cases.security.service.invariant.administracion.AdministracionInvariantService;

class UsuarioSistemaServiceInvariantTest {

    private UsuarioSistemaRepository usuarioSistemaRepository;
    private UsuarioSistemaMapper mapper;
    private UsuarioSistemaValidator usuarioSistemaValidator;
    private AdministracionInvariantService invariantService;

    private UsuarioSistemaService service;

    @BeforeEach
    void setUp() {
        usuarioSistemaRepository =
                mock(UsuarioSistemaRepository.class);

        mapper = mock(UsuarioSistemaMapper.class);

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

    @Test
    void bloqueaLaCuentaDespuesDeValidarContinuidad() {
        Long usuarioId = 10L;
        UsuarioSistema usuario = new UsuarioSistema();
        usuario.setId(usuarioId);

        UsuarioSistemaDTO esperado = new UsuarioSistemaDTO();

        when(usuarioSistemaRepository.findByIdForUpdate(usuarioId))
                .thenReturn(Optional.of(usuario));
        when(usuarioSistemaRepository.save(usuario))
                .thenReturn(usuario);
        when(mapper.convertirADTO(usuario))
                .thenReturn(esperado);

        service.cambiarEstado(usuarioId, false);

        InOrder orden = inOrder(
                invariantService,
                usuarioSistemaRepository);

        orden.verify(invariantService)
                .validarCambioEstadoUsuario(usuarioId, false);
        orden.verify(usuarioSistemaRepository)
                .findByIdForUpdate(usuarioId);
        orden.verify(usuarioSistemaRepository)
                .save(usuario);
    }
}
