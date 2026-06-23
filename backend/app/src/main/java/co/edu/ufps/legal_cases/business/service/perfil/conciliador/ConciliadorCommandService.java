package co.edu.ufps.legal_cases.business.service.perfil.conciliador;

import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarCodigo;
import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarEmail;
import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarNumeroDocumento;
import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarTelefono;
import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarTexto;
import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarUsuario;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.audit.aop.log.Auditable;

import co.edu.ufps.legal_cases.business.dto.perfil.ConciliadorDTO;
import co.edu.ufps.legal_cases.business.model.catalogo.Sede;
import co.edu.ufps.legal_cases.business.model.catalogo.TipoDocumento;
import co.edu.ufps.legal_cases.business.model.perfil.Conciliador;
import co.edu.ufps.legal_cases.business.repository.catalogo.SedeRepository;
import co.edu.ufps.legal_cases.business.repository.catalogo.TipoDocumentoRepository;
import co.edu.ufps.legal_cases.business.repository.perfil.ConciliadorRepository;
import co.edu.ufps.legal_cases.business.service.acceso.perfil.ConciliadorAccessService;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.service.account.usuario.UsuarioSistemaPerfilEstadoService;
import co.edu.ufps.legal_cases.security.service.account.usuario.UsuarioSistemaRegistroService;

@Service
public class ConciliadorCommandService {

    private final ConciliadorRepository conciliadorRepository;
    private final TipoDocumentoRepository tipoDocumentoRepository;
    private final SedeRepository sedeRepository;
    private final UsuarioSistemaRegistroService usuarioSistemaRegistroService;
    private final UsuarioSistemaPerfilEstadoService usuarioSistemaPerfilEstadoService;
    private final ConciliadorAccessService conciliadorAccessService;
    private final ConciliadorValidator conciliadorValidator;
    private final ConciliadorMapper conciliadorMapper;

    public ConciliadorCommandService(
            ConciliadorRepository conciliadorRepository,
            TipoDocumentoRepository tipoDocumentoRepository,
            SedeRepository sedeRepository,
            UsuarioSistemaRegistroService usuarioSistemaRegistroService,
            UsuarioSistemaPerfilEstadoService usuarioSistemaPerfilEstadoService,
            ConciliadorAccessService conciliadorAccessService,
            ConciliadorValidator conciliadorValidator,
            ConciliadorMapper conciliadorMapper) {
        this.conciliadorRepository = conciliadorRepository;
        this.tipoDocumentoRepository = tipoDocumentoRepository;
        this.sedeRepository = sedeRepository;
        this.usuarioSistemaRegistroService = usuarioSistemaRegistroService;
        this.usuarioSistemaPerfilEstadoService = usuarioSistemaPerfilEstadoService;
        this.conciliadorAccessService = conciliadorAccessService;
        this.conciliadorValidator = conciliadorValidator;
        this.conciliadorMapper = conciliadorMapper;
    }

    @Transactional
    @Auditable(action = "CREAR_PERFIL", entityName = "Conciliador")
    public ConciliadorDTO crear(ConciliadorDTO dto) {
        conciliadorAccessService.validarPuedeGestionarConciliadores();
        conciliadorValidator.validarIdNoEnviadoEnCreacion(dto.getId());

        DatosConciliador datos = prepararDatos(dto, true);

        conciliadorValidator.validarCamposObligatorios(datos);
        conciliadorValidator.validarDuplicadosCreacion(
                datos.documento(),
                datos.email(),
                datos.telefono(),
                datos.usuario(),
                datos.codigo());

        Conciliador conciliador = new Conciliador();
        conciliadorMapper.aplicarDatos(conciliador, datos);

        Conciliador conciliadorGuardado = conciliadorRepository.save(conciliador);

        // El perfil se guarda primero porque el usuario del sistema se crea con datos
        // del conciliador persistido.
        UsuarioSistema usuarioSistema = usuarioSistemaRegistroService.crearParaConciliador(conciliadorGuardado);

        // El perfil real queda apuntando al usuario del sistema normalizado.
        conciliadorGuardado.setUsuarioSistema(usuarioSistema);

        return conciliadorMapper.convertirADTO(conciliadorRepository.save(conciliadorGuardado));
    }

