package co.edu.ufps.legal_cases.business.service.acceso.persona;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_PERSONAS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_PERSONAS;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    // =========================================================
    // validarPuedeBuscarPersonas — tests históricos conservados
    // =========================================================

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

    // =========================================================
    // obtenerAlcanceLecturaPersonas — tests nuevos (6.1–6.4)
    // =========================================================

    @Test
    void sinPermisosObtenerAlcanceLanzaAccessDenied() {
        when(usuarioActualService.tieneAlgunPermiso(VER_PERSONAS, GESTIONAR_PERSONAS))
                .thenReturn(false);

        assertThrows(
                AccessDeniedException.class,
                personaAccessService::obtenerAlcanceLecturaPersonas);
    }

    @Test
    void conGestionarPersonasObtenerAlcanceEsGlobal() {
        when(usuarioActualService.tieneAlgunPermiso(VER_PERSONAS, GESTIONAR_PERSONAS))
                .thenReturn(true);
        when(usuarioActualService.tienePermiso(GESTIONAR_PERSONAS)).thenReturn(true);

        AlcanceLecturaPersonas alcance = personaAccessService.obtenerAlcanceLecturaPersonas();

        assertTrue(alcance.esGlobal());
        verify(usuarioActualService, never()).obtenerPerfilActual();
    }

    @Test
    void administrativoConVerPersonasObtenerAlcanceEsGlobal() {
        when(usuarioActualService.tieneAlgunPermiso(VER_PERSONAS, GESTIONAR_PERSONAS))
                .thenReturn(true);
        when(usuarioActualService.tienePermiso(GESTIONAR_PERSONAS)).thenReturn(false);
        when(usuarioActualService.obtenerPerfilActual())
                .thenReturn(new PerfilUsuarioActual(15L, TipoPerfilUsuario.ADMINISTRATIVO));

        AlcanceLecturaPersonas alcance = personaAccessService.obtenerAlcanceLecturaPersonas();

        assertTrue(alcance.esGlobal());
    }

    @Test
    void estudianteConVerPersonasObtenerAlcanceEsRestringido() {
        when(usuarioActualService.tieneAlgunPermiso(VER_PERSONAS, GESTIONAR_PERSONAS))
                .thenReturn(true);
        when(usuarioActualService.tienePermiso(GESTIONAR_PERSONAS)).thenReturn(false);
        when(usuarioActualService.obtenerPerfilActual())
                .thenReturn(new PerfilUsuarioActual(11L, TipoPerfilUsuario.ESTUDIANTE));

        AlcanceLecturaPersonas alcance = personaAccessService.obtenerAlcanceLecturaPersonas();

        assertFalse(alcance.esGlobal());
        assertEquals(TipoPerfilUsuario.ESTUDIANTE, alcance.tipoPerfil());
        assertEquals(11L, alcance.perfilId());
    }

    // =========================================================
    // validarPuedeVerDetallePersona — tests históricos adaptados
    // =========================================================

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

    // =========================================================
    // CONCILIADOR — tests nuevos (6.7)
    // =========================================================

    @Test
    void debePermitirConciliadorRelacionadoConConciliacionActivaYConsultaNoArchivada() {
        habilitarPermisoDeLectura();
        when(usuarioActualService.obtenerPerfilActual())
                .thenReturn(new PerfilUsuarioActual(14L, TipoPerfilUsuario.CONCILIADOR));
        when(personaConsultaScopeRepository.existsPersonaEnConciliacionDeConciliador(
                23L,
                14L,
                EstadoConsulta.ARCHIVADO))
                .thenReturn(true);

        assertDoesNotThrow(() -> personaAccessService.validarPuedeVerDetallePersona(23L));
    }

    @Test
    void debeRechazarConciliadorNoRelacionadoConMensajeGenerico() {
        habilitarPermisoDeLectura();
        when(usuarioActualService.obtenerPerfilActual())
                .thenReturn(new PerfilUsuarioActual(14L, TipoPerfilUsuario.CONCILIADOR));
        when(personaConsultaScopeRepository.existsPersonaEnConciliacionDeConciliador(
                96L,
                14L,
                EstadoConsulta.ARCHIVADO))
                .thenReturn(false);

        ResourceNotFoundException error = assertThrows(
                ResourceNotFoundException.class,
                () -> personaAccessService.validarPuedeVerDetallePersona(96L));

        assertEquals("Persona no encontrada", error.getMessage());
        verify(personaConsultaScopeRepository)
                .existsPersonaEnConciliacionDeConciliador(96L, 14L, EstadoConsulta.ARCHIVADO);
    }

    // =========================================================
    // ADMINISTRATIVO + VER_PERSONAS — test nuevo (6.3)
    // =========================================================

    @Test
    void debePermitirAdministrativoConVerPersonasDeFormaGlobal() {
        habilitarPermisoDeLectura();
        when(usuarioActualService.obtenerPerfilActual())
                .thenReturn(new PerfilUsuarioActual(15L, TipoPerfilUsuario.ADMINISTRATIVO));

        assertDoesNotThrow(() -> personaAccessService.validarPuedeVerDetallePersona(24L));

        verify(personaConsultaScopeRepository, never())
                .existsPersonaEnConsultaDeEstudiante(24L, 15L, EstadoConsulta.ARCHIVADO);
    }

    // =========================================================
    // Precedencia 403/404 — tests obligatorios (6.8)
    // =========================================================

    @Test
    void sinPermisoYPersonaIdNullDebeResponderAccessDenied() {
        when(usuarioActualService.tieneAlgunPermiso(VER_PERSONAS, GESTIONAR_PERSONAS))
                .thenReturn(false);

        assertThrows(
                AccessDeniedException.class,
                () -> personaAccessService.validarPuedeVerDetallePersona(null));
    }

    @Test
    void conPermisoYPersonaIdNullDebeResponderNotFoundGenerico() {
        habilitarPermisoDeLectura();

        ResourceNotFoundException error = assertThrows(
                ResourceNotFoundException.class,
                () -> personaAccessService.validarPuedeVerDetallePersona(null));

        assertEquals("Persona no encontrada", error.getMessage());
    }

    // =========================================================
    // Helper
    // =========================================================

    private void habilitarPermisoDeLectura() {
        when(usuarioActualService.tieneAlgunPermiso(VER_PERSONAS, GESTIONAR_PERSONAS))
                .thenReturn(true);
        when(usuarioActualService.tienePermiso(GESTIONAR_PERSONAS)).thenReturn(false);
    }
}