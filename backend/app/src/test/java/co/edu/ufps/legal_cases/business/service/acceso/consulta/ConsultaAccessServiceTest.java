package co.edu.ufps.legal_cases.business.service.acceso.consulta;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_CONSULTAS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_CONSULTAS;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import co.edu.ufps.legal_cases.common.exception.ResourceNotFoundException;
import co.edu.ufps.legal_cases.security.dto.account.PerfilUsuarioActual;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.service.context.UsuarioActualService;

class ConsultaAccessServiceTest {

    private UsuarioActualService usuarioActualService;
    private ConsultaRepository consultaRepository;
    private ConsultaAccessService consultaAccessService;

    @BeforeEach
    void setUp() {
        usuarioActualService = mock(UsuarioActualService.class);
        consultaRepository = mock(ConsultaRepository.class);
        consultaAccessService = new ConsultaAccessService(usuarioActualService, consultaRepository);
    }

    @Test
    void usuarioSinPermisoFuncionalDebeRecibir403AntesDeConsultarRepositorio() {
        when(usuarioActualService.tieneAlgunPermiso(VER_CONSULTAS, GESTIONAR_CONSULTAS))
                .thenReturn(false);

        assertThrows(
                AccessDeniedException.class,
                () -> consultaAccessService.validarPuedeVerConsulta(10L));

        verify(consultaRepository, never()).findById(10L);
    }

    @Test
    void usuarioConPermisoYConsultaInexistenteDebeRecibir404Generico() {
        permitirLectura();
        when(consultaRepository.findById(10L)).thenReturn(Optional.empty());

        ResourceNotFoundException error = assertThrows(
                ResourceNotFoundException.class,
                () -> consultaAccessService.validarPuedeVerConsulta(10L));

        assertEquals("Consulta no encontrada", error.getMessage());
    }

    @Test
    void usuarioConPermisoYConsultaIdNullDebeRecibir404Generico() {
        permitirLectura();

        assertConsultaNoEncontrada(() -> consultaAccessService.validarPuedeVerConsulta(null));
    }

    @Test
    void estudianteConConsultaPropiaDebeAcceder() {
        Consulta consulta = consultaConEstudiante(11L, 99L);
        permitirLectura();
        when(consultaRepository.findById(10L)).thenReturn(Optional.of(consulta));
        when(usuarioActualService.obtenerPerfilActual())
                .thenReturn(new PerfilUsuarioActual(11L, TipoPerfilUsuario.ESTUDIANTE));

        assertDoesNotThrow(() -> consultaAccessService.validarPuedeVerConsulta(10L));
    }

    @Test
    void estudianteConConsultaAjenaDebeRecibir404Generico() {
        Consulta consulta = consultaConEstudiante(12L, 99L);
        permitirLectura();
        when(consultaRepository.findById(10L)).thenReturn(Optional.of(consulta));
        when(usuarioActualService.obtenerPerfilActual())
                .thenReturn(new PerfilUsuarioActual(11L, TipoPerfilUsuario.ESTUDIANTE));

        assertConsultaNoEncontrada(() -> consultaAccessService.validarPuedeVerConsulta(10L));
    }

    @Test
    void asesorAsignadoDirectamenteDebeAcceder() {
        Consulta consulta = new Consulta();
        consulta.setAsesor(asesor(20L));
        permitirLectura();
        when(consultaRepository.findById(10L)).thenReturn(Optional.of(consulta));
        when(usuarioActualService.obtenerPerfilActual())
                .thenReturn(new PerfilUsuarioActual(20L, TipoPerfilUsuario.ASESOR));

        assertDoesNotThrow(() -> consultaAccessService.validarPuedeVerConsulta(10L));
    }

    @Test
    void asesorDelEstudianteAsociadoDebeAcceder() {
        Consulta consulta = consultaConEstudiante(11L, 20L);
        permitirLectura();
        when(consultaRepository.findById(10L)).thenReturn(Optional.of(consulta));
        when(usuarioActualService.obtenerPerfilActual())
                .thenReturn(new PerfilUsuarioActual(20L, TipoPerfilUsuario.ASESOR));

        assertDoesNotThrow(() -> consultaAccessService.validarPuedeVerConsulta(10L));
    }

