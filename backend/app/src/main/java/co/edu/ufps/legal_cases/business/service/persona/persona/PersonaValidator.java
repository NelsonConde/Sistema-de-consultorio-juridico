package co.edu.ufps.legal_cases.business.service.persona.persona;

import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.estaInformado;
import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarNumeroDocumento;

import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.dto.persona.PersonaDTO;
import co.edu.ufps.legal_cases.business.model.catalogo.Barrio;
import co.edu.ufps.legal_cases.business.model.catalogo.Municipio;
import co.edu.ufps.legal_cases.business.model.persona.Persona;
import co.edu.ufps.legal_cases.business.repository.persona.PersonaRepository;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

@Component
public class PersonaValidator {

    private final PersonaRepository personaRepository;

    public PersonaValidator(PersonaRepository personaRepository) {
        this.personaRepository = personaRepository;
    }

    public void validarCreacion(PersonaDTO dto) {
        validarDtoObligatorio(dto);

        if (dto.getId() != null) {
            throw new BusinessException("El id no debe enviarse en la creación");
        }

        validarTelefonoOCorreo(dto);
        validarDatosAcudienteSiEsMenor(dto);
    }

    public void validarActualizacion(Long id, PersonaDTO dto) {
        validarDtoObligatorio(dto);

        if (dto.getId() != null && !Objects.equals(dto.getId(), id)) {
            throw new BusinessException("No se permite cambiar el id de la persona");
        }

        validarTelefonoOCorreo(dto);
        validarDatosAcudienteSiEsMenor(dto);
    }

    public String normalizarDocumento(String numeroDocumento) {
        String numeroDocumentoNormalizado = normalizarNumeroDocumento(numeroDocumento);

        if (numeroDocumentoNormalizado == null || numeroDocumentoNormalizado.isBlank()) {
            throw new BusinessException("El número de documento es obligatorio");
        }

        return numeroDocumentoNormalizado;
    }

    public void validarDocumentoDisponible(String numeroDocumento) {
        if (personaRepository.existsByNumeroDocumento(numeroDocumento)) {
            throw new BusinessException("Ya existe una persona con ese número de documento");
        }
    }

    public void validarDocumentoDisponibleParaActualizacion(
            Persona personaExistente,
            String numeroDocumentoNuevo) {

        String numeroDocumentoActual = personaExistente.getNumeroDocumento();

        if (!numeroDocumentoActual.equalsIgnoreCase(numeroDocumentoNuevo)
                && personaRepository.existsByNumeroDocumento(numeroDocumentoNuevo)) {
            throw new BusinessException("Ya existe una persona con ese número de documento");
        }
    }

    // Valida que el barrio seleccionado pertenezca al municipio seleccionado.
    // Evita inconsistencias cuando el frontend no filtra correctamente el combo de barrios.
    public void validarBarrioPerteneceAMunicipio(Barrio barrio, Municipio municipio) {
        if (barrio.getMunicipio() == null
                || !Objects.equals(barrio.getMunicipio().getId(), municipio.getId())) {
            throw new BusinessException(
                    "El barrio seleccionado no pertenece al municipio seleccionado");
        }
    }

    private void validarDtoObligatorio(PersonaDTO dto) {
        if (dto == null) {
            throw new BusinessException("Los datos de la persona son obligatorios");
        }
    }

    private boolean esMenorDeEdad(LocalDate fechaNacimiento) {
        if (fechaNacimiento == null) {
            return false;
        }

        return Period.between(fechaNacimiento, LocalDate.now()).getYears() < 18;
    }

    private void validarDatosAcudienteSiEsMenor(PersonaDTO dto) {
        if (!esMenorDeEdad(dto.getFechaNacimiento())) {
            return;
        }

        if (!estaInformado(dto.getNombreCompletoAcudiente())) {
            throw new BusinessException("Si la persona es menor de edad, el nombre del acudiente es obligatorio");
        }

        if (!estaInformado(dto.getRelacionAcudiente())) {
            throw new BusinessException("Si la persona es menor de edad, la relación del acudiente es obligatoria");
        }

        boolean telefonoAcudienteInformado = estaInformado(dto.getTelefonoAcudiente());
        boolean correoAcudienteInformado = estaInformado(dto.getCorreoAcudiente());

        if (!telefonoAcudienteInformado && !correoAcudienteInformado) {
            throw new BusinessException(
                    "Si la persona es menor de edad, debe informar teléfono o correo del acudiente");
        }
    }

    private void validarTelefonoOCorreo(PersonaDTO dto) {
        boolean telefonoInformado = estaInformado(dto.getTelefono());
        boolean correoInformado = estaInformado(dto.getCorreo());

        // El frontend puede enviar textos como "No informa"; esta utilidad los trata como no informados.
        if (!telefonoInformado && !correoInformado) {
            throw new BusinessException("Debe informar al menos teléfono o correo");
        }
    }
}