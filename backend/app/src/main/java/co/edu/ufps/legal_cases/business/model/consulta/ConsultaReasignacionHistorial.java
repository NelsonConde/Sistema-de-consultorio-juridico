package co.edu.ufps.legal_cases.business.model.consulta;

import java.time.Instant;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

/**
 * Evento inmutable que conserva una reasignacion confirmada de una consulta.
 *
 * Los identificadores de responsables son escalares porque cada tipo pertenece
 * a una tabla distinta. La integridad polimorfica se valida en PostgreSQL.
 */
@Entity
@Table(name = "consulta_reasignacion_historial")
@Immutable
@Getter
public class ConsultaReasignacionHistorial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "consulta_id", nullable = false, updatable = false)
    private Long consultaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_responsable", nullable = false, length = 20, updatable = false)
    private TipoResponsableConsulta tipoResponsable;

    @Column(name = "responsable_anterior_id", updatable = false)
    private Long responsableAnteriorId;

    @Column(name = "responsable_nuevo_id", nullable = false, updatable = false)
    private Long responsableNuevoId;

    @Column(name = "motivo", nullable = false, columnDefinition = "TEXT", updatable = false)
    private String motivo;

    @Column(name = "actor_usuario_id", nullable = false, updatable = false)
    private Long actorUsuarioId;

    @Column(name = "ocurrido_en", nullable = false, updatable = false)
    private Instant ocurridoEn;

    protected ConsultaReasignacionHistorial() {
        // Requerido por JPA.
    }

    public ConsultaReasignacionHistorial(
            Long consultaId,
            TipoResponsableConsulta tipoResponsable,
            Long responsableAnteriorId,
            Long responsableNuevoId,
            String motivo,
            Long actorUsuarioId,
            Instant ocurridoEn) {
        this.consultaId = consultaId;
        this.tipoResponsable = tipoResponsable;
        this.responsableAnteriorId = responsableAnteriorId;
        this.responsableNuevoId = responsableNuevoId;
        this.motivo = motivo;
        this.actorUsuarioId = actorUsuarioId;
        this.ocurridoEn = ocurridoEn;
    }
}
