package co.edu.ufps.legal_cases.business.repository.catalogo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.edu.ufps.legal_cases.business.model.catalogo.Nacionalidad;

@Repository
public interface NacionalidadRepository extends JpaRepository<Nacionalidad, Long> {

    Optional<Nacionalidad> findByIdAndActivoTrue(Long id);

    List<Nacionalidad> findByActivoTrueOrderByNombreAsc();

    List<Nacionalidad> findAllByOrderByNombreAsc();

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}