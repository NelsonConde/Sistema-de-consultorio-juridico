package co.edu.ufps.legal_cases.security.service.account.perfil;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.edu.ufps.legal_cases.business.model.perfil.Administrativo;
import co.edu.ufps.legal_cases.business.model.perfil.Asesor;
import co.edu.ufps.legal_cases.business.model.perfil.Conciliador;
import co.edu.ufps.legal_cases.business.model.perfil.Estudiante;
import co.edu.ufps.legal_cases.business.model.perfil.Monitor;
import co.edu.ufps.legal_cases.business.repository.perfil.AdministrativoRepository;
import co.edu.ufps.legal_cases.business.repository.perfil.AsesorRepository;
import co.edu.ufps.legal_cases.business.repository.perfil.ConciliadorRepository;
import co.edu.ufps.legal_cases.business.repository.perfil.EstudianteRepository;
import co.edu.ufps.legal_cases.business.repository.perfil.MonitorRepository;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.common.exception.PerfilOperacionConflictException;
import co.edu.ufps.legal_cases.security.model.access.Rol;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;

@ExtendWith(MockitoExtension.class)
class PerfilOperacionEstadoValidatorTest {

    private static final Long USUARIO_ID = 27L;

    @Mock private AdministrativoRepository administrativoRepository;
    @Mock private AsesorRepository asesorRepository;
    @Mock private ConciliadorRepository conciliadorRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private MonitorRepository monitorRepository;

    @InjectMocks private PerfilOperacionEstadoValidator validator;

    @ParameterizedTest
    @MethodSource("tiposYOperacionesDirectas")
    void rechazaActivacionYDesactivacionDePerfilHistorico(
            TipoPerfilUsuario tipoHistorico,
            OperacionEstadoPerfil operacion) {

        UsuarioSistema usuario = usuario(tipoDiferente(tipoHistorico), false);
        Rol rolOriginal = usuario.getRol();

        PerfilOperacionConflictException exception = assertThrows(
                PerfilOperacionConflictException.class,
                () -> validator.validarOperacionDirecta(
                        usuario,
                        tipoHistorico,
                        operacion));

        assertEquals(
                PerfilOperacionConflictException.CODIGO,
                exception.getCodigo());
        assertFalse(usuario.getActivo());
        assertSame(rolOriginal, usuario.getRol());
        assertEquals(tipoDiferente(tipoHistorico), usuario.getTipoPerfilActual());
    }

    @ParameterizedTest
    @EnumSource(TipoPerfilUsuario.class)
    void rechazaDesactivacionDirectaDelPerfilActual(
            TipoPerfilUsuario tipoPerfilActual) {

        UsuarioSistema usuario = usuario(tipoPerfilActual, true);

        PerfilOperacionConflictException exception = assertThrows(
                PerfilOperacionConflictException.class,
                () -> validator.validarOperacionDirecta(
                        usuario,
                        tipoPerfilActual,
                        OperacionEstadoPerfil.DESACTIVAR));

        assertEquals(
                PerfilOperacionConflictException.CODIGO,
                exception.getCodigo());
        assertEquals(Boolean.TRUE, usuario.getActivo());
    }

    @ParameterizedTest
    @EnumSource(TipoPerfilUsuario.class)
    void permiteActivarPerfilActualSinOtroPerfilActivoYSinHabilitarCuenta(
            TipoPerfilUsuario tipoPerfilActual) {

        UsuarioSistema usuario = usuario(tipoPerfilActual, false);
        Rol rolOriginal = usuario.getRol();

        assertDoesNotThrow(() -> validator.validarOperacionDirecta(
                usuario,
                tipoPerfilActual,
                OperacionEstadoPerfil.ACTIVAR));

        assertFalse(usuario.getActivo());
        assertSame(rolOriginal, usuario.getRol());
        assertEquals(tipoPerfilActual, usuario.getTipoPerfilActual());
    }

    @ParameterizedTest
    @MethodSource("perfilesActualesYActivosIncompatibles")
    void rechazaActivacionDelPerfilActualSiExisteOtroActivo(
            TipoPerfilUsuario tipoPerfilActual,
            TipoPerfilUsuario otroTipoActivo) {

        configurarPerfilActivo(otroTipoActivo);
        UsuarioSistema usuario = usuario(tipoPerfilActual, false);

        PerfilOperacionConflictException exception = assertThrows(
                PerfilOperacionConflictException.class,
                () -> validator.validarOperacionDirecta(
                        usuario,
                        tipoPerfilActual,
                        OperacionEstadoPerfil.ACTIVAR));

        assertEquals(
                PerfilOperacionConflictException.CODIGO,
                exception.getCodigo());
        assertFalse(usuario.getActivo());
    }

