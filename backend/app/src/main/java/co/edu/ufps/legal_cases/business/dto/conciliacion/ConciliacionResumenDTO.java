package co.edu.ufps.legal_cases.business.dto.conciliacion;

import java.time.LocalDateTime;

public record ConciliacionResumenDTO(
        Long id,
        Long version,
        Long consultaId,
        String consulta,
        String estadoCodigo,
        String estadoNombre,
        Long estudianteId,
        String estudianteNombre,
        Long conciliadorId,
        String conciliadorNombre,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaConciliacion,
        LocalDateTime fechaFinalizacion,
        Boolean activo) {
}
