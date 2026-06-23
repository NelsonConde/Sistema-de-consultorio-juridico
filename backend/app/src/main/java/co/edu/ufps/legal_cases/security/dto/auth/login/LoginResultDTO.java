package co.edu.ufps.legal_cases.security.dto.auth.login;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResultDTO {

    //Para que luego de autenticar ya no devolver al frontend la info de usuario y rol
    //sino tambien el token para que lo guarde y envie en cada peticion

    private LoginResponseDTO response;

    private String token;
}