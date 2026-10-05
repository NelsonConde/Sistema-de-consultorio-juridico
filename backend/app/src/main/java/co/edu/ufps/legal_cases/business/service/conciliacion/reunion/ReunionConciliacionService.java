package co.edu.ufps.legal_cases.business.service.conciliacion.reunion;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

import co.edu.ufps.legal_cases.business.dto.conciliacion.reunion.ReunionConciliacionRequestDTO;
import co.edu.ufps.legal_cases.business.dto.conciliacion.reunion.ReunionConciliacionResponseDTO;
import co.edu.ufps.legal_cases.business.dto.conciliacion.reunion.ReunionConciliacionResumenDTO;
import co.edu.ufps.legal_cases.common.dto.PageResponseDTO;
import lombok.AllArgsConstructor;

// Fachada del submódulo de reuniones de conciliación.
@Service
@AllArgsConstructor
public class ReunionConciliacionService {

    private final ReunionConciliacionCommandService reunionConciliacionCommandService;
    private final ReunionConciliacionQueryService reunionConciliacionQueryService;

    public PageResponseDTO<ReunionConciliacionResumenDTO> buscarParaUsuarioActual(
            String search,
            int page,
            int size,
            String sortBy,
            String direction,
            String estado,
            LocalDate fechaDesde,
            LocalDate fechaHasta) {
        return reunionConciliacionQueryService.buscarParaUsuarioActual(
                search,
                page,
                size,
                sortBy,
                direction,
                estado,
                fechaDesde,
                fechaHasta);
    }

    public ReunionConciliacionResponseDTO programar(Long conciliacionId, ReunionConciliacionRequestDTO dto) {
        return reunionConciliacionCommandService.programar(conciliacionId, dto);
    }

    public ReunionConciliacionResponseDTO reprogramar(Long conciliacionId, ReunionConciliacionRequestDTO dto) {
        return reunionConciliacionCommandService.reprogramar(conciliacionId, dto);
    }
}
