package co.edu.ufps.legal_cases.file_storage.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import co.edu.ufps.legal_cases.common.exception.BusinessException;

class FileValidationServiceTest {

    private final FileValidationService service = new FileValidationService();

    @Test
    void aceptaCualquierExtensionCuandoElArchivoEsValido() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "evidencia.bin",
                "application/octet-stream",
                new byte[] {1, 2, 3});

        assertDoesNotThrow(() -> service.validate(file));
    }

    @Test
    void aceptaUnArchivoSinExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "evidencia", "application/octet-stream", new byte[] {1, 2, 3});

        assertDoesNotThrow(() -> service.validate(file));
    }

    @Test
    void aceptaMetadatosDeUnFormatoArbitrarioParaCargaFirmada() {
        assertDoesNotThrow(() -> service.validateMetadata(
                "evidencia-forense.zip", 1_024, "application/zip"));
    }

    @Test
    void rechazaArchivoVacio() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "archivo.txt", "text/plain", new byte[0]);

        assertThrows(BusinessException.class, () -> service.validate(file));
    }
}
