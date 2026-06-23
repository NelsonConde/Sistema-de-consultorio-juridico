package co.edu.ufps.legal_cases.security.service.access;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.dto.access.PermisoDTO;
import co.edu.ufps.legal_cases.security.model.access.Permiso;
import co.edu.ufps.legal_cases.security.repository.access.PermisoRepository;
import co.edu.ufps.legal_cases.security.service.access.permiso.PermisoMapper;
import co.edu.ufps.legal_cases.security.service.access.permiso.PermisoValidator;

@Service
public class PermisoService {

    private final PermisoRepository permisoRepository;
    private final PermisoMapper permisoMapper;
    private final PermisoValidator permisoValidator;

    public PermisoService(
            PermisoRepository permisoRepository,
            PermisoMapper permisoMapper,
            PermisoValidator permisoValidator) {
        this.permisoRepository = permisoRepository;
        this.permisoMapper = permisoMapper;
        this.permisoValidator = permisoValidator;
    }

    @Transactional(readOnly = true)
    public List<PermisoDTO> listar() {
        return permisoRepository.findAll()
                .stream()
                .map(permisoMapper::convertirADTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PermisoDTO> listarActivos() {
        return permisoRepository.findByActivoTrue()
                .stream()
                .map(permisoMapper::convertirADTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public PermisoDTO obtenerPorId(Long id) {
        Permiso permiso = buscarPorId(id);

        return permisoMapper.convertirADTO(permiso);
    }

    @Transactional
    public PermisoDTO crear(PermisoDTO dto) {
        permisoValidator.validarCreacion(dto);

        String nombre = permisoValidator.normalizarNombre(dto.getNombre());
        String descripcion = permisoValidator.normalizarDescripcion(dto.getDescripcion());
        Boolean activo = dto.getActivo() != null ? dto.getActivo() : true;

        validarNombreDisponible(nombre);

        Permiso permiso = new Permiso();
        permisoMapper.aplicarDatos(permiso, nombre, descripcion, activo);

        return permisoMapper.convertirADTO(permisoRepository.save(permiso));
    }

    @Transactional
    public PermisoDTO actualizar(Long id, PermisoDTO dto) {
        Permiso permiso = buscarPorId(id);

        permisoValidator.validarActualizacion(permiso, dto);

        String nombreNuevo = permisoValidator.normalizarNombre(dto.getNombre());
        String descripcionNueva = permisoValidator.normalizarDescripcion(dto.getDescripcion());
        Boolean activoNuevo = dto.getActivo() != null ? dto.getActivo() : permiso.getActivo();

        validarNombreDisponibleParaActualizar(nombreNuevo, permiso.getId());
        permisoValidator.validarExistenCambios(permiso, nombreNuevo, descripcionNueva, activoNuevo);

        permisoMapper.aplicarDatos(permiso, nombreNuevo, descripcionNueva, activoNuevo);

        return permisoMapper.convertirADTO(permisoRepository.save(permiso));
    }

    @Transactional
    public PermisoDTO cambiarEstado(Long id, Boolean activo) {
        Permiso permiso = buscarPorId(id);

        permisoValidator.validarCambioEstado(permiso, activo);

        permiso.setActivo(activo);

        return permisoMapper.convertirADTO(permisoRepository.save(permiso));
    }

    private Permiso buscarPorId(Long id) {
        if (id == null) {
            throw new BusinessException("El id del permiso es obligatorio");
        }

        return permisoRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Permiso no encontrado con id: " + id));
    }

    private void validarNombreDisponible(String nombre) {
        if (permisoRepository.existsByNombreIgnoreCase(nombre)) {
            throw new BusinessException("Ya existe un permiso con ese nombre");
        }
    }

    private void validarNombreDisponibleParaActualizar(String nombre, Long id) {
        if (permisoRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new BusinessException("Ya existe un permiso con ese nombre");
        }
    }
}