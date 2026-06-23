package co.edu.ufps.legal_cases.business.repository.catalogo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.edu.ufps.legal_cases.business.model.catalogo.Tipo;

@Repository
public interface TipoRepository extends JpaRepository<Tipo, Long> {

    Optional<Tipo> findByIdAndActivoTrue(Long id);

    List<Tipo> findByActivoTrueOrderByNombreAsc();

    List<Tipo> findAllByOrderByNombreAsc();

    List<Tipo> findByTemaIdAndActivoTrueOrderByNombreAsc(Long temaId);

    List<Tipo> findByTemaIdOrderByNombreAsc(Long temaId);

    boolean existsByNombreIgnoreCaseAndTemaId(String nombre, Long temaId);

    boolean existsByNombreIgnoreCaseAndTemaIdAndIdNot(String nombre, Long temaId, Long id);
}