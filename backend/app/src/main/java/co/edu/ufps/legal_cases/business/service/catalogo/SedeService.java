package co.edu.ufps.legal_cases.business.service.catalogo;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.dto.catalogo.SedeDTO;
import co.edu.ufps.legal_cases.business.model.catalogo.Sede;
import co.edu.ufps.legal_cases.business.repository.catalogo.SedeRepository;
import co.edu.ufps.legal_cases.business.service.catalogo.sede.SedeMapper;
import co.edu.ufps.legal_cases.business.service.catalogo.sede.SedeValidator;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

@Service
public class SedeService {

    private final SedeRepository sedeRepository;
    private final SedeMapper sedeMapper;
    private final SedeValidator sedeValidator;

    public SedeService(
            SedeRepository sedeRepository,
            SedeMapper sedeMapper,
            SedeValidator sedeValidator) {
        this.sedeRepository = sedeRepository;
        this.sedeMapper = sedeMapper;
        this.sedeValidator = sedeValidator;
    }

    // Lista sedes activas para formularios y selects del frontend.
    @Transactional(readOnly = true)
    public List<SedeDTO> listar() {
        return sedeRepository.findByActivoTrueOrderByNombreAsc()
                .stream()
                .map(sedeMapper::convertirADTO)
                .toList();
    }

    // Lista todas las sedes para administración del catálogo.
    @Transactional(readOnly = true)
    public List<SedeDTO> listarTodos() {
        return sedeRepository.findAllByOrderByNombreAsc()
                .stream()
                .map(sedeMapper::convertirADTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public SedeDTO obtenerPorId(Long id) {
        Sede sede = buscarPorIdActivo(id);
        return sedeMapper.convertirADTO(sede);
    }

    @Transactional
    public SedeDTO crear(SedeDTO dto) {
        sedeValidator.validarCreacion(dto);

        String nombre = sedeValidator.normalizarNombre(dto.getNombre());

        sedeValidator.validarNombreDisponible(nombre);

        Sede sede = sedeMapper.crearEntidad(nombre);

        return sedeMapper.convertirADTO(sedeRepository.save(sede));
    }

    @Transactional
    public SedeDTO actualizar(Long id, SedeDTO dto) {
        Sede sede = buscarPorId(id);

        sedeValidator.validarActualizacion(id, dto);

        String nombreNuevo = sedeValidator.normalizarNombre(dto.getNombre());

        sedeValidator.validarNombreDisponibleParaActualizacion(nombreNuevo, sede.getId());
        sedeValidator.validarExistenCambios(sede, nombreNuevo);

        sedeMapper.aplicarDatos(sede, nombreNuevo);

        return sedeMapper.convertirADTO(sedeRepository.save(sede));
    }

    @Transactional
    public void eliminar(Long id) {
        Sede sede = buscarPorIdActivo(id);

        // Desactivación lógica: se conserva porque puede estar asociada a usuarios o consultas.
        sede.setActivo(false);

        sedeRepository.save(sede);
    }

    @Transactional
    public SedeDTO cambiarEstado(Long id, Boolean activo) {
        Sede sede = buscarPorId(id);

        sedeValidator.validarCambioEstado(sede, activo);

        sede.setActivo(activo);

        return sedeMapper.convertirADTO(sedeRepository.save(sede));
    }

    private Sede buscarPorId(Long id) {
        if (id == null) {
            throw new BusinessException("El id de la sede es obligatorio");
        }

        return sedeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Sede no encontrada con id: " + id));
    }

    private Sede buscarPorIdActivo(Long id) {
        if (id == null) {
            throw new BusinessException("El id de la sede es obligatorio");
        }

        return sedeRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new BusinessException("Sede no encontrada o inactiva con id: " + id));
    }
}