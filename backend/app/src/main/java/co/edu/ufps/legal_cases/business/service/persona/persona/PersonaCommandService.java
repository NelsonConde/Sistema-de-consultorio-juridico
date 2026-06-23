package co.edu.ufps.legal_cases.business.service.persona.persona;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.audit.aop.log.Auditable;

import co.edu.ufps.legal_cases.business.dto.persona.PersonaDTO;
import co.edu.ufps.legal_cases.business.model.catalogo.Barrio;
import co.edu.ufps.legal_cases.business.model.catalogo.Municipio;
import co.edu.ufps.legal_cases.business.model.catalogo.Nacionalidad;
import co.edu.ufps.legal_cases.business.model.persona.Condicion;
import co.edu.ufps.legal_cases.business.model.persona.Empresa;
import co.edu.ufps.legal_cases.business.model.persona.Ocupacion;
import co.edu.ufps.legal_cases.business.model.persona.Persona;
import co.edu.ufps.legal_cases.business.model.persona.TipoPersona;
import co.edu.ufps.legal_cases.business.repository.catalogo.BarrioRepository;
import co.edu.ufps.legal_cases.business.repository.catalogo.MunicipioRepository;
import co.edu.ufps.legal_cases.business.repository.catalogo.NacionalidadRepository;
import co.edu.ufps.legal_cases.business.repository.persona.CondicionRepository;
import co.edu.ufps.legal_cases.business.repository.persona.EmpresaRepository;
import co.edu.ufps.legal_cases.business.repository.persona.OcupacionRepository;
import co.edu.ufps.legal_cases.business.repository.persona.PersonaRepository;
import co.edu.ufps.legal_cases.business.repository.persona.TipoPersonaRepository;
import co.edu.ufps.legal_cases.business.service.acceso.persona.PersonaAccessService;
import co.edu.ufps.legal_cases.business.service.persona.persona.PersonaMapper.DatosPersona;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

@Service
public class PersonaCommandService {

    private final PersonaRepository personaRepository;
    private final PersonaAccessService personaAccessService;
    private final PersonaMapper personaMapper;
    private final PersonaValidator personaValidator;

    // Repositorios de relaciones activas desde formulario.
    private final TipoPersonaRepository tipoPersonaRepository;
    private final NacionalidadRepository nacionalidadRepository;
    private final CondicionRepository condicionRepository;
    private final MunicipioRepository municipioRepository;
    private final BarrioRepository barrioRepository;
    private final OcupacionRepository ocupacionRepository;
    private final EmpresaRepository empresaRepository;

    public PersonaCommandService(
            PersonaRepository personaRepository,
            PersonaAccessService personaAccessService,
            PersonaMapper personaMapper,
            PersonaValidator personaValidator,
            TipoPersonaRepository tipoPersonaRepository,
            NacionalidadRepository nacionalidadRepository,
            CondicionRepository condicionRepository,
            MunicipioRepository municipioRepository,
            BarrioRepository barrioRepository,
            OcupacionRepository ocupacionRepository,
            EmpresaRepository empresaRepository) {
        this.personaRepository = personaRepository;
        this.personaAccessService = personaAccessService;
        this.personaMapper = personaMapper;
        this.personaValidator = personaValidator;
        this.tipoPersonaRepository = tipoPersonaRepository;
        this.nacionalidadRepository = nacionalidadRepository;
        this.condicionRepository = condicionRepository;
        this.municipioRepository = municipioRepository;
        this.barrioRepository = barrioRepository;
        this.ocupacionRepository = ocupacionRepository;
        this.empresaRepository = empresaRepository;
    }

    @Transactional
    @Auditable(action = "REGISTRAR_PERSONA", entityName = "Persona")
    public PersonaDTO crear(PersonaDTO dto) {
        personaAccessService.validarPuedeCrearPersonas();
        personaValidator.validarCreacion(dto);

        String numeroDocumento = personaValidator.normalizarDocumento(dto.getNumeroDocumento());
        personaValidator.validarDocumentoDisponible(numeroDocumento);

        DatosPersona datos = cargarRelaciones(dto);
        personaValidator.validarBarrioPerteneceAMunicipio(datos.barrio(), datos.municipio());

        Persona persona = personaMapper.crearEntidad(dto, numeroDocumento, datos);

        return personaMapper.convertirADTO(personaRepository.save(persona));
    }

