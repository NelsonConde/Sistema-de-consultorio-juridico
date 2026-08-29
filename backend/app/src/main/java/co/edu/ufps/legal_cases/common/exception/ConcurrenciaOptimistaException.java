package co.edu.ufps.legal_cases.common.exception;

// Representa un conflicto de concurrencia cuando el cliente intenta
// modificar una versión anterior de un recurso.
public class ConcurrenciaOptimistaException extends RuntimeException {

    public ConcurrenciaOptimistaException(String message) {
        super(message);
    }
}