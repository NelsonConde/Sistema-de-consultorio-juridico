package co.edu.ufps.legal_cases.security.service.account.usuario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import co.edu.ufps.legal_cases.business.model.perfil.Administrativo;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.model.access.CodigoRolBase;
import co.edu.ufps.legal_cases.security.model.access.Rol;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.repository.access.RolRepository;
import co.edu.ufps.legal_cases.security.repository.account.UsuarioSistemaRepository;

class UsuarioSistemaRegistroRolBaseTest {

    private UsuarioSistemaRepository usuarioSistemaRepository;
    private RolRepository rolRepository;
    private PasswordEncoder passwordEncoder;

    private UsuarioSistemaRegistroService service;

    @BeforeEach
    void setUp() {
        usuarioSistemaRepository =
                mock(UsuarioSistemaRepository.class);

        rolRepository =
                mock(RolRepository.class);

        passwordEncoder =
                mock(PasswordEncoder.class);

        service = new UsuarioSistemaRegistroService(
                usuarioSistemaRepository,
                rolRepository,
                passwordEncoder);

        when(passwordEncoder.encode(any(String.class)))
                .thenReturn("HASH_PRUEBA");

        when(usuarioSistemaRepository.save(any(UsuarioSistema.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void creaAdministrativoConRolBaseRenombrado() {
        Rol rolRenombrado = new Rol();
        rolRenombrado.setId(1L);
        rolRenombrado.setNombre("Gestor institucional");
        rolRenombrado.setActivo(true);
        rolRenombrado.setTipoPerfil(TipoPerfilUsuario.ADMINISTRATIVO);
        rolRenombrado.setCodigoBase(CodigoRolBase.ADMINISTRADOR);

        when(rolRepository.findByCodigoBaseAndActivoTrue(
                CodigoRolBase.ADMINISTRADOR))
                .thenReturn(Optional.of(rolRenombrado));

        when(usuarioSistemaRepository.existsByUsernameIgnoreCase(
                "admin.prueba@ufps.edu.co"))
                .thenReturn(false);

        Administrativo administrativo =
                mock(Administrativo.class);

        when(administrativo.getId())
                .thenReturn(100L);

        when(administrativo.getEmail())
                .thenReturn("admin.prueba@ufps.edu.co");

        when(administrativo.getDocumento())
                .thenReturn("1090000000");

        when(administrativo.getUsuarioSistema())
                .thenReturn(null);

        UsuarioSistema creado =
                service.crearParaAdministrativo(administrativo);

        assertSame(
                rolRenombrado,
                creado.getRol());

        assertEquals(
                TipoPerfilUsuario.ADMINISTRATIVO,
                creado.getTipoPerfilActual());

        assertEquals(
                "admin.prueba@ufps.edu.co",
                creado.getUsername());

        assertTrue(
                creado.getActivo());

        verify(rolRepository)
                .findByCodigoBaseAndActivoTrue(
                        CodigoRolBase.ADMINISTRADOR);

        verify(usuarioSistemaRepository)
                .save(creado);
    }

    @Test
    void rechazaRolBaseConTipoPerfilIncompatible() {
        Rol rolInconsistente = new Rol();
        rolInconsistente.setId(1L);
        rolInconsistente.setNombre("Gestor institucional");
        rolInconsistente.setActivo(true);
        rolInconsistente.setCodigoBase(
                CodigoRolBase.ADMINISTRADOR);

        /*
         * Estado inconsistente creado intencionalmente para comprobar
         * que la capa de negocio no confía únicamente en la base de datos.
         */
        rolInconsistente.setTipoPerfil(
                TipoPerfilUsuario.ASESOR);

        when(rolRepository.findByCodigoBaseAndActivoTrue(
                CodigoRolBase.ADMINISTRADOR))
                .thenReturn(Optional.of(rolInconsistente));

        when(usuarioSistemaRepository.existsByUsernameIgnoreCase(
                "admin.prueba@ufps.edu.co"))
                .thenReturn(false);

        Administrativo administrativo =
                mock(Administrativo.class);

        when(administrativo.getId())
                .thenReturn(100L);

        when(administrativo.getEmail())
                .thenReturn("admin.prueba@ufps.edu.co");

        when(administrativo.getDocumento())
                .thenReturn("1090000000");

        when(administrativo.getUsuarioSistema())
                .thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.crearParaAdministrativo(administrativo));

        assertTrue(
                exception.getMessage()
                        .contains("ADMINISTRADOR"));

        assertTrue(
                exception.getMessage()
                        .contains("ADMINISTRATIVO"));

        verify(usuarioSistemaRepository, never())
                .save(any(UsuarioSistema.class));
    }

    @Test
    void rechazaCreacionCuandoRolBaseNoExisteOEstaInactivo() {
        when(rolRepository.findByCodigoBaseAndActivoTrue(
                CodigoRolBase.ADMINISTRADOR))
                .thenReturn(Optional.empty());

        when(usuarioSistemaRepository.existsByUsernameIgnoreCase(
                "admin.prueba@ufps.edu.co"))
                .thenReturn(false);

        Administrativo administrativo =
                mock(Administrativo.class);

        when(administrativo.getId())
                .thenReturn(100L);

        when(administrativo.getEmail())
                .thenReturn("admin.prueba@ufps.edu.co");

        when(administrativo.getDocumento())
                .thenReturn("1090000000");

        when(administrativo.getUsuarioSistema())
                .thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.crearParaAdministrativo(administrativo));

        assertTrue(
                exception.getMessage()
                        .contains("ADMINISTRADOR"));

        verify(usuarioSistemaRepository, never())
                .save(any(UsuarioSistema.class));
    }
}