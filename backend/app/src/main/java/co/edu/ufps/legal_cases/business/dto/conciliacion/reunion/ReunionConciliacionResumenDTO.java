package co.edu.ufps.legal_cases.business.dto.conciliacion.reunion;

import java.time.LocalDateTime;

public record ReunionConciliacionResumenDTO(
        Long conciliacionId,
        Long version,
        Long conciliacionVersion,
        Long consultaId,
        String estadoCodigo,
        String estadoNombre,
        Long sedeId,
        String sedeNombre,
        LocalDateTime fechaReunion,
        String observaciones,
        Long estudianteId,
        String estudianteNombre,
        Long conciliadorId,
        String conciliadorNombre,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion) {
}
