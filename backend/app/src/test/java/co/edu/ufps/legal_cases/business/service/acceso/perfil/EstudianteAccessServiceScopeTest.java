package co.edu.ufps.legal_cases.business.service.acceso.perfil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import co.edu.ufps.legal_cases.business.repository.perfil.EstudianteRepository;
import co.edu.ufps.legal_cases.security.dto.account.PerfilUsuarioActual;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.service.context.UsuarioActualService;

class EstudianteAccessServiceScopeTest {

    private UsuarioActualService usuarioActualService;
    private EstudianteAccessService estudianteAccessService;

    @BeforeEach
    void setUp() {
        usuarioActualService = mock(UsuarioActualService.class);
        estudianteAccessService = new EstudianteAccessService(
                usuarioActualService,
                mock(EstudianteRepository.class));
    }

    @Test
    void obtenerAsesorActualIdDebeUsarIdDelPerfilYNoIdDelUsuarioSistema() {
        when(usuarioActualService.obtenerPerfilActual())
                .thenReturn(new PerfilUsuarioActual(42L, TipoPerfilUsuario.ASESOR));

        assertEquals(42L, estudianteAccessService.obtenerAsesorActualId());

        verify(usuarioActualService).obtenerPerfilActual();
    }

    @Test
    void obtenerAsesorActualIdDebeRechazarOtroTipoDePerfil() {
        when(usuarioActualService.obtenerPerfilActual())
                .thenReturn(new PerfilUsuarioActual(7L, TipoPerfilUsuario.ESTUDIANTE));

        assertThrows(
                AccessDeniedException.class,
                estudianteAccessService::obtenerAsesorActualId);
    }

    @Test
    void alcanceGlobalDebeDependerDeAdministradorOperativo() {
        when(usuarioActualService.esRolAdministrador()).thenReturn(true, false);

        assertTrue(estudianteAccessService.puedeVerTodosLosEstudiantes());
        assertFalse(estudianteAccessService.puedeVerTodosLosEstudiantes());

        verify(usuarioActualService, org.mockito.Mockito.times(2)).esRolAdministrador();
    }
}
