package co.edu.ufps.legal_cases.security.service.account;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.dto.account.UsuarioSistemaDTO;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.repository.account.UsuarioSistemaRepository;
import co.edu.ufps.legal_cases.security.service.account.usuario.UsuarioSistemaMapper;
import co.edu.ufps.legal_cases.security.service.account.usuario.UsuarioSistemaValidator;

@Service
public class UsuarioSistemaService {

    private final UsuarioSistemaRepository usuarioSistemaRepository;
    private final UsuarioSistemaMapper usuarioSistemaMapper;
    private final UsuarioSistemaValidator usuarioSistemaValidator;

    public UsuarioSistemaService(
            UsuarioSistemaRepository usuarioSistemaRepository,
            UsuarioSistemaMapper usuarioSistemaMapper,
            UsuarioSistemaValidator usuarioSistemaValidator) {
        this.usuarioSistemaRepository = usuarioSistemaRepository;
        this.usuarioSistemaMapper = usuarioSistemaMapper;
        this.usuarioSistemaValidator = usuarioSistemaValidator;
    }

    @Transactional(readOnly = true, noRollbackFor = BusinessException.class)
    public List<UsuarioSistemaDTO> listar() {
        return usuarioSistemaRepository.findAll()
                .stream()
                .map(usuarioSistemaMapper::convertirADTO)
                .toList();
    }

    @Transactional(readOnly = true, noRollbackFor = BusinessException.class)
    public List<UsuarioSistemaDTO> listarActivos() {
        return usuarioSistemaRepository.findByActivoTrue()
                .stream()
                .map(usuarioSistemaMapper::convertirADTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public UsuarioSistemaDTO obtenerPorId(Long id) {
        UsuarioSistema usuario = buscarUsuarioConRolYPermisos(id);

        return usuarioSistemaMapper.convertirADTO(usuario);
    }

    @Transactional
    public UsuarioSistemaDTO cambiarEstado(Long id, Boolean activo) {
        UsuarioSistema usuario = buscarUsuarioConRolYPermisos(id);

        usuarioSistemaValidator.validarCambioEstado(usuario, activo);

        usuario.setActivo(activo);

        return usuarioSistemaMapper.convertirADTO(usuarioSistemaRepository.save(usuario));
    }

    private UsuarioSistema buscarUsuarioConRolYPermisos(Long id) {
        usuarioSistemaValidator.validarIdObligatorio(id);

        return usuarioSistemaRepository.findWithRolAndPermisosById(id)
                .orElseThrow(() -> new BusinessException("Usuario del sistema no encontrado con id: " + id));
    }
}