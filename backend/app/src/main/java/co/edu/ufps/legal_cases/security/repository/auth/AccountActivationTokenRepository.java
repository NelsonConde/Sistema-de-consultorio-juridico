package co.edu.ufps.legal_cases.security.repository.auth;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.model.auth.AccountActivationToken;
import jakarta.persistence.LockModeType;

@Repository
public interface AccountActivationTokenRepository
        extends JpaRepository<AccountActivationToken, Long> {

    List<AccountActivationToken>
    findByUsuarioSistemaAndUsadoFalseAndRevocadoFalse(
            UsuarioSistema usuarioSistema);

    // Incluye tokens revocados: el reenvío no debe eludir el cooldown al revocar.
    boolean existsByUsuarioSistemaAndFechaCreacionAfter(
            UsuarioSistema usuarioSistema,
            LocalDateTime fechaLimite);

    boolean existsByUsuarioSistemaAndUsadoFalseAndRevocadoFalse(
            UsuarioSistema usuarioSistema);

    /*
     * Obtiene únicamente el identificador del usuario para poder
     * adquirir primero su lock sin introducir entidades obsoletas
     * en el persistence context.
     */
    @Query("""
            SELECT t.usuarioSistema.id
            FROM AccountActivationToken t
            WHERE t.tokenHash = :tokenHash
            """)
    Optional<Long> findUsuarioIdByTokenHash(
            @Param("tokenHash") String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT t
            FROM AccountActivationToken t
            JOIN FETCH t.usuarioSistema
            WHERE t.tokenHash = :tokenHash
            """)
    Optional<AccountActivationToken> findByTokenHashForUpdate(
            @Param("tokenHash") String tokenHash);
}
