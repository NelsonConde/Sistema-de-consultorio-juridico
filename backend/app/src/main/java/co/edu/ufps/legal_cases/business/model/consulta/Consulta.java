package co.edu.ufps.legal_cases.business.model.consulta;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import co.edu.ufps.legal_cases.business.model.catalogo.Area;
import co.edu.ufps.legal_cases.business.model.catalogo.Sede;
import co.edu.ufps.legal_cases.business.model.catalogo.Tema;
import co.edu.ufps.legal_cases.business.model.catalogo.Tipo;
import co.edu.ufps.legal_cases.business.model.perfil.Asesor;
import co.edu.ufps.legal_cases.business.model.perfil.Estudiante;
import co.edu.ufps.legal_cases.business.model.perfil.Monitor;
import co.edu.ufps.legal_cases.business.model.persona.Persona;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "consulta")
@Getter
@Setter
public class Consulta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "descripcion", nullable = false, length = 500)
    private String descripcion;

    @Column(name = "hechos", nullable = false, columnDefinition = "TEXT")
    private String hechos;

    @Column(name = "pretensiones", nullable = false, columnDefinition = "TEXT")
    private String pretensiones;

    @Column(name = "concepto_juridico", nullable = false, columnDefinition = "TEXT")
    private String conceptoJuridico;

    @Column(name = "tramite", nullable = false, length = 100)
    private String tramite;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "tipo_violencia", length = 100)
    private String tipoViolencia;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 50)
    private EstadoConsulta estado;

    @Column(name = "resultado", length = 100)
    private String resultado;

    // Fecha de última actualización del registro.
    // Se usa para filtrar consultas por semestre en estadísticas.
    // Se llena automáticamente al crear y actualizar.
    @Column(name = "last_updated_at", nullable = false)
    private LocalDate lastUpdatedAt;

    // Relación simple: persona principal (parte solicitante)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "persona_id", nullable = false)
    private Persona persona;

    // Relación ManyToMany: partes (pueden ser varias personas)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "consulta_parte", joinColumns = @JoinColumn(name = "consulta_id"), inverseJoinColumns = @JoinColumn(name = "persona_id"))
    private List<Persona> partes = new ArrayList<>();

    // Relación ManyToMany: contrapartes
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "consulta_contraparte", joinColumns = @JoinColumn(name = "consulta_id"), inverseJoinColumns = @JoinColumn(name = "persona_id"))
    private List<Persona> contrapartes = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_id", nullable = false)
    private Area area;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tema_id", nullable = false)
    private Tema tema;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_id")
    private Tipo tipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asesor_id")
    private Asesor asesor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monitor_id")
    private Monitor monitor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estudiante_id")
    private Estudiante estudiante;

    @PrePersist
    @PreUpdate
    private void actualizarFecha() {
        this.lastUpdatedAt = LocalDate.now();
    }
}