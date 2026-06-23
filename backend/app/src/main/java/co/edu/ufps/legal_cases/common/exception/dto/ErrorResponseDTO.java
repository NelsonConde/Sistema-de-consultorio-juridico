package co.edu.ufps.legal_cases.common.exception.dto;

import java.time.LocalDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// DTO estándar para respuestas de error de la API.
// Si detalles es null, no se serializa en la respuesta.
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponseDTO {

    private LocalDateTime fecha;

    private int estado;

    private String error;

    private String mensaje;

    private String ruta;

    private Map<String, String> detalles;
}