package co.edu.ufps.legal_cases.business.service.consulta.reasignacion;

import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarTexto;

import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.dto.consulta.ConsultaDTO;
import co.edu.ufps.legal_cases.business.dto.consulta.ConsultaReasignacionDTO;
import co.edu.ufps.legal_cases.business.model.consulta.Consulta;
import co.edu.ufps.legal_cases.business.model.consulta.TipoResponsableConsulta;
import co.edu.ufps.legal_cases.business.model.perfil.Asesor;
import co.edu.ufps.legal_cases.business.model.perfil.Estudiante;
import co.edu.ufps.legal_cases.business.model.perfil.Monitor;
import co.edu.ufps.legal_cases.business.repository.consulta.ConsultaRepository;
import co.edu.ufps.legal_cases.business.service.acceso.consulta.ConsultaAccessService;
import co.edu.ufps.legal_cases.business.service.consulta.consulta.ConsultaEstadoService;
import co.edu.ufps.legal_cases.business.service.consulta.consulta.ConsultaMapper;
import co.edu.ufps.legal_cases.business.service.consulta.consulta.ConsultaRelacionService;
import co.edu.ufps.legal_cases.business.service.consulta.consulta.ConsultaValidator;
import co.edu.ufps.legal_cases.common.concurrency.ConcurrenciaOptimistaValidator;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.common.exception.ConsultaReasignacionNoDisponibleException;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.service.context.UsuarioActualService;
import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ConsultaReasignacionService {

    private final ConsultaRepository consultaRepository;
    private final ConsultaAccessService consultaAccessService;
    private final ConsultaEstadoService consultaEstadoService;
    private final ConsultaRelacionService consultaRelacionService;
    private final ConsultaValidator consultaValidator;
    private final ConsultaMapper consultaMapper;
    private final ConcurrenciaOptimistaValidator concurrenciaOptimistaValidator;
    private final UsuarioActualService usuarioActualService;
    private final Optional<ConsultaReasignacionHistorialWriter> historialWriter;
    private final EntityManager entityManager;

    @Transactional
    public ConsultaDTO reasignar(Long consultaId, ConsultaReasignacionDTO solicitud) {
        DatosSolicitud datos = validarSolicitud(solicitud);

        // Valida primero permiso y alcance sobre la asignacion vigente.
        consultaAccessService.validarPuedeReasignarResponsable(consultaId);

        // PB-34 es obligatorio: sin un escritor real no se modifica la entidad.
        ConsultaReasignacionHistorialWriter writer = historialWriter.orElseThrow(
                ConsultaReasignacionNoDisponibleException::new);

        Consulta consulta = consultaRepository.findById(consultaId)
                .orElseThrow(() -> new BusinessException(
                        "Consulta no encontrada con id: " + consultaId));

        concurrenciaOptimistaValidator.validarVersion(
                solicitud.getVersion(),
                consulta.getVersion(),
                "consulta");
        consultaEstadoService.validarPermiteOperacionOperativa(consulta);

        Long responsableAnteriorId = aplicarReasignacion(
                consulta,
                datos.tipoResponsable(),
                datos.nuevoResponsableId());

        consultaValidator.validarCompatibilidadResponsables(consulta);

        UsuarioSistema actor = usuarioActualService.obtenerUsuarioActual();
        Consulta guardada = consultaRepository.save(consulta);

        writer.registrar(new ConsultaReasignacionHistorialWriter.RegistroReasignacion(
                consulta.getId(),
                datos.tipoResponsable(),
                responsableAnteriorId,
                datos.nuevoResponsableId(),
                datos.motivo(),
                actor.getId()));

        // Detecta el conflicto @Version dentro de la misma transaccion. Si falla
        // este flush o el historial, ambos cambios se revierten.
        entityManager.flush();

        return consultaMapper.convertirADTO(guardada);
    }

    private DatosSolicitud validarSolicitud(ConsultaReasignacionDTO solicitud) {
        if (solicitud == null) {
            throw new BusinessException("Los datos de la reasignacion son obligatorios");
        }
        if (solicitud.getTipoResponsable() == null) {
            throw new BusinessException("El tipo de responsable es obligatorio");
        }
        if (solicitud.getNuevoResponsableId() == null
                || solicitud.getNuevoResponsableId() <= 0) {
            throw new BusinessException("El nuevo responsable es obligatorio");
        }
        if (solicitud.getVersion() == null || solicitud.getVersion() < 0) {
            throw new BusinessException("La version de la consulta es obligatoria");
        }

        String motivo = normalizarTexto(solicitud.getMotivo());
        if (motivo == null) {
            throw new BusinessException("El motivo de la reasignacion es obligatorio");
        }

        return new DatosSolicitud(
                solicitud.getTipoResponsable(),
                solicitud.getNuevoResponsableId(),
                motivo);
    }

    private Long aplicarReasignacion(
            Consulta consulta,
            TipoResponsableConsulta tipoResponsable,
            Long nuevoResponsableId) {

        return switch (tipoResponsable) {
            case ESTUDIANTE -> reasignarEstudiante(consulta, nuevoResponsableId);
            case ASESOR -> reasignarAsesor(consulta, nuevoResponsableId);
            case MONITOR -> reasignarMonitor(consulta, nuevoResponsableId);
        };
    }

    private Long reasignarEstudiante(Consulta consulta, Long nuevoResponsableId) {
        Long responsableAnteriorId = id(consulta.getEstudiante());
        validarResponsableDiferente(responsableAnteriorId, nuevoResponsableId);
        consulta.setEstudiante(consultaRelacionService.obtenerEstudiante(nuevoResponsableId));
        return responsableAnteriorId;
    }

    private Long reasignarAsesor(Consulta consulta, Long nuevoResponsableId) {
        Long responsableAnteriorId = id(consulta.getAsesor());
        validarResponsableDiferente(responsableAnteriorId, nuevoResponsableId);
        consulta.setAsesor(consultaRelacionService.obtenerAsesor(nuevoResponsableId));
        return responsableAnteriorId;
    }

    private Long reasignarMonitor(Consulta consulta, Long nuevoResponsableId) {
        Long responsableAnteriorId = id(consulta.getMonitor());
        validarResponsableDiferente(responsableAnteriorId, nuevoResponsableId);
        consulta.setMonitor(consultaRelacionService.obtenerMonitor(nuevoResponsableId));
        return responsableAnteriorId;
    }

    private void validarResponsableDiferente(Long responsableActualId, Long nuevoResponsableId) {
        if (Objects.equals(responsableActualId, nuevoResponsableId)) {
            throw new BusinessException("El nuevo responsable ya esta asignado a la consulta");
        }
    }

    private Long id(Estudiante estudiante) {
        return estudiante != null ? estudiante.getId() : null;
    }

    private Long id(Asesor asesor) {
        return asesor != null ? asesor.getId() : null;
    }

    private Long id(Monitor monitor) {
        return monitor != null ? monitor.getId() : null;
    }

    private record DatosSolicitud(
            TipoResponsableConsulta tipoResponsable,
            Long nuevoResponsableId,
            String motivo) {
    }
}
