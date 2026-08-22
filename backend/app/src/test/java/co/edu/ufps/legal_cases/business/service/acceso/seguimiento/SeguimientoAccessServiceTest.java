package co.edu.ufps.legal_cases.business.service.acceso.seguimiento;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_ALERTAS_DISCIPLINARIAS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import co.edu.ufps.legal_cases.business.repository.seguimiento.SeguimientoRepository;
import co.edu.ufps.legal_cases.business.service.acceso.consulta.ConsultaAccessService;
import co.edu.ufps.legal_cases.security.dto.account.PerfilUsuarioActual;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.service.context.UsuarioActualService;

class SeguimientoAccessServiceTest {

    private SeguimientoRepository seguimientoRepository;
    private ConsultaAccessService consultaAccessService;
    private UsuarioActualService usuarioActualService;
    private SeguimientoAccessService seguimientoAccessService;

    @BeforeEach
    void setUp() {
        seguimientoRepository = mock(SeguimientoRepository.class);
        consultaAccessService = mock(ConsultaAccessService.class);
        usuarioActualService = mock(UsuarioActualService.class);

        seguimientoAccessService = new SeguimientoAccessService(
                seguimientoRepository,
                consultaAccessService,
                usuarioActualService);
    }

    @Test
    void debeResolverAlcanceGlobalParaAdministrador() {
        when(usuarioActualService.tienePermiso(VER_ALERTAS_DISCIPLINARIAS))
                .thenReturn(true);
        when(usuarioActualService.esRolAdministrador())
                .thenReturn(true);

        AlcanceAlertasDisciplinarias alcance = seguimientoAccessService.resolverAlcanceAlertasDisciplinarias();

        assertEquals(AlcanceAlertasDisciplinarias.Tipo.GLOBAL, alcance.tipo());
        assertNull(alcance.perfilId());
    }

    @Test
    void debeResolverAlcanceDelAsesor() {
        when(usuarioActualService.tienePermiso(VER_ALERTAS_DISCIPLINARIAS))
                .thenReturn(true);
        when(usuarioActualService.esRolAdministrador())
                .thenReturn(false);
        when(usuarioActualService.obtenerPerfilActual())
                .thenReturn(new PerfilUsuarioActual(10L, TipoPerfilUsuario.ASESOR));

        AlcanceAlertasDisciplinarias alcance = seguimientoAccessService.resolverAlcanceAlertasDisciplinarias();

        assertEquals(AlcanceAlertasDisciplinarias.Tipo.ASESOR, alcance.tipo());
        assertEquals(10L, alcance.perfilId());
    }

    @Test
    void debeResolverAlcanceDelMonitor() {
        when(usuarioActualService.tienePermiso(VER_ALERTAS_DISCIPLINARIAS))
                .thenReturn(true);
        when(usuarioActualService.esRolAdministrador())
                .thenReturn(false);
        when(usuarioActualService.obtenerPerfilActual())
                .thenReturn(new PerfilUsuarioActual(20L, TipoPerfilUsuario.MONITOR));

        AlcanceAlertasDisciplinarias alcance = seguimientoAccessService.resolverAlcanceAlertasDisciplinarias();

        assertEquals(AlcanceAlertasDisciplinarias.Tipo.MONITOR, alcance.tipo());
        assertEquals(20L, alcance.perfilId());
    }

    @Test
    void debeRechazarPerfilSinAlcanceDisciplinario() {
        when(usuarioActualService.tienePermiso(VER_ALERTAS_DISCIPLINARIAS))
                .thenReturn(true);
        when(usuarioActualService.esRolAdministrador())
                .thenReturn(false);
        when(usuarioActualService.obtenerPerfilActual())
                .thenReturn(new PerfilUsuarioActual(30L, TipoPerfilUsuario.ESTUDIANTE));

        assertThrows(
                AccessDeniedException.class,
                () -> seguimientoAccessService.resolverAlcanceAlertasDisciplinarias());
    }

    @Test
    void debeRechazarUsuarioSinPermisoDeAlertasDisciplinarias() {
        when(usuarioActualService.tienePermiso(VER_ALERTAS_DISCIPLINARIAS))
                .thenReturn(false);

        assertThrows(
                AccessDeniedException.class,
                () -> seguimientoAccessService.resolverAlcanceAlertasDisciplinarias());
    }
}