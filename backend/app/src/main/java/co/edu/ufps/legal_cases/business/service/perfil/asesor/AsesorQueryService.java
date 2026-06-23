package co.edu.ufps.legal_cases.business.service.perfil.asesor;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.dto.perfil.AsesorDTO;
import co.edu.ufps.legal_cases.business.model.perfil.Asesor;
import co.edu.ufps.legal_cases.business.repository.perfil.AsesorRepository;
import co.edu.ufps.legal_cases.business.service.acceso.perfil.AsesorMonitorAccessService;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

@Service
public class AsesorQueryService {

    private final AsesorRepository asesorRepository;
    private final AsesorMapper asesorMapper;
    private final AsesorMonitorAccessService asesorMonitorAccessService;

    public AsesorQueryService(
            AsesorRepository asesorRepository,
            AsesorMapper asesorMapper,
            AsesorMonitorAccessService asesorMonitorAccessService) {
        this.asesorRepository = asesorRepository;
        this.asesorMapper = asesorMapper;
        this.asesorMonitorAccessService = asesorMonitorAccessService;
    }

    @Transactional(readOnly = true)
    public List<AsesorDTO> listar() {
        asesorMonitorAccessService.validarPuedeListarAsesoresYMonitores();

        return asesorRepository.findAll()
                .stream()
                .map(asesorMapper::convertirADTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AsesorDTO> listarActivos() {
        asesorMonitorAccessService.validarPuedeListarAsesoresYMonitoresActivos();

        return asesorRepository.findByActivoTrue()
                .stream()
                .map(asesorMapper::convertirADTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public AsesorDTO obtenerPorId(Long id) {
        asesorMonitorAccessService.validarPuedeListarAsesoresYMonitores();

        Asesor asesor = buscarPorId(id);

        return asesorMapper.convertirADTO(asesor);
    }

    private Asesor buscarPorId(Long id) {
        if (id == null) {
            throw new BusinessException("El id del asesor es obligatorio");
        }

        return asesorRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Asesor no encontrado con id: " + id));
    }
}