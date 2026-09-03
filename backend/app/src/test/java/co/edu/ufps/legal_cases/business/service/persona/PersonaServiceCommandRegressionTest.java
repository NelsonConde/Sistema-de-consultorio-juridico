package co.edu.ufps.legal_cases.business.service.persona;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import co.edu.ufps.legal_cases.business.dto.persona.PersonaDTO;
import co.edu.ufps.legal_cases.business.service.persona.persona.PersonaCommandService;
import co.edu.ufps.legal_cases.business.service.persona.persona.PersonaQueryService;

class PersonaServiceCommandRegressionTest {

    private PersonaQueryService personaQueryService;
    private PersonaCommandService personaCommandService;
    private PersonaService personaService;

    @BeforeEach
    void setUp() {
        personaQueryService = mock(PersonaQueryService.class);
        personaCommandService = mock(PersonaCommandService.class);
        personaService = new PersonaService(personaQueryService, personaCommandService);
    }

    @Test
    void crearDebeConservarDelegacionAlCommandService() {
        PersonaDTO entrada = new PersonaDTO();
        PersonaDTO salida = PersonaDTO.builder().id(1L).build();
        when(personaCommandService.crear(entrada)).thenReturn(salida);

        assertSame(salida, personaService.crear(entrada));
    }

    @Test
    void actualizarDebeConservarDelegacionAlCommandService() {
        PersonaDTO entrada = new PersonaDTO();
        PersonaDTO salida = PersonaDTO.builder().id(2L).build();
        when(personaCommandService.actualizar(2L, entrada)).thenReturn(salida);

        assertSame(salida, personaService.actualizar(2L, entrada));
    }

    @Test
    void desactivarYReactivarDebenConservarDelegacionAlCommandService() {
        personaService.desactivar(3L, 11L);
        personaService.reactivar(3L, 12L);

        verify(personaCommandService).desactivar(3L, 11L);
        verify(personaCommandService).reactivar(3L, 12L);
    }
}
