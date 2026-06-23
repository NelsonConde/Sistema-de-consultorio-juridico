package co.edu.ufps.legal_cases.business.service.conciliacion.conciliacion;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.model.conciliacion.Conciliacion;
import co.edu.ufps.legal_cases.business.model.conciliacion.EstadoConciliacion;
import co.edu.ufps.legal_cases.business.model.conciliacion.EstadoConciliacionCodigo;
import co.edu.ufps.legal_cases.business.model.consulta.Consulta;
import co.edu.ufps.legal_cases.business.model.consulta.EstadoConsulta;
import co.edu.ufps.legal_cases.business.model.perfil.Conciliador;
import co.edu.ufps.legal_cases.business.model.perfil.Estudiante;
import co.edu.ufps.legal_cases.business.repository.conciliacion.ConciliacionRepository;
import co.edu.ufps.legal_cases.business.repository.conciliacion.reunion.ReunionConciliacionRepository;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

// Valida reglas de negocio propias del módulo de conciliación.
// No valida permisos ni alcance; eso pertenece a ConciliacionAccessService.
@Component
public class ConciliacionValidator {

    private final ConciliacionRepository conciliacionRepository;
    private final ReunionConciliacionRepository reunionConciliacionRepository;

    public ConciliacionValidator(
            ConciliacionRepository conciliacionRepository,
            ReunionConciliacionRepository reunionConciliacionRepository) {
        this.conciliacionRepository = conciliacionRepository;
        this.reunionConciliacionRepository = reunionConciliacionRepository;
    }

    public List<String> codigosEstadosFinalizados() {
        return List.of(
                EstadoConciliacionCodigo.COMPLETO_CONCILIADO,
                EstadoConciliacionCodigo.COMPLETO_NO_CONCILIADO);
    }

    public List<String> codigosEstadosNoFinalizados() {
        return List.of(
                EstadoConciliacionCodigo.EN_ESPERA,
                EstadoConciliacionCodigo.ESPERANDO_REUNION,
                EstadoConciliacionCodigo.REUNION_PROGRAMADA);
    }

    public boolean esEstadoFinalizado(EstadoConciliacion estado) {
        return estado != null && codigosEstadosFinalizados().contains(estado.getCodigo());
    }

    public boolean esEstadoNoFinalizado(EstadoConciliacion estado) {
        return estado != null && codigosEstadosNoFinalizados().contains(estado.getCodigo());
    }

    public void validarConsultaPermiteConciliacion(Consulta consulta) {
        validarConsultaObligatoria(consulta);

        if (EstadoConsulta.CERRADO.equals(consulta.getEstado())) {
            throw new BusinessException("No se puede crear una conciliación sobre una consulta cerrada");
        }

        if (EstadoConsulta.ARCHIVADO.equals(consulta.getEstado())) {
            throw new BusinessException("No se puede crear una conciliación sobre una consulta archivada");
        }
    }

    public void validarConsultaPermiteOperacionConciliacion(Consulta consulta) {
        validarConsultaObligatoria(consulta);

        if (EstadoConsulta.CERRADO.equals(consulta.getEstado())) {
            throw new BusinessException("No se pueden realizar operaciones sobre conciliaciones de una consulta cerrada");
        }

        if (EstadoConsulta.ARCHIVADO.equals(consulta.getEstado())) {
            throw new BusinessException("No se pueden realizar operaciones sobre conciliaciones de una consulta archivada");
        }
    }

    public void validarNoExisteConciliacionActivaNoFinalizada(Long consultaId) {
        if (consultaId == null) {
            throw new BusinessException("La consulta es obligatoria");
        }

        if (conciliacionRepository.existsByConsulta_IdAndActivoTrueAndEstado_CodigoIn(
                consultaId,
                codigosEstadosNoFinalizados())) {
            throw new BusinessException("La consulta ya tiene una conciliación activa no finalizada");
        }
    }

    public void validarConciliacionActiva(Conciliacion conciliacion) {
        validarConciliacionObligatoria(conciliacion);

        if (!Boolean.TRUE.equals(conciliacion.getActivo())) {
            throw new BusinessException("La conciliación se encuentra inactiva");
        }
    }

    public void validarConciliacionNoFinalizada(Conciliacion conciliacion) {
        validarConciliacionActiva(conciliacion);

        if (esEstadoFinalizado(conciliacion.getEstado())) {
            throw new BusinessException("No se puede modificar una conciliación finalizada");
        }
    }

    public void validarEstudianteHabilitadoParaConciliacion(Estudiante estudiante) {
        if (estudiante == null) {
            throw new BusinessException("El estudiante es obligatorio");
        }

        if (!Boolean.TRUE.equals(estudiante.getActivo())) {
            throw new BusinessException("El estudiante asignado se encuentra inactivo");
        }

        if (!Boolean.TRUE.equals(estudiante.getConciliacion())) {
            throw new BusinessException("El estudiante asignado no está habilitado para conciliación");
        }
    }

