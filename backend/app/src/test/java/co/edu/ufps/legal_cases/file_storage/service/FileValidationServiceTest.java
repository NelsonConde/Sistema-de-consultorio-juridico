package co.edu.ufps.legal_cases.file_storage.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import co.edu.ufps.legal_cases.file_storage.exception.FileStorageException;

class FileValidationServiceTest {

    private final FileValidationService service = new FileValidationService();

    @Test
    void aceptaPdfConFirmaValida() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "solicitud.pdf",
                "application/pdf",
                "%PDF-1.7\ncontenido".getBytes(StandardCharsets.UTF_8));

        assertDoesNotThrow(() -> service.validate(file));
    }

    @Test
    void rechazaPdfConExtensionPeroContenidoInvalido() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "solicitud.pdf", "application/pdf", "texto".getBytes(StandardCharsets.UTF_8));

        assertThrows(FileStorageException.class, () -> service.validate(file));
    }

    @Test
    void rechazaExtensionNoPermitida() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "archivo.exe", "application/octet-stream", new byte[] {1, 2, 3});

        assertThrows(FileStorageException.class, () -> service.validate(file));
    }
}
