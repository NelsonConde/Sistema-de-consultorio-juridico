package co.edu.ufps.legal_cases.common.dto;

import java.util.List;

/**
 * Contrato de respuesta paginada reutilizable para cualquier recurso del
 * sistema.
 *
 * <p>La pagina publica es uno-basada, aunque Spring Data trabaje internamente
 * con indices desde cero.
 *
 * <p>Este record no depende de HTTP, de Spring Data Page ni de ningun dominio
 * de negocio concreto. Puede ser usado por Personas, Consultas y cualquier otro
 * recurso que requiera la misma semantica de paginacion.
 */
public record PageResponseDTO<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public PageResponseDTO {
        content = content == null ? List.of() : List.copyOf(content);
    }
}
