package co.edu.ufps.legal_cases.business.service.seguimiento.respuesta;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

import co.edu.ufps.legal_cases.business.dto.seguimiento.respuesta.SeguimientoRespuestaResponseDTO;
import co.edu.ufps.legal_cases.business.model.consulta.EstadoConsulta;
import co.edu.ufps.legal_cases.business.model.seguimiento.respuesta.EstadoRespuestaSeguimiento;
import co.edu.ufps.legal_cases.business.model.seguimiento.respuesta.SeguimientoRespuesta;
import co.edu.ufps.legal_cases.business.repository.seguimiento.respuesta.SeguimientoRespuestaPendienteProjection;
import co.edu.ufps.legal_cases.business.repository.seguimiento.respuesta.SeguimientoRespuestaRepository;
import co.edu.ufps.legal_cases.business.service.acceso.seguimiento.SeguimientoRespuestaAccessService;
import co.edu.ufps.legal_cases.common.dto.PageResponseDTO;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.dto.account.PerfilUsuarioActual;

@Service
public class SeguimientoRespuestaQueryService {

    private static final EstadoConsulta ESTADO_ARCHIVADO = EstadoConsulta.ARCHIVADO;
    private static final int PAGINA_MINIMA = 1;
    private static final int TAMANO_MINIMO = 1;
    private static final int TAMANO_MAXIMO = 50;
    private static final int LONGITUD_MAXIMA_BUSQUEDA = 100;

    private static final Map<String, String> CAMPOS_ORDENAMIENTO = Map.of(
            "id", "id",
            "fechaCreacion", "fechaCreacion",
            "fechaActualizacion", "fechaActualizacion",
            "estudianteNombre", "estudiante.nombre");

    private static final Set<String> CAMPOS_IGNORE_CASE = Set.of("estudianteNombre");

    private final SeguimientoRespuestaRepository seguimientoRespuestaRepository;
    private final SeguimientoRespuestaAccessService seguimientoRespuestaAccessService;
    private final SeguimientoRespuestaMapper seguimientoRespuestaMapper;

    public SeguimientoRespuestaQueryService(
            SeguimientoRespuestaRepository seguimientoRespuestaRepository,
            SeguimientoRespuestaAccessService seguimientoRespuestaAccessService,
            SeguimientoRespuestaMapper seguimientoRespuestaMapper) {
        this.seguimientoRespuestaRepository = seguimientoRespuestaRepository;
        this.seguimientoRespuestaAccessService = seguimientoRespuestaAccessService;
        this.seguimientoRespuestaMapper = seguimientoRespuestaMapper;
    }

    @Transactional(readOnly = true)
    public SeguimientoRespuestaResponseDTO obtenerPorId(Long id) {
        SeguimientoRespuesta respuesta = buscarRespuestaActiva(id);

        if (!seguimientoRespuestaAccessService.puedeVerRespuesta(respuesta)) {
            throw new AccessDeniedException("No tiene permisos para ver esta respuesta");
        }

        return seguimientoRespuestaMapper.convertirAResponseDTO(respuesta);
    }

    @Transactional(readOnly = true)
    public List<SeguimientoRespuestaResponseDTO> listarPorSeguimiento(Long seguimientoId) {
        seguimientoRespuestaAccessService.validarPuedeListarRespuestasDeSeguimiento(seguimientoId);

        return seguimientoRespuestaRepository
                .findBySeguimiento_IdAndActivoTrueAndSeguimiento_ActivoTrueAndSeguimiento_Consulta_EstadoNotOrderByFechaCreacionDesc(
                        seguimientoId,
                        ESTADO_ARCHIVADO)
                .stream()
                .filter(seguimientoRespuestaAccessService::puedeVerRespuesta)
                .map(seguimientoRespuestaMapper::convertirAResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<SeguimientoRespuestaResponseDTO> listarPendientes(
            String search,
            int page,
            int size,
            String sortBy,
            String direction,
            LocalDate fechaDesde,
            LocalDate fechaHasta) {
        seguimientoRespuestaAccessService.validarPuedeListarRespuestasPendientes();

        validarPaginacion(page, size);
        validarRangoFechas(fechaDesde, fechaHasta);

        String termino = normalizarBusqueda(search);
        Sort sort = construirSort(sortBy, direction);
        PageRequest pageable = PageRequest.of(page - 1, size, sort);
        LocalDateTime fechaDesdeInicio = fechaDesde != null ? fechaDesde.atStartOfDay() : null;
        LocalDateTime fechaHastaExclusiva = fechaHasta != null ? fechaHasta.plusDays(1).atStartOfDay() : null;

        boolean alcanceGlobal = seguimientoRespuestaAccessService.usuarioEsAdministrador();
        String tipoPerfil = null;
        Long perfilId = null;

        if (!alcanceGlobal) {
            PerfilUsuarioActual perfil = seguimientoRespuestaAccessService.obtenerPerfilActual();
            if (perfil != null) {
                tipoPerfil = perfil.getTipoPerfil() != null ? perfil.getTipoPerfil().name() : null;
                perfilId = perfil.getPerfilId();
            }
        }

        Page<SeguimientoRespuestaPendienteProjection> resultado =
                seguimientoRespuestaRepository.buscarPendientesPaginado(
                        termino,
                        EstadoRespuestaSeguimiento.PENDIENTE,
                        fechaDesdeInicio,
                        fechaHastaExclusiva,
                        alcanceGlobal,
                        tipoPerfil,
                        perfilId,
                        ESTADO_ARCHIVADO,
                        pageable);

        List<SeguimientoRespuestaResponseDTO> contenido = resultado.getContent()
                .stream()
                .map(seguimientoRespuestaMapper::convertirAResponseDTO)
                .toList();

        return new PageResponseDTO<>(
                contenido,
                page,
                size,
                resultado.getTotalElements(),
                resultado.getTotalPages());
    }

    private SeguimientoRespuesta buscarRespuestaActiva(Long id) {
        if (id == null) {
            throw new BusinessException("El id de la respuesta es obligatorio");
        }

        return seguimientoRespuestaRepository
                .findByIdAndActivoTrueAndSeguimiento_ActivoTrueAndSeguimiento_Consulta_EstadoNot(
                        id,
                        ESTADO_ARCHIVADO)
                .orElseThrow(() -> new BusinessException("Respuesta de seguimiento no encontrada con id: " + id));
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
