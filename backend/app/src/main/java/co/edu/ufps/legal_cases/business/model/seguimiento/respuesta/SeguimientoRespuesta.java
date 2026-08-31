package co.edu.ufps.legal_cases.business.model.seguimiento.respuesta;

import java.time.LocalDateTime;

import co.edu.ufps.legal_cases.business.model.perfil.Estudiante;
import co.edu.ufps.legal_cases.business.model.seguimiento.Seguimiento;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "seguimiento_respuesta")
@Getter
@Setter
public class SeguimientoRespuesta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Seguimiento que fue enviado o mostrado al estudiante.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seguimiento_id", nullable = false)
    private Seguimiento seguimiento;

    // Estudiante que responde el seguimiento.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estudiante_id", nullable = false)
    private Estudiante estudiante;

    @Column(name = "contenido", nullable = false, length = 1000)
    private String contenido;

    // La respuesta inicia pendiente hasta que asesor, monitor o admin la revise.
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoRespuestaSeguimiento estado = EstadoRespuestaSeguimiento.PENDIENTE;

    // Indica si esta respuesta fue enviada o editada después de la fecha de entrega.
    // No bloquea el flujo; solo deja evidencia para revisión.
    @Column(name = "fuera_plazo", nullable = false)
    private Boolean fueraPlazo = false;

    // Observación opcional al aprobar o rechazar.
    @Column(name = "observacion_revision", length = 500)
    private String observacionRevision;

    // Usuario del sistema que aprueba o rechaza la respuesta.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revisado_por_id")
    private UsuarioSistema revisadoPor;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @Column(name = "fecha_decision")
    private LocalDateTime fechaDecision;

    // Borrado lógico para conservar historial.
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @PrePersist
    public void prePersist() {
        LocalDateTime ahora = LocalDateTime.now();

        if (fechaCreacion == null) {
            fechaCreacion = ahora;
        }

        normalizarValores();
    }

    @PreUpdate
    public void preUpdate() {
        fechaActualizacion = LocalDateTime.now();
        normalizarValores();
    }

    private void normalizarValores() {
        if (estado == null) {
            estado = EstadoRespuestaSeguimiento.PENDIENTE;
        }

        if (fueraPlazo == null) {
            fueraPlazo = false;
        }

        if (activo == null) {
            activo = true;
        }
    }
}