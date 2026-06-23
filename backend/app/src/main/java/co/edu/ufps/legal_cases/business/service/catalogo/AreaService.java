package co.edu.ufps.legal_cases.business.service.catalogo;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.dto.catalogo.AreaDTO;
import co.edu.ufps.legal_cases.business.model.catalogo.Area;
import co.edu.ufps.legal_cases.business.repository.catalogo.AreaRepository;
import co.edu.ufps.legal_cases.business.service.catalogo.area.AreaMapper;
import co.edu.ufps.legal_cases.business.service.catalogo.area.AreaValidator;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

@Service
public class AreaService {

    private final AreaRepository areaRepository;
    private final AreaMapper areaMapper;
    private final AreaValidator areaValidator;

    public AreaService(
            AreaRepository areaRepository,
            AreaMapper areaMapper,
            AreaValidator areaValidator) {
        this.areaRepository = areaRepository;
        this.areaMapper = areaMapper;
        this.areaValidator = areaValidator;
    }

    // Lista áreas activas para formularios, selects y uso normal del sistema.
    @Transactional(readOnly = true)
    public List<AreaDTO> listar() {
        return areaRepository.findByActivoTrueOrderByNombreAsc()
                .stream()
                .map(areaMapper::convertirADTO)
                .toList();
    }

    // Lista todas las áreas para administración del catálogo.
    @Transactional(readOnly = true)
    public List<AreaDTO> listarTodos() {
        return areaRepository.findAllByOrderByNombreAsc()
                .stream()
                .map(areaMapper::convertirADTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public AreaDTO obtenerPorId(Long id) {
        Area area = buscarPorIdActivo(id);
        return areaMapper.convertirADTO(area);
    }

    @Transactional
    public AreaDTO crear(AreaDTO dto) {
        areaValidator.validarCreacion(dto);

        String nombre = areaValidator.normalizarNombre(dto.getNombre());

        areaValidator.validarNombreDisponible(nombre);

        Area area = areaMapper.crearEntidad(nombre);

        return areaMapper.convertirADTO(areaRepository.save(area));
    }

    @Transactional
    public AreaDTO actualizar(Long id, AreaDTO dto) {
        Area area = buscarPorId(id);

        areaValidator.validarActualizacion(id, dto);

        String nombreNuevo = areaValidator.normalizarNombre(dto.getNombre());

        areaValidator.validarNombreDisponibleParaActualizacion(nombreNuevo, area.getId());
        areaValidator.validarExistenCambios(area, nombreNuevo);

        areaMapper.aplicarDatos(area, nombreNuevo);

        return areaMapper.convertirADTO(areaRepository.save(area));
    }

    @Transactional
    public AreaDTO cambiarEstado(Long id, Boolean activo) {
        Area area = buscarPorId(id);

        areaValidator.validarCambioEstado(area, activo);

        area.setActivo(activo);

        return areaMapper.convertirADTO(areaRepository.save(area));
    }

    @Transactional
    public void eliminar(Long id) {
        Area area = buscarPorIdActivo(id);

        // Desactivación lógica: se conserva porque puede estar asociada a temas y consultas.
        area.setActivo(false);

        areaRepository.save(area);
    }

    private Area buscarPorId(Long id) {
        if (id == null) {
            throw new BusinessException("El id del área es obligatorio");
        }

        return areaRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Área no encontrada con id: " + id));
    }

    private Area buscarPorIdActivo(Long id) {
        if (id == null) {
            throw new BusinessException("El id del área es obligatorio");
        }

        return areaRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new BusinessException("Área no encontrada o inactiva con id: " + id));
    }
}