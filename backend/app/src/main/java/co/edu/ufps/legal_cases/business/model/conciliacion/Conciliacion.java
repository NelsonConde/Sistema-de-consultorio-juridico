package co.edu.ufps.legal_cases.business.model.conciliacion;

import java.time.LocalDateTime;

import co.edu.ufps.legal_cases.business.model.consulta.Consulta;
import co.edu.ufps.legal_cases.business.model.perfil.Conciliador;
import co.edu.ufps.legal_cases.business.model.perfil.Estudiante;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "conciliacion")
@Getter
@Setter
public class Conciliacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Consulta desde la que nace la conciliación.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "consulta_id", nullable = false)
    private Consulta consulta;

    // Estudiante encargado de la conciliación.
    // Puede quedar null si todavía no hay estudiante habilitado para conciliación.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estudiante_id")
    private Estudiante estudiante;

    // Conciliador encargado de la conciliación.
    // Puede quedar null si todavía no hay conciliador asignado o disponible.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conciliador_id")
    private Conciliador conciliador;

    // Estado funcional. Se valida por codigo, no por nombre.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estado_id", nullable = false)
    private EstadoConciliacion estado;

    // Fecha principal programada para la conciliación.
    // La HU de reuniones podrá ampliar este manejo después.
    @Column(name = "fecha_conciliacion")
    private LocalDateTime fechaConciliacion;

    // Ruta del documento de solicitud.
    // Ejemplo: conciliacion/1/solicitud.pdf
    @Column(name = "documento_solicitud_path", length = 255)
    private String documentoSolicitudPath;

    // Ruta del acta de conciliación.
    // Ejemplo: conciliacion/1/acta.pdf
    @Column(name = "acta_path", length = 255)
    private String actaPath;

    // Usuario del sistema que generó la conciliación.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitado_por_id", nullable = false)
    private UsuarioSistema solicitadoPor;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @Column(name = "fecha_finalizacion")
    private LocalDateTime fechaFinalizacion;

    // Borrado lógico propio de la conciliación.
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

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
        if (activo == null) {
            activo = true;
        }
    }
}