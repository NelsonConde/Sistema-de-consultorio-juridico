package co.edu.ufps.legal_cases.business.model.conciliacion.reunion;

import java.time.LocalDateTime;

import co.edu.ufps.legal_cases.business.model.catalogo.Sede;
import co.edu.ufps.legal_cases.business.model.conciliacion.Conciliacion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "reunion_conciliacion")
@Getter
@Setter
public class ReunionConciliacion {

    @Id
    @Column(name = "conciliacion_id")
    private Long conciliacionId;

    // La reunión es inherente a una conciliación y usa su mismo id.
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "conciliacion_id", nullable = false)
    private Conciliacion conciliacion;

    @Column(name = "fecha_reunion", nullable = false)
    private LocalDateTime fechaReunion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;

    @Column(name = "observaciones", length = 300)
    private String observaciones;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @PrePersist
    public void prePersist() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }

        normalizarValores();
    }

    @PreUpdate
    public void preUpdate() {
        fechaActualizacion = LocalDateTime.now();
        normalizarValores();
    }

    private void normalizarValores() {
        if (observaciones != null) {
            observaciones = observaciones.trim();

            if (observaciones.isBlank()) {
                observaciones = null;
            }
        }
    }
}
