package co.edu.ufps.legal_cases.business.service.proceso;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.dto.proceso.EspecialidadDTO;
import co.edu.ufps.legal_cases.business.model.proceso.Especialidad;
import co.edu.ufps.legal_cases.business.model.proceso.OrganoControl;
import co.edu.ufps.legal_cases.business.repository.proceso.EspecialidadRepository;
import co.edu.ufps.legal_cases.business.repository.proceso.OrganoControlRepository;
import co.edu.ufps.legal_cases.business.service.proceso.catalogo.EspecialidadMapper;
import co.edu.ufps.legal_cases.business.service.proceso.catalogo.EspecialidadValidator;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

@Service
public class EspecialidadService {

    private final EspecialidadRepository especialidadRepository;
    private final OrganoControlRepository organoControlRepository;
    private final EspecialidadMapper especialidadMapper;
    private final EspecialidadValidator especialidadValidator;

    public EspecialidadService(
            EspecialidadRepository especialidadRepository,
            OrganoControlRepository organoControlRepository,
            EspecialidadMapper especialidadMapper,
            EspecialidadValidator especialidadValidator) {
        this.especialidadRepository = especialidadRepository;
        this.organoControlRepository = organoControlRepository;
        this.especialidadMapper = especialidadMapper;
        this.especialidadValidator = especialidadValidator;
    }

    // Lista especialidades activas para formularios y combos del frontend.
    @Transactional(readOnly = true)
    public List<EspecialidadDTO> listar() {
        return especialidadRepository.findByActivoTrueOrderByNombreAsc()
                .stream()
                .map(especialidadMapper::convertirADTO)
                .toList();
    }

    // Lista activas e inactivas para administración del catálogo.
    @Transactional(readOnly = true)
    public List<EspecialidadDTO> listarTodos() {
        return especialidadRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Especialidad::getNombre, String.CASE_INSENSITIVE_ORDER))
                .map(especialidadMapper::convertirADTO)
                .toList();
    }

    // Carga solo especialidades activas de un órgano activo.
    // Así el formulario no permite seleccionar relaciones inactivas.
    @Transactional(readOnly = true)
    public List<EspecialidadDTO> listarPorOrganoControl(Long organoControlId) {
        obtenerOrganoControlActivo(organoControlId);

        return especialidadRepository.findByOrganoControlIdAndActivoTrueOrderByNombreAsc(organoControlId)
                .stream()
                .map(especialidadMapper::convertirADTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public EspecialidadDTO obtenerPorId(Long id) {
        Especialidad especialidad = buscarPorIdActivo(id);
        return especialidadMapper.convertirADTO(especialidad);
    }

    @Transactional
    public EspecialidadDTO crear(EspecialidadDTO dto) {
        especialidadValidator.validarCreacion(dto);

        String nombre = especialidadValidator.normalizarNombre(dto.getNombre());
        OrganoControl organoControl = obtenerOrganoControlActivo(dto.getOrganoControlId());

        especialidadValidator.validarNombreDisponible(nombre, organoControl.getId());

        Especialidad especialidad = especialidadMapper.crearEntidad(nombre, organoControl);

        return especialidadMapper.convertirADTO(especialidadRepository.save(especialidad));
    }

    @Transactional
    public EspecialidadDTO actualizar(Long id, EspecialidadDTO dto) {
        Especialidad especialidad = buscarPorId(id);

        especialidadValidator.validarActualizacion(id, dto);

        String nombreNuevo = especialidadValidator.normalizarNombre(dto.getNombre());
        OrganoControl organoControlNuevo = obtenerOrganoControlActivo(dto.getOrganoControlId());

        especialidadValidator.validarNombreDisponibleParaActualizacion(
                nombreNuevo,
                organoControlNuevo.getId(),
                id);

        especialidadValidator.validarExistenCambios(
                especialidad,
                nombreNuevo,
                organoControlNuevo);

        // Actualizar datos del catálogo no debe cambiar activo.
        // Para eso existe cambiarEstado().
        especialidadMapper.aplicarDatos(
                especialidad,
                nombreNuevo,
                organoControlNuevo);

        return especialidadMapper.convertirADTO(especialidadRepository.save(especialidad));
    }

    @Transactional
    public EspecialidadDTO cambiarEstado(Long id, Boolean activo) {
        Especialidad especialidad = buscarPorId(id);

        especialidadValidator.validarCambioEstado(especialidad, activo);

        especialidad.setActivo(activo);

        return especialidadMapper.convertirADTO(especialidadRepository.save(especialidad));
    }

    @Transactional
    public void eliminar(Long id) {
        Especialidad especialidad = buscarPorIdActivo(id);

        // Se desactiva para conservar procesos históricos que ya usan esta especialidad.
        especialidad.setActivo(false);

        especialidadRepository.save(especialidad);
    }

    private OrganoControl obtenerOrganoControlActivo(Long organoControlId) {
        if (organoControlId == null) {
            throw new BusinessException("El órgano de control es obligatorio");
        }

        return organoControlRepository.findByIdAndActivoTrue(organoControlId)
                .orElseThrow(() -> new BusinessException(
                        "Órgano de control no encontrado o inactivo con id: " + organoControlId));
    }

    private Especialidad buscarPorId(Long id) {
        if (id == null) {
            throw new BusinessException("El id de la especialidad es obligatorio");
        }

        return especialidadRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Especialidad no encontrada con id: " + id));
    }

    private Especialidad buscarPorIdActivo(Long id) {
        if (id == null) {
            throw new BusinessException("El id de la especialidad es obligatorio");
        }

        return especialidadRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new BusinessException("Especialidad no encontrada o inactiva con id: " + id));
    }
}