package co.edu.ufps.legal_cases.common.exception;

public class ConsultaReasignacionNoDisponibleException extends RuntimeException {

    public ConsultaReasignacionNoDisponibleException() {
        super("La reasignacion no puede completarse en este momento");
    }
}
