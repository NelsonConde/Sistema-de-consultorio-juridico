package co.edu.ufps.legal_cases.common.exception;

// Excepción controlada para reglas de negocio.
// Se usa cuando la solicitud es válida técnicamente,pero no cumple una regla funcional del sistema.
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BusinessException(String mensaje) {
        super(mensaje);
    }
}