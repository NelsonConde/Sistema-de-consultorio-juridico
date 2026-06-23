package co.edu.ufps.legal_cases.business.service.consulta.consulta;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.dto.consulta.ConsultaDTO;
import co.edu.ufps.legal_cases.business.model.catalogo.Area;
import co.edu.ufps.legal_cases.business.model.catalogo.Sede;
import co.edu.ufps.legal_cases.business.model.catalogo.Tema;
import co.edu.ufps.legal_cases.business.model.catalogo.Tipo;
import co.edu.ufps.legal_cases.business.model.consulta.Consulta;
import co.edu.ufps.legal_cases.business.model.perfil.Asesor;
import co.edu.ufps.legal_cases.business.model.perfil.Estudiante;
import co.edu.ufps.legal_cases.business.model.perfil.Monitor;
import co.edu.ufps.legal_cases.business.model.persona.Persona;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

@Component
public class ConsultaCambioEstructuralValidator {

    public void validarSiTieneActividad(
            Consulta consulta,
            ConsultaDTO dto,
            boolean tieneActividadAsociada,
            boolean puedeAsignarResponsables) {

        if (!tieneActividadAsociada) {
            return;
        }

        if (consulta == null || dto == null) {
            throw new BusinessException("Los datos de la consulta son obligatorios");
        }

        if (cambiaDatosEstructurales(consulta, dto, puedeAsignarResponsables)) {
            throw new BusinessException(
                    "No se pueden modificar datos estructurales de la consulta porque ya tiene procesos, seguimientos o conciliaciones asociadas");
        }
    }

    private boolean cambiaDatosEstructurales(
            Consulta consulta,
            ConsultaDTO dto,
            boolean puedeAsignarResponsables) {

        return cambiaPersonaPrincipal(consulta, dto)
                || cambiaCatalogosBase(consulta, dto)
                || cambiaPersonasRelacionadas(consulta, dto)
                || cambiaResponsables(consulta, dto, puedeAsignarResponsables);
    }

    private boolean cambiaPersonaPrincipal(Consulta consulta, ConsultaDTO dto) {
        return !Objects.equals(idPersona(consulta.getPersona()), dto.getPersonaId());
    }

    private boolean cambiaCatalogosBase(Consulta consulta, ConsultaDTO dto) {
        return !Objects.equals(idSede(consulta.getSede()), dto.getSedeId())
                || !Objects.equals(idArea(consulta.getArea()), dto.getAreaId())
                || !Objects.equals(idTema(consulta.getTema()), dto.getTemaId())
                || !Objects.equals(idTipo(consulta.getTipo()), dto.getTipoId());
    }

    private boolean cambiaPersonasRelacionadas(Consulta consulta, ConsultaDTO dto) {
        return !idsPersonas(consulta.getPartes()).equals(ids(dto.getPartesIds()))
                || !idsPersonas(consulta.getContrapartes()).equals(ids(dto.getContrapartesIds()));
    }

    private boolean cambiaResponsables(
            Consulta consulta,
            ConsultaDTO dto,
            boolean puedeAsignarResponsables) {

        if (!puedeAsignarResponsables) {
            return false;
        }

        return !Objects.equals(idAsesor(consulta.getAsesor()), dto.getAsesorId())
                || !Objects.equals(idMonitor(consulta.getMonitor()), dto.getMonitorId())
                || !Objects.equals(idEstudiante(consulta.getEstudiante()), dto.getEstudianteId());
    }

    private Long idPersona(Persona persona) {
        return persona != null ? persona.getId() : null;
    }

    private Long idSede(Sede sede) {
        return sede != null ? sede.getId() : null;
    }

    private Long idArea(Area area) {
        return area != null ? area.getId() : null;
    }

    private Long idTema(Tema tema) {
        return tema != null ? tema.getId() : null;
    }

    private Long idTipo(Tipo tipo) {
        return tipo != null ? tipo.getId() : null;
    }

    private Long idAsesor(Asesor asesor) {
        return asesor != null ? asesor.getId() : null;
    }

    private Long idMonitor(Monitor monitor) {
        return monitor != null ? monitor.getId() : null;
    }

    private Long idEstudiante(Estudiante estudiante) {
        return estudiante != null ? estudiante.getId() : null;
    }

    private Set<Long> idsPersonas(List<Persona> personas) {
        Set<Long> resultado = new HashSet<>();

        if (personas == null) {
            return resultado;
        }

        personas.stream()
                .filter(Objects::nonNull)
                .map(Persona::getId)
                .filter(Objects::nonNull)
                .forEach(resultado::add);

        return resultado;
    }

    private Set<Long> ids(List<Long> ids) {
        Set<Long> resultado = new HashSet<>();

        if (ids == null) {
            return resultado;
        }

        ids.stream()
                .filter(Objects::nonNull)
                .forEach(resultado::add);

        return resultado;
    }
}