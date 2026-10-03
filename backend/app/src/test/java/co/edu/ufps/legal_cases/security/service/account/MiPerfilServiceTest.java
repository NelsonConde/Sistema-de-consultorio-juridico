package co.edu.ufps.legal_cases.security.service.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import co.edu.ufps.legal_cases.audit.aop.log.AuditAspect;
import co.edu.ufps.legal_cases.audit.aop.log.AuditExpressionEvaluator;
import co.edu.ufps.legal_cases.audit.aop.log.AuditStateSnapshotService;
import co.edu.ufps.legal_cases.audit.aop.log.Auditable;
import co.edu.ufps.legal_cases.audit.model.log.AuditEvent;
import co.edu.ufps.legal_cases.audit.model.log.AuditOutcome;
import co.edu.ufps.legal_cases.audit.service.log.AuditLogService;
import co.edu.ufps.legal_cases.audit.service.log.AuditRequestContext;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.dto.account.ActualizarContactoDTO;
import co.edu.ufps.legal_cases.security.dto.account.MiPerfilDTO;
import co.edu.ufps.legal_cases.security.dto.account.PerfilContactoDatos;
import co.edu.ufps.legal_cases.security.model.access.Rol;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.service.account.perfil.contacto.PerfilContactoResolver;
import co.edu.ufps.legal_cases.security.service.account.perfil.contacto.PerfilContactoResolverRegistry;
import co.edu.ufps.legal_cases.security.service.context.UsuarioActualService;
import jakarta.persistence.EntityManager;

class MiPerfilServiceTest {

    private UsuarioActualService usuarioActualService;
    private PerfilContactoResolverRegistry registry;
    private MiPerfilService service;

    @BeforeEach
    void setUp() {
        usuarioActualService = mock(UsuarioActualService.class);
        registry = mock(PerfilContactoResolverRegistry.class);
        service = new MiPerfilService(usuarioActualService, registry, new MiPerfilMapper());
    }

    @ParameterizedTest(name = "obtenerMiPerfil para {0}")
    @EnumSource(TipoPerfilUsuario.class)
    @DisplayName("Lectura de perfil propio para los cinco tipos de perfil")
    void obtenerMiPerfil_cincoPerfiles(TipoPerfilUsuario tipo) {
        UsuarioSistema usuario = crearUsuario(1L, "usuario." + tipo.name().toLowerCase(), tipo);
        PerfilContactoResolver resolver = mock(PerfilContactoResolver.class);
        PerfilContactoDatos contacto = new PerfilContactoDatos(
                "Nombre " + tipo.name(),
                tipo.name().toLowerCase() + "@prueba.local",
                "+57 300 000 0000",
                "Sede Principal",
                "COD-" + tipo.name());

        when(usuarioActualService.obtenerUsuarioActual()).thenReturn(usuario);
        when(registry.obtenerResolver(tipo)).thenReturn(resolver);
        when(resolver.obtenerContacto(1L)).thenReturn(contacto);

        MiPerfilDTO dto = service.obtenerMiPerfil();

        assertEquals(usuario.getUsername(), dto.getUsername());
        assertEquals("Rol " + tipo.name(), dto.getRolNombre());
        assertEquals(tipo.name(), dto.getTipoPerfil());
        assertEquals("Nombre " + tipo.name(), dto.getNombre());
        assertEquals(tipo.name().toLowerCase() + "@prueba.local", dto.getEmail());
        assertEquals("+57 300 000 0000", dto.getTelefono());
        assertEquals("Sede Principal", dto.getSede());
        assertEquals("COD-" + tipo.name(), dto.getCodigo());
    }

    @ParameterizedTest(name = "actualizarContacto para {0}")
    @EnumSource(TipoPerfilUsuario.class)
    @DisplayName("Actualización válida de contacto para los cinco tipos de perfil")
    void actualizarContacto_cincoPerfiles(TipoPerfilUsuario tipo) {
        UsuarioSistema usuario = crearUsuario(2L, "user." + tipo.name().toLowerCase(), tipo);
        PerfilContactoResolver resolver = mock(PerfilContactoResolver.class);
        ActualizarContactoDTO input = new ActualizarContactoDTO();
        input.setEmail(tipo.name().toLowerCase() + ".nuevo@prueba.local");
        input.setTelefono("+57 300 123 4567");

        PerfilContactoDatos contactoActualizado = new PerfilContactoDatos(
                "Nombre " + tipo.name(),
                input.getEmail(),
                input.getTelefono(),
                "Sede Principal",
                "COD-" + tipo.name());

        when(usuarioActualService.obtenerUsuarioActual()).thenReturn(usuario);
        when(registry.obtenerResolver(tipo)).thenReturn(resolver);
        when(resolver.obtenerContacto(2L)).thenReturn(contactoActualizado);

        MiPerfilDTO dto = service.actualizarContacto(input);

        verify(resolver).actualizarContacto(2L, input);
        assertEquals(input.getEmail(), dto.getEmail());
        assertEquals(input.getTelefono(), dto.getTelefono());
        assertEquals("COD-" + tipo.name(), dto.getCodigo());
        assertEquals("Sede Principal", dto.getSede());
    }

