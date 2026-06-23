package co.edu.ufps.legal_cases.business.repository.catalogo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.edu.ufps.legal_cases.business.model.catalogo.Sede;

@Repository
public interface SedeRepository extends JpaRepository<Sede, Long> {

    Optional<Sede> findByIdAndActivoTrue(Long id);

    List<Sede> findByActivoTrueOrderByNombreAsc();

    List<Sede> findAllByOrderByNombreAsc();

    Optional<Sede> findByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}