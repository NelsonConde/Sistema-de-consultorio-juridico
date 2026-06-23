package co.edu.ufps.legal_cases.security.service.account.cambio;

import co.edu.ufps.legal_cases.security.dto.account.cambio.CambiarPerfilBaseDTO;
import co.edu.ufps.legal_cases.security.dto.account.cambio.ResultadoCambioPerfil;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;

// Para obligar al resto de servicios por cada perfil a tener estos metodos
public interface PerfilCambioHandler<T extends CambiarPerfilBaseDTO> {

    TipoPerfilUsuario getTipoPerfil();  // Para saber que perfil esta manejando

    Class<T> getDtoClass(); // Esto para saber que datos necesita ese tipo de cambio

    ResultadoCambioPerfil crearOActualizarPerfil(UsuarioSistema usuarioSistema, T dto); // Este ya hace el cambio
}