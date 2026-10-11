package co.edu.ufps.legal_cases.business.service.acceso.consulta;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.ASIGNAR_RESPONSABLES_CONSULTA;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import co.edu.ufps.legal_cases.business.model.consulta.Consulta;
import co.edu.ufps.legal_cases.business.model.perfil.Asesor;
import co.edu.ufps.legal_cases.business.model.perfil.Estudiante;
import co.edu.ufps.legal_cases.business.model.perfil.Monitor;
import co.edu.ufps.legal_cases.business.repository.consulta.ConsultaRepository;
import co.edu.ufps.legal_cases.security.dto.account.PerfilUsuarioActual;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.service.context.UsuarioActualService;

class ConsultaAccessServiceReasignacionTest {

    private UsuarioActualService usuarioActualService;
    private ConsultaRepository consultaRepository;
    private ConsultaAccessService service;

    @BeforeEach
    void setUp() {
        usuarioActualService = mock(UsuarioActualService.class);
        consultaRepository = mock(ConsultaRepository.class);
        service = new ConsultaAccessService(usuarioActualService, consultaRepository);
    }

    @Test
    void permiteAlAdministradorConPermisoFuncional() {
        Consulta consulta = new Consulta();
        consulta.setId(10L);
        habilitarPermiso();
        when(usuarioActualService.esRolAdministrador()).thenReturn(true);
        when(consultaRepository.findById(10L)).thenReturn(Optional.of(consulta));

        assertDoesNotThrow(() -> service.validarPuedeReasignarResponsable(10L));

        verify(usuarioActualService, never()).obtenerPerfilActual();
    }

    @Test
    void permiteAlAsesorSoloEnConsultaDentroDeSuAlcance() {
        Consulta consulta = new Consulta();
        consulta.setId(10L);
        Asesor asesor = new Asesor();
        asesor.setId(20L);
        consulta.setAsesor(asesor);
        habilitarPermiso();
        when(usuarioActualService.obtenerPerfilActual())
                .thenReturn(new PerfilUsuarioActual(20L, TipoPerfilUsuario.ASESOR));
        when(consultaRepository.findById(10L)).thenReturn(Optional.of(consulta));

        assertDoesNotThrow(() -> service.validarPuedeReasignarResponsable(10L));
    }

    @Test
    void rechazaAlMonitorFueraDelAlcanceAunqueTengaPermiso() {
        Consulta consulta = new Consulta();
        consulta.setId(10L);
        Monitor monitor = new Monitor();
        monitor.setId(30L);
        consulta.setMonitor(monitor);
        habilitarPermiso();
        when(usuarioActualService.obtenerPerfilActual())
                .thenReturn(new PerfilUsuarioActual(31L, TipoPerfilUsuario.MONITOR));
        when(consultaRepository.findById(10L)).thenReturn(Optional.of(consulta));

        assertThrows(
                AccessDeniedException.class,
                () -> service.validarPuedeReasignarResponsable(10L));
    }

    @Test
    void rechazaSinPermisoAntesDeConsultarElExpediente() {
        when(usuarioActualService.tienePermiso(ASIGNAR_RESPONSABLES_CONSULTA))
                .thenReturn(false);

        assertThrows(
                AccessDeniedException.class,
                () -> service.validarPuedeReasignarResponsable(10L));

        verify(consultaRepository, never()).findById(10L);
    }

    @Test
    void rechazaEstudianteAunqueTengaPermiso() {
        habilitarPermiso();
        when(usuarioActualService.esEstudiante()).thenReturn(true);

        assertThrows(
                AccessDeniedException.class,
                () -> service.validarPuedeReasignarResponsable(10L));

        verify(consultaRepository, never()).findById(10L);
    }

    @Test
    void recalculaAccesoDelEstudianteDespuesDeLaReasignacion() {
        Consulta consulta = new Consulta();
        Estudiante anterior = new Estudiante();
        anterior.setId(40L);
        Estudiante nuevo = new Estudiante();
        nuevo.setId(41L);
        consulta.setEstudiante(anterior);
        when(usuarioActualService.obtenerPerfilActual())
                .thenReturn(new PerfilUsuarioActual(40L, TipoPerfilUsuario.ESTUDIANTE));

        assertTrue(service.puedeAccederAConsulta(consulta));

        consulta.setEstudiante(nuevo);
        assertFalse(service.puedeAccederAConsulta(consulta));

        when(usuarioActualService.obtenerPerfilActual())
                .thenReturn(new PerfilUsuarioActual(41L, TipoPerfilUsuario.ESTUDIANTE));
        assertTrue(service.puedeAccederAConsulta(consulta));
    }

    @Test
    void recalculaAccesoDelAsesorDespuesDeLaReasignacion() {
        Consulta consulta = new Consulta();
        consulta.setAsesor(asesor(50L));
        when(usuarioActualService.obtenerPerfilActual())
                .thenReturn(new PerfilUsuarioActual(50L, TipoPerfilUsuario.ASESOR));

        assertTrue(service.puedeAccederAConsulta(consulta));

        consulta.setAsesor(asesor(51L));
        assertFalse(service.puedeAccederAConsulta(consulta));

        when(usuarioActualService.obtenerPerfilActual())
                .thenReturn(new PerfilUsuarioActual(51L, TipoPerfilUsuario.ASESOR));
        assertTrue(service.puedeAccederAConsulta(consulta));
    }

    @Test
    void recalculaAccesoDelMonitorDespuesDeLaReasignacion() {
        Consulta consulta = new Consulta();
        consulta.setMonitor(monitor(60L));
        when(usuarioActualService.obtenerPerfilActual())
                .thenReturn(new PerfilUsuarioActual(60L, TipoPerfilUsuario.MONITOR));

        assertTrue(service.puedeAccederAConsulta(consulta));

        consulta.setMonitor(monitor(61L));
        assertFalse(service.puedeAccederAConsulta(consulta));

        when(usuarioActualService.obtenerPerfilActual())
                .thenReturn(new PerfilUsuarioActual(61L, TipoPerfilUsuario.MONITOR));
        assertTrue(service.puedeAccederAConsulta(consulta));
    }

    private void habilitarPermiso() {
        when(usuarioActualService.tienePermiso(ASIGNAR_RESPONSABLES_CONSULTA))
                .thenReturn(true);
        when(usuarioActualService.esEstudiante()).thenReturn(false);
        when(usuarioActualService.esRolAdministrador()).thenReturn(false);
    }

    private Asesor asesor(Long id) {
        Asesor asesor = new Asesor();
        asesor.setId(id);
        return asesor;
    }

    private Monitor monitor(Long id) {
        Monitor monitor = new Monitor();
        monitor.setId(id);
        return monitor;
    }
}
