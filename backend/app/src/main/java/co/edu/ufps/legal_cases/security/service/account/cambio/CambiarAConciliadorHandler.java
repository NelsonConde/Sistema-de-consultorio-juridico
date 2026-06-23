package co.edu.ufps.legal_cases.security.service.account.cambio;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.model.catalogo.Sede;
import co.edu.ufps.legal_cases.business.model.catalogo.TipoDocumento;
import co.edu.ufps.legal_cases.business.model.perfil.Conciliador;
import co.edu.ufps.legal_cases.business.model.perfil.TipoConciliador;
import co.edu.ufps.legal_cases.business.repository.perfil.ConciliadorRepository;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.dto.account.cambio.CambiarPerfilAConciliadorDTO;
import co.edu.ufps.legal_cases.security.dto.account.cambio.DatosPerfilCambioNormalizados;
import co.edu.ufps.legal_cases.security.dto.account.cambio.ResultadoCambioPerfil;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;

// Servicio concreto para cambiar un usuario al perfil CONCILIADOR.
// Se encarga de crear, actualizar o reactivar el perfil conciliador
// de un usuario que está cambiando de rol.
@Component
@Transactional
public class CambiarAConciliadorHandler
        implements PerfilCambioHandler<CambiarPerfilAConciliadorDTO> {

    private final ConciliadorRepository conciliadorRepository;
    private final PerfilCambioDatosService perfilCambioDatosService;
    private final PerfilCambioDuplicadosService perfilCambioDuplicadosService;

    public CambiarAConciliadorHandler(
            ConciliadorRepository conciliadorRepository,
            PerfilCambioDatosService perfilCambioDatosService,
            PerfilCambioDuplicadosService perfilCambioDuplicadosService) {
        this.conciliadorRepository = conciliadorRepository;
        this.perfilCambioDatosService = perfilCambioDatosService;
        this.perfilCambioDuplicadosService = perfilCambioDuplicadosService;
    }

    @Override
    public TipoPerfilUsuario getTipoPerfil() {
        // Indica que este handler solo maneja cambios hacia CONCILIADOR.
        return TipoPerfilUsuario.CONCILIADOR;
    }

    @Override
    public Class<CambiarPerfilAConciliadorDTO> getDtoClass() {
        // Indica qué DTO espera este handler.
        // Esto permite validar que no se use por error un DTO de otro perfil.
        return CambiarPerfilAConciliadorDTO.class;
    }

    @Override
    public ResultadoCambioPerfil crearOActualizarPerfil(
            UsuarioSistema usuarioSistema,
            CambiarPerfilAConciliadorDTO dto) {

        // Valida que se haya enviado el tipo de conciliador.
        // Este dato es propio del perfil CONCILIADOR.
        validarTipoConciliador(dto.getTipoConciliador());

        // Normaliza los datos comunes del perfil:
        // nombre, documento, email, teléfono, usuario y código.
        // En conciliador el documento es obligatorio, por eso el último parámetro es true.
        DatosPerfilCambioNormalizados datos =
                perfilCambioDatosService.normalizarDatosBasicos(
                        dto,
                        usuarioSistema,
                        true
                );

        // Obtiene el tipo de documento desde el catálogo.
        // En este flujo se permite opcional.
        TipoDocumento tipoDocumento =
                perfilCambioDatosService.obtenerTipoDocumentoOpcional(dto.getTipoDocumentoId());

        // Obtiene la sede desde el catálogo.
        // En este flujo se permite opcional.
        Sede sede =
                perfilCambioDatosService.obtenerSedeOpcional(dto.getSedeId());

        // Si el usuario ya tuvo un perfil conciliador, se reutiliza ese registro.
        // Si nunca lo tuvo, se crea uno nuevo.
        Conciliador conciliador = conciliadorRepository
                .findByUsuarioSistema_Id(usuarioSistema.getId())
                .orElseGet(Conciliador::new);

        // Valida duplicados usando el servicio centralizado.
        // Si el conciliador es nuevo, valida contra todos.
        // Si ya existe, permite conservar sus mismos datos y solo bloquea
        // si documento, email, teléfono, usuario o código pertenecen a otro registro.
        perfilCambioDuplicadosService.validarConciliador(
                conciliador.getId(),
                datos
        );

        // Se asocia el perfil conciliador al mismo UsuarioSistema.
        // Es la misma cuenta; lo que cambia es el perfil/rol activo, no el usuario de login.
        conciliador.setUsuarioSistema(usuarioSistema);
        conciliador.setNombre(datos.getNombre());
        conciliador.setTipoDocumento(tipoDocumento);
        conciliador.setDocumento(datos.getDocumento());
        conciliador.setEmail(datos.getEmail());
        conciliador.setTelefono(datos.getTelefono());
        conciliador.setUsuario(datos.getUsuario());
        conciliador.setCodigo(datos.getCodigo());
        conciliador.setSede(sede);
        conciliador.setTipoConciliador(dto.getTipoConciliador());
        conciliador.setActivo(true);

        Conciliador conciliadorGuardado = conciliadorRepository.save(conciliador);

        // Devuelve solo lo necesario para que el orquestador sepa
        // cuál perfil quedó activo después del cambio.
        return new ResultadoCambioPerfil(
                conciliadorGuardado.getId(),
                TipoPerfilUsuario.CONCILIADOR
        );
    }

    private void validarTipoConciliador(TipoConciliador tipoConciliador) {
        // El tipo de conciliador es obligatorio porque define
        // la clasificación específica del perfil conciliador.
        if (tipoConciliador == null) {
            throw new BusinessException("El tipo de conciliador es obligatorio");
        }
    }
}