package co.edu.ufps.legal_cases.business.repository.perfil;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import co.edu.ufps.legal_cases.business.dto.seguimiento.notificacion.SeguimientoDestinatarioDTO;
import co.edu.ufps.legal_cases.business.model.perfil.Administrativo;

@Repository
public interface AdministrativoRepository extends JpaRepository<Administrativo, Long> {

    boolean existsByDocumento(String documento);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByTelefono(String telefono);

    boolean existsByUsuarioIgnoreCase(String usuario);

    boolean existsByCodigoIgnoreCase(String codigo);

    boolean existsByDocumentoAndIdNot(String documento, Long id);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    boolean existsByTelefonoAndIdNot(String telefono, Long id);

    boolean existsByUsuarioIgnoreCaseAndIdNot(String usuario, Long id);

    boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Long id);

    List<Administrativo> findByActivoTrue();

    List<Administrativo> findByDirectoraTrueAndActivoTrue();

    Optional<Administrativo> findByUsuarioSistema_IdAndActivoTrue(Long usuarioSistemaId);

    Optional<Administrativo> findByUsuarioSistema_Id(Long usuarioSistemaId);

    // Para luego notificar en caso de alertas disiplinarias
    @Query("""
            SELECT new co.edu.ufps.legal_cases.business.dto.seguimiento.notificacion.SeguimientoDestinatarioDTO(
                a.email,
                a.nombre
            )
            FROM Administrativo a
            WHERE a.activo = true
            """)
    List<SeguimientoDestinatarioDTO> findDestinatariosActivos();
}