package co.edu.ufps.legal_cases.security.service.account.cambio;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.model.catalogo.Area;
import co.edu.ufps.legal_cases.business.model.catalogo.Sede;
import co.edu.ufps.legal_cases.business.model.catalogo.TipoDocumento;
import co.edu.ufps.legal_cases.business.model.perfil.Asesor;
import co.edu.ufps.legal_cases.business.repository.perfil.AsesorRepository;
import co.edu.ufps.legal_cases.security.dto.account.cambio.CambiarPerfilAAsesorDTO;
import co.edu.ufps.legal_cases.security.dto.account.cambio.DatosPerfilCambioNormalizados;
import co.edu.ufps.legal_cases.security.dto.account.cambio.ResultadoCambioPerfil;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;

// Servicio concreto para cambiar un usuario al perfil ASESOR.
// Se encarga de crear, actualizar o reactivar el perfil asesor
// de un usuario que está cambiando de rol.
@Component
@Transactional
public class CambiarAAsesorHandler
        implements PerfilCambioHandler<CambiarPerfilAAsesorDTO> {

    private final AsesorRepository asesorRepository;
    private final PerfilCambioDatosService perfilCambioDatosService;
    private final PerfilCambioDuplicadosService perfilCambioDuplicadosService;

    public CambiarAAsesorHandler(
            AsesorRepository asesorRepository,
            PerfilCambioDatosService perfilCambioDatosService,
            PerfilCambioDuplicadosService perfilCambioDuplicadosService) {
        this.asesorRepository = asesorRepository;
        this.perfilCambioDatosService = perfilCambioDatosService;
        this.perfilCambioDuplicadosService = perfilCambioDuplicadosService;
    }

    @Override
    public TipoPerfilUsuario getTipoPerfil() {
        // Indica que este handler solo maneja cambios hacia ASESOR.
        return TipoPerfilUsuario.ASESOR;
    }

    @Override
    public Class<CambiarPerfilAAsesorDTO> getDtoClass() {
        // Indica qué DTO espera este handler.
        // Esto permite validar que no se use por error un DTO de otro perfil.
        return CambiarPerfilAAsesorDTO.class;
    }

    @Override
    public ResultadoCambioPerfil crearOActualizarPerfil(
            UsuarioSistema usuarioSistema,
            CambiarPerfilAAsesorDTO dto) {

        // Normaliza los datos comunes del perfil:
        // nombre, documento, email, teléfono, usuario y código.
        // En asesor el documento es obligatorio, por eso el último parámetro es true.
        DatosPerfilCambioNormalizados datos =
                perfilCambioDatosService.normalizarDatosBasicos(
                        dto,
                        usuarioSistema,
                        true
                );

        // Obtiene el tipo de documento desde el catálogo.
        // En asesor es obligatorio.
        TipoDocumento tipoDocumento =
                perfilCambioDatosService.obtenerTipoDocumentoObligatorio(dto.getTipoDocumentoId());

        // Obtiene la sede desde el catálogo.
        // En asesor es obligatoria.
        Sede sede =
                perfilCambioDatosService.obtenerSedeObligatoria(dto.getSedeId());

        // Obtiene el área desde el catálogo.
        // En asesor es obligatoria porque el asesor debe pertenecer a un área.
        Area area =
                perfilCambioDatosService.obtenerAreaObligatoria(dto.getAreaId());

        // Si el usuario ya tuvo un perfil asesor, se reutiliza ese registro.
        // Si nunca lo tuvo, se crea uno nuevo.
        Asesor asesor = asesorRepository
                .findByUsuarioSistema_Id(usuarioSistema.getId())
                .orElseGet(Asesor::new);

        // Valida duplicados usando el servicio centralizado.
        // Si el asesor es nuevo, valida contra todos.
        // Si ya existe, permite conservar sus mismos datos y solo bloquea
        // si documento, email, teléfono, usuario o código pertenecen a otro registro.
        perfilCambioDuplicadosService.validarAsesor(
                asesor.getId(),
                datos
        );

        // Se asocia el perfil asesor al mismo UsuarioSistema.
        // Es la misma cuenta; lo que cambia es el perfil/rol activo, no el usuario de login.
        asesor.setUsuarioSistema(usuarioSistema);
        asesor.setNombre(datos.getNombre());
        asesor.setTipoDocumento(tipoDocumento);
        asesor.setDocumento(datos.getDocumento());
        asesor.setEmail(datos.getEmail());
        asesor.setTelefono(datos.getTelefono());
        asesor.setUsuario(datos.getUsuario());
        asesor.setCodigo(datos.getCodigo());
        asesor.setSede(sede);
        asesor.setArea(area);
        asesor.setActivo(true);

        Asesor asesorGuardado = asesorRepository.save(asesor);

        // Devuelve solo lo necesario para que el orquestador sepa
        // cuál perfil quedó activo después del cambio.
        return new ResultadoCambioPerfil(
                asesorGuardado.getId(),
                TipoPerfilUsuario.ASESOR
        );
    }
}