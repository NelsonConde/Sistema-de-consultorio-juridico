package co.edu.ufps.legal_cases.business.service.persona;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.dto.persona.CondicionDTO;
import co.edu.ufps.legal_cases.business.model.persona.Condicion;
import co.edu.ufps.legal_cases.business.repository.persona.CondicionRepository;
import co.edu.ufps.legal_cases.business.service.persona.condicion.CondicionMapper;
import co.edu.ufps.legal_cases.business.service.persona.condicion.CondicionValidator;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

@Service
public class CondicionService {

    private final CondicionRepository condicionRepository;
    private final CondicionMapper condicionMapper;
    private final CondicionValidator condicionValidator;

    public CondicionService(
            CondicionRepository condicionRepository,
            CondicionMapper condicionMapper,
            CondicionValidator condicionValidator) {
        this.condicionRepository = condicionRepository;
        this.condicionMapper = condicionMapper;
        this.condicionValidator = condicionValidator;
    }

    // Lista condiciones activas para formularios, selects y uso normal.
    @Transactional(readOnly = true)
    public List<CondicionDTO> listar() {
        return condicionRepository.findByActivoTrueOrderByNombreAsc()
                .stream()
                .map(condicionMapper::convertirADTO)
                .toList();
    }

    // Lista todas las condiciones para administración del catálogo.
    @Transactional(readOnly = true)
    public List<CondicionDTO> listarTodos() {
        return condicionRepository.findAllByOrderByNombreAsc()
                .stream()
                .map(condicionMapper::convertirADTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public CondicionDTO obtenerPorId(Long id) {
        Condicion condicion = buscarPorIdActivo(id);
        return condicionMapper.convertirADTO(condicion);
    }

    @Transactional
    public CondicionDTO crear(CondicionDTO dto) {
        condicionValidator.validarCreacion(dto);

        String nombre = condicionValidator.normalizarNombre(dto.getNombre());

        condicionValidator.validarNombreDisponible(nombre);

        Condicion condicion = condicionMapper.crearEntidad(nombre);

        return condicionMapper.convertirADTO(condicionRepository.save(condicion));
    }

    @Transactional
    public CondicionDTO actualizar(Long id, CondicionDTO dto) {
        Condicion condicion = buscarPorId(id);

        condicionValidator.validarActualizacion(id, dto);

        String nombreNuevo = condicionValidator.normalizarNombre(dto.getNombre());

        condicionValidator.validarNombreDisponibleParaActualizacion(nombreNuevo, condicion.getId());
        condicionValidator.validarExistenCambios(condicion, nombreNuevo);

        condicionMapper.aplicarDatos(condicion, nombreNuevo);

        return condicionMapper.convertirADTO(condicionRepository.save(condicion));
    }

    @Transactional
    public CondicionDTO cambiarEstado(Long id, Boolean activo) {
        Condicion condicion = buscarPorId(id);

        condicionValidator.validarCambioEstado(condicion, activo);

        condicion.setActivo(activo);

        return condicionMapper.convertirADTO(condicionRepository.save(condicion));
    }

    @Transactional
    public void eliminar(Long id) {
        Condicion condicion = buscarPorIdActivo(id);

        // Desactivación lógica: se conserva porque puede estar asociada a personas.
        condicion.setActivo(false);

        condicionRepository.save(condicion);
    }

    private Condicion buscarPorId(Long id) {
        if (id == null) {
            throw new BusinessException("El id de la condición es obligatorio");
        }

        return condicionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Condición no encontrada con id: " + id));
    }

    private Condicion buscarPorIdActivo(Long id) {
        if (id == null) {
            throw new BusinessException("El id de la condición es obligatorio");
        }

        return condicionRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new BusinessException(
                        "Condición no encontrada o inactiva con id: " + id));
    }
}