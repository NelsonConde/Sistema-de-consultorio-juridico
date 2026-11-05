package co.edu.ufps.legal_cases.business.service.perfil.monitor;

import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarCodigo;
import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarEmail;
import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarNumeroDocumento;
import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarTelefono;
import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarTexto;
import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarUsuario;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.audit.aop.log.Auditable;

import co.edu.ufps.legal_cases.business.dto.perfil.MonitorDTO;
import co.edu.ufps.legal_cases.business.model.catalogo.Sede;
import co.edu.ufps.legal_cases.business.model.catalogo.TipoDocumento;
import co.edu.ufps.legal_cases.business.model.perfil.Monitor;
import co.edu.ufps.legal_cases.business.repository.catalogo.SedeRepository;
import co.edu.ufps.legal_cases.business.repository.catalogo.TipoDocumentoRepository;
import co.edu.ufps.legal_cases.business.repository.perfil.MonitorRepository;
import co.edu.ufps.legal_cases.business.service.acceso.perfil.AsesorMonitorAccessService;
import co.edu.ufps.legal_cases.business.service.consulta.consulta.ConsultaResponsableOperacionService;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.repository.account.UsuarioSistemaRepository;
import co.edu.ufps.legal_cases.security.service.account.perfil.OperacionEstadoPerfil;
import co.edu.ufps.legal_cases.security.service.account.perfil.PerfilOperacionEstadoValidator;
import co.edu.ufps.legal_cases.security.service.account.usuario.UsuarioSistemaRegistroService;

@Service
public class MonitorCommandService {

    private final MonitorRepository monitorRepository;
    private final TipoDocumentoRepository tipoDocumentoRepository;
    private final SedeRepository sedeRepository;
    private final UsuarioSistemaRegistroService usuarioSistemaRegistroService;
    private final UsuarioSistemaRepository usuarioSistemaRepository;
    private final PerfilOperacionEstadoValidator perfilOperacionEstadoValidator;
    private final AsesorMonitorAccessService asesorMonitorAccessService;
    private final MonitorValidator monitorValidator;
    private final MonitorMapper monitorMapper;
    private final ConsultaResponsableOperacionService consultaResponsableOperacionService;

    public MonitorCommandService(
            MonitorRepository monitorRepository,
            TipoDocumentoRepository tipoDocumentoRepository,
            SedeRepository sedeRepository,
            UsuarioSistemaRegistroService usuarioSistemaRegistroService,
            UsuarioSistemaRepository usuarioSistemaRepository,
            PerfilOperacionEstadoValidator perfilOperacionEstadoValidator,
            AsesorMonitorAccessService asesorMonitorAccessService,
            MonitorValidator monitorValidator,
            MonitorMapper monitorMapper,
            ConsultaResponsableOperacionService consultaResponsableOperacionService) {
        this.monitorRepository = monitorRepository;
        this.tipoDocumentoRepository = tipoDocumentoRepository;
        this.sedeRepository = sedeRepository;
        this.usuarioSistemaRegistroService = usuarioSistemaRegistroService;
        this.usuarioSistemaRepository = usuarioSistemaRepository;
        this.perfilOperacionEstadoValidator = perfilOperacionEstadoValidator;
        this.asesorMonitorAccessService = asesorMonitorAccessService;
        this.monitorValidator = monitorValidator;
        this.monitorMapper = monitorMapper;
        this.consultaResponsableOperacionService = consultaResponsableOperacionService;
    }

    @Transactional
    @Auditable(action = "CREAR_PERFIL", entityName = "Monitor", entityId = "#result.id")
    public MonitorDTO crear(MonitorDTO dto) {
        asesorMonitorAccessService.validarPuedeGestionarAsesoresYMonitores();
        monitorValidator.validarIdNoEnviadoEnCreacion(dto.getId());

        DatosMonitor datos = prepararDatos(dto, true);

        monitorValidator.validarDuplicadosCreacion(
                datos.documento(),
                datos.email(),
                datos.telefono(),
                datos.usuario(),
                datos.codigo());

        Monitor monitor = new Monitor();
        monitorMapper.aplicarDatos(monitor, datos);

        Monitor monitorGuardado = monitorRepository.save(monitor);

        // El perfil se guarda primero porque el usuario del sistema se crea con datos
        // del monitor persistido.
        UsuarioSistema usuarioSistema = usuarioSistemaRegistroService.crearParaMonitor(monitorGuardado);

        // El perfil real queda apuntando al usuario del sistema normalizado.
        monitorGuardado.setUsuarioSistema(usuarioSistema);

        return monitorMapper.convertirADTO(monitorRepository.save(monitorGuardado));
    }

