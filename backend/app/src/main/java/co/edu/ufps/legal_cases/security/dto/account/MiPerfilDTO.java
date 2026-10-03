package co.edu.ufps.legal_cases.security.dto.account;

import lombok.Getter;
import lombok.Setter;

// DTO de salida para la vista "Mi Perfil".
// username, rolNombre y tipoPerfil vienen de UsuarioSistema.
// nombre, email, telefono, sede y codigo vienen del perfil real
// (Estudiante/Asesor/Monitor/Administrativo/Conciliador), resuelto por
// PerfilContactoResolverRegistry.
// El frontend trata email y telefono como editables; el resto es de
// solo lectura (no hay endpoint que permita modificarlos desde aquí).
@Getter
@Setter
public class MiPerfilDTO {

    private String username;
    private String rolNombre;
    private String tipoPerfil;
    private String nombre;
    private String email;
    private String telefono;
    private String sede;
    private String codigo;
}