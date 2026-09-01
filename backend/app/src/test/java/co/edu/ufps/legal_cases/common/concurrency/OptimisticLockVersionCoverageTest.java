package co.edu.ufps.legal_cases.common.concurrency;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import co.edu.ufps.legal_cases.business.model.conciliacion.Conciliacion;
import co.edu.ufps.legal_cases.business.model.conciliacion.reunion.ReunionConciliacion;
import co.edu.ufps.legal_cases.business.model.consulta.Consulta;
import co.edu.ufps.legal_cases.business.model.persona.Persona;
import co.edu.ufps.legal_cases.business.model.proceso.Proceso;
import co.edu.ufps.legal_cases.business.model.seguimiento.Seguimiento;
import co.edu.ufps.legal_cases.business.model.seguimiento.respuesta.SeguimientoRespuesta;
import jakarta.persistence.Version;

class OptimisticLockVersionCoverageTest {

    static Stream<Class<?>> agregadosMutables() {
        return Stream.of(
                Consulta.class,
                Persona.class,
                Proceso.class,
                Seguimiento.class,
                SeguimientoRespuesta.class,
                Conciliacion.class,
                ReunionConciliacion.class);
    }

    @ParameterizedTest
    @MethodSource("agregadosMutables")
    void agregadoMutableTieneCampoVersionado(
            Class<?> tipoEntidad) throws Exception {

        Field version =
                tipoEntidad.getDeclaredField("version");

        assertNotNull(version);

        assertTrue(
                version.isAnnotationPresent(Version.class),
                () -> tipoEntidad.getSimpleName()
                        + " debe declarar @Version");
    }
}
