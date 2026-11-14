package co.edu.ufps.legal_cases.security.service.auth.activation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import co.edu.ufps.legal_cases.security.service.account.usuario.UsuarioSistemaRegistradoEvent;

@Component
public class AccountActivationInitialInvitationListener {

    private static final Logger log = LoggerFactory.getLogger(
            AccountActivationInitialInvitationListener.class);

    private final AccountActivationInitialInvitationService invitationService;
    private final AccountActivationEmailService emailService;

    public AccountActivationInitialInvitationListener(
            AccountActivationInitialInvitationService invitationService,
            AccountActivationEmailService emailService) {
        this.invitationService = invitationService;
        this.emailService = emailService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void procesar(UsuarioSistemaRegistradoEvent event) {
        try {
            invitationService.crearTokenInicial(event.usuarioId())
                    .ifPresent(token -> enviar(event, token));
        } catch (RuntimeException ex) {
            log.error(
                    "No se pudo preparar la invitación inicial para usuarioId={}. La cuenta permanece pendiente y admite reenvío administrativo.",
                    event.usuarioId(),
                    ex);
        }
    }

    private void enviar(
            UsuarioSistemaRegistradoEvent event,
            String token) {
        try {
            emailService.enviarInvitacionActivacion(event.email(), token);
            log.info(
                    "Invitación inicial de activación despachada para usuarioId={}",
                    event.usuarioId());
        } catch (RuntimeException ex) {
            log.error(
                    "No se pudo despachar la invitación inicial para usuarioId={}. El token confirmado queda disponible para recuperación administrativa.",
                    event.usuarioId(),
                    ex);
        }
    }
}
