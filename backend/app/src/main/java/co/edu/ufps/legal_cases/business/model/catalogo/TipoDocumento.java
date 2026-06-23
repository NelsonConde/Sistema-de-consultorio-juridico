package co.edu.ufps.legal_cases.business.model.catalogo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tipodoc")
@Getter
@Setter
public class TipoDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "displayname", nullable = false, length = 100)
    private String nombre;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;
}