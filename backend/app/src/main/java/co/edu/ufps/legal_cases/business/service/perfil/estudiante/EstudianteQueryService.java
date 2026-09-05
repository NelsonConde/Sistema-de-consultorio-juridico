package co.edu.ufps.legal_cases.business.service.perfil.estudiante;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.dto.perfil.EstudianteDTO;
import co.edu.ufps.legal_cases.business.dto.perfil.EstudianteResumenDTO;
import co.edu.ufps.legal_cases.business.model.perfil.Estudiante;
import co.edu.ufps.legal_cases.business.repository.perfil.EstudianteRepository;
import co.edu.ufps.legal_cases.business.repository.perfil.EstudianteResumenProjection;
import co.edu.ufps.legal_cases.business.service.acceso.perfil.EstudianteAccessService;
import co.edu.ufps.legal_cases.common.dto.PageResponseDTO;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

@Service
public class EstudianteQueryService {

    private static final int PAGINA_MINIMA = 1;
    private static final int TAMANO_MINIMO = 1;
    private static final int TAMANO_MAXIMO = 50;
    private static final int LONGITUD_MAXIMA_BUSQUEDA = 100;

    private static final Map<String, String> CAMPOS_ORDENAMIENTO = Map.ofEntries(
            Map.entry("id", "id"),
            Map.entry("nombre", "nombre"),
            Map.entry("documento", "documento"),
            Map.entry("email", "email"),
            Map.entry("usuario", "usuario"),
            Map.entry("codigo", "codigo"),
            Map.entry("activo", "activo"),
            Map.entry("sedeNombre", "sede.nombre"),
            Map.entry("asesorNombre", "asesor.nombre"),
            Map.entry("conciliacion", "conciliacion"));

    private static final Set<String> CAMPOS_IGNORE_CASE = Set.of(
            "nombre",
            "documento",
            "email",
            "usuario",
            "codigo",
            "sedeNombre",
            "asesorNombre");

    private final EstudianteRepository estudianteRepository;
    private final EstudianteAccessService estudianteAccessService;
    private final EstudianteMapper estudianteMapper;

