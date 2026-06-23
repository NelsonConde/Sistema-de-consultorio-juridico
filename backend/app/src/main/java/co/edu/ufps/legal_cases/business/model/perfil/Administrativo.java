package co.edu.ufps.legal_cases.business.model.perfil;

import co.edu.ufps.legal_cases.business.model.catalogo.Sede;
import co.edu.ufps.legal_cases.business.model.catalogo.TipoDocumento;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "administrativo")
@Getter
@Setter
public class Administrativo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Usuario del sistema asociado a este administrativo.
    // Esta relación hace parte de la normalización nueva.
    // Por ahora es nullable para permitir migrar datos existentes sin romper el arranque.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_sistema_id", unique = true)
    private UsuarioSistema usuarioSistema;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_documento", nullable = false)
    private TipoDocumento tipoDocumento;

    @Column(name = "documento", nullable = false, unique = true, length = 30)
    private String documento;

    @Column(name = "email", nullable = false, unique = true, length = 120)
    private String email;

    @Column(name = "telefono", nullable = false, unique = true, length = 30)
    private String telefono;

    @Column(name = "usuario", nullable = false, unique = true, length = 50)
    private String usuario;

    @Column(name = "codigo", nullable = false, unique = true, length = 30)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sede", nullable = false)
    private Sede sede;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "directora", nullable = false)
    private Boolean directora = false;
}