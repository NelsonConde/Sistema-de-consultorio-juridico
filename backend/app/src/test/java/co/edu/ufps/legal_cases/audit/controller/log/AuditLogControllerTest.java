package co.edu.ufps.legal_cases.audit.controller.log;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_AUDITORIA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import co.edu.ufps.legal_cases.audit.service.log.AuditLogService;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import jakarta.validation.constraints.Max;

class AuditLogControllerTest {

    @Test
    void requiresDedicatedAuditPermission() {
        PreAuthorize authorization = AuditLogController.class.getAnnotation(PreAuthorize.class);

        assertTrue(authorization.value().contains(VER_AUDITORIA));
        assertEquals("Ver auditoría", VER_AUDITORIA);
    }

    @Test
    void declaresMaximumPageSizeOfOneHundred() throws Exception {
        Method method = AuditLogController.class.getDeclaredMethod(
                "getAuditLogs",
                int.class,
                int.class,
                String.class,
                String.class,
                String.class,
                co.edu.ufps.legal_cases.audit.model.log.AuditOutcome.class,
                String.class,
                java.time.Instant.class,
                java.time.Instant.class,
                String.class,
                String.class);

        Max max = method.getParameters()[1].getAnnotation(Max.class);
        assertEquals(100, max.value());
    }

    @Test
    void rejectsUnknownSortField() {
        AuditLogController controller = new AuditLogController(mock(AuditLogService.class));

        assertThrows(BusinessException.class, () -> controller.getAuditLogs(
                0, 20, null, null, null, null, null, null, null, "details", "desc"));
    }
}
