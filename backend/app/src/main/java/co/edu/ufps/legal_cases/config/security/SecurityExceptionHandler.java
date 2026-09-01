package co.edu.ufps.legal_cases.config.security;

import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.common.exception.dto.ErrorResponseDTO;
import co.edu.ufps.legal_cases.audit.service.log.AuditSecurityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

// Maneja errores generados directamente por Spring Security.
// Estos errores no siempre pasan por GlobalExceptionHandler,
// por eso se responden aquí con el mismo DTO estándar de la API.
//
// 401 = no autenticado o token inválido.
// 403 = autenticado, pero sin permisos suficientes.
@Component
public class SecurityExceptionHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final JsonMapper jsonMapper;
    private final AuditSecurityService auditSecurityService;

    public SecurityExceptionHandler(JsonMapper jsonMapper) {
        this(jsonMapper, null);
    }

    @Autowired
    public SecurityExceptionHandler(JsonMapper jsonMapper, AuditSecurityService auditSecurityService) {
        this.jsonMapper = jsonMapper;
        this.auditSecurityService = auditSecurityService;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            org.springframework.security.core.AuthenticationException authException) throws IOException {

        recordDenied(request, "AUTHENTICATION_REQUIRED", "UNAUTHENTICATED");

        ErrorResponseDTO error = construirError(
                HttpStatus.UNAUTHORIZED,
                "No autenticado",
                "Debe iniciar sesión para acceder a este recurso",
                request);

        escribirRespuesta(response, HttpStatus.UNAUTHORIZED, error);
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {

        recordDenied(request, "ACCESS_DENIED", "INSUFFICIENT_AUTHORITY");

        ErrorResponseDTO error = construirError(
                HttpStatus.FORBIDDEN,
                "No autorizado",
                "No tiene permisos para acceder a este recurso",
                request);

        escribirRespuesta(response, HttpStatus.FORBIDDEN, error);
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
                .correlacionId(CorrelationIdContext.getOrCreate(request))
                .build();
    }

    private void escribirRespuesta(
            HttpServletResponse response,
            HttpStatus status,
            ErrorResponseDTO error) throws IOException {

        response.setStatus(status.value());
        response.setHeader(CorrelationIdContext.HEADER_NAME, error.getCorrelacionId());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        jsonMapper.writeValue(response.getWriter(), error);
    }

    private void recordDenied(HttpServletRequest request, String action, String reasonCode) {
        if (auditSecurityService != null) {
            auditSecurityService.recordDenied(request, action, reasonCode);
        }
    }
}
