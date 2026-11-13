package co.edu.ufps.legal_cases.security.service.account.usuario;

public record UsuarioSistemaRegistradoEvent(
        Long usuarioId,
        String email) {
}
