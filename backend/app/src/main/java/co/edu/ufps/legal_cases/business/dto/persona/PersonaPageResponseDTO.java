package co.edu.ufps.legal_cases.business.dto.persona;

import java.util.List;

/**
 * Respuesta paginada estable del buscador de personas.
 *
 * La pagina publica es uno-basada, aunque Spring Data trabaje internamente con
 * indices desde cero.
 */
public record PersonaPageResponseDTO(
        List<PersonaResumenDTO> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public PersonaPageResponseDTO {
        content = content == null ? List.of() : List.copyOf(content);
    }
}