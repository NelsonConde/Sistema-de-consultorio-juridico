package co.edu.ufps.legal_cases.business.model.proceso;

import java.time.LocalDateTime;

import co.edu.ufps.legal_cases.business.model.catalogo.Departamento;
import co.edu.ufps.legal_cases.business.model.consulta.Consulta;
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
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "proceso")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Proceso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "numero_radicado", unique = true, length = 23)
    private String numeroRadicado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departamento_id", nullable = false)
    private Departamento departamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consulta_id", nullable = false)
    private Consulta consulta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organo_control_id")
    private OrganoControl organoControl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "especialidad_id")
    private Especialidad especialidad;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    // Estado real del resultado del proceso.
    // No reemplaza activo; activo sigue siendo borrado lógico.
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 40)
    private EstadoProceso estado = EstadoProceso.PENDIENTE;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @PrePersist
    private void prepararPersistencia() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }

        normalizarValoresPorDefecto();
    }

    @PreUpdate
    private void prepararActualizacion() {
        normalizarValoresPorDefecto();
    }

    private void normalizarValoresPorDefecto() {
        if (estado == null) {
            estado = EstadoProceso.PENDIENTE;
        }

        if (activo == null) {
            activo = true;
        }
    }
}
