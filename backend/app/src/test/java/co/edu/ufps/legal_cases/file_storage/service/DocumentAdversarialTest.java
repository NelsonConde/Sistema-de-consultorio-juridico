package co.edu.ufps.legal_cases.file_storage.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.security.access.AccessDeniedException;

import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.file_storage.dto.FileUploadRequest;
import co.edu.ufps.legal_cases.file_storage.model.FileAsset;
import co.edu.ufps.legal_cases.file_storage.model.FileAssetStatus;
import co.edu.ufps.legal_cases.file_storage.model.FileResourceType;
import co.edu.ufps.legal_cases.file_storage.repository.FileAssetRepository;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.service.context.UsuarioActualService;

class DocumentAdversarialTest {

    private static final Duration UPLOAD_TTL = Duration.ofMinutes(10);
    private static final Duration DOWNLOAD_TTL = Duration.ofMinutes(5);

    private FileAssetService assetService;
    private FileValidationService validationService;
    private FileResourceAuthorizationService authorizationService;
    private StorageProvider storageProvider;
    private UsuarioActualService currentUser;
    private FileResourceService service;

    @BeforeEach
    void setUp() {
        assetService = mock(FileAssetService.class);
        validationService = mock(FileValidationService.class);
        authorizationService = mock(FileResourceAuthorizationService.class);
        storageProvider = mock(StorageProvider.class);
        currentUser = mock(UsuarioActualService.class);
        service = new FileResourceService(
                assetService,
                validationService,
                authorizationService,
                storageProvider,
                currentUser,
                UPLOAD_TTL,
                DOWNLOAD_TTL);
    }

    @Test
    @DisplayName("QA-DOC-01: un usuario ajeno no inicia carga ni genera URL")
    void usuarioAjenoNoPuedeIniciarCarga() {
        doThrow(new AccessDeniedException("recurso ajeno"))
                .when(authorizationService).authorizeUpload(FileResourceType.CONSULTA, 41L, null);

        assertThrows(AccessDeniedException.class, () -> service.initiate(
                FileResourceType.CONSULTA,
                41L,
                null,
                request("evidencia.txt", 12, "text/plain", null)));

        verifyNoInteractions(validationService, assetService, storageProvider);
    }

    @Test
    @DisplayName("QA-DOC-02: un usuario ajeno no obtiene listado ni metadata")
    void usuarioAjenoNoPuedeListar() {
        doThrow(new AccessDeniedException("recurso ajeno"))
                .when(authorizationService).authorizeRead(FileResourceType.CONSULTA, 41L, null);

        assertThrows(AccessDeniedException.class,
                () -> service.list(FileResourceType.CONSULTA, 41L, null));

        verifyNoInteractions(assetService, storageProvider);
    }

    @Test
    @DisplayName("QA-DOC-03: un usuario ajeno no obtiene URL firmada de descarga")
    void usuarioAjenoNoPuedeDescargar() {
        FileAsset asset = readyAsset(7L, 41L, "consulta/41/documento.txt", "documento.txt", 12L);
        when(assetService.findReady(7L)).thenReturn(asset);
        doThrow(new AccessDeniedException("recurso ajeno"))
                .when(authorizationService).authorizeRead(asset, null);

        assertThrows(AccessDeniedException.class, () -> service.prepareDownload(7L, null));

        verify(storageProvider, never()).createDownloadUrl(any(), any());
    }

    @Test
    @DisplayName("QA-DOC-04: un usuario ajeno no elimina objeto ni metadata")
    void usuarioAjenoNoPuedeEliminar() {
        FileAsset asset = readyAsset(7L, 41L, "consulta/41/documento.txt", "documento.txt", 12L);
        when(assetService.findReady(7L)).thenReturn(asset);
        when(authorizationService.parseType("CONSULTA")).thenReturn(FileResourceType.CONSULTA);
        doThrow(new AccessDeniedException("recurso ajeno"))
                .when(authorizationService).authorizeUpload(FileResourceType.CONSULTA, 41L, null);

        assertThrows(AccessDeniedException.class, () -> service.delete(7L, null));

        verify(assetService, never()).markDeletePending(anyLong());
        verify(assetService, never()).markDeleted(anyLong());
        verifyNoInteractions(storageProvider);
    }

