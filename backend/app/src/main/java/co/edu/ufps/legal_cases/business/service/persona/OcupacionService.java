package co.edu.ufps.legal_cases.business.service.persona;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.dto.persona.OcupacionDTO;
import co.edu.ufps.legal_cases.business.model.persona.Ocupacion;
import co.edu.ufps.legal_cases.business.repository.persona.OcupacionRepository;
import co.edu.ufps.legal_cases.business.service.persona.ocupacion.OcupacionMapper;
import co.edu.ufps.legal_cases.business.service.persona.ocupacion.OcupacionValidator;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

@Service
public class OcupacionService {

    private final OcupacionRepository ocupacionRepository;
    private final OcupacionMapper ocupacionMapper;
    private final OcupacionValidator ocupacionValidator;

    public OcupacionService(
            OcupacionRepository ocupacionRepository,
            OcupacionMapper ocupacionMapper,
            OcupacionValidator ocupacionValidator) {
        this.ocupacionRepository = ocupacionRepository;
        this.ocupacionMapper = ocupacionMapper;
        this.ocupacionValidator = ocupacionValidator;
    }

    // Lista ocupaciones activas para formularios, selects y uso normal.
    @Transactional(readOnly = true)
    public List<OcupacionDTO> listar() {
        return ocupacionRepository.findByActivoTrueOrderByNombreAsc()
                .stream()
                .map(ocupacionMapper::convertirADTO)
                .toList();
    }

    // Lista todas las ocupaciones para administración del catálogo.
    @Transactional(readOnly = true)
    public List<OcupacionDTO> listarTodos() {
        return ocupacionRepository.findAllByOrderByNombreAsc()
                .stream()
                .map(ocupacionMapper::convertirADTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public OcupacionDTO obtenerPorId(Long id) {
        Ocupacion ocupacion = buscarPorIdActivo(id);
        return ocupacionMapper.convertirADTO(ocupacion);
    }

    @Transactional
    public OcupacionDTO crear(OcupacionDTO dto) {
        ocupacionValidator.validarCreacion(dto);

        String nombre = ocupacionValidator.normalizarNombre(dto.getNombre());

        ocupacionValidator.validarNombreDisponible(nombre);

        Ocupacion ocupacion = ocupacionMapper.crearEntidad(nombre);

        return ocupacionMapper.convertirADTO(ocupacionRepository.save(ocupacion));
    }

    @Transactional
    public OcupacionDTO actualizar(Long id, OcupacionDTO dto) {
        Ocupacion ocupacion = buscarPorId(id);

        ocupacionValidator.validarActualizacion(id, dto);

        String nombreNuevo = ocupacionValidator.normalizarNombre(dto.getNombre());

        ocupacionValidator.validarNombreDisponibleParaActualizacion(nombreNuevo, ocupacion.getId());
        ocupacionValidator.validarExistenCambios(ocupacion, nombreNuevo);

        ocupacionMapper.aplicarDatos(ocupacion, nombreNuevo);

        return ocupacionMapper.convertirADTO(ocupacionRepository.save(ocupacion));
    }

    @Transactional
    public OcupacionDTO cambiarEstado(Long id, Boolean activo) {
        Ocupacion ocupacion = buscarPorId(id);

        ocupacionValidator.validarCambioEstado(ocupacion, activo);

        ocupacion.setActivo(activo);

        return ocupacionMapper.convertirADTO(ocupacionRepository.save(ocupacion));
    }

    @Transactional
    public void eliminar(Long id) {
        Ocupacion ocupacion = buscarPorIdActivo(id);

        // Desactivación lógica: se conserva porque puede estar asociada a personas.
        ocupacion.setActivo(false);

        ocupacionRepository.save(ocupacion);
    }

    private Ocupacion buscarPorId(Long id) {
        if (id == null) {
            throw new BusinessException("El id de la ocupación es obligatorio");
        }

        return ocupacionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Ocupación no encontrada con id: " + id));
    }

    private Ocupacion buscarPorIdActivo(Long id) {
        if (id == null) {
            throw new BusinessException("El id de la ocupación es obligatorio");
        }

        return ocupacionRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new BusinessException(
                        "Ocupación no encontrada o inactiva con id: " + id));
    }
}