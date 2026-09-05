package co.edu.ufps.legal_cases.file_storage.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import co.edu.ufps.legal_cases.business.service.acceso.conciliacion.ConciliacionAccessService;
import co.edu.ufps.legal_cases.business.service.acceso.consulta.ConsultaAccessService;
import co.edu.ufps.legal_cases.business.service.acceso.proceso.ProcesoAccessService;
import co.edu.ufps.legal_cases.business.service.acceso.seguimiento.SeguimientoAccessService;
import co.edu.ufps.legal_cases.business.service.acceso.seguimiento.SeguimientoRespuestaAccessService;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.file_storage.dto.ExpedienteDocumentoResponse;
import co.edu.ufps.legal_cases.file_storage.model.FileAsset;
import co.edu.ufps.legal_cases.file_storage.model.FileAssetStatus;
import co.edu.ufps.legal_cases.file_storage.model.FileResourceType;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.service.context.UsuarioActualService;

class FileResourceServiceExpedienteTest {

    private FileAssetService fileAssetService;
    private FileValidationService validationService;
    private FileResourceAuthorizationService authorizationService;
    private StorageProvider storageProvider;
    private UsuarioActualService usuarioActualService;
    private ConsultaAccessService consultaAccessService;
    private ProcesoAccessService procesoAccessService;
    private FileResourceService service;

    @BeforeEach
    void setUp() {
        fileAssetService = mock(FileAssetService.class);
        validationService = mock(FileValidationService.class);
        authorizationService = mock(FileResourceAuthorizationService.class);
        storageProvider = mock(StorageProvider.class);
        usuarioActualService = mock(UsuarioActualService.class);
        consultaAccessService = mock(ConsultaAccessService.class);
        procesoAccessService = mock(ProcesoAccessService.class);

        service = new FileResourceService(
                fileAssetService,
                validationService,
                authorizationService,
                storageProvider,
                usuarioActualService,
                consultaAccessService,
                Duration.ofMinutes(10),
                Duration.ofMinutes(5));
    }

    @Test
    void listExpedienteFilesValidaAutorizacionDeConsultaPrimero() {
        service.listExpedienteFiles(100L, null, null, null, null, null, null);

        verify(consultaAccessService).validarPuedeVerConsulta(100L);
        verify(fileAssetService).findExpedienteFiles(
                eq(100L), eq(null), eq(null), eq(null), eq(null), any(), any());
    }

    @Test
    void listExpedienteFilesLanzaExcepcionSiFechaDesdeEsPosteriorAFechaHasta() {
        LocalDate desde = LocalDate.of(2026, 9, 10);
        LocalDate hasta = LocalDate.of(2026, 9, 1);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.listExpedienteFiles(100L, null, null, null, null, desde, hasta));

        assertEquals("La fecha desde no puede ser posterior a la fecha hasta", ex.getMessage());
    }

    @Test
    void listExpedienteFilesMapeaCorrectamenteADtoSinExponerBucketNiObjectKey() {
        UUID docLogico = UUID.randomUUID();
        LocalDateTime ahora = LocalDateTime.now();

        UsuarioSistema autor = new UsuarioSistema();
        autor.setId(7L);
        autor.setUsername("abogado@ufps.edu.co");

        FileAsset refAnterior = new FileAsset();
        refAnterior.setId(10L);

        FileAsset asset = new FileAsset();
        asset.setId(11L);
        asset.setBucket("private-bucket");
        asset.setObjectKey("consulta/100/secret-key.pdf");
        asset.setDocumentoLogico(docLogico);
        asset.setVersion(2);
        asset.setTipoDocumental("CONSULTA_ANEXO");
        asset.setOrigen("CARGA_USUARIO");
        asset.setReferenciaAnterior(refAnterior);
        asset.setResourceType("CONSULTA");
        asset.setResourceId(100L);
        asset.setOriginalFileName("memorial.pdf");
        asset.setSize(4096L);
        asset.setContentType("application/pdf");
        asset.setStatus(FileAssetStatus.VIGENTE);
        asset.setUploadedBy(autor);
        asset.setCreatedAt(ahora);

        when(fileAssetService.findExpedienteFiles(eq(100L), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(asset));

        List<ExpedienteDocumentoResponse> response = service.listExpedienteFiles(
                100L, "CONSULTA_ANEXO", "CONSULTA", "CARGA_USUARIO", "abogado@ufps.edu.co", null, null);

        assertNotNull(response);
        assertEquals(1, response.size());
        ExpedienteDocumentoResponse dto = response.get(0);

        assertEquals(11L, dto.id());
        assertEquals(docLogico, dto.documentoLogico());
        assertEquals(2, dto.version());
        assertEquals("CONSULTA_ANEXO", dto.tipoDocumental());
        assertEquals("CARGA_USUARIO", dto.origen());
        assertEquals(10L, dto.referenciaAnteriorId());
        assertEquals("CONSULTA", dto.resourceType());
        assertEquals(100L, dto.resourceId());
        assertEquals("memorial.pdf", dto.fileName());
        assertEquals(4096L, dto.size());
        assertEquals("application/pdf", dto.contentType());
        assertEquals("VIGENTE", dto.status());
        assertEquals(7L, dto.autorId());
        assertEquals("abogado@ufps.edu.co", dto.autorUsername());
        assertEquals(ahora, dto.createdAt());
    }

    @Test
    void authorizationServiceSoportaProcesoParaUploadYRead() {
        FileResourceAuthorizationService auth = new FileResourceAuthorizationService(
                mock(ConsultaAccessService.class),
                mock(SeguimientoAccessService.class),
                mock(SeguimientoRespuestaAccessService.class),
                mock(ConciliacionAccessService.class),
                procesoAccessService);

        auth.authorizeUpload(FileResourceType.PROCESO, 50L, null);
        verify(procesoAccessService).validarPuedeActualizarProceso(50L);

        auth.authorizeRead(FileResourceType.PROCESO, 50L, null);
        verify(procesoAccessService).validarPuedeVerProceso(50L);
    }
}
