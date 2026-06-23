package co.edu.ufps.legal_cases.business.repository.persona;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.edu.ufps.legal_cases.business.model.persona.Empresa;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

    Optional<Empresa> findByIdAndActivoTrue(Long id);

    List<Empresa> findByActivoTrueOrderByNombreAsc();

    List<Empresa> findAllByOrderByNombreAsc();

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}