package co.edu.ufps.legal_cases.business.service.persona.persona;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.audit.aop.log.Auditable;
import co.edu.ufps.legal_cases.business.model.consulta.EstadoConsulta;
import co.edu.ufps.legal_cases.common.dto.PageResponseDTO;
import co.edu.ufps.legal_cases.business.dto.persona.PersonaDTO;
import co.edu.ufps.legal_cases.business.dto.persona.PersonaResumenDTO;
import co.edu.ufps.legal_cases.business.model.persona.Persona;
import co.edu.ufps.legal_cases.business.repository.persona.PersonaRepository;
import co.edu.ufps.legal_cases.business.repository.persona.PersonaResumenProjection;
import co.edu.ufps.legal_cases.business.service.acceso.persona.AlcanceLecturaPersonas;
import co.edu.ufps.legal_cases.business.service.acceso.persona.PersonaAccessService;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.common.exception.ResourceNotFoundException;

@Service
public class PersonaQueryService {

    private static final int PAGINA_MINIMA = 1;
    private static final int TAMANO_MINIMO = 1;
    private static final int TAMANO_MAXIMO = 50;
    private static final int LONGITUD_MAXIMA_BUSQUEDA = 100;

    private static final Map<String, String> CAMPOS_ORDENAMIENTO = Map.of(
            "nombres", "nombres",
            "apellidos", "apellidos",
            "numeroDocumento", "numeroDocumento",
            "tipoDocumento", "tipoDocumento",
            "tipoPersona", "tipoPersona.nombre",
            "activo", "activo"
    );

    private static final Set<String> CAMPOS_IGNORE_CASE = Set.of(
            "nombres",
            "apellidos",
            "numeroDocumento"
    );

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
    public PageResponseDTO<PersonaResumenDTO> listar(String search, int page, int size, String sortBy, String direction) {
        return buscarResumen(search, page, size, sortBy, direction, null);
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<PersonaResumenDTO> listarActivos(String search, int page, int size, String sortBy, String direction) {
        return buscarResumen(search, page, size, sortBy, direction, true);
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
            String sortBy,
            String direction,
            Boolean activo) {
        AlcanceLecturaPersonas alcance = personaAccessService.obtenerAlcanceLecturaPersonas();
        validarPaginacion(page, size);
        Sort sort = construirSort(sortBy, direction);

        String termino = normalizarBusqueda(search);
        PageRequest pageable = PageRequest.of(page - 1, size, sort);

        String tipoPerfil = alcance.tipoPerfil() != null ? alcance.tipoPerfil().name() : null;

        Page<PersonaResumenProjection> resultado = personaRepository.buscarResumen(
                termino,
                activo,
                alcance.esGlobal(),
                tipoPerfil,
                alcance.perfilId(),
                EstadoConsulta.ARCHIVADO,
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

    private Sort construirSort(String sortBy, String direction) {
        if (sortBy == null || sortBy.isBlank()) {
            throw new BusinessException("El campo de ordenamiento no puede estar vacío");
        }

        String campoLimpio = sortBy.trim();
        if (!CAMPOS_ORDENAMIENTO.containsKey(campoLimpio)) {
            throw new BusinessException("El campo de ordenamiento '" + campoLimpio + "' no es válido");
        }

        if (direction == null || direction.isBlank()) {
            throw new BusinessException("La dirección de ordenamiento no puede estar vacía");
        }

        String direccionLimpia = direction.trim().toLowerCase(Locale.ROOT);
        Sort.Direction sortDirection;
        if ("asc".equals(direccionLimpia)) {
            sortDirection = Sort.Direction.ASC;
        } else if ("desc".equals(direccionLimpia)) {
            sortDirection = Sort.Direction.DESC;
        } else {
            throw new BusinessException("La dirección de ordenamiento debe ser 'asc' o 'desc'");
        }

        String propiedadJPA = CAMPOS_ORDENAMIENTO.get(campoLimpio);
        Sort.Order ordenPrincipal;
        if (CAMPOS_IGNORE_CASE.contains(campoLimpio)) {
            ordenPrincipal = new Sort.Order(sortDirection, propiedadJPA).ignoreCase();
        } else {
            ordenPrincipal = new Sort.Order(sortDirection, propiedadJPA);
        }

        Sort.Order desempateId = Sort.Order.asc("id");

        return Sort.by(ordenPrincipal, desempateId);
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
