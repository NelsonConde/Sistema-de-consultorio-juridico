package co.edu.ufps.legal_cases.business.service.catalogo;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.dto.catalogo.BarrioDTO;
import co.edu.ufps.legal_cases.business.model.catalogo.Barrio;
import co.edu.ufps.legal_cases.business.model.catalogo.Municipio;
import co.edu.ufps.legal_cases.business.repository.catalogo.BarrioRepository;
import co.edu.ufps.legal_cases.business.repository.catalogo.MunicipioRepository;
import co.edu.ufps.legal_cases.business.service.catalogo.barrio.BarrioMapper;
import co.edu.ufps.legal_cases.business.service.catalogo.barrio.BarrioValidator;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

@Service
public class BarrioService {

    private final BarrioRepository barrioRepository;
    private final MunicipioRepository municipioRepository;
    private final BarrioMapper barrioMapper;
    private final BarrioValidator barrioValidator;

    public BarrioService(
            BarrioRepository barrioRepository,
            MunicipioRepository municipioRepository,
            BarrioMapper barrioMapper,
            BarrioValidator barrioValidator) {
        this.barrioRepository = barrioRepository;
        this.municipioRepository = municipioRepository;
        this.barrioMapper = barrioMapper;
        this.barrioValidator = barrioValidator;
    }

    // Lista barrios activos para formularios, selects y uso normal.
    @Transactional(readOnly = true)
    public List<BarrioDTO> listar() {
        return barrioRepository.findByActivoTrueOrderByNombreAsc()
                .stream()
                .map(barrioMapper::convertirADTO)
                .toList();
    }

    // Lista todos los barrios para administración del catálogo.
    @Transactional(readOnly = true)
    public List<BarrioDTO> listarTodos() {
        return barrioRepository.findAllByOrderByNombreAsc()
                .stream()
                .map(barrioMapper::convertirADTO)
                .toList();
    }

    // Lista barrios activos de un municipio activo.
    @Transactional(readOnly = true)
    public List<BarrioDTO> listarPorMunicipio(Long municipioId) {
        obtenerMunicipioActivo(municipioId);

        return barrioRepository.findByMunicipioIdAndActivoTrueOrderByNombreAsc(municipioId)
                .stream()
                .map(barrioMapper::convertirADTO)
                .toList();
    }

    // Lista todos los barrios de un municipio para administración.
    @Transactional(readOnly = true)
    public List<BarrioDTO> listarTodosPorMunicipio(Long municipioId) {
        obtenerMunicipioExistente(municipioId);

        return barrioRepository.findByMunicipioIdOrderByNombreAsc(municipioId)
                .stream()
                .map(barrioMapper::convertirADTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public BarrioDTO obtenerPorId(Long id) {
        Barrio barrio = buscarPorIdActivo(id);
        return barrioMapper.convertirADTO(barrio);
    }

    @Transactional
    public BarrioDTO crear(BarrioDTO dto) {
        barrioValidator.validarCreacion(dto);

        String nombre = barrioValidator.normalizarNombre(dto.getNombre());
        Municipio municipio = obtenerMunicipioActivo(dto.getMunicipioId());

        barrioValidator.validarNombreDisponible(nombre, municipio.getId());

        Barrio barrio = barrioMapper.crearEntidad(nombre, municipio);

        return barrioMapper.convertirADTO(barrioRepository.save(barrio));
    }

    @Transactional
    public BarrioDTO actualizar(Long id, BarrioDTO dto) {
        Barrio barrio = buscarPorId(id);

        barrioValidator.validarActualizacion(id, dto);

        String nombreNuevo = barrioValidator.normalizarNombre(dto.getNombre());
        Municipio municipioNuevo = obtenerMunicipioActivo(dto.getMunicipioId());

        barrioValidator.validarNombreDisponibleParaActualizacion(
                nombreNuevo, municipioNuevo.getId(), barrio.getId());

        barrioValidator.validarExistenCambios(barrio, nombreNuevo, municipioNuevo);

        barrioMapper.aplicarDatos(barrio, nombreNuevo, municipioNuevo);

        return barrioMapper.convertirADTO(barrioRepository.save(barrio));
    }

    @Transactional
    public BarrioDTO cambiarEstado(Long id, Boolean activo) {
        Barrio barrio = buscarPorId(id);

        barrioValidator.validarCambioEstado(barrio, activo);

        barrio.setActivo(activo);

        return barrioMapper.convertirADTO(barrioRepository.save(barrio));
    }

    @Transactional
    public void eliminar(Long id) {
        Barrio barrio = buscarPorIdActivo(id);

        // Desactivación lógica: se conserva porque puede estar asociado a personas.
        barrio.setActivo(false);

        barrioRepository.save(barrio);
    }

    private Municipio obtenerMunicipioActivo(Long municipioId) {
        if (municipioId == null) {
            throw new BusinessException("El municipio es obligatorio");
        }

        return municipioRepository.findByIdAndActivoTrue(municipioId)
                .orElseThrow(() -> new BusinessException(
                        "Municipio no encontrado o inactivo con id: " + municipioId));
    }

    private void obtenerMunicipioExistente(Long municipioId) {
        if (municipioId == null) {
            throw new BusinessException("El municipio es obligatorio");
        }

        municipioRepository.findById(municipioId)
                .orElseThrow(() -> new BusinessException(
                        "Municipio no encontrado con id: " + municipioId));
    }

    private Barrio buscarPorId(Long id) {
        if (id == null) {
            throw new BusinessException("El id del barrio es obligatorio");
        }

        return barrioRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Barrio no encontrado con id: " + id));
    }

    private Barrio buscarPorIdActivo(Long id) {
        if (id == null) {
            throw new BusinessException("El id del barrio es obligatorio");
        }

        return barrioRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new BusinessException(
                        "Barrio no encontrado o inactivo con id: " + id));
    }
}