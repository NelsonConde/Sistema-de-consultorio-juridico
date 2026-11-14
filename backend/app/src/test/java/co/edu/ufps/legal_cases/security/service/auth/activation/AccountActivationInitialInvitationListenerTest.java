package co.edu.ufps.legal_cases.security.service.auth.activation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import co.edu.ufps.legal_cases.security.service.account.usuario.UsuarioSistemaRegistradoEvent;

class AccountActivationInitialInvitationListenerTest {

    private AccountActivationInitialInvitationService invitationService;
    private AccountActivationEmailService emailService;
    private AccountActivationInitialInvitationListener listener;

    @BeforeEach
    void setUp() {
        invitationService = Mockito.mock(AccountActivationInitialInvitationService.class);
        emailService = Mockito.mock(AccountActivationEmailService.class);
        listener = new AccountActivationInitialInvitationListener(
                invitationService,
                emailService);
    }

    @Test
    void noEnviaCuandoYaExisteUnaInvitacionActiva() {
        UsuarioSistemaRegistradoEvent event = event();
        when(invitationService.crearTokenInicial(event.usuarioId()))
                .thenReturn(Optional.empty());

        listener.procesar(event);

        verify(emailService, never())
                .enviarInvitacionActivacion(
                        Mockito.anyString(),
                        Mockito.anyString());
    }

    @Test
    void falloAlPersistirTokenNoPropagaAlUsuarioYaCreado() {
        UsuarioSistemaRegistradoEvent event = event();
        when(invitationService.crearTokenInicial(event.usuarioId()))
                .thenThrow(new IllegalStateException("database unavailable"));

        assertDoesNotThrow(() -> listener.procesar(event));

        verify(emailService, never())
                .enviarInvitacionActivacion(
                        Mockito.anyString(),
                        Mockito.anyString());
    }

    @Test
    void falloDelProveedorNoPropagaAlUsuarioYaCreado() {
        UsuarioSistemaRegistradoEvent event = event();
        when(invitationService.crearTokenInicial(event.usuarioId()))
                .thenReturn(Optional.of("raw-token"));
        doThrow(new IllegalStateException("provider unavailable"))
                .when(emailService)
                .enviarInvitacionActivacion(event.email(), "raw-token");

        assertDoesNotThrow(() -> listener.procesar(event));

        verify(emailService)
                .enviarInvitacionActivacion(event.email(), "raw-token");
    }

    @Test
    void listenerSeEjecutaExclusivamenteDespuesDelCommit()
            throws NoSuchMethodException {
        Method method = AccountActivationInitialInvitationListener.class
                .getMethod("procesar", UsuarioSistemaRegistradoEvent.class);
        TransactionalEventListener listenerAnnotation =
                method.getAnnotation(TransactionalEventListener.class);

        assertEquals(TransactionPhase.AFTER_COMMIT, listenerAnnotation.phase());
        assertEquals(false, listenerAnnotation.fallbackExecution());
    }

    private UsuarioSistemaRegistradoEvent event() {
        return new UsuarioSistemaRegistradoEvent(
                7L,
                "pending@ufps.edu.co");
    }
}
