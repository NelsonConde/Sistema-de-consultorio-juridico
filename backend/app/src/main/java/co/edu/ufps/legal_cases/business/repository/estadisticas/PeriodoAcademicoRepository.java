package co.edu.ufps.legal_cases.business.repository.estadisticas;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.edu.ufps.legal_cases.business.model.estadisticas.PeriodoAcademico;

@Repository
public interface PeriodoAcademicoRepository
        extends JpaRepository<PeriodoAcademico, Long> {

    Optional<PeriodoAcademico> findByAnioAndSemestreAndActivoTrue(
            Integer anio,
            Integer semestre);

    List<PeriodoAcademico> findByActivoTrueOrderByAnioAscSemestreAsc();
}