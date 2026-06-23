package co.edu.ufps.legal_cases.security.model.account;

import java.time.LocalDateTime;

import co.edu.ufps.legal_cases.security.model.access.Rol;
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
@Table(name = "usuario_cambio_perfil_historial")
@Getter
@Setter
public class UsuarioCambioPerfilHistorial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Usuario del sistema al que se le realizó el cambio de perfil/rol.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_sistema_id", nullable = false)
    private UsuarioSistema usuarioSistema;

    // Perfil anterior del usuario antes del cambio.
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_perfil_anterior", nullable = false, length = 30)
    private TipoPerfilUsuario tipoPerfilAnterior;

    // Id del registro real anterior.
    // Ejemplo: estudiante.id, asesor.id, monitor.id, etc.
    @Column(name = "perfil_anterior_id", nullable = false)
    private Long perfilAnteriorId;

    // Rol anterior del usuario antes del cambio.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rol_anterior_id")
    private Rol rolAnterior;

    @Column(name = "rol_anterior_nombre", nullable = false, length = 100)
    private String rolAnteriorNombre;

    // Perfil nuevo del usuario después del cambio.
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_perfil_nuevo", nullable = false, length = 30)
    private TipoPerfilUsuario tipoPerfilNuevo;

    // Id del registro real nuevo.
    // Ejemplo: administrativo.id, conciliador.id, estudiante.id, etc.
    @Column(name = "perfil_nuevo_id", nullable = false)
    private Long perfilNuevoId;

    // Rol nuevo del usuario después del cambio.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rol_nuevo_id")
    private Rol rolNuevo;

    @Column(name = "rol_nuevo_nombre", nullable = false, length = 100)
    private String rolNuevoNombre;

    // Usuario que realizó el cambio.
    // Puede ser null si más adelante el cambio se ejecuta desde un proceso interno.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cambiado_por_usuario_id")
    private UsuarioSistema cambiadoPorUsuario;

    @Column(name = "cambiado_por_username", length = 120)
    private String cambiadoPorUsername;

    @Column(name = "motivo", nullable = false, length = 500)
    private String motivo;

    @Column(name = "fecha_cambio", nullable = false)
    private LocalDateTime fechaCambio;

    @PrePersist
    public void prePersist() {
        if (fechaCambio == null) {
            fechaCambio = LocalDateTime.now();
        }
    }
}