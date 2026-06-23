package co.edu.ufps.legal_cases.security.dto.account.cambio;

import co.edu.ufps.legal_cases.business.model.perfil.TipoConciliador;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CambiarPerfilAConciliadorDTO extends CambiarPerfilBaseDTO {

    // Define si el conciliador es interno, externo u otro tipo permitido por el dominio.
    @NotNull(message = "El tipo de conciliador es obligatorio")
    private TipoConciliador tipoConciliador;
}