package co.edu.ufps.legal_cases.business.service.perfil.monitor;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.dto.perfil.MonitorDTO;
import co.edu.ufps.legal_cases.business.dto.perfil.MonitorResumenDTO;
import co.edu.ufps.legal_cases.business.model.perfil.Monitor;
import co.edu.ufps.legal_cases.business.repository.perfil.MonitorRepository;
import co.edu.ufps.legal_cases.business.repository.perfil.MonitorResumenProjection;
import co.edu.ufps.legal_cases.business.service.acceso.perfil.AsesorMonitorAccessService;
import co.edu.ufps.legal_cases.common.dto.PageResponseDTO;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

@Service
public class MonitorQueryService {

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
            "sedeNombre", "sede.nombre");

    private static final Set<String> CAMPOS_IGNORE_CASE = Set.of(
            "nombre",
            "documento",
            "email",
            "usuario",
            "codigo",
            "sedeNombre");

    private final MonitorRepository monitorRepository;
    private final MonitorMapper monitorMapper;
    private final AsesorMonitorAccessService asesorMonitorAccessService;

    public MonitorQueryService(
            MonitorRepository monitorRepository,
            MonitorMapper monitorMapper,
            AsesorMonitorAccessService asesorMonitorAccessService) {
        this.monitorRepository = monitorRepository;
        this.monitorMapper = monitorMapper;
        this.asesorMonitorAccessService = asesorMonitorAccessService;
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<MonitorResumenDTO> buscar(
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

        Page<MonitorResumenProjection> resultado =
                monitorRepository.buscarResumenPaginado(termino, activo, pageable);

        List<MonitorResumenDTO> contenido = resultado.getContent()
                .stream()
                .map(monitorMapper::convertirAResumenDTO)
                .toList();

        return new PageResponseDTO<>(
                contenido,
                page,
                size,
                resultado.getTotalElements(),
                resultado.getTotalPages());
    }

    @Transactional(readOnly = true)
    public List<MonitorDTO> listarActivos() {
        asesorMonitorAccessService.validarPuedeListarAsesoresYMonitoresActivos();

        return monitorRepository.findByActivoTrue()
                .stream()
                .map(monitorMapper::convertirADTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public MonitorDTO obtenerPorId(Long id) {
        asesorMonitorAccessService.validarPuedeListarAsesoresYMonitores();

        Monitor monitor = buscarPorId(id);

        return monitorMapper.convertirADTO(monitor);
    }

    private Monitor buscarPorId(Long id) {
        if (id == null) {
            throw new BusinessException("El id del monitor es obligatorio");
        }

        return monitorRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Monitor no encontrado con id: " + id));
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
