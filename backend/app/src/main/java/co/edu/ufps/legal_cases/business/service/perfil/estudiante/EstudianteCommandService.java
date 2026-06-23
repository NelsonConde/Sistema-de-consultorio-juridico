package co.edu.ufps.legal_cases.business.service.perfil.estudiante;

import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarCodigo;
import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarEmail;
import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarNumeroDocumento;
import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarTelefono;
import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarTexto;
import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarUsuario;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.audit.aop.log.Auditable;

import co.edu.ufps.legal_cases.business.dto.perfil.EstudianteDTO;
import co.edu.ufps.legal_cases.business.model.catalogo.Sede;
import co.edu.ufps.legal_cases.business.model.catalogo.TipoDocumento;
import co.edu.ufps.legal_cases.business.model.perfil.Asesor;
import co.edu.ufps.legal_cases.business.model.perfil.Estudiante;
import co.edu.ufps.legal_cases.business.repository.catalogo.SedeRepository;
import co.edu.ufps.legal_cases.business.repository.catalogo.TipoDocumentoRepository;
import co.edu.ufps.legal_cases.business.repository.perfil.AsesorRepository;
import co.edu.ufps.legal_cases.business.repository.perfil.EstudianteRepository;
import co.edu.ufps.legal_cases.business.service.acceso.perfil.EstudianteAccessService;
import co.edu.ufps.legal_cases.business.service.consulta.consulta.ConsultaResponsableOperacionService;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.service.account.usuario.UsuarioSistemaPerfilEstadoService;
import co.edu.ufps.legal_cases.security.service.account.usuario.UsuarioSistemaRegistroService;

@Service
public class EstudianteCommandService {

    private final EstudianteRepository estudianteRepository;
    private final TipoDocumentoRepository tipoDocumentoRepository;
    private final SedeRepository sedeRepository;
    private final AsesorRepository asesorRepository;
    private final UsuarioSistemaRegistroService usuarioSistemaRegistroService;
    private final UsuarioSistemaPerfilEstadoService usuarioSistemaPerfilEstadoService;
    private final EstudianteAccessService estudianteAccessService;
    private final EstudianteValidator estudianteValidator;
    private final EstudianteMapper estudianteMapper;
    private final ConsultaResponsableOperacionService consultaResponsableOperacionService;

    public EstudianteCommandService(
            EstudianteRepository estudianteRepository,
            TipoDocumentoRepository tipoDocumentoRepository,
            SedeRepository sedeRepository,
            AsesorRepository asesorRepository,
            UsuarioSistemaRegistroService usuarioSistemaRegistroService,
            UsuarioSistemaPerfilEstadoService usuarioSistemaPerfilEstadoService,
            EstudianteAccessService estudianteAccessService,
            EstudianteValidator estudianteValidator,
            EstudianteMapper estudianteMapper,
            ConsultaResponsableOperacionService consultaResponsableOperacionService) {
        this.estudianteRepository = estudianteRepository;
        this.tipoDocumentoRepository = tipoDocumentoRepository;
        this.sedeRepository = sedeRepository;
        this.asesorRepository = asesorRepository;
        this.usuarioSistemaRegistroService = usuarioSistemaRegistroService;
        this.usuarioSistemaPerfilEstadoService = usuarioSistemaPerfilEstadoService;
        this.estudianteAccessService = estudianteAccessService;
        this.estudianteValidator = estudianteValidator;
        this.estudianteMapper = estudianteMapper;
        this.consultaResponsableOperacionService = consultaResponsableOperacionService;
    }

    @Transactional
    @Auditable(action = "CREAR_PERFIL", entityName = "Estudiante")
    public EstudianteDTO crear(EstudianteDTO dto) {
        estudianteAccessService.validarPuedeGestionarEstudiantes();
        estudianteValidator.validarIdNoEnviadoEnCreacion(dto.getId());

        DatosEstudiante datos = prepararDatos(dto, true, false);

        estudianteValidator.validarDuplicadosCreacion(
                datos.documento(),
                datos.email(),
                datos.telefono(),
                datos.usuario(),
                datos.codigo());

        Estudiante estudiante = new Estudiante();
        estudianteMapper.aplicarDatos(estudiante, datos);

        Estudiante estudianteGuardado = estudianteRepository.save(estudiante);

        // El perfil se guarda primero porque el usuario del sistema se crea con datos
        // del estudiante persistido.
        UsuarioSistema usuarioSistema = usuarioSistemaRegistroService.crearParaEstudiante(estudianteGuardado);

        estudianteGuardado.setUsuarioSistema(usuarioSistema);

        return estudianteMapper.convertirADTO(estudianteRepository.save(estudianteGuardado));
    }

    @Transactional
    @Auditable(action = "ACTUALIZAR_PERFIL", entityName = "Estudiante")
    public EstudianteDTO actualizar(Long id, EstudianteDTO dto) {
        estudianteAccessService.validarPuedeGestionarEstudiantes();

        Estudiante existente = buscarPorId(id);

        estudianteValidator.validarIdNoCambiado(id, dto.getId());

        // Actualizar datos del perfil no debe cambiar activo ni conciliación.
        // Para esos campos existen cambiarEstado() y cambiarConciliacion().
        DatosEstudiante datos = prepararDatos(
                dto,
                existente.getActivo(),
                existente.getConciliacion());

        estudianteValidator.validarDuplicadosActualizacion(
                id,
                datos.documento(),
                datos.email(),
                datos.telefono(),
                datos.usuario(),
                datos.codigo());

        estudianteValidator.validarExistenCambios(existente, datos);

        estudianteMapper.aplicarDatos(existente, datos);

        return estudianteMapper.convertirADTO(estudianteRepository.save(existente));
    }

