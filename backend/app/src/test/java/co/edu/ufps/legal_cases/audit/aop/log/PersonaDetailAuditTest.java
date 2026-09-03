package co.edu.ufps.legal_cases.audit.aop.log;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.Map;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import co.edu.ufps.legal_cases.audit.model.log.AuditEvent;
import co.edu.ufps.legal_cases.audit.model.log.AuditOutcome;
import co.edu.ufps.legal_cases.audit.model.log.AuditSource;
import co.edu.ufps.legal_cases.audit.service.log.AuditLogService;
import co.edu.ufps.legal_cases.audit.service.log.AuditRequestContext;
import co.edu.ufps.legal_cases.business.dto.persona.PersonaDTO;
import jakarta.persistence.EntityManager;

class PersonaDetailAuditTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void accesoExitosoDebeAuditarIdSinRegistrarFichaCompleta() throws Throwable {
        AuditLogService auditLogService = mock(AuditLogService.class);
        AuditAspect auditAspect = new AuditAspect(
                auditLogService,
                new AuditRequestContext(),
                new AuditExpressionEvaluator(),
                new AuditStateSnapshotService(mock(EntityManager.class)));
        Method method = Fixture.class.getDeclaredMethod("obtenerPorId", Long.class);
        Auditable auditable = method.getAnnotation(Auditable.class);
        MethodSignature signature = mock(MethodSignature.class);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);

        PersonaDTO resultadoSensible = PersonaDTO.builder()
                .id(42L)
                .numeroDocumento("1090123456")
                .correo("persona@example.com")
                .telefono("3001234567")
                .direccion("Direccion sensible")
                .salario(5000000)
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "asesor.demo",
                "N/A",
                java.util.List.of()));

        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[] {42L});
        when(joinPoint.proceed()).thenReturn(resultadoSensible);

        assertEquals(resultadoSensible, auditAspect.audit(joinPoint, auditable));

        ArgumentCaptor<AuditEvent> event = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditLogService).recordSuccess(event.capture());

        AuditEvent auditEvent = event.getValue();
        assertEquals("asesor.demo", auditEvent.getActorUsername());
        assertEquals("CONSULTAR_DETALLE_PERSONA", auditEvent.getAction());
        assertEquals("Persona", auditEvent.getEntityName());
        assertEquals("42", auditEvent.getEntityId());
        assertEquals(AuditOutcome.SUCCESS, auditEvent.getOutcome());
        assertEquals(AuditSource.SYSTEM, auditEvent.getSource());
        assertNotNull(auditEvent.getOccurredAt());
        assertNotNull(auditEvent.getCorrelationId());
        assertEquals(Map.of(), auditEvent.getMetadata());
        assertEquals(Map.of(), auditEvent.getBeforeState());
        assertEquals(Map.of(), auditEvent.getAfterState());

        String eventoSerializado = auditEvent.toString();
        assertTrue(eventoSerializado.contains("entityId=42"));
        assertFalse(eventoSerializado.contains("1090123456"));
        assertFalse(eventoSerializado.contains("persona@example.com"));
        assertFalse(eventoSerializado.contains("3001234567"));
        assertFalse(eventoSerializado.contains("Direccion sensible"));
        assertFalse(eventoSerializado.contains("5000000"));

        verify(joinPoint).proceed();
    }

    static class Fixture {

        @Auditable(
                action = "CONSULTAR_DETALLE_PERSONA",
                entityName = "Persona",
                entityId = "#id")
        PersonaDTO obtenerPorId(Long id) {
            return PersonaDTO.builder()
                    .id(id)
                    .build();
        }
    }
}
