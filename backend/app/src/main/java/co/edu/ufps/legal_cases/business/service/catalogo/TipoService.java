package co.edu.ufps.legal_cases.business.service.catalogo;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.dto.catalogo.TipoDTO;
import co.edu.ufps.legal_cases.business.model.catalogo.Tema;
import co.edu.ufps.legal_cases.business.model.catalogo.Tipo;
import co.edu.ufps.legal_cases.business.repository.catalogo.TemaRepository;
import co.edu.ufps.legal_cases.business.repository.catalogo.TipoRepository;
import co.edu.ufps.legal_cases.business.service.catalogo.tipo.TipoMapper;
import co.edu.ufps.legal_cases.business.service.catalogo.tipo.TipoValidator;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

@Service
public class TipoService {

    private final TipoRepository tipoRepository;
    private final TemaRepository temaRepository;
    private final TipoMapper tipoMapper;
    private final TipoValidator tipoValidator;

    public TipoService(
            TipoRepository tipoRepository,
            TemaRepository temaRepository,
            TipoMapper tipoMapper,
            TipoValidator tipoValidator) {
        this.tipoRepository = tipoRepository;
        this.temaRepository = temaRepository;
        this.tipoMapper = tipoMapper;
        this.tipoValidator = tipoValidator;
    }

    // Lista tipos activos para formularios, selects y uso normal del sistema.
    @Transactional(readOnly = true)
    public List<TipoDTO> listar() {
        return tipoRepository.findByActivoTrueOrderByNombreAsc()
                .stream()
                .map(tipoMapper::convertirADTO)
                .toList();
    }

    // Lista todos los tipos para administración del catálogo.
    @Transactional(readOnly = true)
    public List<TipoDTO> listarTodos() {
        return tipoRepository.findAllByOrderByNombreAsc()
                .stream()
                .map(tipoMapper::convertirADTO)
                .toList();
    }

    // Lista tipos activos de un tema activo.
    @Transactional(readOnly = true)
    public List<TipoDTO> listarPorTema(Long temaId) {
        validarTemaActivo(temaId);

        return tipoRepository.findByTemaIdAndActivoTrueOrderByNombreAsc(temaId)
                .stream()
                .map(tipoMapper::convertirADTO)
                .toList();
    }

    // Lista todos los tipos de un tema para administración.
    @Transactional(readOnly = true)
    public List<TipoDTO> listarTodosPorTema(Long temaId) {
        validarTemaExistente(temaId);

        return tipoRepository.findByTemaIdOrderByNombreAsc(temaId)
                .stream()
                .map(tipoMapper::convertirADTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public TipoDTO obtenerPorId(Long id) {
        Tipo tipo = buscarPorIdActivo(id);
        return tipoMapper.convertirADTO(tipo);
    }

    @Transactional
    public TipoDTO crear(TipoDTO dto) {
        tipoValidator.validarCreacion(dto);

        String nombre = tipoValidator.normalizarNombre(dto.getNombre());
        Tema tema = obtenerTemaActivo(dto.getTemaId());

        tipoValidator.validarNombreDisponible(nombre, tema.getId());

        Tipo tipo = tipoMapper.crearEntidad(nombre, tema);

        return tipoMapper.convertirADTO(tipoRepository.save(tipo));
    }

    @Transactional
    public TipoDTO actualizar(Long id, TipoDTO dto) {
        Tipo tipo = buscarPorId(id);

        tipoValidator.validarActualizacion(id, dto);

        String nombreNuevo = tipoValidator.normalizarNombre(dto.getNombre());
        Tema temaNuevo = obtenerTemaActivo(dto.getTemaId());

        tipoValidator.validarNombreDisponibleParaActualizacion(
                nombreNuevo,
                temaNuevo.getId(),
                tipo.getId());

        tipoValidator.validarExistenCambios(tipo, nombreNuevo, temaNuevo);

        tipoMapper.aplicarDatos(tipo, nombreNuevo, temaNuevo);

        return tipoMapper.convertirADTO(tipoRepository.save(tipo));
    }

    @Transactional
    public TipoDTO cambiarEstado(Long id, Boolean activo) {
        Tipo tipo = buscarPorId(id);

        tipoValidator.validarCambioEstado(tipo, activo);

        tipo.setActivo(activo);

        return tipoMapper.convertirADTO(tipoRepository.save(tipo));
    }

    @Transactional
    public void eliminar(Long id) {
        Tipo tipo = buscarPorIdActivo(id);

        // Desactivación lógica: se conserva porque puede estar asociado a consultas.
        tipo.setActivo(false);

        tipoRepository.save(tipo);
    }

    private void validarTemaActivo(Long temaId) {
        obtenerTemaActivo(temaId);
    }

    private void validarTemaExistente(Long temaId) {
        if (temaId == null) {
            throw new BusinessException("El tema es obligatorio");
        }

        temaRepository.findById(temaId)
                .orElseThrow(() -> new BusinessException("Tema no encontrado con id: " + temaId));
    }

    private Tema obtenerTemaActivo(Long temaId) {
        if (temaId == null) {
            throw new BusinessException("El tema es obligatorio");
        }

        return temaRepository.findByIdAndActivoTrue(temaId)
                .orElseThrow(() -> new BusinessException("Tema no encontrado o inactivo con id: " + temaId));
    }

    private Tipo buscarPorId(Long id) {
        if (id == null) {
            throw new BusinessException("El id del tipo es obligatorio");
        }

        return tipoRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Tipo no encontrado con id: " + id));
    }

    private Tipo buscarPorIdActivo(Long id) {
        if (id == null) {
            throw new BusinessException("El id del tipo es obligatorio");
        }

        return tipoRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new BusinessException("Tipo no encontrado o inactivo con id: " + id));
    }
}