    @Test
    @DisplayName("Duplicado de email o teléfono lanza BusinessException amigable")
    void actualizarContacto_duplicadoLanzaBusinessException() {
        UsuarioSistema usuario = crearUsuario(3L, "estudiante.demo", TipoPerfilUsuario.ESTUDIANTE);
        PerfilContactoResolver resolver = mock(PerfilContactoResolver.class);
        ActualizarContactoDTO input = new ActualizarContactoDTO();
        input.setEmail("duplicado@prueba.local");
        input.setTelefono("+57 300 999 8888");

        when(usuarioActualService.obtenerUsuarioActual()).thenReturn(usuario);
        when(registry.obtenerResolver(TipoPerfilUsuario.ESTUDIANTE)).thenReturn(resolver);
        doThrow(new DataIntegrityViolationException("unique_email"))
                .when(resolver).actualizarContacto(3L, input);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.actualizarContacto(input));

        assertEquals("El correo o telefono ya esta en uso por otro usuario", ex.getMessage());
    }

    @Test
    @DisplayName("Anotación @Auditable presente en actualizarContacto con metadatos correctos")
    void actualizarContacto_tieneAnotacionAuditable() throws NoSuchMethodException {
        Method method = MiPerfilService.class.getDeclaredMethod("actualizarContacto", ActualizarContactoDTO.class);
        Auditable auditable = method.getAnnotation(Auditable.class);

        assertNotNull(auditable, "El método actualizarContacto debe tener la anotación @Auditable");
        assertEquals("ACTUALIZAR_CONTACTO_PROPIO", auditable.action());
        assertEquals("UsuarioSistema", auditable.entityName());
        assertEquals("#result.username", auditable.entityId());
    }

    @Test
    @DisplayName("AuditAspect registra evento de éxito con actor, acción y entidad al actualizar contacto")
    void actualizarContacto_interceptadoPorAuditAspectRegistraExito() throws Throwable {
        AuditLogService auditLogService = mock(AuditLogService.class);
        AuditAspect auditAspect = new AuditAspect(
                auditLogService,
                new AuditRequestContext(),
                new AuditExpressionEvaluator(),
                new AuditStateSnapshotService(mock(EntityManager.class)));

        Method method = MiPerfilService.class.getDeclaredMethod("actualizarContacto", ActualizarContactoDTO.class);
        Auditable auditable = method.getAnnotation(Auditable.class);
        MethodSignature signature = mock(MethodSignature.class);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);

        ActualizarContactoDTO input = new ActualizarContactoDTO();
        input.setEmail("actor@prueba.local");
        input.setTelefono("+57 300 555 4444");

        MiPerfilDTO resultado = new MiPerfilDTO();
        resultado.setUsername("actor.sistema");
        resultado.setRolNombre("Rol Estudiante");
        resultado.setTipoPerfil(TipoPerfilUsuario.ESTUDIANTE.name());
        resultado.setNombre("Actor Prueba");
        resultado.setEmail(input.getEmail());
        resultado.setTelefono(input.getTelefono());
        resultado.setSede("Sede Central");
        resultado.setCodigo("COD-ACTOR");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("actor.sistema", "N/A", java.util.List.of()));

        try {
            when(signature.getMethod()).thenReturn(method);
            when(joinPoint.getSignature()).thenReturn(signature);
            when(joinPoint.getArgs()).thenReturn(new Object[] {input});
            when(joinPoint.proceed()).thenReturn(resultado);

            Object retorno = auditAspect.audit(joinPoint, auditable);
            assertEquals(resultado, retorno);

            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditLogService).recordSuccess(captor.capture());

            AuditEvent event = captor.getValue();
            assertEquals("actor.sistema", event.getActorUsername());
            assertEquals("ACTUALIZAR_CONTACTO_PROPIO", event.getAction());
            assertEquals("UsuarioSistema", event.getEntityName());
            assertEquals("actor.sistema", event.getEntityId());
            assertEquals(AuditOutcome.SUCCESS, event.getOutcome());
            assertNotNull(event.getOccurredAt());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private UsuarioSistema crearUsuario(Long id, String username, TipoPerfilUsuario tipo) {
        Rol rol = new Rol();
        rol.setNombre("Rol " + tipo.name());
        rol.setTipoPerfil(tipo);
        rol.setActivo(true);

        UsuarioSistema usuario = new UsuarioSistema();
        usuario.setId(id);
        usuario.setUsername(username);
        usuario.setRol(rol);
        usuario.setTipoPerfilActual(tipo);
        usuario.setActivo(true);
        return usuario;
    }
}
