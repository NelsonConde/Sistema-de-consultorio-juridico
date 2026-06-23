package co.edu.ufps.legal_cases.business.dto.estadisticas;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// DTO genérico para estadísticas agrupadas: nombre del grupo y cantidad.
// Se usa para consultas por estado, por área, etc.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConteoDTO {

    private String nombre;
    private long cantidad;
}