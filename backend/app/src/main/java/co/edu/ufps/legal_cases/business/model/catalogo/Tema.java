package co.edu.ufps.legal_cases.business.model.catalogo;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tema")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Tema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false, length = 80)
    private String nombre;

    @ManyToOne
    @JoinColumn(name = "area_id", nullable = false)
    private Area area;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @JsonIgnore
    @OneToMany(mappedBy = "tema")
    private List<Tipo> tipos;
}