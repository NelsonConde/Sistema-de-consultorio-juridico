package co.edu.ufps.legal_cases.business.service.persona.persona;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.dto.persona.PersonaDTO;
import co.edu.ufps.legal_cases.business.model.persona.Persona;
import co.edu.ufps.legal_cases.business.repository.persona.PersonaRepository;
import co.edu.ufps.legal_cases.business.service.acceso.persona.PersonaAccessService;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

@Service
public class PersonaQueryService {

    private final PersonaRepository personaRepository;
    private final PersonaAccessService personaAccessService;
    private final PersonaMapper personaMapper;

    public PersonaQueryService(
            PersonaRepository personaRepository,
            PersonaAccessService personaAccessService,
            PersonaMapper personaMapper) {
        this.personaRepository = personaRepository;
        this.personaAccessService = personaAccessService;
        this.personaMapper = personaMapper;
    }

    @Transactional(readOnly = true)
    public List<PersonaDTO> listar() {
        personaAccessService.validarPuedeVerPersonas();

        return personaRepository.findAll()
                .stream()
                .map(personaMapper::convertirADTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PersonaDTO> listarActivos() {
        return personaRepository.findByActivoTrueOrderByNombresAscApellidosAsc()
                .stream()
                .map(personaMapper::convertirADTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public PersonaDTO obtenerPorId(Long id) {
        personaAccessService.validarPuedeVerPersonas();

        Persona persona = buscarPorId(id);

        return personaMapper.convertirADTO(persona);
    }

    private Persona buscarPorId(Long id) {
        if (id == null) {
            throw new BusinessException("El id de la persona es obligatorio");
        }

        return personaRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Persona no encontrada con id: " + id));
    }
}