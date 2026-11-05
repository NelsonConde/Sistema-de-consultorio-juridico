package co.edu.ufps.legal_cases.security.dto.auth.activation;

import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReenviarInvitacionRequestDTO {

    private Long usuarioId;

    @Email(message = "El formato del correo es inválido")
    private String email;

    public ReenviarInvitacionRequestDTO(Long usuarioId, String email) {
        this.usuarioId = usuarioId;
        this.email = email;
    }
}
