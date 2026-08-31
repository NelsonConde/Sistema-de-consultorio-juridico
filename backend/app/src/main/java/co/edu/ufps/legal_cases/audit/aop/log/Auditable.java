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
 * desea registrar un evento probatorio tanto si el método termina correctamente
 * como si falla o deniega la operación.
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

    /**
     * Expresión SpEL explícita que obtiene el identificador lógico. Puede usar los
     * parámetros del método y la variable {@code #result}.
     */
    String entityId();

    /**
     * Metadatos permitidos en formato {@code nombre=expresionSpel}. Los valores
     * complejos, archivos y claves sensibles son rechazados por el evaluador.
     */
    String[] metadata() default {};

    /** Propiedades escalares cuya transición anterior/nueva debe conservarse. */
    String[] trackedFields() default {};

    /**
     * Motivo funcional explícito. Nunca se toma automáticamente del mensaje de una
     * excepción ni de los argumentos completos del método.
     */
    String reason() default "";
}
