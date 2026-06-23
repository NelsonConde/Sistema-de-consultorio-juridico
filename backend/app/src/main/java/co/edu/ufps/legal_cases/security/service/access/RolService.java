package co.edu.ufps.legal_cases.security.service.access;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.dto.access.RolDTO;
import co.edu.ufps.legal_cases.security.model.access.Permiso;
import co.edu.ufps.legal_cases.security.model.access.Rol;
import co.edu.ufps.legal_cases.security.repository.access.PermisoRepository;
import co.edu.ufps.legal_cases.security.repository.access.RolRepository;
import co.edu.ufps.legal_cases.security.service.access.rol.RolMapper;
import co.edu.ufps.legal_cases.security.service.access.rol.RolValidator;

@Service
public class RolService {

    private final RolRepository rolRepository;
    private final PermisoRepository permisoRepository;
    private final RolMapper rolMapper;
    private final RolValidator rolValidator;

    public RolService(
            RolRepository rolRepository,
            PermisoRepository permisoRepository,
            RolMapper rolMapper,
            RolValidator rolValidator) {
        this.rolRepository = rolRepository;
        this.permisoRepository = permisoRepository;
        this.rolMapper = rolMapper;
        this.rolValidator = rolValidator;
    }

    @Transactional(readOnly = true)
    public List<RolDTO> listar() {
        return rolRepository.findAll()
                .stream()
                .map(rolMapper::convertirADTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RolDTO> listarActivos() {
        return rolRepository.findByActivoTrue()
                .stream()
                .map(rolMapper::convertirADTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public RolDTO obtenerPorId(Long id) {
        Rol rol = buscarRolConPermisos(id);

        return rolMapper.convertirADTO(rol);
    }

    @Transactional
    public RolDTO crear(RolDTO dto) {
        rolValidator.validarCreacion(dto);

        String nombre = rolValidator.normalizarNombre(dto.getNombre());
        String descripcion = rolValidator.normalizarDescripcion(dto.getDescripcion());
        Boolean activo = dto.getActivo() != null ? dto.getActivo() : true;

        validarNombreDisponible(nombre);

        // Se cargan únicamente permisos activos para evitar asignar permisos deshabilitados.
        Set<Permiso> permisos = obtenerPermisosActivos(dto.getPermisoIds());

        Rol rol = new Rol();
        rolMapper.aplicarDatos(rol, nombre, descripcion, activo, permisos);

        return rolMapper.convertirADTO(rolRepository.save(rol));
    }

    @Transactional
    public RolDTO actualizar(Long id, RolDTO dto) {
        Rol rol = buscarRolConPermisos(id);

        rolValidator.validarActualizacion(rol, dto);

        String nombreNuevo = rolValidator.normalizarNombre(dto.getNombre());
        String descripcionNueva = rolValidator.normalizarDescripcion(dto.getDescripcion());
        Boolean activoNuevo = dto.getActivo() != null ? dto.getActivo() : rol.getActivo();

        validarNombreDisponibleParaActualizar(nombreNuevo, rol.getId());

        // Si se envían permisos, se reemplaza la lista actual; si no se envían, se conservan.
        Set<Permiso> permisosNuevos = dto.getPermisoIds() != null
                ? obtenerPermisosActivos(dto.getPermisoIds())
                : rol.getPermisos();

        rolValidator.validarExistenCambios(
                rol,
                nombreNuevo,
                descripcionNueva,
                activoNuevo,
                permisosNuevos);

        rolMapper.aplicarDatos(rol, nombreNuevo, descripcionNueva, activoNuevo, permisosNuevos);

        return rolMapper.convertirADTO(rolRepository.save(rol));
    }

    @Transactional
    public RolDTO cambiarEstado(Long id, Boolean activo) {
        Rol rol = buscarRolConPermisos(id);

        rolValidator.validarCambioEstado(rol, activo);

        rol.setActivo(activo);

        return rolMapper.convertirADTO(rolRepository.save(rol));
    }

    @Transactional
    public RolDTO asignarPermiso(Long rolId, Long permisoId) {
        Rol rol = buscarRolConPermisos(rolId);
        Permiso permiso = obtenerPermisoActivo(permisoId);

        rolValidator.validarPermisoNoAsignado(rol, permiso);

        rol.getPermisos().add(permiso);

        return rolMapper.convertirADTO(rolRepository.save(rol));
    }

    @Transactional
    public RolDTO quitarPermiso(Long rolId, Long permisoId) {
        Rol rol = buscarRolConPermisos(rolId);

        rolValidator.validarPermisoIdObligatorio(permisoId);

        boolean eliminado = rol.getPermisos()
                .removeIf(permiso -> permiso.getId().equals(permisoId));

        rolValidator.validarPermisoAsignado(eliminado);

        return rolMapper.convertirADTO(rolRepository.save(rol));
    }

    // Busca el rol con sus permisos para evitar problemas por carga perezosa.
    private Rol buscarRolConPermisos(Long id) {
        if (id == null) {
            throw new BusinessException("El id del rol es obligatorio");
        }

        return rolRepository.findWithPermisosById(id)
                .orElseThrow(() -> new BusinessException("Rol no encontrado con id: " + id));
    }

    // Valida que el permiso exista y esté activo antes de asociarlo a un rol.
    private Permiso obtenerPermisoActivo(Long permisoId) {
        rolValidator.validarPermisoIdObligatorio(permisoId);

        return permisoRepository.findByIdAndActivoTrue(permisoId)
                .orElseThrow(() -> new BusinessException("Permiso no encontrado o inactivo con id: " + permisoId));
    }

    // Convierte los ids recibidos en entidades Permiso válidas y activas.
    private Set<Permiso> obtenerPermisosActivos(Set<Long> permisoIds) {
        rolValidator.validarListaPermisos(permisoIds);

        if (permisoIds == null || permisoIds.isEmpty()) {
            return new HashSet<>();
        }

        List<Permiso> permisos = permisoRepository.findByIdInAndActivoTrue(permisoIds);

        if (permisos.size() != permisoIds.size()) {
            throw new BusinessException("Uno o más permisos no existen o están inactivos");
        }

        return new HashSet<>(permisos);
    }

    private void validarNombreDisponible(String nombre) {
        if (rolRepository.existsByNombreIgnoreCase(nombre)) {
            throw new BusinessException("Ya existe un rol con ese nombre");
        }
    }

    private void validarNombreDisponibleParaActualizar(String nombre, Long id) {
        if (rolRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new BusinessException("Ya existe un rol con ese nombre");
        }
    }
}