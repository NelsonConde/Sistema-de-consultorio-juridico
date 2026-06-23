package co.edu.ufps.legal_cases.business.model.catalogo;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "area")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Area {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false, unique = true, length = 50)
    private String nombre;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @JsonIgnore
    @OneToMany(mappedBy = "area")
    private List<Tema> temas;
}