package co.edu.ufps.legal_cases.security.model.auth;

import java.time.LocalDateTime;

import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "account_activation_token")
@Getter
@Setter
public class AccountActivationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Nunca se persiste el token real.
     * Solo se almacena su SHA-256 hexadecimal.
     */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_sistema_id", nullable = false)
    private UsuarioSistema usuarioSistema;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    /*
     * Un token usado ya completó exitosamente una activación.
     */
    @Column(name = "usado", nullable = false)
    private Boolean usado = false;

    @Column(name = "fecha_uso")
    private LocalDateTime fechaUso;

    /*
     * Un token revocado fue invalidado antes de utilizarse,
     * por ejemplo porque se generó una invitación posterior.
     */
    @Column(name = "revocado", nullable = false)
    private Boolean revocado = false;

    @Column(name = "fecha_revocacion")
    private LocalDateTime fechaRevocacion;
}