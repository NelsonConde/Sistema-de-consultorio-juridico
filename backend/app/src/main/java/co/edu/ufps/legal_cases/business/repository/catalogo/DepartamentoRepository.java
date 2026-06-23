package co.edu.ufps.legal_cases.business.repository.catalogo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.edu.ufps.legal_cases.business.model.catalogo.Departamento;

@Repository
public interface DepartamentoRepository extends JpaRepository<Departamento, Long> {

    Optional<Departamento> findByIdAndActivoTrue(Long id);

    List<Departamento> findByActivoTrueOrderByNombreAsc();

    List<Departamento> findAllByOrderByNombreAsc();

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}