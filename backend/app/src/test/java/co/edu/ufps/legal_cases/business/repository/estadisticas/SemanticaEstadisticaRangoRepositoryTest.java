package co.edu.ufps.legal_cases.business.repository.estadisticas;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.data.jpa.repository.Query;

import co.edu.ufps.legal_cases.business.repository.consulta.ConsultaRepository;
import co.edu.ufps.legal_cases.business.repository.proceso.ProcesoRepository;

class SemanticaEstadisticaRangoRepositoryTest {

    @Test
    void consultasPorRangoUsanFechaDeConsultaYExcluyenArchivadas() {
        List<Method> metodos = metodosPorRango(ConsultaRepository.class);

        assertFalse(metodos.isEmpty());
        assertAll(metodos.stream().map(method -> (Executable) () -> {
            String sql = obtenerSql(method);
            assertTrue(sql.contains("c.fecha"), method.getName());
            assertFalse(sql.contains("last_updated_at"), method.getName());
            assertTrue(sql.contains("ARCHIVADO"), method.getName());
        }));
    }

    @Test
    void clasificacionPorRangoUsaEstadoCerradoYNoResultado() throws Exception {
        Method method = ConsultaRepository.class.getMethod(
                "contarFinalizadasYPendientesPorRango",
                String.class,
                String.class);

        String sql = obtenerSql(method);

        assertAll(
                () -> assertTrue(sql.contains("c.estado = 'CERRADO'")),
                () -> assertTrue(sql.contains("c.estado <> 'CERRADO'")),
                () -> assertTrue(sql.contains("c.estado <> 'ARCHIVADO'")),
                () -> assertFalse(sql.contains("c.resultado")));
    }

    @Test
    void procesosPorRangoUsanFechaDeCreacionPropiaYSoloRegistrosActivos()
            throws Exception {

        Method method = ProcesoRepository.class.getMethod(
                "contarProcesosPorEstadoPorRango",
                String.class,
                String.class);

        String sql = obtenerSql(method);

        assertAll(
                () -> assertTrue(sql.contains("p.fecha_creacion")),
                () -> assertFalse(sql.contains("c.fecha")),
                () -> assertTrue(sql.contains("c.estado <> 'ARCHIVADO'")),
                () -> assertTrue(sql.contains("p.activo = true")));
    }

    private List<Method> metodosPorRango(Class<?> repositoryType) {
        return Arrays.stream(repositoryType.getDeclaredMethods())
                .filter(method -> method.getName().contains("PorRango"))
                .filter(method -> method.isAnnotationPresent(Query.class))
                .toList();
    }

    private String obtenerSql(Method method) {
        return method.getAnnotation(Query.class).value();
    }
}
