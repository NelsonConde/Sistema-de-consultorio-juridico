package co.edu.ufps.legal_cases.business.service.catalogo;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.dto.catalogo.MunicipioDTO;
import co.edu.ufps.legal_cases.business.model.catalogo.Departamento;
import co.edu.ufps.legal_cases.business.model.catalogo.Municipio;
import co.edu.ufps.legal_cases.business.repository.catalogo.DepartamentoRepository;
import co.edu.ufps.legal_cases.business.repository.catalogo.MunicipioRepository;
import co.edu.ufps.legal_cases.business.service.catalogo.municipio.MunicipioMapper;
import co.edu.ufps.legal_cases.business.service.catalogo.municipio.MunicipioValidator;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

@Service
public class MunicipioService {

    private final MunicipioRepository municipioRepository;
    private final DepartamentoRepository departamentoRepository;
    private final MunicipioMapper municipioMapper;
    private final MunicipioValidator municipioValidator;

    public MunicipioService(
            MunicipioRepository municipioRepository,
            DepartamentoRepository departamentoRepository,
            MunicipioMapper municipioMapper,
            MunicipioValidator municipioValidator) {
        this.municipioRepository = municipioRepository;
        this.departamentoRepository = departamentoRepository;
        this.municipioMapper = municipioMapper;
        this.municipioValidator = municipioValidator;
    }

    // Lista municipios activos para formularios, selects y uso normal.
    @Transactional(readOnly = true)
    public List<MunicipioDTO> listar() {
        return municipioRepository.findByActivoTrueOrderByNombreAsc()
                .stream()
                .map(municipioMapper::convertirADTO)
                .toList();
    }

    // Lista todos los municipios para administración del catálogo.
    @Transactional(readOnly = true)
    public List<MunicipioDTO> listarTodos() {
        return municipioRepository.findAllByOrderByNombreAsc()
                .stream()
                .map(municipioMapper::convertirADTO)
                .toList();
    }

    // Lista municipios activos de un departamento activo.
    @Transactional(readOnly = true)
    public List<MunicipioDTO> listarPorDepartamento(Long departamentoId) {
        obtenerDepartamentoActivo(departamentoId);

        return municipioRepository.findByDepartamentoIdAndActivoTrueOrderByNombreAsc(departamentoId)
                .stream()
                .map(municipioMapper::convertirADTO)
                .toList();
    }

    // Lista todos los municipios de un departamento para administración.
    @Transactional(readOnly = true)
    public List<MunicipioDTO> listarTodosPorDepartamento(Long departamentoId) {
        obtenerDepartamentoExistente(departamentoId);

        return municipioRepository.findByDepartamentoIdOrderByNombreAsc(departamentoId)
                .stream()
                .map(municipioMapper::convertirADTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public MunicipioDTO obtenerPorId(Long id) {
        Municipio municipio = buscarPorIdActivo(id);
        return municipioMapper.convertirADTO(municipio);
    }

    @Transactional
    public MunicipioDTO crear(MunicipioDTO dto) {
        municipioValidator.validarCreacion(dto);

        String nombre = municipioValidator.normalizarNombre(dto.getNombre());
        Departamento departamento = obtenerDepartamentoActivo(dto.getDepartamentoId());

        municipioValidator.validarNombreDisponible(nombre, departamento.getId());

        Municipio municipio = municipioMapper.crearEntidad(nombre, departamento);

        return municipioMapper.convertirADTO(municipioRepository.save(municipio));
    }

    @Transactional
    public MunicipioDTO actualizar(Long id, MunicipioDTO dto) {
        Municipio municipio = buscarPorId(id);

        municipioValidator.validarActualizacion(id, dto);

        String nombreNuevo = municipioValidator.normalizarNombre(dto.getNombre());
        Departamento departamentoNuevo = obtenerDepartamentoActivo(dto.getDepartamentoId());

        municipioValidator.validarNombreDisponibleParaActualizacion(
                nombreNuevo, departamentoNuevo.getId(), municipio.getId());

        municipioValidator.validarExistenCambios(municipio, nombreNuevo, departamentoNuevo);

        municipioMapper.aplicarDatos(municipio, nombreNuevo, departamentoNuevo);

        return municipioMapper.convertirADTO(municipioRepository.save(municipio));
    }

    @Transactional
    public MunicipioDTO cambiarEstado(Long id, Boolean activo) {
        Municipio municipio = buscarPorId(id);

        municipioValidator.validarCambioEstado(municipio, activo);

        municipio.setActivo(activo);

        return municipioMapper.convertirADTO(municipioRepository.save(municipio));
    }

    @Transactional
    public void eliminar(Long id) {
        Municipio municipio = buscarPorIdActivo(id);

        // Desactivación lógica: se conserva porque puede estar asociado a barrios y personas.
        municipio.setActivo(false);

        municipioRepository.save(municipio);
    }

    private Departamento obtenerDepartamentoActivo(Long departamentoId) {
        if (departamentoId == null) {
            throw new BusinessException("El departamento es obligatorio");
        }

        return departamentoRepository.findByIdAndActivoTrue(departamentoId)
                .orElseThrow(() -> new BusinessException(
                        "Departamento no encontrado o inactivo con id: " + departamentoId));
    }

    private void obtenerDepartamentoExistente(Long departamentoId) {
        if (departamentoId == null) {
            throw new BusinessException("El departamento es obligatorio");
        }

        departamentoRepository.findById(departamentoId)
                .orElseThrow(() -> new BusinessException(
                        "Departamento no encontrado con id: " + departamentoId));
    }

    private Municipio buscarPorId(Long id) {
        if (id == null) {
            throw new BusinessException("El id del municipio es obligatorio");
        }

        return municipioRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Municipio no encontrado con id: " + id));
    }

    private Municipio buscarPorIdActivo(Long id) {
        if (id == null) {
            throw new BusinessException("El id del municipio es obligatorio");
        }

        return municipioRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new BusinessException(
                        "Municipio no encontrado o inactivo con id: " + id));
    }
}