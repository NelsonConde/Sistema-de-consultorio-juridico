package co.edu.ufps.legal_cases.business.service.conciliacion.conciliacion;

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

import co.edu.ufps.legal_cases.business.dto.conciliacion.ConciliacionDetalleResponseDTO;
import co.edu.ufps.legal_cases.business.dto.conciliacion.ConciliacionResponseDTO;
import co.edu.ufps.legal_cases.business.dto.conciliacion.ConciliacionResumenDTO;
import co.edu.ufps.legal_cases.business.model.conciliacion.Conciliacion;
import co.edu.ufps.legal_cases.business.model.conciliacion.EstadoConciliacionCodigo;
import co.edu.ufps.legal_cases.business.model.consulta.Consulta;
import co.edu.ufps.legal_cases.business.model.consulta.EstadoConsulta;
import co.edu.ufps.legal_cases.business.repository.conciliacion.ConciliacionRepository;
import co.edu.ufps.legal_cases.business.repository.conciliacion.ConciliacionResumenProjection;
import co.edu.ufps.legal_cases.business.repository.conciliacion.reunion.ReunionConciliacionRepository;
import co.edu.ufps.legal_cases.business.repository.consulta.ConsultaRepository;
import co.edu.ufps.legal_cases.business.service.acceso.conciliacion.ConciliacionAccessService;
import co.edu.ufps.legal_cases.business.service.acceso.conciliacion.ConciliacionAlcanceService;
import co.edu.ufps.legal_cases.business.service.conciliacion.reunion.ReunionConciliacionMapper;
import co.edu.ufps.legal_cases.common.dto.PageResponseDTO;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.dto.account.PerfilUsuarioActual;
import lombok.AllArgsConstructor;

// Maneja lecturas del módulo de conciliación.
// Aplica permisos/alcance y arma respuestas de listado o detalle.
@Service
@AllArgsConstructor
public class ConciliacionQueryService {

    private static final int PAGINA_MINIMA = 1;
    private static final int TAMANO_MINIMO = 1;
    private static final int TAMANO_MAXIMO = 50;
    private static final int LONGITUD_MAXIMA_BUSQUEDA = 100;
    private static final EstadoConsulta ESTADO_ARCHIVADO = EstadoConsulta.ARCHIVADO;

    private static final Map<String, String> CAMPOS_ORDENAMIENTO = Map.of(
            "id", "id",
            "fechaCreacion", "fechaCreacion",
            "fechaConciliacion", "fechaConciliacion",
            "estado", "estado.codigo"
    );

    private final ConciliacionRepository conciliacionRepository;
    private final ConsultaRepository consultaRepository;
    private final ReunionConciliacionRepository reunionConciliacionRepository;
    private final ConciliacionAccessService conciliacionAccessService;
    private final ConciliacionAlcanceService conciliacionAlcanceService;
    private final ConciliacionMapper conciliacionMapper;
    private final ReunionConciliacionMapper reunionConciliacionMapper;

    @Transactional(readOnly = true)
    public PageResponseDTO<ConciliacionResumenDTO> buscarParaUsuarioActual(
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

        Page<ConciliacionResumenProjection> resultado = conciliacionRepository.buscarResumenPaginado(
                termino,
                estadoCodigo,
                fechaDesdeInicio,
                fechaHastaExclusiva,
                alcanceGlobal,
                tipoPerfil,
                perfilId,
                ESTADO_ARCHIVADO,
                pageable);

        List<ConciliacionResumenDTO> contenido = resultado.getContent()
                .stream()
                .map(conciliacionMapper::convertirAResumen)
                .toList();

        return new PageResponseDTO<>(
                contenido,
                page,
                size,
                resultado.getTotalElements(),
                resultado.getTotalPages());
    }

    @Transactional(readOnly = true)
    public List<ConciliacionResponseDTO> listarPorConsulta(Long consultaId) {
        conciliacionAccessService.validarPuedeListarConciliaciones();

        if (consultaId == null) {
            throw new BusinessException("La consulta es obligatoria");
        }

        return conciliacionRepository
                .findByConsulta_IdAndActivoTrueAndConsulta_EstadoNotOrderByIdDesc(
                        consultaId,
                        EstadoConsulta.ARCHIVADO)
                .stream()
                .filter(conciliacionAlcanceService::puedeVerConciliacion)
                .map(conciliacionMapper::convertirAResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public ConciliacionDetalleResponseDTO obtenerDetalle(Long id) {
        conciliacionAccessService.validarPuedeVerConciliacion(id);

        Conciliacion conciliacion = conciliacionRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new BusinessException("Conciliación no encontrada con id: " + id));

        cargarConsultaConPersonas(conciliacion);

        ConciliacionDetalleResponseDTO detalle = conciliacionMapper.convertirADetalleResponseDTO(conciliacion);

        reunionConciliacionRepository.findByConciliacion_Id(conciliacion.getId())
                .map(reunionConciliacionMapper::convertirAResponseDTO)
                .ifPresent(detalle::setReunion);

        return detalle;
    }

    private void cargarConsultaConPersonas(Conciliacion conciliacion) {
        if (conciliacion.getConsulta() == null || conciliacion.getConsulta().getId() == null) {
            throw new BusinessException("La conciliación no tiene consulta asociada");
        }

        Long consultaId = conciliacion.getConsulta().getId();

        Consulta consulta = consultaRepository.findByIdConPartes(consultaId)
                .orElseThrow(() -> new BusinessException("Consulta no encontrada con id: " + consultaId));

        // Carga contrapartes en la misma transacción.
        // Se hace separado para respetar el patrón actual del backend y evitar fetch
        // simultáneo de dos colecciones.
        consultaRepository.findByIdConContrapartes(consultaId);

        conciliacion.setConsulta(consulta);
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
            throw new BusinessException("La busqueda no puede superar los 100 caracteres");
        }

        return termino;
    }

    private String normalizarEstado(String estado) {
        String estadoCodigo = EstadoConciliacionCodigo.normalizar(estado);
        return estadoCodigo == null || estadoCodigo.isBlank() ? null : estadoCodigo;
    }
}
