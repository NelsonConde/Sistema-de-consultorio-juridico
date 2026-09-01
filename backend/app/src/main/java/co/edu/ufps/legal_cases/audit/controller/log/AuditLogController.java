package co.edu.ufps.legal_cases.audit.controller.log;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_AUDITORIA;

import java.time.Instant;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.ufps.legal_cases.audit.dto.log.AuditLogDTO;
import co.edu.ufps.legal_cases.audit.dto.log.AuditLogFilter;
import co.edu.ufps.legal_cases.audit.model.log.AuditOutcome;
import co.edu.ufps.legal_cases.audit.service.log.AuditLogService;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Consulta de la bitácora con permiso y parámetros deliberadamente limitados. */
@RestController
@RequestMapping("/api/audit")
@PreAuthorize("hasAuthority('" + VER_AUDITORIA + "')")
@Validated
public class AuditLogController {

    private static final Map<String, String> SORT_FIELDS = Map.of(
            "occurredAt", "occurredAt",
            "actorUsername", "actorUsername",
            "action", "action",
            "entityName", "entityName",
            "outcome", "outcome");

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ResponseEntity<Page<AuditLogDTO>> getAuditLogs(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityName,
            @RequestParam(required = false) AuditOutcome outcome,
            @RequestParam(required = false) String correlationId,
            @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "occurredAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        validateRange(from, to);
        String property = SORT_FIELDS.get(sortBy);
        if (property == null) {
            throw new BusinessException("El campo de ordenamiento de auditoría no es válido");
        }
        Sort.Direction direction = parseDirection(sortDir);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, property));
        AuditLogFilter filter = new AuditLogFilter(
                username, action, entityName, outcome, correlationId, from, to);
        return ResponseEntity.ok(auditLogService.getAuditLogs(filter, pageable));
    }

    private Sort.Direction parseDirection(String value) {
        if ("asc".equalsIgnoreCase(value)) {
            return Sort.Direction.ASC;
        }
        if ("desc".equalsIgnoreCase(value)) {
            return Sort.Direction.DESC;
        }
        throw new BusinessException("La dirección de ordenamiento de auditoría no es válida");
    }

    private void validateRange(Instant from, Instant to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException("El inicio del rango de auditoría debe ser anterior al fin");
        }
    }
}
