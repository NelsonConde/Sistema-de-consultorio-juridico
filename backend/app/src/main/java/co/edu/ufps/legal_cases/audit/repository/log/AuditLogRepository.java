package co.edu.ufps.legal_cases.audit.repository.log;

import co.edu.ufps.legal_cases.audit.model.log.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Repositorio de Spring Data JPA para la entidad AuditLog.
 * Proporciona métodos para consultar los registros de auditoría de forma
 * paginada.
 * Aunque técnicamente hereda métodos de modificación (save, delete), la base de
 * datos y la
 * aplicación previenen las actualizaciones y eliminaciones.
 */
@Repository
public interface AuditLogRepository
        extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {
}
