package co.edu.ufps.legal_cases.common.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class PageResponseDTOTest {

    @Test
    void contentNuloDebeProducirListaVacia() {
        PageResponseDTO<String> dto = new PageResponseDTO<>(null, 1, 10, 0, 0);

        assertEquals(List.of(), dto.content());
    }

    @Test
    void contentDebeSerDefensivamenteCopiadoParaEvitarMutacionExterna() {
        List<String> original = new ArrayList<>(List.of("a", "b"));
        PageResponseDTO<String> dto = new PageResponseDTO<>(original, 1, 10, 2, 1);

        // Mutación de la lista original no debe afectar al DTO.
        original.add("c");

        assertEquals(2, dto.content().size());
        assertNotSame(original, dto.content());
    }

    @Test
    void listaInternaNoPuedeSerModificadaDesdeElExterior() {
        PageResponseDTO<String> dto = new PageResponseDTO<>(List.of("x"), 1, 10, 1, 1);

        // List.copyOf devuelve una lista no modificable.
        assertThrows(UnsupportedOperationException.class,
                () -> dto.content().add("y"));
    }
}
