package co.edu.ufps.legal_cases.audit.aop.log;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import co.edu.ufps.legal_cases.audit.service.log.AuditLogService;
import co.edu.ufps.legal_cases.business.dto.persona.PersonaDTO;

class PersonaDetailAuditTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void accesoExitosoDebeAuditarIdSinRegistrarFichaCompleta() {
        AuditLogService auditLogService = mock(AuditLogService.class);
        AuditAspect auditAspect = new AuditAspect(auditLogService);
        JoinPoint joinPoint = mock(JoinPoint.class);
        Signature signature = mock(Signature.class);
        Auditable auditable = mock(Auditable.class);

        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("obtenerPorId");
        when(joinPoint.getArgs()).thenReturn(new Object[] { 42L });
        when(auditable.action()).thenReturn("CONSULTAR_DETALLE_PERSONA");
        when(auditable.entityName()).thenReturn("Persona");

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

        auditAspect.registrarActividadAuditoria(joinPoint, auditable, resultadoSensible);

        ArgumentCaptor<String> entityId = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> details = ArgumentCaptor.forClass(String.class);
        verify(auditLogService).logAction(
                org.mockito.ArgumentMatchers.eq("asesor.demo"),
                org.mockito.ArgumentMatchers.eq("CONSULTAR_DETALLE_PERSONA"),
                org.mockito.ArgumentMatchers.eq("Persona"),
                entityId.capture(),
                details.capture());

        assertEquals("42", entityId.getValue());
        assertTrue(details.getValue().contains("Argumentos: [42]"));
        assertFalse(details.getValue().contains("1090123456"));
        assertFalse(details.getValue().contains("persona@example.com"));
        assertFalse(details.getValue().contains("3001234567"));
        assertFalse(details.getValue().contains("Direccion sensible"));
        assertFalse(details.getValue().contains("5000000"));
    }
}