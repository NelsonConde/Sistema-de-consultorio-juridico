package co.edu.ufps.legal_cases.business.service.acceso.conciliacion;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_CONCILIACIONES;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_CONCILIACIONES;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import co.edu.ufps.legal_cases.business.repository.conciliacion.ConciliacionRepository;
import co.edu.ufps.legal_cases.business.repository.consulta.ConsultaRepository;
import co.edu.ufps.legal_cases.security.service.context.UsuarioActualService;

class ConciliacionAccessServiceTest {

    private UsuarioActualService usuarioActualService;
    private ConciliacionAccessService conciliacionAccessService;

    @BeforeEach
    void setUp() {
        usuarioActualService = mock(UsuarioActualService.class);
        conciliacionAccessService = new ConciliacionAccessService(
                mock(ConciliacionRepository.class),
                mock(ConsultaRepository.class),
                mock(ConciliacionAlcanceService.class),
                usuarioActualService);
    }

    @Test
    void listarConciliacionesDebePermitirVerConciliaciones() {
        when(usuarioActualService.tienePermiso(VER_CONCILIACIONES))
                .thenReturn(true);

        assertDoesNotThrow(() -> conciliacionAccessService.validarPuedeListarConciliaciones());
    }

    @Test
    void listarConciliacionesSoloConGestionarDebeResponderAccessDenied() {
        when(usuarioActualService.tienePermiso(VER_CONCILIACIONES))
                .thenReturn(false);
        when(usuarioActualService.tienePermiso(GESTIONAR_CONCILIACIONES))
                .thenReturn(true);

        assertThrows(
                AccessDeniedException.class,
                () -> conciliacionAccessService.validarPuedeListarConciliaciones());
    }

    @Test
    void listarConciliacionesSinPermisoFuncionalDebeResponderAccessDenied() {
        when(usuarioActualService.tienePermiso(VER_CONCILIACIONES))
                .thenReturn(false);

        assertThrows(
                AccessDeniedException.class,
                () -> conciliacionAccessService.validarPuedeListarConciliaciones());
    }
}
