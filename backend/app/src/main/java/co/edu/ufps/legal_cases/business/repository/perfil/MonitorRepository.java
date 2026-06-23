package co.edu.ufps.legal_cases.business.repository.perfil;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import co.edu.ufps.legal_cases.business.dto.seguimiento.notificacion.SeguimientoDestinatarioDTO;
import co.edu.ufps.legal_cases.business.model.perfil.Monitor;

@Repository
public interface MonitorRepository extends JpaRepository<Monitor, Long> {

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

    List<Monitor> findByActivoTrue();

    Optional<Monitor> findByIdAndActivoTrue(Long id);

    Optional<Monitor> findByUsuarioSistema_IdAndActivoTrue(Long usuarioSistemaId);

    Optional<Monitor> findByUsuarioSistema_Id(Long usuarioSistemaId);

    // Para obtener los datos de notificar
    @Query("""
            SELECT new co.edu.ufps.legal_cases.business.dto.seguimiento.notificacion.SeguimientoDestinatarioDTO(
                m.email,
                m.nombre
            )
            FROM Monitor m
            WHERE m.usuarioSistema.id = :usuarioSistemaId
            AND m.activo = true
            """)
    Optional<SeguimientoDestinatarioDTO> findDestinatarioByUsuarioSistemaId(
            @Param("usuarioSistemaId") Long usuarioSistemaId);
}