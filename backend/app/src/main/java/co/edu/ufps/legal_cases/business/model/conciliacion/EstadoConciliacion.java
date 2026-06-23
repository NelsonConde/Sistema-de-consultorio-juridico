package co.edu.ufps.legal_cases.business.model.conciliacion;

import java.util.Locale;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "estado_conciliacion")
@Getter
@Setter
public class EstadoConciliacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Código técnico usado por backend para reglas de negocio.
    @Column(name = "codigo", nullable = false, unique = true, length = 40)
    private String codigo;

    // Nombre visible para frontend/usuarios.
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "orden")
    private Integer orden;

    @PrePersist
    @PreUpdate
    private void normalizarValores() {
        if (codigo != null) {
            codigo = codigo.trim().toUpperCase(Locale.ROOT);
        }

        if (nombre != null) {
            nombre = nombre.trim();
        }

        if (activo == null) {
            activo = true;
        }
    }
}