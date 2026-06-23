package co.edu.ufps.legal_cases.security.controller.account;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.ASIGNAR_ROL_USUARIOS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.CAMBIAR_ESTADO_USUARIOS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_USUARIOS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_USUARIOS;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.ufps.legal_cases.security.dto.account.UsuarioSistemaDTO;
import co.edu.ufps.legal_cases.security.dto.account.cambio.CambiarPerfilAAdministrativoDTO;
import co.edu.ufps.legal_cases.security.dto.account.cambio.CambiarPerfilAAsesorDTO;
import co.edu.ufps.legal_cases.security.dto.account.cambio.CambiarPerfilAConciliadorDTO;
import co.edu.ufps.legal_cases.security.dto.account.cambio.CambiarPerfilAEstudianteDTO;
import co.edu.ufps.legal_cases.security.dto.account.cambio.CambiarPerfilAMonitorDTO;
import co.edu.ufps.legal_cases.security.service.account.UsuarioCambioPerfilService;
import co.edu.ufps.legal_cases.security.service.account.UsuarioSistemaService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/usuarios-sistema")
public class UsuarioSistemaController {

    private final UsuarioSistemaService usuarioSistemaService;
    private final UsuarioCambioPerfilService usuarioCambioPerfilService;

    public UsuarioSistemaController(
            UsuarioSistemaService usuarioSistemaService,
            UsuarioCambioPerfilService usuarioCambioPerfilService) {
        this.usuarioSistemaService = usuarioSistemaService;
        this.usuarioCambioPerfilService = usuarioCambioPerfilService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('" + VER_USUARIOS + "', '" + GESTIONAR_USUARIOS + "')")
    public List<UsuarioSistemaDTO> listar() {
        return usuarioSistemaService.listar();
    }

    @GetMapping("/activos")
    @PreAuthorize("hasAnyAuthority('" + VER_USUARIOS + "', '" + GESTIONAR_USUARIOS + "')")
    public List<UsuarioSistemaDTO> listarActivos() {
        return usuarioSistemaService.listarActivos();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('" + VER_USUARIOS + "', '" + GESTIONAR_USUARIOS + "')")
    public UsuarioSistemaDTO obtenerPorId(@PathVariable Long id) {
        return usuarioSistemaService.obtenerPorId(id);
    }

    @PatchMapping("/{id}/activo")
    @PreAuthorize("hasAnyAuthority('" + CAMBIAR_ESTADO_USUARIOS + "', '" + GESTIONAR_USUARIOS + "')")
    public UsuarioSistemaDTO cambiarEstado(
            @PathVariable Long id,
            @RequestParam Boolean activo) {
        return usuarioSistemaService.cambiarEstado(id, activo);
    }

    // Cambia el perfil real del usuario y deja historial del cambio.
    @PatchMapping("/{id}/perfil/administrativo")
    @PreAuthorize("hasAnyAuthority('" + ASIGNAR_ROL_USUARIOS + "', '" + GESTIONAR_USUARIOS + "')")
    public UsuarioSistemaDTO cambiarAAdministrativo(
            @PathVariable Long id,
            @Valid @RequestBody CambiarPerfilAAdministrativoDTO dto,
            Authentication authentication) {

        return usuarioCambioPerfilService.cambiarAAdministrativo(
                id,
                dto,
                obtenerUsernameAutenticado(authentication));
    }

    @PatchMapping("/{id}/perfil/estudiante")
    @PreAuthorize("hasAnyAuthority('" + ASIGNAR_ROL_USUARIOS + "', '" + GESTIONAR_USUARIOS + "')")
    public UsuarioSistemaDTO cambiarAEstudiante(
            @PathVariable Long id,
            @Valid @RequestBody CambiarPerfilAEstudianteDTO dto,
            Authentication authentication) {

        return usuarioCambioPerfilService.cambiarAEstudiante(
                id,
                dto,
                obtenerUsernameAutenticado(authentication));
    }

    @PatchMapping("/{id}/perfil/asesor")
    @PreAuthorize("hasAnyAuthority('" + ASIGNAR_ROL_USUARIOS + "', '" + GESTIONAR_USUARIOS + "')")
    public UsuarioSistemaDTO cambiarAAsesor(
            @PathVariable Long id,
            @Valid @RequestBody CambiarPerfilAAsesorDTO dto,
            Authentication authentication) {

        return usuarioCambioPerfilService.cambiarAAsesor(
                id,
                dto,
                obtenerUsernameAutenticado(authentication));
    }

    @PatchMapping("/{id}/perfil/monitor")
    @PreAuthorize("hasAnyAuthority('" + ASIGNAR_ROL_USUARIOS + "', '" + GESTIONAR_USUARIOS + "')")
    public UsuarioSistemaDTO cambiarAMonitor(
            @PathVariable Long id,
            @Valid @RequestBody CambiarPerfilAMonitorDTO dto,
            Authentication authentication) {

        return usuarioCambioPerfilService.cambiarAMonitor(
                id,
                dto,
                obtenerUsernameAutenticado(authentication));
    }

    @PatchMapping("/{id}/perfil/conciliador")
    @PreAuthorize("hasAnyAuthority('" + ASIGNAR_ROL_USUARIOS + "', '" + GESTIONAR_USUARIOS + "')")
    public UsuarioSistemaDTO cambiarAConciliador(
            @PathVariable Long id,
            @Valid @RequestBody CambiarPerfilAConciliadorDTO dto,
            Authentication authentication) {

        return usuarioCambioPerfilService.cambiarAConciliador(
                id,
                dto,
                obtenerUsernameAutenticado(authentication));
    }

    private String obtenerUsernameAutenticado(Authentication authentication) {
        return authentication != null ? authentication.getName() : null;
    }
}