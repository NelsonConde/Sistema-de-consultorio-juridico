package co.edu.ufps.legal_cases.business.model.catalogo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "barrio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Barrio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @ManyToOne
    @JoinColumn(name = "municipio_id", nullable = false)
    private Municipio municipio;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;
}