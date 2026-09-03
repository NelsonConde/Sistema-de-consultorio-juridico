package co.edu.ufps.legal_cases.business.service.persona.persona;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.audit.aop.log.Auditable;
import co.edu.ufps.legal_cases.common.dto.PageResponseDTO;
import co.edu.ufps.legal_cases.business.dto.persona.PersonaDTO;
import co.edu.ufps.legal_cases.business.dto.persona.PersonaResumenDTO;
import co.edu.ufps.legal_cases.business.model.persona.Persona;
import co.edu.ufps.legal_cases.business.repository.persona.PersonaRepository;
import co.edu.ufps.legal_cases.business.repository.persona.PersonaResumenProjection;
import co.edu.ufps.legal_cases.business.service.acceso.persona.PersonaAccessService;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.common.exception.ResourceNotFoundException;

@Service
public class PersonaQueryService {

    private static final int PAGINA_MINIMA = 1;
    private static final int TAMANO_MINIMO = 1;
    private static final int TAMANO_MAXIMO = 50;
    private static final int LONGITUD_MAXIMA_BUSQUEDA = 100;

    private final PersonaRepository personaRepository;
    private final PersonaAccessService personaAccessService;
    private final PersonaMapper personaMapper;
    private final PersonaResumenMapper personaResumenMapper;

    public PersonaQueryService(
            PersonaRepository personaRepository,
            PersonaAccessService personaAccessService,
            PersonaMapper personaMapper,
            PersonaResumenMapper personaResumenMapper) {
        this.personaRepository = personaRepository;
        this.personaAccessService = personaAccessService;
        this.personaMapper = personaMapper;
        this.personaResumenMapper = personaResumenMapper;
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<PersonaResumenDTO> listar(String search, int page, int size) {
        return buscarResumen(search, page, size, null);
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<PersonaResumenDTO> listarActivos(String search, int page, int size) {
        return buscarResumen(search, page, size, true);
    }

    @Transactional(readOnly = true)
    @Auditable(action = "CONSULTAR_DETALLE_PERSONA", entityName = "Persona", entityId = "#id")
    public PersonaDTO obtenerPorId(Long id) {
        personaAccessService.validarPuedeVerDetallePersona(id);

        Persona persona = buscarPorId(id);

        return personaMapper.convertirADTO(persona);
    }

    private PageResponseDTO<PersonaResumenDTO> buscarResumen(
            String search,
            int page,
            int size,
            Boolean activo) {
        personaAccessService.validarPuedeBuscarPersonas();
        validarPaginacion(page, size);

        String termino = normalizarBusqueda(search);
        PageRequest pageable = PageRequest.of(page - 1, size);
        Page<PersonaResumenProjection> resultado = personaRepository.buscarResumen(
                termino,
                activo,
                pageable);

        List<PersonaResumenDTO> contenido = resultado.getContent()
                .stream()
                .map(personaResumenMapper::convertirAResumen)
                .toList();

        return new PageResponseDTO<>(
                contenido,
                page,
                size,
                resultado.getTotalElements(),
                resultado.getTotalPages());
    }

    private void validarPaginacion(int page, int size) {
        if (page < PAGINA_MINIMA) {
            throw new BusinessException("La página debe ser mayor o igual a 1");
        }

        if (size < TAMANO_MINIMO || size > TAMANO_MAXIMO) {
            throw new BusinessException("El tamaño de página debe estar entre 1 y 50");
        }
    }

    private String normalizarBusqueda(String search) {
        if (search == null) {
            return null;
        }

        String termino = search.trim().replaceAll("\\s+", " ");

        if (termino.isEmpty()) {
            return null;
        }

        if (termino.length() > LONGITUD_MAXIMA_BUSQUEDA) {
            throw new BusinessException("La búsqueda no puede superar los 100 caracteres");
        }

        return termino;
    }

    private Persona buscarPorId(Long id) {
        if (id == null) {
            throw new ResourceNotFoundException("Persona no encontrada");
        }

        return personaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Persona no encontrada"));
    }
}
