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
import co.edu.ufps.legal_cases.security.service.invariant.administracion.AdministracionInvariantService;

// Orquesta el cambio de perfil y mantiene sincronizados el perfil real,
// el rol del usuario y el historial asociado a la transición.
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
    private final AdministracionInvariantService administracionInvariantService;

    public UsuarioCambioPerfilService(
            UsuarioSistemaRepository usuarioSistemaRepository,
            RolRepository rolRepository,
            PerfilUsuarioResolverService perfilUsuarioResolverService,
            PerfilEstadoService perfilEstadoService,
            UsuarioCambioPerfilHistorialService historialService,
            UsuarioSistemaService usuarioSistemaService,
            PerfilCambioHandlerRegistry handlerRegistry,
            UsuarioCambioPerfilValidator usuarioCambioPerfilValidator,
            AdministracionInvariantService administracionInvariantService) {

        this.usuarioSistemaRepository = usuarioSistemaRepository;
        this.rolRepository = rolRepository;
        this.perfilUsuarioResolverService =
                perfilUsuarioResolverService;
        this.perfilEstadoService = perfilEstadoService;
        this.historialService = historialService;
        this.usuarioSistemaService = usuarioSistemaService;
        this.handlerRegistry = handlerRegistry;
        this.usuarioCambioPerfilValidator =
                usuarioCambioPerfilValidator;
        this.administracionInvariantService =
                administracionInvariantService;
    }

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

    private <T extends CambiarPerfilBaseDTO>
            UsuarioSistemaDTO cambiarPerfil(
                    Long usuarioSistemaId,
                    TipoPerfilUsuario tipoPerfilDestino,
                    T dto,
                    String cambiadoPorUsername) {

        usuarioCambioPerfilValidator.validarDatosCambio(
                usuarioSistemaId,
                tipoPerfilDestino,
                dto);

        // La protección se ejecuta antes de cargar el usuario y su perfil.
        // Así, cualquier espera por concurrencia ocurre antes de trabajar
        // con estado persistido que podría quedar obsoleto.
        administracionInvariantService.validarCambioPerfil(
                usuarioSistemaId,
                tipoPerfilDestino);

        UsuarioSistema usuario =
                obtenerUsuarioSistemaActivo(
                        usuarioSistemaId);

        PerfilUsuarioActual perfilAnterior =
                obtenerPerfilAnteriorValidandoDestino(
                        usuario,
                        tipoPerfilDestino);

        Rol rolAnterior = usuario.getRol();

        Rol rolNuevo = obtenerRolDestino(
                dto.getRolId(),
                tipoPerfilDestino);

        UsuarioSistema cambiadoPorUsuario =
                obtenerUsuarioCambiador(
                        cambiadoPorUsername);

        perfilEstadoService.desactivarPerfilActual(
                usuario.getId(),
                perfilAnterior.getTipoPerfil());

        ResultadoCambioPerfil resultadoCambio =
                handlerRegistry
                        .obtenerHandler(
                                tipoPerfilDestino,
                                dto)
                        .crearOActualizarPerfil(
                                usuario,
                                dto);

        actualizarUsuarioSistema(
                usuario,
                rolNuevo,
                tipoPerfilDestino);

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

        return usuarioSistemaService.obtenerPorId(
                usuario.getId());
    }

    private UsuarioSistema obtenerUsuarioSistemaActivo(
            Long usuarioSistemaId) {

        UsuarioSistema usuario =
                usuarioSistemaRepository
                        .findWithRolAndPermisosById(
                                usuarioSistemaId)
                        .orElseThrow(() ->
                                new BusinessException(
                                        "Usuario del sistema no encontrado con id: "
                                                + usuarioSistemaId));

        usuarioCambioPerfilValidator
                .validarUsuarioPuedeCambiarPerfil(usuario);

        return usuario;
    }

    private PerfilUsuarioActual
            obtenerPerfilAnteriorValidandoDestino(
                    UsuarioSistema usuario,
                    TipoPerfilUsuario tipoPerfilDestino) {

        PerfilUsuarioActual perfilAnterior =
                perfilUsuarioResolverService
                        .obtenerPerfilActivoObligatorio(usuario);

        usuarioCambioPerfilValidator
                .validarPerfilDestinoDiferente(
                        perfilAnterior,
                        tipoPerfilDestino);

        return perfilAnterior;
    }

    private Rol obtenerRolDestino(
            Long rolId,
            TipoPerfilUsuario tipoPerfilDestino) {

        Rol rol = rolRepository.findById(rolId)
                .orElse(null);

        usuarioCambioPerfilValidator.validarRolDestino(
                rol,
                rolId,
                tipoPerfilDestino);

        return rol;
    }

    private UsuarioSistema obtenerUsuarioCambiador(
            String cambiadoPorUsername) {

        String username =
                normalizarEmail(cambiadoPorUsername);

        if (username == null) {
            return null;
        }

        return usuarioSistemaRepository
                .findByUsernameIgnoreCase(username)
                .orElse(null);
    }

    private void actualizarUsuarioSistema(
            UsuarioSistema usuario,
            Rol rolNuevo,
            TipoPerfilUsuario tipoPerfilNuevo) {

        usuario.setRol(rolNuevo);
        usuario.setTipoPerfilActual(tipoPerfilNuevo);

        usuarioSistemaRepository.save(usuario);
    }
}