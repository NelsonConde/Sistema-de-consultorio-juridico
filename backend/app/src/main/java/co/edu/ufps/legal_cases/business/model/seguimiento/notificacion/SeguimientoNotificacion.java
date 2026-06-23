package co.edu.ufps.legal_cases.business.model.seguimiento.notificacion;

import java.time.LocalDate;
import java.time.LocalDateTime;

import co.edu.ufps.legal_cases.business.model.seguimiento.Seguimiento;
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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
// Esta restriccion asegura que no existan 2 notificaciones iguales para el mismo seguimiento.
// Se usa tipo + momento porque una misma persona puede tener correo inmediato y recordatorio.
@Table(name = "seguimiento_notificacion", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_seguimiento_notificacion_tipo_momento",
                columnNames = {
                        "seguimiento_id",
                        "tipo_notificacion",
                        "momento_notificacion"
                }
        )
})
@Getter
@Setter
public class SeguimientoNotificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Un seguimiento puede generar varias notificaciones por correo.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seguimiento_id", nullable = false)
    private Seguimiento seguimiento;

    // Define a quien va dirigida la notificacion.
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_notificacion", nullable = false, length = 40)
    private TipoNotificacionSeguimiento tipoNotificacion;

    // Define si se envia inmediatamente o como recordatorio.
    @Enumerated(EnumType.STRING)
    @Column(name = "momento_notificacion", nullable = false, length = 30)
    private MomentoNotificacionSeguimiento momentoNotificacion;

    // Fecha en la que el scheduler debe enviar el correo.
    // Para notificaciones inmediatas queda con la fecha actual.
    @Column(name = "fecha_programada", nullable = false)
    private LocalDate fechaProgramada;

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
    private Boolean activo = true;

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

    // Para asegurarse de que los campos de estado no queden nulos.
    private void normalizarEstado() {
        if (enviada == null) {
            enviada = false;
        }

        if (activo == null) {
            activo = true;
        }

        if (intentos == null) {
            intentos = 0;
        }
    }
}