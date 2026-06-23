package co.edu.ufps.legal_cases.business.service.persona;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.dto.persona.TipoPersonaDTO;
import co.edu.ufps.legal_cases.business.model.persona.TipoPersona;
import co.edu.ufps.legal_cases.business.repository.persona.TipoPersonaRepository;
import co.edu.ufps.legal_cases.business.service.persona.tipopersona.TipoPersonaMapper;
import co.edu.ufps.legal_cases.business.service.persona.tipopersona.TipoPersonaValidator;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

@Service
public class TipoPersonaService {

    private final TipoPersonaRepository tipoPersonaRepository;
    private final TipoPersonaMapper tipoPersonaMapper;
    private final TipoPersonaValidator tipoPersonaValidator;

    public TipoPersonaService(
            TipoPersonaRepository tipoPersonaRepository,
            TipoPersonaMapper tipoPersonaMapper,
            TipoPersonaValidator tipoPersonaValidator) {
        this.tipoPersonaRepository = tipoPersonaRepository;
        this.tipoPersonaMapper = tipoPersonaMapper;
        this.tipoPersonaValidator = tipoPersonaValidator;
    }

    // Lista tipos de persona activos para formularios, selects y uso normal.
    @Transactional(readOnly = true)
    public List<TipoPersonaDTO> listar() {
        return tipoPersonaRepository.findByActivoTrueOrderByNombreAsc()
                .stream()
                .map(tipoPersonaMapper::convertirADTO)
                .toList();
    }

    // Lista todos los tipos de persona para administración del catálogo.
    @Transactional(readOnly = true)
    public List<TipoPersonaDTO> listarTodos() {
        return tipoPersonaRepository.findAllByOrderByNombreAsc()
                .stream()
                .map(tipoPersonaMapper::convertirADTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public TipoPersonaDTO obtenerPorId(Long id) {
        TipoPersona tipoPersona = buscarPorIdActivo(id);
        return tipoPersonaMapper.convertirADTO(tipoPersona);
    }

    @Transactional
    public TipoPersonaDTO crear(TipoPersonaDTO dto) {
        tipoPersonaValidator.validarCreacion(dto);

        String nombre = tipoPersonaValidator.normalizarNombre(dto.getNombre());

        tipoPersonaValidator.validarNombreDisponible(nombre);

        TipoPersona tipoPersona = tipoPersonaMapper.crearEntidad(nombre);

        return tipoPersonaMapper.convertirADTO(tipoPersonaRepository.save(tipoPersona));
    }

    @Transactional
    public TipoPersonaDTO actualizar(Long id, TipoPersonaDTO dto) {
        TipoPersona tipoPersona = buscarPorId(id);

        tipoPersonaValidator.validarActualizacion(id, dto);

        String nombreNuevo = tipoPersonaValidator.normalizarNombre(dto.getNombre());

        tipoPersonaValidator.validarNombreDisponibleParaActualizacion(nombreNuevo, tipoPersona.getId());
        tipoPersonaValidator.validarExistenCambios(tipoPersona, nombreNuevo);

        tipoPersonaMapper.aplicarDatos(tipoPersona, nombreNuevo);

        return tipoPersonaMapper.convertirADTO(tipoPersonaRepository.save(tipoPersona));
    }

    @Transactional
    public TipoPersonaDTO cambiarEstado(Long id, Boolean activo) {
        TipoPersona tipoPersona = buscarPorId(id);

        tipoPersonaValidator.validarCambioEstado(tipoPersona, activo);

        tipoPersona.setActivo(activo);

        return tipoPersonaMapper.convertirADTO(tipoPersonaRepository.save(tipoPersona));
    }

    @Transactional
    public void eliminar(Long id) {
        TipoPersona tipoPersona = buscarPorIdActivo(id);

        // Desactivación lógica: se conserva porque puede estar asociado a personas.
        tipoPersona.setActivo(false);

        tipoPersonaRepository.save(tipoPersona);
    }

    private TipoPersona buscarPorId(Long id) {
        if (id == null) {
            throw new BusinessException("El id del tipo de persona es obligatorio");
        }

        return tipoPersonaRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Tipo de persona no encontrado con id: " + id));
    }

    private TipoPersona buscarPorIdActivo(Long id) {
        if (id == null) {
            throw new BusinessException("El id del tipo de persona es obligatorio");
        }

        return tipoPersonaRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new BusinessException(
                        "Tipo de persona no encontrado o inactivo con id: " + id));
    }
}