package co.edu.ufps.legal_cases.business.dto.consulta;

import java.time.LocalDate;

import co.edu.ufps.legal_cases.business.model.consulta.EstadoConsulta;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de respuesta para la búsqueda de consultas jurídicas.
 * Los campos coinciden exactamente con lo que el frontend espera:
 * id, consulta, fecha, nombre, apellido, cedula
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConsultaBusquedaDTO {

    private Long id;

    /** Descripción de la consulta (campo "consulta" en el frontend). */
    private String consulta;

    private LocalDate fecha;

    private String nombre;

    private String apellido;

    /** Número de documento de la persona (campo "cedula" en el frontend). */
    private String cedula;

    private EstadoConsulta estado;
}