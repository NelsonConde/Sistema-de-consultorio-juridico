package co.edu.ufps.legal_cases.security.model.access;

import java.util.HashSet;
import java.util.Set;

import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "rol")
@Getter
@Setter
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false, unique = true, length = 50)
    private String nombre;

    @Column(name = "descripcion", length = 255)
    private String descripcion;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "rol_permiso", joinColumns = @JoinColumn(name = "rol_id"), inverseJoinColumns = @JoinColumn(name = "permiso_id"))
    private Set<Permiso> permisos = new HashSet<>();

    // Tipo de perfil al que pertenece este rol.
    // Se usa para validar que un usuario no quede con un rol incompatible
    // con su perfil real activo.
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_perfil", length = 30, nullable = false)
    private TipoPerfilUsuario tipoPerfil;
}