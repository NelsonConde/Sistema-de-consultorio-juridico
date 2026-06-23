package co.edu.ufps.legal_cases.business.model.conciliacion.reunion.notificacion;

import java.time.LocalDateTime;

import co.edu.ufps.legal_cases.business.model.conciliacion.Conciliacion;
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
@Table(name = "reunion_conciliacion_notificacion")
@Getter
@Setter
public class ReunionConciliacionNotificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Una conciliación puede generar varias notificaciones por destinatario y momento.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conciliacion_id", nullable = false)
    private Conciliacion conciliacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_destinatario", nullable = false, length = 40)
    private TipoDestinatarioReunionConciliacion tipoDestinatario;

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo", nullable = false, length = 40)
    private MotivoNotificacionReunionConciliacion motivo;

    @Enumerated(EnumType.STRING)
    @Column(name = "momento_notificacion", nullable = false, length = 30)
    private MomentoNotificacionReunionConciliacion momentoNotificacion;

    @Column(name = "destinatario_email", length = 150)
    private String destinatarioEmail;

    @Column(name = "destinatario_nombre", length = 150)
    private String destinatarioNombre;

    @Column(name = "fecha_programada", nullable = false)
    private LocalDateTime fechaProgramada;

    @Column(name = "fecha_envio")
    private LocalDateTime fechaEnvio;

    @Column(name = "enviada", nullable = false)
    private Boolean enviada = false;

    @Column(name = "intentos", nullable = false)
    private Integer intentos = 0;

    @Column(name = "error", length = 500)
    private String error;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @Column(name = "activa", nullable = false)
    private Boolean activa = true;

    @Column(name = "fecha_cancelacion")
    private LocalDateTime fechaCancelacion;

    @PrePersist
    public void prePersist() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }

        normalizarEstado();
    }

    @PreUpdate
    public void preUpdate() {
        fechaActualizacion = LocalDateTime.now();
        normalizarEstado();
    }

    // Mantiene valores seguros cuando JPA persiste o actualiza la notificación.
    private void normalizarEstado() {
        if (enviada == null) {
            enviada = false;
        }

        if (activa == null) {
            activa = true;
        }

        if (intentos == null) {
            intentos = 0;
        }
    }
}
