package co.edu.ufps.legal_cases.audit.aop.log;

import co.edu.ufps.legal_cases.audit.service.log.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Aspecto (AOP) que intercepta de forma transparente la ejecución de métodos marcados
 * con la anotación @Auditable. Su función es extraer el usuario autenticado del
 * contexto de seguridad y recolectar la información del evento para enviarla al
 * servicio de auditoría, sin acoplar la lógica de negocio a la de trazabilidad.
 */
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogService auditLogService;

    @AfterReturning(pointcut = "@annotation(auditable)", returning = "result")
    public void registrarActividadAuditoria(JoinPoint joinPoint, Auditable auditable, Object result) {
        String nombreUsuario = obtenerNombreUsuario();
        String accion = auditable.action();
        String nombreEntidad = auditable.entityName();
        
        // Intentar extraer el ID de la entidad si es posible
        String idEntidad = extraerIdEntidad(result, joinPoint);
        
        String detalles = "Método ejecutado: " + joinPoint.getSignature().getName() + 
                          ". Argumentos: " + Arrays.toString(joinPoint.getArgs());

        auditLogService.logAction(nombreUsuario, accion, nombreEntidad, idEntidad, detalles);
    }

    private String obtenerNombreUsuario() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "SISTEMA";
    }

    private String extraerIdEntidad(Object result, JoinPoint joinPoint) {
        // Lógica heurística para intentar sacar el ID de la entidad afectada.
        // 1. El resultado puede ser un DTO con getId() o con getters que terminan en "Id".
        if (result != null) {
            String idDesdeResultado = extraerIdDeObjeto(result);
            if (idDesdeResultado != null) {
                return idDesdeResultado;
            }
        }

        // 2. Si no hay id en el resultado, intentar tomarlo de los argumentos del método.
        for (Object arg : joinPoint.getArgs()) {
            if (arg == null) {
                continue;
            }
            if (arg instanceof Long || arg instanceof String) {
                return arg.toString();
            }
            String idDesdeArgumento = extraerIdDeObjeto(arg);
            if (idDesdeArgumento != null) {
                return idDesdeArgumento;
            }
        }

        return null;
    }

    private String extraerIdDeObjeto(Object objeto) {
        try {
            Method getIdMethod = objeto.getClass().getMethod("getId");
            Object id = getIdMethod.invoke(objeto);
            if (id != null) {
                return id.toString();
            }
        } catch (Exception ignored) {
            // Continuar si no existe getId().
        }

        for (Method method : objeto.getClass().getMethods()) {
            if (method.getParameterCount() != 0) {
                continue;
            }
            String nombreMetodo = method.getName();
            if (nombreMetodo.startsWith("get") && nombreMetodo.endsWith("Id")
                    && !"getClass".equals(nombreMetodo)) {
                try {
                    Object id = method.invoke(objeto);
                    if (id != null) {
                        return id.toString();
                    }
                } catch (Exception ignored) {
                    // Ignorar invocaciones fallidas.
                }
            }
        }

        return null;
    }
}
