package co.edu.ufps.legal_cases.business.dto.consulta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import co.edu.ufps.legal_cases.business.model.consulta.TipoResponsableConsulta;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class ConsultaReasignacionDTOTest {

    @Test
    void exigeUnUnicoRolResponsableIdMotivoYVersionValidos() {
        ConsultaReasignacionDTO solicitud = new ConsultaReasignacionDTO();
        solicitud.setNuevoResponsableId(0L);
        solicitud.setMotivo("   ");
        solicitud.setVersion(-1L);

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            Set<ConstraintViolation<ConsultaReasignacionDTO>> errores = validator.validate(solicitud);

            assertEquals(4, errores.size());
            assertTrue(errores.stream().anyMatch(error ->
                    error.getPropertyPath().toString().equals("tipoResponsable")));
            assertTrue(errores.stream().anyMatch(error ->
                    error.getPropertyPath().toString().equals("nuevoResponsableId")));
            assertTrue(errores.stream().anyMatch(error ->
                    error.getPropertyPath().toString().equals("motivo")));
            assertTrue(errores.stream().anyMatch(error ->
                    error.getPropertyPath().toString().equals("version")));
        }
    }

    @Test
    void aceptaSolicitudValida() {
        ConsultaReasignacionDTO solicitud = new ConsultaReasignacionDTO();
        solicitud.setTipoResponsable(TipoResponsableConsulta.ESTUDIANTE);
        solicitud.setNuevoResponsableId(20L);
        solicitud.setMotivo("Rotacion de consultorio");
        solicitud.setVersion(0L);

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertTrue(factory.getValidator().validate(solicitud).isEmpty());
        }
    }
}
