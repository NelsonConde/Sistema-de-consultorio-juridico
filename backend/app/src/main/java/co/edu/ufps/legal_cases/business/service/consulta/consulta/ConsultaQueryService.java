package co.edu.ufps.legal_cases.business.service.consulta.consulta;

import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarTexto;

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
import co.edu.ufps.legal_cases.business.dto.consulta.ConsultaBusquedaDTO;
import co.edu.ufps.legal_cases.business.dto.consulta.ConsultaDTO;
import co.edu.ufps.legal_cases.business.model.consulta.Consulta;
import co.edu.ufps.legal_cases.business.model.consulta.EstadoConsulta;
import co.edu.ufps.legal_cases.business.repository.consulta.ConsultaRepository;
import co.edu.ufps.legal_cases.business.repository.consulta.ConsultaResumenProjection;
import co.edu.ufps.legal_cases.business.service.acceso.consulta.ConsultaAccessService;
import co.edu.ufps.legal_cases.common.dto.PageResponseDTO;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.common.exception.ResourceNotFoundException;
import co.edu.ufps.legal_cases.security.dto.account.PerfilUsuarioActual;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;

@Service
public class ConsultaQueryService {

    private static final int PAGINA_MINIMA = 1;
    private static final int TAMANO_MINIMO = 1;
    private static final int TAMANO_MAXIMO = 50;
    private static final int LONGITUD_MAXIMA_BUSQUEDA = 100;

    private static final Map<String, String> CAMPOS_ORDENAMIENTO = Map.of(
            "fecha", "fecha",
            "consulta", "descripcion",
            "nombre", "persona.nombres",
            "apellido", "persona.apellidos",
            "cedula", "persona.numeroDocumento",
            "estado", "estado"
    );

    private static final Set<String> CAMPOS_IGNORE_CASE = Set.of(
            "consulta",
            "nombre",
            "apellido",
            "cedula"
    );

    private final ConsultaRepository consultaRepository;
    private final ConsultaAccessService consultaAccessService;
    private final ConsultaMapper consultaMapper;

    public ConsultaQueryService(
            ConsultaRepository consultaRepository,
            ConsultaAccessService consultaAccessService,
            ConsultaMapper consultaMapper) {
        this.consultaRepository = consultaRepository;
        this.consultaAccessService = consultaAccessService;
        this.consultaMapper = consultaMapper;
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<ConsultaBusquedaDTO> buscarParaUsuarioActual(
            String search,
            int page,
            int size,
            String sortBy,
            String direction,
            Long areaId,
            EstadoConsulta estado,
            Long asesorId,
            Long monitorId,
            Long estudianteId) {
        consultaAccessService.validarPuedeBuscarConsultas();

        validarPaginacion(page, size);
        Sort sort = construirSort(sortBy, direction);
        String termino = normalizarBusqueda(search);
        PageRequest pageable = PageRequest.of(page - 1, size, sort);

        boolean alcanceGlobal = consultaAccessService.usuarioEsAdministrador();
        String tipoPerfil = null;
        Long perfilId = null;

        if (!alcanceGlobal) {
            PerfilUsuarioActual perfil = consultaAccessService.obtenerPerfilActual();
            if (perfil != null) {
                tipoPerfil = perfil.getTipoPerfil() != null ? perfil.getTipoPerfil().name() : null;
                perfilId = perfil.getPerfilId();
            }
        }

        Page<ConsultaResumenProjection> resultado = consultaRepository.buscarResumenPaginado(
                termino,
                areaId,
                estado,
                asesorId,
                monitorId,
                estudianteId,
                alcanceGlobal,
                tipoPerfil,
                perfilId,
                EstadoConsulta.ARCHIVADO,
                pageable);

        List<ConsultaBusquedaDTO> contenido = resultado.getContent()
                .stream()
                .map(consultaMapper::convertirABusquedaDTO)
                .toList();

        return new PageResponseDTO<>(
                contenido,
                page,
                size,
                resultado.getTotalElements(),
                resultado.getTotalPages());
    }

    // Busca consultas por texto libre y devuelve solo las que pertenecen al alcance del usuario actual.
    @Transactional(readOnly = true)
    public List<ConsultaBusquedaDTO> buscarParaUsuarioActual(String search) {
        consultaAccessService.validarPuedeBuscarConsultas();

        String termino = normalizarTexto(search);
        PerfilUsuarioActual perfil = consultaAccessService.obtenerPerfilActual();

        if (consultaAccessService.usuarioEsAdministrador()) {
            return consultaRepository.buscarParaAdministrador(termino)
                    .stream()
                    .map(consultaMapper::convertirABusquedaDTO)
                    .toList();
        }

        if (perfil.getTipoPerfil() == TipoPerfilUsuario.ESTUDIANTE) {
            return consultaRepository.buscarParaEstudiante(termino, perfil.getPerfilId())
                    .stream()
                    .map(consultaMapper::convertirABusquedaDTO)
                    .toList();
        }

        if (perfil.getTipoPerfil() == TipoPerfilUsuario.ASESOR) {
            return consultaRepository.buscarParaAsesor(termino, perfil.getPerfilId())
                    .stream()
                    .map(consultaMapper::convertirABusquedaDTO)
                    .toList();
        }

        if (perfil.getTipoPerfil() == TipoPerfilUsuario.MONITOR) {
            return consultaRepository.buscarParaMonitor(termino, perfil.getPerfilId())
                    .stream()
                    .map(consultaMapper::convertirABusquedaDTO)
                    .toList();
        }

        if (perfil.getTipoPerfil() == TipoPerfilUsuario.CONCILIADOR) {
            // Cuando conciliaciones tenga alcance real, aquí se listarán las consultas asociadas.
            return List.of();
        }

        return List.of();
    }

    // Se conserva temporalmente para compatibilidad interna si alguna clase todavía lo llama.
    @Transactional(readOnly = true)
    public List<ConsultaBusquedaDTO> buscar(String search) {
        return buscarParaUsuarioActual(search);
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

    @Transactional(readOnly = true)
    @Auditable(action = "READ_LEGAL_CASE_LIST", entityName = "Consulta", entityId = "'collection'")
    public List<ConsultaDTO> listar() {
        consultaAccessService.validarPuedeBuscarConsultas();

        return consultaRepository.findAll()
                .stream()
                .filter(consultaAccessService::puedeAccederAConsulta)
                .map(consultaMapper::convertirADTO)
                .toList();
    }

    @Transactional(readOnly = true)
    @Auditable(action = "READ_LEGAL_CASE", entityName = "Consulta", entityId = "#id")
    public ConsultaDTO obtenerPorId(Long id) {
        consultaAccessService.validarPuedeVerConsulta(id);

        Consulta consulta = consultaRepository.findByIdConPartes(id)
                .orElseThrow(() -> new ResourceNotFoundException("Consulta no encontrada"));

        // Esta consulta carga la colección en el contexto para que el mapper tenga contrapartes disponibles.
        consultaRepository.findByIdConContrapartes(id);

        return consultaMapper.convertirADTO(consulta);
    }

    @Transactional(readOnly = true)
    @Auditable(action = "READ_ARCHIVED_LEGAL_CASES", entityName = "Consulta", entityId = "'archived'")
    public List<ConsultaBusquedaDTO> listarArchivadas() {
        consultaAccessService.validarPuedeListarConsultasArchivadas();

        return consultaRepository.findByEstado(EstadoConsulta.ARCHIVADO)
                .stream()
                .map(consultaMapper::convertirABusquedaDTO)
                .toList();
    }
}
