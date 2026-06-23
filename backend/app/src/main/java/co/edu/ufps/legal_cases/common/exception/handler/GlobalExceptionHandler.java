package co.edu.ufps.legal_cases.common.exception.handler;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.common.exception.dto.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Maneja reglas de negocio controladas por los services.
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponseDTO> manejarBusinessException(
            BusinessException ex,
            HttpServletRequest request) {

        ErrorResponseDTO error = construirError(
                HttpStatus.BAD_REQUEST,
                "Error de negocio",
                mensajeSeguro(ex.getMessage(), "La solicitud no cumple una regla de negocio"),
                request);

        return ResponseEntity.badRequest().body(error);
    }

    // Maneja errores de validación en DTOs con @Valid:
    // @NotNull, @NotBlank, @Email, @Size, etc.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> manejarErroresValidacion(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, String> detalles = new LinkedHashMap<>();

        ex.getBindingResult().getFieldErrors()
                .forEach(error -> detalles.put(error.getField(), error.getDefaultMessage()));

        ErrorResponseDTO respuesta = construirErrorConDetalles(
                HttpStatus.BAD_REQUEST,
                "Error de validación",
                "Uno o más campos no son válidos",
                request,
                detalles);

        return ResponseEntity.badRequest().body(respuesta);
    }

    // Maneja validaciones aplicadas sobre parámetros o path variables,
    // cuando se usa validación directa fuera de un DTO.
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseDTO> manejarViolacionesDeRestriccion(
            ConstraintViolationException ex,
            HttpServletRequest request) {

        Map<String, String> detalles = new LinkedHashMap<>();

        ex.getConstraintViolations().forEach(violation -> detalles.put(
                violation.getPropertyPath().toString(),
                violation.getMessage()));

        ErrorResponseDTO respuesta = construirErrorConDetalles(
                HttpStatus.BAD_REQUEST,
                "Error de validación",
                "Uno o más parámetros no son válidos",
                request,
                detalles);

        return ResponseEntity.badRequest().body(respuesta);
    }

    // Maneja parámetros con tipo inválido, por ejemplo:
    // ?id=abc, ?activo=texto, ?estado=valor_no_valido
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDTO> manejarParametroInvalido(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        ErrorResponseDTO error = construirError(
                HttpStatus.BAD_REQUEST,
                "Solicitud inválida",
                construirMensajeParametroInvalido(ex),
                request);

        return ResponseEntity.badRequest().body(error);
    }

    // Maneja parámetros obligatorios que no fueron enviados.
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponseDTO> manejarParametroObligatorioFaltante(
            MissingServletRequestParameterException ex,
            HttpServletRequest request) {

        ErrorResponseDTO error = construirError(
                HttpStatus.BAD_REQUEST,
                "Solicitud inválida",
                "El parámetro obligatorio '" + ex.getParameterName() + "' no fue enviado",
                request);

        return ResponseEntity.badRequest().body(error);
    }

    // Maneja errores en el cuerpo JSON:
    // JSON mal formado, enum inválido en body o estructura incompatible.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDTO> manejarCuerpoNoValido(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        ErrorResponseDTO error = construirError(
                HttpStatus.BAD_REQUEST,
                "Solicitud inválida",
                "El cuerpo de la solicitud no es válido",
                request);

        return ResponseEntity.badRequest().body(error);
    }

    // Maneja métodos HTTP no soportados para un endpoint.
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponseDTO> manejarMetodoNoSoportado(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {

        ErrorResponseDTO error = construirError(
                HttpStatus.METHOD_NOT_ALLOWED,
                "Método no permitido",
                "El método HTTP usado no está permitido para este recurso",
                request);

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(error);
    }

    // Maneja usuarios autenticados que no tienen permisos suficientes.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDTO> manejarAccessDeniedException(
            AccessDeniedException ex,
            HttpServletRequest request) {

        ErrorResponseDTO error = construirError(
                HttpStatus.FORBIDDEN,
                "No autorizado",
                "No tiene permisos para acceder a este recurso",
                request);

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    // Maneja cualquier excepción no controlada.
    // El detalle técnico queda en logs, pero no se expone al cliente.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> manejarExceptionGeneral(
            Exception ex,
            HttpServletRequest request) {

        log.error("Error no controlado en la ruta {}", request.getRequestURI(), ex);

        ErrorResponseDTO error = construirError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Error interno del servidor",
                "Ocurrió un error inesperado",
                request);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
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
                .detalles(detalles)
                .build();
    }

    private String construirMensajeParametroInvalido(MethodArgumentTypeMismatchException ex) {
        String nombreParametro = ex.getName();

        if (nombreParametro == null || nombreParametro.isBlank()) {
            return "Uno de los parámetros enviados no es válido";
        }

        return "El valor enviado para el parámetro '" + nombreParametro + "' no es válido";
    }

    private String mensajeSeguro(String mensaje, String mensajePorDefecto) {
        if (mensaje == null || mensaje.isBlank()) {
            return mensajePorDefecto;
        }

        return mensaje;
    }
}