    public EstudianteQueryService(
            EstudianteRepository estudianteRepository,
            EstudianteAccessService estudianteAccessService,
            EstudianteMapper estudianteMapper) {
        this.estudianteRepository = estudianteRepository;
        this.estudianteAccessService = estudianteAccessService;
        this.estudianteMapper = estudianteMapper;
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<EstudianteResumenDTO> buscar(
            String search,
            int page,
            int size,
            String sortBy,
            String direction,
            Boolean activo) {
        estudianteAccessService.validarPuedeListarEstudiantes();

        validarPaginacion(page, size);
        String termino = normalizarBusqueda(search);
        String campoOrdenamiento = validarCampoOrdenamiento(sortBy);
        Sort.Direction direccion = validarDireccion(direction);

        AlcanceListado alcance = resolverAlcanceListado();
        if (!alcance.puedeConsultar()) {
            return new PageResponseDTO<>(List.of(), page, size, 0, 0);
        }

        Sort sort = construirSort(campoOrdenamiento, direccion);
        PageRequest pageable = PageRequest.of(page - 1, size, sort);

        Page<EstudianteResumenProjection> resultado = estudianteRepository.buscarResumenPaginado(
                termino,
                activo,
                alcance.asesorId(),
                pageable);

        List<EstudianteResumenDTO> contenido = resultado.getContent()
                .stream()
                .map(estudianteMapper::convertirAResumenDTO)
                .toList();

        return new PageResponseDTO<>(
                contenido,
                page,
                size,
                resultado.getTotalElements(),
                resultado.getTotalPages());
    }

    @Transactional(readOnly = true)
    public List<EstudianteDTO> listar() {
        estudianteAccessService.validarPuedeListarEstudiantes();

        if (estudianteAccessService.puedeVerTodosLosEstudiantes()) {
            return estudianteRepository.findAll()
                    .stream()
                    .map(estudianteMapper::convertirADTO)
                    .toList();
        }

        if (estudianteAccessService.usuarioEsAsesor()) {
            return estudianteRepository.findByAsesorIdAndActivoTrue(
                            estudianteAccessService.obtenerAsesorActualId())
                    .stream()
                    .map(estudianteMapper::convertirADTO)
                    .toList();
        }

        return List.of();
    }

    @Transactional(readOnly = true)
    public List<EstudianteDTO> listarActivos() {
        estudianteAccessService.validarPuedeListarEstudiantes();

        if (estudianteAccessService.puedeVerTodosLosEstudiantes()) {
            return estudianteRepository.findByActivoTrueOrderByNombreAsc()
                    .stream()
                    .map(estudianteMapper::convertirADTO)
                    .toList();
        }

        if (estudianteAccessService.usuarioEsAsesor()) {
            return estudianteRepository.findByAsesorIdAndActivoTrue(
                            estudianteAccessService.obtenerAsesorActualId())
                    .stream()
                    .map(estudianteMapper::convertirADTO)
                    .toList();
        }

        return List.of();
    }

    @Transactional(readOnly = true)
    public List<EstudianteDTO> listarConConciliacion() {
        estudianteAccessService.validarPuedeListarEstudiantes();

        return estudianteRepository.findByConciliacionTrueAndActivoTrue()
                .stream()
                .filter(estudianteAccessService::puedeVerEstudiante)
                .map(estudianteMapper::convertirADTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EstudianteDTO> listarPorAsesor(Long asesorId) {
        estudianteAccessService.validarPuedeListarEstudiantesPorAsesor(asesorId);

        return obtenerActivosPorAsesor(asesorId);
    }

    @Transactional(readOnly = true)
    public List<EstudianteDTO> listarActivosPorAsesor(Long asesorId) {
        estudianteAccessService.validarPuedeListarEstudiantesPorAsesor(asesorId);

        return obtenerActivosPorAsesor(asesorId);
    }

    @Transactional(readOnly = true)
    public EstudianteDTO obtenerPorId(Long id) {
        estudianteAccessService.validarPuedeVerEstudiante(id);

        Estudiante estudiante = estudianteRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Estudiante no encontrado con id: " + id));

        return estudianteMapper.convertirADTO(estudiante);
    }

    private AlcanceListado resolverAlcanceListado() {
        if (estudianteAccessService.puedeVerTodosLosEstudiantes()) {
            return new AlcanceListado(true, null);
        }

        if (!estudianteAccessService.usuarioEsAsesor()) {
            return new AlcanceListado(false, null);
        }

        Long asesorId = estudianteAccessService.obtenerAsesorActualId();
        if (asesorId == null) {
            throw new AccessDeniedException("No se pudo resolver el perfil de asesor actual");
        }

        return new AlcanceListado(true, asesorId);
    }

    private List<EstudianteDTO> obtenerActivosPorAsesor(Long asesorId) {
        return estudianteRepository.findByAsesorIdAndActivoTrue(asesorId)
                .stream()
                .map(estudianteMapper::convertirADTO)
                .toList();
    }

    private void validarPaginacion(int page, int size) {
        if (page < PAGINA_MINIMA) {
            throw new BusinessException("La pagina debe ser mayor o igual a 1");
        }

        if (size < TAMANO_MINIMO || size > TAMANO_MAXIMO) {
            throw new BusinessException("El tamano de pagina debe estar entre 1 y 50");
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
            throw new BusinessException("La busqueda no puede superar los 100 caracteres");
        }

        return termino;
    }

    private String validarCampoOrdenamiento(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            throw new BusinessException("El campo de ordenamiento no puede estar vacio");
        }

        String campo = sortBy.trim();
        if (!CAMPOS_ORDENAMIENTO.containsKey(campo)) {
            throw new BusinessException("El campo de ordenamiento '" + campo + "' no es valido");
        }

        return campo;
    }

    private Sort.Direction validarDireccion(String direction) {
        if (direction == null || direction.isBlank()) {
            throw new BusinessException("La direccion de ordenamiento no puede estar vacia");
        }

        return switch (direction.trim().toLowerCase(Locale.ROOT)) {
            case "asc" -> Sort.Direction.ASC;
            case "desc" -> Sort.Direction.DESC;
            default -> throw new BusinessException("La direccion de ordenamiento debe ser 'asc' o 'desc'");
        };
    }

    private Sort construirSort(String campo, Sort.Direction direccion) {
        Sort.Order ordenPrincipal = new Sort.Order(direccion, CAMPOS_ORDENAMIENTO.get(campo));
        if (CAMPOS_IGNORE_CASE.contains(campo)) {
            ordenPrincipal = ordenPrincipal.ignoreCase();
        }

        if ("id".equals(campo)) {
            return Sort.by(ordenPrincipal);
        }

        return Sort.by(ordenPrincipal, Sort.Order.asc("id"));
    }

    private record AlcanceListado(boolean puedeConsultar, Long asesorId) {
    }
}
