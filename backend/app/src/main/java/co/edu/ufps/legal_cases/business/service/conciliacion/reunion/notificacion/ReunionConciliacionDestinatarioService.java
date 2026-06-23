package co.edu.ufps.legal_cases.business.service.conciliacion.reunion.notificacion;

import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarEmail;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.dto.conciliacion.reunion.notificacion.ReunionConciliacionDestinatarioDTO;
import co.edu.ufps.legal_cases.business.dto.seguimiento.notificacion.SeguimientoDestinatarioDTO;
import co.edu.ufps.legal_cases.business.model.conciliacion.Conciliacion;
import co.edu.ufps.legal_cases.business.model.conciliacion.reunion.notificacion.TipoDestinatarioReunionConciliacion;
import co.edu.ufps.legal_cases.business.repository.conciliacion.ConciliacionRepository;
import co.edu.ufps.legal_cases.business.repository.consulta.ConsultaRepository;
import co.edu.ufps.legal_cases.business.repository.perfil.AdministrativoRepository;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;

// Calcula destinatarios de reunión sin decidir si se envía inmediato o recordatorio.
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReunionConciliacionDestinatarioService {

    private final ConciliacionRepository conciliacionRepository;
    private final ConsultaRepository consultaRepository;
    private final AdministrativoRepository administrativoRepository;

    public List<ReunionConciliacionDestinatarioDTO> obtenerDestinatariosPartes(Long conciliacionId) {
        Long consultaId = obtenerConsultaId(conciliacionId);

        List<ReunionConciliacionDestinatarioDTO> destinatarios = new ArrayList<>();

        destinatarios.addAll(convertir(
                consultaRepository.findDestinatarioPersonaPrincipalByConsultaId(consultaId),
                TipoDestinatarioReunionConciliacion.CONSULTANTE));

        destinatarios.addAll(convertir(
                consultaRepository.findDestinatariosPartesByConsultaId(consultaId),
                TipoDestinatarioReunionConciliacion.PARTE));

        destinatarios.addAll(convertir(
                consultaRepository.findDestinatariosContrapartesByConsultaId(consultaId),
                TipoDestinatarioReunionConciliacion.CONTRAPARTE));

        return limpiarDestinatarios(destinatarios);
    }

    public List<ReunionConciliacionDestinatarioDTO> obtenerDestinatariosAdministrativos() {
        return limpiarDestinatarios(convertir(
                administrativoRepository.findDestinatariosActivos(),
                TipoDestinatarioReunionConciliacion.ADMINISTRATIVO));
    }

    private Long obtenerConsultaId(Long conciliacionId) {
        if (conciliacionId == null) {
            throw new BusinessException("La conciliación es obligatoria para calcular destinatarios");
        }

        Conciliacion conciliacion = conciliacionRepository.findByIdAndActivoTrue(conciliacionId)
                .orElseThrow(() -> new BusinessException("Conciliación no encontrada con id: " + conciliacionId));

        if (conciliacion.getConsulta() == null || conciliacion.getConsulta().getId() == null) {
            throw new BusinessException("La conciliación no tiene consulta asociada");
        }

        return conciliacion.getConsulta().getId();
    }

    private List<ReunionConciliacionDestinatarioDTO> convertir(
            List<SeguimientoDestinatarioDTO> destinatarios,
            TipoDestinatarioReunionConciliacion tipoDestinatario) {

        if (destinatarios == null || destinatarios.isEmpty()) {
            return List.of();
        }

        return destinatarios.stream()
                .map(destinatario -> new ReunionConciliacionDestinatarioDTO(
                        destinatario.getEmail(),
                        destinatario.getNombre(),
                        tipoDestinatario))
                .toList();
    }

    private List<ReunionConciliacionDestinatarioDTO> limpiarDestinatarios(
            List<ReunionConciliacionDestinatarioDTO> candidatos) {

        Map<String, ReunionConciliacionDestinatarioDTO> destinatariosPorEmail = new LinkedHashMap<>();

        for (ReunionConciliacionDestinatarioDTO candidato : candidatos) {
            agregarSiEsValido(destinatariosPorEmail, candidato);
        }

        return new ArrayList<>(destinatariosPorEmail.values());
    }

    private void agregarSiEsValido(
            Map<String, ReunionConciliacionDestinatarioDTO> destinatariosPorEmail,
            ReunionConciliacionDestinatarioDTO candidato) {

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
                new ReunionConciliacionDestinatarioDTO(
                        email,
                        nombre,
                        candidato.getTipoDestinatario()));
    }
}
