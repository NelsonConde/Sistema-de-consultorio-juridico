package co.edu.ufps.legal_cases.file_storage.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.file_storage.exception.FileStorageException;
import co.edu.ufps.legal_cases.file_storage.model.FileAsset;
import co.edu.ufps.legal_cases.file_storage.model.FileAssetStatus;
import co.edu.ufps.legal_cases.file_storage.model.FileResourceType;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.service.context.UsuarioActualService;

class FileResourceChecksumTest {
    private final byte[] bytes = "%PDF-1.7\ncontenido sintetico".getBytes(StandardCharsets.UTF_8);
    private FileAssetService assets;
    private StorageProvider storage;
    private FileResourceAuthorizationService authorization;
    private FileResourceService service;
    private FileAsset asset;

    @BeforeEach
    void setUp() throws Exception {
        assets = mock(FileAssetService.class);
        storage = mock(StorageProvider.class);
        authorization = mock(FileResourceAuthorizationService.class);
        UsuarioActualService userService = mock(UsuarioActualService.class);
        service = new FileResourceService(assets, new FileValidationService(), authorization,
                storage, userService, Duration.ofMinutes(10), Duration.ofMinutes(5));
        UsuarioSistema user = new UsuarioSistema();
        user.setId(9L);
        asset = new FileAsset();
        asset.setUploadId(UUID.randomUUID());
        asset.setUploadedBy(user);
        asset.setResourceId(41L);
        asset.setObjectKey("consulta/41/evidencia.pdf");
        asset.setOriginalFileName("evidencia.pdf");
        asset.setContentType("application/pdf");
        asset.setSize((long) bytes.length);
        asset.setChecksum(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
        asset.setStatus(FileAssetStatus.UPLOADING);
        when(assets.findByUploadId(asset.getUploadId())).thenReturn(asset);
        when(userService.obtenerUsuarioActualId()).thenReturn(9L);
        when(storage.head(asset.getObjectKey())).thenReturn(
                new StorageProvider.StorageObjectMetadata(bytes.length, asset.getContentType()));
        when(storage.load(asset.getObjectKey())).thenReturn(new ByteArrayResource(bytes));
        setType(FileResourceType.CONSULTA);
    }

    @ParameterizedTest
    @EnumSource(value = FileResourceType.class, names = {"CONSULTA", "CONCILIACION"})
    void checksumCorrectoEnMayusculasPermiteReadyConUnaLectura(FileResourceType type) {
        setType(type);
        asset.setChecksum(asset.getChecksum().toUpperCase(Locale.ROOT));
        when(assets.markReady(asset.getUploadId(), bytes.length, asset.getContentType())).thenAnswer(invocation -> {
            asset.setStatus(FileAssetStatus.READY);
            return asset;
        });

        assertEquals("READY", service.complete(asset.getUploadId(), null).status());

        verify(storage).load(asset.getObjectKey());
        verify(assets).markReady(asset.getUploadId(), bytes.length, asset.getContentType());
        verify(assets, never()).markUploadFailed(any());
    }

    @ParameterizedTest
    @EnumSource(value = FileResourceType.class, names = {"CONSULTA", "CONCILIACION"})
    void checksumIncorrectoMarcaFallidaSinReadyNiBorrarObjeto(FileResourceType type) {
        setType(type);
        asset.setChecksum("a".repeat(64));

        BusinessException failure = assertThrows(BusinessException.class,
                () -> service.complete(asset.getUploadId(), null));

        assertEquals("La huella del archivo no coincide con el contenido almacenado", failure.getMessage());
        verify(assets).markUploadFailed(asset.getUploadId());
        verify(assets, never()).markReady(any(), anyLong(), any());
        verify(storage, never()).delete(any());
    }

    @ParameterizedTest
    @EnumSource(value = FileResourceType.class, names = {"CONSULTA", "CONCILIACION"})
    void errorAlAbrirStreamMarcaFallidaYPreservaCausa(FileResourceType type) throws Exception {
        setType(type);
        IOException cause = new IOException("fallo sintetico");
        Resource resource = mock(Resource.class);
        when(resource.getInputStream()).thenThrow(cause);
        when(storage.load(asset.getObjectKey())).thenReturn(resource);

        FileStorageException failure = assertThrows(FileStorageException.class,
                () -> service.complete(asset.getUploadId(), null));

        assertSame(cause, failure.getCause());
        verify(assets).markUploadFailed(asset.getUploadId());
        verify(assets, never()).markReady(any(), anyLong(), any());
        if (type == FileResourceType.CONSULTA) {
            verify(storage, never()).delete(any());
        }
    }

    @ParameterizedTest
    @EnumSource(value = FileResourceType.class, names = {"CONSULTA", "CONCILIACION"})
    void errorDuranteLecturaCierraStreamYPreservaCausa(FileResourceType type) throws Exception {
        setType(type);
        IOException cause = new IOException("fallo sintetico durante lectura");
        Resource resource = mock(Resource.class);
        InputStream input = mock(InputStream.class);
        when(input.read(any(byte[].class))).thenThrow(cause);
        when(input.read(any(byte[].class), anyInt(), anyInt())).thenThrow(cause);
        when(resource.getInputStream()).thenReturn(input);
        when(storage.load(asset.getObjectKey())).thenReturn(resource);

        FileStorageException failure = assertThrows(FileStorageException.class,
                () -> service.complete(asset.getUploadId(), null));

        assertSame(cause, failure.getCause());
        if (type == FileResourceType.CONSULTA) {
            assertEquals("No se pudo verificar la integridad del archivo", failure.getMessage());
            verify(storage, never()).delete(any());
        }
        verify(input).close();
        verify(assets).markUploadFailed(asset.getUploadId());
        verify(assets, never()).markReady(any(), anyLong(), any());
    }

    private void setType(FileResourceType type) {
        asset.setResourceType(type.name());
        when(authorization.parseType(type.name())).thenReturn(type);
    }
}
