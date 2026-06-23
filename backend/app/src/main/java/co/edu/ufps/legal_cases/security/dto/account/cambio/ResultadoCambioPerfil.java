package co.edu.ufps.legal_cases.security.dto.account.cambio;

import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import lombok.AllArgsConstructor;
import lombok.Getter;

// Resultado interno del handler de cambio de perfil.
// Permite que el orquestador sepa qué perfil quedó activo después del cambio.
@Getter
@AllArgsConstructor
public class ResultadoCambioPerfil {

    private Long perfilId;

    private TipoPerfilUsuario tipoPerfil;
}