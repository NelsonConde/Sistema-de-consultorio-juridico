package co.edu.ufps.legal_cases.audit.service.log;

import java.time.LocalDateTime;

import co.edu.ufps.legal_cases.audit.dto.log.AuditLogDTO;
import co.edu.ufps.legal_cases.audit.model.log.AuditLog;
import co.edu.ufps.legal_cases.audit.repository.log.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio encargado de la lógica principal para almacenar y consultar
 * registros de auditoría.
 * Utiliza ejecuciones asíncronas para el almacenamiento con el fin de evitar
 * cuellos de botella en los flujos principales de negocio.
 */
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Guarda el registro de auditoría de forma asíncrona para no bloquear el hilo
     * de negocio.
     */
    @Async
    @Transactional
    public void logAction(String username, String action, String entityName, String entityId, String details) {
        AuditLog auditLog = AuditLog.builder()
                .username(username)
                .action(action)
                .entityName(entityName)
                .entityId(entityId)
                .details(details)
                .timestamp(LocalDateTime.now())
                .build();

        auditLogRepository.save(auditLog);
    }

    /**
     * Consulta paginada para los administrativos. (chequear frontend luego)
     */
    @Transactional(readOnly = true)
    public Page<AuditLogDTO> getAuditLogs(Pageable pageable) {
        return auditLogRepository
                .findAll(pageable)
                .map(AuditLogDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogDTO> getAuditLogsByUsername(String username, Pageable pageable) {
        return auditLogRepository
                .findByUsernameContainingIgnoreCase(username, pageable)
                .map(AuditLogDTO::fromEntity);
    }
}
