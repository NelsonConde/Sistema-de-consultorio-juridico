package co.edu.ufps.legal_cases.business.repository.perfil;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query(value = """
            SELECT a.id AS id,
                   a.nombre AS nombre,
                   a.documento AS documento,
                   a.email AS email,
                   a.usuario AS usuario,
                   a.codigo AS codigo,
                   a.activo AS activo,
                   a.directora AS directora,
                   sede.id AS sedeId,
                   sede.nombre AS sedeNombre
            FROM Administrativo a
            JOIN a.sede sede
            WHERE (
                    CAST(:search AS String) IS NULL
                    OR LOWER(a.nombre) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(a.documento) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(a.email) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(a.usuario) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(a.codigo) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
              )
              AND (:activo IS NULL OR a.activo = :activo)
            """, countQuery = """
            SELECT COUNT(a.id)
            FROM Administrativo a
            JOIN a.sede sede
            WHERE (
                    CAST(:search AS String) IS NULL
                    OR LOWER(a.nombre) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(a.documento) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(a.email) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(a.usuario) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(a.codigo) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
              )
              AND (:activo IS NULL OR a.activo = :activo)
            """)
    Page<AdministrativoResumenProjection> buscarResumenPaginado(
            @Param("search") String search,
            @Param("activo") Boolean activo,
            Pageable pageable);

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
