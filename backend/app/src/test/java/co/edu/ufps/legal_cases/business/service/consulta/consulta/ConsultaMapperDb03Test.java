package co.edu.ufps.legal_cases.business.service.consulta.consulta;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import co.edu.ufps.legal_cases.business.dto.consulta.ConsultaBusquedaDTO;
import co.edu.ufps.legal_cases.business.model.consulta.Consulta;
import co.edu.ufps.legal_cases.business.model.persona.Persona;

class ConsultaMapperDb03Test {

    @Test
    void busquedaExponeVersionParaAccionesOptimistasDesdeListados() {
        Consulta consulta = new Consulta();
        consulta.setId(10L);
        consulta.setVersion(7L);
        consulta.setDescripcion("Consulta DB-03");

        Persona persona = new Persona();
        persona.setNombres("Ana");
        persona.setApellidos("Perez");
        persona.setNumeroDocumento("123");
        consulta.setPersona(persona);

        ConsultaBusquedaDTO dto =
                new ConsultaMapper().convertirABusquedaDTO(consulta);

        assertEquals(10L, dto.getId());
        assertEquals(7L, dto.getVersion());
        assertEquals("Consulta DB-03", dto.getConsulta());
    }
}