    @Test
    void asesorAjenoDebeRecibir404Generico() {
        Consulta consulta = consultaConEstudiante(11L, 21L);
        consulta.setAsesor(asesor(22L));
        permitirLectura();
        when(consultaRepository.findById(10L)).thenReturn(Optional.of(consulta));
        when(usuarioActualService.obtenerPerfilActual())
                .thenReturn(new PerfilUsuarioActual(20L, TipoPerfilUsuario.ASESOR));

        assertConsultaNoEncontrada(() -> consultaAccessService.validarPuedeVerConsulta(10L));
    }

    @Test
    void monitorAsignadoDebeAcceder() {
        Consulta consulta = new Consulta();
        consulta.setMonitor(monitor(30L));
        permitirLectura();
        when(consultaRepository.findById(10L)).thenReturn(Optional.of(consulta));
        when(usuarioActualService.obtenerPerfilActual())
                .thenReturn(new PerfilUsuarioActual(30L, TipoPerfilUsuario.MONITOR));

        assertDoesNotThrow(() -> consultaAccessService.validarPuedeVerConsulta(10L));
    }

    @Test
    void monitorAjenoDebeRecibir404Generico() {
        Consulta consulta = new Consulta();
        consulta.setMonitor(monitor(31L));
        permitirLectura();
        when(consultaRepository.findById(10L)).thenReturn(Optional.of(consulta));
        when(usuarioActualService.obtenerPerfilActual())
                .thenReturn(new PerfilUsuarioActual(30L, TipoPerfilUsuario.MONITOR));

        assertConsultaNoEncontrada(() -> consultaAccessService.validarPuedeVerConsulta(10L));
    }

    @Test
    void conciliadorDebeRecibir404Generico() {
        Consulta consulta = new Consulta();
        permitirLectura();
        when(consultaRepository.findById(10L)).thenReturn(Optional.of(consulta));
        when(usuarioActualService.obtenerPerfilActual())
                .thenReturn(new PerfilUsuarioActual(40L, TipoPerfilUsuario.CONCILIADOR));

        assertConsultaNoEncontrada(() -> consultaAccessService.validarPuedeVerConsulta(10L));
    }

    @Test
    void administradorConConsultaExistenteDebeAcceder() {
        permitirLectura();
        when(usuarioActualService.esRolAdministrador()).thenReturn(true);
        when(consultaRepository.findById(10L)).thenReturn(Optional.of(new Consulta()));

        assertDoesNotThrow(() -> consultaAccessService.validarPuedeVerConsulta(10L));
    }

    @Test
    void perfilActualNullDebeSerFailClosedSinNullPointerException() {
        permitirLectura();
        when(consultaRepository.findById(10L)).thenReturn(Optional.of(new Consulta()));
        when(usuarioActualService.obtenerPerfilActual()).thenReturn(null);

        assertConsultaNoEncontrada(() -> consultaAccessService.validarPuedeVerConsulta(10L));
    }

    @Test
    void perfilConTipoNullDebeSerFailClosedSinNullPointerException() {
        permitirLectura();
        when(consultaRepository.findById(10L)).thenReturn(Optional.of(new Consulta()));
        when(usuarioActualService.obtenerPerfilActual())
                .thenReturn(new PerfilUsuarioActual(11L, null));

        assertConsultaNoEncontrada(() -> consultaAccessService.validarPuedeVerConsulta(10L));
    }

    @Test
    void perfilConPerfilIdNullDebeSerFailClosedSinNullPointerException() {
        permitirLectura();
        when(consultaRepository.findById(10L)).thenReturn(Optional.of(new Consulta()));
        when(usuarioActualService.obtenerPerfilActual())
                .thenReturn(new PerfilUsuarioActual(null, TipoPerfilUsuario.ESTUDIANTE));

        assertConsultaNoEncontrada(() -> consultaAccessService.validarPuedeVerConsulta(10L));
    }

    private void permitirLectura() {
        when(usuarioActualService.tieneAlgunPermiso(VER_CONSULTAS, GESTIONAR_CONSULTAS))
                .thenReturn(true);
    }

    private void assertConsultaNoEncontrada(Executable executable) {
        ResourceNotFoundException error = assertThrows(ResourceNotFoundException.class, executable::execute);
        assertEquals("Consulta no encontrada", error.getMessage());
    }

    private Consulta consultaConEstudiante(Long estudianteId, Long asesorId) {
        Consulta consulta = new Consulta();
        Estudiante estudiante = new Estudiante();
        estudiante.setId(estudianteId);
        estudiante.setAsesor(asesor(asesorId));
        consulta.setEstudiante(estudiante);
        return consulta;
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

    @FunctionalInterface
    private interface Executable {
        void execute();
    }
}