    @ParameterizedTest(name = "QA-DOC-05 rechaza nombre: {0}")
    @ValueSource(strings = {"../archivo.txt", "..\\archivo.txt", "carpeta/archivo.txt", "carpeta\\archivo.txt"})
    void traversalEsRechazadoSinPersistir(String unsafeName) {
        FileAssetRepository repository = mock(FileAssetRepository.class);
        UsuarioActualService userService = mock(UsuarioActualService.class);
        FileAssetService realAssetService = new FileAssetService(repository, userService, "test-bucket");
        StorageProvider provider = mock(StorageProvider.class);
        FileResourceService realService = new FileResourceService(
                realAssetService,
                new FileValidationService(),
                mock(FileResourceAuthorizationService.class),
                provider,
                userService,
                UPLOAD_TTL,
                DOWNLOAD_TTL);

        assertThrows(BusinessException.class, () -> realService.initiate(
                FileResourceType.CONSULTA,
                41L,
                null,
                request(unsafeName, 12, "text/plain", null)));

        verifyNoInteractions(repository, provider);
    }

    @Test
    @DisplayName("QA-DOC-06: nombres duplicados conservan nombre y usan objectKey distinto")
    void nombreDuplicadoNoSobrescribeObjeto() {
        FileAssetRepository repository = mock(FileAssetRepository.class);
        UsuarioActualService userService = mock(UsuarioActualService.class);
        UsuarioSistema user = new UsuarioSistema();
        user.setId(9L);
        when(userService.obtenerUsuarioActual()).thenReturn(user);
        when(repository.save(any(FileAsset.class))).thenAnswer(invocation -> invocation.getArgument(0));
        FileAssetService realAssetService = new FileAssetService(repository, userService, "test-bucket");

        FileAsset first = realAssetService.startUpload(
                FileResourceType.CONSULTA, 41L, "evidencia.txt", "text/plain", 12, null);
        FileAsset second = realAssetService.startUpload(
                FileResourceType.CONSULTA, 41L, "evidencia.txt", "text/plain", 12, null);

        assertEquals("evidencia.txt", first.getOriginalFileName());
        assertEquals("evidencia.txt", second.getOriginalFileName());
        assertNotEquals(first.getObjectKey(), second.getObjectKey());
        assertTrue(first.getObjectKey().startsWith("consulta/41/"));
        assertTrue(second.getObjectKey().startsWith("consulta/41/"));
    }

    @Test
    @DisplayName("QA-DOC-07: rechaza archivo superior a 10 MiB")
    void archivoSuperiorAlLimiteEsRechazado() {
        FileResourceService realService = serviceWithRealValidation();

        assertThrows(BusinessException.class, () -> realService.initiate(
                FileResourceType.CONSULTA,
                41L,
                null,
                request("grande.bin", 10L * 1024 * 1024 + 1, "application/octet-stream", null)));

        verifyNoInteractions(assetService, storageProvider);
    }

    @Test
    @DisplayName("QA-DOC-08: complete falla y marca FAILED si el tamano almacenado difiere")
    void tamanoAlmacenadoDistintoFallaCarga() {
        UUID uploadId = UUID.randomUUID();
        FileAsset asset = uploadingAsset(uploadId, 9L, "consulta/41/evidencia.bin", 100L, validChecksum());
        when(assetService.findByUploadId(uploadId)).thenReturn(asset);
        when(currentUser.obtenerUsuarioActualId()).thenReturn(9L);
        when(authorizationService.parseType("CONSULTA")).thenReturn(FileResourceType.CONSULTA);
        when(storageProvider.head(asset.getObjectKey()))
                .thenReturn(new StorageProvider.StorageObjectMetadata(99L, "application/octet-stream"));

        assertThrows(IllegalArgumentException.class, () -> service.complete(uploadId, null));

        verify(assetService).markUploadFailed(uploadId);
        verify(assetService, never()).markReady(any(), anyLong(), any());
    }

    @Test
    @DisplayName("QA-DOC-09: rechaza checksum con formato invalido")
    void checksumConFormatoInvalidoEsRechazado() {
        FileResourceService realService = serviceWithRealValidation();

        assertThrows(BusinessException.class, () -> realService.initiate(
                FileResourceType.CONSULTA,
                41L,
                null,
                request("evidencia.bin", 12, "application/octet-stream", "no-es-sha256")));

        verifyNoInteractions(assetService, storageProvider);
    }

