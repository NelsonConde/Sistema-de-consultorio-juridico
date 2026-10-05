package co.edu.ufps.legal_cases.business.service.consulta.pdf;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import co.edu.ufps.legal_cases.audit.aop.log.Auditable;
import co.edu.ufps.legal_cases.business.dto.consulta.ficha.FichaExpedienteDTO;
import co.edu.ufps.legal_cases.business.model.catalogo.Area;
import co.edu.ufps.legal_cases.business.model.catalogo.Municipio;
import co.edu.ufps.legal_cases.business.model.catalogo.Sede;
import co.edu.ufps.legal_cases.business.model.catalogo.Tema;
import co.edu.ufps.legal_cases.business.model.catalogo.Tipo;
import co.edu.ufps.legal_cases.business.model.conciliacion.Conciliacion;
import co.edu.ufps.legal_cases.business.model.conciliacion.EstadoConciliacion;
import co.edu.ufps.legal_cases.business.model.consulta.Consulta;
import co.edu.ufps.legal_cases.business.model.consulta.EstadoConsulta;
import co.edu.ufps.legal_cases.business.model.perfil.Asesor;
import co.edu.ufps.legal_cases.business.model.perfil.Conciliador;
import co.edu.ufps.legal_cases.business.model.perfil.Estudiante;
import co.edu.ufps.legal_cases.business.model.perfil.Monitor;
import co.edu.ufps.legal_cases.business.model.persona.Persona;
import co.edu.ufps.legal_cases.business.model.proceso.EstadoProceso;
import co.edu.ufps.legal_cases.business.model.proceso.Proceso;
import co.edu.ufps.legal_cases.business.model.seguimiento.CategoriaSeguimiento;
import co.edu.ufps.legal_cases.business.model.seguimiento.EstadoSeguimiento;
import co.edu.ufps.legal_cases.business.model.seguimiento.Seguimiento;
import co.edu.ufps.legal_cases.business.model.seguimiento.respuesta.EstadoRespuestaSeguimiento;
import co.edu.ufps.legal_cases.business.model.seguimiento.respuesta.SeguimientoRespuesta;
import co.edu.ufps.legal_cases.business.repository.conciliacion.ConciliacionRepository;
import co.edu.ufps.legal_cases.business.repository.consulta.ConsultaRepository;
import co.edu.ufps.legal_cases.business.repository.proceso.ProcesoRepository;
import co.edu.ufps.legal_cases.business.repository.seguimiento.SeguimientoRepository;
import co.edu.ufps.legal_cases.business.repository.seguimiento.respuesta.SeguimientoRespuestaRepository;
import co.edu.ufps.legal_cases.business.service.acceso.consulta.ConsultaAccessService;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.file_storage.model.FileAsset;
import co.edu.ufps.legal_cases.file_storage.model.FileAssetStatus;
import co.edu.ufps.legal_cases.file_storage.repository.FileAssetRepository;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;

class FichaExpedienteServiceTest {

    private ConsultaAccessService consultaAccessService;
    private ConsultaRepository consultaRepository;
    private SeguimientoRepository seguimientoRepository;
    private SeguimientoRespuestaRepository seguimientoRespuestaRepository;
    private ProcesoRepository procesoRepository;
    private ConciliacionRepository conciliacionRepository;
    private FileAssetRepository fileAssetRepository;
    private FichaExpedientePdfService fichaExpedientePdfService;
    private FichaExpedienteService fichaExpedienteService;

