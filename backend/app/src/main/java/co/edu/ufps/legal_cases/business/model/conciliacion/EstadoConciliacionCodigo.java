package co.edu.ufps.legal_cases.business.model.conciliacion;

import java.util.Locale;

// Esta clase define los codigos tecnicos para estados de conciliacion que se usan para validaciones en backend
public final class EstadoConciliacionCodigo {

    public static final String EN_ESPERA = "EN_ESPERA";
    public static final String ESPERANDO_REUNION = "ESPERANDO_REUNION";
    public static final String REUNION_PROGRAMADA = "REUNION_PROGRAMADA";
    public static final String COMPLETO_CONCILIADO = "COMPLETO_CONCILIADO";
    public static final String COMPLETO_NO_CONCILIADO = "COMPLETO_NO_CONCILIADO";

    private EstadoConciliacionCodigo() {
    }

    public static String normalizar(String codigo) {
        if (codigo == null) {
            return null;
        }

        return codigo.trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("[\\s-]+", "_");
    }
}