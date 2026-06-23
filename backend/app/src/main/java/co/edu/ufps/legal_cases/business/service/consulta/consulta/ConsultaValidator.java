package co.edu.ufps.legal_cases.business.service.consulta.consulta;

import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarTexto;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.dto.consulta.ConsultaDTO;
import co.edu.ufps.legal_cases.business.model.catalogo.Area;
import co.edu.ufps.legal_cases.business.model.catalogo.Tema;
import co.edu.ufps.legal_cases.business.model.catalogo.Tipo;
import co.edu.ufps.legal_cases.business.model.consulta.Consulta;
import co.edu.ufps.legal_cases.business.model.consulta.EstadoConsulta;
import co.edu.ufps.legal_cases.business.model.perfil.Asesor;
import co.edu.ufps.legal_cases.business.model.perfil.Estudiante;
import co.edu.ufps.legal_cases.business.model.perfil.Monitor;
import co.edu.ufps.legal_cases.business.model.persona.Persona;
import co.edu.ufps.legal_cases.business.service.acceso.consulta.ConsultaAccessService;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

// Valida reglas de negocio propias de Consulta.
// Aquí no se consultan repositories; se validan objetos ya cargados por el service.
@Component
public class ConsultaValidator {

    private final ConsultaAccessService consultaAccessService;

    public ConsultaValidator(ConsultaAccessService consultaAccessService) {
        this.consultaAccessService = consultaAccessService;
    }

    public void validarIdNoEnviadoEnCreacion(Long id) {
        if (id != null) {
            throw new BusinessException("El id no debe enviarse en la creación");
        }
    }

    public void validarIdNoCambiado(Long idExistente, Long idDto) {
        if (idDto != null && !idDto.equals(idExistente)) {
            throw new BusinessException("No se permite cambiar el id de la consulta");
        }
    }

    public void validarCamposObligatorios(ConsultaDTO dto) {
        if (dto == null) {
            throw new BusinessException("Los datos de la consulta son obligatorios");
        }

        if (dto.getFecha() == null)
            throw new BusinessException("La fecha es obligatoria");
        if (normalizarTexto(dto.getDescripcion()) == null)
            throw new BusinessException("La descripción es obligatoria");
        if (normalizarTexto(dto.getHechos()) == null)
            throw new BusinessException("Los hechos son obligatorios");
        if (normalizarTexto(dto.getPretensiones()) == null)
            throw new BusinessException("Las pretensiones son obligatorias");
        if (normalizarTexto(dto.getConceptoJuridico()) == null)
            throw new BusinessException("El concepto jurídico es obligatorio");
        if (normalizarTexto(dto.getTramite()) == null)
            throw new BusinessException("El trámite es obligatorio");
        if (dto.getPersonaId() == null)
            throw new BusinessException("La persona es obligatoria");
        if (dto.getSedeId() == null)
            throw new BusinessException("La sede es obligatoria");
        if (dto.getAreaId() == null)
            throw new BusinessException("El área es obligatoria");
        if (dto.getTemaId() == null)
            throw new BusinessException("El tema es obligatorio");
    }

    public void validarEstadoInicialPendienteSiFueEnviado(EstadoConsulta estado) {
        if (estado != null && !EstadoConsulta.PENDIENTE.equals(estado)) {
            throw new BusinessException("Una consulta nueva debe iniciar en estado pendiente");
        }
    }

    public void validarEstadoNoCambiadoEnActualizacion(
            EstadoConsulta estadoActual,
            EstadoConsulta estadoDto) {

        if (estadoDto != null && !Objects.equals(estadoActual, estadoDto)) {
            throw new BusinessException("El estado de la consulta no se cambia desde la edición de datos generales");
        }
    }

    public void validarCambioEstadoPermitido(Consulta consulta, EstadoConsulta estadoNuevo) {
        if (estadoNuevo == null) {
            throw new BusinessException("El estado es obligatorio");
        }

        if (Objects.equals(consulta.getEstado(), estadoNuevo)) {
            throw new BusinessException("La consulta ya tiene ese estado");
        }

        consultaAccessService.validarPuedeCambiarEstadoConsulta(consulta.getId(), estadoNuevo);
    }

    public void validarNoArchivada(Consulta consulta) {
        if (consulta.getEstado() == EstadoConsulta.ARCHIVADO) {
            throw new BusinessException("No se puede modificar una consulta archivada");
        }
    }

    public void validarNoArchivadaParaArchivar(Consulta consulta) {
        if (consulta.getEstado() == EstadoConsulta.ARCHIVADO) {
            throw new BusinessException("La consulta ya se encuentra archivada");
        }
    }

    public void validarCoherenciaDominio(Consulta consulta) {
        validarJerarquiaCatalogos(
                consulta.getArea(),
                consulta.getTema(),
                consulta.getTipo());

        validarResponsables(
                consulta.getArea(),
                consulta.getAsesor(),
                consulta.getEstudiante());

        validarPersonasSinDuplicados(
                consulta.getPersona(),
                consulta.getPartes(),
                consulta.getContrapartes());
    }

