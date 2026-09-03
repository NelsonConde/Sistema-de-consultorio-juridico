package co.edu.ufps.legal_cases.business.service.estadisticas.periodo;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import co.edu.ufps.legal_cases.common.exception.BusinessException;

@Service
public class PeriodoEstadisticoService {

    static final int AÑO_MINIMO = 2024;

    private final Clock clock;

    public PeriodoEstadisticoService(Clock clock) {
        this.clock = clock;
    }

    public PeriodoEstadistico obtener(int año, int semestre) {
        validarSemestre(semestre);
        validarAño(año);

        PeriodoEstadistico periodo = construir(año, semestre);

        if (periodo.inicio().isAfter(LocalDate.now(clock))) {
            throw new BusinessException(
                    "No se pueden consultar estadísticas de un semestre que aún no ha comenzado");
        }

        return periodo;
    }

    public List<PeriodoEstadistico> listarDisponibles() {
        LocalDate hoy = LocalDate.now(clock);
        List<PeriodoEstadistico> periodos = new ArrayList<>();

        for (int año = AÑO_MINIMO; año <= hoy.getYear(); año++) {
            agregarSiInicio(año, 1, hoy, periodos);
            agregarSiInicio(año, 2, hoy, periodos);
        }

        return List.copyOf(periodos);
    }

    private void agregarSiInicio(
            int año,
            int semestre,
            LocalDate hoy,
            List<PeriodoEstadistico> periodos) {

        PeriodoEstadistico periodo = construir(año, semestre);

        if (!periodo.inicio().isAfter(hoy)) {
            periodos.add(periodo);
        }
    }

    private PeriodoEstadistico construir(int año, int semestre) {
        if (semestre == 1) {
            return new PeriodoEstadistico(
                    año,
                    semestre,
                    LocalDate.of(año, 1, 1),
                    LocalDate.of(año, 6, 30));
        }

        return new PeriodoEstadistico(
                año,
                semestre,
                LocalDate.of(año, 7, 1),
                LocalDate.of(año, 12, 31));
    }

    private void validarSemestre(int semestre) {
        if (semestre < 1 || semestre > 2) {
            throw new BusinessException(
                    "El semestre debe ser 1 o 2");
        }
    }

    private void validarAño(int año) {
        if (año < AÑO_MINIMO) {
            throw new BusinessException(
                    "No hay datos disponibles antes del año " + AÑO_MINIMO);
        }
    }
}