package co.edu.ufps.legal_cases.security.service.account.cambio;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.model.catalogo.Sede;
import co.edu.ufps.legal_cases.business.model.catalogo.TipoDocumento;
import co.edu.ufps.legal_cases.business.model.perfil.Administrativo;
import co.edu.ufps.legal_cases.business.repository.perfil.AdministrativoRepository;
import co.edu.ufps.legal_cases.security.dto.account.cambio.CambiarPerfilAAdministrativoDTO;
import co.edu.ufps.legal_cases.security.dto.account.cambio.DatosPerfilCambioNormalizados;
import co.edu.ufps.legal_cases.security.dto.account.cambio.ResultadoCambioPerfil;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;

// Servicio concreto para cambiar un usuario al perfil ADMINISTRATIVO.
// Se encarga de crear, actualizar o reactivar el perfil administrativo
// de un usuario que está cambiando de rol.
@Component
@Transactional
public class CambiarAAdministrativoHandler
        implements PerfilCambioHandler<CambiarPerfilAAdministrativoDTO> {

    private final AdministrativoRepository administrativoRepository;
    private final PerfilCambioDatosService perfilCambioDatosService;
    private final PerfilCambioDuplicadosService perfilCambioDuplicadosService;

    public CambiarAAdministrativoHandler(
            AdministrativoRepository administrativoRepository,
            PerfilCambioDatosService perfilCambioDatosService,
            PerfilCambioDuplicadosService perfilCambioDuplicadosService) {
        this.administrativoRepository = administrativoRepository;
        this.perfilCambioDatosService = perfilCambioDatosService;
        this.perfilCambioDuplicadosService = perfilCambioDuplicadosService;
    }

    @Override
    public TipoPerfilUsuario getTipoPerfil() {
        return TipoPerfilUsuario.ADMINISTRATIVO;
    }

    @Override
    public Class<CambiarPerfilAAdministrativoDTO> getDtoClass() {
        return CambiarPerfilAAdministrativoDTO.class;
    }

    @Override
    public ResultadoCambioPerfil crearOActualizarPerfil(
            UsuarioSistema usuarioSistema,
            CambiarPerfilAAdministrativoDTO dto) {

        // Normaliza los datos comunes del perfil:
        // nombre, documento, email, teléfono, usuario y código.
        // El email se toma desde el UsuarioSistema porque la cuenta sigue siendo la misma.
        DatosPerfilCambioNormalizados datos =
                perfilCambioDatosService.normalizarDatosBasicos(
                        dto,
                        usuarioSistema,
                        false
                );

        // Los datos ya están normalizados, pero los catálogos deben consultarse
        // como objetos reales para poder asignarlos a la entidad Administrativo.
        TipoDocumento tipoDocumento =
                perfilCambioDatosService.obtenerTipoDocumentoOpcional(dto.getTipoDocumentoId());

        Sede sede =
                perfilCambioDatosService.obtenerSedeOpcional(dto.getSedeId());

        // Si el usuario ya tuvo un perfil administrativo, se reutiliza ese registro.
        // Si nunca lo tuvo, se crea uno nuevo.
        Administrativo administrativo = administrativoRepository
                .findByUsuarioSistema_Id(usuarioSistema.getId())
                .orElseGet(Administrativo::new);

        // Valida duplicados usando el servicio centralizado.
        // Si el administrativo es nuevo, valida contra todos.
        // Si ya existe, permite conservar sus mismos datos y solo bloquea si
        // documento, email, teléfono, usuario o código pertenecen a otro registro.
        perfilCambioDuplicadosService.validarAdministrativo(
                administrativo.getId(),
                datos
        );

        // Se asocia el perfil administrativo al mismo UsuarioSistema.
        // Es la misma cuenta; lo que cambia es el perfil/rol activo, no el usuario de login.
        administrativo.setUsuarioSistema(usuarioSistema);
        administrativo.setNombre(datos.getNombre());
        administrativo.setTipoDocumento(tipoDocumento);
        administrativo.setDocumento(datos.getDocumento());
        administrativo.setEmail(datos.getEmail());
        administrativo.setTelefono(datos.getTelefono());
        administrativo.setUsuario(datos.getUsuario());
        administrativo.setCodigo(datos.getCodigo());
        administrativo.setSede(sede);
        administrativo.setActivo(true);
        administrativo.setDirectora(dto.getDirectora() != null ? dto.getDirectora() : false);

        Administrativo administrativoGuardado = administrativoRepository.save(administrativo);

        // Devuelve solo lo necesario para que el orquestador sepa
        // cuál perfil quedó activo después del cambio.
        return new ResultadoCambioPerfil(
                administrativoGuardado.getId(),
                TipoPerfilUsuario.ADMINISTRATIVO
        );
    }
}