    @BeforeEach
    void setUp() {
        consultaAccessService = mock(ConsultaAccessService.class);
        consultaRepository = mock(ConsultaRepository.class);
        seguimientoRepository = mock(SeguimientoRepository.class);
        seguimientoRespuestaRepository = mock(SeguimientoRespuestaRepository.class);
        procesoRepository = mock(ProcesoRepository.class);
        conciliacionRepository = mock(ConciliacionRepository.class);
        fileAssetRepository = mock(FileAssetRepository.class);
        fichaExpedientePdfService = new FichaExpedientePdfService(); // Usamos la implementación real para verificar el binario PDF

        fichaExpedienteService = new FichaExpedienteService(
                consultaAccessService,
                consultaRepository,
                seguimientoRepository,
                seguimientoRespuestaRepository,
                procesoRepository,
                conciliacionRepository,
                fileAssetRepository,
                fichaExpedientePdfService
        );
    }

    @Test
    @DisplayName("Debe consolidar datos completos y generar un PDF válido con cabecera %PDF-")
    void debeConsolidarDatosCompletosYGenerarPdfValido() {
        Long consultaId = 1L;
        Consulta consulta = crearConsultaCompleta(consultaId);

        when(consultaRepository.findByIdConPartes(consultaId)).thenReturn(Optional.of(consulta));
        when(consultaRepository.findByIdConContrapartes(consultaId)).thenReturn(Optional.of(consulta));

        Seguimiento seguimiento = crearSeguimiento(10L, consulta);
        when(seguimientoRepository.findByConsulta_IdAndActivoTrueOrderByFechaCreacionDesc(consultaId))
                .thenReturn(List.of(seguimiento));

        SeguimientoRespuesta respuesta = crearRespuesta(100L, seguimiento);
        when(seguimientoRespuestaRepository.findBySeguimiento_IdAndActivoTrueOrderByFechaCreacionDesc(10L))
                .thenReturn(List.of(respuesta));

        Proceso proceso = crearProceso(50L, consulta);
        when(procesoRepository.findByConsulta_IdAndActivoTrueOrderByIdDesc(consultaId))
                .thenReturn(List.of(proceso));

        Conciliacion conciliacion = crearConciliacion(70L, consulta);
        when(conciliacionRepository.findByConsulta_IdAndActivoTrueOrderByIdDesc(consultaId))
                .thenReturn(List.of(conciliacion));

        FileAsset fileConsulta = crearFileAsset(1L, "CONSULTA", consultaId, "poder_especial.pdf");
        FileAsset fileSeguimiento = crearFileAsset(2L, "SEGUIMIENTO", 10L, "informe_actividades.pdf");

        when(fileAssetRepository.findByResourceTypeAndResourceIdAndStatusInOrderByCreatedAtDesc(
                eq("CONSULTA"), eq(consultaId), anyList()))
                .thenReturn(List.of(fileConsulta));

        when(fileAssetRepository.findByResourceTypeAndResourceIdInAndStatusInOrderByCreatedAtDesc(
                eq("SEGUIMIENTO"), anyList(), anyList()))
                .thenReturn(List.of(fileSeguimiento));

        // Ejecutar consolidación y generación de PDF
        byte[] pdfBytes = fichaExpedienteService.generarFichaPdf(consultaId);

        // Verificaciones
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500, "El PDF generado debe contener información y un tamaño significativo");

        // Validar firma mágica de archivo PDF (%PDF-)
        String magicHeader = new String(pdfBytes, 0, 5, StandardCharsets.US_ASCII);
        assertEquals("%PDF-", magicHeader, "El archivo debe iniciar con la cabecera estándar de PDF");

