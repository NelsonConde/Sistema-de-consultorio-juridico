package co.edu.ufps.legal_cases.business.service.perfil.administrativo;

import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarCodigo;
import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarEmail;
import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarNumeroDocumento;
import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarTelefono;
import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarTexto;
import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarUsuario;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.audit.aop.log.Auditable;

import co.edu.ufps.legal_cases.business.dto.perfil.AdministrativoDTO;
import co.edu.ufps.legal_cases.business.model.catalogo.Sede;
import co.edu.ufps.legal_cases.business.model.catalogo.TipoDocumento;
import co.edu.ufps.legal_cases.business.model.perfil.Administrativo;
import co.edu.ufps.legal_cases.business.repository.catalogo.SedeRepository;
import co.edu.ufps.legal_cases.business.repository.catalogo.TipoDocumentoRepository;
import co.edu.ufps.legal_cases.business.repository.perfil.AdministrativoRepository;
import co.edu.ufps.legal_cases.business.service.acceso.perfil.AdministrativoAccessService;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.service.account.usuario.UsuarioSistemaPerfilEstadoService;
import co.edu.ufps.legal_cases.security.service.account.usuario.UsuarioSistemaRegistroService;

@Service
public class AdministrativoCommandService {

    private final AdministrativoRepository administrativoRepository;
    private final TipoDocumentoRepository tipoDocumentoRepository;
    private final SedeRepository sedeRepository;
    private final UsuarioSistemaRegistroService usuarioSistemaRegistroService;
    private final UsuarioSistemaPerfilEstadoService usuarioSistemaPerfilEstadoService;
    private final AdministrativoAccessService administrativoAccessService;
    private final AdministrativoValidator administrativoValidator;
    private final AdministrativoMapper administrativoMapper;

    public AdministrativoCommandService(
            AdministrativoRepository administrativoRepository,
            TipoDocumentoRepository tipoDocumentoRepository,
            SedeRepository sedeRepository,
            UsuarioSistemaRegistroService usuarioSistemaRegistroService,
            UsuarioSistemaPerfilEstadoService usuarioSistemaPerfilEstadoService,
            AdministrativoAccessService administrativoAccessService,
            AdministrativoValidator administrativoValidator,
            AdministrativoMapper administrativoMapper) {
        this.administrativoRepository = administrativoRepository;
        this.tipoDocumentoRepository = tipoDocumentoRepository;
        this.sedeRepository = sedeRepository;
        this.usuarioSistemaRegistroService = usuarioSistemaRegistroService;
        this.usuarioSistemaPerfilEstadoService = usuarioSistemaPerfilEstadoService;
        this.administrativoAccessService = administrativoAccessService;
        this.administrativoValidator = administrativoValidator;
        this.administrativoMapper = administrativoMapper;
    }

    @Transactional
    @Auditable(action = "CREAR_PERFIL", entityName = "Administrativo")
    public AdministrativoDTO crear(AdministrativoDTO dto) {
        administrativoAccessService.validarPuedeGestionarAdministradores();
        administrativoValidator.validarIdNoEnviadoEnCreacion(dto.getId());

        DatosAdministrativo datos = prepararDatos(dto, true, false);

        administrativoValidator.validarCamposObligatorios(datos);
        administrativoValidator.validarDuplicadosCreacion(
                datos.documento(),
                datos.email(),
                datos.telefono(),
                datos.usuario(),
                datos.codigo());

        Administrativo administrativo = new Administrativo();
        administrativoMapper.aplicarDatos(administrativo, datos);

        Administrativo administrativoGuardado = administrativoRepository.save(administrativo);

        // El perfil se guarda primero porque el usuario del sistema se crea con datos
        // del administrativo persistido.
        UsuarioSistema usuarioSistema = usuarioSistemaRegistroService.crearParaAdministrativo(administrativoGuardado);

        // El perfil real queda apuntando al usuario del sistema normalizado.
        administrativoGuardado.setUsuarioSistema(usuarioSistema);

        return administrativoMapper.convertirADTO(administrativoRepository.save(administrativoGuardado));
    }

