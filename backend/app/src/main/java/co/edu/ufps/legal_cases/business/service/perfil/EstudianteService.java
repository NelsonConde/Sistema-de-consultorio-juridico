package co.edu.ufps.legal_cases.business.service.perfil;

import java.util.List;

import org.springframework.stereotype.Service;

import co.edu.ufps.legal_cases.business.dto.perfil.EstudianteDTO;
import co.edu.ufps.legal_cases.business.service.perfil.estudiante.EstudianteCommandService;
import co.edu.ufps.legal_cases.business.service.perfil.estudiante.EstudianteQueryService;

// Fachada del módulo de estudiantes.
// El controller entra por aquí, pero lectura y escritura quedan separadas por responsabilidad.
@Service
public class EstudianteService {

    private final EstudianteQueryService estudianteQueryService;
    private final EstudianteCommandService estudianteCommandService;

    public EstudianteService(
            EstudianteQueryService estudianteQueryService,
            EstudianteCommandService estudianteCommandService) {
        this.estudianteQueryService = estudianteQueryService;
        this.estudianteCommandService = estudianteCommandService;
    }

    public List<EstudianteDTO> listar() {
        return estudianteQueryService.listar();
    }

    public List<EstudianteDTO> listarActivos() {
        return estudianteQueryService.listarActivos();
    }

    public List<EstudianteDTO> listarConConciliacion() {
        return estudianteQueryService.listarConConciliacion();
    }

    public List<EstudianteDTO> listarPorAsesor(Long asesorId) {
        return estudianteQueryService.listarPorAsesor(asesorId);
    }

    // Solo estudiantes activos de un asesor específico.
    public List<EstudianteDTO> listarActivosPorAsesor(Long asesorId) {
        return estudianteQueryService.listarActivosPorAsesor(asesorId);
    }

    public EstudianteDTO obtenerPorId(Long id) {
        return estudianteQueryService.obtenerPorId(id);
    }

    public EstudianteDTO crear(EstudianteDTO dto) {
        return estudianteCommandService.crear(dto);
    }

    public EstudianteDTO actualizar(Long id, EstudianteDTO dto) {
        return estudianteCommandService.actualizar(id, dto);
    }

    public EstudianteDTO cambiarEstado(Long id, Boolean activo) {
        return estudianteCommandService.cambiarEstado(id, activo);
    }

    public EstudianteDTO cambiarConciliacion(Long id, Boolean conciliacion) {
        return estudianteCommandService.cambiarConciliacion(id, conciliacion);
    }

    public void eliminar(Long id) {
        estudianteCommandService.eliminar(id);
    }
}