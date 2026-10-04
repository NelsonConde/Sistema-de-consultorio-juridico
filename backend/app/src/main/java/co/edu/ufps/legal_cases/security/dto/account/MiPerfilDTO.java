package co.edu.ufps.legal_cases.security.dto.account;

import lombok.Getter;
import lombok.Setter;

// DTO de salida para la vista "Mi Perfil".
// La identidad se deriva siempre de la sesión autenticada.
// El documento nunca se expone completo.
// Email y telefono son los únicos datos editables desde autogestión.
@Getter
@Setter
public class MiPerfilDTO {

    private String username;
    private String rolNombre;
    private String tipoPerfil;
    private String nombre;
    private String documentoEnmascarado;
    private String email;
    private String telefono;
    private String sede;
    private String codigo;
}