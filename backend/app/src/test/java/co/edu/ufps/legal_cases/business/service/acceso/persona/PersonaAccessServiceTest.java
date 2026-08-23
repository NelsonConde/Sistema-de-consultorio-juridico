package co.edu.ufps.legal_cases.business.service.acceso.persona;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_PERSONAS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_PERSONAS;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import co.edu.ufps.legal_cases.business.model.consulta.EstadoConsulta;
import co.edu.ufps.legal_cases.business.repository.persona.PersonaConsultaScopeRepository;
import co.edu.ufps.legal_cases.common.exception.ResourceNotFoundException;
import co.edu.ufps.legal_cases.security.dto.account.PerfilUsuarioActual;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.service.context.UsuarioActualService;

class PersonaAccessServiceTest {

    private UsuarioActualService usuarioActualService;
    private PersonaConsultaScopeRepository personaConsultaScopeRepository;
    private PersonaAccessService personaAccessService;

    @BeforeEach
    void setUp() {
        usuarioActualService = mock(UsuarioActualService.class);
        personaConsultaScopeRepository = mock(PersonaConsultaScopeRepository.class);
        personaAccessService = new PersonaAccessService(
                usuarioActualService,
                personaConsultaScopeRepository);
    }

    @Test
    void debePermitirBusquedaMinimaConPermisoSinExigirRelacionPrevia() {
        when(usuarioActualService.tieneAlgunPermiso(VER_PERSONAS, GESTIONAR_PERSONAS))
                .thenReturn(true);

        assertDoesNotThrow(personaAccessService::validarPuedeBuscarPersonas);

        verify(usuarioActualService, never()).obtenerPerfilActual();
    }

    @Test
    void debeRechazarBusquedaSinPermisoFuncional() {
        when(usuarioActualService.tieneAlgunPermiso(VER_PERSONAS, GESTIONAR_PERSONAS))
                .thenReturn(false);

        assertThrows(
                AccessDeniedException.class,
                personaAccessService::validarPuedeBuscarPersonas);
    }

    @Test
    void debePermitirDetallePorCapacidadAdministrativaSinConsultarRolNiAlcance() {
        habilitarPermisoDeLectura();
        when(usuarioActualService.tienePermiso(GESTIONAR_PERSONAS)).thenReturn(true);

        assertDoesNotThrow(() -> personaAccessService.validarPuedeVerDetallePersona(9L));

        verify(usuarioActualService, never()).obtenerPerfilActual();
        verify(personaConsultaScopeRepository, never())
                .existsPersonaEnConsultaDeEstudiante(9L, 1L, EstadoConsulta.ARCHIVADO);
    }

    @Test
    void debePermitirEstudianteRelacionadoConConsultaNoArchivada() {
        habilitarPermisoDeLectura();
        when(usuarioActualService.obtenerPerfilActual())
                .thenReturn(new PerfilUsuarioActual(11L, TipoPerfilUsuario.ESTUDIANTE));
        when(personaConsultaScopeRepository.existsPersonaEnConsultaDeEstudiante(
                20L,
                11L,
                EstadoConsulta.ARCHIVADO))
                .thenReturn(true);

        assertDoesNotThrow(() -> personaAccessService.validarPuedeVerDetallePersona(20L));
    }

    @Test
    void debePermitirAsesorRelacionadoDirectamenteOMedianteSuEstudiante() {
        habilitarPermisoDeLectura();
        when(usuarioActualService.obtenerPerfilActual())
                .thenReturn(new PerfilUsuarioActual(12L, TipoPerfilUsuario.ASESOR));
        when(personaConsultaScopeRepository.existsPersonaEnConsultaDeAsesor(
                21L,
                12L,
                EstadoConsulta.ARCHIVADO))
                .thenReturn(true);

        assertDoesNotThrow(() -> personaAccessService.validarPuedeVerDetallePersona(21L));
    }

