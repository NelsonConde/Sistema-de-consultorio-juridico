package co.edu.ufps.legal_cases.security.service.context;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import co.edu.ufps.legal_cases.security.dto.account.PerfilUsuarioActual;
import co.edu.ufps.legal_cases.security.model.access.Rol;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.repository.account.UsuarioSistemaRepository;
import co.edu.ufps.legal_cases.security.service.account.perfil.PerfilUsuarioResolverService;

class UsuarioActualServiceAdministrativeTest {

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void reconoceAdministradorAunqueRolSeaRenombrado() {
        UsuarioSistemaRepository repository =
                mock(UsuarioSistemaRepository.class);
        PerfilUsuarioResolverService resolver =
                mock(PerfilUsuarioResolverService.class);

        Rol rol = new Rol();
        rol.setId(1L);
        rol.setNombre("Gestor institucional");
        rol.setActivo(true);
        rol.setTipoPerfil(TipoPerfilUsuario.ADMINISTRATIVO);

        UsuarioSistema usuario = new UsuarioSistema();
        usuario.setId(10L);
        usuario.setUsername("gestor@prueba.local");
        usuario.setActivo(true);
        usuario.setTipoPerfilActual(TipoPerfilUsuario.ADMINISTRATIVO);
        usuario.setRol(rol);

        when(repository.findWithRolAndPermisosByUsernameIgnoreCase(
                "gestor@prueba.local"))
                .thenReturn(Optional.of(usuario));
        when(resolver.obtenerPerfilActivoObligatorio(usuario))
                .thenReturn(new PerfilUsuarioActual(
                        5L,
                        TipoPerfilUsuario.ADMINISTRATIVO));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "gestor@prueba.local",
                        null,
                        List.of()));

        UsuarioActualService service =
                new UsuarioActualService(repository, resolver);

        assertTrue(service.esAdministradorOperativo());
        assertTrue(service.esRolAdministrador());
    }
}