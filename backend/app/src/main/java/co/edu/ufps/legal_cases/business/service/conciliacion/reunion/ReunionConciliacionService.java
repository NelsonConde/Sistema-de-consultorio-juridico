package co.edu.ufps.legal_cases.business.service.conciliacion.reunion;

import org.springframework.stereotype.Service;

import co.edu.ufps.legal_cases.business.dto.conciliacion.reunion.ReunionConciliacionRequestDTO;
import co.edu.ufps.legal_cases.business.dto.conciliacion.reunion.ReunionConciliacionResponseDTO;
import lombok.AllArgsConstructor;

// Fachada del submódulo de reuniones de conciliación.
@Service
@AllArgsConstructor
public class ReunionConciliacionService {

    private final ReunionConciliacionCommandService reunionConciliacionCommandService;

    public ReunionConciliacionResponseDTO programar(Long conciliacionId, ReunionConciliacionRequestDTO dto) {
        return reunionConciliacionCommandService.programar(conciliacionId, dto);
    }

    public ReunionConciliacionResponseDTO reprogramar(Long conciliacionId, ReunionConciliacionRequestDTO dto) {
        return reunionConciliacionCommandService.reprogramar(conciliacionId, dto);
    }
}
