package co.edu.ufps.legal_cases.security.service.auth.activation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.security.model.account.EstadoActivacionCuenta;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.repository.account.UsuarioSistemaRepository;

class AccountActivationInitialInvitationServiceTest {

    private UsuarioSistemaRepository usuarioRepository;
    private AccountActivationTokenService tokenService;
    private AccountActivationInitialInvitationService service;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioSistemaRepository.class);
        tokenService = mock(AccountActivationTokenService.class);
        service = new AccountActivationInitialInvitationService(
                usuarioRepository,
                tokenService);
    }

    @Test
    void creaTokenSoloParaCuentaPendienteSinInvitacionActiva() {
        UsuarioSistema usuario = pendingUser();
        when(usuarioRepository.findByIdForUpdate(11L))
                .thenReturn(Optional.of(usuario));
        when(tokenService.existeTokenActivo(usuario))
                .thenReturn(false);
        when(tokenService.crearTokenParaUsuario(usuario))
                .thenReturn(new AccountActivationTokenService.TokenActivacion(
                        "raw-token",
                        LocalDateTime.now().plusHours(48)));

        Optional<String> token = service.crearTokenInicial(11L);

        assertEquals(Optional.of("raw-token"), token);
        verify(tokenService).crearTokenParaUsuario(usuario);
    }

    @Test
    void eventoRepetidoNoDuplicaTokenActivo() {
        UsuarioSistema usuario = pendingUser();
        when(usuarioRepository.findByIdForUpdate(11L))
                .thenReturn(Optional.of(usuario));
        when(tokenService.existeTokenActivo(usuario))
                .thenReturn(true);

        assertTrue(service.crearTokenInicial(11L).isEmpty());

        verify(tokenService, never()).crearTokenParaUsuario(usuario);
    }

    @Test
    void persistenciaPosteriorEstaDeclaradaEnTransaccionIndependiente()
            throws NoSuchMethodException {
        Method method = AccountActivationInitialInvitationService.class
                .getMethod("crearTokenInicial", Long.class);
        Transactional transactional = method.getAnnotation(Transactional.class);

        assertEquals(Propagation.REQUIRES_NEW, transactional.propagation());
    }

    private UsuarioSistema pendingUser() {
        UsuarioSistema usuario = new UsuarioSistema();
        usuario.setId(11L);
        usuario.setUsername("pending@ufps.edu.co");
        usuario.setActivo(true);
        usuario.setEstadoActivacion(EstadoActivacionCuenta.PENDIENTE);
        usuario.setPasswordHash(null);
        return usuario;
    }
}
