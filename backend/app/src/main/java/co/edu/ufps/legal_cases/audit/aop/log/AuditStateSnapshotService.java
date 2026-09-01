package co.edu.ufps.legal_cases.audit.aop.log;

import java.time.temporal.TemporalAccessor;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;

/** Captura sólo propiedades escalares declaradas expresamente en la anotación. */
@Component
public class AuditStateSnapshotService {

    private static final int MAX_VALUE_LENGTH = 500;

    private final EntityManager entityManager;

    public AuditStateSnapshotService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Map<String, String> captureEntity(
            String entityName,
            String entityId,
            String[] trackedFields) {
        if (entityId == null || trackedFields == null || trackedFields.length == 0) {
            return Map.of();
        }
        try {
            Class<?> entityClass = resolveEntityClass(entityName);
            Object id = convertId(entityId, entityClass);
            if (entityClass == null || id == null) {
                return Map.of();
            }
            Object entity = entityManager.find(entityClass, id);
            return captureObject(entity, trackedFields);
        } catch (RuntimeException ignored) {
            return Map.of();
        }
    }

    public Map<String, String> captureResult(Object result, String[] trackedFields) {
        return captureObject(result, trackedFields);
    }

    private Map<String, String> captureObject(Object source, String[] trackedFields) {
        if (source == null || trackedFields == null || trackedFields.length == 0) {
            return Map.of();
        }
        BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(source);
        Map<String, String> snapshot = new LinkedHashMap<>();
        for (String field : trackedFields) {
            if (field == null || field.isBlank() || !wrapper.isReadableProperty(field)) {
                continue;
            }
            try {
                String value = scalarText(wrapper.getPropertyValue(field));
                if (value != null) {
                    snapshot.put(field, value);
                }
            } catch (RuntimeException ignored) {
                // Una propiedad opcional no debe afectar la operación de negocio.
            }
        }
        return snapshot;
    }

    private Class<?> resolveEntityClass(String entityName) {
        if (entityName == null) {
            return null;
        }
        return entityManager.getMetamodel().getEntities().stream()
                .map(EntityType::getJavaType)
                .filter(type -> type.getSimpleName().equals(entityName))
                .findFirst()
                .orElse(null);
    }

    private Object convertId(String value, Class<?> entityClass) {
        if (entityClass == null) {
            return null;
        }
        try {
            Class<?> idType = entityManager.getMetamodel().entity(entityClass)
                    .getIdType().getJavaType();
            if (idType == Long.class || idType == long.class) {
                return Long.valueOf(value);
            }
            if (idType == UUID.class) {
                return UUID.fromString(value);
            }
            return idType == String.class ? value : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private String scalarText(Object value) {
        if (value == null) {
            return null;
        }
        boolean scalar = value instanceof CharSequence
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Enum<?>
                || value instanceof UUID
                || value instanceof TemporalAccessor;
        if (!scalar) {
            return null;
        }
        String text = value.toString();
        return text.length() <= MAX_VALUE_LENGTH ? text : text.substring(0, MAX_VALUE_LENGTH);
    }
}
