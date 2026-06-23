package co.edu.ufps.legal_cases.security.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

// DTO usado para devolver mensajes simples desde los endpoints de autenticación,
// por ejemplo en recuperación de contraseña, sin devolver datos sensibles.
@Getter
@AllArgsConstructor
public class AuthMessageResponseDTO {

    private String mensaje;
}