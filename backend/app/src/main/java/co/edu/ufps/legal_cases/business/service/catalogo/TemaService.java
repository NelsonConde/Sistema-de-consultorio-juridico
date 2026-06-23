package co.edu.ufps.legal_cases.business.service.catalogo;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.dto.catalogo.TemaDTO;
import co.edu.ufps.legal_cases.business.model.catalogo.Area;
import co.edu.ufps.legal_cases.business.model.catalogo.Tema;
import co.edu.ufps.legal_cases.business.repository.catalogo.AreaRepository;
import co.edu.ufps.legal_cases.business.repository.catalogo.TemaRepository;
import co.edu.ufps.legal_cases.business.service.catalogo.tema.TemaMapper;
import co.edu.ufps.legal_cases.business.service.catalogo.tema.TemaValidator;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

@Service
public class TemaService {

    private final TemaRepository temaRepository;
    private final AreaRepository areaRepository;
    private final TemaMapper temaMapper;
    private final TemaValidator temaValidator;

    public TemaService(
            TemaRepository temaRepository,
            AreaRepository areaRepository,
            TemaMapper temaMapper,
            TemaValidator temaValidator) {
        this.temaRepository = temaRepository;
        this.areaRepository = areaRepository;
        this.temaMapper = temaMapper;
        this.temaValidator = temaValidator;
    }

    // Lista temas activos para formularios, selects y uso normal del sistema.
    @Transactional(readOnly = true)
    public List<TemaDTO> listar() {
        return temaRepository.findByActivoTrueOrderByNombreAsc()
                .stream()
                .map(temaMapper::convertirADTO)
                .toList();
    }

    // Lista todos los temas para administración del catálogo.
    @Transactional(readOnly = true)
    public List<TemaDTO> listarTodos() {
        return temaRepository.findAllByOrderByNombreAsc()
                .stream()
                .map(temaMapper::convertirADTO)
                .toList();
    }

    // Lista temas activos de un área activa.
    @Transactional(readOnly = true)
    public List<TemaDTO> listarPorArea(Long areaId) {
        validarAreaActiva(areaId);

        return temaRepository.findByAreaIdAndActivoTrueOrderByNombreAsc(areaId)
                .stream()
                .map(temaMapper::convertirADTO)
                .toList();
    }

    // Lista todos los temas de un área para administración.
    @Transactional(readOnly = true)
    public List<TemaDTO> listarTodosPorArea(Long areaId) {
        validarAreaExistente(areaId);

        return temaRepository.findByAreaIdOrderByNombreAsc(areaId)
                .stream()
                .map(temaMapper::convertirADTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public TemaDTO obtenerPorId(Long id) {
        Tema tema = buscarPorIdActivo(id);
        return temaMapper.convertirADTO(tema);
    }

    @Transactional
    public TemaDTO crear(TemaDTO dto) {
        temaValidator.validarCreacion(dto);

        String nombre = temaValidator.normalizarNombre(dto.getNombre());
        Area area = obtenerAreaActiva(dto.getAreaId());

        temaValidator.validarNombreDisponible(nombre, area.getId());

        Tema tema = temaMapper.crearEntidad(nombre, area);

        return temaMapper.convertirADTO(temaRepository.save(tema));
    }

    @Transactional
    public TemaDTO actualizar(Long id, TemaDTO dto) {
        Tema tema = buscarPorId(id);

        temaValidator.validarActualizacion(id, dto);

        String nombreNuevo = temaValidator.normalizarNombre(dto.getNombre());
        Area areaNueva = obtenerAreaActiva(dto.getAreaId());

        temaValidator.validarNombreDisponibleParaActualizacion(
                nombreNuevo,
                areaNueva.getId(),
                tema.getId());

        temaValidator.validarExistenCambios(tema, nombreNuevo, areaNueva);

        temaMapper.aplicarDatos(tema, nombreNuevo, areaNueva);

        return temaMapper.convertirADTO(temaRepository.save(tema));
    }

    @Transactional
    public TemaDTO cambiarEstado(Long id, Boolean activo) {
        Tema tema = buscarPorId(id);

        temaValidator.validarCambioEstado(tema, activo);

        tema.setActivo(activo);

        return temaMapper.convertirADTO(temaRepository.save(tema));
    }

    @Transactional
    public void eliminar(Long id) {
        Tema tema = buscarPorIdActivo(id);

        // Desactivación lógica: se conserva porque puede estar asociado a tipos y consultas.
        tema.setActivo(false);

        temaRepository.save(tema);
    }

    private void validarAreaActiva(Long areaId) {
        obtenerAreaActiva(areaId);
    }

    private void validarAreaExistente(Long areaId) {
        if (areaId == null) {
            throw new BusinessException("El área es obligatoria");
        }

        areaRepository.findById(areaId)
                .orElseThrow(() -> new BusinessException("Área no encontrada con id: " + areaId));
    }

    private Area obtenerAreaActiva(Long areaId) {
        if (areaId == null) {
            throw new BusinessException("El área es obligatoria");
        }

        return areaRepository.findByIdAndActivoTrue(areaId)
                .orElseThrow(() -> new BusinessException("Área no encontrada o inactiva con id: " + areaId));
    }

    private Tema buscarPorId(Long id) {
        if (id == null) {
            throw new BusinessException("El id del tema es obligatorio");
        }

        return temaRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Tema no encontrado con id: " + id));
    }

    private Tema buscarPorIdActivo(Long id) {
        if (id == null) {
            throw new BusinessException("El id del tema es obligatorio");
        }

        return temaRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new BusinessException("Tema no encontrado o inactivo con id: " + id));
    }
}