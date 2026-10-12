package co.edu.ufps.legal_cases.common.exception;

// Representa una operacion directa incompatible con la politica de perfiles.
// Su codigo funcional permite distinguirla de un conflicto de concurrencia.
public class PerfilOperacionConflictException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public static final String CODIGO = "CONFLICTO_OPERACION_PERFIL";

    public PerfilOperacionConflictException(String mensaje) {
        super(mensaje);
    }

    public String getCodigo() {
        return CODIGO;
    }
}
