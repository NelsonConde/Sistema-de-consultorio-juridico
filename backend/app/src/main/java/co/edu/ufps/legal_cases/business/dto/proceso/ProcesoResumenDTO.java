package co.edu.ufps.legal_cases.business.dto.proceso;

import java.time.LocalDateTime;

import co.edu.ufps.legal_cases.business.model.proceso.EstadoProceso;

public record ProcesoResumenDTO(
        Long id,
        Long version,
        String numeroRadicado,
        Long departamentoId,
        String departamentoNombre,
        Long consultaId,
        String consulta,
        Long organoControlId,
        String organoControlNombre,
        Long especialidadId,
        String especialidadNombre,
        EstadoProceso estado,
        Boolean activo,
        LocalDateTime fechaCreacion) {
}