    public void validarRequisitosParaEstadoOperativo(Consulta consulta, EstadoConsulta estadoNuevo) {
        if (!requiereResponsablesParaAtencion(estadoNuevo)) {
            return;
        }

        // Para entrar en atención real, la consulta debe tener responsables internos mínimos.
        if (consulta.getAsesor() == null) {
            throw new BusinessException("No se puede activar la consulta porque no tiene asesor asignado");
        }

        if (consulta.getEstudiante() == null) {
            throw new BusinessException("No se puede activar la consulta porque no tiene estudiante asignado");
        }

        validarCoherenciaDominio(consulta);
    }

    public boolean tieneResponsablesEnDto(ConsultaDTO dto) {
        return dto != null
                && (dto.getAsesorId() != null
                        || dto.getMonitorId() != null
                        || dto.getEstudianteId() != null);
    }

    public boolean cambiaResponsablesEnDto(Consulta consulta, ConsultaDTO dto) {
        if (consulta == null || dto == null) {
            return false;
        }

        return idFueEnviadoYEsDiferente(obtenerId(consulta.getAsesor()), dto.getAsesorId())
                || idFueEnviadoYEsDiferente(obtenerId(consulta.getMonitor()), dto.getMonitorId())
                || idFueEnviadoYEsDiferente(obtenerId(consulta.getEstudiante()), dto.getEstudianteId());
    }

    private void validarJerarquiaCatalogos(Area area, Tema tema, Tipo tipo) {
        if (area == null) {
            throw new BusinessException("El área es obligatoria");
        }

        if (tema == null) {
            throw new BusinessException("El tema es obligatorio");
        }

        if (tema.getArea() == null || !Objects.equals(tema.getArea().getId(), area.getId())) {
            throw new BusinessException("El tema seleccionado no pertenece al área de la consulta");
        }

        if (tipo != null) {
            if (tipo.getTema() == null || !Objects.equals(tipo.getTema().getId(), tema.getId())) {
                throw new BusinessException("El tipo seleccionado no pertenece al tema de la consulta");
            }
        }
    }

    private void validarResponsables(Area area, Asesor asesor, Estudiante estudiante) {
        if (asesor != null) {
            if (asesor.getArea() == null || !Objects.equals(asesor.getArea().getId(), area.getId())) {
                throw new BusinessException("El asesor asignado no pertenece al área de la consulta");
            }
        }

        if (estudiante != null && estudiante.getAsesor() != null && asesor != null) {
            if (!Objects.equals(estudiante.getAsesor().getId(), asesor.getId())) {
                throw new BusinessException("El estudiante asignado no pertenece al asesor seleccionado");
            }
        }

        if (estudiante != null && estudiante.getAsesor() != null) {
            Asesor asesorDelEstudiante = estudiante.getAsesor();

            if (asesorDelEstudiante.getArea() == null
                    || !Objects.equals(asesorDelEstudiante.getArea().getId(), area.getId())) {
                throw new BusinessException("El asesor del estudiante no pertenece al área de la consulta");
            }
        }
    }

    private void validarPersonasSinDuplicados(
            Persona personaPrincipal,
            List<Persona> partes,
            List<Persona> contrapartes) {

        if (personaPrincipal == null) {
            throw new BusinessException("La persona principal es obligatoria");
        }

        Set<Long> idsPartes = obtenerIds(partes);
        Set<Long> idsContrapartes = obtenerIds(contrapartes);

        if (idsPartes.contains(personaPrincipal.getId())) {
            throw new BusinessException("La persona principal no puede repetirse como parte adicional");
        }

        if (idsContrapartes.contains(personaPrincipal.getId())) {
            throw new BusinessException("La persona principal no puede repetirse como contraparte");
        }

        for (Long parteId : idsPartes) {
            if (idsContrapartes.contains(parteId)) {
                throw new BusinessException("Una misma persona no puede estar como parte y contraparte");
            }
        }

        validarSinDuplicados(partes, "partes");
        validarSinDuplicados(contrapartes, "contrapartes");
    }

    private boolean requiereResponsablesParaAtencion(EstadoConsulta estado) {
        return EstadoConsulta.ACTIVO.equals(estado)
                || EstadoConsulta.EN_PROCESO.equals(estado)
                || EstadoConsulta.URGENTE.equals(estado);
    }

    private boolean idFueEnviadoYEsDiferente(Long idActual, Long idDto) {
        return idDto != null && !Objects.equals(idActual, idDto);
    }

    private Long obtenerId(Asesor asesor) {
        return asesor != null ? asesor.getId() : null;
    }

    private Long obtenerId(Monitor monitor) {
        return monitor != null ? monitor.getId() : null;
    }

    private Long obtenerId(Estudiante estudiante) {
        return estudiante != null ? estudiante.getId() : null;
    }

    private Set<Long> obtenerIds(List<Persona> personas) {
        Set<Long> ids = new HashSet<>();

        if (personas == null) {
            return ids;
        }

        personas.stream()
                .filter(Objects::nonNull)
                .map(Persona::getId)
                .filter(Objects::nonNull)
                .forEach(ids::add);

        return ids;
    }

    private void validarSinDuplicados(List<Persona> personas, String grupo) {
        if (personas == null) {
            return;
        }

        Set<Long> ids = new HashSet<>();

        for (Persona persona : personas) {
            if (persona == null || persona.getId() == null) {
                continue;
            }

            if (!ids.add(persona.getId())) {
                throw new BusinessException("Existen personas repetidas en " + grupo);
            }
        }
    }
}