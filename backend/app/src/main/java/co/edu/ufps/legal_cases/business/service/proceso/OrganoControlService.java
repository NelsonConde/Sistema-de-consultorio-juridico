package co.edu.ufps.legal_cases.business.service.proceso;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.dto.proceso.OrganoControlDTO;
import co.edu.ufps.legal_cases.business.model.proceso.OrganoControl;
import co.edu.ufps.legal_cases.business.repository.proceso.OrganoControlRepository;
import co.edu.ufps.legal_cases.business.service.proceso.catalogo.OrganoControlMapper;
import co.edu.ufps.legal_cases.business.service.proceso.catalogo.OrganoControlValidator;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

@Service
public class OrganoControlService {

    private final OrganoControlRepository organoControlRepository;
    private final OrganoControlMapper organoControlMapper;
    private final OrganoControlValidator organoControlValidator;

    public OrganoControlService(
            OrganoControlRepository organoControlRepository,
            OrganoControlMapper organoControlMapper,
            OrganoControlValidator organoControlValidator) {
        this.organoControlRepository = organoControlRepository;
        this.organoControlMapper = organoControlMapper;
        this.organoControlValidator = organoControlValidator;
    }

    // Lista órganos activos para formularios y combos de procesos.
    @Transactional(readOnly = true)
    public List<OrganoControlDTO> listar() {
        return organoControlRepository.findByActivoTrueOrderByNombreAsc()
                .stream()
                .map(organoControlMapper::convertirADTO)
                .toList();
    }

    // Lista activos e inactivos para administración del catálogo.
    @Transactional(readOnly = true)
    public List<OrganoControlDTO> listarTodos() {
        return organoControlRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(OrganoControl::getNombre, String.CASE_INSENSITIVE_ORDER))
                .map(organoControlMapper::convertirADTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrganoControlDTO obtenerPorId(Long id) {
        OrganoControl organoControl = buscarPorIdActivo(id);
        return organoControlMapper.convertirADTO(organoControl);
    }

    @Transactional
    public OrganoControlDTO crear(OrganoControlDTO dto) {
        organoControlValidator.validarCreacion(dto);

        String nombre = organoControlValidator.normalizarNombre(dto.getNombre());

        organoControlValidator.validarNombreDisponible(nombre);

        OrganoControl organoControl = organoControlMapper.crearEntidad(nombre);

        return organoControlMapper.convertirADTO(organoControlRepository.save(organoControl));
    }

    @Transactional
    public OrganoControlDTO actualizar(Long id, OrganoControlDTO dto) {
        OrganoControl organoControl = buscarPorId(id);

        organoControlValidator.validarActualizacion(id, dto);

        String nombreNuevo = organoControlValidator.normalizarNombre(dto.getNombre());

        organoControlValidator.validarNombreDisponibleParaActualizacion(nombreNuevo, id);
        organoControlValidator.validarExistenCambios(organoControl, nombreNuevo);

        // Actualizar datos del catálogo no debe cambiar activo.
        // Para eso existe cambiarEstado().
        organoControlMapper.aplicarDatos(organoControl, nombreNuevo);

        return organoControlMapper.convertirADTO(organoControlRepository.save(organoControl));
    }

    @Transactional
    public OrganoControlDTO cambiarEstado(Long id, Boolean activo) {
        OrganoControl organoControl = buscarPorId(id);

        organoControlValidator.validarCambioEstado(organoControl, activo);

        if (Boolean.FALSE.equals(activo)) {
            organoControlValidator.validarPuedeDesactivarse(id);
        }

        organoControl.setActivo(activo);

        return organoControlMapper.convertirADTO(organoControlRepository.save(organoControl));
    }

    @Transactional
    public void eliminar(Long id) {
        OrganoControl organoControl = buscarPorIdActivo(id);

        organoControlValidator.validarPuedeDesactivarse(id);

        // Se desactiva para conservar el catálogo usado por procesos históricos.
        organoControl.setActivo(false);

        organoControlRepository.save(organoControl);
    }

    private OrganoControl buscarPorId(Long id) {
        if (id == null) {
            throw new BusinessException("El id del órgano de control es obligatorio");
        }

        return organoControlRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Órgano de control no encontrado con id: " + id));
    }

    private OrganoControl buscarPorIdActivo(Long id) {
        if (id == null) {
            throw new BusinessException("El id del órgano de control es obligatorio");
        }

        return organoControlRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new BusinessException("Órgano de control no encontrado o inactivo con id: " + id));
    }
}