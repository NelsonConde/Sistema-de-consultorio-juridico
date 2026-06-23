package co.edu.ufps.legal_cases.business.dto.estadisticas;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// DTO para listar los semestres disponibles en el selector del frontend.
// Solo se listan semestres que ya hayan comenzado.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SemestreDTO {

    private int año;
    private int semestre;

    // Etiqueta legible para el frontend: "2025-1", "2025-2", etc.
    private String etiqueta;

    // Rango del semestre para referencia visual.
    private String periodoInicio;
    private String periodoFin;
}