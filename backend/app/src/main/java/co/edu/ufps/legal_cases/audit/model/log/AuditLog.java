package co.edu.ufps.legal_cases.audit.model.log;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entidad JPA que representa un registro de auditoría en la base de datos.
 * Esta clase almacena la información sobre "quién hizo qué, a qué entidad y cuándo".
 * Todos sus campos están marcados como updatable=false para garantizar inmutabilidad
 * a nivel de aplicación (además de la inmutabilidad a nivel de base de datos).
 */
@Entity
@Table(name = "audit_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false)
    private Long id;

    @Column(nullable = false, updatable = false)
    private String username;

    @Column(name = "action", nullable = false, updatable = false)
    private String action;

    @Column(name = "entity_name", nullable = false, updatable = false)
    private String entityName;

    @Column(name = "entity_id", updatable = false)
    private String entityId;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @Column(columnDefinition = "TEXT", updatable = false)
    private String details;
}
