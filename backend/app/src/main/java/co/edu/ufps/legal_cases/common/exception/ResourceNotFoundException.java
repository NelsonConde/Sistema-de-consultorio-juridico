package co.edu.ufps.legal_cases.common.exception;

// Excepción controlada para recursos que no están disponibles para el solicitante.
// También puede utilizarse cuando diferenciar entre inexistencia y falta de alcance
// revelaría información que el usuario no debe conocer.
public class ResourceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(String mensaje) {
        super(mensaje);
    }
}