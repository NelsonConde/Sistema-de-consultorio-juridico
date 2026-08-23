package co.edu.ufps.legal_cases.file_storage.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import co.edu.ufps.legal_cases.business.service.acceso.conciliacion.ConciliacionAccessService;
import co.edu.ufps.legal_cases.business.service.acceso.consulta.ConsultaAccessService;
import co.edu.ufps.legal_cases.business.service.acceso.seguimiento.SeguimientoAccessService;
import co.edu.ufps.legal_cases.business.service.acceso.seguimiento.SeguimientoRespuestaAccessService;

/**
 * Resuelve el recurso dueño de una clave documental y delega la autorización
 * en el servicio de acceso funcional correspondiente.
 */
@Service
public class FileAccessService {

    private static final Pattern CONSULTA = Pattern.compile("^(\\d+)(?:/.*)?$");
    private static final Pattern TAREA = Pattern.compile("^tareas-(\\d+)-documentos(?:/.*)?$");
    private static final Pattern RESPUESTA = Pattern.compile("^tareas-(\\d+)-respuestas-(\\d+)(?:/.*)?$");
    private static final Pattern CONCILIACION = Pattern.compile("^conciliacion/(\\d+)(?:/.*)?$");

    private final ConsultaAccessService consultaAccessService;
    private final SeguimientoAccessService seguimientoAccessService;
    private final SeguimientoRespuestaAccessService respuestaAccessService;
    private final ConciliacionAccessService conciliacionAccessService;

    public FileAccessService(
            ConsultaAccessService consultaAccessService,
            SeguimientoAccessService seguimientoAccessService,
            SeguimientoRespuestaAccessService respuestaAccessService,
            ConciliacionAccessService conciliacionAccessService) {
        this.consultaAccessService = consultaAccessService;
        this.seguimientoAccessService = seguimientoAccessService;
        this.respuestaAccessService = respuestaAccessService;
        this.conciliacionAccessService = conciliacionAccessService;
    }

    public void authorizeUpload(String key) {
        ResourceReference resource = resolve(key);
        switch (resource.type()) {
            case CONSULTA -> consultaAccessService.validarPuedeEditarConsulta(resource.firstId());
            case TAREA -> seguimientoAccessService.validarPuedeEditarSeguimiento(resource.firstId());
            case RESPUESTA -> respuestaAccessService.validarPuedeSubirArchivoRespuesta(
                    resource.firstId(), resource.secondId());
            case CONCILIACION -> conciliacionAccessService.validarPuedeReemplazarSolicitud(resource.firstId());
        }
    }

    public void authorizeRead(String key) {
        ResourceReference resource = resolve(key);
        switch (resource.type()) {
            case CONSULTA -> consultaAccessService.validarPuedeVerConsulta(resource.firstId());
            case TAREA -> seguimientoAccessService.validarPuedeVerSeguimiento(resource.firstId());
            case RESPUESTA -> respuestaAccessService.validarPuedeLeerArchivoRespuesta(
                    resource.firstId(), resource.secondId());
            case CONCILIACION -> conciliacionAccessService.validarPuedeVerConciliacion(resource.firstId());
        }
    }

    public void authorizeList(String prefix) {
        authorizeRead(prefix);
    }

    public void denyUnscopedOperation() {
        throw new AccessDeniedException("La operación de archivos requiere un recurso asociado");
    }

    private ResourceReference resolve(String rawKey) {
        String key = normalize(rawKey);
        Matcher matcher = RESPUESTA.matcher(key);
        if (matcher.matches()) {
            return new ResourceReference(ResourceType.RESPUESTA,
                    Long.valueOf(matcher.group(1)), Long.valueOf(matcher.group(2)));
        }

        matcher = TAREA.matcher(key);
        if (matcher.matches()) {
            return new ResourceReference(ResourceType.TAREA, Long.valueOf(matcher.group(1)), null);
        }

        matcher = CONCILIACION.matcher(key);
        if (matcher.matches()) {
            return new ResourceReference(ResourceType.CONCILIACION, Long.valueOf(matcher.group(1)), null);
        }

        matcher = CONSULTA.matcher(key);
        if (matcher.matches()) {
            return new ResourceReference(ResourceType.CONSULTA, Long.valueOf(matcher.group(1)), null);
        }

        throw new AccessDeniedException("La ruta de archivo no identifica un recurso autorizado");
    }

    private static String normalize(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            throw new AccessDeniedException("La ruta de archivo es obligatoria");
        }
        String key = rawKey.replace('\\', '/');
        if (key.startsWith("/") || key.contains("..")
                || key.contains("%") || key.indexOf('\0') >= 0
                || key.chars().anyMatch(Character::isISOControl)) {
            throw new AccessDeniedException("La ruta de archivo es inválida");
        }
        return key;
    }

    private enum ResourceType {
        CONSULTA,
        TAREA,
        RESPUESTA,
        CONCILIACION
    }

    private record ResourceReference(ResourceType type, Long firstId, Long secondId) {
    }
}
