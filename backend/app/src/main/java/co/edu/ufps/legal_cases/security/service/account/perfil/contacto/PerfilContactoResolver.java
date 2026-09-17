package co.edu.ufps.legal_cases.security.service.account.perfil.contacto;

import co.edu.ufps.legal_cases.security.dto.account.ActualizarContactoDTO;
import co.edu.ufps.legal_cases.security.dto.account.PerfilContactoDatos;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;

// Strategy para leer y actualizar los datos de contacto (nombre, email,
// telefono, sede) del perfil real asociado a un UsuarioSistema.
//
// Es un strategy paralelo a PerfilUsuarioActivoResolver: ese resuelve
// identidad (perfilId/tipoPerfil) para autorizacion y se usa en login y
// cambio de contraseña. Este resuelve contacto para "Mi Perfil" y se
// mantiene separado para no acoplar ambos flujos.
public interface PerfilContactoResolver {

    TipoPerfilUsuario getTipoPerfil();

    PerfilContactoDatos obtenerContacto(Long usuarioSistemaId);

    void actualizarContacto(Long usuarioSistemaId, ActualizarContactoDTO dto);
}