package co.edu.ufps.legal_cases.business.repository.catalogo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.edu.ufps.legal_cases.business.model.catalogo.Barrio;

@Repository
public interface BarrioRepository extends JpaRepository<Barrio, Long> {

    Optional<Barrio> findByIdAndActivoTrue(Long id);

    List<Barrio> findByActivoTrueOrderByNombreAsc();

    List<Barrio> findAllByOrderByNombreAsc();

    List<Barrio> findByMunicipioIdAndActivoTrueOrderByNombreAsc(Long municipioId);

    List<Barrio> findByMunicipioIdOrderByNombreAsc(Long municipioId);

    boolean existsByNombreIgnoreCaseAndMunicipioId(String nombre, Long municipioId);

    boolean existsByNombreIgnoreCaseAndMunicipioIdAndIdNot(String nombre, Long municipioId, Long id);
}