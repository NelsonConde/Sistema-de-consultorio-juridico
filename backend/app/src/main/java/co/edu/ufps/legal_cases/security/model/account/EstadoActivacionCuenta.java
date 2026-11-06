package co.edu.ufps.legal_cases.security.model.account;

/**
 * Estado del proceso de activación inicial de una cuenta.
 *
 * ACTIVA no representa si la cuenta está suspendida o habilitada.
 * Esa responsabilidad continúa perteneciendo al campo UsuarioSistema.activo.
 */
public enum EstadoActivacionCuenta {

    PENDIENTE,
    ACTIVA
}