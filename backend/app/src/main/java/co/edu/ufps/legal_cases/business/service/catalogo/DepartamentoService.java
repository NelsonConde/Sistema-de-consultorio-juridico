package co.edu.ufps.legal_cases.business.service.catalogo;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.dto.catalogo.DepartamentoDTO;
import co.edu.ufps.legal_cases.business.model.catalogo.Departamento;
import co.edu.ufps.legal_cases.business.repository.catalogo.DepartamentoRepository;
import co.edu.ufps.legal_cases.business.service.catalogo.departamento.DepartamentoMapper;
import co.edu.ufps.legal_cases.business.service.catalogo.departamento.DepartamentoValidator;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

@Service
public class DepartamentoService {

    private final DepartamentoRepository departamentoRepository;
    private final DepartamentoMapper departamentoMapper;
    private final DepartamentoValidator departamentoValidator;

    public DepartamentoService(
            DepartamentoRepository departamentoRepository,
            DepartamentoMapper departamentoMapper,
            DepartamentoValidator departamentoValidator) {
        this.departamentoRepository = departamentoRepository;
        this.departamentoMapper = departamentoMapper;
        this.departamentoValidator = departamentoValidator;
    }

    // Lista departamentos activos para formularios y selects del frontend.
    @Transactional(readOnly = true)
    public List<DepartamentoDTO> listar() {
        return departamentoRepository.findByActivoTrueOrderByNombreAsc()
                .stream()
                .map(departamentoMapper::convertirADTO)
                .toList();
    }

    // Lista todos los departamentos para administración del catálogo.
    @Transactional(readOnly = true)
    public List<DepartamentoDTO> listarTodos() {
        return departamentoRepository.findAllByOrderByNombreAsc()
                .stream()
                .map(departamentoMapper::convertirADTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public DepartamentoDTO obtenerPorId(Long id) {
        Departamento departamento = buscarPorIdActivo(id);
        return departamentoMapper.convertirADTO(departamento);
    }

    @Transactional
    public DepartamentoDTO crear(DepartamentoDTO dto) {
        departamentoValidator.validarCreacion(dto);

        String nombre = departamentoValidator.normalizarNombre(dto.getNombre());

        departamentoValidator.validarNombreDisponible(nombre);

        Departamento departamento = departamentoMapper.crearEntidad(nombre);

        return departamentoMapper.convertirADTO(departamentoRepository.save(departamento));
    }

    @Transactional
    public DepartamentoDTO actualizar(Long id, DepartamentoDTO dto) {
        Departamento departamento = buscarPorId(id);

        departamentoValidator.validarActualizacion(id, dto);

        String nombreNuevo = departamentoValidator.normalizarNombre(dto.getNombre());

        departamentoValidator.validarNombreDisponibleParaActualizacion(nombreNuevo, departamento.getId());
        departamentoValidator.validarExistenCambios(departamento, nombreNuevo);

        departamentoMapper.aplicarDatos(departamento, nombreNuevo);

        return departamentoMapper.convertirADTO(departamentoRepository.save(departamento));
    }

    @Transactional
    public void eliminar(Long id) {
        Departamento departamento = buscarPorIdActivo(id);

        // Desactivación lógica: se conserva porque puede estar asociado a procesos u otros registros.
        departamento.setActivo(false);

        departamentoRepository.save(departamento);
    }

    @Transactional
    public DepartamentoDTO cambiarEstado(Long id, Boolean activo) {
        Departamento departamento = buscarPorId(id);

        departamentoValidator.validarCambioEstado(departamento, activo);

        departamento.setActivo(activo);

        return departamentoMapper.convertirADTO(departamentoRepository.save(departamento));
    }

    private Departamento buscarPorId(Long id) {
        if (id == null) {
            throw new BusinessException("El id del departamento es obligatorio");
        }

        return departamentoRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Departamento no encontrado con id: " + id));
    }

    private Departamento buscarPorIdActivo(Long id) {
        if (id == null) {
            throw new BusinessException("El id del departamento es obligatorio");
        }

        return departamentoRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new BusinessException("Departamento no encontrado o inactivo con id: " + id));
    }
}