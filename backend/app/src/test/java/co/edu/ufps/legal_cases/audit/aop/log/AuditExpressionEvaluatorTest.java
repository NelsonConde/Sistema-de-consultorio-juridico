package co.edu.ufps.legal_cases.audit.aop.log;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.reflect.Method;
import java.util.Map;

import org.junit.jupiter.api.Test;

class AuditExpressionEvaluatorTest {

    private final AuditExpressionEvaluator evaluator = new AuditExpressionEvaluator();

    @Test
    void evaluatesExplicitScalarsAndRejectsSecretsAndRuntimeObjects() throws Exception {
        Method method = Fixture.class.getDeclaredMethod(
                "execute", Long.class, String.class, SecretPayload.class);
        Object[] arguments = {7L, "CLOSED", new SecretPayload("must-not-leak")};

        assertEquals("7", evaluator.evaluateText("#id", method, arguments, null));

        Map<String, String> metadata = evaluator.evaluateMetadata(
                new String[] {
                    "requestedState=#state",
                    "password=#state",
                    "payload=#payload",
                    "invalid"
                },
                method,
                arguments,
                null);

        assertEquals(Map.of("requestedState", "CLOSED"), metadata);
        assertFalse(metadata.toString().contains("must-not-leak"));
    }

    static class Fixture {
        void execute(Long id, String state, SecretPayload payload) {
        }
    }

    record SecretPayload(String password) {
        @Override
        public String toString() {
            return "runtime-object:" + password;
        }
    }
}
