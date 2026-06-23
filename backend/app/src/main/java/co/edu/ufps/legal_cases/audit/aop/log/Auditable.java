package co.edu.ufps.legal_cases.audit.aop.log;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * AOP
 * Anotación personalizada que marca un método para ser interceptado por el
 * sistema de auditoría.
 * Debe ser colocada sobre métodos de servicios de negocio en los cuales se
 * desea registrar
 * un evento auditable cuando el método se ejecute exitosamente.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    /**
     * El tipo de acción que se está realizando (ej. "CREAR_CASO",
     * "ACTUALIZAR_ESTADO").
     */
    String action();

    /**
     * El nombre de la entidad afectada (ej. "Caso", "Seguimiento", "Usuario").
     */
    String entityName();
}
