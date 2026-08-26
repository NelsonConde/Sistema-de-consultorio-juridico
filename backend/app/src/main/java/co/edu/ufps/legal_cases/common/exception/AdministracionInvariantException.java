package co.edu.ufps.legal_cases.common.exception;

// Representa un conflicto donde una operación válida de forma aislada
// dejaría la administración del sistema en un estado no permitido.
public class AdministracionInvariantException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public AdministracionInvariantException(String mensaje) {
        super(mensaje);
    }
}