    @Transactional
    @Auditable(action = "DESACTIVAR/REACTIVAR_PERFIL", entityName = "Estudiante")
    public EstudianteDTO cambiarEstado(Long id, Boolean activo) {
        estudianteAccessService.validarPuedeCambiarEstadoEstudiante();

        Estudiante estudiante = buscarPorId(id);

        estudianteValidator.validarCambioEstado(estudiante, activo);

        if (Boolean.FALSE.equals(activo)) {
            consultaResponsableOperacionService.validarEstudianteSinConsultasOperativas(id);
        }

        estudiante.setActivo(activo);

        Estudiante guardado = estudianteRepository.save(estudiante);

        // El estado operativo del perfil y del usuario de acceso deben permanecer
        // sincronizados.
        usuarioSistemaPerfilEstadoService.sincronizarEstadoSiExiste(
                guardado.getUsuarioSistema(),
                activo);

        return estudianteMapper.convertirADTO(guardado);
    }

    @Transactional
    @Auditable(action = "HABILITAR_CONCILIACION", entityName = "Estudiante")
    public EstudianteDTO cambiarConciliacion(Long id, Boolean conciliacion) {
        estudianteAccessService.validarPuedeGestionarEstudiantes();

        Estudiante estudiante = buscarPorId(id);

        estudianteValidator.validarCambioConciliacion(estudiante, conciliacion);

        estudiante.setConciliacion(conciliacion);

        return estudianteMapper.convertirADTO(estudianteRepository.save(estudiante));
    }

    @Transactional
    @Auditable(action = "ELIMINAR_PERFIL", entityName = "Estudiante")
    public void eliminar(Long id) {
        estudianteAccessService.validarPuedeGestionarEstudiantes();

        Estudiante estudiante = buscarPorId(id);

        // Se conserva el perfil y se desactiva porque puede estar asociado a consultas
        // y seguimientos.
        estudianteValidator.validarCambioEstado(estudiante, false);
        consultaResponsableOperacionService.validarEstudianteSinConsultasOperativas(id);

        estudiante.setActivo(false);

        Estudiante guardado = estudianteRepository.save(estudiante);

        usuarioSistemaPerfilEstadoService.sincronizarEstadoSiExiste(
                guardado.getUsuarioSistema(),
                false);
    }

    private DatosEstudiante prepararDatos(
            EstudianteDTO dto,
            Boolean activo,
            Boolean conciliacion) {
        String nombre = normalizarTexto(dto.getNombre());
        String documento = normalizarNumeroDocumento(dto.getDocumento());
        String email = normalizarEmail(dto.getEmail());
        String telefono = normalizarTelefono(dto.getTelefono());
        String usuario = normalizarUsuario(dto.getUsuario());
        String codigo = normalizarCodigo(dto.getCodigo());

        estudianteValidator.validarCamposObligatorios(
                nombre,
                documento,
                email,
                telefono,
                usuario,
                codigo);

        TipoDocumento tipoDocumento = obtenerTipoDocumento(dto.getTipoDocumentoId());
        Sede sede = obtenerSede(dto.getSedeId());
        Asesor asesor = obtenerAsesor(dto.getAsesorId());

        return new DatosEstudiante(
                nombre,
                documento,
                email,
                telefono,
                usuario,
                codigo,
                tipoDocumento,
                sede,
                asesor,
                activo,
                conciliacion);
    }

    private Estudiante buscarPorId(Long id) {
        if (id == null) {
            throw new BusinessException("El id del estudiante es obligatorio");
        }

        return estudianteRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Estudiante no encontrado con id: " + id));
    }

    private TipoDocumento obtenerTipoDocumento(Long tipoDocumentoId) {
        if (tipoDocumentoId == null) {
            throw new BusinessException("El tipo de documento es obligatorio");
        }

        return tipoDocumentoRepository.findByIdAndActivoTrue(tipoDocumentoId)
                .orElseThrow(() -> new BusinessException(
                        "Tipo de documento no encontrado o inactivo con id: " + tipoDocumentoId));
    }

    private Sede obtenerSede(Long sedeId) {
        if (sedeId == null) {
            throw new BusinessException("La sede es obligatoria");
        }

        return sedeRepository.findByIdAndActivoTrue(sedeId)
                .orElseThrow(() -> new BusinessException("Sede no encontrada o inactiva con id: " + sedeId));
    }

    private Asesor obtenerAsesor(Long asesorId) {
        if (asesorId == null) {
            throw new BusinessException("El asesor es obligatorio");
        }

        return asesorRepository.findByIdAndActivoTrue(asesorId)
                .orElseThrow(() -> new BusinessException("Asesor no encontrado o inactivo con id: " + asesorId));
    }
}