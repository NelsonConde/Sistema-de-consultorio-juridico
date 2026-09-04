package co.edu.ufps.legal_cases.business.service.acceso.proceso;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_PROCESOS;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import co.edu.ufps.legal_cases.business.repository.proceso.ProcesoRepository;
import co.edu.ufps.legal_cases.business.service.acceso.consulta.ConsultaAccessService;
import co.edu.ufps.legal_cases.security.service.context.UsuarioActualService;

class ProcesoAccessServiceTest {

    private UsuarioActualService usuarioActualService;
    private ProcesoAccessService procesoAccessService;

    @BeforeEach
    void setUp() {
        usuarioActualService = mock(UsuarioActualService.class);
        procesoAccessService = new ProcesoAccessService(
                mock(ProcesoRepository.class),
                mock(ConsultaAccessService.class),
                usuarioActualService);
    }

    @Test
    void listarProcesosDebePermitirVerProcesos() {
        when(usuarioActualService.tienePermiso(VER_PROCESOS))
                .thenReturn(true);

        assertDoesNotThrow(() -> procesoAccessService.validarPuedeListarProcesos());
    }

    @Test
    void listarProcesosSoloConGestionarDebeResponderAccessDenied() {
        when(usuarioActualService.tienePermiso(VER_PROCESOS))
                .thenReturn(false);

        assertThrows(
                AccessDeniedException.class,
                () -> procesoAccessService.validarPuedeListarProcesos());
    }

    @Test
    void listarProcesosSinPermisoFuncionalDebeResponderAccessDenied() {
        when(usuarioActualService.tienePermiso(VER_PROCESOS))
                .thenReturn(false);

        assertThrows(
                AccessDeniedException.class,
                () -> procesoAccessService.validarPuedeListarProcesos());
    }
}
