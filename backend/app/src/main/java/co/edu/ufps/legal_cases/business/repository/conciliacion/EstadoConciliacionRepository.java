package co.edu.ufps.legal_cases.business.repository.conciliacion;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.edu.ufps.legal_cases.business.model.conciliacion.EstadoConciliacion;

@Repository
public interface EstadoConciliacionRepository extends JpaRepository<EstadoConciliacion, Long> {

    Optional<EstadoConciliacion> findByCodigoAndActivoTrue(String codigo);

    Optional<EstadoConciliacion> findByCodigo(String codigo);

    List<EstadoConciliacion> findByActivoTrueOrderByOrdenAscNombreAsc();
}