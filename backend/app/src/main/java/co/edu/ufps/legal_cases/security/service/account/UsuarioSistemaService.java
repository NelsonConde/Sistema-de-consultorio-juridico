package co.edu.ufps.legal_cases.security.service.account;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.common.dto.PageResponseDTO;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.dto.account.UsuarioSistemaResumenDTO;
import co.edu.ufps.legal_cases.security.dto.account.UsuarioSistemaDTO;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.repository.account.UsuarioSistemaRepository;
import co.edu.ufps.legal_cases.security.repository.account.UsuarioSistemaResumenProjection;
import co.edu.ufps.legal_cases.security.service.account.usuario.UsuarioSistemaMapper;
import co.edu.ufps.legal_cases.security.service.account.usuario.UsuarioSistemaValidator;
import co.edu.ufps.legal_cases.security.service.invariant.administracion.AdministracionInvariantService;

@Service
public class UsuarioSistemaService {

    private static final int PAGINA_MINIMA = 1;
    private static final int TAMANO_MINIMO = 1;
    private static final int TAMANO_MAXIMO = 50;
    private static final int LONGITUD_MAXIMA_BUSQUEDA = 100;

    private static final Map<String, String> CAMPOS_ORDENAMIENTO = Map.of(
            "id", "id",
            "username", "username",
            "activo", "activo",
            "rolNombre", "rol.nombre",
            "tipoPerfil", "tipoPerfilActual");

    private static final Set<String> CAMPOS_IGNORE_CASE = Set.of(
            "username",
            "rolNombre");

    private final UsuarioSistemaRepository usuarioSistemaRepository;
    private final UsuarioSistemaMapper usuarioSistemaMapper;
    private final UsuarioSistemaValidator usuarioSistemaValidator;
    private final AdministracionInvariantService administracionInvariantService;

    public UsuarioSistemaService(
            UsuarioSistemaRepository usuarioSistemaRepository,
            UsuarioSistemaMapper usuarioSistemaMapper,
            UsuarioSistemaValidator usuarioSistemaValidator,
            AdministracionInvariantService administracionInvariantService) {

        this.usuarioSistemaRepository = usuarioSistemaRepository;
        this.usuarioSistemaMapper = usuarioSistemaMapper;
        this.usuarioSistemaValidator = usuarioSistemaValidator;
        this.administracionInvariantService =
                administracionInvariantService;
    }

    @Transactional(readOnly = true, noRollbackFor = BusinessException.class)
    public PageResponseDTO<UsuarioSistemaResumenDTO> buscar(
            String search,
            int page,
            int size,
            String sortBy,
            String direction,
            Boolean activo,
            TipoPerfilUsuario tipoPerfil) {
        validarPaginacion(page, size);

        String termino = normalizarBusqueda(search);
        Sort sort = construirSort(sortBy, direction);
        PageRequest pageable = PageRequest.of(page - 1, size, sort);

        Page<UsuarioSistemaResumenProjection> resultado =
                usuarioSistemaRepository.buscarResumenPaginado(
                        termino,
                        activo,
                        tipoPerfil,
                        pageable);

        List<UsuarioSistemaResumenDTO> contenido = resultado.getContent()
                .stream()
                .map(usuarioSistemaMapper::convertirAResumenDTO)
                .toList();

        return new PageResponseDTO<>(
                contenido,
                page,
                size,
                resultado.getTotalElements(),
                resultado.getTotalPages());
    }

    @Transactional(readOnly = true, noRollbackFor = BusinessException.class)
    public List<UsuarioSistemaDTO> listarActivos() {
        return usuarioSistemaRepository.findByActivoTrue()
                .stream()
                .map(usuarioSistemaMapper::convertirADTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public UsuarioSistemaDTO obtenerPorId(Long id) {
        usuarioSistemaValidator.validarIdObligatorio(id);

        return usuarioSistemaMapper.convertirADTO(
                buscarUsuarioConRolYPermisos(id));
    }

    @Transactional
    public UsuarioSistemaDTO cambiarEstado(
            Long id,
            Boolean activo) {

        usuarioSistemaValidator.validarIdObligatorio(id);
        usuarioSistemaValidator.validarEstadoObligatorio(activo);

        administracionInvariantService
                .validarCambioEstadoUsuario(id, activo);

        UsuarioSistema usuario =
                buscarUsuarioConRolYPermisos(id);

        usuarioSistemaValidator.validarCambioEstado(usuario, activo);

        usuario.setActivo(activo);

        return usuarioSistemaMapper.convertirADTO(
                usuarioSistemaRepository.save(usuario));
    }

    private UsuarioSistema buscarUsuarioConRolYPermisos(Long id) {
        return usuarioSistemaRepository
                .findWithRolAndPermisosById(id)
                .orElseThrow(() ->
                        new BusinessException(
                                "Usuario del sistema no encontrado con id: "
                                        + id));
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
