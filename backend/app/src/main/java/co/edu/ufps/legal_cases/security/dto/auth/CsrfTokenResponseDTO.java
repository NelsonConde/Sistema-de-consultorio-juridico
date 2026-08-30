package co.edu.ufps.legal_cases.security.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

// Se usa para devolver el token CSRF al cliente para que lo envie en los encabezados
@Getter
@AllArgsConstructor
public class CsrfTokenResponseDTO {

    private String headerName;
    private String token;
}