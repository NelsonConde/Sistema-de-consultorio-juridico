package co.edu.ufps.legal_cases.security.dto.account.cambio;

import lombok.AllArgsConstructor;
import lombok.Getter;

// Datos comunes ya normalizados para crear, actualizar o reactivar un perfil.
// Se usan internamente después de limpiar textos, documento, teléfono, usuario y código.
@Getter
@AllArgsConstructor
public class DatosPerfilCambioNormalizados {

    private String nombre;

    private String documento;

    private String email;

    private String telefono;

    private String usuario;

    private String codigo;
}