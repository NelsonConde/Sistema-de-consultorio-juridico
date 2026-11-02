package co.edu.ufps.legal_cases.security.service.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.dto.account.PerfilUsuarioActual;
import co.edu.ufps.legal_cases.security.dto.account.UsuarioSistemaDTO;
import co.edu.ufps.legal_cases.security.dto.auth.login.LoginRequestDTO;
import co.edu.ufps.legal_cases.security.dto.auth.login.LoginResponseDTO;
import co.edu.ufps.legal_cases.security.dto.auth.login.LoginResultDTO;
import co.edu.ufps.legal_cases.security.dto.auth.password.CambiarPasswordRequestDTO;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.repository.account.UsuarioSistemaRepository;
import co.edu.ufps.legal_cases.security.service.account.usuario.UsuarioSistemaMapper;
import co.edu.ufps.legal_cases.security.service.auth.login.LoginMapper;
import co.edu.ufps.legal_cases.security.service.auth.login.LoginValidator;
import co.edu.ufps.legal_cases.security.service.auth.password.CambioPasswordValidator;
import co.edu.ufps.legal_cases.security.service.jwt.JwtService;
import co.edu.ufps.legal_cases.audit.aop.log.Auditable;

@Service
public class AuthService {

    private final UsuarioSistemaRepository usuarioSistemaRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginMapper loginMapper;
    private final LoginValidator loginValidator;
    private final CambioPasswordValidator cambioPasswordValidator;
    private final UsuarioSistemaMapper usuarioSistemaMapper;

    public AuthService(
            UsuarioSistemaRepository usuarioSistemaRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            LoginMapper loginMapper,
            LoginValidator loginValidator,
            CambioPasswordValidator cambioPasswordValidator,
            UsuarioSistemaMapper usuarioSistemaMapper) {
        this.usuarioSistemaRepository = usuarioSistemaRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.loginMapper = loginMapper;
        this.loginValidator = loginValidator;
        this.cambioPasswordValidator = cambioPasswordValidator;
        this.usuarioSistemaMapper = usuarioSistemaMapper;
    }

    @Transactional(readOnly = true)
    public LoginResultDTO login(LoginRequestDTO dto) {
        String username = loginValidator.obtenerUsernameNormalizado(dto);

        loginValidator.validarPasswordInformada(dto.getPassword());

        UsuarioSistema usuario = usuarioSistemaRepository
                .findWithRolAndPermisosByUsernameIgnoreCase(username)
                .orElseThrow(() -> new BusinessException("Usuario o contraseña incorrectos"));

        PerfilUsuarioActual perfilActual =
                loginValidator.validarUsuarioPuedeAutenticarse(usuario);

        loginValidator.validarPasswordCorrecta(
                dto.getPassword(),
                usuario,
                passwordEncoder);

        LoginResponseDTO response =
                loginMapper.convertirAResponse(usuario, perfilActual);

        String token = jwtService.generarToken(
                usuario.getUsername(),
                usuario.getAuthVersion());

        return new LoginResultDTO(response, token);
    }

    @Transactional(readOnly = true)
    public UsuarioSistemaDTO me(String token) {
        UsuarioSistema usuario = obtenerUsuarioAutenticado(token);

        return usuarioSistemaMapper.convertirADTO(usuario);
    }

    @Transactional
    @Auditable(
            action = "CAMBIAR_PASSWORD_PROPIA",
            entityName = "UsuarioSistema",
            entityId = "'self'")
    public String cambiarPassword(
            String token,
            CambiarPasswordRequestDTO dto) {

        UsuarioSistema usuario =
                obtenerUsuarioAutenticadoParaActualizar(token);

        cambioPasswordValidator.validarDatosCambioPassword(dto);

        cambioPasswordValidator.validarPasswordActual(
                dto.getPasswordActual(),
                usuario,
                passwordEncoder);

        cambioPasswordValidator.validarPasswordNuevaDiferente(
                dto.getPasswordNueva(),
                usuario,
                passwordEncoder);

        usuario.setPasswordHash(
                passwordEncoder.encode(dto.getPasswordNueva()));

        // Invalida todos los JWT emitidos con la versión anterior.
        usuario.setAuthVersion(usuario.getAuthVersion() + 1L);

        usuarioSistemaRepository.save(usuario);

        // La sesión que realizó correctamente el cambio recibe una credencial nueva.
        return jwtService.generarToken(
                usuario.getUsername(),
                usuario.getAuthVersion());
    }

    private UsuarioSistema obtenerUsuarioAutenticadoParaActualizar(
            String token) {

        if (token == null || token.isBlank()) {
            throw new BusinessException("No hay sesión activa");
        }

        String username = jwtService.obtenerUsername(token);

        Long usuarioId = usuarioSistemaRepository
                .findIdByUsernameIgnoreCase(username)
                .orElseThrow(() ->
                        new BusinessException("Usuario no encontrado"));

        UsuarioSistema usuario = usuarioSistemaRepository
                .findByIdForUpdate(usuarioId)
                .orElseThrow(() ->
                        new BusinessException("Usuario no encontrado"));

        validarVersionAutenticacion(token, usuario);
        loginValidator.validarUsuarioPuedeAutenticarse(usuario);

        return usuario;
    }

    // Obtiene el usuario autenticado a partir del JWT enviado en la cookie.
    // También valida que la credencial siga vigente y que usuario, rol y perfil estén activos.
    private UsuarioSistema obtenerUsuarioAutenticado(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException("No hay sesión activa");
        }

        String username = jwtService.obtenerUsername(token);

        UsuarioSistema usuario = usuarioSistemaRepository
                .findWithRolAndPermisosByUsernameIgnoreCase(username)
                .orElseThrow(() ->
                        new BusinessException("Usuario no encontrado"));

        validarVersionAutenticacion(token, usuario);
        loginValidator.validarUsuarioPuedeAutenticarse(usuario);

        return usuario;
    }

    private void validarVersionAutenticacion(
            String token,
            UsuarioSistema usuario) {

        long versionToken = jwtService.obtenerAuthVersion(token);

        if (versionToken != usuario.getAuthVersion()) {
            throw new BusinessException("Token inválido o expirado");
        }
    }
}