package co.edu.ufps.legal_cases.common.exception.handler;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import co.edu.ufps.legal_cases.audit.service.log.AuditSecurityService;
import co.edu.ufps.legal_cases.common.exception.AdministracionInvariantException;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.common.exception.ConcurrenciaOptimistaException;
import co.edu.ufps.legal_cases.common.exception.dto.ErrorResponseDTO;
import co.edu.ufps.legal_cases.common.observability.CorrelationIdContext;
import co.edu.ufps.legal_cases.file_storage.exception.FileStorageException;
import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private AuditSecurityService auditSecurityService;

    @Autowired(required = false)
    void setAuditSecurityService(
            AuditSecurityService auditSecurityService) {

        this.auditSecurityService = auditSecurityService;
    }

    @ExceptionHandler(AdministracionInvariantException.class)
    public ResponseEntity<ErrorResponseDTO>
    manejarAdministracionInvariantException(
            AdministracionInvariantException ex,
            HttpServletRequest request) {

        ErrorResponseDTO error = construirError(
                HttpStatus.CONFLICT,
                "Conflicto de administración",
                mensajeSeguro(
                        ex.getMessage(),
                        "La operación compromete la continuidad administrativa"),
                request);

        return construirRespuesta(HttpStatus.CONFLICT, error);
    }

    @ExceptionHandler({
            ConcurrenciaOptimistaException.class,
            OptimisticLockingFailureException.class,
            OptimisticLockException.class
    })
    public ResponseEntity<ErrorResponseDTO> manejarConflictoConcurrencia(
            Exception ex,
            HttpServletRequest request) {

        String correlationId = obtenerCorrelationId(request);

        log.warn(
                "Conflicto de concurrencia [{}] en {}: {}",
                correlationId,
                request.getRequestURI(),
                ex.getClass().getSimpleName());

        ErrorResponseDTO error = construirError(
                HttpStatus.CONFLICT,
                "Conflicto de concurrencia",
                "El recurso fue modificado por otro usuario. "
                        + "Recargue la información y revise sus cambios.",
                request);

        return construirRespuesta(HttpStatus.CONFLICT, error);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponseDTO> manejarBusinessException(
            BusinessException ex,
            HttpServletRequest request) {

        ErrorResponseDTO error = construirError(
                HttpStatus.BAD_REQUEST,
                "Error de negocio",
                mensajeSeguro(
                        ex.getMessage(),
                        "La solicitud no cumple una regla de negocio"),
                request);

        return construirRespuesta(HttpStatus.BAD_REQUEST, error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> manejarErroresValidacion(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, String> detalles = new LinkedHashMap<>();

        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error -> detalles.put(
                        error.getField(),
                        error.getDefaultMessage()));

        ErrorResponseDTO respuesta = construirErrorConDetalles(
                HttpStatus.BAD_REQUEST,
                "Error de validación",
                "Uno o más campos no son válidos",
                request,
                detalles);

        return construirRespuesta(
                HttpStatus.BAD_REQUEST,
                respuesta);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseDTO>
    manejarViolacionesDeRestriccion(
            ConstraintViolationException ex,
            HttpServletRequest request) {

        Map<String, String> detalles = new LinkedHashMap<>();

        ex.getConstraintViolations()
                .forEach(violation -> detalles.put(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()));

        ErrorResponseDTO respuesta = construirErrorConDetalles(
                HttpStatus.BAD_REQUEST,
                "Error de validación",
                "Uno o más parámetros no son válidos",
                request,
                detalles);

        return construirRespuesta(
                HttpStatus.BAD_REQUEST,
                respuesta);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDTO> manejarParametroInvalido(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        ErrorResponseDTO error = construirError(
                HttpStatus.BAD_REQUEST,
                "Solicitud inválida",
                construirMensajeParametroInvalido(ex),
                request);

        return construirRespuesta(HttpStatus.BAD_REQUEST, error);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponseDTO>
    manejarParametroObligatorioFaltante(
            MissingServletRequestParameterException ex,
            HttpServletRequest request) {

        ErrorResponseDTO error = construirError(
                HttpStatus.BAD_REQUEST,
                "Solicitud inválida",
                "El parámetro obligatorio '"
                        + ex.getParameterName()
                        + "' no fue enviado",
                request);

        return construirRespuesta(HttpStatus.BAD_REQUEST, error);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDTO> manejarCuerpoNoValido(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        ErrorResponseDTO error = construirError(
                HttpStatus.BAD_REQUEST,
                "Solicitud inválida",
                "El cuerpo de la solicitud no es válido",
                request);

        return construirRespuesta(HttpStatus.BAD_REQUEST, error);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponseDTO> manejarMetodoNoSoportado(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {

        ErrorResponseDTO error = construirError(
                HttpStatus.METHOD_NOT_ALLOWED,
                "Método no permitido",
                "El método HTTP usado no está permitido para este recurso",
                request);

        return construirRespuesta(
                HttpStatus.METHOD_NOT_ALLOWED,
                error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDTO> manejarAccessDeniedException(
            AccessDeniedException ex,
            HttpServletRequest request) {

        if (auditSecurityService != null) {
            auditSecurityService.recordDenied(
                    request,
                    "ACCESS_DENIED",
                    "INSUFFICIENT_AUTHORITY");
        }

        ErrorResponseDTO error = construirError(
                HttpStatus.FORBIDDEN,
                "No autorizado",
                "No tiene permisos para acceder a este recurso",
                request);

        return construirRespuesta(HttpStatus.FORBIDDEN, error);
    }

    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<ErrorResponseDTO> manejarErrorDeAlmacenamiento(
            FileStorageException ex,
            HttpServletRequest request) {

        String correlationId = obtenerCorrelationId(request);

        log.error(
                "Error de almacenamiento [{}] en {}",
                correlationId,
                request.getRequestURI(),
                ex);

        ErrorResponseDTO error = construirError(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Almacenamiento no disponible",
                "No se pudo completar la operación de archivos",
                request);

        return construirRespuesta(
                HttpStatus.SERVICE_UNAVAILABLE,
                error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> manejarExceptionGeneral(
            Exception ex,
            HttpServletRequest request) {

        String correlationId = obtenerCorrelationId(request);

        log.error(
                "Error no controlado [{}] en la ruta {}",
                correlationId,
                request.getRequestURI(),
                ex);

        ErrorResponseDTO error = construirError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Error interno del servidor",
                "Ocurrió un error inesperado",
                request);

        return construirRespuesta(
                HttpStatus.INTERNAL_SERVER_ERROR,
                error);
    }

    private ErrorResponseDTO construirError(
            HttpStatus status,
            String error,
            String mensaje,
            HttpServletRequest request) {

        return ErrorResponseDTO.builder()
                .fecha(LocalDateTime.now())
                .estado(status.value())
                .error(error)
                .mensaje(mensaje)
                .ruta(request.getRequestURI())
                .correlacionId(obtenerCorrelationId(request))
                .build();
    }

    private ErrorResponseDTO construirErrorConDetalles(
            HttpStatus status,
            String error,
            String mensaje,
            HttpServletRequest request,
            Map<String, String> detalles) {

        return ErrorResponseDTO.builder()
                .fecha(LocalDateTime.now())
                .estado(status.value())
                .error(error)
                .mensaje(mensaje)
                .ruta(request.getRequestURI())
                .correlacionId(obtenerCorrelationId(request))
                .detalles(detalles)
                .build();
    }

    private ResponseEntity<ErrorResponseDTO> construirRespuesta(
            HttpStatus status,
            ErrorResponseDTO error) {

        return ResponseEntity.status(status)
                .header(
                        CorrelationIdContext.HEADER_NAME,
                        error.getCorrelacionId())
                .body(error);
    }

    private String obtenerCorrelationId(
            HttpServletRequest request) {

        return CorrelationIdContext.getOrCreate(request);
    }

    private String construirMensajeParametroInvalido(
            MethodArgumentTypeMismatchException ex) {

        String nombreParametro = ex.getName();

        if (nombreParametro == null || nombreParametro.isBlank()) {
            return "Uno de los parámetros enviados no es válido";
        }

        return "El valor enviado para el parámetro '"
                + nombreParametro
                + "' no es válido";
    }

    private String mensajeSeguro(
            String mensaje,
            String mensajePorDefecto) {

        if (mensaje == null || mensaje.isBlank()) {
            return mensajePorDefecto;
        }

        return mensaje;
    }
}