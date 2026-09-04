package co.edu.ufps.legal_cases.business.dto.agenda;

import java.time.OffsetDateTime;

public record AgendaEventDTO(
        String id,
        String type,
        String title,
        OffsetDateTime start,
        OffsetDateTime end,
        boolean allDay,
        Long resourceId,
        Long consultaId,
        String status,
        boolean overdue) {
}
