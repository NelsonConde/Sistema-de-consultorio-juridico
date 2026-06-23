package co.edu.ufps.legal_cases.security.service.account.cambio;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.model.catalogo.Sede;
import co.edu.ufps.legal_cases.business.model.catalogo.TipoDocumento;
import co.edu.ufps.legal_cases.business.model.perfil.Asesor;
import co.edu.ufps.legal_cases.business.model.perfil.Estudiante;
import co.edu.ufps.legal_cases.business.repository.perfil.AsesorRepository;
import co.edu.ufps.legal_cases.business.repository.perfil.EstudianteRepository;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.dto.account.cambio.CambiarPerfilAEstudianteDTO;
import co.edu.ufps.legal_cases.security.dto.account.cambio.DatosPerfilCambioNormalizados;
import co.edu.ufps.legal_cases.security.dto.account.cambio.ResultadoCambioPerfil;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;

// Servicio concreto para cambiar un usuario al perfil ESTUDIANTE.
// Se encarga de crear, actualizar o reactivar el perfil estudiante
// de un usuario que está cambiando de rol.
@Component
@Transactional
public class CambiarAEstudianteHandler
        implements PerfilCambioHandler<CambiarPerfilAEstudianteDTO> {

    private final EstudianteRepository estudianteRepository;
    private final AsesorRepository asesorRepository;
    private final PerfilCambioDatosService perfilCambioDatosService;
    private final PerfilCambioDuplicadosService perfilCambioDuplicadosService;

    public CambiarAEstudianteHandler(
            EstudianteRepository estudianteRepository,
            AsesorRepository asesorRepository,
            PerfilCambioDatosService perfilCambioDatosService,
            PerfilCambioDuplicadosService perfilCambioDuplicadosService) {
        this.estudianteRepository = estudianteRepository;
        this.asesorRepository = asesorRepository;
        this.perfilCambioDatosService = perfilCambioDatosService;
        this.perfilCambioDuplicadosService = perfilCambioDuplicadosService;
    }

    @Override
    public TipoPerfilUsuario getTipoPerfil() {
        // Indica que este handler solo maneja cambios hacia ESTUDIANTE.
        return TipoPerfilUsuario.ESTUDIANTE;
    }

    @Override
    public Class<CambiarPerfilAEstudianteDTO> getDtoClass() {
        // Indica qué DTO espera este handler.
        // Esto permite validar que no se use por error un DTO de otro perfil.
        return CambiarPerfilAEstudianteDTO.class;
    }

    @Override
    public ResultadoCambioPerfil crearOActualizarPerfil(
            UsuarioSistema usuarioSistema,
            CambiarPerfilAEstudianteDTO dto) {

        // Normaliza los datos comunes del perfil:
        // nombre, documento, email, teléfono, usuario y código.
        // En estudiante el documento es obligatorio, por eso el último parámetro es true.
        DatosPerfilCambioNormalizados datos =
                perfilCambioDatosService.normalizarDatosBasicos(
                        dto,
                        usuarioSistema,
                        true
                );

        // Obtiene el tipo de documento desde el catálogo.
        // En estudiante es obligatorio.
        TipoDocumento tipoDocumento =
                perfilCambioDatosService.obtenerTipoDocumentoObligatorio(dto.getTipoDocumentoId());

        // Obtiene la sede desde el catálogo.
        // En estudiante es obligatoria.
        Sede sede =
                perfilCambioDatosService.obtenerSedeObligatoria(dto.getSedeId());

        // Todo estudiante debe estar asociado a un asesor activo.
        Asesor asesor = obtenerAsesorActivo(dto.getAsesorId());

        // Si el usuario ya tuvo un perfil estudiante, se reutiliza ese registro.
        // Si nunca lo tuvo, se crea uno nuevo.
        Estudiante estudiante = estudianteRepository
                .findByUsuarioSistema_Id(usuarioSistema.getId())
                .orElseGet(Estudiante::new);

        // Valida duplicados usando el servicio centralizado.
        // Si el estudiante es nuevo, valida contra todos.
        // Si ya existe, permite conservar sus mismos datos y solo bloquea
        // si documento, email, teléfono, usuario o código pertenecen a otro registro.
        perfilCambioDuplicadosService.validarEstudiante(
                estudiante.getId(),
                datos
        );

        // Se asocia el perfil estudiante al mismo UsuarioSistema.
        // Es la misma cuenta; lo que cambia es el perfil/rol activo, no el usuario de login.
        estudiante.setUsuarioSistema(usuarioSistema);
        estudiante.setNombre(datos.getNombre());
        estudiante.setTipoDocumento(tipoDocumento);
        estudiante.setDocumento(datos.getDocumento());
        estudiante.setEmail(datos.getEmail());
        estudiante.setTelefono(datos.getTelefono());
        estudiante.setUsuario(datos.getUsuario());
        estudiante.setCodigo(datos.getCodigo());
        estudiante.setSede(sede);
        estudiante.setAsesor(asesor);
        estudiante.setActivo(true);
        estudiante.setConciliacion(dto.getConciliacion() != null ? dto.getConciliacion() : false);

        Estudiante estudianteGuardado = estudianteRepository.save(estudiante);

        // Devuelve solo lo necesario para que el orquestador sepa
        // cuál perfil quedó activo después del cambio.
        return new ResultadoCambioPerfil(
                estudianteGuardado.getId(),
                TipoPerfilUsuario.ESTUDIANTE
        );
    }

    private Asesor obtenerAsesorActivo(Long asesorId) {
        // El asesor es obligatorio porque un estudiante debe quedar asociado
        // a un asesor activo.
        if (asesorId == null) {
            throw new BusinessException("El asesor es obligatorio");
        }

        // Busca el asesor y valida que esté activo.
        // Si no existe o está inactivo, no se permite crear/reactivar el estudiante.
        return asesorRepository.findByIdAndActivoTrue(asesorId)
                .orElseThrow(() -> new BusinessException(
                        "Asesor no encontrado o inactivo con id: " + asesorId));
    }
}