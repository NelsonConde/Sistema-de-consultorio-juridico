package co.edu.ufps.legal_cases.business.model.proceso;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

class ProcesoTemporalMilestoneTest {

    @Test
    void fechaCreacionDebeSerInmutableEnActualizacionesJpa() throws Exception {
        Field campoFechaCreacion =
                Proceso.class.getDeclaredField("fechaCreacion");

        Column column = campoFechaCreacion.getAnnotation(Column.class);

        assertNotNull(column);
        assertFalse(column.updatable());
        assertEquals("fecha_creacion", column.name());
    }

    @Test
    void debeAsignarFechaCreacionAlPersistir() throws Exception {
        Proceso proceso = new Proceso();

        Method prePersist = obtenerMetodoAnotado(PrePersist.class);
        prePersist.setAccessible(true);
        prePersist.invoke(proceso);

        assertNotNull(proceso.getFechaCreacion());
    }

    @Test
    void noDebeModificarFechaCreacionAlActualizar() throws Exception {
        Proceso proceso = new Proceso();
        LocalDateTime fechaOriginal =
                LocalDateTime.of(2026, 8, 10, 14, 30);

        proceso.setFechaCreacion(fechaOriginal);

        Method preUpdate = obtenerMetodoAnotado(PreUpdate.class);
        preUpdate.setAccessible(true);
        preUpdate.invoke(proceso);

        assertEquals(fechaOriginal, proceso.getFechaCreacion());
    }

    private Method obtenerMetodoAnotado(
            Class<? extends java.lang.annotation.Annotation> annotationType) {

        return java.util.Arrays.stream(Proceso.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(annotationType))
                .findFirst()
                .orElseThrow();
    }
}
