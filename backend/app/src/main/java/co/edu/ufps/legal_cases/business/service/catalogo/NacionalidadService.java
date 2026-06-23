package co.edu.ufps.legal_cases.business.service.catalogo;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.dto.catalogo.NacionalidadDTO;
import co.edu.ufps.legal_cases.business.model.catalogo.Nacionalidad;
import co.edu.ufps.legal_cases.business.repository.catalogo.NacionalidadRepository;
import co.edu.ufps.legal_cases.business.service.catalogo.nacionalidad.NacionalidadMapper;
import co.edu.ufps.legal_cases.business.service.catalogo.nacionalidad.NacionalidadValidator;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

@Service
public class NacionalidadService {

    private final NacionalidadRepository nacionalidadRepository;
    private final NacionalidadMapper nacionalidadMapper;
    private final NacionalidadValidator nacionalidadValidator;

    public NacionalidadService(
            NacionalidadRepository nacionalidadRepository,
            NacionalidadMapper nacionalidadMapper,
            NacionalidadValidator nacionalidadValidator) {
        this.nacionalidadRepository = nacionalidadRepository;
        this.nacionalidadMapper = nacionalidadMapper;
        this.nacionalidadValidator = nacionalidadValidator;
    }

    // Lista nacionalidades activas para formularios, selects y uso normal.
    @Transactional(readOnly = true)
    public List<NacionalidadDTO> listar() {
        return nacionalidadRepository.findByActivoTrueOrderByNombreAsc()
                .stream()
                .map(nacionalidadMapper::convertirADTO)
                .toList();
    }

    // Lista todas las nacionalidades para administración del catálogo.
    @Transactional(readOnly = true)
    public List<NacionalidadDTO> listarTodos() {
        return nacionalidadRepository.findAllByOrderByNombreAsc()
                .stream()
                .map(nacionalidadMapper::convertirADTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public NacionalidadDTO obtenerPorId(Long id) {
        Nacionalidad nacionalidad = buscarPorIdActivo(id);
        return nacionalidadMapper.convertirADTO(nacionalidad);
    }

    @Transactional
    public NacionalidadDTO crear(NacionalidadDTO dto) {
        nacionalidadValidator.validarCreacion(dto);

        String nombre = nacionalidadValidator.normalizarNombre(dto.getNombre());

        nacionalidadValidator.validarNombreDisponible(nombre);

        Nacionalidad nacionalidad = nacionalidadMapper.crearEntidad(nombre);

        return nacionalidadMapper.convertirADTO(nacionalidadRepository.save(nacionalidad));
    }

    @Transactional
    public NacionalidadDTO actualizar(Long id, NacionalidadDTO dto) {
        Nacionalidad nacionalidad = buscarPorId(id);

        nacionalidadValidator.validarActualizacion(id, dto);

        String nombreNuevo = nacionalidadValidator.normalizarNombre(dto.getNombre());

        nacionalidadValidator.validarNombreDisponibleParaActualizacion(nombreNuevo, nacionalidad.getId());
        nacionalidadValidator.validarExistenCambios(nacionalidad, nombreNuevo);

        nacionalidadMapper.aplicarDatos(nacionalidad, nombreNuevo);

        return nacionalidadMapper.convertirADTO(nacionalidadRepository.save(nacionalidad));
    }

    @Transactional
    public NacionalidadDTO cambiarEstado(Long id, Boolean activo) {
        Nacionalidad nacionalidad = buscarPorId(id);

        nacionalidadValidator.validarCambioEstado(nacionalidad, activo);

        nacionalidad.setActivo(activo);

        return nacionalidadMapper.convertirADTO(nacionalidadRepository.save(nacionalidad));
    }

    @Transactional
    public void eliminar(Long id) {
        Nacionalidad nacionalidad = buscarPorIdActivo(id);

        // Desactivación lógica: se conserva porque puede estar asociada a personas.
        nacionalidad.setActivo(false);

        nacionalidadRepository.save(nacionalidad);
    }

    private Nacionalidad buscarPorId(Long id) {
        if (id == null) {
            throw new BusinessException("El id de la nacionalidad es obligatorio");
        }

        return nacionalidadRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Nacionalidad no encontrada con id: " + id));
    }

    private Nacionalidad buscarPorIdActivo(Long id) {
        if (id == null) {
            throw new BusinessException("El id de la nacionalidad es obligatorio");
        }

        return nacionalidadRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new BusinessException(
                        "Nacionalidad no encontrada o inactiva con id: " + id));
    }
}