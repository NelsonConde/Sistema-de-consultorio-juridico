package co.edu.ufps.legal_cases.business.repository.catalogo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.edu.ufps.legal_cases.business.model.catalogo.Tema;

@Repository
public interface TemaRepository extends JpaRepository<Tema, Long> {

    Optional<Tema> findByIdAndActivoTrue(Long id);

    List<Tema> findByActivoTrueOrderByNombreAsc();

    List<Tema> findAllByOrderByNombreAsc();

    List<Tema> findByAreaIdAndActivoTrueOrderByNombreAsc(Long areaId);

    List<Tema> findByAreaIdOrderByNombreAsc(Long areaId);

    boolean existsByNombreIgnoreCaseAndAreaId(String nombre, Long areaId);

    boolean existsByNombreIgnoreCaseAndAreaIdAndIdNot(String nombre, Long areaId, Long id);
}