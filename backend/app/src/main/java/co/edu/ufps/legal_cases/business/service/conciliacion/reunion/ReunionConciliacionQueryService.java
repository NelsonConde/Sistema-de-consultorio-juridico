package co.edu.ufps.legal_cases.business.service.conciliacion.reunion;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.dto.conciliacion.reunion.ReunionConciliacionResumenDTO;
import co.edu.ufps.legal_cases.business.model.conciliacion.EstadoConciliacionCodigo;
import co.edu.ufps.legal_cases.business.model.consulta.EstadoConsulta;
import co.edu.ufps.legal_cases.business.repository.conciliacion.reunion.ReunionConciliacionRepository;
import co.edu.ufps.legal_cases.business.repository.conciliacion.reunion.ReunionConciliacionResumenProjection;
import co.edu.ufps.legal_cases.business.service.acceso.conciliacion.ConciliacionAccessService;
import co.edu.ufps.legal_cases.common.dto.PageResponseDTO;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.dto.account.PerfilUsuarioActual;

@Service
public class ReunionConciliacionQueryService {

    private static final int PAGINA_MINIMA = 1;
    private static final int TAMANO_MINIMO = 1;
    private static final int TAMANO_MAXIMO = 50;
    private static final int LONGITUD_MAXIMA_BUSQUEDA = 100;
    private static final EstadoConsulta ESTADO_ARCHIVADO = EstadoConsulta.ARCHIVADO;

    private static final Map<String, String> CAMPOS_ORDENAMIENTO = Map.of(
            "id", "conciliacionId",
            "fechaReunion", "fechaReunion",
            "fechaCreacion", "fechaCreacion",
            "estado", "conciliacion.estado.codigo",
            "sede", "sede.nombre"
    );

    private final ReunionConciliacionRepository reunionConciliacionRepository;
    private final ConciliacionAccessService conciliacionAccessService;
    private final ReunionConciliacionMapper reunionConciliacionMapper;

    public ReunionConciliacionQueryService(
            ReunionConciliacionRepository reunionConciliacionRepository,
            ConciliacionAccessService conciliacionAccessService,
            ReunionConciliacionMapper reunionConciliacionMapper) {
        this.reunionConciliacionRepository = reunionConciliacionRepository;
        this.conciliacionAccessService = conciliacionAccessService;
        this.reunionConciliacionMapper = reunionConciliacionMapper;
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<ReunionConciliacionResumenDTO> buscarParaUsuarioActual(
            String search,
            int page,
            int size,
            String sortBy,
            String direction,
            String estado,
            LocalDate fechaDesde,
            LocalDate fechaHasta) {
        conciliacionAccessService.validarPuedeListarConciliaciones();

        validarPaginacion(page, size);
        validarRangoFechas(fechaDesde, fechaHasta);

        String termino = normalizarBusqueda(search);
        String estadoCodigo = normalizarEstado(estado);
        Sort sort = construirSort(sortBy, direction);
        PageRequest pageable = PageRequest.of(page - 1, size, sort);
        LocalDateTime fechaDesdeInicio = fechaDesde != null ? fechaDesde.atStartOfDay() : null;
        LocalDateTime fechaHastaExclusiva = fechaHasta != null ? fechaHasta.plusDays(1).atStartOfDay() : null;

        boolean alcanceGlobal = conciliacionAccessService.usuarioEsAdministrador();
        String tipoPerfil = null;
        Long perfilId = null;

        if (!alcanceGlobal) {
            PerfilUsuarioActual perfil = conciliacionAccessService.obtenerPerfilActual();
            if (perfil != null) {
                tipoPerfil = perfil.getTipoPerfil() != null ? perfil.getTipoPerfil().name() : null;
                perfilId = perfil.getPerfilId();
            }
        }

        Page<ReunionConciliacionResumenProjection> resultado =
                reunionConciliacionRepository.buscarResumenPaginado(
                        termino,
                        estadoCodigo,
                        fechaDesdeInicio,
                        fechaHastaExclusiva,
                        alcanceGlobal,
                        tipoPerfil,
                        perfilId,
                        ESTADO_ARCHIVADO,
                        pageable);

        List<ReunionConciliacionResumenDTO> contenido = resultado.getContent()
                .stream()
                .map(reunionConciliacionMapper::convertirAResumen)
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
            throw new BusinessException("La pagina debe ser mayor o igual a 1");
        }

        if (size < TAMANO_MINIMO || size > TAMANO_MAXIMO) {
            throw new BusinessException("El tamano de pagina debe estar entre 1 y 50");
        }
    }

    private void validarRangoFechas(LocalDate fechaDesde, LocalDate fechaHasta) {
        if (fechaDesde != null && fechaHasta != null && fechaDesde.isAfter(fechaHasta)) {
            throw new BusinessException("La fecha desde no puede ser posterior a la fecha hasta");
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

        String propiedadJpa = CAMPOS_ORDENAMIENTO.get(campoLimpio);
        Sort.Order ordenPrincipal = new Sort.Order(sortDirection, propiedadJpa);

        if ("conciliacionId".equals(propiedadJpa)) {
            return Sort.by(ordenPrincipal);
        }

        return Sort.by(ordenPrincipal, Sort.Order.asc("conciliacionId"));
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

    private String normalizarEstado(String estado) {
        String estadoCodigo = EstadoConciliacionCodigo.normalizar(estado);
        return estadoCodigo == null || estadoCodigo.isBlank() ? null : estadoCodigo;
    }
}