    @Transactional
    @Auditable(action = "ACTUALIZAR_PERSONA", entityName = "Persona")
    public PersonaDTO actualizar(Long id, PersonaDTO dto) {
        personaAccessService.validarPuedeEditarPersonas();
        personaValidator.validarActualizacion(id, dto);

        Persona persona = buscarPorId(id);
        String numeroDocumentoNuevo = personaValidator.normalizarDocumento(dto.getNumeroDocumento());
        personaValidator.validarDocumentoDisponibleParaActualizacion(persona, numeroDocumentoNuevo);

        DatosPersona datos = cargarRelaciones(dto);
        personaValidator.validarBarrioPerteneceAMunicipio(datos.barrio(), datos.municipio());

        // Se actualiza sobre la entidad existente para no tocar campos de control como activo.
        personaMapper.aplicarDatos(persona, dto, numeroDocumentoNuevo, datos);

        return personaMapper.convertirADTO(personaRepository.save(persona));
    }

    @Transactional
    @Auditable(action = "DESACTIVAR_PERSONA", entityName = "Persona")
    public void desactivar(Long id) {
        personaAccessService.validarPuedeCambiarEstadoPersonas();

        Persona persona = buscarPorId(id);
        persona.setActivo(false);
        personaRepository.save(persona);
    }

    @Transactional
    @Auditable(action = "REACTIVAR_PERSONA", entityName = "Persona")
    public void reactivar(Long id) {
        personaAccessService.validarPuedeCambiarEstadoPersonas();

        Persona persona = buscarPorId(id);
        persona.setActivo(true);
        personaRepository.save(persona);
    }

    // Carga todas las relaciones activas desde formulario en un solo lugar.
    // Las relaciones deben estar activas porque vienen de selects del formulario.
    private DatosPersona cargarRelaciones(PersonaDTO dto) {
        TipoPersona tipoPersona = obtenerActivo(
                tipoPersonaRepository.findByIdAndActivoTrue(dto.getTipoPersonaId()),
                "Tipo de persona", dto.getTipoPersonaId());

        Nacionalidad nacionalidad = obtenerActivo(
                nacionalidadRepository.findByIdAndActivoTrue(dto.getNacionalidadId()),
                "Nacionalidad", dto.getNacionalidadId());

        Condicion condicionActual = obtenerActivo(
                condicionRepository.findByIdAndActivoTrue(dto.getCondicionActualId()),
                "Condición actual", dto.getCondicionActualId());

        Municipio municipio = obtenerActivo(
                municipioRepository.findByIdAndActivoTrue(dto.getMunicipioId()),
                "Municipio", dto.getMunicipioId());

        Barrio barrio = obtenerActivo(
                barrioRepository.findByIdAndActivoTrue(dto.getBarrioId()),
                "Barrio", dto.getBarrioId());

        Ocupacion ocupacion = obtenerActivo(
                ocupacionRepository.findByIdAndActivoTrue(dto.getOcupacionId()),
                "Ocupación", dto.getOcupacionId());

        Empresa empresa = obtenerActivo(
                empresaRepository.findByIdAndActivoTrue(dto.getEmpresaId()),
                "Empresa", dto.getEmpresaId());

        return new DatosPersona(tipoPersona, nacionalidad, condicionActual, municipio, barrio, ocupacion, empresa);
    }

    private <T> T obtenerActivo(java.util.Optional<T> resultado, String entidad, Long id) {
        if (id == null) {
            throw new BusinessException(entidad + " es obligatoria");
        }
        return resultado.orElseThrow(
                () -> new BusinessException(entidad + " no encontrada o inactiva con id: " + id));
    }

    private Persona buscarPorId(Long id) {
        if (id == null) {
            throw new BusinessException("El id de la persona es obligatorio");
        }

        return personaRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Persona no encontrada con id: " + id));
    }
}