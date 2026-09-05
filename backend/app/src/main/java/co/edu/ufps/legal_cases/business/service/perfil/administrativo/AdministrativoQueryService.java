package co.edu.ufps.legal_cases.business.service.perfil.administrativo;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.dto.perfil.AdministrativoDTO;
import co.edu.ufps.legal_cases.business.dto.perfil.AdministrativoResumenDTO;
import co.edu.ufps.legal_cases.business.model.perfil.Administrativo;
import co.edu.ufps.legal_cases.business.repository.perfil.AdministrativoRepository;
import co.edu.ufps.legal_cases.business.repository.perfil.AdministrativoResumenProjection;
import co.edu.ufps.legal_cases.business.service.acceso.perfil.AdministrativoAccessService;
import co.edu.ufps.legal_cases.common.dto.PageResponseDTO;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

@Service
public class AdministrativoQueryService {

    private static final int PAGINA_MINIMA = 1;
    private static final int TAMANO_MINIMO = 1;
    private static final int TAMANO_MAXIMO = 50;
    private static final int LONGITUD_MAXIMA_BUSQUEDA = 100;

    private static final Map<String, String> CAMPOS_ORDENAMIENTO = Map.of(
            "id", "id",
            "nombre", "nombre",
            "documento", "documento",
            "email", "email",
            "usuario", "usuario",
            "codigo", "codigo",
            "activo", "activo",
            "directora", "directora");

    private static final Set<String> CAMPOS_IGNORE_CASE = Set.of(
            "nombre",
            "documento",
            "email",
            "usuario",
            "codigo");

    private final AdministrativoRepository administrativoRepository;
    private final AdministrativoAccessService administrativoAccessService;
    private final AdministrativoMapper administrativoMapper;

    public AdministrativoQueryService(
            AdministrativoRepository administrativoRepository,
            AdministrativoAccessService administrativoAccessService,
            AdministrativoMapper administrativoMapper) {
        this.administrativoRepository = administrativoRepository;
        this.administrativoAccessService = administrativoAccessService;
        this.administrativoMapper = administrativoMapper;
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<AdministrativoResumenDTO> buscar(
            String search,
            int page,
            int size,
            String sortBy,
            String direction,
            Boolean activo) {
        administrativoAccessService.validarPuedeVerAdministradores();

        validarPaginacion(page, size);

        String termino = normalizarBusqueda(search);
        Sort sort = construirSort(sortBy, direction);
        PageRequest pageable = PageRequest.of(page - 1, size, sort);

        Page<AdministrativoResumenProjection> resultado =
                administrativoRepository.buscarResumenPaginado(termino, activo, pageable);

        List<AdministrativoResumenDTO> contenido = resultado.getContent()
                .stream()
                .map(administrativoMapper::convertirAResumenDTO)
                .toList();

        return new PageResponseDTO<>(
                contenido,
                page,
                size,
                resultado.getTotalElements(),
                resultado.getTotalPages());
    }

    @Transactional(readOnly = true)
    public List<AdministrativoDTO> listarActivos() {
        administrativoAccessService.validarPuedeVerAdministradores();

        return administrativoRepository.findByActivoTrue()
                .stream()
                .map(administrativoMapper::convertirADTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdministrativoDTO> listarDirectoras() {
        administrativoAccessService.validarPuedeVerAdministradores();

        return administrativoRepository.findByDirectoraTrueAndActivoTrue()
                .stream()
                .map(administrativoMapper::convertirADTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdministrativoDTO obtenerPorId(Long id) {
        administrativoAccessService.validarPuedeVerAdministradores();

        Administrativo administrativo = buscarPorId(id);

        return administrativoMapper.convertirADTO(administrativo);
    }

    private Administrativo buscarPorId(Long id) {
        if (id == null) {
            throw new BusinessException("El id del administrativo es obligatorio");
        }

        return administrativoRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Administrativo no encontrado con id: " + id));
    }

    private void validarPaginacion(int page, int size) {
        if (page < PAGINA_MINIMA) {
            throw new BusinessException("La pagina debe ser mayor o igual a 1");
        }

        if (size < TAMANO_MINIMO || size > TAMANO_MAXIMO) {
            throw new BusinessException("El tamano de pagina debe estar entre 1 y 50");
        }
    }

    private Sort construirSort(String sortBy, String direction) {
        if (sortBy == null || sortBy.isBlank()) {
            throw new BusinessException("El campo de ordenamiento no puede estar vacio");
        }

        String campoLimpio = sortBy.trim();
        if (!CAMPOS_ORDENAMIENTO.containsKey(campoLimpio)) {
            throw new BusinessException("El campo de ordenamiento '" + campoLimpio + "' no es valido");
        }

        if (direction == null || direction.isBlank()) {
            throw new BusinessException("La direccion de ordenamiento no puede estar vacia");
        }

        String direccionLimpia = direction.trim().toLowerCase(Locale.ROOT);
        Sort.Direction sortDirection;
        if ("asc".equals(direccionLimpia)) {
            sortDirection = Sort.Direction.ASC;
        } else if ("desc".equals(direccionLimpia)) {
            sortDirection = Sort.Direction.DESC;
        } else {
            throw new BusinessException("La direccion de ordenamiento debe ser 'asc' o 'desc'");
        }

        String propiedadJPA = CAMPOS_ORDENAMIENTO.get(campoLimpio);
        Sort.Order ordenPrincipal;
        if (CAMPOS_IGNORE_CASE.contains(campoLimpio)) {
            ordenPrincipal = new Sort.Order(sortDirection, propiedadJPA).ignoreCase();
        } else {
            ordenPrincipal = new Sort.Order(sortDirection, propiedadJPA);
        }

        if ("id".equals(campoLimpio)) {
            return Sort.by(ordenPrincipal);
        }

        return Sort.by(ordenPrincipal, Sort.Order.asc("id"));
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
}