    // QA-DOC-10: reproduccion explicita en DocumentChecksumIntegrityIT.

    @Test
    @DisplayName("QA-DOC-11: conciliacion rechaza PDF sin firma y marca carga fallida")
    void pdfSinFirmaEsRechazadoDuranteComplete() {
        UUID uploadId = UUID.randomUUID();
        byte[] storedBytes = "contenido-falso".getBytes(StandardCharsets.UTF_8);
        FileAsset asset = uploadingAsset(
                uploadId, 9L, "conciliacion/41/acta.pdf", storedBytes.length, null);
        asset.setResourceType("CONCILIACION");
        asset.setOriginalFileName("acta.pdf");
        asset.setContentType("application/pdf");
        when(assetService.findByUploadId(uploadId)).thenReturn(asset);
        when(currentUser.obtenerUsuarioActualId()).thenReturn(9L);
        when(authorizationService.parseType("CONCILIACION")).thenReturn(FileResourceType.CONCILIACION);
        when(storageProvider.head(asset.getObjectKey()))
                .thenReturn(new StorageProvider.StorageObjectMetadata(storedBytes.length, "application/pdf"));
        when(storageProvider.load(asset.getObjectKey())).thenReturn(new ByteArrayResource(storedBytes));
        FileResourceService realService = new FileResourceService(
                assetService,
                new FileValidationService(),
                authorizationService,
                storageProvider,
                currentUser,
                UPLOAD_TTL,
                DOWNLOAD_TTL);

        assertThrows(BusinessException.class, () -> realService.complete(uploadId, null));

        verify(storageProvider).delete(asset.getObjectKey());
        verify(assetService).markUploadFailed(uploadId);
        verify(assetService, never()).markReady(any(), anyLong(), any());
    }

    @Test
    @DisplayName("QA-DOC-12: usa exactamente el TTL configurado para descarga")
    void descargaEntregaTtlConfiguradoAlProveedor() {
        FileAsset asset = readyAsset(7L, 41L, "consulta/41/documento.txt", "documento.txt", 12L);
        when(assetService.findReady(7L)).thenReturn(asset);
        when(storageProvider.createDownloadUrl(eq(asset.getObjectKey()), any(Duration.class)))
                .thenReturn(new StorageProvider.PresignedDownload(
                        "https://storage.invalid/synthetic",
                        Instant.parse("2026-10-03T17:05:00Z")));
        ArgumentCaptor<Duration> validity = ArgumentCaptor.forClass(Duration.class);

        service.prepareDownload(7L, null);

        verify(storageProvider).createDownloadUrl(eq(asset.getObjectKey()), validity.capture());
        assertEquals(DOWNLOAD_TTL, validity.getValue());
    }

    private FileResourceService serviceWithRealValidation() {
        return new FileResourceService(
                assetService,
                new FileValidationService(),
                authorizationService,
                storageProvider,
                currentUser,
                UPLOAD_TTL,
                DOWNLOAD_TTL);
    }

    private static FileUploadRequest request(String name, long size, String contentType, String checksum) {
        return new FileUploadRequest(name, size, contentType, checksum);
    }

    private static FileAsset uploadingAsset(
            UUID uploadId, Long userId, String objectKey, long size, String checksum) {
        UsuarioSistema user = new UsuarioSistema();
        user.setId(userId);
        FileAsset asset = new FileAsset();
        asset.setUploadId(uploadId);
        asset.setUploadedBy(user);
        asset.setResourceType("CONSULTA");
        asset.setResourceId(41L);
        asset.setObjectKey(objectKey);
        asset.setOriginalFileName("evidencia.bin");
        asset.setContentType("application/octet-stream");
        asset.setSize(size);
        asset.setChecksum(checksum == null ? "" : checksum);
        asset.setStatus(FileAssetStatus.UPLOADING);
        return asset;
    }

    private static FileAsset readyAsset(Long id, Long resourceId, String objectKey, String name, long size) {
        FileAsset asset = new FileAsset();
        asset.setId(id);
        asset.setResourceType("CONSULTA");
        asset.setResourceId(resourceId);
        asset.setObjectKey(objectKey);
        asset.setOriginalFileName(name);
        asset.setContentType("text/plain");
        asset.setSize(size);
        asset.setChecksum("");
        asset.setStatus(FileAssetStatus.READY);
        return asset;
    }

    private static String validChecksum() {
        return "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    }
}
