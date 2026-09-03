package co.edu.ufps.legal_cases.business.service.estadisticas.estadisticas;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import co.edu.ufps.legal_cases.business.dto.estadisticas.EstadisticasSemestreDTO;
import co.edu.ufps.legal_cases.security.dto.account.PerfilUsuarioActual;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.service.context.UsuarioActualService;

class EstadisticasPerfilActualQueryServiceTest {

    private UsuarioActualService usuarioActualService;
    private EstadisticasPerfilQueryService estadisticasPerfilQueryService;
    private EstadisticasPerfilActualQueryService service;

    @BeforeEach
    void setUp() {
        usuarioActualService = mock(UsuarioActualService.class);
        estadisticasPerfilQueryService =
                mock(EstadisticasPerfilQueryService.class);

        service = new EstadisticasPerfilActualQueryService(
                usuarioActualService,
                estadisticasPerfilQueryService);
    }

    @Test
    void debeConsultarEstadisticasDelEstudianteAutenticado() {
        EstadisticasSemestreDTO esperado =
                mock(EstadisticasSemestreDTO.class);

        when(usuarioActualService.obtenerPerfilActual())
                .thenReturn(new PerfilUsuarioActual(
                        11L,
                        TipoPerfilUsuario.ESTUDIANTE));

        when(estadisticasPerfilQueryService
                .obtenerPorEstudiante(2026, 2, 11L))
                .thenReturn(esperado);

        EstadisticasSemestreDTO resultado =
                service.obtener(2026, 2);

        assertSame(esperado, resultado);

        verify(estadisticasPerfilQueryService)
                .obtenerPorEstudiante(2026, 2, 11L);
    }

    @Test
    void debeConsultarEstadisticasDelAsesorAutenticado() {
        EstadisticasSemestreDTO esperado =
                mock(EstadisticasSemestreDTO.class);

        when(usuarioActualService.obtenerPerfilActual())
                .thenReturn(new PerfilUsuarioActual(
                        22L,
                        TipoPerfilUsuario.ASESOR));

        when(estadisticasPerfilQueryService
                .obtenerPorAsesor(2026, 2, 22L))
                .thenReturn(esperado);

        EstadisticasSemestreDTO resultado =
                service.obtener(2026, 2);

        assertSame(esperado, resultado);

        verify(estadisticasPerfilQueryService)
                .obtenerPorAsesor(2026, 2, 22L);
    }

    @Test
    void debeConsultarEstadisticasDelMonitorAutenticado() {
        EstadisticasSemestreDTO esperado =
                mock(EstadisticasSemestreDTO.class);

        when(usuarioActualService.obtenerPerfilActual())
                .thenReturn(new PerfilUsuarioActual(
                        33L,
                        TipoPerfilUsuario.MONITOR));

        when(estadisticasPerfilQueryService
                .obtenerPorMonitor(2026, 2, 33L))
                .thenReturn(esperado);

        EstadisticasSemestreDTO resultado =
                service.obtener(2026, 2);

        assertSame(esperado, resultado);

        verify(estadisticasPerfilQueryService)
                .obtenerPorMonitor(2026, 2, 33L);
    }

    @Test
    void debeRechazarPerfilSinEstadisticasPersonales() {
        when(usuarioActualService.obtenerPerfilActual())
                .thenReturn(new PerfilUsuarioActual(
                        44L,
                        TipoPerfilUsuario.CONCILIADOR));

        assertThrows(
                AccessDeniedException.class,
                () -> service.obtener(2026, 2));

        verifyNoInteractions(estadisticasPerfilQueryService);
    }
}