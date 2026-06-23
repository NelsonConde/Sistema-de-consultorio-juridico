package co.edu.ufps.legal_cases.business.service.consulta.consulta;

import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarTexto;

import org.springframework.stereotype.Service;

import co.edu.ufps.legal_cases.business.dto.consulta.ConsultaDTO;
import co.edu.ufps.legal_cases.business.model.consulta.Consulta;
import co.edu.ufps.legal_cases.business.model.perfil.Asesor;
import co.edu.ufps.legal_cases.business.model.perfil.Estudiante;
import lombok.AllArgsConstructor;

// Centraliza la construcción de Consulta desde el DTO.
// Mantiene fuera del CommandService la asignación de relaciones y normalización de textos.
@Service
@AllArgsConstructor
public class ConsultaConstruccionService {

    private final ConsultaRelacionService consultaRelacionService;

    public Consulta aplicarDatos(
            Consulta consulta,
            ConsultaDTO dto,
            boolean puedeAsignarResponsables) {

        consulta.setFecha(dto.getFecha());
        consulta.setDescripcion(normalizarTexto(dto.getDescripcion()));
        consulta.setHechos(normalizarTexto(dto.getHechos()));
        consulta.setPretensiones(normalizarTexto(dto.getPretensiones()));
        consulta.setConceptoJuridico(normalizarTexto(dto.getConceptoJuridico()));
        consulta.setTramite(normalizarTexto(dto.getTramite()));
        consulta.setObservaciones(normalizarTexto(dto.getObservaciones()));
        consulta.setTipoViolencia(normalizarTexto(dto.getTipoViolencia()));
        consulta.setResultado(normalizarTexto(dto.getResultado()));

        consulta.setPersona(consultaRelacionService.obtenerPersona(dto.getPersonaId()));
        consulta.setSede(consultaRelacionService.obtenerSede(dto.getSedeId()));
        consulta.setArea(consultaRelacionService.obtenerArea(dto.getAreaId()));
        consulta.setTema(consultaRelacionService.obtenerTema(dto.getTemaId()));
        consulta.setTipo(consultaRelacionService.obtenerTipo(dto.getTipoId()));

        if (puedeAsignarResponsables) {
            asignarResponsablesDesdeDto(consulta, dto);
        }

        consulta.getPartes().clear();
        consulta.getPartes().addAll(consultaRelacionService.obtenerPersonas(dto.getPartesIds()));

        consulta.getContrapartes().clear();
        consulta.getContrapartes().addAll(consultaRelacionService.obtenerPersonas(dto.getContrapartesIds()));

        return consulta;
    }

    private void asignarResponsablesDesdeDto(Consulta consulta, ConsultaDTO dto) {
        Asesor asesor = consultaRelacionService.obtenerAsesor(dto.getAsesorId());
        Estudiante estudiante = consultaRelacionService.obtenerEstudiante(dto.getEstudianteId());

        // Si se asigna estudiante sin asesor explícito, se toma el asesor activo del
        // estudiante.
        // Esto evita consultas con estudiante asignado pero sin responsable académico.
        if (asesor == null && estudiante != null) {
            asesor = consultaRelacionService.obtenerAsesorDelEstudianteActivo(estudiante);
        }

        consulta.setAsesor(asesor);
        consulta.setMonitor(consultaRelacionService.obtenerMonitor(dto.getMonitorId()));
        consulta.setEstudiante(estudiante);
    }
}