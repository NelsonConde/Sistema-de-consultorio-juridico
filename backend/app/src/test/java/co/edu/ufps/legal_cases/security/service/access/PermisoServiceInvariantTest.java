package co.edu.ufps.legal_cases.security.service.access;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import co.edu.ufps.legal_cases.common.exception.AdministracionInvariantException;
import co.edu.ufps.legal_cases.security.repository.access.PermisoRepository;
import co.edu.ufps.legal_cases.security.service.access.permiso.PermisoMapper;
import co.edu.ufps.legal_cases.security.service.access.permiso.PermisoValidator;
import co.edu.ufps.legal_cases.security.service.invariant.administracion.AdministracionInvariantService;

class PermisoServiceInvariantTest {

    private PermisoRepository permisoRepository;
    private PermisoValidator permisoValidator;
    private AdministracionInvariantService invariantService;

    private PermisoService service;

    @BeforeEach
    void setUp() {
        permisoRepository =
                mock(PermisoRepository.class);

        PermisoMapper permisoMapper =
                mock(PermisoMapper.class);

        permisoValidator =
                mock(PermisoValidator.class);

        invariantService =
                mock(AdministracionInvariantService.class);

        service = new PermisoService(
                permisoRepository,
                permisoMapper,
                permisoValidator,
                invariantService);
    }

    @Test
    void ejecutaGuardAntesDeConsultarOMutarPermiso() {
        Long permisoId = 10L;

        doThrow(
                new AdministracionInvariantException(
                        "operación bloqueada"))
                .when(invariantService)
                .validarCambioEstadoPermiso(
                        permisoId,
                        false);

        assertThrows(
                AdministracionInvariantException.class,
                () -> service.cambiarEstado(
                        permisoId,
                        false));

        verify(permisoValidator)
                .validarIdObligatorio(
                        permisoId);

        verify(permisoValidator)
                .validarEstadoObligatorio(
                        false);

        verify(invariantService)
                .validarCambioEstadoPermiso(
                        permisoId,
                        false);

        verifyNoInteractions(
                permisoRepository);
    }
}