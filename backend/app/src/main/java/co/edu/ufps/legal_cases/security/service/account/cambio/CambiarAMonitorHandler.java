package co.edu.ufps.legal_cases.security.service.account.cambio;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.model.catalogo.Sede;
import co.edu.ufps.legal_cases.business.model.catalogo.TipoDocumento;
import co.edu.ufps.legal_cases.business.model.perfil.Monitor;
import co.edu.ufps.legal_cases.business.repository.perfil.MonitorRepository;
import co.edu.ufps.legal_cases.security.dto.account.cambio.CambiarPerfilAMonitorDTO;
import co.edu.ufps.legal_cases.security.dto.account.cambio.DatosPerfilCambioNormalizados;
import co.edu.ufps.legal_cases.security.dto.account.cambio.ResultadoCambioPerfil;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;

// Servicio concreto para cambiar un usuario al perfil MONITOR.
// Se encarga de crear, actualizar o reactivar el perfil monitor
// de un usuario que está cambiando de rol.
@Component
@Transactional
public class CambiarAMonitorHandler
        implements PerfilCambioHandler<CambiarPerfilAMonitorDTO> {

    private final MonitorRepository monitorRepository;
    private final PerfilCambioDatosService perfilCambioDatosService;
    private final PerfilCambioDuplicadosService perfilCambioDuplicadosService;

    public CambiarAMonitorHandler(
            MonitorRepository monitorRepository,
            PerfilCambioDatosService perfilCambioDatosService,
            PerfilCambioDuplicadosService perfilCambioDuplicadosService) {
        this.monitorRepository = monitorRepository;
        this.perfilCambioDatosService = perfilCambioDatosService;
        this.perfilCambioDuplicadosService = perfilCambioDuplicadosService;
    }

    @Override
    public TipoPerfilUsuario getTipoPerfil() {
        // Indica que este handler solo maneja cambios hacia MONITOR.
        return TipoPerfilUsuario.MONITOR;
    }

    @Override
    public Class<CambiarPerfilAMonitorDTO> getDtoClass() {
        // Indica qué DTO espera este handler.
        // Esto permite validar que no se use por error un DTO de otro perfil.
        return CambiarPerfilAMonitorDTO.class;
    }

    @Override
    public ResultadoCambioPerfil crearOActualizarPerfil(
            UsuarioSistema usuarioSistema,
            CambiarPerfilAMonitorDTO dto) {

        // Normaliza los datos comunes del perfil:
        // nombre, documento, email, teléfono, usuario y código.
        // En monitor el documento no es obligatorio, por eso el último parámetro es false.
        DatosPerfilCambioNormalizados datos =
                perfilCambioDatosService.normalizarDatosBasicos(
                        dto,
                        usuarioSistema,
                        false
                );

        // Obtiene el tipo de documento desde el catálogo.
        // En monitor es opcional.
        TipoDocumento tipoDocumento =
                perfilCambioDatosService.obtenerTipoDocumentoOpcional(dto.getTipoDocumentoId());

        // Obtiene la sede desde el catálogo.
        // En monitor es opcional.
        Sede sede =
                perfilCambioDatosService.obtenerSedeOpcional(dto.getSedeId());

        // Si el usuario ya tuvo un perfil monitor, se reutiliza ese registro.
        // Si nunca lo tuvo, se crea uno nuevo.
        Monitor monitor = monitorRepository
                .findByUsuarioSistema_Id(usuarioSistema.getId())
                .orElseGet(Monitor::new);

        // Valida duplicados usando el servicio centralizado.
        // Si el monitor es nuevo, valida contra todos.
        // Si ya existe, permite conservar sus mismos datos y solo bloquea
        // si documento, email, teléfono, usuario o código pertenecen a otro registro.
        perfilCambioDuplicadosService.validarMonitor(
                monitor.getId(),
                datos
        );

        // Se asocia el perfil monitor al mismo UsuarioSistema.
        // Es la misma cuenta; lo que cambia es el perfil/rol activo, no el usuario de login.
        monitor.setUsuarioSistema(usuarioSistema);
        monitor.setNombre(datos.getNombre());
        monitor.setTipoDocumento(tipoDocumento);
        monitor.setDocumento(datos.getDocumento());
        monitor.setEmail(datos.getEmail());
        monitor.setTelefono(datos.getTelefono());
        monitor.setUsuario(datos.getUsuario());
        monitor.setCodigo(datos.getCodigo());
        monitor.setSede(sede);
        monitor.setActivo(true);

        Monitor monitorGuardado = monitorRepository.save(monitor);

        // Devuelve solo lo necesario para que el orquestador sepa
        // cuál perfil quedó activo después del cambio.
        return new ResultadoCambioPerfil(
                monitorGuardado.getId(),
                TipoPerfilUsuario.MONITOR
        );
    }
}