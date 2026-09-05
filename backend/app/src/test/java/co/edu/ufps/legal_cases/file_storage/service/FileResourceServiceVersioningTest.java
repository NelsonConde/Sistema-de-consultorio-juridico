package co.edu.ufps.legal_cases.file_storage.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import co.edu.ufps.legal_cases.file_storage.dto.FileResponse;
import co.edu.ufps.legal_cases.file_storage.dto.FileUploadRequest;
import co.edu.ufps.legal_cases.file_storage.dto.FileUploadResponse;
import co.edu.ufps.legal_cases.file_storage.model.FileAsset;
import co.edu.ufps.legal_cases.file_storage.model.FileAssetStatus;
import co.edu.ufps.legal_cases.file_storage.model.FileResourceType;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.service.context.UsuarioActualService;

class FileResourceServiceVersioningTest {

    private FileAssetService fileAssetService;
    private FileValidationService validationService;
    private FileResourceAuthorizationService authorizationService;
    private StorageProvider storageProvider;
    private UsuarioActualService usuarioActualService;
    private FileResourceService service;

    @BeforeEach
    void setUp() {
        fileAssetService = mock(FileAssetService.class);
        validationService = mock(FileValidationService.class);
        authorizationService = mock(FileResourceAuthorizationService.class);
        storageProvider = mock(StorageProvider.class);
        usuarioActualService = mock(UsuarioActualService.class);

        service = new FileResourceService(
                fileAssetService,
                validationService,
                authorizationService,
                storageProvider,
                usuarioActualService,
                Duration.ofMinutes(10),
                Duration.ofMinutes(5));
    }

    @Test
    void initiatePropagaDocumentoLogicoYTipoDocumental() {
        UUID docLogico = UUID.randomUUID();
        UUID uploadId = UUID.randomUUID();

        FileUploadRequest request = new FileUploadRequest(
                "anexo.pdf",
                1024L,
                "application/pdf",
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                docLogico,
                "CONSULTA_ANEXO");

        FileAsset asset = new FileAsset();
        asset.setUploadId(uploadId);
        asset.setObjectKey("consulta/1/key.pdf");
        asset.setDocumentoLogico(docLogico);
        asset.setVersion(2);
        asset.setTipoDocumental("CONSULTA_ANEXO");

        when(fileAssetService.startUpload(
                eq(FileResourceType.CONSULTA),
                eq(1L),
                eq("anexo.pdf"),
                eq("application/pdf"),
                eq(1024L),
                eq("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"),
                eq(docLogico),
                eq("CONSULTA_ANEXO"))).thenReturn(asset);

        when(storageProvider.createUploadUrl(eq("consulta/1/key.pdf"), any(), eq(1024L), any()))
                .thenReturn(new StorageProvider.PresignedUpload("http://upload.url", Instant.now().plusSeconds(600)));

        FileUploadResponse response = service.initiate(FileResourceType.CONSULTA, 1L, null, request);

        assertNotNull(response);
        assertEquals(uploadId.toString(), response.uploadId());
        assertEquals("http://upload.url", response.uploadUrl());

        verify(authorizationService).authorizeUpload(FileResourceType.CONSULTA, 1L, null);
    }

    @Test
    void completeRetornaFileResponseConMetadatosDeVersionado() {
        UUID uploadId = UUID.randomUUID();
        UUID docLogico = UUID.randomUUID();

        UsuarioSistema usuario = new UsuarioSistema();
        usuario.setId(10L);
        when(usuarioActualService.obtenerUsuarioActualId()).thenReturn(10L);

        FileAsset v1 = new FileAsset();
        v1.setId(1L);

        FileAsset v2 = new FileAsset();
        v2.setId(2L);
        v2.setUploadId(uploadId);
        v2.setUploadedBy(usuario);
        v2.setResourceType("CONSULTA");
        v2.setResourceId(1L);
        v2.setObjectKey("consulta/1/key2.pdf");
        v2.setSize(2048L);
        v2.setOriginalFileName("doc_v2.pdf");
        v2.setContentType("application/pdf");
        v2.setDocumentoLogico(docLogico);
        v2.setVersion(2);
        v2.setTipoDocumental("CONSULTA_ANEXO");
        v2.setOrigen("CARGA_USUARIO");
        v2.setReferenciaAnterior(v1);
        v2.setStatus(FileAssetStatus.VIGENTE);

        when(fileAssetService.findByUploadId(uploadId)).thenReturn(v2);
        when(authorizationService.parseType("CONSULTA")).thenReturn(FileResourceType.CONSULTA);
        when(storageProvider.head("consulta/1/key2.pdf"))
                .thenReturn(new StorageProvider.StorageObjectMetadata(2048L, "application/pdf"));
        when(fileAssetService.markReady(uploadId, 2048L, "application/pdf")).thenReturn(v2);

        FileResponse response = service.complete(uploadId, null);

        assertNotNull(response);
        assertEquals(2L, response.id());
        assertEquals(docLogico, response.documentoLogico());
        assertEquals(2, response.version());
        assertEquals("CONSULTA_ANEXO", response.tipoDocumental());
        assertEquals("CARGA_USUARIO", response.origen());
        assertEquals(1L, response.referenciaAnteriorId());
        assertEquals("VIGENTE", response.status());
    }

    @Test
    void listVersionsAutorizaYRetornaVersiones() {
        UUID docLogico = UUID.randomUUID();

        FileAsset v1 = new FileAsset();
        v1.setId(1L);
        v1.setDocumentoLogico(docLogico);
        v1.setVersion(1);
        v1.setSize(1024L);
        v1.setOriginalFileName("doc_v1.pdf");
        v1.setContentType("application/pdf");
        v1.setStatus(FileAssetStatus.HISTORICO);

        FileAsset v2 = new FileAsset();
        v2.setId(2L);
        v2.setDocumentoLogico(docLogico);
        v2.setVersion(2);
        v2.setSize(2048L);
        v2.setOriginalFileName("doc_v2.pdf");
        v2.setContentType("application/pdf");
        v2.setReferenciaAnterior(v1);
        v2.setStatus(FileAssetStatus.VIGENTE);

        when(fileAssetService.listVersions(docLogico)).thenReturn(List.of(v2, v1));

        List<FileResponse> result = service.listVersions(docLogico, null);

        assertEquals(2, result.size());
        assertEquals(2, result.get(0).version());
        assertEquals("VIGENTE", result.get(0).status());
        assertEquals(1, result.get(1).version());
        assertEquals("HISTORICO", result.get(1).status());

        verify(authorizationService).authorizeRead(v2, null);
    }
}