    @ParameterizedTest
    @MethodSource("perfilesActualesYActivosIncompatibles")
    void rechazaCambioExplicitoSiExisteOtroPerfilActivo(
            TipoPerfilUsuario tipoPerfilActual,
            TipoPerfilUsuario otroTipoActivo) {

        configurarPerfilActivo(otroTipoActivo);
        UsuarioSistema usuario = usuario(tipoPerfilActual, true);
        Rol rolOriginal = usuario.getRol();

        PerfilOperacionConflictException exception = assertThrows(
                PerfilOperacionConflictException.class,
                () -> validator.validarExclusividadAntesDeCambioExplicito(
                        usuario));

        assertEquals(
                PerfilOperacionConflictException.CODIGO,
                exception.getCodigo());
        assertEquals(Boolean.TRUE, usuario.getActivo());
        assertSame(rolOriginal, usuario.getRol());
        assertEquals(tipoPerfilActual, usuario.getTipoPerfilActual());
    }

    @ParameterizedTest
    @EnumSource(TipoPerfilUsuario.class)
    void permiteCambioExplicitoSinOtroPerfilActivo(
            TipoPerfilUsuario tipoPerfilActual) {

        UsuarioSistema usuario = usuario(tipoPerfilActual, true);

        assertDoesNotThrow(() ->
                validator.validarExclusividadAntesDeCambioExplicito(usuario));
    }

    @Test
    void rechazaDatosObligatoriosAusentesSinConsultarPerfiles() {
        assertThrows(
                BusinessException.class,
                () -> validator.validarOperacionDirecta(
                        null,
                        TipoPerfilUsuario.ASESOR,
                        OperacionEstadoPerfil.ACTIVAR));

        verifyNoInteractions(
                administrativoRepository,
                asesorRepository,
                conciliadorRepository,
                estudianteRepository,
                monitorRepository);
    }

    private UsuarioSistema usuario(
            TipoPerfilUsuario tipoPerfilActual,
            boolean activo) {

        UsuarioSistema usuario = new UsuarioSistema();
        usuario.setId(USUARIO_ID);
        usuario.setActivo(activo);
        usuario.setTipoPerfilActual(tipoPerfilActual);
        usuario.setRol(new Rol());
        return usuario;
    }

    private static Stream<Arguments> tiposYOperacionesDirectas() {
        return Stream.of(TipoPerfilUsuario.values())
                .flatMap(tipo -> Stream.of(OperacionEstadoPerfil.values())
                        .map(operacion -> Arguments.of(tipo, operacion)));
    }

    private static Stream<Arguments> perfilesActualesYActivosIncompatibles() {
        return Stream.of(
                Arguments.of(TipoPerfilUsuario.ADMINISTRATIVO, TipoPerfilUsuario.ASESOR),
                Arguments.of(TipoPerfilUsuario.ASESOR, TipoPerfilUsuario.CONCILIADOR),
                Arguments.of(TipoPerfilUsuario.CONCILIADOR, TipoPerfilUsuario.ESTUDIANTE),
                Arguments.of(TipoPerfilUsuario.ESTUDIANTE, TipoPerfilUsuario.MONITOR),
                Arguments.of(TipoPerfilUsuario.MONITOR, TipoPerfilUsuario.ADMINISTRATIVO));
    }

    private TipoPerfilUsuario tipoDiferente(TipoPerfilUsuario tipoPerfil) {
        TipoPerfilUsuario[] tipos = TipoPerfilUsuario.values();
        return tipos[(tipoPerfil.ordinal() + 1) % tipos.length];
    }

    private void configurarPerfilActivo(TipoPerfilUsuario tipoPerfil) {
        switch (tipoPerfil) {
            case ADMINISTRATIVO -> when(administrativoRepository
                    .findByUsuarioSistema_IdAndActivoTrue(USUARIO_ID))
                    .thenReturn(Optional.of(new Administrativo()));
            case ASESOR -> when(asesorRepository
                    .findByUsuarioSistema_IdAndActivoTrue(USUARIO_ID))
                    .thenReturn(Optional.of(new Asesor()));
            case CONCILIADOR -> when(conciliadorRepository
                    .findByUsuarioSistema_IdAndActivoTrue(USUARIO_ID))
                    .thenReturn(Optional.of(new Conciliador()));
            case ESTUDIANTE -> when(estudianteRepository
                    .findByUsuarioSistema_IdAndActivoTrue(USUARIO_ID))
                    .thenReturn(Optional.of(new Estudiante()));
            case MONITOR -> when(monitorRepository
                    .findByUsuarioSistema_IdAndActivoTrue(USUARIO_ID))
                    .thenReturn(Optional.of(new Monitor()));
        }
    }
}
