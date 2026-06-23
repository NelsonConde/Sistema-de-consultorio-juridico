package co.edu.ufps.legal_cases.file_storage.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import co.edu.ufps.legal_cases.file_storage.exception.FileNotFoundException;
import co.edu.ufps.legal_cases.file_storage.exception.FileStorageException;
import jakarta.annotation.PostConstruct;

/**
 * Basado en https://www.codejava.net/frameworks/spring-boot/file-download-upload-rest-api-examples
 */

@Service
public class FileStorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    private Path fileStoragePath;

    @PostConstruct
    public void init() {
        try {
            fileStoragePath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(fileStoragePath);
            System.out.println("Directorio de carga inicializado en: " + fileStoragePath);
        } catch (IOException e) {
            throw new FileStorageException("No se pudo crear el directorio de carga.", e);
        }
    }

    public Path getFileStoragePath() {
        return fileStoragePath;
    }

    public String storeFile(MultipartFile file, String subDir) {
        String fileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));

        try {
            if (fileName.contains("..")) {
                throw new FileStorageException("Lo sentimos, el nombre de archivo contiene una secuencia de ruta inválida " + fileName);
            }

            Path targetLocation = this.fileStoragePath;
            if (subDir != null && !subDir.isEmpty()) {
                if (subDir.contains("..")) {
                    throw new FileStorageException("Lo sentimos, el directorio contiene una secuencia de ruta inválida " + subDir);
                }
                targetLocation = targetLocation.resolve(subDir).normalize();

                if (!Files.exists(targetLocation)) {
                    Files.createDirectories(targetLocation);
                }
            }

            targetLocation = targetLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            if (subDir != null && !subDir.isEmpty()) {
                return subDir + "/" + fileName;
            }
            return fileName;
        } catch (IOException ex) {
            throw new FileStorageException("No se pudo guardar el archivo " + fileName + ". Por favor intente de nuevo.", ex);
        }
    }

    public String storeFileAs(MultipartFile file, String subDir, String targetFileName) {
        String fileName = StringUtils.cleanPath(Objects.requireNonNull(targetFileName));

        try {
            if (fileName.contains("..")) {
                throw new FileStorageException("Lo sentimos, el nombre de archivo contiene una secuencia de ruta inválida " + fileName);
            }

            Path targetLocation = this.fileStoragePath;
            if (subDir != null && !subDir.isEmpty()) {
                if (subDir.contains("..")) {
                    throw new FileStorageException("Lo sentimos, el directorio contiene una secuencia de ruta inválida " + subDir);
                }

                targetLocation = targetLocation.resolve(subDir).normalize();

                if (!Files.exists(targetLocation)) {
                    Files.createDirectories(targetLocation);
                }
            }

            targetLocation = targetLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            if (subDir != null && !subDir.isEmpty()) {
                return subDir + "/" + fileName;
            }

            return fileName;
        } catch (IOException ex) {
            throw new FileStorageException("No se pudo guardar el archivo " + fileName + ". Por favor intente de nuevo.", ex);
        }
    }

    public Resource loadFileAsResource(String fileName) {
        if (fileName.contains("..")) {
            throw new FileStorageException("Lo sentimos, el nombre de archivo contiene una secuencia de ruta inválida " + fileName);
        }

        try {
            Path filePath = this.fileStoragePath.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                return resource;
            } else {
                throw new FileNotFoundException("Archivo no encontrado " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new FileNotFoundException("Archivo no encontrado " + fileName, ex);
        }
    }

    public List<String> listFiles(String subDir) {
        try {
            Path targetPath = this.fileStoragePath;
            if (subDir != null && !subDir.isEmpty()) {
                if (subDir.contains("..")) {
                    throw new FileStorageException("Lo sentimos, el directorio contiene una secuencia de ruta inválida " + subDir);
                }
                targetPath = targetPath.resolve(subDir).normalize();
            }

            if (!Files.exists(targetPath) || !Files.isDirectory(targetPath)) {
                throw new FileNotFoundException("Directorio no encontrado " + subDir);
            }

            List<String> fileNames = new ArrayList<>();
            try (java.util.stream.Stream<Path> stream = Files.list(targetPath)) {
                stream.forEach(p -> fileNames.add(p.getFileName().toString()));
            }

            return fileNames;
        } catch (IOException ex) {
            throw new FileStorageException("No se pudo listar los archivos", ex);
        }
    }

    public List<String> listDirectories() {
        try {
            List<String> directories = new ArrayList<>();

            if (Files.exists(this.fileStoragePath)) {
                try (java.util.stream.Stream<Path> stream = Files.walk(this.fileStoragePath)) {
                    stream.filter(Files::isDirectory)
                          .filter(p -> !p.equals(this.fileStoragePath))
                          .forEach(p -> {
                              String relativePath = this.fileStoragePath.relativize(p).toString().replace("\\", "/");
                              directories.add(relativePath);
                          });
                }
            }

            return directories;
        } catch (IOException ex) {
            throw new FileStorageException("No se pudo listar los directorios", ex);
        }
    }
}