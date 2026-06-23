package co.edu.ufps.legal_cases.business.service.seguimiento.notificacion;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.dto.seguimiento.notificacion.DatosCorreoSeguimientoDTO;
import co.edu.ufps.legal_cases.business.dto.seguimiento.notificacion.SeguimientoDestinatarioDTO;
import co.edu.ufps.legal_cases.business.model.seguimiento.notificacion.SeguimientoNotificacion;
import co.edu.ufps.legal_cases.business.repository.seguimiento.SeguimientoRepository;
import co.edu.ufps.legal_cases.business.repository.seguimiento.notificacion.SeguimientoNotificacionRepository;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;

// Envía notificaciones pendientes y registra el resultado del intento.
// No decide si la notificación aplica; eso ya viene resuelto por los servicios de sincronización.
@Service
@RequiredArgsConstructor
public class SeguimientoEnvioNotificacionService {

    private static final int LONGITUD_MAXIMA_ERROR = 500;

    private final SeguimientoRepository seguimientoRepository;
    private final SeguimientoNotificacionRepository seguimientoNotificacionRepository;
    private final SeguimientoDestinatarioService seguimientoDestinatarioService;
    private final SeguimientoCorreoService seguimientoCorreoService;

    @Transactional
    public void enviarNotificacionPendiente(SeguimientoNotificacion notificacion) {
        // Si no existe o esta inactiva, no se intenta enviar.
        if (notificacion == null || !Boolean.TRUE.equals(notificacion.getActivo())) {
            return;
        }

        // Obtiene los datos necesarios para armar el correo.
        DatosCorreoSeguimientoDTO datos = obtenerDatosCorreo(notificacion.getSeguimiento().getId());

        try {
            // Obtiene los destinatarios segun el tipo de notificacion.
            List<SeguimientoDestinatarioDTO> destinatarios = seguimientoDestinatarioService.obtenerDestinatarios(
                    datos.getSeguimientoId(),
                    notificacion.getTipoNotificacion());

            // Si no hay destinatarios, se marca como enviada para que no quede pendiente siempre.
            if (destinatarios.isEmpty()) {
                marcarComoEnviada(notificacion);
                return;
            }

            // Envia el correo con el tipo y el momento de la notificacion.
            seguimientoCorreoService.enviar(
                    destinatarios,
                    notificacion.getTipoNotificacion(),
                    notificacion.getMomentoNotificacion(),
                    datos.getDescripcion(),
                    datos.getCategoria(),
                    datos.getConsultaId(),
                    datos.getFechaEntrega());

            // Si no hubo error, se marca como enviada.
            marcarComoEnviada(notificacion);

        } catch (Exception ex) {
            // Si falla el envio, se guarda el error y aumenta el contador de intentos.
            marcarConError(notificacion, ex);
        }
    }

    // Obtiene los datos necesarios para mandar correos de seguimiento.
    private DatosCorreoSeguimientoDTO obtenerDatosCorreo(Long seguimientoId) {
        if (seguimientoId == null) {
            throw new BusinessException("El id del seguimiento es obligatorio para notificar");
        }

        // Consulta un DTO con la informacion minima, no toda la entidad.
        return seguimientoRepository.findDatosCorreoById(seguimientoId)
                .orElseThrow(() -> new BusinessException("Seguimiento no encontrado con id: " + seguimientoId));
    }

    private void marcarComoEnviada(SeguimientoNotificacion notificacion) {
        // Se marca como enviada porque el correo salio bien o no habia destinatarios.
        notificacion.setEnviada(true);

        // Queda activa porque es un registro valido dentro del historial.
        notificacion.setActivo(true);

        // Guarda la fecha real del envio.
        notificacion.setFechaEnvio(LocalDateTime.now());

        // Limpia errores anteriores si el envio fue exitoso.
        notificacion.setError(null);

        seguimientoNotificacionRepository.save(notificacion);
    }

    // Actualiza la notificacion con el error y la cantidad de intentos hechos.
    private void marcarConError(SeguimientoNotificacion notificacion, Exception ex) {
        Integer intentos = notificacion.getIntentos() != null ? notificacion.getIntentos() : 0;

        // Aumenta el contador para saber cuantas veces se intento enviar.
        notificacion.setIntentos(intentos + 1);

        // Guarda el error recortado para no superar el tamaño de la columna.
        notificacion.setError(recortarError(ex.getMessage()));

        seguimientoNotificacionRepository.save(notificacion);
    }

    // Si el error es muy largo, lo limita para que quepa en la columna.
    private String recortarError(String mensaje) {
        if (mensaje == null || mensaje.isBlank()) {
            return "Error desconocido al enviar correo";
        }

        if (mensaje.length() <= LONGITUD_MAXIMA_ERROR) {
            return mensaje;
        }

        return mensaje.substring(0, LONGITUD_MAXIMA_ERROR);
    }
}