    @Transactional
    @Auditable(action = "ACTUALIZAR_PERFIL", entityName = "Monitor", entityId = "#id")
    public MonitorDTO actualizar(Long id, MonitorDTO dto) {
        asesorMonitorAccessService.validarPuedeGestionarAsesoresYMonitores();

        Monitor existente = buscarPorId(id);

        monitorValidator.validarIdNoCambiado(id, dto.getId());

        // Actualizar datos del perfil no debe cambiar el estado activo.
        // Para eso existe cambiarEstado().
        DatosMonitor datos = prepararDatos(dto, existente.getActivo());

        monitorValidator.validarDuplicadosActualizacion(
                id,
                datos.documento(),
                datos.email(),
                datos.telefono(),
                datos.usuario(),
                datos.codigo());

        monitorValidator.validarExistenCambios(existente, datos);

        monitorMapper.aplicarDatos(existente, datos);

        return monitorMapper.convertirADTO(monitorRepository.save(existente));
    }

    @Transactional
    @Auditable(
            action = "CAMBIAR_ESTADO_PERFIL",
            entityName = "Monitor",
            entityId = "#id",
            trackedFields = "activo",
            metadata = "requestedActive=#activo")
    public MonitorDTO cambiarEstado(Long id, Boolean activo) {
        asesorMonitorAccessService.validarPuedeGestionarAsesoresYMonitores();

        Monitor monitor = buscarPorIdConUsuarioBloqueado(id);

        if (activo == null) {
            monitorValidator.validarCambioEstado(monitor, null);
        }

        validarOperacionDirectaSiTieneUsuario(monitor, activo);

        monitorValidator.validarCambioEstado(monitor, activo);

        if (Boolean.FALSE.equals(activo)) {
            consultaResponsableOperacionService.validarMonitorSinConsultasOperativas(id);
        }

        monitor.setActivo(activo);

        return monitorMapper.convertirADTO(
                monitorRepository.save(monitor));
    }

    @Transactional
    @Auditable(action = "ELIMINAR_PERFIL", entityName = "Monitor", entityId = "#id")
    public void eliminar(Long id) {
        asesorMonitorAccessService.validarPuedeGestionarAsesoresYMonitores();

        Monitor monitor = buscarPorIdConUsuarioBloqueado(id);

        validarOperacionDirectaSiTieneUsuario(monitor, false);

        // Se conserva el perfil y se desactiva porque puede estar asociado a consultas
        // o seguimientos.
        monitorValidator.validarCambioEstado(monitor, false);
        consultaResponsableOperacionService.validarMonitorSinConsultasOperativas(id);

        monitor.setActivo(false);

        monitorRepository.save(monitor);
    }

    private Monitor buscarPorIdConUsuarioBloqueado(Long id) {
        monitorRepository.findUsuarioSistemaIdById(id)
                .ifPresent(this::bloquearUsuarioSistema);

        return buscarPorId(id);
    }

    private void bloquearUsuarioSistema(Long usuarioSistemaId) {
        usuarioSistemaRepository.findByIdForUpdate(usuarioSistemaId)
                .orElseThrow(() -> new BusinessException(
                        "Usuario del sistema no encontrado con id: "
                                + usuarioSistemaId));
    }

    private void validarOperacionDirectaSiTieneUsuario(
            Monitor monitor,
            Boolean activo) {

        UsuarioSistema usuarioSistema = monitor.getUsuarioSistema();

        if (usuarioSistema == null) {
            return;
        }

        perfilOperacionEstadoValidator.validarOperacionDirecta(
                usuarioSistema,
                TipoPerfilUsuario.MONITOR,
                Boolean.TRUE.equals(activo)
                        ? OperacionEstadoPerfil.ACTIVAR
                        : OperacionEstadoPerfil.DESACTIVAR);
    }

    private DatosMonitor prepararDatos(MonitorDTO dto, Boolean activo) {
        String nombre = normalizarTexto(dto.getNombre());
        String documento = normalizarNumeroDocumento(dto.getDocumento());
        String email = normalizarEmail(dto.getEmail());
        String telefono = normalizarTelefono(dto.getTelefono());
        String usuario = normalizarUsuario(dto.getUsuario());
        String codigo = normalizarCodigo(dto.getCodigo());

        monitorValidator.validarCamposObligatorios(
                nombre,
                documento,
                email,
                telefono,
                usuario,
                codigo);

        TipoDocumento tipoDocumento = obtenerTipoDocumento(dto.getTipoDocumentoId());
        Sede sede = obtenerSede(dto.getSedeId());

        return new DatosMonitor(
                nombre,
                documento,
                email,
                telefono,
                usuario,
                codigo,
                tipoDocumento,
                sede,
                activo);
    }

    private Monitor buscarPorId(Long id) {
        if (id == null) {
            throw new BusinessException("El id del monitor es obligatorio");
        }

        return monitorRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Monitor no encontrado con id: " + id));
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
}
