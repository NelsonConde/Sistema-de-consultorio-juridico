package co.edu.ufps.legal_cases.business.service.consulta.consulta;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import co.edu.ufps.legal_cases.business.dto.consulta.ConsultaDTO;
import co.edu.ufps.legal_cases.business.model.catalogo.Area;
import co.edu.ufps.legal_cases.business.model.catalogo.Sede;
import co.edu.ufps.legal_cases.business.model.catalogo.Tema;
import co.edu.ufps.legal_cases.business.model.catalogo.Tipo;
import co.edu.ufps.legal_cases.business.model.consulta.Consulta;
import co.edu.ufps.legal_cases.business.model.perfil.Asesor;
import co.edu.ufps.legal_cases.business.model.perfil.Estudiante;
import co.edu.ufps.legal_cases.business.model.perfil.Monitor;
import co.edu.ufps.legal_cases.business.model.persona.Persona;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

class ConsultaCambioEstructuralValidatorTest {

    private ConsultaCambioEstructuralValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ConsultaCambioEstructuralValidator();
    }

    @Test
    void debePermitirCambiosNarrativosSiTieneActividad() {
        Consulta consulta = consultaBase();
        ConsultaDTO dto = dtoBase();

        assertDoesNotThrow(() -> validator.validarSiTieneActividad(
                consulta,
                dto,
                true,
                true));
    }

    @Test
    void debePermitirCambiosEstructuralesSiNoTieneActividad() {
        Consulta consulta = consultaBase();
        ConsultaDTO dto = dtoBase();
        dto.setAreaId(99L);

        assertDoesNotThrow(() -> validator.validarSiTieneActividad(
                consulta,
                dto,
                false,
                true));
    }

    @Test
    void debeBloquearCambioDePersonaPrincipalSiTieneActividad() {
        Consulta consulta = consultaBase();
        ConsultaDTO dto = dtoBase();
        dto.setPersonaId(99L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> validator.validarSiTieneActividad(consulta, dto, true, true));

        assertEquals(
                "No se pueden modificar datos estructurales de la consulta porque ya tiene procesos, seguimientos o conciliaciones asociadas",
                exception.getMessage());
    }

    @Test
    void debeBloquearCambioDeAreaSiTieneActividad() {
        Consulta consulta = consultaBase();
        ConsultaDTO dto = dtoBase();
        dto.setAreaId(99L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> validator.validarSiTieneActividad(consulta, dto, true, true));

        assertEquals(
                "No se pueden modificar datos estructurales de la consulta porque ya tiene procesos, seguimientos o conciliaciones asociadas",
                exception.getMessage());
    }

    @Test
    void debeBloquearCambioDePartesSiTieneActividad() {
        Consulta consulta = consultaBase();
        ConsultaDTO dto = dtoBase();
        dto.setPartesIds(List.of(2L, 99L));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> validator.validarSiTieneActividad(consulta, dto, true, true));

        assertEquals(
                "No se pueden modificar datos estructurales de la consulta porque ya tiene procesos, seguimientos o conciliaciones asociadas",
                exception.getMessage());
    }

    @Test
    void debeBloquearCambioDeResponsablesSiTieneActividadYPuedeAsignar() {
        Consulta consulta = consultaBase();
        ConsultaDTO dto = dtoBase();
        dto.setAsesorId(99L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> validator.validarSiTieneActividad(consulta, dto, true, true));

        assertEquals(
                "No se pueden modificar datos estructurales de la consulta porque ya tiene procesos, seguimientos o conciliaciones asociadas",
                exception.getMessage());
    }

    @Test
    void noDebeEvaluarResponsablesSiUsuarioNoPuedeAsignar() {
        Consulta consulta = consultaBase();
        ConsultaDTO dto = dtoBase();
        dto.setAsesorId(99L);
        dto.setEstudianteId(99L);
        dto.setMonitorId(99L);

        assertDoesNotThrow(() -> validator.validarSiTieneActividad(
                consulta,
                dto,
                true,
                false));
    }

    private Consulta consultaBase() {
        Consulta consulta = new Consulta();
        consulta.setId(1L);
        consulta.setPersona(persona(1L));
        consulta.setSede(sede(1L));
        consulta.setArea(area(1L));
        consulta.setTema(tema(1L));
        consulta.setTipo(tipo(1L));
        consulta.setAsesor(asesor(1L));
        consulta.setEstudiante(estudiante(1L));
        consulta.setMonitor(monitor(1L));
        consulta.setPartes(List.of(persona(2L), persona(3L)));
        consulta.setContrapartes(List.of(persona(4L), persona(5L)));
        return consulta;
    }

    private ConsultaDTO dtoBase() {
        ConsultaDTO dto = new ConsultaDTO();
        dto.setPersonaId(1L);
        dto.setSedeId(1L);
        dto.setAreaId(1L);
        dto.setTemaId(1L);
        dto.setTipoId(1L);
        dto.setAsesorId(1L);
        dto.setEstudianteId(1L);
        dto.setMonitorId(1L);
        dto.setPartesIds(List.of(2L, 3L));
        dto.setContrapartesIds(List.of(4L, 5L));
        return dto;
    }

    private Persona persona(Long id) {
        Persona persona = new Persona();
        persona.setId(id);
        return persona;
    }

    private Sede sede(Long id) {
        Sede sede = new Sede();
        sede.setId(id);
        return sede;
    }

    private Area area(Long id) {
        Area area = new Area();
        area.setId(id);
        return area;
    }

    private Tema tema(Long id) {
        Tema tema = new Tema();
        tema.setId(id);
        return tema;
    }

    private Tipo tipo(Long id) {
        Tipo tipo = new Tipo();
        tipo.setId(id);
        return tipo;
    }

    private Asesor asesor(Long id) {
        Asesor asesor = new Asesor();
        asesor.setId(id);
        return asesor;
    }

    private Estudiante estudiante(Long id) {
        Estudiante estudiante = new Estudiante();
        estudiante.setId(id);
        return estudiante;
    }

    private Monitor monitor(Long id) {
        Monitor monitor = new Monitor();
        monitor.setId(id);
        return monitor;
    }
}