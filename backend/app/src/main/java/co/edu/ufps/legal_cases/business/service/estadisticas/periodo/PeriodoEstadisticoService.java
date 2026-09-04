package co.edu.ufps.legal_cases.business.service.estadisticas.periodo;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import co.edu.ufps.legal_cases.business.model.estadisticas.PeriodoAcademico;
import co.edu.ufps.legal_cases.business.repository.estadisticas.PeriodoAcademicoRepository;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

@Service
public class PeriodoEstadisticoService {

    private final PeriodoAcademicoRepository periodoAcademicoRepository;
    private final Clock clock;

    public PeriodoEstadisticoService(
            PeriodoAcademicoRepository periodoAcademicoRepository,
            Clock clock) {
        this.periodoAcademicoRepository = periodoAcademicoRepository;
        this.clock = clock;
    }

    public PeriodoEstadistico obtener(int año, int semestre) {
        validarSemestre(semestre);

        PeriodoAcademico periodoAcademico =
                periodoAcademicoRepository
                        .findByAnioAndSemestreAndActivoTrue(año, semestre)
                        .orElseThrow(() -> new BusinessException(
                                "No existe un periodo académico activo para "
                                        + año
                                        + "-"
                                        + semestre));

        PeriodoEstadistico periodo = mapear(periodoAcademico);

        if (periodo.inicio().isAfter(LocalDate.now(clock))) {
            throw new BusinessException(
                    "No se pueden consultar estadísticas de un semestre que aún no ha comenzado");
        }

        return periodo;
    }

    public List<PeriodoEstadistico> listarDisponibles() {
        LocalDate hoy = LocalDate.now(clock);

        return periodoAcademicoRepository
                .findByActivoTrueOrderByAnioAscSemestreAsc()
                .stream()
                .map(this::mapear)
                .filter(periodo -> !periodo.inicio().isAfter(hoy))
                .toList();
    }

    private PeriodoEstadistico mapear(
            PeriodoAcademico periodoAcademico) {

        validarFechas(periodoAcademico);

        return new PeriodoEstadistico(
                periodoAcademico.getAnio(),
                periodoAcademico.getSemestre(),
                periodoAcademico.getFechaInicio(),
                periodoAcademico.getFechaFin());
    }

    private void validarSemestre(int semestre) {
        if (semestre < 1 || semestre > 2) {
            throw new BusinessException(
                    "El semestre debe ser 1 o 2");
        }
    }

    private void validarFechas(
            PeriodoAcademico periodoAcademico) {

        if (periodoAcademico.getFechaInicio() == null
                || periodoAcademico.getFechaFin() == null) {
            throw new BusinessException(
                    "El periodo académico tiene fechas incompletas");
        }

        if (periodoAcademico
                .getFechaInicio()
                .isAfter(periodoAcademico.getFechaFin())) {
            throw new BusinessException(
                    "La fecha de inicio del periodo académico no puede ser posterior a la fecha de fin");
        }
    }
}