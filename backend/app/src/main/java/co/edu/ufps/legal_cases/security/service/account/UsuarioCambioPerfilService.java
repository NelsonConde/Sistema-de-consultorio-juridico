package co.edu.ufps.legal_cases.security.service.account;

import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarEmail;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.dto.account.PerfilUsuarioActual;
import co.edu.ufps.legal_cases.security.dto.account.UsuarioSistemaDTO;
import co.edu.ufps.legal_cases.security.dto.account.cambio.CambiarPerfilAAdministrativoDTO;
import co.edu.ufps.legal_cases.security.dto.account.cambio.CambiarPerfilAAsesorDTO;
import co.edu.ufps.legal_cases.security.dto.account.cambio.CambiarPerfilAConciliadorDTO;
import co.edu.ufps.legal_cases.security.dto.account.cambio.CambiarPerfilAEstudianteDTO;
import co.edu.ufps.legal_cases.security.dto.account.cambio.CambiarPerfilAMonitorDTO;
import co.edu.ufps.legal_cases.security.dto.account.cambio.CambiarPerfilBaseDTO;
import co.edu.ufps.legal_cases.security.dto.account.cambio.ResultadoCambioPerfil;
import co.edu.ufps.legal_cases.security.model.access.Rol;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.repository.access.RolRepository;
import co.edu.ufps.legal_cases.security.repository.account.UsuarioSistemaRepository;
import co.edu.ufps.legal_cases.security.service.account.cambio.PerfilCambioHandlerRegistry;
import co.edu.ufps.legal_cases.security.service.account.cambio.UsuarioCambioPerfilHistorialService;
import co.edu.ufps.legal_cases.security.service.account.cambio.UsuarioCambioPerfilValidator;
import co.edu.ufps.legal_cases.security.service.account.perfil.PerfilEstadoService;
import co.edu.ufps.legal_cases.security.service.account.perfil.PerfilUsuarioResolverService;

// Este servicio orquesta el cambio de perfil:
// valida el usuario actual, desactiva el perfil anterior,
// ejecuta el handler del nuevo perfil, actualiza UsuarioSistema y registra historial.
@Service
@Transactional
public class UsuarioCambioPerfilService {

    private final UsuarioSistemaRepository usuarioSistemaRepository;
    private final RolRepository rolRepository;
    private final PerfilUsuarioResolverService perfilUsuarioResolverService;
    private final PerfilEstadoService perfilEstadoService;
    private final UsuarioCambioPerfilHistorialService historialService;
    private final UsuarioSistemaService usuarioSistemaService;
    private final PerfilCambioHandlerRegistry handlerRegistry;
    private final UsuarioCambioPerfilValidator usuarioCambioPerfilValidator;

    public UsuarioCambioPerfilService(
            UsuarioSistemaRepository usuarioSistemaRepository,
            RolRepository rolRepository,
            PerfilUsuarioResolverService perfilUsuarioResolverService,
            PerfilEstadoService perfilEstadoService,
            UsuarioCambioPerfilHistorialService historialService,
            UsuarioSistemaService usuarioSistemaService,
            PerfilCambioHandlerRegistry handlerRegistry,
            UsuarioCambioPerfilValidator usuarioCambioPerfilValidator) {
        this.usuarioSistemaRepository = usuarioSistemaRepository;
        this.rolRepository = rolRepository;
        this.perfilUsuarioResolverService = perfilUsuarioResolverService;
        this.perfilEstadoService = perfilEstadoService;
        this.historialService = historialService;
        this.usuarioSistemaService = usuarioSistemaService;
        this.handlerRegistry = handlerRegistry;
        this.usuarioCambioPerfilValidator = usuarioCambioPerfilValidator;
    }

    // Métodos para que el controller use y no tenga detrás la lógica de negocio.
    public UsuarioSistemaDTO cambiarAAdministrativo(
            Long usuarioSistemaId,
            CambiarPerfilAAdministrativoDTO dto,
            String cambiadoPorUsername) {

        return cambiarPerfil(
                usuarioSistemaId,
                TipoPerfilUsuario.ADMINISTRATIVO,
                dto,
                cambiadoPorUsername);
    }

    public UsuarioSistemaDTO cambiarAEstudiante(
            Long usuarioSistemaId,
            CambiarPerfilAEstudianteDTO dto,
            String cambiadoPorUsername) {

        return cambiarPerfil(
                usuarioSistemaId,
                TipoPerfilUsuario.ESTUDIANTE,
                dto,
                cambiadoPorUsername);
    }

    public UsuarioSistemaDTO cambiarAAsesor(
            Long usuarioSistemaId,
            CambiarPerfilAAsesorDTO dto,
            String cambiadoPorUsername) {

        return cambiarPerfil(
                usuarioSistemaId,
                TipoPerfilUsuario.ASESOR,
                dto,
                cambiadoPorUsername);
    }

    public UsuarioSistemaDTO cambiarAMonitor(
            Long usuarioSistemaId,
            CambiarPerfilAMonitorDTO dto,
            String cambiadoPorUsername) {

        return cambiarPerfil(
                usuarioSistemaId,
                TipoPerfilUsuario.MONITOR,
                dto,
                cambiadoPorUsername);
    }

