package co.edu.ufps.legal_cases.file_storage.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.HandlerMapping;

import co.edu.ufps.legal_cases.file_storage.exception.FileNotFoundException;
import co.edu.ufps.legal_cases.file_storage.service.FileStorageService;

/**
 * Basado en https://www.codejava.net/frameworks/spring-boot/file-download-upload-rest-api-examples
 */

@RestController
@RequestMapping("/api/files")
@PreAuthorize("isAuthenticated()")
public class FileUploadController {

    private final FileStorageService fileStorageService;

    public FileUploadController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam MultipartFile file,
            @RequestParam(value = "path", required = false) String path) {
        Map<String, Object> response = new HashMap<>();

        try {
            String fileName = fileStorageService.storeFile(file, path);

            response.put("fileName", fileName);
            response.put("fileSize", file.getSize());
            response.put("message", "¡Archivo cargado exitosamente!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", "No se pudo cargar el archivo: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/upload-multiple")
    public ResponseEntity<List<Map<String, Object>>> uploadMultipleFiles(
            @RequestParam MultipartFile[] files,
            @RequestParam(required = false) String path) {
        List<Map<String, Object>> results = new ArrayList<>();

        for (MultipartFile file : files) {
            Map<String, Object> fileResponse = new HashMap<>();
            try {
                String fileName = fileStorageService.storeFile(file, path);

                fileResponse.put("fileName", fileName);
                fileResponse.put("fileSize", file.getSize());
                fileResponse.put("message", "Cargado exitosamente");
            } catch (Exception e) {
                fileResponse.put("error", "Error al cargar: " + e.getMessage());
            }
            results.add(fileResponse);
        }

        return ResponseEntity.ok(results);
    }

    @GetMapping("/download/**")
    public ResponseEntity<Resource> downloadFile(HttpServletRequest request) {
        try {
            String pathWithinHandler = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
            String requestedFile = java.net.URLDecoder.decode(
                    pathWithinHandler.replaceFirst("^.*/download/", ""),
                    java.nio.charset.StandardCharsets.UTF_8.name()
            );

            Resource resource = fileStorageService.loadFileAsResource(requestedFile);

            String contentType = null;
            try {
                contentType = Files.probeContentType(resource.getFile().toPath());
            } catch (IOException ex) {
                // No se puede determinar el tipo, por ahora se puede ignorar
            }

            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (FileNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping({"/list", "/list/{subDir:.+}"})
    public ResponseEntity<List<String>> listFiles(@PathVariable(required = false) String subDir) {
        try {
            List<String> fileNames = fileStorageService.listFiles(subDir);
            return ResponseEntity.ok(fileNames);
        } catch (FileNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/directories")
    public ResponseEntity<List<String>> listDirectories() {
        try {
            List<String> directories = fileStorageService.listDirectories();
            return ResponseEntity.ok(directories);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
