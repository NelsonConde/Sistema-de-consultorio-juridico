package co.edu.ufps.legal_cases.business.service.persona;

import java.util.List;

import org.springframework.stereotype.Service;

import co.edu.ufps.legal_cases.business.dto.persona.PersonaDTO;
import co.edu.ufps.legal_cases.business.service.persona.persona.PersonaCommandService;
import co.edu.ufps.legal_cases.business.service.persona.persona.PersonaQueryService;

@Service
public class PersonaService {

    private final PersonaQueryService personaQueryService;
    private final PersonaCommandService personaCommandService;

    public PersonaService(
            PersonaQueryService personaQueryService,
            PersonaCommandService personaCommandService) {
        this.personaQueryService = personaQueryService;
        this.personaCommandService = personaCommandService;
    }

    // Fachada del módulo: el controller sigue entrando por aquí,
    // pero la lectura y escritura quedan separadas por responsabilidad.
    public List<PersonaDTO> listar() {
        return personaQueryService.listar();
    }

    public List<PersonaDTO> listarActivos() {
        return personaQueryService.listarActivos();
    }

    public PersonaDTO obtenerPorId(Long id) {
        return personaQueryService.obtenerPorId(id);
    }

    public PersonaDTO crear(PersonaDTO personaDTO) {
        return personaCommandService.crear(personaDTO);
    }

    public PersonaDTO actualizar(Long id, PersonaDTO personaDTO) {
        return personaCommandService.actualizar(id, personaDTO);
    }

    public void desactivar(Long id) {
        personaCommandService.desactivar(id);
    }

    public void reactivar(Long id) {
        personaCommandService.reactivar(id);
    }
}