package co.edu.ufps.legal_cases.business.service.perfil.monitor;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.dto.perfil.MonitorDTO;
import co.edu.ufps.legal_cases.business.model.perfil.Monitor;
import co.edu.ufps.legal_cases.business.repository.perfil.MonitorRepository;
import co.edu.ufps.legal_cases.business.service.acceso.perfil.AsesorMonitorAccessService;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

@Service
public class MonitorQueryService {

    private final MonitorRepository monitorRepository;
    private final MonitorMapper monitorMapper;
    private final AsesorMonitorAccessService asesorMonitorAccessService;

    public MonitorQueryService(
            MonitorRepository monitorRepository,
            MonitorMapper monitorMapper,
            AsesorMonitorAccessService asesorMonitorAccessService) {
        this.monitorRepository = monitorRepository;
        this.monitorMapper = monitorMapper;
        this.asesorMonitorAccessService = asesorMonitorAccessService;
    }

    @Transactional(readOnly = true)
    public List<MonitorDTO> listar() {
        asesorMonitorAccessService.validarPuedeListarAsesoresYMonitores();

        return monitorRepository.findAll()
                .stream()
                .map(monitorMapper::convertirADTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MonitorDTO> listarActivos() {
        asesorMonitorAccessService.validarPuedeListarAsesoresYMonitoresActivos();

        return monitorRepository.findByActivoTrue()
                .stream()
                .map(monitorMapper::convertirADTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public MonitorDTO obtenerPorId(Long id) {
        asesorMonitorAccessService.validarPuedeListarAsesoresYMonitores();

        Monitor monitor = buscarPorId(id);

        return monitorMapper.convertirADTO(monitor);
    }

    private Monitor buscarPorId(Long id) {
        if (id == null) {
            throw new BusinessException("El id del monitor es obligatorio");
        }

        return monitorRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Monitor no encontrado con id: " + id));
    }
}