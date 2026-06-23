package co.edu.ufps.legal_cases.business.service.perfil.administrativo;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.dto.perfil.AdministrativoDTO;
import co.edu.ufps.legal_cases.business.model.perfil.Administrativo;
import co.edu.ufps.legal_cases.business.repository.perfil.AdministrativoRepository;
import co.edu.ufps.legal_cases.business.service.acceso.perfil.AdministrativoAccessService;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

@Service
public class AdministrativoQueryService {

    private final AdministrativoRepository administrativoRepository;
    private final AdministrativoAccessService administrativoAccessService;
    private final AdministrativoMapper administrativoMapper;

    public AdministrativoQueryService(
            AdministrativoRepository administrativoRepository,
            AdministrativoAccessService administrativoAccessService,
            AdministrativoMapper administrativoMapper) {
        this.administrativoRepository = administrativoRepository;
        this.administrativoAccessService = administrativoAccessService;
        this.administrativoMapper = administrativoMapper;
    }

    @Transactional(readOnly = true)
    public List<AdministrativoDTO> listar() {
        administrativoAccessService.validarPuedeVerAdministradores();

        return administrativoRepository.findAll()
                .stream()
                .map(administrativoMapper::convertirADTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdministrativoDTO> listarActivos() {
        administrativoAccessService.validarPuedeVerAdministradores();

        return administrativoRepository.findByActivoTrue()
                .stream()
                .map(administrativoMapper::convertirADTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdministrativoDTO> listarDirectoras() {
        administrativoAccessService.validarPuedeVerAdministradores();

        return administrativoRepository.findByDirectoraTrueAndActivoTrue()
                .stream()
                .map(administrativoMapper::convertirADTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdministrativoDTO obtenerPorId(Long id) {
        administrativoAccessService.validarPuedeVerAdministradores();

        Administrativo administrativo = buscarPorId(id);

        return administrativoMapper.convertirADTO(administrativo);
    }

    private Administrativo buscarPorId(Long id) {
        if (id == null) {
            throw new BusinessException("El id del administrativo es obligatorio");
        }

        return administrativoRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Administrativo no encontrado con id: " + id));
    }
}