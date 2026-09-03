package co.edu.ufps.legal_cases.business.service.estadisticas.periodo;

import java.time.LocalDate;

public record PeriodoEstadistico(
        int año,
        int semestre,
        LocalDate inicio,
        LocalDate fin) {
}