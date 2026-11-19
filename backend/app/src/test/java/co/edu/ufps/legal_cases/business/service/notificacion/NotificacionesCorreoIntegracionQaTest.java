package co.edu.ufps.legal_cases.business.service.notificacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import co.edu.ufps.legal_cases.business.dto.conciliacion.reunion.notificacion.DatosCorreoReunionConciliacionDTO;
import co.edu.ufps.legal_cases.business.dto.conciliacion.reunion.notificacion.ReunionConciliacionDestinatarioDTO;
import co.edu.ufps.legal_cases.business.dto.seguimiento.notificacion.DatosCorreoSeguimientoDTO;
import co.edu.ufps.legal_cases.business.dto.seguimiento.notificacion.DatosNotificacionSeguimientoDTO;
import co.edu.ufps.legal_cases.business.dto.seguimiento.notificacion.SeguimientoDestinatarioDTO;
import co.edu.ufps.legal_cases.business.model.conciliacion.Conciliacion;
import co.edu.ufps.legal_cases.business.model.conciliacion.reunion.ReunionConciliacion;
import co.edu.ufps.legal_cases.business.model.conciliacion.reunion.notificacion.MomentoNotificacionReunionConciliacion;
import co.edu.ufps.legal_cases.business.model.conciliacion.reunion.notificacion.MotivoNotificacionReunionConciliacion;
import co.edu.ufps.legal_cases.business.model.conciliacion.reunion.notificacion.ReunionConciliacionNotificacion;
import co.edu.ufps.legal_cases.business.model.conciliacion.reunion.notificacion.TipoDestinatarioReunionConciliacion;
import co.edu.ufps.legal_cases.business.model.consulta.Consulta;
import co.edu.ufps.legal_cases.business.model.seguimiento.Seguimiento;
import co.edu.ufps.legal_cases.business.model.seguimiento.notificacion.MomentoNotificacionSeguimiento;
import co.edu.ufps.legal_cases.business.model.seguimiento.notificacion.SeguimientoNotificacion;
import co.edu.ufps.legal_cases.business.model.seguimiento.notificacion.TipoNotificacionSeguimiento;
import co.edu.ufps.legal_cases.business.repository.conciliacion.ConciliacionRepository;
import co.edu.ufps.legal_cases.business.repository.conciliacion.reunion.ReunionConciliacionRepository;
import co.edu.ufps.legal_cases.business.repository.conciliacion.reunion.notificacion.ReunionConciliacionNotificacionRepository;
import co.edu.ufps.legal_cases.business.repository.consulta.ConsultaRepository;
import co.edu.ufps.legal_cases.business.repository.perfil.AdministrativoRepository;
import co.edu.ufps.legal_cases.business.repository.perfil.AsesorRepository;
import co.edu.ufps.legal_cases.business.repository.perfil.MonitorRepository;
import co.edu.ufps.legal_cases.business.repository.seguimiento.SeguimientoRepository;
import co.edu.ufps.legal_cases.business.repository.seguimiento.notificacion.SeguimientoNotificacionRepository;
import co.edu.ufps.legal_cases.business.service.conciliacion.reunion.notificacion.ReunionConciliacionCorreoService;
import co.edu.ufps.legal_cases.business.service.conciliacion.reunion.notificacion.ReunionConciliacionDestinatarioService;
import co.edu.ufps.legal_cases.business.service.conciliacion.reunion.notificacion.ReunionConciliacionEnvioNotificacionService;
import co.edu.ufps.legal_cases.business.service.conciliacion.reunion.notificacion.ReunionConciliacionNotificacionEstadoService;
import co.edu.ufps.legal_cases.business.service.seguimiento.notificacion.SeguimientoCorreoService;
import co.edu.ufps.legal_cases.business.service.seguimiento.notificacion.SeguimientoDestinatarioService;
import co.edu.ufps.legal_cases.business.service.seguimiento.notificacion.SeguimientoEnvioNotificacionService;
import co.edu.ufps.legal_cases.common.email.EmailService;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.service.auth.EmailTemplateService;
import co.edu.ufps.legal_cases.security.service.auth.activation.AccountActivationEmailService;

public class NotificacionesCorreoIntegracionQaTest {

    private static final Path EVIDENCE_DIR = Paths.get("target/test-output/INT-04");

    @BeforeEach
    void setupDir() throws IOException {
        Files.createDirectories(EVIDENCE_DIR);
    }

