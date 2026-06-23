package co.edu.ufps.legal_cases.business.service.conciliacion.conciliacion;

import org.springframework.stereotype.Service;

import co.edu.ufps.legal_cases.business.model.conciliacion.Conciliacion;
import co.edu.ufps.legal_cases.business.model.conciliacion.EstadoConciliacion;
import co.edu.ufps.legal_cases.business.model.conciliacion.EstadoConciliacionCodigo;
import co.edu.ufps.legal_cases.business.model.consulta.Consulta;
import co.edu.ufps.legal_cases.business.model.perfil.Conciliador;
import co.edu.ufps.legal_cases.business.model.perfil.Estudiante;
import co.edu.ufps.legal_cases.business.repository.conciliacion.ConciliacionRepository;
import co.edu.ufps.legal_cases.business.repository.conciliacion.EstadoConciliacionRepository;
import co.edu.ufps.legal_cases.business.repository.consulta.ConsultaRepository;
import co.edu.ufps.legal_cases.business.repository.perfil.ConciliadorRepository;
import co.edu.ufps.legal_cases.business.repository.perfil.EstudianteRepository;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import lombok.AllArgsConstructor;

// Centraliza la resolución de relaciones usadas por Conciliacion.
// Mantiene fuera del CommandService los lookups de consulta, estudiante, conciliador y estado.
@Service
@AllArgsConstructor
public class ConciliacionRelacionService {

    private final ConsultaRepository consultaRepository;
    private final ConciliacionRepository conciliacionRepository;
    private final EstudianteRepository estudianteRepository;
    private final ConciliadorRepository conciliadorRepository;
    private final EstadoConciliacionRepository estadoConciliacionRepository;

    public Consulta obtenerConsulta(Long id) {
        if (id == null) {
            throw new BusinessException("La consulta es obligatoria");
        }

        return consultaRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Consulta no encontrada con id: " + id));
    }

    public Conciliacion obtenerConciliacionActiva(Long id) {
        if (id == null) {
            throw new BusinessException("La conciliación es obligatoria");
        }

        return conciliacionRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new BusinessException("Conciliación no encontrada con id: " + id));
    }

    public Estudiante obtenerEstudianteActivo(Long id) {
        if (id == null) {
            throw new BusinessException("El estudiante es obligatorio");
        }

        return estudianteRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new BusinessException("Estudiante no encontrado o inactivo con id: " + id));
    }

    public Conciliador obtenerConciliadorActivo(Long id) {
        if (id == null) {
            throw new BusinessException("El conciliador es obligatorio");
        }

        return conciliadorRepository.findById(id)
                .filter(conciliador -> Boolean.TRUE.equals(conciliador.getActivo()))
                .orElseThrow(() -> new BusinessException("Conciliador no encontrado o inactivo con id: " + id));
    }

    public EstadoConciliacion obtenerEstadoActivoPorCodigo(String codigo) {
        String codigoNormalizado = EstadoConciliacionCodigo.normalizar(codigo);

        if (codigoNormalizado == null || codigoNormalizado.isBlank()) {
            throw new BusinessException("El estado de conciliación es obligatorio");
        }

        return estadoConciliacionRepository.findByCodigoAndActivoTrue(codigoNormalizado)
                .orElseThrow(() -> new BusinessException(
                        "Estado de conciliación no encontrado o inactivo: " + codigoNormalizado));
    }
}