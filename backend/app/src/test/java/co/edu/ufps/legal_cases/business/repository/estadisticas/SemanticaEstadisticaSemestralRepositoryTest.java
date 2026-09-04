package co.edu.ufps.legal_cases.business.repository.estadisticas;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

class SemanticaEstadisticaSemestralRepositoryTest {

    @Test
    void consultasSemestralesUsanFechaDeConsultaYExcluyenArchivadas() {
        List<Method> metodos =
                metodosSemestrales(ConsultaRepository.class);

        assertFalse(metodos.isEmpty());

        assertAll(metodos.stream()
                .map(method -> (Executable) () -> {
                    String sql = obtenerSql(method);

                    assertTrue(
                            sql.contains("c.fecha"),
                            method.getName());

                    assertFalse(
                            sql.contains("last_updated_at"),
                            method.getName());

                    assertTrue(
                            sql.contains("ARCHIVADO"),
                            method.getName());
                }));
    }

    @Test
    void clasificacionSemestralUsaEstadoCerradoYNoResultado() {
        List<Method> metodos = Arrays.stream(
                        ConsultaRepository.class.getDeclaredMethods())
                .filter(method -> method.getName().startsWith(
                        "contarFinalizadasYPendientesPorPeriodo"))
                .toList();

        assertEquals(4, metodos.size());

        assertAll(metodos.stream()
                .map(method -> (Executable) () -> {
                    String sql = obtenerSql(method);

                    assertTrue(
                            sql.contains("c.estado = 'CERRADO'"),
                            method.getName());

                    assertTrue(
                            sql.contains("c.estado <> 'CERRADO'"),
                            method.getName());

                    assertTrue(
                            sql.contains("c.estado <> 'ARCHIVADO'"),
                            method.getName());

                    assertFalse(
                            sql.contains("c.resultado"),
                            method.getName());
                }));
    }

    @Test
    void procesosSemestralesUsanFechaDeCreacionPropiaYSoloRegistrosActivos() {
        List<Method> metodos =
                metodosSemestrales(ProcesoRepository.class);

        assertFalse(metodos.isEmpty());

        assertAll(metodos.stream()
                .map(method -> (Executable) () -> {
                    String sql = obtenerSql(method);

                    assertTrue(
                            sql.contains("p.fecha_creacion"),
                            method.getName());

                    assertFalse(
                            sql.contains("c.fecha"),
                            method.getName());

                    assertTrue(
                            sql.contains("c.estado <> 'ARCHIVADO'"),
                            method.getName());

                    assertTrue(
                            sql.contains("p.activo = true"),
                            method.getName());
                }));
    }

    private List<Method> metodosSemestrales(
            Class<?> repositoryType) {

        return Arrays.stream(repositoryType.getDeclaredMethods())
                .filter(method ->
                        method.getName().contains("PorPeriodo"))
                .filter(method ->
                        method.isAnnotationPresent(Query.class))
                .toList();
    }

    private String obtenerSql(Method method) {
        return method.getAnnotation(Query.class).value();
    }
}