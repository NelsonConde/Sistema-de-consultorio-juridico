package co.edu.ufps.legal_cases.business.service.persona.persona;

import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarNumeroDocumento;
import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarTelefono;
import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarTexto;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.dto.persona.PersonaDTO;
import co.edu.ufps.legal_cases.business.model.catalogo.Barrio;
import co.edu.ufps.legal_cases.business.model.catalogo.Municipio;
import co.edu.ufps.legal_cases.business.model.catalogo.Nacionalidad;
import co.edu.ufps.legal_cases.business.model.persona.Condicion;
import co.edu.ufps.legal_cases.business.model.persona.Empresa;
import co.edu.ufps.legal_cases.business.model.persona.Ocupacion;
import co.edu.ufps.legal_cases.business.model.persona.Persona;
import co.edu.ufps.legal_cases.business.model.persona.TipoPersona;

@Component
public class PersonaMapper {

    // Convierte la entidad a DTO para que el controller no exponga el modelo JPA.
    public PersonaDTO convertirADTO(Persona persona) {
        PersonaDTO dto = new PersonaDTO();

        dto.setId(persona.getId());

        // Relaciones: se expone solo el id para que el frontend pueda reconstruir los selects.
        dto.setTipoPersonaId(persona.getTipoPersona().getId());
        dto.setTipoDocumento(persona.getTipoDocumento());
        dto.setNumeroDocumento(persona.getNumeroDocumento());
        dto.setFechaExpedicion(persona.getFechaExpedicion());
        dto.setCiudadExpedicion(persona.getCiudadExpedicion());
        dto.setNombres(persona.getNombres());
        dto.setApellidos(persona.getApellidos());
        dto.setNombreIdentitario(persona.getNombreIdentitario());
        dto.setPronombre(persona.getPronombre());
        dto.setSexo(persona.getSexo());
        dto.setGenero(persona.getGenero());
        dto.setOrientacionSexual(persona.getOrientacionSexual());
        dto.setFechaNacimiento(persona.getFechaNacimiento());
        dto.setTelefono(persona.getTelefono());
        dto.setCorreo(persona.getCorreo());
        dto.setNacionalidadId(persona.getNacionalidad().getId());
        dto.setEstadoCivil(persona.getEstadoCivil());
        dto.setEscolaridad(persona.getEscolaridad());
        dto.setGrupoEtnico(persona.getGrupoEtnico());
        dto.setCondicionActualId(persona.getCondicionActual().getId());
        dto.setSabeLeerEscribir(persona.getSabeLeerEscribir());
        dto.setDiscapacidad(persona.getDiscapacidad());
        dto.setCaracterizacionPcd(persona.getCaracterizacionPcd());
        dto.setNecesitaAjustePcd(persona.getNecesitaAjustePcd());

        // Vivienda: se expone el id del municipio y barrio; el departamento se infiere del municipio.
        dto.setMunicipioId(persona.getMunicipio().getId());
        dto.setDepartamentoId(persona.getMunicipio().getDepartamento().getId());
        dto.setBarrioId(persona.getBarrio().getId());
        dto.setDireccion(persona.getDireccion());
        dto.setComuna(persona.getComuna());
        dto.setLocalidad(persona.getLocalidad());
        dto.setEstrato(persona.getEstrato());
        dto.setTipoVivienda(persona.getTipoVivienda());
        dto.setZona(persona.getZona());
        dto.setTenencia(persona.getTenencia());
        dto.setNumeroPersonasACargo(persona.getNumeroPersonasACargo());
        dto.setIngresosAdicionales(persona.getIngresosAdicionales());
        dto.setEnergiaElectrica(persona.getEnergiaElectrica());
        dto.setAcueducto(persona.getAcueducto());
        dto.setAlcantarillado(persona.getAlcantarillado());

        dto.setOcupacionId(persona.getOcupacion().getId());
        dto.setEmpresaId(persona.getEmpresa().getId());
        dto.setSalario(persona.getSalario());
        dto.setCargo(persona.getCargo());
        dto.setDireccionEmpresa(persona.getDireccionEmpresa());
        dto.setTelefonoEmpresa(persona.getTelefonoEmpresa());

        dto.setNombreCompletoAcudiente(persona.getNombreCompletoAcudiente());
        dto.setRelacionAcudiente(persona.getRelacionAcudiente());
        dto.setTelefonoAcudiente(persona.getTelefonoAcudiente());
        dto.setCorreoAcudiente(persona.getCorreoAcudiente());
        dto.setDireccionAcudiente(persona.getDireccionAcudiente());

        dto.setComoSeEntero(persona.getComoSeEntero());
        dto.setRelacionConUniversidad(persona.getRelacionConUniversidad());
        dto.setActivo(persona.getActivo());

        return dto;
    }

