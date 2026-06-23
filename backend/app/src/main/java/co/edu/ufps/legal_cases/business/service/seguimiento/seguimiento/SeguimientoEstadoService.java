package co.edu.ufps.legal_cases.business.service.seguimiento.seguimiento;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.model.seguimiento.EstadoSeguimiento;
import co.edu.ufps.legal_cases.business.model.seguimiento.Seguimiento;
import co.edu.ufps.legal_cases.business.model.seguimiento.respuesta.EstadoRespuestaSeguimiento;
import co.edu.ufps.legal_cases.business.model.seguimiento.respuesta.SeguimientoRespuesta;
import co.edu.ufps.legal_cases.business.repository.seguimiento.SeguimientoRepository;
import co.edu.ufps.legal_cases.business.repository.seguimiento.respuesta.SeguimientoRespuestaRepository;
import co.edu.ufps.legal_cases.business.service.consulta.consulta.ConsultaEstadoService;
import co.edu.ufps.legal_cases.business.service.seguimiento.SeguimientoNotificacionService;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class SeguimientoEstadoService {

    private final SeguimientoRepository seguimientoRepository;
    private final SeguimientoRespuestaRepository seguimientoRespuestaRepository;
    private final SeguimientoNotificacionService seguimientoNotificacionService;
    private final SeguimientoValidator seguimientoValidator;
    private final ConsultaEstadoService consultaEstadoService;

    @Transactional
    public Seguimiento cambiarEstado(Long seguimientoId, EstadoSeguimiento estado) {
        Seguimiento seguimiento = obtenerSeguimientoActivo(seguimientoId);

        consultaEstadoService.validarPermiteOperacionOperativa(seguimiento.getConsulta());

        seguimientoValidator.validarCambioEstadoSeguimiento(seguimiento, estado);
        validarTransicionPermitida(seguimiento, estado);

        seguimiento.setEstado(estado);

        Seguimiento seguimientoGuardado = seguimientoRepository.save(seguimiento);

        aplicarEfectosPorEstado(seguimientoGuardado);

        return seguimientoGuardado;
    }

    @Transactional
    public Seguimiento completarPorRespuestaAprobada(SeguimientoRespuesta respuesta) {
        if (respuesta == null || respuesta.getSeguimiento() == null) {
            throw new BusinessException("La respuesta no tiene seguimiento asociado");
        }

        Seguimiento seguimiento = obtenerSeguimientoActivo(respuesta.getSeguimiento().getId());

        if (EstadoSeguimiento.COMPLETADO.equals(seguimiento.getEstado())) {
            return seguimiento;
        }

        if (EstadoSeguimiento.CANCELADO.equals(seguimiento.getEstado())) {
            throw new BusinessException("No se puede aprobar una respuesta de un seguimiento cancelado");
        }

        seguimiento.setEstado(EstadoSeguimiento.COMPLETADO);

        Seguimiento seguimientoGuardado = seguimientoRepository.save(seguimiento);

        aplicarEfectosPorEstado(seguimientoGuardado);

        return seguimientoGuardado;
    }

    public void validarPermiteRespuesta(Seguimiento seguimiento) {
        if (seguimiento == null) {
            throw new BusinessException("El seguimiento es obligatorio");
        }

        consultaEstadoService.validarPermiteOperacionOperativa(seguimiento.getConsulta());

        if (!EstadoSeguimiento.PENDIENTE.equals(seguimiento.getEstado())) {
            throw new BusinessException("Solo se pueden responder seguimientos pendientes");
        }
    }

    public void aplicarEfectosPorEstado(Seguimiento seguimiento) {
        if (seguimiento == null || seguimiento.getId() == null) {
            return;
        }

        if (EstadoSeguimiento.PENDIENTE.equals(seguimiento.getEstado())) {
            seguimientoNotificacionService.sincronizarNotificaciones(seguimiento.getId());
            return;
        }

        // Los seguimientos completados o cancelados no deben conservar notificaciones
        // pendientes.
        seguimientoNotificacionService.cancelarNotificacionesPendientes(seguimiento.getId());
    }

    private void validarTransicionPermitida(Seguimiento seguimiento, EstadoSeguimiento estadoNuevo) {
        if (EstadoSeguimiento.COMPLETADO.equals(estadoNuevo)) {
            validarPuedeCompletar(seguimiento);
        }

        if (EstadoSeguimiento.PENDIENTE.equals(estadoNuevo)) {
            validarPuedeReabrirComoPendiente(seguimiento);
        }
    }

    private void validarPuedeCompletar(Seguimiento seguimiento) {
        if (tieneRespuestaPendiente(seguimiento.getId())) {
            throw new BusinessException("No se puede completar el seguimiento porque tiene respuestas pendientes");
        }

        if (Boolean.TRUE.equals(seguimiento.getNotificarEstudiante())
                && !tieneRespuestaAprobada(seguimiento.getId())) {
            throw new BusinessException(
                    "No se puede completar el seguimiento porque no tiene una respuesta aprobada");
        }
    }

    private void validarPuedeReabrirComoPendiente(Seguimiento seguimiento) {
        if (tieneRespuestaAprobada(seguimiento.getId())) {
            throw new BusinessException(
                    "No se puede reabrir un seguimiento que ya tiene respuesta aprobada");
        }
    }

    private boolean tieneRespuestaPendiente(Long seguimientoId) {
        return seguimientoRespuestaRepository.existsBySeguimiento_IdAndActivoTrueAndEstado(
                seguimientoId,
                EstadoRespuestaSeguimiento.PENDIENTE);
    }

    private boolean tieneRespuestaAprobada(Long seguimientoId) {
        return seguimientoRespuestaRepository.existsBySeguimiento_IdAndActivoTrueAndEstado(
                seguimientoId,
                EstadoRespuestaSeguimiento.APROBADA);
    }

    private Seguimiento obtenerSeguimientoActivo(Long seguimientoId) {
        if (seguimientoId == null) {
            throw new BusinessException("El id del seguimiento es obligatorio");
        }

        return seguimientoRepository.findByIdAndActivoTrue(seguimientoId)
                .orElseThrow(() -> new BusinessException("Seguimiento no encontrado con id: " + seguimientoId));
    }
}