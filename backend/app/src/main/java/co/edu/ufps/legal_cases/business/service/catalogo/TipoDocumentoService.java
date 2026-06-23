package co.edu.ufps.legal_cases.business.service.catalogo;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.dto.catalogo.TipoDocumentoDTO;
import co.edu.ufps.legal_cases.business.model.catalogo.TipoDocumento;
import co.edu.ufps.legal_cases.business.repository.catalogo.TipoDocumentoRepository;
import co.edu.ufps.legal_cases.business.service.catalogo.tipoDocumento.TipoDocumentoMapper;
import co.edu.ufps.legal_cases.business.service.catalogo.tipoDocumento.TipoDocumentoValidator;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

@Service
public class TipoDocumentoService {

    private final TipoDocumentoRepository tipoDocumentoRepository;
    private final TipoDocumentoMapper tipoDocumentoMapper;
    private final TipoDocumentoValidator tipoDocumentoValidator;

    public TipoDocumentoService(
            TipoDocumentoRepository tipoDocumentoRepository,
            TipoDocumentoMapper tipoDocumentoMapper,
            TipoDocumentoValidator tipoDocumentoValidator) {
        this.tipoDocumentoRepository = tipoDocumentoRepository;
        this.tipoDocumentoMapper = tipoDocumentoMapper;
        this.tipoDocumentoValidator = tipoDocumentoValidator;
    }

    // Lista todos los tipos de documento para administración del catálogo.
    @Transactional(readOnly = true)
    public List<TipoDocumentoDTO> listar() {
        return tipoDocumentoRepository.findAllByOrderByNombreAsc()
                .stream()
                .map(tipoDocumentoMapper::convertirADTO)
                .toList();
    }

    // Lista solo tipos de documento activos para formularios.
    @Transactional(readOnly = true)
    public List<TipoDocumentoDTO> listarActivos() {
        return tipoDocumentoRepository.findByActivoTrueOrderByNombreAsc()
                .stream()
                .map(tipoDocumentoMapper::convertirADTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public TipoDocumentoDTO obtenerPorId(Long id) {
        TipoDocumento tipoDocumento = buscarPorId(id);
        return tipoDocumentoMapper.convertirADTO(tipoDocumento);
    }

    @Transactional
    public TipoDocumentoDTO crear(TipoDocumentoDTO dto) {
        tipoDocumentoValidator.validarCreacion(dto);

        String nombre = tipoDocumentoValidator.normalizarNombre(dto);

        tipoDocumentoValidator.validarNombreDisponible(nombre);

        TipoDocumento tipoDocumento = tipoDocumentoMapper.crearEntidad(nombre);

        return tipoDocumentoMapper.convertirADTO(tipoDocumentoRepository.save(tipoDocumento));
    }

    @Transactional
    public TipoDocumentoDTO actualizar(Long id, TipoDocumentoDTO dto) {
        TipoDocumento tipoDocumento = buscarPorId(id);

        tipoDocumentoValidator.validarActualizacion(id, dto, tipoDocumento);

        String nombreNuevo = tipoDocumentoValidator.normalizarNombre(dto);

        tipoDocumentoValidator.validarExistenCambios(tipoDocumento, nombreNuevo);
        tipoDocumentoValidator.validarNombreDisponibleParaActualizacion(nombreNuevo, tipoDocumento.getId());

        // Actualizar datos del catálogo no debe cambiar activo.
        // Para eso existe cambiarEstado().
        tipoDocumentoMapper.aplicarDatos(tipoDocumento, nombreNuevo);

        return tipoDocumentoMapper.convertirADTO(tipoDocumentoRepository.save(tipoDocumento));
    }

    @Transactional
    public TipoDocumentoDTO cambiarEstado(Long id, Boolean activo) {
        TipoDocumento tipoDocumento = buscarPorId(id);

        tipoDocumentoValidator.validarCambioEstado(tipoDocumento, activo);

        tipoDocumento.setActivo(activo);

        return tipoDocumentoMapper.convertirADTO(tipoDocumentoRepository.save(tipoDocumento));
    }

    private TipoDocumento buscarPorId(Long id) {
        if (id == null) {
            throw new BusinessException("El id del tipo de documento es obligatorio");
        }

        return tipoDocumentoRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Tipo de documento no encontrado con id: " + id));
    }
}