    @Test
    @DisplayName("Paso 1: Condición de envío - Notificación inactiva o nula no se despacha al proveedor")
    void testCondicionDeEnvioNotificacionInactiva() {
        SeguimientoRepository seguimientoRepo = mock(SeguimientoRepository.class);
        SeguimientoNotificacionRepository notifRepo = mock(SeguimientoNotificacionRepository.class);
        SeguimientoDestinatarioService destService = mock(SeguimientoDestinatarioService.class);
        SeguimientoCorreoService correoService = mock(SeguimientoCorreoService.class);

        SeguimientoEnvioNotificacionService envioService = new SeguimientoEnvioNotificacionService(
                seguimientoRepo, notifRepo, destService, correoService);

        // Caso A: Notificación nula
        envioService.enviarNotificacionPendiente(null);
        verify(correoService, never()).enviar(anyList(), any(), any(), any(), any(), any(), any());

        // Caso B: Notificación inactiva (activo = false)
        SeguimientoNotificacion inactiva = new SeguimientoNotificacion();
        inactiva.setActivo(false);
        inactiva.setEnviada(false);
        envioService.enviarNotificacionPendiente(inactiva);

        verify(correoService, never()).enviar(anyList(), any(), any(), any(), any(), any(), any());
        verify(notifRepo, never()).save(any());
        assertFalse(inactiva.getEnviada());
    }

    @Test
    @DisplayName("Paso 2: Selección y desduplicación de destinatarios por roles y tipos")
    void testSeleccionYDesduplicacionDestinatarios() {
        ConsultaRepository consultaRepo = mock(ConsultaRepository.class);
        AdministrativoRepository adminRepo = mock(AdministrativoRepository.class);
        ConciliacionRepository concilRepo = mock(ConciliacionRepository.class);

        ReunionConciliacionDestinatarioService destService = new ReunionConciliacionDestinatarioService(
                concilRepo, consultaRepo, adminRepo);

        Long conciliacionId = 100L;
        Long consultaId = 50L;
        Conciliacion concil = new Conciliacion();
        concil.setId(conciliacionId);
        Consulta consulta = new Consulta();
        consulta.setId(consultaId);
        concil.setConsulta(consulta);

        when(concilRepo.findByIdAndActivoTrue(conciliacionId)).thenReturn(Optional.of(concil));

        // Mock de destinatarios con un correo duplicado entre persona principal y partes
        when(consultaRepo.findDestinatarioPersonaPrincipalByConsultaId(consultaId)).thenReturn(List.of(
                new SeguimientoDestinatarioDTO("persona.principal@ufps.edu.co", "Juan Pérez")));
        when(consultaRepo.findDestinatariosPartesByConsultaId(consultaId)).thenReturn(List.of(
                new SeguimientoDestinatarioDTO("persona.principal@ufps.edu.co", "Juan Pérez Repetido"),
                new SeguimientoDestinatarioDTO("parte.interesada@ufps.edu.co", "María Gómez")));
        when(consultaRepo.findDestinatariosContrapartesByConsultaId(consultaId)).thenReturn(List.of(
                new SeguimientoDestinatarioDTO("contraparte@ufps.edu.co", "Pedro López"),
                new SeguimientoDestinatarioDTO("", "Sin Correo"),
                new SeguimientoDestinatarioDTO(null, "Correo Nulo")));

        List<ReunionConciliacionDestinatarioDTO> resultado = destService.obtenerDestinatariosPartes(conciliacionId);

        // Se deben desduplicar y limpiar los nulos/vacíos
        assertEquals(3, resultado.size());
        assertEquals("persona.principal@ufps.edu.co", resultado.get(0).getEmail());
        assertEquals("parte.interesada@ufps.edu.co", resultado.get(1).getEmail());
        assertEquals("contraparte@ufps.edu.co", resultado.get(2).getEmail());
    }

