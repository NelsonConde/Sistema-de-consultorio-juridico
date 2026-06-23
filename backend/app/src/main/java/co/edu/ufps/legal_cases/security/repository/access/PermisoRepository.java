package co.edu.ufps.legal_cases.security.repository.access;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.edu.ufps.legal_cases.security.model.access.Permiso;

@Repository
public interface PermisoRepository extends JpaRepository<Permiso, Long> {

    Optional<Permiso> findByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    List<Permiso> findByActivoTrue();

    // Para buscar un permiso específico y validar que esté activo.
    Optional<Permiso> findByIdAndActivoTrue(Long id);

    // Para buscar varios permisos activos a la vez.
    List<Permiso> findByIdInAndActivoTrue(Set<Long> ids);
}