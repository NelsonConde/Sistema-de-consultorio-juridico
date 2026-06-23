package co.edu.ufps.legal_cases.security.repository.account;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.edu.ufps.legal_cases.security.model.account.UsuarioCambioPerfilHistorial;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;

@Repository
public interface UsuarioCambioPerfilHistorialRepository
        extends JpaRepository<UsuarioCambioPerfilHistorial, Long> {

    List<UsuarioCambioPerfilHistorial> findByUsuarioSistemaOrderByFechaCambioDesc(
            UsuarioSistema usuarioSistema
    );
}