    public void validarConciliadorActivo(Conciliador conciliador) {
        if (conciliador == null) {
            throw new BusinessException("El conciliador es obligatorio");
        }

        if (!Boolean.TRUE.equals(conciliador.getActivo())) {
            throw new BusinessException("El conciliador asignado se encuentra inactivo");
        }
    }

    public void validarCambioEstado(Conciliacion conciliacion, EstadoConciliacion estadoNuevo) {
        validarConciliacionNoFinalizada(conciliacion);
        validarConsultaPermiteOperacionConciliacion(conciliacion.getConsulta());

        if (estadoNuevo == null) {
            throw new BusinessException("El estado de conciliación es obligatorio");
        }

        if (esEstadoFinalizado(estadoNuevo)) {
            throw new BusinessException("Para finalizar la conciliación debe usar el endpoint de finalización con acta");
        }

        if (EstadoConciliacionCodigo.EN_ESPERA.equals(estadoNuevo.getCodigo())) {
            throw new BusinessException("El estado en espera se calcula automáticamente según las asignaciones");
        }

        if (conciliacion.getEstado() != null
                && Objects.equals(conciliacion.getEstado().getCodigo(), estadoNuevo.getCodigo())) {
            throw new BusinessException("La conciliación ya tiene ese estado");
        }

        if (EstadoConciliacionCodigo.ESPERANDO_REUNION.equals(estadoNuevo.getCodigo())) {
            validarTieneResponsablesMinimos(conciliacion);
        }

        if (EstadoConciliacionCodigo.REUNION_PROGRAMADA.equals(estadoNuevo.getCodigo())) {
            validarTieneResponsablesMinimos(conciliacion);
            validarTieneReunionProgramada(conciliacion);
        }
    }

    public void validarFinalizacion(Conciliacion conciliacion, EstadoConciliacion estadoFinal) {
        validarConciliacionNoFinalizada(conciliacion);
        validarConsultaPermiteOperacionConciliacion(conciliacion.getConsulta());

        if (!esEstadoFinalizado(estadoFinal)) {
            throw new BusinessException("La finalización solo permite estados conciliado o no conciliado");
        }

        validarTieneResponsablesMinimos(conciliacion);
    }

    public void validarFechaConciliacion(LocalDateTime fechaConciliacion) {
        if (fechaConciliacion == null) {
            throw new BusinessException("La fecha de conciliación es obligatoria");
        }
    }

    public void validarFechaConciliacionFutura(LocalDateTime fechaConciliacion) {
        validarFechaConciliacion(fechaConciliacion);

        if (!fechaConciliacion.isAfter(LocalDateTime.now())) {
            throw new BusinessException("La fecha de conciliación debe ser futura");
        }
    }

    public void validarPuedeProgramarFecha(Conciliacion conciliacion) {
        validarConciliacionNoFinalizada(conciliacion);
        validarConsultaPermiteOperacionConciliacion(conciliacion.getConsulta());
        validarTieneResponsablesMinimos(conciliacion);
    }

    public void validarTieneResponsablesMinimos(Conciliacion conciliacion) {
        validarConciliacionActiva(conciliacion);

        if (conciliacion.getEstudiante() == null) {
            throw new BusinessException("La conciliación no tiene estudiante asignado");
        }

        if (conciliacion.getConciliador() == null) {
            throw new BusinessException("La conciliación no tiene conciliador asignado");
        }
    }

    public String calcularCodigoEstadoDespuesDeAsignacion(Conciliacion conciliacion) {
        if (conciliacion != null
                && conciliacion.getId() != null
                && reunionConciliacionRepository.existsByConciliacion_Id(conciliacion.getId())) {
            return EstadoConciliacionCodigo.REUNION_PROGRAMADA;
        }

        if (conciliacion != null
                && conciliacion.getEstudiante() != null
                && conciliacion.getConciliador() != null) {
            return EstadoConciliacionCodigo.ESPERANDO_REUNION;
        }

        return EstadoConciliacionCodigo.EN_ESPERA;
    }

    private void validarTieneReunionProgramada(Conciliacion conciliacion) {
        if (conciliacion.getId() == null
                || !reunionConciliacionRepository.existsByConciliacion_Id(conciliacion.getId())) {
            throw new BusinessException("No se puede marcar como reunión programada sin reunión de conciliación registrada");
        }
    }

    private void validarConsultaObligatoria(Consulta consulta) {
        if (consulta == null || consulta.getId() == null) {
            throw new BusinessException("La consulta es obligatoria");
        }
    }

    private void validarConciliacionObligatoria(Conciliacion conciliacion) {
        if (conciliacion == null || conciliacion.getId() == null) {
            throw new BusinessException("La conciliación es obligatoria");
        }
    }
}
