package co.edu.ufps.legal_cases.business.service.persona;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.dto.persona.EmpresaDTO;
import co.edu.ufps.legal_cases.business.model.persona.Empresa;
import co.edu.ufps.legal_cases.business.repository.persona.EmpresaRepository;
import co.edu.ufps.legal_cases.business.service.persona.empresa.EmpresaMapper;
import co.edu.ufps.legal_cases.business.service.persona.empresa.EmpresaValidator;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

@Service
public class EmpresaService {

    private final EmpresaRepository empresaRepository;
    private final EmpresaMapper empresaMapper;
    private final EmpresaValidator empresaValidator;

    public EmpresaService(
            EmpresaRepository empresaRepository,
            EmpresaMapper empresaMapper,
            EmpresaValidator empresaValidator) {
        this.empresaRepository = empresaRepository;
        this.empresaMapper = empresaMapper;
        this.empresaValidator = empresaValidator;
    }

    // Lista empresas activas para formularios, selects y uso normal.
    @Transactional(readOnly = true)
    public List<EmpresaDTO> listar() {
        return empresaRepository.findByActivoTrueOrderByNombreAsc()
                .stream()
                .map(empresaMapper::convertirADTO)
                .toList();
    }

    // Lista todas las empresas para administración del catálogo.
    @Transactional(readOnly = true)
    public List<EmpresaDTO> listarTodos() {
        return empresaRepository.findAllByOrderByNombreAsc()
                .stream()
                .map(empresaMapper::convertirADTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public EmpresaDTO obtenerPorId(Long id) {
        Empresa empresa = buscarPorIdActivo(id);
        return empresaMapper.convertirADTO(empresa);
    }

    @Transactional
    public EmpresaDTO crear(EmpresaDTO dto) {
        empresaValidator.validarCreacion(dto);

        String nombre = empresaValidator.normalizarNombre(dto.getNombre());

        empresaValidator.validarNombreDisponible(nombre);

        Empresa empresa = empresaMapper.crearEntidad(nombre);

        return empresaMapper.convertirADTO(empresaRepository.save(empresa));
    }

    @Transactional
    public EmpresaDTO actualizar(Long id, EmpresaDTO dto) {
        Empresa empresa = buscarPorId(id);

        empresaValidator.validarActualizacion(id, dto);

        String nombreNuevo = empresaValidator.normalizarNombre(dto.getNombre());

        empresaValidator.validarNombreDisponibleParaActualizacion(nombreNuevo, empresa.getId());
        empresaValidator.validarExistenCambios(empresa, nombreNuevo);

        empresaMapper.aplicarDatos(empresa, nombreNuevo);

        return empresaMapper.convertirADTO(empresaRepository.save(empresa));
    }

    @Transactional
    public EmpresaDTO cambiarEstado(Long id, Boolean activo) {
        Empresa empresa = buscarPorId(id);

        empresaValidator.validarCambioEstado(empresa, activo);

        empresa.setActivo(activo);

        return empresaMapper.convertirADTO(empresaRepository.save(empresa));
    }

    @Transactional
    public void eliminar(Long id) {
        Empresa empresa = buscarPorIdActivo(id);

        // Desactivación lógica: se conserva porque puede estar asociada a personas.
        empresa.setActivo(false);

        empresaRepository.save(empresa);
    }

    private Empresa buscarPorId(Long id) {
        if (id == null) {
            throw new BusinessException("El id de la empresa es obligatorio");
        }

        return empresaRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Empresa no encontrada con id: " + id));
    }

    private Empresa buscarPorIdActivo(Long id) {
        if (id == null) {
            throw new BusinessException("El id de la empresa es obligatorio");
        }

        return empresaRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new BusinessException(
                        "Empresa no encontrada o inactiva con id: " + id));
    }
}