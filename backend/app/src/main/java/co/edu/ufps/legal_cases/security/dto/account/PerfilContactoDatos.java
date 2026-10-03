package co.edu.ufps.legal_cases.security.dto.account;

import lombok.AllArgsConstructor;
import lombok.Getter;

// DTO interno de transporte: lo que cada PerfilContactoResolver extrae de la
// tabla real del perfil (Estudiante/Asesor/Monitor/Administrativo/Conciliador).
// No se expone directamente al frontend; MiPerfilMapper lo combina con datos
// de UsuarioSistema para construir MiPerfilDTO.
@Getter
@AllArgsConstructor
public class PerfilContactoDatos {

    private String nombre;
    private String email;
    private String telefono;
    private String sedeNombre;
    private String codigo;
}