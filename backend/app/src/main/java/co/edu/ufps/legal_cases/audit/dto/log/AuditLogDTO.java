package co.edu.ufps.legal_cases.audit.dto.log;

import co.edu.ufps.legal_cases.audit.model.log.AuditLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data Transfer Object para enviar información de auditoría a los clientes
 * REST.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogDTO {
    private Long id;
    private String username;
    private String action;
    private String entityName;
    private String entityId;
    private LocalDateTime timestamp;
    private String details;

    public static AuditLogDTO fromEntity(AuditLog auditLog) {
        if (auditLog == null) {
            return null;
        }
        return AuditLogDTO.builder()
                .id(auditLog.getId())
                .username(auditLog.getUsername())
                .action(auditLog.getAction())
                .entityName(auditLog.getEntityName())
                .entityId(auditLog.getEntityId())
                .timestamp(auditLog.getTimestamp())
                .details(auditLog.getDetails())
                .build();
    }
}