    @Test
    @DisplayName("Paso 3: Fallo de proveedor externo - No marca como enviada, incrementa intentos y registra error")
    void testFalloProveedorExternoRegistraErrorSinFalsaEntrega() throws IOException {
        SeguimientoRepository seguimientoRepo = mock(SeguimientoRepository.class);
        SeguimientoNotificacionRepository notifRepo = mock(SeguimientoNotificacionRepository.class);
        SeguimientoDestinatarioService destService = mock(SeguimientoDestinatarioService.class);
        SeguimientoCorreoService correoService = mock(SeguimientoCorreoService.class);

        SeguimientoEnvioNotificacionService envioService = new SeguimientoEnvioNotificacionService(
                seguimientoRepo, notifRepo, destService, correoService);

        Long seguimientoId = 77L;
        Seguimiento seguimiento = new Seguimiento();
        seguimiento.setId(seguimientoId);

        SeguimientoNotificacion notificacion = new SeguimientoNotificacion();
        notificacion.setId(1L);
        notificacion.setSeguimiento(seguimiento);
        notificacion.setTipoNotificacion(TipoNotificacionSeguimiento.PARTES);
        notificacion.setMomentoNotificacion(MomentoNotificacionSeguimiento.INMEDIATA);
        notificacion.setActivo(true);
        notificacion.setEnviada(false);
        notificacion.setIntentos(0);

        DatosCorreoSeguimientoDTO datosCorreo = new DatosCorreoSeguimientoDTO(
                seguimientoId,
                "Descripción del seguimiento para prueba",
                "CATEGORIA_A",
                10L,
                LocalDate.of(2026, 11, 20),
                3,
                true,
                true,
                false);

        when(seguimientoRepo.findDatosCorreoById(seguimientoId)).thenReturn(Optional.of(datosCorreo));
        when(destService.obtenerDestinatarios(seguimientoId, TipoNotificacionSeguimiento.PARTES))
                .thenReturn(List.of(new SeguimientoDestinatarioDTO("consultante@ufps.edu.co", "Consultante QA")));

        // Simular caída externa del proveedor de correos (timeout / error SMTP / Brevo 503)
        String mensajeFalloProveedor = "503 Service Unavailable: Conexión rechazada por proveedor SMTP";
        doThrow(new RuntimeException(mensajeFalloProveedor))
                .when(correoService).enviar(anyList(), any(), any(), any(), any(), any(), any());

        // Ejecutar el envío
        envioService.enviarNotificacionPendiente(notificacion);

        // Verificaciones críticas de INT-04:
        // 1. La notificación NO queda marcada como enviada (enviada == false)
        assertFalse(notificacion.getEnviada(), "CRÍTICO: No se debe equiparar solicitud con entrega efectiva");
        assertNull(notificacion.getFechaEnvio(), "La fecha de envío real debe ser nula al fallar");

        // 2. Se incrementa el contador de intentos
        assertEquals(1, notificacion.getIntentos());

        // 3. Se registra el detalle del error en la entidad
        assertNotNull(notificacion.getError());
        assertTrue(notificacion.getError().contains("503 Service Unavailable"));

        // 4. Se persiste el estado con error en la base de datos
        verify(notifRepo).save(notificacion);

        // Guardar evidencia textual
        String evidenciaTexto = String.format("""
                === EVIDENCIA INT-04: MANEJO DE FALLO DEL PROVEEDOR EXTERNO DE CORREO ===
                Seguimiento ID: %d
                Tipo de Notificación: %s (%s)
                Destinatarios Identificados: [consultante@ufps.edu.co]
                Proveedor Simulado: Brevo / SMTP externo
                Evento: Error simulado durante transporte -> '%s'

                RESULTADOS OBSERVADOS:
                - Estado 'enviada': %s (FALSE - Sin falso positivo de entrega)
                - Fecha de envío: %s
                - Contador de intentos: %d
                - Mensaje de error registrado en BD: '%s'
                - Persistencia: Invocación verify(notificacionRepository.save(notificacion)) ejecutada exitosamente.
                """,
                seguimientoId,
                notificacion.getTipoNotificacion(),
                notificacion.getMomentoNotificacion(),
                mensajeFalloProveedor,
                notificacion.getEnviada(),
                notificacion.getFechaEnvio(),
                notificacion.getIntentos(),
                notificacion.getError());

        Files.writeString(EVIDENCE_DIR.resolve("reporte_integracion_notificaciones.txt"), evidenciaTexto);
    }

