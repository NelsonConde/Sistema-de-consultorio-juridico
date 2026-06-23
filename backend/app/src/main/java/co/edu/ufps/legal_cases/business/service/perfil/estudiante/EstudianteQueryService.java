package co.edu.ufps.legal_cases.business.service.perfil.estudiante;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.dto.perfil.EstudianteDTO;
import co.edu.ufps.legal_cases.business.model.perfil.Estudiante;
import co.edu.ufps.legal_cases.business.repository.perfil.EstudianteRepository;
import co.edu.ufps.legal_cases.business.service.acceso.perfil.EstudianteAccessService;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

// Servicio que maneja las consultas y usa el servicio de acceso para validar permisos
@Service
public class EstudianteQueryService {

    private final EstudianteRepository estudianteRepository;
    private final EstudianteAccessService estudianteAccessService;
    private final EstudianteMapper estudianteMapper;

    public EstudianteQueryService(
            EstudianteRepository estudianteRepository,
            EstudianteAccessService estudianteAccessService,
            EstudianteMapper estudianteMapper) {
        this.estudianteRepository = estudianteRepository;
        this.estudianteAccessService = estudianteAccessService;
        this.estudianteMapper = estudianteMapper;
    }

    @Transactional(readOnly = true)
    public List<EstudianteDTO> listar() {
        estudianteAccessService.validarPuedeListarEstudiantes();

        if (estudianteAccessService.puedeVerTodosLosEstudiantes()) {
            return estudianteRepository.findAll()
                    .stream()
                    .map(estudianteMapper::convertirADTO)
                    .toList();
        }

        if (estudianteAccessService.usuarioEsAsesor()) {
            return estudianteRepository.findByAsesorIdAndActivoTrue(estudianteAccessService.obtenerAsesorActualId())
                    .stream()
                    .map(estudianteMapper::convertirADTO)
                    .toList();
        }

        return List.of();
    }

    @Transactional(readOnly = true)
    public List<EstudianteDTO> listarActivos() {
        estudianteAccessService.validarPuedeListarEstudiantes();

        if (estudianteAccessService.puedeVerTodosLosEstudiantes()) {
            return estudianteRepository.findByActivoTrueOrderByNombreAsc()
                    .stream()
                    .map(estudianteMapper::convertirADTO)
                    .toList();
        }

        if (estudianteAccessService.usuarioEsAsesor()) {
            return estudianteRepository.findByAsesorIdAndActivoTrue(estudianteAccessService.obtenerAsesorActualId())
                    .stream()
                    .map(estudianteMapper::convertirADTO)
                    .toList();
        }

        return List.of();
    }

    @Transactional(readOnly = true)
    public List<EstudianteDTO> listarConConciliacion() {
        estudianteAccessService.validarPuedeListarEstudiantes();

        return estudianteRepository.findByConciliacionTrueAndActivoTrue()
                .stream()
                .filter(estudianteAccessService::puedeVerEstudiante)
                .map(estudianteMapper::convertirADTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EstudianteDTO> listarPorAsesor(Long asesorId) {
        estudianteAccessService.validarPuedeListarEstudiantesPorAsesor(asesorId);

        return estudianteRepository.findByAsesorIdAndActivoTrue(asesorId)
                .stream()
                .map(estudianteMapper::convertirADTO)
                .toList();
    }

    // Solo estudiantes activos de un asesor específico.
    @Transactional(readOnly = true)
    public List<EstudianteDTO> listarActivosPorAsesor(Long asesorId) {
        estudianteAccessService.validarPuedeListarEstudiantesPorAsesor(asesorId);

        return estudianteRepository.findByAsesorIdAndActivoTrue(asesorId)
                .stream()
                .map(estudianteMapper::convertirADTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public EstudianteDTO obtenerPorId(Long id) {
        estudianteAccessService.validarPuedeVerEstudiante(id);

        Estudiante estudiante = estudianteRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Estudiante no encontrado con id: " + id));

        return estudianteMapper.convertirADTO(estudiante);
    }
}