package co.edu.ufps.legal_cases.audit.aop.log;

import java.lang.reflect.Method;
import java.time.temporal.TemporalAccessor;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

/** Evalúa exclusivamente las expresiones declaradas en {@link Auditable}. */
@Component
public class AuditExpressionEvaluator {

    private static final Pattern SAFE_KEY = Pattern.compile("[A-Za-z][A-Za-z0-9_.-]{0,63}");
    private static final Pattern SENSITIVE_KEY = Pattern.compile(
            ".*(password|contrasena|contraseña|token|secret|cookie|authorization|document|documento|email|correo).*",
            Pattern.CASE_INSENSITIVE);
    private static final int MAX_VALUE_LENGTH = 500;

    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNames = new DefaultParameterNameDiscoverer();

    public String evaluateText(
            String expression,
            Method method,
            Object[] arguments,
            Object result) {
        if (expression == null || expression.isBlank()) {
            return null;
        }
        Object value = evaluate(expression, method, arguments, result);
        return toAllowedText(value);
    }

    public Map<String, String> evaluateMetadata(
            String[] declarations,
            Method method,
            Object[] arguments,
            Object result) {
        Map<String, String> metadata = new LinkedHashMap<>();
        if (declarations == null) {
            return metadata;
        }
        for (String declaration : declarations) {
            int separator = declaration == null ? -1 : declaration.indexOf('=');
            if (separator <= 0) {
                continue;
            }
            String key = declaration.substring(0, separator).trim();
            String expression = declaration.substring(separator + 1).trim();
            if (!SAFE_KEY.matcher(key).matches() || SENSITIVE_KEY.matcher(key).matches()) {
                continue;
            }
            try {
                String value = evaluateText(expression, method, arguments, result);
                if (value != null) {
                    metadata.put(key, value);
                }
            } catch (RuntimeException ignored) {
                // Una expresión opcional no debe alterar el caso de uso auditado.
            }
        }
        return metadata;
    }

    private Object evaluate(String expression, Method method, Object[] arguments, Object result) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        String[] names = parameterNames.getParameterNames(method);
        if (names != null) {
            for (int index = 0; index < names.length; index++) {
                context.setVariable(names[index], arguments[index]);
            }
        }
        context.setVariable("result", result);
        return parser.parseExpression(expression).getValue(context);
    }

    private String toAllowedText(Object value) {
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
        String text = value.toString().replace('\r', ' ').replace('\n', ' ').trim();
        if (text.isBlank()) {
            return null;
        }
        return text.length() <= MAX_VALUE_LENGTH ? text : text.substring(0, MAX_VALUE_LENGTH);
    }
}
