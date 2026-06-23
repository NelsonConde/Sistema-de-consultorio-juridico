package co.edu.ufps.legal_cases.business.service.perfil.conciliador;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.dto.perfil.ConciliadorDTO;
import co.edu.ufps.legal_cases.business.model.perfil.Conciliador;
import co.edu.ufps.legal_cases.business.repository.perfil.ConciliadorRepository;
import co.edu.ufps.legal_cases.business.service.acceso.perfil.ConciliadorAccessService;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

@Service
public class ConciliadorQueryService {

    private final ConciliadorRepository conciliadorRepository;
    private final ConciliadorMapper conciliadorMapper;
    private final ConciliadorAccessService conciliadorAccessService;

    public ConciliadorQueryService(
            ConciliadorRepository conciliadorRepository,
            ConciliadorMapper conciliadorMapper,
            ConciliadorAccessService conciliadorAccessService) {
        this.conciliadorRepository = conciliadorRepository;
        this.conciliadorMapper = conciliadorMapper;
        this.conciliadorAccessService = conciliadorAccessService;
    }

    @Transactional(readOnly = true)
    public List<ConciliadorDTO> listar() {
        conciliadorAccessService.validarPuedeListarConciliadores();

        return conciliadorRepository.findAll()
                .stream()
                .map(conciliadorMapper::convertirADTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ConciliadorDTO> listarActivos() {
        conciliadorAccessService.validarPuedeListarConciliadoresActivos();

        return conciliadorRepository.findByActivoTrue()
                .stream()
                .map(conciliadorMapper::convertirADTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public ConciliadorDTO obtenerPorId(Long id) {
        conciliadorAccessService.validarPuedeListarConciliadores();

        Conciliador conciliador = buscarPorId(id);

        return conciliadorMapper.convertirADTO(conciliador);
    }

    private Conciliador buscarPorId(Long id) {
        if (id == null) {
            throw new BusinessException("El id del conciliador es obligatorio");
        }

        return conciliadorRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Conciliador no encontrado con id: " + id));
    }
}