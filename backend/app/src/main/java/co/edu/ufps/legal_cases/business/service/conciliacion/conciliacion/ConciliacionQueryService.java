package co.edu.ufps.legal_cases.business.service.conciliacion.conciliacion;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.dto.conciliacion.ConciliacionDetalleResponseDTO;
import co.edu.ufps.legal_cases.business.dto.conciliacion.ConciliacionResponseDTO;
import co.edu.ufps.legal_cases.business.model.conciliacion.Conciliacion;
import co.edu.ufps.legal_cases.business.model.consulta.Consulta;
import co.edu.ufps.legal_cases.business.model.consulta.EstadoConsulta;
import co.edu.ufps.legal_cases.business.repository.conciliacion.ConciliacionRepository;
import co.edu.ufps.legal_cases.business.repository.conciliacion.reunion.ReunionConciliacionRepository;
import co.edu.ufps.legal_cases.business.repository.consulta.ConsultaRepository;
import co.edu.ufps.legal_cases.business.service.acceso.conciliacion.ConciliacionAccessService;
import co.edu.ufps.legal_cases.business.service.acceso.conciliacion.ConciliacionAlcanceService;
import co.edu.ufps.legal_cases.business.service.conciliacion.reunion.ReunionConciliacionMapper;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import lombok.AllArgsConstructor;

// Maneja lecturas del módulo de conciliación.
// Aplica permisos/alcance y arma respuestas de listado o detalle.
@Service
@AllArgsConstructor
public class ConciliacionQueryService {

    private final ConciliacionRepository conciliacionRepository;
    private final ConsultaRepository consultaRepository;
    private final ReunionConciliacionRepository reunionConciliacionRepository;
    private final ConciliacionAccessService conciliacionAccessService;
    private final ConciliacionAlcanceService conciliacionAlcanceService;
    private final ConciliacionMapper conciliacionMapper;
    private final ReunionConciliacionMapper reunionConciliacionMapper;

    @Transactional(readOnly = true)
    public List<ConciliacionResponseDTO> listar() {
        conciliacionAccessService.validarPuedeListarConciliaciones();

        return conciliacionRepository
                .findByActivoTrueAndConsulta_EstadoNotOrderByIdDesc(EstadoConsulta.ARCHIVADO)
                .stream()
                .filter(conciliacionAlcanceService::puedeVerConciliacion)
                .map(conciliacionMapper::convertirAResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ConciliacionResponseDTO> listarPorConsulta(Long consultaId) {
        conciliacionAccessService.validarPuedeListarConciliaciones();

        if (consultaId == null) {
            throw new BusinessException("La consulta es obligatoria");
        }

        return conciliacionRepository
                .findByConsulta_IdAndActivoTrueAndConsulta_EstadoNotOrderByIdDesc(
                        consultaId,
                        EstadoConsulta.ARCHIVADO)
                .stream()
                .filter(conciliacionAlcanceService::puedeVerConciliacion)
                .map(conciliacionMapper::convertirAResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public ConciliacionDetalleResponseDTO obtenerDetalle(Long id) {
        conciliacionAccessService.validarPuedeVerConciliacion(id);

        Conciliacion conciliacion = conciliacionRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new BusinessException("Conciliación no encontrada con id: " + id));

        cargarConsultaConPersonas(conciliacion);

        ConciliacionDetalleResponseDTO detalle = conciliacionMapper.convertirADetalleResponseDTO(conciliacion);

        reunionConciliacionRepository.findByConciliacion_Id(conciliacion.getId())
                .map(reunionConciliacionMapper::convertirAResponseDTO)
                .ifPresent(detalle::setReunion);

        return detalle;
    }

    private void cargarConsultaConPersonas(Conciliacion conciliacion) {
        if (conciliacion.getConsulta() == null || conciliacion.getConsulta().getId() == null) {
            throw new BusinessException("La conciliación no tiene consulta asociada");
        }

        Long consultaId = conciliacion.getConsulta().getId();

        Consulta consulta = consultaRepository.findByIdConPartes(consultaId)
                .orElseThrow(() -> new BusinessException("Consulta no encontrada con id: " + consultaId));

        // Carga contrapartes en la misma transacción.
        // Se hace separado para respetar el patrón actual del backend y evitar fetch
        // simultáneo de dos colecciones.
        consultaRepository.findByIdConContrapartes(consultaId);

        conciliacion.setConsulta(consulta);
    }
}
