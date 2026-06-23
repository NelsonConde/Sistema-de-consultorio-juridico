package co.edu.ufps.legal_cases.business.dto.conciliacion;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// DTO liviano para mostrar consultante, partes y contrapartes en el detalle.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConciliacionPersonaDTO {

    private Long id;

    private String nombre;
}