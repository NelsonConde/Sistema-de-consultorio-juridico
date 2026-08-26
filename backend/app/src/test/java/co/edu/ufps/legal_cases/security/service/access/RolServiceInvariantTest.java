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
import co.edu.ufps.legal_cases.security.repository.access.RolRepository;
import co.edu.ufps.legal_cases.security.service.access.rol.RolMapper;
import co.edu.ufps.legal_cases.security.service.access.rol.RolValidator;
import co.edu.ufps.legal_cases.security.service.invariant.administracion.AdministracionInvariantService;

class RolServiceInvariantTest {

    private RolRepository rolRepository;
    private PermisoRepository permisoRepository;
    private RolValidator rolValidator;
    private AdministracionInvariantService invariantService;

    private RolService service;

    @BeforeEach
    void setUp() {
        rolRepository =
                mock(RolRepository.class);

        permisoRepository =
                mock(PermisoRepository.class);

        RolMapper rolMapper =
                mock(RolMapper.class);

        rolValidator =
                mock(RolValidator.class);

        invariantService =
                mock(AdministracionInvariantService.class);

        service = new RolService(
                rolRepository,
                permisoRepository,
                rolMapper,
                rolValidator,
                invariantService);
    }

    @Test
    void ejecutaGuardAntesDeConsultarOMutarRol() {
        Long rolId = 10L;

        doThrow(
                new AdministracionInvariantException(
                        "operación bloqueada"))
                .when(invariantService)
                .validarCambioEstadoRol(
                        rolId,
                        false);

        assertThrows(
                AdministracionInvariantException.class,
                () -> service.cambiarEstado(
                        rolId,
                        false));

        verify(rolValidator)
                .validarIdObligatorio(rolId);

        verify(rolValidator)
                .validarEstadoObligatorio(false);

        verify(invariantService)
                .validarCambioEstadoRol(
                        rolId,
                        false);

        verifyNoInteractions(
                rolRepository,
                permisoRepository);
    }

    @Test
    void ejecutaGuardAntesDeQuitarPermiso() {
        Long rolId = 10L;
        Long permisoId = 20L;

        doThrow(
                new AdministracionInvariantException(
                        "operación bloqueada"))
                .when(invariantService)
                .validarRetiroPermisoRol(
                        rolId,
                        permisoId);

        assertThrows(
                AdministracionInvariantException.class,
                () -> service.quitarPermiso(
                        rolId,
                        permisoId));

        verify(invariantService)
                .validarRetiroPermisoRol(
                        rolId,
                        permisoId);

        verifyNoInteractions(
                rolRepository,
                permisoRepository);
    }
}