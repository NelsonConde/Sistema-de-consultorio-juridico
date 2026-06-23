package co.edu.ufps.legal_cases.common.util;

import java.util.Locale;

// Utilidades comunes para normalizar datos de entrada.
// No validan reglas de negocio; solo limpian formatos para que los services y validators trabajen con datos consistentes.
public final class NormalizacionUtils {

    private static final String VALOR_NO_INFORMA = "No informa";

    private NormalizacionUtils() {
        // Clase utilitaria: no debe instanciarse.
    }

    // Limpia espacios al inicio y final, y compacta espacios múltiples internos.
    // Si el resultado queda vacío, retorna null.
    public static String normalizarTexto(String valor) {
        if (valor == null) {
            return null;
        }

        String limpio = valor.trim().replaceAll("\\s+", " ");

        return limpio.isBlank() ? null : limpio;
    }

    // Determina si un texto fue realmente informado.
    // Se considera no informado cuando es null, vacío o "No informa".
    public static boolean estaInformado(String valor) {
        String limpio = normalizarTexto(valor);

        return limpio != null && !limpio.equalsIgnoreCase(VALOR_NO_INFORMA);
    }

    // Normaliza números de documento removiendo puntos, guiones y espacios.
    public static String normalizarNumeroDocumento(String valor) {
        return removerCaracteres(normalizarTexto(valor), "[\\.\\-\\s]");
    }

    // Normaliza teléfonos dejando únicamente dígitos.
    public static String normalizarTelefono(String valor) {
        return removerCaracteres(normalizarTexto(valor), "[^0-9]");
    }

    // Normaliza correos para comparaciones y persistencia consistente.
    public static String normalizarEmail(String valor) {
        return convertirMinuscula(normalizarTexto(valor));
    }

    // Normaliza nombres de usuario para evitar duplicados por mayúsculas/minúsculas.
    public static String normalizarUsuario(String valor) {
        return convertirMinuscula(normalizarTexto(valor));
    }

    // Normaliza códigos institucionales en mayúscula.
    public static String normalizarCodigo(String valor) {
        return convertirMayuscula(normalizarTexto(valor));
    }

    private static String removerCaracteres(String valor, String patron) {
        if (valor == null) {
            return null;
        }

        String limpio = valor.replaceAll(patron, "");

        return limpio.isBlank() ? null : limpio;
    }

    private static String convertirMinuscula(String valor) {
        return valor != null ? valor.toLowerCase(Locale.ROOT) : null;
    }

    private static String convertirMayuscula(String valor) {
        return valor != null ? valor.toUpperCase(Locale.ROOT) : null;
    }
}