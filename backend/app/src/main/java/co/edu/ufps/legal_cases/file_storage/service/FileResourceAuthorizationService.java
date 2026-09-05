package co.edu.ufps.legal_cases.file_storage.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import co.edu.ufps.legal_cases.business.service.acceso.conciliacion.ConciliacionAccessService;
import co.edu.ufps.legal_cases.business.service.acceso.consulta.ConsultaAccessService;
import co.edu.ufps.legal_cases.business.service.acceso.proceso.ProcesoAccessService;
import co.edu.ufps.legal_cases.business.service.acceso.seguimiento.SeguimientoAccessService;
import co.edu.ufps.legal_cases.business.service.acceso.seguimiento.SeguimientoRespuestaAccessService;
import co.edu.ufps.legal_cases.file_storage.model.FileAsset;
import co.edu.ufps.legal_cases.file_storage.model.FileResourceType;

/** Autorización por recurso funcional, independiente de la clave del bucket. */
@Service
public class FileResourceAuthorizationService {

    private final ConsultaAccessService consultaAccessService;
    private final SeguimientoAccessService seguimientoAccessService;
    private final SeguimientoRespuestaAccessService respuestaAccessService;
    private final ConciliacionAccessService conciliacionAccessService;
    private final ProcesoAccessService procesoAccessService;

    public FileResourceAuthorizationService(
            ConsultaAccessService consultaAccessService,
            SeguimientoAccessService seguimientoAccessService,
            SeguimientoRespuestaAccessService respuestaAccessService,
            ConciliacionAccessService conciliacionAccessService,
            ProcesoAccessService procesoAccessService) {
        this.consultaAccessService = consultaAccessService;
        this.seguimientoAccessService = seguimientoAccessService;
        this.respuestaAccessService = respuestaAccessService;
        this.conciliacionAccessService = conciliacionAccessService;
        this.procesoAccessService = procesoAccessService;
    }

    public void authorizeUpload(FileResourceType type, Long resourceId, Long parentId) {
        validateIds(type, resourceId);
        switch (type) {
            case CONSULTA -> consultaAccessService.validarPuedeEditarConsulta(resourceId);
            case SEGUIMIENTO -> seguimientoAccessService.validarPuedeEditarSeguimiento(resourceId);
            case RESPUESTA -> {
                requireParent(parentId);
                respuestaAccessService.validarPuedeSubirArchivoRespuesta(parentId, resourceId);
            }
            case CONCILIACION -> conciliacionAccessService.validarPuedeReemplazarSolicitud(resourceId);
            case PROCESO -> procesoAccessService.validarPuedeActualizarProceso(resourceId);
        }
    }

    public void authorizeRead(FileAsset asset, Long parentId) {
        FileResourceType type = parseType(asset.getResourceType());
        Long resourceId = asset.getResourceId();
        validateIds(type, resourceId);
        switch (type) {
            case CONSULTA -> consultaAccessService.validarPuedeVerConsulta(resourceId);
            case SEGUIMIENTO -> seguimientoAccessService.validarPuedeVerSeguimiento(resourceId);
            case RESPUESTA -> {
                requireParent(parentId);
                respuestaAccessService.validarPuedeLeerArchivoRespuesta(parentId, resourceId);
            }
            case CONCILIACION -> conciliacionAccessService.validarPuedeVerConciliacion(resourceId);
            case PROCESO -> procesoAccessService.validarPuedeVerProceso(resourceId);
        }
    }

    public void authorizeRead(FileResourceType type, Long resourceId, Long parentId) {
        validateIds(type, resourceId);
        switch (type) {
            case CONSULTA -> consultaAccessService.validarPuedeVerConsulta(resourceId);
            case SEGUIMIENTO -> seguimientoAccessService.validarPuedeVerSeguimiento(resourceId);
            case RESPUESTA -> {
                requireParent(parentId);
                respuestaAccessService.validarPuedeLeerArchivoRespuesta(parentId, resourceId);
            }
            case CONCILIACION -> conciliacionAccessService.validarPuedeVerConciliacion(resourceId);
            case PROCESO -> procesoAccessService.validarPuedeVerProceso(resourceId);
        }
    }

    public FileResourceType parseType(String value) {
        if ("SEGUIMIENTO_RESPUESTA".equals(value)) {
            return FileResourceType.RESPUESTA;
        }
        try {
            return FileResourceType.valueOf(value);
        } catch (RuntimeException ex) {
            throw new AccessDeniedException("El recurso documental no es válido");
        }
    }

    private static void validateIds(FileResourceType type, Long resourceId) {
        if (type == null || resourceId == null || resourceId <= 0) {
            throw new AccessDeniedException("El recurso documental no es válido");
        }
    }

    private static void requireParent(Long parentId) {
        if (parentId == null || parentId <= 0) {
            throw new AccessDeniedException("La relación del recurso documental no es válida");
        }
    }
}
