package co.edu.ufps.legal_cases.audit.controller.log;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.ACCEDER_ADMINISTRACION;

import co.edu.ufps.legal_cases.audit.dto.log.AuditLogDTO;
import co.edu.ufps.legal_cases.audit.service.log.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controlador REST que expone los endpoints de consulta de los registros de auditoría.
 * Todos los métodos de este controlador están estrictamente protegidos para permitir
 * el acceso únicamente a usuarios con el rol ADMIN, cumpliendo con los requerimientos
 * de seguridad.
 */
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @PreAuthorize("hasAuthority('" + ACCEDER_ADMINISTRACION + "')")
    public ResponseEntity<Page<AuditLogDTO>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String username,
            @RequestParam(defaultValue = "timestamp") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<AuditLogDTO> auditLogs;
        
        if (username != null && !username.isBlank()) {
            auditLogs = auditLogService.getAuditLogsByUsername(username, pageable);
        } else {
            auditLogs = auditLogService.getAuditLogs(pageable);
        }
        
        return ResponseEntity.ok(auditLogs);
    }
}
