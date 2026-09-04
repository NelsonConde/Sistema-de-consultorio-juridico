package co.edu.ufps.legal_cases.business.service.proceso.proceso;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.dto.proceso.ProcesoDTO;
import co.edu.ufps.legal_cases.business.dto.proceso.ProcesoResumenDTO;
import co.edu.ufps.legal_cases.business.model.consulta.EstadoConsulta;
import co.edu.ufps.legal_cases.business.model.proceso.EstadoProceso;
import co.edu.ufps.legal_cases.business.model.proceso.Proceso;
import co.edu.ufps.legal_cases.business.repository.proceso.ProcesoRepository;
import co.edu.ufps.legal_cases.business.repository.proceso.ProcesoResumenProjection;
import co.edu.ufps.legal_cases.business.service.acceso.proceso.ProcesoAccessService;
import co.edu.ufps.legal_cases.common.dto.PageResponseDTO;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.dto.account.PerfilUsuarioActual;

@Service
public class ProcesoQueryService {

    private static final int PAGINA_MINIMA = 1;
    private static final int TAMANO_MINIMO = 1;
    private static final int TAMANO_MAXIMO = 50;
    private static final int LONGITUD_MAXIMA_BUSQUEDA = 100;
    private static final EstadoConsulta ESTADO_ARCHIVADO = EstadoConsulta.ARCHIVADO;

    private static final Map<String, String> CAMPOS_ORDENAMIENTO = Map.of(
            "id", "id",
            "fechaCreacion", "fechaCreacion",
            "numeroRadicado", "numeroRadicado",
            "estado", "estado"
    );

    private static final Set<String> CAMPOS_IGNORE_CASE = Set.of(
            "numeroRadicado"
    );

    private final ProcesoRepository procesoRepository;
    private final ProcesoAccessService procesoAccessService;
    private final ProcesoMapper procesoMapper;

    public ProcesoQueryService(
            ProcesoRepository procesoRepository,
            ProcesoAccessService procesoAccessService,
            ProcesoMapper procesoMapper) {
        this.procesoRepository = procesoRepository;
        this.procesoAccessService = procesoAccessService;
        this.procesoMapper = procesoMapper;
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<ProcesoResumenDTO> buscarParaUsuarioActual(
            String search,
            int page,
            int size,
            String sortBy,
            String direction,
            EstadoProceso estado,
            LocalDate fechaDesde,
            LocalDate fechaHasta) {
        procesoAccessService.validarPuedeListarProcesos();

        validarPaginacion(page, size);
        validarRangoFechas(fechaDesde, fechaHasta);

        String termino = normalizarBusqueda(search);
        Sort sort = construirSort(sortBy, direction);
        PageRequest pageable = PageRequest.of(page - 1, size, sort);
        LocalDateTime fechaDesdeInicio = fechaDesde != null ? fechaDesde.atStartOfDay() : null;
        LocalDateTime fechaHastaExclusiva = fechaHasta != null ? fechaHasta.plusDays(1).atStartOfDay() : null;

        boolean alcanceGlobal = procesoAccessService.usuarioEsAdministrador();
        String tipoPerfil = null;
        Long perfilId = null;

        if (!alcanceGlobal) {
            PerfilUsuarioActual perfil = procesoAccessService.obtenerPerfilActual();
            if (perfil != null) {
                tipoPerfil = perfil.getTipoPerfil() != null ? perfil.getTipoPerfil().name() : null;
                perfilId = perfil.getPerfilId();
            }
        }

        Page<ProcesoResumenProjection> resultado = procesoRepository.buscarResumenPaginado(
                termino,
                estado,
                fechaDesdeInicio,
                fechaHastaExclusiva,
                alcanceGlobal,
                tipoPerfil,
                perfilId,
                ESTADO_ARCHIVADO,
                pageable);

        List<ProcesoResumenDTO> contenido = resultado.getContent()
                .stream()
                .map(procesoMapper::convertirAResumen)
                .toList();

        return new PageResponseDTO<>(
                contenido,
                page,
                size,
                resultado.getTotalElements(),
                resultado.getTotalPages());
    }

    // Obtiene un proceso activo específico después de validar acceso sobre ese registro.
    // Si el proceso pertenece a una consulta archivada, no se expone por este flujo operativo.
    @Transactional(readOnly = true)
    public ProcesoDTO obtenerPorId(Long id) {
        procesoAccessService.validarPuedeVerProceso(id);

        Proceso proceso = buscarProcesoActivo(id);

        return procesoMapper.convertirADTO(proceso);
    }

    private Proceso buscarProcesoActivo(Long id) {
        if (id == null) {
            throw new BusinessException("El id del proceso es obligatorio");
        }

        return procesoRepository.findByIdAndActivoTrueAndConsulta_EstadoNot(id, ESTADO_ARCHIVADO)
                .orElseThrow(() -> new BusinessException("Proceso no encontrado con id: " + id));
    }

    private void validarPaginacion(int page, int size) {
        if (page < PAGINA_MINIMA) {
            throw new BusinessException("La página debe ser mayor o igual a 1");
        }

        if (size < TAMANO_MINIMO || size > TAMANO_MAXIMO) {
            throw new BusinessException("El tamaño de página debe estar entre 1 y 50");
        }
    }

    private void validarRangoFechas(LocalDate fechaDesde, LocalDate fechaHasta) {
        if (fechaDesde != null && fechaHasta != null && fechaDesde.isAfter(fechaHasta)) {
            throw new BusinessException("La fecha desde no puede ser posterior a la fecha hasta");
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

        String propiedadJpa = CAMPOS_ORDENAMIENTO.get(campoLimpio);
        Sort.Order ordenPrincipal;
        if (CAMPOS_IGNORE_CASE.contains(campoLimpio)) {
            ordenPrincipal = new Sort.Order(sortDirection, propiedadJpa).ignoreCase();
        } else {
            ordenPrincipal = new Sort.Order(sortDirection, propiedadJpa);
        }

        if ("id".equals(propiedadJpa)) {
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
            throw new BusinessException("La búsqueda no puede superar los 100 caracteres");
        }

        return termino;
    }
}
