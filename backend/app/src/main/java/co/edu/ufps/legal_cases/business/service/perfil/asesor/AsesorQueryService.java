package co.edu.ufps.legal_cases.business.service.perfil.asesor;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.dto.perfil.AsesorDTO;
import co.edu.ufps.legal_cases.business.dto.perfil.AsesorResumenDTO;
import co.edu.ufps.legal_cases.business.model.perfil.Asesor;
import co.edu.ufps.legal_cases.business.repository.perfil.AsesorRepository;
import co.edu.ufps.legal_cases.business.repository.perfil.AsesorResumenProjection;
import co.edu.ufps.legal_cases.business.service.acceso.perfil.AsesorMonitorAccessService;
import co.edu.ufps.legal_cases.common.dto.PageResponseDTO;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

@Service
public class AsesorQueryService {

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
            "areaNombre", "area.nombre");

    private static final Set<String> CAMPOS_IGNORE_CASE = Set.of(
            "nombre",
            "documento",
            "email",
            "usuario",
            "codigo",
            "areaNombre");

    private final AsesorRepository asesorRepository;
    private final AsesorMapper asesorMapper;
    private final AsesorMonitorAccessService asesorMonitorAccessService;

    public AsesorQueryService(
            AsesorRepository asesorRepository,
            AsesorMapper asesorMapper,
            AsesorMonitorAccessService asesorMonitorAccessService) {
        this.asesorRepository = asesorRepository;
        this.asesorMapper = asesorMapper;
        this.asesorMonitorAccessService = asesorMonitorAccessService;
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<AsesorResumenDTO> buscar(
            String search,
            int page,
            int size,
            String sortBy,
            String direction,
            Boolean activo) {
        asesorMonitorAccessService.validarPuedeListarAsesoresYMonitores();

        validarPaginacion(page, size);

        String termino = normalizarBusqueda(search);
        Sort sort = construirSort(sortBy, direction);
        PageRequest pageable = PageRequest.of(page - 1, size, sort);

        Page<AsesorResumenProjection> resultado =
                asesorRepository.buscarResumenPaginado(termino, activo, pageable);

        List<AsesorResumenDTO> contenido = resultado.getContent()
                .stream()
                .map(asesorMapper::convertirAResumenDTO)
                .toList();

        return new PageResponseDTO<>(
                contenido,
                page,
                size,
                resultado.getTotalElements(),
                resultado.getTotalPages());
    }

    @Transactional(readOnly = true)
    public List<AsesorDTO> listarActivos() {
        asesorMonitorAccessService.validarPuedeListarAsesoresYMonitoresActivos();

        return asesorRepository.findByActivoTrue()
                .stream()
                .map(asesorMapper::convertirADTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public AsesorDTO obtenerPorId(Long id) {
        asesorMonitorAccessService.validarPuedeListarAsesoresYMonitores();

        Asesor asesor = buscarPorId(id);

        return asesorMapper.convertirADTO(asesor);
    }

    private Asesor buscarPorId(Long id) {
        if (id == null) {
            throw new BusinessException("El id del asesor es obligatorio");
        }

        return asesorRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Asesor no encontrado con id: " + id));
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