    public UsuarioSistemaDTO cambiarAConciliador(
            Long usuarioSistemaId,
            CambiarPerfilAConciliadorDTO dto,
            String cambiadoPorUsername) {

        return cambiarPerfil(
                usuarioSistemaId,
                TipoPerfilUsuario.CONCILIADOR,
                dto,
                cambiadoPorUsername);
    }

    private <T extends CambiarPerfilBaseDTO> UsuarioSistemaDTO cambiarPerfil(
            Long usuarioSistemaId,
            TipoPerfilUsuario tipoPerfilDestino,
            T dto,
            String cambiadoPorUsername) {

        // Valida que los datos básicos para hacer el cambio no vengan vacíos.
        usuarioCambioPerfilValidator.validarDatosCambio(
                usuarioSistemaId,
                tipoPerfilDestino,
                dto);

        // Busca el usuario del sistema que se va a cambiar y el perfil que tiene actualmente.
        UsuarioSistema usuario = obtenerUsuarioSistemaActivo(usuarioSistemaId);
        PerfilUsuarioActual perfilAnterior = obtenerPerfilAnteriorValidandoDestino(usuario, tipoPerfilDestino);

        Rol rolAnterior = usuario.getRol();

        // Busca y valida el rol nuevo que tendrá el usuario y revisa que rol y perfil estén relacionados.
        Rol rolNuevo = obtenerRolDestino(dto.getRolId(), tipoPerfilDestino);

        UsuarioSistema cambiadoPorUsuario = obtenerUsuarioCambiador(cambiadoPorUsername);

        // Desactiva el perfil anterior del usuario.
        perfilEstadoService.desactivarPerfilActual(
                usuario.getId(),
                perfilAnterior.getTipoPerfil());

        // Busca el handler correspondiente al perfil destino y le pide crear o actualizar el nuevo perfil.
        ResultadoCambioPerfil resultadoCambio = handlerRegistry
                .obtenerHandler(tipoPerfilDestino, dto)
                .crearOActualizarPerfil(usuario, dto);

        // Le cambia el rol y el tipo de perfil en la tabla de usuario del sistema.
        actualizarUsuarioSistema(usuario, rolNuevo, tipoPerfilDestino);

        // Registra el cambio.
        historialService.registrarCambio(
                usuario,
                perfilAnterior,
                rolAnterior,
                resultadoCambio.getTipoPerfil(),
                resultadoCambio.getPerfilId(),
                rolNuevo,
                dto.getMotivo(),
                cambiadoPorUsuario,
                cambiadoPorUsername);

        return usuarioSistemaService.obtenerPorId(usuario.getId());
    }

    // Se asegura que el que quiere cambiar de perfil pueda hacerlo.
    private UsuarioSistema obtenerUsuarioSistemaActivo(Long usuarioSistemaId) {
        UsuarioSistema usuario = usuarioSistemaRepository.findWithRolAndPermisosById(usuarioSistemaId)
                .orElseThrow(() -> new BusinessException(
                        "Usuario del sistema no encontrado con id: " + usuarioSistemaId));

        usuarioCambioPerfilValidator.validarUsuarioPuedeCambiarPerfil(usuario);

        return usuario;
    }

    // Valida que el cambio no vaya a ser del mismo perfil.
    private PerfilUsuarioActual obtenerPerfilAnteriorValidandoDestino(
            UsuarioSistema usuario,
            TipoPerfilUsuario tipoPerfilDestino) {

        PerfilUsuarioActual perfilAnterior = perfilUsuarioResolverService.obtenerPerfilActivoObligatorio(usuario);

        usuarioCambioPerfilValidator.validarPerfilDestinoDiferente(
                perfilAnterior,
                tipoPerfilDestino);

        return perfilAnterior;
    }

    // Aquí valida la existencia del rol y del perfil al que se quiere cambiar.
    // También se valida que el perfil y el rol estén relacionados:
    // no puede haber un rol Admin con perfil estudiante; los dos deben corresponder.
    private Rol obtenerRolDestino(Long rolId, TipoPerfilUsuario tipoPerfilDestino) {
        Rol rol = rolRepository.findById(rolId)
                .orElse(null);

        usuarioCambioPerfilValidator.validarRolDestino(
                rol,
                rolId,
                tipoPerfilDestino);

        return rol;
    }

    // Es para buscar al usuario del sistema que ejecuta el cambio.
    private UsuarioSistema obtenerUsuarioCambiador(String cambiadoPorUsername) {
        String username = normalizarEmail(cambiadoPorUsername);

        if (username == null) {
            return null;
        }

        return usuarioSistemaRepository.findByUsernameIgnoreCase(username)
                .orElse(null);
    }

    // Cuando ya se tiene el historial de cambio de perfil/rol,
    // entonces se actualiza el usuario de sistema con el nuevo rol y tipo de perfil.
    private void actualizarUsuarioSistema(
            UsuarioSistema usuario,
            Rol rolNuevo,
            TipoPerfilUsuario tipoPerfilNuevo) {

        usuario.setRol(rolNuevo);
        usuario.setTipoPerfilActual(tipoPerfilNuevo);

        usuarioSistemaRepository.save(usuario);
    }
}