    public Persona crearEntidad(PersonaDTO dto, String numeroDocumento, DatosPersona datos) {
        Persona persona = new Persona();
        aplicarDatos(persona, dto, numeroDocumento, datos);
        return persona;
    }

    public void aplicarDatos(Persona persona, PersonaDTO dto, String numeroDocumento, DatosPersona datos) {
        persona.setTipoPersona(datos.tipoPersona());
        persona.setTipoDocumento(normalizarTexto(dto.getTipoDocumento()));
        persona.setNumeroDocumento(normalizarNumeroDocumento(numeroDocumento));
        persona.setFechaExpedicion(dto.getFechaExpedicion());
        persona.setCiudadExpedicion(normalizarTexto(dto.getCiudadExpedicion()));
        persona.setNombres(normalizarTexto(dto.getNombres()));
        persona.setApellidos(normalizarTexto(dto.getApellidos()));
        persona.setNombreIdentitario(normalizarTexto(dto.getNombreIdentitario()));
        persona.setPronombre(normalizarTexto(dto.getPronombre()));
        persona.setSexo(normalizarTexto(dto.getSexo()));
        persona.setGenero(normalizarTexto(dto.getGenero()));
        persona.setOrientacionSexual(normalizarTexto(dto.getOrientacionSexual()));
        persona.setFechaNacimiento(dto.getFechaNacimiento());
        persona.setTelefono(normalizarTelefono(dto.getTelefono()));
        persona.setCorreo(normalizarTexto(dto.getCorreo()));
        persona.setNacionalidad(datos.nacionalidad());
        persona.setEstadoCivil(normalizarTexto(dto.getEstadoCivil()));
        persona.setEscolaridad(normalizarTexto(dto.getEscolaridad()));
        persona.setGrupoEtnico(normalizarTexto(dto.getGrupoEtnico()));
        persona.setCondicionActual(datos.condicionActual());
        persona.setSabeLeerEscribir(dto.getSabeLeerEscribir());
        persona.setDiscapacidad(normalizarTexto(dto.getDiscapacidad()));
        persona.setCaracterizacionPcd(normalizarTexto(dto.getCaracterizacionPcd()));
        persona.setNecesitaAjustePcd(dto.getNecesitaAjustePcd());

        persona.setMunicipio(datos.municipio());
        persona.setBarrio(datos.barrio());
        persona.setDireccion(normalizarTexto(dto.getDireccion()));
        persona.setComuna(normalizarTexto(dto.getComuna()));
        persona.setLocalidad(normalizarTexto(dto.getLocalidad()));
        persona.setEstrato(dto.getEstrato());
        persona.setTipoVivienda(normalizarTexto(dto.getTipoVivienda()));
        persona.setZona(normalizarTexto(dto.getZona()));
        persona.setTenencia(normalizarTexto(dto.getTenencia()));
        persona.setNumeroPersonasACargo(dto.getNumeroPersonasACargo());
        persona.setIngresosAdicionales(dto.getIngresosAdicionales());
        persona.setEnergiaElectrica(dto.getEnergiaElectrica());
        persona.setAcueducto(dto.getAcueducto());
        persona.setAlcantarillado(dto.getAlcantarillado());

        persona.setOcupacion(datos.ocupacion());
        persona.setEmpresa(datos.empresa());
        persona.setSalario(dto.getSalario());
        persona.setCargo(normalizarTexto(dto.getCargo()));
        persona.setDireccionEmpresa(normalizarTexto(dto.getDireccionEmpresa()));
        persona.setTelefonoEmpresa(normalizarTelefono(dto.getTelefonoEmpresa()));

        persona.setNombreCompletoAcudiente(normalizarTexto(dto.getNombreCompletoAcudiente()));
        persona.setRelacionAcudiente(normalizarTexto(dto.getRelacionAcudiente()));
        persona.setTelefonoAcudiente(normalizarTelefono(dto.getTelefonoAcudiente()));
        persona.setCorreoAcudiente(normalizarTexto(dto.getCorreoAcudiente()));
        persona.setDireccionAcudiente(normalizarTexto(dto.getDireccionAcudiente()));

        persona.setComoSeEntero(normalizarTexto(dto.getComoSeEntero()));
        persona.setRelacionConUniversidad(normalizarTexto(dto.getRelacionConUniversidad()));

        // El estado activo no se actualiza desde el formulario principal.
        // Para eso existen los flujos separados de desactivar y reactivar.
    }

    // Record auxiliar que agrupa las relaciones ya cargadas y validadas.
    // Permite que el mapper reciba entidades listas en lugar de DTOs crudos.
    public record DatosPersona(
            TipoPersona tipoPersona,
            Nacionalidad nacionalidad,
            Condicion condicionActual,
            Municipio municipio,
            Barrio barrio,
            Ocupacion ocupacion,
            Empresa empresa) {
    }
}