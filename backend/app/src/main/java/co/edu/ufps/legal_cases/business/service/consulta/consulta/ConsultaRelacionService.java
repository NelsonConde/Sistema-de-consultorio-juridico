package co.edu.ufps.legal_cases.business.service.consulta.consulta;

import java.util.List;

import org.springframework.stereotype.Service;

import co.edu.ufps.legal_cases.business.model.catalogo.Area;
import co.edu.ufps.legal_cases.business.model.catalogo.Sede;
import co.edu.ufps.legal_cases.business.model.catalogo.Tema;
import co.edu.ufps.legal_cases.business.model.catalogo.Tipo;
import co.edu.ufps.legal_cases.business.model.perfil.Asesor;
import co.edu.ufps.legal_cases.business.model.perfil.Estudiante;
import co.edu.ufps.legal_cases.business.model.perfil.Monitor;
import co.edu.ufps.legal_cases.business.model.persona.Persona;
import co.edu.ufps.legal_cases.business.repository.catalogo.AreaRepository;
import co.edu.ufps.legal_cases.business.repository.catalogo.SedeRepository;
import co.edu.ufps.legal_cases.business.repository.catalogo.TemaRepository;
import co.edu.ufps.legal_cases.business.repository.catalogo.TipoRepository;
import co.edu.ufps.legal_cases.business.repository.perfil.AsesorRepository;
import co.edu.ufps.legal_cases.business.repository.perfil.EstudianteRepository;
import co.edu.ufps.legal_cases.business.repository.perfil.MonitorRepository;
import co.edu.ufps.legal_cases.business.repository.persona.PersonaRepository;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import lombok.AllArgsConstructor;

// Centraliza la resolución de relaciones activas usadas por Consulta.
// Mantiene fuera del CommandService los lookups de catálogos, perfiles y personas.
@Service
@AllArgsConstructor
public class ConsultaRelacionService {

    private final PersonaRepository personaRepository;
    private final SedeRepository sedeRepository;
    private final AreaRepository areaRepository;
    private final TemaRepository temaRepository;
    private final TipoRepository tipoRepository;
    private final AsesorRepository asesorRepository;
    private final MonitorRepository monitorRepository;
    private final EstudianteRepository estudianteRepository;

    public Persona obtenerPersona(Long id) {
        if (id == null) {
            throw new BusinessException("La persona es obligatoria");
        }

        return personaRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new BusinessException("Persona no encontrada o inactiva con id: " + id));
    }

    public List<Persona> obtenerPersonas(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        return ids.stream()
                .map(this::obtenerPersona)
                .toList();
    }

    public Sede obtenerSede(Long id) {
        if (id == null) {
            throw new BusinessException("La sede es obligatoria");
        }

        return sedeRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new BusinessException("Sede no encontrada o inactiva con id: " + id));
    }

    public Area obtenerArea(Long id) {
        if (id == null) {
            throw new BusinessException("El área es obligatoria");
        }

        return areaRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new BusinessException("Área no encontrada o inactiva con id: " + id));
    }

    public Tema obtenerTema(Long id) {
        if (id == null) {
            throw new BusinessException("El tema es obligatorio");
        }

        return temaRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new BusinessException("Tema no encontrado o inactivo con id: " + id));
    }

    public Tipo obtenerTipo(Long id) {
        if (id == null) {
            return null;
        }

        return tipoRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new BusinessException("Tipo no encontrado o inactivo con id: " + id));
    }

    public Asesor obtenerAsesor(Long id) {
        if (id == null) {
            return null;
        }

        return asesorRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new BusinessException("Asesor no encontrado o inactivo con id: " + id));
    }

    public Monitor obtenerMonitor(Long id) {
        if (id == null) {
            return null;
        }

        return monitorRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new BusinessException("Monitor no encontrado o inactivo con id: " + id));
    }

    public Estudiante obtenerEstudiante(Long id) {
        if (id == null) {
            return null;
        }

        return estudianteRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new BusinessException("Estudiante no encontrado o inactivo con id: " + id));
    }

    public Asesor obtenerAsesorDelEstudianteActivo(Estudiante estudiante) {
        if (estudiante == null || estudiante.getAsesor() == null) {
            throw new BusinessException("El estudiante seleccionado no tiene asesor asignado");
        }

        return asesorRepository.findByIdAndActivoTrue(estudiante.getAsesor().getId())
                .orElseThrow(() -> new BusinessException(
                        "El asesor asignado al estudiante no existe o está inactivo"));
    }
}