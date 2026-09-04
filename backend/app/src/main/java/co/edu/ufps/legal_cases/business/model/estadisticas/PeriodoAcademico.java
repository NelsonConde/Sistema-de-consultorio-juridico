package co.edu.ufps.legal_cases.business.model.estadisticas;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "periodo_academico",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_periodo_academico_anio_semestre",
                        columnNames = {"anio", "semestre"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PeriodoAcademico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "anio", nullable = false)
    private Integer anio;

    @Column(name = "semestre", nullable = false)
    private Integer semestre;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;
}