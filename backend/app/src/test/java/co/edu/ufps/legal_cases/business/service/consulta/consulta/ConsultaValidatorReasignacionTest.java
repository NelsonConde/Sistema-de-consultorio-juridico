package co.edu.ufps.legal_cases.business.service.consulta.consulta;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import co.edu.ufps.legal_cases.business.model.catalogo.Area;
import co.edu.ufps.legal_cases.business.model.consulta.Consulta;
import co.edu.ufps.legal_cases.business.model.perfil.Asesor;
import co.edu.ufps.legal_cases.business.model.perfil.Estudiante;
import co.edu.ufps.legal_cases.business.service.acceso.consulta.ConsultaAccessService;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

class ConsultaValidatorReasignacionTest {

    private ConsultaValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ConsultaValidator(mock(ConsultaAccessService.class));
    }

    @Test
    void aceptaAsesorYEstudianteCoherentesConElArea() {
        Area area = area(1L);
        Asesor asesor = asesor(10L, area);
        Estudiante estudiante = estudiante(20L, asesor);
        Consulta consulta = consulta(area, asesor, estudiante);

        assertDoesNotThrow(() -> validator.validarCompatibilidadResponsables(consulta));
    }

    @Test
    void rechazaAsesorDeOtraArea() {
        Area areaConsulta = area(1L);
        Consulta consulta = consulta(
                areaConsulta,
                asesor(10L, area(2L)),
                null);

        assertThrows(
                BusinessException.class,
                () -> validator.validarCompatibilidadResponsables(consulta));
    }

    @Test
    void rechazaEstudianteQueNoPerteneceAlAsesorAsignado() {
        Area area = area(1L);
        Asesor asesorConsulta = asesor(10L, area);
        Asesor asesorEstudiante = asesor(11L, area);
        Consulta consulta = consulta(
                area,
                asesorConsulta,
                estudiante(20L, asesorEstudiante));

        assertThrows(
                BusinessException.class,
                () -> validator.validarCompatibilidadResponsables(consulta));
    }

    private Consulta consulta(Area area, Asesor asesor, Estudiante estudiante) {
        Consulta consulta = new Consulta();
        consulta.setId(1L);
        consulta.setArea(area);
        consulta.setAsesor(asesor);
        consulta.setEstudiante(estudiante);
        return consulta;
    }

    private Area area(Long id) {
        Area area = new Area();
        area.setId(id);
        return area;
    }

    private Asesor asesor(Long id, Area area) {
        Asesor asesor = new Asesor();
        asesor.setId(id);
        asesor.setArea(area);
        return asesor;
    }

    private Estudiante estudiante(Long id, Asesor asesor) {
        Estudiante estudiante = new Estudiante();
        estudiante.setId(id);
        estudiante.setAsesor(asesor);
        return estudiante;
    }
}