        // Validar que se llamó a la verificación de acceso ANTES de consultar los datos
        verify(consultaAccessService).validarPuedeVerConsulta(consultaId);
    }

    @Test
    @DisplayName("Debe generar PDF exitosamente cuando los campos opcionales son nulos o están vacíos")
    void debeGenerarPdfConCamposOpcionalesNulos() {
        Long consultaId = 2L;
        Consulta consulta = new Consulta();
        consulta.setId(consultaId);
        consulta.setFecha(LocalDate.of(2026, 10, 1));
        consulta.setDescripcion("Consulta simple sin datos adicionales");
        consulta.setHechos("Hechos básicos");
        consulta.setPretensiones("Pretensiones básicas");
        consulta.setConceptoJuridico("Concepto preliminar");
        consulta.setTramite("Orientación");
        consulta.setEstado(EstadoConsulta.PENDIENTE);
        consulta.setLastUpdatedAt(LocalDate.of(2026, 10, 2));

        // Solicitante mínimo
        Persona solicitante = new Persona();
        solicitante.setId(20L);
        solicitante.setNombres("Carlos");
        solicitante.setApellidos("Gómez");
        solicitante.setTipoDocumento("CC");
        solicitante.setNumeroDocumento("98765432");
        consulta.setPersona(solicitante);

        // Sin partes adicionales, contrapartes, clasificación detallada ni responsables
        consulta.setPartes(Collections.emptyList());
        consulta.setContrapartes(Collections.emptyList());

        when(consultaRepository.findByIdConPartes(consultaId)).thenReturn(Optional.of(consulta));
        when(consultaRepository.findByIdConContrapartes(consultaId)).thenReturn(Optional.of(consulta));
        when(seguimientoRepository.findByConsulta_IdAndActivoTrueOrderByFechaCreacionDesc(consultaId))
                .thenReturn(Collections.emptyList());
        when(procesoRepository.findByConsulta_IdAndActivoTrueOrderByIdDesc(consultaId))
                .thenReturn(Collections.emptyList());
        when(conciliacionRepository.findByConsulta_IdAndActivoTrueOrderByIdDesc(consultaId))
                .thenReturn(Collections.emptyList());
        when(fileAssetRepository.findByResourceTypeAndResourceIdAndStatusInOrderByCreatedAtDesc(
                eq("CONSULTA"), eq(consultaId), anyList()))
                .thenReturn(Collections.emptyList());

        byte[] pdfBytes = assertDoesNotThrow(() -> fichaExpedienteService.generarFichaPdf(consultaId));

        assertNotNull(pdfBytes);
        String magicHeader = new String(pdfBytes, 0, 5, StandardCharsets.US_ASCII);
        assertEquals("%PDF-", magicHeader);
    }

    @Test
    @DisplayName("Debe rechazar la generación y propagar AccessDeniedException ante intento de tercero ajeno")
    void debeRechazarAccesoAjeno() {
        Long consultaId = 99L;

        // Simula rechazo por falta de pertenencia o permisos en la consulta
        doThrow(new AccessDeniedException("No tiene permisos para ver esta consulta"))
                .when(consultaAccessService).validarPuedeVerConsulta(consultaId);

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> fichaExpedienteService.generarFichaPdf(consultaId));

        assertEquals("No tiene permisos para ver esta consulta", ex.getMessage());

        // Asegurar que NO se consultaron repositorios posteriores
        verify(consultaRepository, never()).findByIdConPartes(any());
        verify(seguimientoRepository, never()).findByConsulta_IdAndActivoTrueOrderByFechaCreacionDesc(any());
        verify(procesoRepository, never()).findByConsulta_IdAndActivoTrueOrderByIdDesc(any());
        verify(conciliacionRepository, never()).findByConsulta_IdAndActivoTrueOrderByIdDesc(any());
    }

    @Test
    @DisplayName("Debe lanzar BusinessException cuando la consulta no existe")
    void debeLanzarExcepcionCuandoConsultaNoExiste() {
        Long consultaId = 404L;
        doNothing().when(consultaAccessService).validarPuedeVerConsulta(consultaId);
        when(consultaRepository.findByIdConPartes(consultaId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> fichaExpedienteService.generarFichaPdf(consultaId));

        assertTrue(ex.getMessage().contains("Consulta no encontrada"));
    }

    @Test
    @DisplayName("Debe lanzar BusinessException cuando consultaId es nulo")
    void debeLanzarExcepcionCuandoIdEsNulo() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> fichaExpedienteService.generarFichaPdf(null));

        assertTrue(ex.getMessage().contains("La consulta es obligatoria"));
    }

    @Test
    @DisplayName("Verifica que las anotaciones @Auditable estén configuradas con la acción y entidad correctas")
    void debeVerificarAnotacionesDeAuditoria() throws NoSuchMethodException {
        Method metodoGenerarPdf = FichaExpedienteService.class.getMethod("generarFichaPdf", Long.class);
        Auditable auditablePdf = metodoGenerarPdf.getAnnotation(Auditable.class);
        assertNotNull(auditablePdf, "El método generarFichaPdf debe tener la anotación @Auditable");
        assertEquals("GENERAR_FICHA_EXPEDIENTE_PDF", auditablePdf.action());
        assertEquals("Consulta", auditablePdf.entityName());
        assertEquals("#consultaId", auditablePdf.entityId());

        Method metodoConsolidar = FichaExpedienteService.class.getMethod("consolidarFichaExpediente", Long.class);
        Auditable auditableConsolidar = metodoConsolidar.getAnnotation(Auditable.class);
        assertNotNull(auditableConsolidar, "El método consolidarFichaExpediente debe tener la anotación @Auditable");
        assertEquals("CONSULTAR_FICHA_EXPEDIENTE", auditableConsolidar.action());
        assertEquals("Consulta", auditableConsolidar.entityName());
        assertEquals("#consultaId", auditableConsolidar.entityId());
    }

    @Test
    @DisplayName("AuditAspect registra evento SUCCESS con acción GENERAR_FICHA_EXPEDIENTE_PDF al generar PDF")
    void debeRegistrarEventoAuditoriaExitoso() throws Throwable {
        co.edu.ufps.legal_cases.audit.service.log.AuditLogService auditLogService = mock(co.edu.ufps.legal_cases.audit.service.log.AuditLogService.class);
        co.edu.ufps.legal_cases.audit.aop.log.AuditAspect auditAspect = new co.edu.ufps.legal_cases.audit.aop.log.AuditAspect(
                auditLogService,
                new co.edu.ufps.legal_cases.audit.service.log.AuditRequestContext(),
                new co.edu.ufps.legal_cases.audit.aop.log.AuditExpressionEvaluator(),
                new co.edu.ufps.legal_cases.audit.aop.log.AuditStateSnapshotService(mock(jakarta.persistence.EntityManager.class)));

        Method method = FichaExpedienteService.class.getDeclaredMethod("generarFichaPdf", Long.class);
        Auditable auditable = method.getAnnotation(Auditable.class);
        org.aspectj.lang.reflect.MethodSignature signature = mock(org.aspectj.lang.reflect.MethodSignature.class);
        org.aspectj.lang.ProceedingJoinPoint joinPoint = mock(org.aspectj.lang.ProceedingJoinPoint.class);

        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{15L});
        when(joinPoint.proceed()).thenReturn(new byte[]{1, 2, 3});

        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("funcionario.autorizado", "N/A"));

        try {
            auditAspect.audit(joinPoint, auditable);

            org.mockito.ArgumentCaptor<co.edu.ufps.legal_cases.audit.model.log.AuditEvent> captor =
                    org.mockito.ArgumentCaptor.forClass(co.edu.ufps.legal_cases.audit.model.log.AuditEvent.class);
            verify(auditLogService).recordSuccess(captor.capture());
            co.edu.ufps.legal_cases.audit.model.log.AuditEvent event = captor.getValue();
            assertEquals("GENERAR_FICHA_EXPEDIENTE_PDF", event.getAction());
            assertEquals("Consulta", event.getEntityName());
            assertEquals("15", event.getEntityId());
            assertEquals(co.edu.ufps.legal_cases.audit.model.log.AuditOutcome.SUCCESS, event.getOutcome());
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("AuditAspect registra evento DENIED con acción GENERAR_FICHA_EXPEDIENTE_PDF ante rechazo de tercero")
    void debeRegistrarEventoAuditoriaDenegado() throws Throwable {
        co.edu.ufps.legal_cases.audit.service.log.AuditLogService auditLogService = mock(co.edu.ufps.legal_cases.audit.service.log.AuditLogService.class);
        co.edu.ufps.legal_cases.audit.aop.log.AuditAspect auditAspect = new co.edu.ufps.legal_cases.audit.aop.log.AuditAspect(
                auditLogService,
                new co.edu.ufps.legal_cases.audit.service.log.AuditRequestContext(),
                new co.edu.ufps.legal_cases.audit.aop.log.AuditExpressionEvaluator(),
                new co.edu.ufps.legal_cases.audit.aop.log.AuditStateSnapshotService(mock(jakarta.persistence.EntityManager.class)));

        Method method = FichaExpedienteService.class.getDeclaredMethod("generarFichaPdf", Long.class);
        Auditable auditable = method.getAnnotation(Auditable.class);
        org.aspectj.lang.reflect.MethodSignature signature = mock(org.aspectj.lang.reflect.MethodSignature.class);
        org.aspectj.lang.ProceedingJoinPoint joinPoint = mock(org.aspectj.lang.ProceedingJoinPoint.class);

        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{99L});
        when(joinPoint.proceed()).thenThrow(new AccessDeniedException("No tiene permisos para ver esta consulta"));

        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("estudiante.ajeno", "N/A"));

        try {
            assertThrows(AccessDeniedException.class, () -> auditAspect.audit(joinPoint, auditable));

            org.mockito.ArgumentCaptor<co.edu.ufps.legal_cases.audit.model.log.AuditEvent> captor =
                    org.mockito.ArgumentCaptor.forClass(co.edu.ufps.legal_cases.audit.model.log.AuditEvent.class);
            verify(auditLogService).recordFailure(captor.capture());
            co.edu.ufps.legal_cases.audit.model.log.AuditEvent event = captor.getValue();
            assertEquals("GENERAR_FICHA_EXPEDIENTE_PDF", event.getAction());
            assertEquals("Consulta", event.getEntityName());
            assertEquals("99", event.getEntityId());
            assertEquals(co.edu.ufps.legal_cases.audit.model.log.AuditOutcome.DENIED, event.getOutcome());
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("El índice documental consolida solo metadatos y no expone buckets ni claves internas")
    void debeGarantizarSeguridadEnIndiceDocumental() {
        Long consultaId = 10L;
        Consulta consulta = crearConsultaCompleta(consultaId);

        when(consultaRepository.findByIdConPartes(consultaId)).thenReturn(Optional.of(consulta));
        when(consultaRepository.findByIdConContrapartes(consultaId)).thenReturn(Optional.of(consulta));
        when(seguimientoRepository.findByConsulta_IdAndActivoTrueOrderByFechaCreacionDesc(consultaId))
                .thenReturn(Collections.emptyList());
        when(procesoRepository.findByConsulta_IdAndActivoTrueOrderByIdDesc(consultaId))
                .thenReturn(Collections.emptyList());
        when(conciliacionRepository.findByConsulta_IdAndActivoTrueOrderByIdDesc(consultaId))
                .thenReturn(Collections.emptyList());

        FileAsset asset = new FileAsset();
        asset.setId(301L);
        asset.setBucket("bucket-secreto-privado");
        asset.setObjectKey("claves/internas/ruta/oculta.pdf");
        asset.setOriginalFileName("documento_publico.pdf");
        asset.setResourceType("CONSULTA");
        asset.setResourceId(consultaId);
        asset.setContentType("application/pdf");
        asset.setSize(102400L);
        asset.setStatus(FileAssetStatus.READY);
        asset.setCreatedAt(LocalDateTime.of(2026, 9, 20, 10, 0));

        UsuarioSistema uploader = new UsuarioSistema();
        uploader.setUsername("asesor.juridico");
        asset.setUploadedBy(uploader);

        when(fileAssetRepository.findByResourceTypeAndResourceIdAndStatusInOrderByCreatedAtDesc(
                eq("CONSULTA"), eq(consultaId), anyList()))
                .thenReturn(List.of(asset));

        FichaExpedienteDTO dto = fichaExpedienteService.consolidarFichaExpediente(consultaId);

        assertNotNull(dto.getIndiceDocumental());
        assertEquals(1, dto.getIndiceDocumental().size());

        var docDto = dto.getIndiceDocumental().get(0);
        assertEquals(301L, docDto.getId());
        assertEquals("documento_publico.pdf", docDto.getNombreArchivo());
        assertEquals("CONSULTA", docDto.getRecursoOrigen());
        assertEquals("100.0 KB", docDto.getTamañoFormateado());
        assertEquals("asesor.juridico", docDto.getCargadoPor());
    }

    // --- Métodos utilitarios de prueba ---

    private Consulta crearConsultaCompleta(Long id) {
        Consulta c = new Consulta();
        c.setId(id);
        c.setFecha(LocalDate.of(2026, 9, 15));
        c.setDescripcion("Asesoría jurídica por conflicto de linderos");
        c.setHechos("El vecino ocupó una franja de 2 metros en el costado sur del predio.");
        c.setPretensiones("Restitución pacífica del área y delimitación catastral.");
        c.setConceptoJuridico("Se recomienda iniciar trámite de deslinde o conciliación prejudicial.");
        c.setTramite("Conciliación Extrajudicial");
        c.setObservaciones("Partes con disposición previa de diálogo");
        c.setTipoViolencia(null);
        c.setEstado(EstadoConsulta.EN_PROCESO);
        c.setResultado("En proceso de concertación");
        c.setLastUpdatedAt(LocalDate.of(2026, 9, 28));

        Sede sede = new Sede();
        sede.setNombre("Sede Central Cúcuta");
        c.setSede(sede);

        Area area = new Area();
        area.setNombre("Derecho Civil");
        c.setArea(area);

        Tema tema = new Tema();
        tema.setNombre("Propiedad y Posesión");
        c.setTema(tema);

        Tipo tipo = new Tipo();
        tipo.setNombre("Acción Reivindicatoria");
        c.setTipo(tipo);

        Persona solicitante = new Persona();
        solicitante.setId(101L);
        solicitante.setNombres("María");
        solicitante.setApellidos("Rodríguez");
        solicitante.setTipoDocumento("CC");
        solicitante.setNumeroDocumento("1090123456");
        solicitante.setTelefono("3101234567");
        solicitante.setCorreo("maria.rodriguez@email.com");
        solicitante.setDireccion("Calle 10 # 5-20");
        solicitante.setEstrato(2);
        Municipio m = new Municipio();
        m.setNombre("Cúcuta");
        solicitante.setMunicipio(m);
        c.setPersona(solicitante);

        Persona parteAdicional = new Persona();
        parteAdicional.setId(102L);
        parteAdicional.setNombres("Pedro");
        parteAdicional.setApellidos("Rodríguez");
        parteAdicional.setTipoDocumento("CC");
        parteAdicional.setNumeroDocumento("1090654321");
        c.setPartes(List.of(parteAdicional));

        Persona contraparte = new Persona();
        contraparte.setId(103L);
        contraparte.setNombres("José");
        contraparte.setApellidos("Pérez");
        contraparte.setTipoDocumento("CC");
        contraparte.setNumeroDocumento("88123456");
        c.setContrapartes(List.of(contraparte));

        Estudiante e = new Estudiante();
        e.setId(5L);
        e.setNombre("Juan Estudiante");
        e.setDocumento("1150998877");
        e.setEmail("estudiante@ufps.edu.co");
        e.setTelefono("3159988776");
        e.setCodigo("1151234");
        c.setEstudiante(e);

        Asesor a = new Asesor();
        a.setId(3L);
        a.setNombre("Dra. Clara Asesora");
        a.setDocumento("60123456");
        a.setEmail("asesora@ufps.edu.co");
        a.setTelefono("3001122334");
        c.setAsesor(a);

        Monitor mon = new Monitor();
        mon.setId(4L);
        mon.setNombre("Carlos Monitor");
        mon.setDocumento("1090334455");
        mon.setEmail("monitor@ufps.edu.co");
        mon.setTelefono("3203344556");
        c.setMonitor(mon);

        return c;
    }

    private Seguimiento crearSeguimiento(Long id, Consulta c) {
        Seguimiento s = new Seguimiento();
        s.setId(id);
        s.setConsulta(c);
        s.setDescripcion("Elaborar solicitud de conciliación y notificar al convocante");
        s.setFechaCreacion(LocalDateTime.of(2026, 9, 16, 9, 30));
        s.setFechaEntrega(LocalDate.of(2026, 9, 23));
        s.setEstado(EstadoSeguimiento.PENDIENTE);
        s.setActivo(true);
        s.setAlertaDisciplinaria(false);

        CategoriaSeguimiento cat = new CategoriaSeguimiento();
        cat.setNombre("Actuación Procesal");
        s.setCategoriaSeguimiento(cat);

        UsuarioSistema autor = new UsuarioSistema();
        autor.setUsername("docente.asesor");
        s.setAutor(autor);
        return s;
    }

    private SeguimientoRespuesta crearRespuesta(Long id, Seguimiento s) {
        SeguimientoRespuesta r = new SeguimientoRespuesta();
        r.setId(id);
        r.setSeguimiento(s);
        r.setContenido("Se redactó la solicitud de conciliación y se remitió a revisión.");
        r.setObservacionRevision("Aprobado sin modificaciones");
        r.setEstado(EstadoRespuestaSeguimiento.APROBADA);
        r.setFechaCreacion(LocalDateTime.of(2026, 9, 20, 14, 0));

        Estudiante e = new Estudiante();
        e.setNombre("Juan Estudiante");
        r.setEstudiante(e);
        return r;
    }

    private Proceso crearProceso(Long id, Consulta c) {
        Proceso p = new Proceso();
        p.setId(id);
        p.setConsulta(c);
        p.setNumeroRadicado("54001400300120260012300");
        p.setEstado(EstadoProceso.PENDIENTE);
        p.setActivo(true);
        return p;
    }

    private Conciliacion crearConciliacion(Long id, Consulta c) {
        Conciliacion conc = new Conciliacion();
        conc.setId(id);
        conc.setConsulta(c);
        conc.setFechaCreacion(LocalDateTime.of(2026, 9, 18, 11, 0));
        conc.setFechaConciliacion(LocalDateTime.of(2026, 10, 5, 10, 0));
        conc.setActivo(true);

        EstadoConciliacion ec = new EstadoConciliacion();
        ec.setCodigo("CONC_PROG");
        ec.setNombre("Programada");
        conc.setEstado(ec);

        Conciliador conciliador = new Conciliador();
        conciliador.setNombre("Dr. Andrés Conciliador");
        conc.setConciliador(conciliador);

        Estudiante est = new Estudiante();
        est.setNombre("Juan Estudiante");
        conc.setEstudiante(est);

        return conc;
    }

    private FileAsset crearFileAsset(Long id, String resourceType, Long resourceId, String fileName) {
        FileAsset asset = new FileAsset();
        asset.setId(id);
        asset.setResourceType(resourceType);
        asset.setResourceId(resourceId);
        asset.setOriginalFileName(fileName);
        asset.setContentType("application/pdf");
        asset.setSize(45056L);
        asset.setStatus(FileAssetStatus.READY);
        asset.setCreatedAt(LocalDateTime.of(2026, 9, 17, 16, 20));

        UsuarioSistema uploader = new UsuarioSistema();
        uploader.setUsername("estudiante.juridico");
        asset.setUploadedBy(uploader);
        return asset;
    }
}
