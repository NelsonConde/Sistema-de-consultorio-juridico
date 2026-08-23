package co.edu.ufps.legal_cases.file_storage.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    @Mock
    private StorageProvider storageProvider;

    @Mock
    private FileAssetService fileAssetService;

    @Mock
    private FileValidationService fileValidationService;

    @Test
    void creaClaveVersionadaAlReemplazarArchivoActivo() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "README.txt", "text/plain", "contenido".getBytes());
        when(fileAssetService.isActive("1/README.txt")).thenReturn(true);
        when(fileAssetService.isActive(org.mockito.ArgumentMatchers.argThat(
                key -> key.startsWith("1/") && !"1/README.txt".equals(key)))).thenReturn(false);
        when(storageProvider.store(any(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(1));

        FileStorageService service = new FileStorageService(
                storageProvider, fileAssetService, fileValidationService);

        String storedKey = service.storeFile(file, "1");

        assertTrue(storedKey.startsWith("1/"));
        assertTrue(storedKey.endsWith("-README.txt"));
        verify(fileAssetService).begin(storedKey, file);
        verify(fileAssetService).activate(storedKey, file);
    }
}
