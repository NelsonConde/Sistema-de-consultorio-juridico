package co.edu.ufps.legal_cases.business.service.persona.persona;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import co.edu.ufps.legal_cases.business.dto.persona.PersonaResumenDTO;
import co.edu.ufps.legal_cases.business.repository.persona.PersonaResumenProjection;

class PersonaResumenMapperTest {

    private PersonaResumenMapper personaResumenMapper;

    @BeforeEach
    void setUp() {
        personaResumenMapper = new PersonaResumenMapper();
    }

    @Test
    void debeEnmascararDocumentoYMapearSoloElResumen() {
        PersonaResumenProjection projection = mock(PersonaResumenProjection.class);
        when(projection.getId()).thenReturn(7L);
        when(projection.getNombres()).thenReturn("Ana Maria");
        when(projection.getApellidos()).thenReturn("Perez Rojas");
        when(projection.getTipoDocumento()).thenReturn("CC");
        when(projection.getNumeroDocumento()).thenReturn("1090123456");
        when(projection.getTipoPersona()).thenReturn("Solicitante");
        when(projection.getActivo()).thenReturn(true);

        PersonaResumenDTO resultado = personaResumenMapper.convertirAResumen(projection);

        assertEquals(7L, resultado.id());
        assertEquals("Ana Maria", resultado.nombres());
        assertEquals("Perez Rojas", resultado.apellidos());
        assertEquals("CC", resultado.tipoDocumento());
        assertEquals("******3456", resultado.numeroDocumentoEnmascarado());
        assertEquals("Solicitante", resultado.tipoPersona());
        assertEquals(true, resultado.activo());
    }

    @Test
    void debeOcultarCompletamenteDocumentosDeCuatroCaracteresOMenos() {
        assertEquals("****", personaResumenMapper.enmascararDocumento("1234"));
        assertEquals("**", personaResumenMapper.enmascararDocumento("12"));
    }

    @Test
    void debeMantenerNuloCuandoNoHayDocumento() {
        assertNull(personaResumenMapper.enmascararDocumento(null));
        assertNull(personaResumenMapper.enmascararDocumento("   "));
    }
}