    @Test
    void debePermitirMonitorRelacionadoConConsultaNoArchivada() {
        habilitarPermisoDeLectura();
        when(usuarioActualService.obtenerPerfilActual())
                .thenReturn(new PerfilUsuarioActual(13L, TipoPerfilUsuario.MONITOR));
        when(personaConsultaScopeRepository.existsPersonaEnConsultaDeMonitor(
                22L,
                13L,
                EstadoConsulta.ARCHIVADO))
                .thenReturn(true);

        assertDoesNotThrow(() -> personaAccessService.validarPuedeVerDetallePersona(22L));
    }

    @Test
    void debeOcultarDetalleFueraDeAlcanceConMensajeGenerico() {
        habilitarPermisoDeLectura();
        when(usuarioActualService.obtenerPerfilActual())
                .thenReturn(new PerfilUsuarioActual(11L, TipoPerfilUsuario.ESTUDIANTE));
        when(personaConsultaScopeRepository.existsPersonaEnConsultaDeEstudiante(
                99L,
                11L,
                EstadoConsulta.ARCHIVADO))
                .thenReturn(false);

        ResourceNotFoundException error = assertThrows(
                ResourceNotFoundException.class,
                () -> personaAccessService.validarPuedeVerDetallePersona(99L));

        assertEquals("Persona no encontrada", error.getMessage());
    }

    @Test
    void debeRechazarAsesorNoRelacionado() {
        habilitarPermisoDeLectura();
        when(usuarioActualService.obtenerPerfilActual())
                .thenReturn(new PerfilUsuarioActual(12L, TipoPerfilUsuario.ASESOR));
        when(personaConsultaScopeRepository.existsPersonaEnConsultaDeAsesor(
                98L,
                12L,
                EstadoConsulta.ARCHIVADO))
                .thenReturn(false);

        assertThrows(
                ResourceNotFoundException.class,
                () -> personaAccessService.validarPuedeVerDetallePersona(98L));
    }

    @Test
    void debeRechazarMonitorNoRelacionado() {
        habilitarPermisoDeLectura();
        when(usuarioActualService.obtenerPerfilActual())
                .thenReturn(new PerfilUsuarioActual(13L, TipoPerfilUsuario.MONITOR));
        when(personaConsultaScopeRepository.existsPersonaEnConsultaDeMonitor(
                97L,
                13L,
                EstadoConsulta.ARCHIVADO))
                .thenReturn(false);

        assertThrows(
                ResourceNotFoundException.class,
                () -> personaAccessService.validarPuedeVerDetallePersona(97L));
    }

    @Test
    void debeRechazarConciliadorAunqueTengaPermisoDeLectura() {
        habilitarPermisoDeLectura();
        when(usuarioActualService.obtenerPerfilActual())
                .thenReturn(new PerfilUsuarioActual(14L, TipoPerfilUsuario.CONCILIADOR));

        ResourceNotFoundException error = assertThrows(
                ResourceNotFoundException.class,
                () -> personaAccessService.validarPuedeVerDetallePersona(23L));

        assertEquals("Persona no encontrada", error.getMessage());
        verify(personaConsultaScopeRepository, never())
                .existsPersonaEnConsultaDeEstudiante(23L, 14L, EstadoConsulta.ARCHIVADO);
    }

    @Test
    void administrativoSinGestionarPersonasNoObtieneExcepcionGlobal() {
        habilitarPermisoDeLectura();
        when(usuarioActualService.obtenerPerfilActual())
                .thenReturn(new PerfilUsuarioActual(15L, TipoPerfilUsuario.ADMINISTRATIVO));

        assertThrows(
                ResourceNotFoundException.class,
                () -> personaAccessService.validarPuedeVerDetallePersona(24L));
    }

    @Test
    void idNuloUsaLaMismaRespuestaGenerica() {
        habilitarPermisoDeLectura();

        ResourceNotFoundException error = assertThrows(
                ResourceNotFoundException.class,
                () -> personaAccessService.validarPuedeVerDetallePersona(null));

        assertEquals("Persona no encontrada", error.getMessage());
    }

    private void habilitarPermisoDeLectura() {
        when(usuarioActualService.tieneAlgunPermiso(VER_PERSONAS, GESTIONAR_PERSONAS))
                .thenReturn(true);
        when(usuarioActualService.tienePermiso(GESTIONAR_PERSONAS)).thenReturn(false);
    }
}