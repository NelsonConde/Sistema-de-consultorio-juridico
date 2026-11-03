package co.edu.ufps.legal_cases.security.model.account;

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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "usuario_sistema")
@Getter
@Setter
public class UsuarioSistema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Se usa el correo como usuario de acceso al sistema.
    @Column(name = "username", nullable = false, unique = true, length = 120)
    private String username;

    /*
     * Una cuenta pendiente todavía no tiene contraseña.
     * El hash solo se establece cuando el usuario completa la activación.
     */
    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    /**
     * Versión de las credenciales utilizada para invalidar JWT emitidos
     * antes de un cambio o restablecimiento de contraseña.
     */
    @Column(name = "auth_version", nullable = false)
    private long authVersion = 0L;

    /*
     * Estado exclusivo del proceso de activación inicial.
     *
     * El valor por defecto ACTIVA conserva el comportamiento de cuentas
     * existentes y objetos creados por código anterior. El flujo de registro
     * de nuevas cuentas establecerá PENDIENTE explícitamente.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_activacion", nullable = false, length = 20)
    private EstadoActivacionCuenta estadoActivacion =
            EstadoActivacionCuenta.ACTIVA;

    /*
     * Estado operativo de acceso de la cuenta.
     * No debe utilizarse para representar una activación pendiente.
     */
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    // Tipo de perfil real activo del usuario.
    // Este campo permite resolver el perfil desde su tabla real usando usuario_sistema_id.
    // Ejemplo:
    // ESTUDIANTE -> buscar en estudiante.usuario_sistema_id
    // ASESOR -> buscar en asesor.usuario_sistema_id
    // MONITOR -> buscar en monitor.usuario_sistema_id
    // ADMINISTRATIVO -> buscar en administrativo.usuario_sistema_id
    // CONCILIADOR -> buscar en conciliador.usuario_sistema_id
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_perfil_actual", length = 30)
    private TipoPerfilUsuario tipoPerfilActual;

    // Rol administrable asignado al usuario.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;
}