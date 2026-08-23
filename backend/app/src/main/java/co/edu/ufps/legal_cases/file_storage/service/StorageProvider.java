package co.edu.ufps.legal_cases.file_storage.service;

import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * Abstracción del almacenamiento documental. La aplicación trabaja con claves
 * lógicas y no conoce el proveedor físico de objetos.
 */
public interface StorageProvider {

    String store(MultipartFile file, String objectKey);

    Resource load(String objectKey);

    List<String> list(String prefix);

    List<String> listDirectories(String prefix);
}
