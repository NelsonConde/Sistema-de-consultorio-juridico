package co.edu.ufps.legal_cases.business.service.seguimiento.notificacion;

import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarEmail;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.dto.seguimiento.notificacion.DatosNotificacionSeguimientoDTO;
import co.edu.ufps.legal_cases.business.dto.seguimiento.notificacion.SeguimientoDestinatarioDTO;
import co.edu.ufps.legal_cases.business.model.seguimiento.notificacion.TipoNotificacionSeguimiento;
import co.edu.ufps.legal_cases.business.repository.consulta.ConsultaRepository;
import co.edu.ufps.legal_cases.business.repository.perfil.AdministrativoRepository;
import co.edu.ufps.legal_cases.business.repository.perfil.AsesorRepository;
import co.edu.ufps.legal_cases.business.repository.perfil.MonitorRepository;
import co.edu.ufps.legal_cases.business.repository.seguimiento.SeguimientoRepository;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;

// Este servicio obtiene los destinatarios a notificar en un seguimiento.
// No decide si es inmediata o recordatorio, porque eso depende del momento de la notificación.
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SeguimientoDestinatarioService {

    private final SeguimientoRepository seguimientoRepository;
    private final ConsultaRepository consultaRepository;
    private final AdministrativoRepository administrativoRepository;
    private final AsesorRepository asesorRepository;
    private final MonitorRepository monitorRepository;

    public List<SeguimientoDestinatarioDTO> obtenerDestinatarios(
            Long seguimientoId,
            TipoNotificacionSeguimiento tipoNotificacion) {

        validarTipoNotificacion(tipoNotificacion);

        // Obtiene los datos mínimos del seguimiento.
        DatosNotificacionSeguimientoDTO datos = obtenerDatosNotificacion(seguimientoId);

        // El tipo define a quién se le envía:
        // PARTES -> persona principal, partes y contrapartes.
        // ESTUDIANTE -> estudiante de la consulta.
        // ALERTA_DISCIPLINARIA -> administrativos activos.
        // AUTOR -> usuario que creó el seguimiento.
        List<SeguimientoDestinatarioDTO> candidatos = switch (tipoNotificacion) {
            case PARTES -> obtenerDestinatariosPartes(datos.getConsultaId());
            case ESTUDIANTE -> obtenerDestinatarioEstudiante(datos.getConsultaId());
            case ALERTA_DISCIPLINARIA -> administrativoRepository.findDestinatariosActivos();
            case AUTOR -> obtenerDestinatarioAutor(datos);
        };

        // Al final se limpian correos vacíos, inválidos y duplicados.
        return limpiarDestinatarios(candidatos);
    }

    private DatosNotificacionSeguimientoDTO obtenerDatosNotificacion(Long seguimientoId) {
        if (seguimientoId == null) {
            throw new BusinessException("El id del seguimiento es obligatorio");
        }

        // Trae solo los datos necesarios para calcular destinatarios.
        return seguimientoRepository.findDatosNotificacionById(seguimientoId)
                .orElseThrow(() -> new BusinessException("Seguimiento no encontrado con id: " + seguimientoId));
    }

    private List<SeguimientoDestinatarioDTO> obtenerDestinatariosPartes(Long consultaId) {
        List<SeguimientoDestinatarioDTO> destinatarios = new ArrayList<>();

        // Agrega la persona principal de la consulta.
        destinatarios.addAll(consultaRepository.findDestinatarioPersonaPrincipalByConsultaId(consultaId));

        // Agrega las partes adicionales.
        destinatarios.addAll(consultaRepository.findDestinatariosPartesByConsultaId(consultaId));

        // Agrega las contrapartes.
        destinatarios.addAll(consultaRepository.findDestinatariosContrapartesByConsultaId(consultaId));

        return destinatarios;
    }

    private List<SeguimientoDestinatarioDTO> obtenerDestinatarioEstudiante(Long consultaId) {
        // Si la consulta no tiene estudiante activo, retorna lista vacía.
        return consultaRepository.findDestinatarioEstudianteByConsultaId(consultaId)
                .map(List::of)
                .orElseGet(List::of);
    }

    private List<SeguimientoDestinatarioDTO> obtenerDestinatarioAutor(DatosNotificacionSeguimientoDTO datos) {
        if (datos.getAutorUsuarioSistemaId() == null) {
            return List.of();
        }

        // El autor puede ser asesor o monitor.
        // Se busca primero como asesor y luego como monitor.
        return asesorRepository.findDestinatarioByUsuarioSistemaId(datos.getAutorUsuarioSistemaId())
                .or(() -> monitorRepository.findDestinatarioByUsuarioSistemaId(datos.getAutorUsuarioSistemaId()))
                .map(List::of)
                .orElseGet(() -> crearDestinatarioAutorConCorreo(datos));
    }

    private List<SeguimientoDestinatarioDTO> crearDestinatarioAutorConCorreo(DatosNotificacionSeguimientoDTO datos) {
        // Si no se encuentra el perfil real del autor, se usa el correo del usuario sistema.
        if (datos.getAutorEmail() == null || datos.getAutorEmail().isBlank()) {
            return List.of();
        }

        return List.of(new SeguimientoDestinatarioDTO(
                datos.getAutorEmail(),
                datos.getAutorEmail()
        ));
    }

    private List<SeguimientoDestinatarioDTO> limpiarDestinatarios(
            List<SeguimientoDestinatarioDTO> candidatos) {

        Map<String, SeguimientoDestinatarioDTO> destinatariosPorEmail = new LinkedHashMap<>();

        // Depura la lista final para evitar correos vacíos o repetidos.
        for (SeguimientoDestinatarioDTO candidato : candidatos) {
            agregarSiEsValido(destinatariosPorEmail, candidato);
        }

        return new ArrayList<>(destinatariosPorEmail.values());
    }

    // Valida el correo del destinatario y lo agrega al mapa si no está repetido.
    private void agregarSiEsValido(
            Map<String, SeguimientoDestinatarioDTO> destinatariosPorEmail,
            SeguimientoDestinatarioDTO candidato) {

        if (candidato == null) {
            return;
        }

        String email = normalizarEmail(candidato.getEmail());

        if (email == null) {
            return;
        }

        String nombre = candidato.getNombre();

        if (nombre == null || nombre.isBlank()) {
            nombre = email;
        }

        destinatariosPorEmail.putIfAbsent(
                email,
                new SeguimientoDestinatarioDTO(email, nombre)
        );
    }

    private void validarTipoNotificacion(TipoNotificacionSeguimiento tipoNotificacion) {
        if (tipoNotificacion == null) {
            throw new BusinessException("El tipo de notificación es obligatorio");
        }
    }
}