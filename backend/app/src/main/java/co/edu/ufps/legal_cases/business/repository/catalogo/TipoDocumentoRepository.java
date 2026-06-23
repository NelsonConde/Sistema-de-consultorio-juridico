package co.edu.ufps.legal_cases.business.repository.catalogo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.edu.ufps.legal_cases.business.model.catalogo.TipoDocumento;

@Repository
public interface TipoDocumentoRepository extends JpaRepository<TipoDocumento, Long> {

    Optional<TipoDocumento> findByIdAndActivoTrue(Long id);

    List<TipoDocumento> findByActivoTrueOrderByNombreAsc();

    List<TipoDocumento> findAllByOrderByNombreAsc();

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}