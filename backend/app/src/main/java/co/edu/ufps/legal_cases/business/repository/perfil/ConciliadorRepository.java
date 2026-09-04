package co.edu.ufps.legal_cases.business.repository.perfil;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import co.edu.ufps.legal_cases.business.model.perfil.Conciliador;
import co.edu.ufps.legal_cases.business.model.perfil.TipoConciliador;

@Repository
public interface ConciliadorRepository extends JpaRepository<Conciliador, Long> {

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

    List<Conciliador> findByActivoTrue();

    Optional<Conciliador> findByUsuarioSistema_IdAndActivoTrue(Long usuarioSistemaId);

    Optional<Conciliador> findByUsuarioSistema_Id(Long usuarioSistemaId);

    @Query(value = """
            SELECT c.id AS id,
                   c.nombre AS nombre,
                   c.documento AS documento,
                   c.email AS email,
                   c.usuario AS usuario,
                   c.codigo AS codigo,
                   c.activo AS activo,
                   c.tipoConciliador AS tipoConciliador,
                   sede.id AS sedeId,
                   sede.nombre AS sedeNombre
            FROM Conciliador c
            JOIN c.sede sede
            WHERE (
                    CAST(:search AS String) IS NULL
                    OR LOWER(c.nombre) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(c.documento) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(c.email) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(c.usuario) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(c.codigo) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
              )
              AND (:activo IS NULL OR c.activo = :activo)
              AND (:tipoConciliador IS NULL OR c.tipoConciliador = :tipoConciliador)
            """, countQuery = """
            SELECT COUNT(c.id)
            FROM Conciliador c
            JOIN c.sede sede
            WHERE (
                    CAST(:search AS String) IS NULL
                    OR LOWER(c.nombre) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(c.documento) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(c.email) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(c.usuario) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(c.codigo) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
              )
              AND (:activo IS NULL OR c.activo = :activo)
              AND (:tipoConciliador IS NULL OR c.tipoConciliador = :tipoConciliador)
            """)
    Page<ConciliadorResumenProjection> buscarResumenPaginado(
            @Param("search") String search,
            @Param("activo") Boolean activo,
            @Param("tipoConciliador") TipoConciliador tipoConciliador,
            Pageable pageable);
}