package co.edu.ufps.legal_cases.security.dto.account;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import co.edu.ufps.legal_cases.security.model.account.EstadoActivacionCuenta;

// DTO de salida para usuarios del sistema.
// No tiene validaciones porque no se recibe como entrada.
@Getter
@Setter
public class UsuarioSistemaDTO {

    private Long id;

    private String username;

    private Boolean activo;

    private EstadoActivacionCuenta estadoActivacion;

    private Long rolId;

    private String rolNombre;

    private Long perfilId;

    private String tipoPerfil;

    private List<String> permisos;
}
