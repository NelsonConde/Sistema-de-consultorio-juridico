package co.edu.ufps.legal_cases.business.repository.perfil;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import co.edu.ufps.legal_cases.business.model.perfil.Estudiante;

@Repository
public interface EstudianteRepository extends JpaRepository<Estudiante, Long> {

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

    List<Estudiante> findByActivoTrueOrderByNombreAsc();

    Optional<Estudiante> findByIdAndActivoTrue(Long id);

    List<Estudiante> findByConciliacionTrueAndActivoTrue();

    // Estudiantes activos de un asesor específico
    List<Estudiante> findByAsesorIdAndActivoTrue(Long asesorId);

    Optional<Estudiante> findByUsuarioSistema_IdAndActivoTrue(Long usuarioSistemaId);

    Optional<Estudiante> findByUsuarioSistema_Id(Long usuarioSistemaId);

    @Query(value = """
            SELECT e.id AS id,
                   e.nombre AS nombre,
                   e.documento AS documento,
                   e.email AS email,
                   e.usuario AS usuario,
                   e.codigo AS codigo,
                   e.activo AS activo,
                   sede.id AS sedeId,
                   sede.nombre AS sedeNombre,
                   asesor.id AS asesorId,
                   asesor.nombre AS asesorNombre,
                   e.conciliacion AS conciliacion
            FROM Estudiante e
            JOIN e.sede sede
            JOIN e.asesor asesor
            WHERE (
                    CAST(:search AS String) IS NULL
                    OR LOWER(e.nombre) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(e.documento) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(e.email) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(e.usuario) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(e.codigo) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
              )
              AND (:activo IS NULL OR e.activo = :activo)
              AND (:asesorIdScope IS NULL OR asesor.id = :asesorIdScope)
            """, countQuery = """
            SELECT COUNT(e.id)
            FROM Estudiante e
            JOIN e.sede sede
            JOIN e.asesor asesor
            WHERE (
                    CAST(:search AS String) IS NULL
                    OR LOWER(e.nombre) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(e.documento) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(e.email) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(e.usuario) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(e.codigo) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
              )
              AND (:activo IS NULL OR e.activo = :activo)
              AND (:asesorIdScope IS NULL OR asesor.id = :asesorIdScope)
            """)
    Page<EstudianteResumenProjection> buscarResumenPaginado(
            @Param("search") String search,
            @Param("activo") Boolean activo,
            @Param("asesorIdScope") Long asesorIdScope,
            Pageable pageable);
}
