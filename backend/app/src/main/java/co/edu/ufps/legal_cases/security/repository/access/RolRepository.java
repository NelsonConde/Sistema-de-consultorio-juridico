package co.edu.ufps.legal_cases.security.repository.access;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.edu.ufps.legal_cases.security.model.access.Rol;

@Repository
public interface RolRepository extends JpaRepository<Rol, Long> {

    Optional<Rol> findByNombreIgnoreCase(String nombre);

    Optional<Rol> findByNombreIgnoreCaseAndActivoTrue(String nombre);

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    List<Rol> findByActivoTrue();

    //Trae el rol con sus permisos asociados para evitar el problema de LazyInitializationException
    @EntityGraph(attributePaths = "permisos")
    Optional<Rol> findWithPermisosById(Long id);

    Optional<Rol> findByIdAndActivoTrue(Long id);
    
}