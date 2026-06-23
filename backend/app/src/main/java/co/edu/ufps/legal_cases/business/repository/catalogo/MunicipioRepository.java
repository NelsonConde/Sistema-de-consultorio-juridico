package co.edu.ufps.legal_cases.business.repository.catalogo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.edu.ufps.legal_cases.business.model.catalogo.Municipio;

@Repository
public interface MunicipioRepository extends JpaRepository<Municipio, Long> {

    Optional<Municipio> findByIdAndActivoTrue(Long id);

    List<Municipio> findByActivoTrueOrderByNombreAsc();

    List<Municipio> findAllByOrderByNombreAsc();

    List<Municipio> findByDepartamentoIdAndActivoTrueOrderByNombreAsc(Long departamentoId);

    List<Municipio> findByDepartamentoIdOrderByNombreAsc(Long departamentoId);

    boolean existsByNombreIgnoreCaseAndDepartamentoId(String nombre, Long departamentoId);

    boolean existsByNombreIgnoreCaseAndDepartamentoIdAndIdNot(String nombre, Long departamentoId, Long id);
}