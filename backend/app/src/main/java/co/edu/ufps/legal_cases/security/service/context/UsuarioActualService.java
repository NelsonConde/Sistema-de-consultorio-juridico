package co.edu.ufps.legal_cases.security.service.context;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.dto.account.PerfilUsuarioActual;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.repository.account.UsuarioSistemaRepository;
import co.edu.ufps.legal_cases.security.service.account.perfil.PerfilUsuarioResolverService;

// Servicio de contexto para consultar el usuario autenticado.
// Centraliza el acceso a Spring Security y evita duplicar lecturas del usuario actual.
@Service
public class UsuarioActualService {

    private static final String ROL_ADMINISTRADOR = "Administrador";

    private final UsuarioSistemaRepository usuarioSistemaRepository;
    private final PerfilUsuarioResolverService perfilUsuarioResolverService;

    public UsuarioActualService(
            UsuarioSistemaRepository usuarioSistemaRepository,
            PerfilUsuarioResolverService perfilUsuarioResolverService) {
        this.usuarioSistemaRepository = usuarioSistemaRepository;
        this.perfilUsuarioResolverService = perfilUsuarioResolverService;
    }

    @Transactional(readOnly = true)
    public UsuarioSistema obtenerUsuarioActual() {
        String username = obtenerUsernameAutenticado();

        UsuarioSistema usuario = usuarioSistemaRepository
                .findWithRolAndPermisosByUsernameIgnoreCase(username)
                .orElseThrow(() -> new BusinessException("Usuario autenticado no encontrado"));

        validarUsuarioPuedeOperar(usuario);

        return usuario;
    }

    @Transactional(readOnly = true)
    public Long obtenerUsuarioActualId() {
        return obtenerUsuarioActual().getId();
    }

    @Transactional(readOnly = true)
    public PerfilUsuarioActual obtenerPerfilActual() {
        UsuarioSistema usuario = obtenerUsuarioActual();

        return perfilUsuarioResolverService.obtenerPerfilActivoObligatorio(usuario);
    }

    @Transactional(readOnly = true)
    public Long obtenerPerfilActualId() {
        return obtenerPerfilActual().getPerfilId();
    }

    @Transactional(readOnly = true)
    public TipoPerfilUsuario obtenerTipoPerfilActual() {
        return obtenerPerfilActual().getTipoPerfil();
    }

    public boolean tienePermiso(String permiso) {
        if (permiso == null || permiso.isBlank()) {
            return false;
        }

        Authentication authentication = obtenerAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        return obtenerAuthorities(authentication)
                .stream()
                .anyMatch(authority -> Objects.equals(authority.getAuthority(), permiso));
    }

    public boolean tieneAlgunPermiso(String... permisos) {
        if (permisos == null || permisos.length == 0) {
            return false;
        }

        return Arrays.stream(permisos)
                .anyMatch(this::tienePermiso);
    }

    public boolean tieneTodosLosPermisos(String... permisos) {
        if (permisos == null || permisos.length == 0) {
            return false;
        }

        return Arrays.stream(permisos)
                .allMatch(this::tienePermiso);
    }

    @Transactional(readOnly = true)
    public boolean esAdministrativo() {
        return esTipoPerfil(TipoPerfilUsuario.ADMINISTRATIVO);
    }

    @Transactional(readOnly = true)
    public boolean esAsesor() {
        return esTipoPerfil(TipoPerfilUsuario.ASESOR);
    }

    @Transactional(readOnly = true)
    public boolean esMonitor() {
        return esTipoPerfil(TipoPerfilUsuario.MONITOR);
    }

    @Transactional(readOnly = true)
    public boolean esEstudiante() {
        return esTipoPerfil(TipoPerfilUsuario.ESTUDIANTE);
    }

    @Transactional(readOnly = true)
    public boolean esConciliador() {
        return esTipoPerfil(TipoPerfilUsuario.CONCILIADOR);
    }

    @Transactional(readOnly = true)
    public boolean esRolAdministrador() {
        UsuarioSistema usuario = obtenerUsuarioActual();

        return usuario.getRol() != null
                && ROL_ADMINISTRADOR.equalsIgnoreCase(usuario.getRol().getNombre());
    }

    @Transactional(readOnly = true)
    public boolean esTipoPerfil(TipoPerfilUsuario tipoPerfil) {
        if (tipoPerfil == null) {
            return false;
        }

        return obtenerTipoPerfilActual() == tipoPerfil;
    }

    private String obtenerUsernameAutenticado() {
        Authentication authentication = obtenerAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException("No hay usuario autenticado");
        }

        String username = authentication.getName();

        if (username == null || username.isBlank()) {
            throw new BusinessException("No se pudo identificar el usuario autenticado");
        }

        return username;
    }

    private Authentication obtenerAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private Collection<? extends GrantedAuthority> obtenerAuthorities(Authentication authentication) {
        return authentication.getAuthorities();
    }

    private void validarUsuarioPuedeOperar(UsuarioSistema usuario) {
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new BusinessException("El usuario se encuentra inactivo");
        }

        if (usuario.getRol() == null || !Boolean.TRUE.equals(usuario.getRol().getActivo())) {
            throw new BusinessException("El rol del usuario se encuentra inactivo");
        }

        perfilUsuarioResolverService.obtenerPerfilActivoObligatorio(usuario);
    }
}