    @Test
    @DisplayName("Paso 4: Conciliación - Fallo en envío genera alerta administrativa a coordinadores")
    void testConciliacionFalloGeneraAlertaAdministrativa() {
        ReunionConciliacionRepository reunionRepo = mock(ReunionConciliacionRepository.class);
        ReunionConciliacionNotificacionRepository notifRepo = mock(ReunionConciliacionNotificacionRepository.class);
        ReunionConciliacionDestinatarioService destService = mock(ReunionConciliacionDestinatarioService.class);
        ReunionConciliacionNotificacionEstadoService estadoService = mock(ReunionConciliacionNotificacionEstadoService.class);
        ReunionConciliacionCorreoService correoService = mock(ReunionConciliacionCorreoService.class);

        ReunionConciliacionEnvioNotificacionService envioService = new ReunionConciliacionEnvioNotificacionService(
                reunionRepo, notifRepo, destService, estadoService, correoService);

        Conciliacion conciliacion = new Conciliacion();
        conciliacion.setId(88L);
        Consulta consulta = new Consulta();
        consulta.setId(44L);
        conciliacion.setConsulta(consulta);

        ReunionConciliacionNotificacion notifParte = new ReunionConciliacionNotificacion();
        notifParte.setId(201L);
        notifParte.setConciliacion(conciliacion);
        notifParte.setTipoDestinatario(TipoDestinatarioReunionConciliacion.PARTE);
        notifParte.setMotivo(MotivoNotificacionReunionConciliacion.PROGRAMACION);
        notifParte.setDestinatarioEmail("parte.citada@ufps.edu.co");
        notifParte.setDestinatarioNombre("Ciudadano Citado");
        notifParte.setActiva(true);
        notifParte.setEnviada(false);
        notifParte.setIntentos(0);

        ReunionConciliacion reunion = new ReunionConciliacion();
        reunion.setFechaReunion(LocalDateTime.of(2026, 11, 25, 9, 0));
        when(reunionRepo.findByConciliacion_Id(88L)).thenReturn(Optional.of(reunion));

        // Administradores disponibles para recibir alerta
        ReunionConciliacionDestinatarioDTO admin1 = new ReunionConciliacionDestinatarioDTO(
                "coordinador.juridico@ufps.edu.co", "Dr. Coordinador", TipoDestinatarioReunionConciliacion.ADMINISTRATIVO);
        when(destService.obtenerDestinatariosAdministrativos()).thenReturn(List.of(admin1));

        ReunionConciliacionNotificacion alertaAdmin = new ReunionConciliacionNotificacion();
        alertaAdmin.setId(999L);
        alertaAdmin.setConciliacion(conciliacion);
        alertaAdmin.setTipoDestinatario(TipoDestinatarioReunionConciliacion.ADMINISTRATIVO);
        alertaAdmin.setDestinatarioEmail(admin1.getEmail());
        alertaAdmin.setDestinatarioNombre(admin1.getNombre());
        alertaAdmin.setActiva(true);
        alertaAdmin.setEnviada(false);

        when(estadoService.crear(eq(88L), eq(admin1), eq(MotivoNotificacionReunionConciliacion.ERROR_ENVIO),
                eq(MomentoNotificacionReunionConciliacion.INMEDIATA), any())).thenReturn(alertaAdmin);

        // Fallo al enviar a la parte citada
        doThrow(new RuntimeException("Mailbox full or provider timeout"))
                .when(correoService).enviar(eq("parte.citada@ufps.edu.co"), anyString(), any());

        // Ejecutar envío
        envioService.enviarNotificacionPendiente(notifParte);

        // Verificaciones:
        // 1. La notificación original queda con enviada = false y con error registrado
        assertFalse(notifParte.getEnviada());
        assertEquals(1, notifParte.getIntentos());
        assertTrue(notifParte.getError().contains("Mailbox full or provider timeout"));

        // 2. Se generó y envió la alerta administrativa al coordinador
        verify(estadoService).crear(eq(88L), eq(admin1), eq(MotivoNotificacionReunionConciliacion.ERROR_ENVIO),
                eq(MomentoNotificacionReunionConciliacion.INMEDIATA), any());
        verify(correoService).enviar(eq(admin1.getEmail()), eq(admin1.getNombre()), any());
    }

    @Test
    @DisplayName("Paso 5: Activación de cuenta - Fallo de proveedor no marca la cuenta ni simula entrega")
    void testActivacionFalloProveedorPropagaError() {
        EmailService emailService = mock(EmailService.class);
        EmailTemplateService templateService = mock(EmailTemplateService.class);
        AccountActivationEmailService activationService = new AccountActivationEmailService(
                emailService, templateService, "http://localhost:3000/activar-cuenta", 48);

        when(templateService.construirInvitacionActivacion(anyString(), eq(48L)))
                .thenReturn("<html>Invitación</html>");

        doThrow(new RuntimeException("Brevo API 401 Unauthorized - Clave inválida"))
                .when(emailService).enviarHtml(anyString(), isNull(), anyString(), anyString(), anyString());

        // Se verifica que lanza BusinessException controlada y no finge éxito
        BusinessException ex = assertThrows(BusinessException.class, () ->
                activationService.enviarInvitacionActivacion("usuario.nuevo@ufps.edu.co", "tok-xyz-987"));

        assertTrue(ex.getMessage().contains("No se pudo enviar el correo de activación"));
    }
}