    @Transactional
    @Auditable(action = "ACTUALIZAR_PERFIL", entityName = "Administrativo")
    public AdministrativoDTO actualizar(Long id, AdministrativoDTO dto) {
        administrativoAccessService.validarPuedeGestionarAdministradores();

        Administrativo existente = buscarPorId(id);

        administrativoValidator.validarIdNoCambiado(id, dto.getId());

        // Actualizar datos del perfil no debe cambiar activo ni directora.
        // Para esos campos existen cambiarEstado() y cambiarDirectora().
        DatosAdministrativo datos = prepararDatos(
                dto,
                existente.getActivo(),
                existente.getDirectora());

        administrativoValidator.validarCamposObligatorios(datos);
        administrativoValidator.validarDuplicadosActualizacion(
                id,
                datos.documento(),
                datos.email(),
                datos.telefono(),
                datos.usuario(),
                datos.codigo());

        administrativoValidator.validarExistenCambios(existente, datos);

        administrativoMapper.aplicarDatos(existente, datos);

        return administrativoMapper.convertirADTO(administrativoRepository.save(existente));
    }

    @Transactional
    @Auditable(action = "DESACTIVAR/REACTIVAR_PERFIL", entityName = "Administrativo")
    public AdministrativoDTO cambiarEstado(Long id, Boolean activo) {
        administrativoAccessService.validarPuedeGestionarAdministradores();

        Administrativo administrativo = buscarPorId(id);

        administrativoValidator.validarCambioEstado(administrativo, activo);

        administrativo.setActivo(activo);

        Administrativo guardado = administrativoRepository.save(administrativo);

        // El estado operativo del perfil y del usuario de acceso deben permanecer
        // sincronizados.
        usuarioSistemaPerfilEstadoService.sincronizarEstadoSiExiste(
                guardado.getUsuarioSistema(),
                activo);

        return administrativoMapper.convertirADTO(guardado);
    }

    @Transactional
    @Auditable(action = "CAMBIAR_DIRECTORA", entityName = "Administrativo")
    public AdministrativoDTO cambiarDirectora(Long id, Boolean directora) {
        administrativoAccessService.validarPuedeGestionarAdministradores();

        Administrativo administrativo = buscarPorId(id);

        administrativoValidator.validarCambioDirectora(administrativo, directora);

        administrativo.setDirectora(directora);

        return administrativoMapper.convertirADTO(administrativoRepository.save(administrativo));
    }

    @Transactional
    @Auditable(action = "ELIMINAR_PERFIL", entityName = "Administrativo")
    public void eliminar(Long id) {
        administrativoAccessService.validarPuedeGestionarAdministradores();

        Administrativo administrativo = buscarPorId(id);

        // Se conserva el perfil y se desactiva porque tiene usuario del sistema
        // asociado.
        administrativoValidator.validarCambioEstado(administrativo, false);

        administrativo.setActivo(false);

        Administrativo guardado = administrativoRepository.save(administrativo);

        usuarioSistemaPerfilEstadoService.sincronizarEstadoSiExiste(
                guardado.getUsuarioSistema(),
                false);
    }

    private DatosAdministrativo prepararDatos(
            AdministrativoDTO dto,
            Boolean activo,
            Boolean directora) {
        String nombre = normalizarTexto(dto.getNombre());
        String documento = normalizarNumeroDocumento(dto.getDocumento());
        String email = normalizarEmail(dto.getEmail());
        String telefono = normalizarTelefono(dto.getTelefono());
        String usuario = normalizarUsuario(dto.getUsuario());
        String codigo = normalizarCodigo(dto.getCodigo());

        TipoDocumento tipoDocumento = obtenerTipoDocumento(dto.getTipoDocumentoId());
        Sede sede = obtenerSede(dto.getSedeId());

        return new DatosAdministrativo(
                nombre,
                documento,
                email,
                telefono,
                usuario,
                codigo,
                tipoDocumento,
                sede,
                activo,
                directora);
    }

    private Administrativo buscarPorId(Long id) {
        if (id == null) {
            throw new BusinessException("El id del administrativo es obligatorio");
        }

        return administrativoRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Administrativo no encontrado con id: " + id));
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