    @Transactional
    @Auditable(action = "ACTUALIZAR_PERFIL", entityName = "Conciliador")
    public ConciliadorDTO actualizar(Long id, ConciliadorDTO dto) {
        conciliadorAccessService.validarPuedeGestionarConciliadores();

        Conciliador existente = buscarPorId(id);

        conciliadorValidator.validarIdNoCambiado(id, dto.getId());

        // Actualizar datos del perfil no debe cambiar el estado activo.
        // Para eso existe cambiarEstado().
        DatosConciliador datos = prepararDatos(dto, existente.getActivo());

        conciliadorValidator.validarCamposObligatorios(datos);
        conciliadorValidator.validarDuplicadosActualizacion(
                id,
                datos.documento(),
                datos.email(),
                datos.telefono(),
                datos.usuario(),
                datos.codigo());

        conciliadorValidator.validarExistenCambios(existente, datos);

        conciliadorMapper.aplicarDatos(existente, datos);

        return conciliadorMapper.convertirADTO(conciliadorRepository.save(existente));
    }

    @Transactional
    @Auditable(action = "DESACTIVAR/REACTIVAR_PERFIL", entityName = "Conciliador")
    public ConciliadorDTO cambiarEstado(Long id, Boolean activo) {
        conciliadorAccessService.validarPuedeGestionarConciliadores();

        Conciliador conciliador = buscarPorId(id);

        conciliadorValidator.validarCambioEstado(conciliador, activo);

        conciliador.setActivo(activo);

        Conciliador guardado = conciliadorRepository.save(conciliador);

        // El estado operativo del perfil y del usuario de acceso deben permanecer
        // sincronizados.
        usuarioSistemaPerfilEstadoService.sincronizarEstadoSiExiste(
                guardado.getUsuarioSistema(),
                activo);

        return conciliadorMapper.convertirADTO(guardado);
    }

    @Transactional
    @Auditable(action = "ELIMINAR_PERFIL", entityName = "Conciliador")
    public void eliminar(Long id) {
        conciliadorAccessService.validarPuedeGestionarConciliadores();

        Conciliador conciliador = buscarPorId(id);

        // Se conserva el perfil y se desactiva porque puede quedar asociado a
        // conciliaciones.
        conciliadorValidator.validarCambioEstado(conciliador, false);

        conciliador.setActivo(false);

        Conciliador guardado = conciliadorRepository.save(conciliador);

        usuarioSistemaPerfilEstadoService.sincronizarEstadoSiExiste(
                guardado.getUsuarioSistema(),
                false);
    }

    private DatosConciliador prepararDatos(ConciliadorDTO dto, Boolean activo) {
        String nombre = normalizarTexto(dto.getNombre());
        String documento = normalizarNumeroDocumento(dto.getDocumento());
        String email = normalizarEmail(dto.getEmail());
        String telefono = normalizarTelefono(dto.getTelefono());
        String usuario = normalizarUsuario(dto.getUsuario());
        String codigo = normalizarCodigo(dto.getCodigo());

        TipoDocumento tipoDocumento = obtenerTipoDocumento(dto.getTipoDocumentoId());
        Sede sede = obtenerSede(dto.getSedeId());

        return new DatosConciliador(
                nombre,
                documento,
                email,
                telefono,
                usuario,
                codigo,
                tipoDocumento,
                sede,
                dto.getTipoConciliador(),
                activo);
    }

    private Conciliador buscarPorId(Long id) {
        if (id == null) {
            throw new BusinessException("El id del conciliador es obligatorio");
        }

        return conciliadorRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Conciliador no encontrado con id: " + id));
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