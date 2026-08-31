package co.edu.ufps.legal_cases.audit.model.log;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Registro probatorio append-only. La clase no expone setters y la base de datos
 * refuerza la prohibición de UPDATE y DELETE mediante una migración versionada.
 */
@Entity
@Table(name = "audit_logs")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false)
    private Long id;

    @Column(name = "actor_username", nullable = false, updatable = false, length = 150)
    private String actorUsername;

    @Column(nullable = false, updatable = false, length = 100)
    private String action;

    @Column(name = "entity_name", nullable = false, updatable = false, length = 100)
    private String entityName;

    @Column(name = "entity_id", updatable = false, length = 150)
    private String entityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private AuditOutcome outcome;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private AuditSource source;

    @Column(name = "correlation_id", nullable = false, updatable = false, length = 100)
    private String correlationId;

    @Column(name = "ip_address", updatable = false, length = 45)
    private String ipAddress;

    @Column(name = "user_agent", updatable = false, length = 512)
    private String userAgent;

    @Column(name = "reason_code", updatable = false, length = 120)
    private String reasonCode;

    @Column(columnDefinition = "TEXT", updatable = false)
    private String reason;

    @Column(name = "before_state_json", columnDefinition = "TEXT", updatable = false)
    private String beforeStateJson;

    @Column(name = "after_state_json", columnDefinition = "TEXT", updatable = false)
    private String afterStateJson;

    @Column(name = "metadata_json", columnDefinition = "TEXT", updatable = false)
    private String metadataJson;
}
