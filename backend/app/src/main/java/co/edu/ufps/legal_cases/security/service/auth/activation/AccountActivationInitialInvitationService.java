package co.edu.ufps.legal_cases.security.service.auth.activation;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.audit.aop.log.Auditable;
import co.edu.ufps.legal_cases.security.model.account.EstadoActivacionCuenta;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.repository.account.UsuarioSistemaRepository;

@Service
public class AccountActivationInitialInvitationService {

    private final UsuarioSistemaRepository usuarioSistemaRepository;
    private final AccountActivationTokenService tokenService;

    public AccountActivationInitialInvitationService(
            UsuarioSistemaRepository usuarioSistemaRepository,
            AccountActivationTokenService tokenService) {
        this.usuarioSistemaRepository = usuarioSistemaRepository;
        this.tokenService = tokenService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Auditable(
            action = "CREAR_INVITACION_ACTIVACION_INICIAL",
            entityName = "UsuarioSistema",
            entityId = "#usuarioId")
    public Optional<String> crearTokenInicial(Long usuarioId) {
        if (usuarioId == null) {
            return Optional.empty();
        }

        UsuarioSistema usuario = usuarioSistemaRepository
                .findByIdForUpdate(usuarioId)
                .orElse(null);

        if (!puedeRecibirInvitacionInicial(usuario)
                || tokenService.existeTokenActivo(usuario)) {
            return Optional.empty();
        }

        return Optional.of(
                tokenService.crearTokenParaUsuario(usuario).token());
    }

    private boolean puedeRecibirInvitacionInicial(UsuarioSistema usuario) {
        return usuario != null
                && Boolean.TRUE.equals(usuario.getActivo())
                && usuario.getEstadoActivacion() == EstadoActivacionCuenta.PENDIENTE
                && usuario.getPasswordHash() == null;
    }
}
