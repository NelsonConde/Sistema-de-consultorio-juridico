package co.edu.ufps.legal_cases.audit.repository.log;

import co.edu.ufps.legal_cases.audit.model.log.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * Repositorio de Spring Data JPA para la entidad AuditLog.
 * Proporciona métodos para consultar los registros de auditoría de forma
 * paginada.
 * Aunque técnicamente hereda métodos de modificación (save, delete), la base de
 * datos y la
 * aplicación previenen las actualizaciones y eliminaciones.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByUsernameContainingIgnoreCase(String username, Pageable pageable);

    Page<AuditLog> findByAction(String action, Pageable pageable);

    Page<AuditLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    // So, Los repositorios JPA por defecto proveen save(), delete(), etc.
    // Aunque se exponga delete(), el trigger de la base de datos lo bloqueará.
    // toca chequear en supabase que se haya ejecutado con éxito el script
    // audit_inmutable.sql
}
