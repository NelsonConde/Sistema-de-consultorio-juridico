package co.edu.ufps.legal_cases.business.service.perfil.asesor;

import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarCodigo;
import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarEmail;
import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarNumeroDocumento;
import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarTelefono;
import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarTexto;
import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarUsuario;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.audit.aop.log.Auditable;
import co.edu.ufps.legal_cases.business.dto.perfil.AsesorDTO;
import co.edu.ufps.legal_cases.business.model.catalogo.Area;
import co.edu.ufps.legal_cases.business.model.catalogo.Sede;
import co.edu.ufps.legal_cases.business.model.catalogo.TipoDocumento;
import co.edu.ufps.legal_cases.business.model.perfil.Asesor;
import co.edu.ufps.legal_cases.business.repository.catalogo.AreaRepository;
import co.edu.ufps.legal_cases.business.repository.catalogo.SedeRepository;
import co.edu.ufps.legal_cases.business.repository.catalogo.TipoDocumentoRepository;
import co.edu.ufps.legal_cases.business.repository.perfil.AsesorRepository;
import co.edu.ufps.legal_cases.business.service.acceso.perfil.AsesorMonitorAccessService;
import co.edu.ufps.legal_cases.business.service.consulta.consulta.ConsultaResponsableOperacionService;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.service.account.usuario.UsuarioSistemaPerfilEstadoService;
import co.edu.ufps.legal_cases.security.service.account.usuario.UsuarioSistemaRegistroService;

@Service
public class AsesorCommandService {

    private final AsesorRepository asesorRepository;
    private final TipoDocumentoRepository tipoDocumentoRepository;
    private final SedeRepository sedeRepository;
    private final AreaRepository areaRepository;
    private final UsuarioSistemaRegistroService usuarioSistemaRegistroService;
    private final UsuarioSistemaPerfilEstadoService usuarioSistemaPerfilEstadoService;
    private final AsesorMonitorAccessService asesorMonitorAccessService;
    private final AsesorValidator asesorValidator;
    private final AsesorMapper asesorMapper;
    private final ConsultaResponsableOperacionService consultaResponsableOperacionService;

    public AsesorCommandService(
            AsesorRepository asesorRepository,
            TipoDocumentoRepository tipoDocumentoRepository,
            SedeRepository sedeRepository,
            AreaRepository areaRepository,
            UsuarioSistemaRegistroService usuarioSistemaRegistroService,
            UsuarioSistemaPerfilEstadoService usuarioSistemaPerfilEstadoService,
            AsesorMonitorAccessService asesorMonitorAccessService,
            AsesorValidator asesorValidator,
            AsesorMapper asesorMapper,
            ConsultaResponsableOperacionService consultaResponsableOperacionService) {
        this.asesorRepository = asesorRepository;
        this.tipoDocumentoRepository = tipoDocumentoRepository;
        this.sedeRepository = sedeRepository;
        this.areaRepository = areaRepository;
        this.usuarioSistemaRegistroService = usuarioSistemaRegistroService;
        this.usuarioSistemaPerfilEstadoService = usuarioSistemaPerfilEstadoService;
        this.asesorMonitorAccessService = asesorMonitorAccessService;
        this.asesorValidator = asesorValidator;
        this.asesorMapper = asesorMapper;
        this.consultaResponsableOperacionService = consultaResponsableOperacionService;
    }

    @Transactional
    @Auditable(action = "CREAR_PERFIL", entityName = "Asesor")
    public AsesorDTO crear(AsesorDTO dto) {
        asesorMonitorAccessService.validarPuedeGestionarAsesoresYMonitores();
        asesorValidator.validarIdNoEnviadoEnCreacion(dto.getId());

        DatosAsesor datos = prepararDatos(dto, true);

        asesorValidator.validarDuplicadosCreacion(
                datos.documento(),
                datos.email(),
                datos.telefono(),
                datos.usuario(),
                datos.codigo());

        Asesor asesor = new Asesor();
        asesorMapper.aplicarDatos(asesor, datos);

        Asesor asesorGuardado = asesorRepository.save(asesor);

        // El perfil se guarda primero porque el usuario del sistema se crea con datos
        // del asesor persistido.
        UsuarioSistema usuarioSistema = usuarioSistemaRegistroService.crearParaAsesor(asesorGuardado);

        // El perfil real queda apuntando al usuario del sistema normalizado.
        asesorGuardado.setUsuarioSistema(usuarioSistema);

        return asesorMapper.convertirADTO(asesorRepository.save(asesorGuardado));
    }

