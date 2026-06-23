package co.edu.ufps.legal_cases.business.model.proceso;

public enum EstadoProceso {
    PENDIENTE,
    SENTENCIA_FAVORABLE,
    SENTENCIA_DESFAVORABLE,
    DESISTIMIENTO,
    RECHAZO,
    PRESCRIPCION;

    public boolean esFinal() {
        return this != PENDIENTE;
    }
}