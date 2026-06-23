package co.edu.ufps.legal_cases.common.util;

import java.util.Objects;
import java.util.function.Function;

// Utilidades comunes para comparar valores evitando errores por null.
// Se usan principalmente en validators para detectar cambios reales entre entidades y DTOs.
public final class ComparacionUtils {

    private ComparacionUtils() {
        // Clase utilitaria: no debe instanciarse.
    }

    // Compara textos ignorando mayúsculas y minúsculas.
    // Si alguno es null, conserva comparación segura con Objects.equals.
    public static boolean equalsIgnoreCase(String actual, String nuevo) {
        if (actual == null || nuevo == null) {
            return Objects.equals(actual, nuevo);
        }

        return actual.equalsIgnoreCase(nuevo);
    }

    // Compara dos objetos por su id sin exponer a los validators a errores por null.
    // El extractor permite usar cualquier entidad que tenga id.
    public static <T> boolean mismoId(T actual, T nuevo, Function<T, Long> obtenerId) {
        Long idActual = actual != null ? obtenerId.apply(actual) : null;
        Long idNuevo = nuevo != null ? obtenerId.apply(nuevo) : null;

        return Objects.equals(idActual, idNuevo);
    }
}