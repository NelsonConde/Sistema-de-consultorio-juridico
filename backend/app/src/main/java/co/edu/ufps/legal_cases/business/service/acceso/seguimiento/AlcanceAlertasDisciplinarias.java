package co.edu.ufps.legal_cases.business.service.acceso.seguimiento;

/**
 * Representa el alcance permitido para consultar alertas disciplinarias.
 * El perfilId solo aplica cuando el alcance está limitado a un asesor o
 * monitor.
 */
public record AlcanceAlertasDisciplinarias(
        Tipo tipo,
        Long perfilId) {

    public enum Tipo {
        GLOBAL,
        ASESOR,
        MONITOR
    }

    public AlcanceAlertasDisciplinarias {
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo de alcance es obligatorio");
        }

        if (tipo == Tipo.GLOBAL && perfilId != null) {
            throw new IllegalArgumentException(
                    "El alcance global no debe tener un perfil asociado");
        }

        if (tipo != Tipo.GLOBAL && perfilId == null) {
            throw new IllegalArgumentException(
                    "El perfil es obligatorio para un alcance restringido");
        }
    }

    public static AlcanceAlertasDisciplinarias global() {
        return new AlcanceAlertasDisciplinarias(Tipo.GLOBAL, null);
    }

    public static AlcanceAlertasDisciplinarias asesor(Long asesorId) {
        return new AlcanceAlertasDisciplinarias(Tipo.ASESOR, asesorId);
    }

    public static AlcanceAlertasDisciplinarias monitor(Long monitorId) {
        return new AlcanceAlertasDisciplinarias(Tipo.MONITOR, monitorId);
    }
}