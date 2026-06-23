package co.edu.ufps.legal_cases.business.model.conciliacion.reunion;

import java.time.LocalDateTime;

import co.edu.ufps.legal_cases.business.model.catalogo.Sede;
import co.edu.ufps.legal_cases.business.model.conciliacion.Conciliacion;
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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "reunion_conciliacion_historial")
@Getter
@Setter
public class ReunionConciliacionHistorial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conciliacion_id", nullable = false)
    private Conciliacion conciliacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evento", nullable = false, length = 30)
    private TipoEventoReunionConciliacion tipoEvento;

    @Column(name = "fecha_reunion_anterior")
    private LocalDateTime fechaReunionAnterior;

    @Column(name = "fecha_reunion_nueva", nullable = false)
    private LocalDateTime fechaReunionNueva;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sede_anterior_id")
    private Sede sedeAnterior;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sede_nueva_id", nullable = false)
    private Sede sedeNueva;

    @Column(name = "observaciones_anteriores", length = 300)
    private String observacionesAnteriores;

    @Column(name = "observaciones_nuevas", length = 300)
    private String observacionesNuevas;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "realizado_por_id", nullable = false)
    private UsuarioSistema realizadoPor;

    @Column(name = "fecha_evento", nullable = false)
    private LocalDateTime fechaEvento;

    @PrePersist
    public void prePersist() {
        if (fechaEvento == null) {
            fechaEvento = LocalDateTime.now();
        }

        observacionesAnteriores = normalizar(observacionesAnteriores);
        observacionesNuevas = normalizar(observacionesNuevas);
    }

    private String normalizar(String texto) {
        if (texto == null) {
            return null;
        }

        String valor = texto.trim();
        return valor.isBlank() ? null : valor;
    }
}
