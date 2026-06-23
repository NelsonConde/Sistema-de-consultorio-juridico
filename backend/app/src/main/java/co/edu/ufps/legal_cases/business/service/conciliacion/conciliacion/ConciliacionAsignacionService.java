package co.edu.ufps.legal_cases.business.service.conciliacion.conciliacion;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import co.edu.ufps.legal_cases.business.model.consulta.Consulta;
import co.edu.ufps.legal_cases.business.model.perfil.Conciliador;
import co.edu.ufps.legal_cases.business.model.perfil.Estudiante;
import co.edu.ufps.legal_cases.business.repository.conciliacion.ConciliacionRepository;
import co.edu.ufps.legal_cases.business.repository.perfil.ConciliadorRepository;
import co.edu.ufps.legal_cases.business.repository.perfil.EstudianteRepository;
import lombok.AllArgsConstructor;

// Decide asignaciones iniciales de conciliación.
// No persiste conciliaciones ni valida permisos; solo selecciona candidatos según carga.
@Service
@AllArgsConstructor
public class ConciliacionAsignacionService {

    private final EstudianteRepository estudianteRepository;
    private final ConciliadorRepository conciliadorRepository;
    private final ConciliacionRepository conciliacionRepository;
    private final ConciliacionValidator conciliacionValidator;

    public Estudiante seleccionarEstudianteParaNuevaConciliacion(Consulta consulta) {
        Estudiante estudianteConsulta = consulta != null ? consulta.getEstudiante() : null;

        if (estudianteHabilitado(estudianteConsulta)) {
            return estudianteConsulta;
        }

        return seleccionarEstudianteConMenorCarga();
    }

    public Conciliador seleccionarConciliadorParaNuevaConciliacion() {
        List<Conciliador> conciliadores = conciliadorRepository.findByActivoTrue();

        if (conciliadores == null || conciliadores.isEmpty()) {
            return null;
        }

        return conciliadores.stream()
                .min(comparadorConciliadoresPorCarga())
                .orElse(null);
    }

    private Estudiante seleccionarEstudianteConMenorCarga() {
        List<Estudiante> estudiantes = estudianteRepository.findByConciliacionTrueAndActivoTrue();

        if (estudiantes == null || estudiantes.isEmpty()) {
            return null;
        }

        return estudiantes.stream()
                .min(comparadorEstudiantesPorCarga())
                .orElse(null);
    }

    private Comparator<Estudiante> comparadorEstudiantesPorCarga() {
        return Comparator
                .comparingLong((Estudiante estudiante) -> contarConciliacionesNoFinalizadasEstudiante(estudiante))
                .thenComparing(estudiante -> normalizarTexto(estudiante.getNombre()))
                .thenComparing(estudiante -> obtenerIdSeguro(estudiante.getId()));
    }

    private Comparator<Conciliador> comparadorConciliadoresPorCarga() {
        return Comparator
                .comparingLong((Conciliador conciliador) -> contarConciliacionesNoFinalizadasConciliador(conciliador))
                .thenComparing(conciliador -> normalizarTexto(conciliador.getNombre()))
                .thenComparing(conciliador -> obtenerIdSeguro(conciliador.getId()));
    }

    private long contarConciliacionesNoFinalizadasEstudiante(Estudiante estudiante) {
        if (estudiante == null || estudiante.getId() == null) {
            return Long.MAX_VALUE;
        }

        return conciliacionRepository.countByEstudiante_IdAndActivoTrueAndEstado_CodigoIn(
                estudiante.getId(),
                conciliacionValidator.codigosEstadosNoFinalizados());
    }

    private long contarConciliacionesNoFinalizadasConciliador(Conciliador conciliador) {
        if (conciliador == null || conciliador.getId() == null) {
            return Long.MAX_VALUE;
        }

        return conciliacionRepository.countByConciliador_IdAndActivoTrueAndEstado_CodigoIn(
                conciliador.getId(),
                conciliacionValidator.codigosEstadosNoFinalizados());
    }

    private boolean estudianteHabilitado(Estudiante estudiante) {
        return estudiante != null
                && Boolean.TRUE.equals(estudiante.getActivo())
                && Boolean.TRUE.equals(estudiante.getConciliacion());
    }

    private String normalizarTexto(String texto) {
        return texto != null ? texto.trim().toLowerCase(Locale.ROOT) : "";
    }

    private Long obtenerIdSeguro(Long id) {
        return id != null ? id : Long.MAX_VALUE;
    }
}