    @Transactional
    @Auditable(action = "ACTUALIZAR_PERFIL", entityName = "Asesor")
    public AsesorDTO actualizar(Long id, AsesorDTO dto) {
        asesorMonitorAccessService.validarPuedeGestionarAsesoresYMonitores();

        Asesor existente = buscarPorId(id);

        asesorValidator.validarIdNoCambiado(id, dto.getId());

        // Actualizar datos del perfil no debe cambiar el estado activo.
        // Para eso existe cambiarEstado().
        DatosAsesor datos = prepararDatos(dto, existente.getActivo());

        asesorValidator.validarDuplicadosActualizacion(
                id,
                datos.documento(),
                datos.email(),
                datos.telefono(),
                datos.usuario(),
                datos.codigo());

        asesorValidator.validarExistenCambios(existente, datos);

        asesorMapper.aplicarDatos(existente, datos);

        return asesorMapper.convertirADTO(asesorRepository.save(existente));
    }

    @Transactional
    @Auditable(action = "DESACTIVAR/REACTIVAR_PERFIL", entityName = "Asesor")
    public AsesorDTO cambiarEstado(Long id, Boolean activo) {
        asesorMonitorAccessService.validarPuedeGestionarAsesoresYMonitores();

        Asesor asesor = buscarPorId(id);

        asesorValidator.validarCambioEstado(asesor, activo);

        if (Boolean.FALSE.equals(activo)) {
            consultaResponsableOperacionService.validarAsesorSinConsultasOperativas(id);
        }

        asesor.setActivo(activo);

        Asesor guardado = asesorRepository.save(asesor);

        // El estado operativo del perfil y del usuario de acceso deben permanecer
        // sincronizados.
        usuarioSistemaPerfilEstadoService.sincronizarEstadoSiExiste(
                guardado.getUsuarioSistema(),
                activo);

        return asesorMapper.convertirADTO(guardado);
    }

    @Transactional
    @Auditable(action = "ELIMINAR_PERFIL", entityName = "Asesor")
    public void eliminar(Long id) {
        asesorMonitorAccessService.validarPuedeGestionarAsesoresYMonitores();

        Asesor asesor = buscarPorId(id);

        // Se conserva el perfil y se desactiva porque puede estar asociado a
        // estudiantes o consultas.
        asesorValidator.validarCambioEstado(asesor, false);
        consultaResponsableOperacionService.validarAsesorSinConsultasOperativas(id);

        asesor.setActivo(false);

        Asesor guardado = asesorRepository.save(asesor);

        usuarioSistemaPerfilEstadoService.sincronizarEstadoSiExiste(
                guardado.getUsuarioSistema(),
                false);
    }

    private DatosAsesor prepararDatos(AsesorDTO dto, Boolean activo) {
        String nombre = normalizarTexto(dto.getNombre());
        String documento = normalizarNumeroDocumento(dto.getDocumento());
        String email = normalizarEmail(dto.getEmail());
        String telefono = normalizarTelefono(dto.getTelefono());
        String usuario = normalizarUsuario(dto.getUsuario());
        String codigo = normalizarCodigo(dto.getCodigo());

        asesorValidator.validarCamposObligatorios(
                nombre,
                documento,
                email,
                telefono,
                usuario,
                codigo);

        TipoDocumento tipoDocumento = obtenerTipoDocumento(dto.getTipoDocumentoId());
        Sede sede = obtenerSede(dto.getSedeId());
        Area area = obtenerArea(dto.getAreaId());

        return new DatosAsesor(
                nombre,
                documento,
                email,
                telefono,
                usuario,
                codigo,
                tipoDocumento,
                sede,
                area,
                activo);
    }

    private Asesor buscarPorId(Long id) {
        if (id == null) {
            throw new BusinessException("El id del asesor es obligatorio");
        }

        return asesorRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Asesor no encontrado con id: " + id));
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

    private Area obtenerArea(Long areaId) {
        if (areaId == null) {
            throw new BusinessException("El área es obligatoria");
        }

        return areaRepository.findByIdAndActivoTrue(areaId)
                .orElseThrow(() -> new BusinessException("Área no encontrada o inactiva con id: " + areaId));
    }
}