package co.edu.ufps.legal_cases.security.service.account.usuario;

import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import co.edu.ufps.legal_cases.security.dto.account.PerfilUsuarioActual;
import co.edu.ufps.legal_cases.security.dto.account.UsuarioSistemaDTO;
import co.edu.ufps.legal_cases.security.dto.account.cambio.CambiarPerfilAEstudianteDTO;
import co.edu.ufps.legal_cases.security.dto.account.cambio.ResultadoCambioPerfil;
import co.edu.ufps.legal_cases.security.model.access.Rol;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.repository.access.RolRepository;
import co.edu.ufps.legal_cases.security.repository.account.UsuarioSistemaRepository;
import co.edu.ufps.legal_cases.security.service.account.UsuarioCambioPerfilService;
import co.edu.ufps.legal_cases.security.service.account.UsuarioSistemaService;
import co.edu.ufps.legal_cases.security.service.account.cambio.*;
import co.edu.ufps.legal_cases.security.service.account.perfil.PerfilEstadoService;
import co.edu.ufps.legal_cases.security.service.account.perfil.PerfilUsuarioResolverService;
import co.edu.ufps.legal_cases.security.service.invariant.administracion.AdministracionInvariantService;

class UsuarioCambioPerfilServiceInvariantTest {

    private UsuarioSistemaRepository usuarioRepository;
    private RolRepository rolRepository;
    private PerfilUsuarioResolverService perfilResolver;
    private PerfilEstadoService perfilEstadoService;
    private UsuarioSistemaService usuarioService;
    private PerfilCambioHandlerRegistry registry;
    private AdministracionInvariantService invariantService;
    private UsuarioCambioPerfilService service;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioSistemaRepository.class);
        rolRepository = mock(RolRepository.class);
        perfilResolver = mock(PerfilUsuarioResolverService.class);
        perfilEstadoService = mock(PerfilEstadoService.class);
        usuarioService = mock(UsuarioSistemaService.class);
        registry = mock(PerfilCambioHandlerRegistry.class);
        invariantService = mock(AdministracionInvariantService.class);

        service = new UsuarioCambioPerfilService(
                usuarioRepository,
                rolRepository,
                perfilResolver,
                perfilEstadoService,
                mock(UsuarioCambioPerfilHistorialService.class),
                usuarioService,
                registry,
                mock(UsuarioCambioPerfilValidator.class),
                invariantService);
    }

    @Test
    void guardSeEjecutaAntesDeDesactivarPerfilAnterior() {
        Rol anterior = rol(1L, TipoPerfilUsuario.ADMINISTRATIVO);
        Rol destino = rol(2L, TipoPerfilUsuario.ESTUDIANTE);

        UsuarioSistema usuario = new UsuarioSistema();
        usuario.setId(10L);
        usuario.setActivo(true);
        usuario.setTipoPerfilActual(TipoPerfilUsuario.ADMINISTRATIVO);
        usuario.setRol(anterior);

        when(usuarioRepository.findWithRolAndPermisosById(10L))
                .thenReturn(Optional.of(usuario));
        when(perfilResolver.obtenerPerfilActivoObligatorio(usuario))
                .thenReturn(new PerfilUsuarioActual(
                        100L,
                        TipoPerfilUsuario.ADMINISTRATIVO));
        when(rolRepository.findById(2L))
                .thenReturn(Optional.of(destino));

        CambiarPerfilAEstudianteDTO dto =
                new CambiarPerfilAEstudianteDTO();
        dto.setRolId(2L);

        @SuppressWarnings("unchecked")
        PerfilCambioHandler<CambiarPerfilAEstudianteDTO> handler =
                mock(PerfilCambioHandler.class);

        when(registry.obtenerHandler(
                TipoPerfilUsuario.ESTUDIANTE,
                dto)).thenReturn(handler);
        when(handler.crearOActualizarPerfil(usuario, dto))
                .thenReturn(new ResultadoCambioPerfil(
                        200L,
                        TipoPerfilUsuario.ESTUDIANTE));
        when(usuarioService.obtenerPorId(10L))
                .thenReturn(new UsuarioSistemaDTO());

        service.cambiarAEstudiante(
                10L,
                dto,
                "actor@prueba.local");

        InOrder orden = inOrder(
                invariantService,
                usuarioRepository,
                perfilEstadoService);

        orden.verify(invariantService).validarCambioPerfil(
                10L,
                TipoPerfilUsuario.ESTUDIANTE);
        orden.verify(usuarioRepository)
                .findWithRolAndPermisosById(10L);
        orden.verify(perfilEstadoService).desactivarPerfilActual(
                10L,
                TipoPerfilUsuario.ADMINISTRATIVO);
    }

    private Rol rol(Long id, TipoPerfilUsuario tipo) {
        Rol rol = new Rol();
        rol.setId(id);
        rol.setNombre("Rol " + id);
        rol.setActivo(true);
        rol.setTipoPerfil(tipo);
        return rol;
    }
}