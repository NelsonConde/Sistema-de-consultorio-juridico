package co.edu.ufps.legal_cases.business.service.seguimiento;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.dto.seguimiento.CategoriaSeguimientoDTO;
import co.edu.ufps.legal_cases.business.model.seguimiento.CategoriaSeguimiento;
import co.edu.ufps.legal_cases.business.repository.seguimiento.CategoriaSeguimientoRepository;
import co.edu.ufps.legal_cases.business.service.seguimiento.catalogo.CategoriaSeguimientoMapper;
import co.edu.ufps.legal_cases.business.service.seguimiento.catalogo.CategoriaSeguimientoValidator;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

@Service
public class CategoriaSeguimientoService {

    private final CategoriaSeguimientoRepository categoriaSeguimientoRepository;
    private final CategoriaSeguimientoMapper categoriaSeguimientoMapper;
    private final CategoriaSeguimientoValidator categoriaSeguimientoValidator;

    public CategoriaSeguimientoService(
            CategoriaSeguimientoRepository categoriaSeguimientoRepository,
            CategoriaSeguimientoMapper categoriaSeguimientoMapper,
            CategoriaSeguimientoValidator categoriaSeguimientoValidator) {
        this.categoriaSeguimientoRepository = categoriaSeguimientoRepository;
        this.categoriaSeguimientoMapper = categoriaSeguimientoMapper;
        this.categoriaSeguimientoValidator = categoriaSeguimientoValidator;
    }

    // Lista todas las categorías para administración, incluyendo inactivas.
    @Transactional(readOnly = true)
    public List<CategoriaSeguimientoDTO> listar() {
        return categoriaSeguimientoRepository.findAll()
                .stream()
                .map(categoriaSeguimientoMapper::convertirADTO)
                .toList();
    }

    // Lista solo categorías activas para formularios de seguimiento.
    @Transactional(readOnly = true)
    public List<CategoriaSeguimientoDTO> listarActivas() {
        return categoriaSeguimientoRepository.findByActivoTrueOrderByNombreAsc()
                .stream()
                .map(categoriaSeguimientoMapper::convertirADTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoriaSeguimientoDTO obtenerPorId(Long id) {
        CategoriaSeguimiento categoria = buscarPorId(id);
        return categoriaSeguimientoMapper.convertirADTO(categoria);
    }

    @Transactional
    public CategoriaSeguimientoDTO crear(CategoriaSeguimientoDTO dto) {
        categoriaSeguimientoValidator.validarCreacion(dto);

        String nombre = categoriaSeguimientoValidator.normalizarNombre(dto.getNombre());

        categoriaSeguimientoValidator.validarNombreDisponible(nombre);

        CategoriaSeguimiento categoria = categoriaSeguimientoMapper.crearEntidad(nombre);

        return categoriaSeguimientoMapper.convertirADTO(categoriaSeguimientoRepository.save(categoria));
    }

    @Transactional
    public CategoriaSeguimientoDTO actualizar(Long id, CategoriaSeguimientoDTO dto) {
        CategoriaSeguimiento categoria = buscarPorId(id);

        categoriaSeguimientoValidator.validarActualizacion(id, dto);

        String nombreNuevo = categoriaSeguimientoValidator.normalizarNombre(dto.getNombre());

        categoriaSeguimientoValidator.validarNombreDisponibleParaActualizacion(nombreNuevo, id);
        categoriaSeguimientoValidator.validarExistenCambios(categoria, nombreNuevo);

        // Actualizar datos del catálogo no debe cambiar activo.
        // Para eso existe cambiarEstado().
        categoriaSeguimientoMapper.aplicarDatos(categoria, nombreNuevo);

        return categoriaSeguimientoMapper.convertirADTO(categoriaSeguimientoRepository.save(categoria));
    }

    @Transactional
    public CategoriaSeguimientoDTO cambiarEstado(Long id, Boolean activo) {
        CategoriaSeguimiento categoria = buscarPorId(id);

        categoriaSeguimientoValidator.validarCambioEstado(categoria, activo);

        categoria.setActivo(activo);

        return categoriaSeguimientoMapper.convertirADTO(categoriaSeguimientoRepository.save(categoria));
    }

    @Transactional
    public void eliminar(Long id) {
        CategoriaSeguimiento categoria = buscarPorId(id);

        categoriaSeguimientoValidator.validarCambioEstado(categoria, false);

        // Desactivación lógica: se conserva porque puede estar asociada a seguimientos existentes.
        categoria.setActivo(false);

        categoriaSeguimientoRepository.save(categoria);
    }

    private CategoriaSeguimiento buscarPorId(Long id) {
        if (id == null) {
            throw new BusinessException("El id de la categoría es obligatorio");
        }

        return categoriaSeguimientoRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "Categoría de seguimiento no encontrada con id: " + id));
    }
}