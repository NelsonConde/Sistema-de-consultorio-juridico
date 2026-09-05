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

    @Query(value = """
            SELECT m.id AS id,
                   m.nombre AS nombre,
                   m.documento AS documento,
                   m.email AS email,
                   m.usuario AS usuario,
                   m.codigo AS codigo,
                   m.activo AS activo,
                   sede.id AS sedeId,
                   sede.nombre AS sedeNombre
            FROM Monitor m
            JOIN m.sede sede
            WHERE (
                    CAST(:search AS String) IS NULL
                    OR LOWER(m.nombre) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(m.documento) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(m.email) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(m.usuario) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(m.codigo) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
              )
              AND (:activo IS NULL OR m.activo = :activo)
            """, countQuery = """
            SELECT COUNT(m.id)
            FROM Monitor m
            JOIN m.sede sede
            WHERE (
                    CAST(:search AS String) IS NULL
                    OR LOWER(m.nombre) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(m.documento) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(m.email) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(m.usuario) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(m.codigo) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
              )
              AND (:activo IS NULL OR m.activo = :activo)
            """)
    Page<MonitorResumenProjection> buscarResumenPaginado(
            @Param("search") String search,
            @Param("activo") Boolean activo,
            Pageable pageable);

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
