package co.edu.ufps.legal_cases.business.repository.perfil;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import co.edu.ufps.legal_cases.business.dto.seguimiento.notificacion.SeguimientoDestinatarioDTO;
import co.edu.ufps.legal_cases.business.model.perfil.Asesor;

import java.util.Optional;

@Repository
public interface AsesorRepository extends JpaRepository<Asesor, Long> {

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

    List<Asesor> findByActivoTrue();

    // Para en estudiante poder ver los asesores activos
    Optional<Asesor> findByIdAndActivoTrue(Long id);

    Optional<Asesor> findByUsuarioSistema_IdAndActivoTrue(Long usuarioSistemaId);

    Optional<Asesor> findByUsuarioSistema_Id(Long usuarioSistemaId);

    // Para obtener datos y notificar
    @Query("""
            SELECT new co.edu.ufps.legal_cases.business.dto.seguimiento.notificacion.SeguimientoDestinatarioDTO(
                a.email,
                a.nombre
            )
            FROM Asesor a
            WHERE a.usuarioSistema.id = :usuarioSistemaId
            AND a.activo = true
            """)
    Optional<SeguimientoDestinatarioDTO> findDestinatarioByUsuarioSistemaId(
            @Param("usuarioSistemaId") Long usuarioSistemaId);
}