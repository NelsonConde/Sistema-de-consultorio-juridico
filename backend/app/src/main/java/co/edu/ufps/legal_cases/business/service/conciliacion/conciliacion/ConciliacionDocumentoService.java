package co.edu.ufps.legal_cases.business.service.conciliacion.conciliacion;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.file_storage.service.FileStorageService;

// Centraliza validaciones y rutas de documentos de conciliación.
// No decide permisos ni estados; eso queda en AccessService y Validator.
@Service
public class ConciliacionDocumentoService {

    private static final String BASE_PATH = "conciliacion";
    private static final String SOLICITUD_FILE_NAME = "solicitud.pdf";
    private static final String ACTA_FILE_NAME = "acta.pdf";
    private static final String PDF_EXTENSION = ".pdf";
    private static final String PDF_CONTENT_TYPE = "application/pdf";

    private final FileStorageService fileStorageService;

    public ConciliacionDocumentoService(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    public String guardarSolicitud(Long conciliacionId, MultipartFile archivo) {
        validarConciliacionId(conciliacionId);
        validarPdfObligatorio(archivo, "La solicitud de conciliación es obligatoria");

        return fileStorageService.storeFileAs(
                archivo,
                construirDirectorio(conciliacionId),
                SOLICITUD_FILE_NAME);
    }

    public String guardarActa(Long conciliacionId, MultipartFile archivo) {
        validarConciliacionId(conciliacionId);
        validarPdfObligatorio(archivo, "El acta de conciliación es obligatoria");

        return fileStorageService.storeFileAs(
                archivo,
                construirDirectorio(conciliacionId),
                ACTA_FILE_NAME);
    }

    public String construirRutaSolicitud(Long conciliacionId) {
        validarConciliacionId(conciliacionId);
        return construirDirectorio(conciliacionId) + "/" + SOLICITUD_FILE_NAME;
    }

    public String construirRutaActa(Long conciliacionId) {
        validarConciliacionId(conciliacionId);
        return construirDirectorio(conciliacionId) + "/" + ACTA_FILE_NAME;
    }

    private void validarPdfObligatorio(MultipartFile archivo, String mensajeObligatorio) {
        if (archivo == null || archivo.isEmpty()) {
            throw new BusinessException(mensajeObligatorio);
        }

        String nombreOriginal = archivo.getOriginalFilename();

        if (nombreOriginal == null || !nombreOriginal.toLowerCase().endsWith(PDF_EXTENSION)) {
            throw new BusinessException("El archivo debe ser un PDF");
        }

        String contentType = archivo.getContentType();

        if (contentType != null && !PDF_CONTENT_TYPE.equalsIgnoreCase(contentType)) {
            throw new BusinessException("El archivo debe tener tipo de contenido PDF");
        }
    }

    private void validarConciliacionId(Long conciliacionId) {
        if (conciliacionId == null) {
            throw new BusinessException("La conciliación es obligatoria para guardar documentos");
        }
    }

    private String construirDirectorio(Long conciliacionId) {
        return BASE_PATH + "/" + conciliacionId;
    }
}