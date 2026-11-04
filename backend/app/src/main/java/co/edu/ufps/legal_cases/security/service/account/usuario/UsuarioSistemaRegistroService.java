package co.edu.ufps.legal_cases.security.service.account.usuario;

import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarEmail;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.model.perfil.Administrativo;
import co.edu.ufps.legal_cases.business.model.perfil.Asesor;
import co.edu.ufps.legal_cases.business.model.perfil.Conciliador;
import co.edu.ufps.legal_cases.business.model.perfil.Estudiante;
import co.edu.ufps.legal_cases.business.model.perfil.Monitor;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.model.access.CodigoRolBase;
import co.edu.ufps.legal_cases.security.model.access.Rol;
import co.edu.ufps.legal_cases.security.model.account.EstadoActivacionCuenta;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.repository.access.RolRepository;
import co.edu.ufps.legal_cases.security.repository.account.UsuarioSistemaRepository;

// Service interno para crear usuarios del sistema a partir de perfiles reales del negocio.
// Por esto no uso dto, sino directamente las entidades de negocio.
// Lo uso en los servicios de cada perfil (AsesorService, EstudianteService, etc.)
// para crear el usuario del sistema justo después de crear el perfil real.
@Service
@Transactional
public class UsuarioSistemaRegistroService {

    private final UsuarioSistemaRepository usuarioSistemaRepository;
    private final RolRepository rolRepository;

    public UsuarioSistemaRegistroService(
            UsuarioSistemaRepository usuarioSistemaRepository,
            RolRepository rolRepository) {

        this.usuarioSistemaRepository = usuarioSistemaRepository;
        this.rolRepository = rolRepository;
    }

    public UsuarioSistema crearParaAsesor(Asesor asesor) {
        validarPerfilAsesor(asesor);

        return crearYGuardarUsuarioBase(
                asesor.getEmail(),
                CodigoRolBase.ASESOR);
    }

    public UsuarioSistema crearParaEstudiante(Estudiante estudiante) {
        validarPerfilEstudiante(estudiante);

        return crearYGuardarUsuarioBase(
                estudiante.getEmail(),
                CodigoRolBase.ESTUDIANTE);
    }

    public UsuarioSistema crearParaMonitor(Monitor monitor) {
        validarPerfilMonitor(monitor);

        return crearYGuardarUsuarioBase(
                monitor.getEmail(),
                CodigoRolBase.MONITOR);
    }

    public UsuarioSistema crearParaAdministrativo(Administrativo administrativo) {
        validarPerfilAdministrativo(administrativo);

        return crearYGuardarUsuarioBase(
                administrativo.getEmail(),
                CodigoRolBase.ADMINISTRADOR);
    }

    public UsuarioSistema crearParaConciliador(Conciliador conciliador) {
        validarPerfilConciliador(conciliador);

        return crearYGuardarUsuarioBase(
                conciliador.getEmail(),
                CodigoRolBase.CONCILIADOR);
    }

    private UsuarioSistema crearYGuardarUsuarioBase(
            String email,
            CodigoRolBase codigoRolBase) {

        UsuarioSistema usuario = crearUsuarioBase(
                email,
                codigoRolBase);

        // Arriba se creó el objeto pero aquí es donde se guarda.
        return usuarioSistemaRepository.save(usuario);
    }

    // Aquí se crea el objeto pero no se guarda.
    private UsuarioSistema crearUsuarioBase(
            String email,
            CodigoRolBase codigoRolBase) {

        if (codigoRolBase == null) {
            throw new BusinessException(
                    "El código del rol base es obligatorio para crear el usuario del sistema");
        }

        String username = normalizarEmail(email);

        if (username == null) {
            throw new BusinessException(
                    "El correo es obligatorio para crear el usuario del sistema");
        }

        if (usuarioSistemaRepository.existsByUsernameIgnoreCase(username)) {
            throw new BusinessException(
                    "Ya existe un usuario del sistema con ese correo");
        }

        Rol rol = obtenerRolBaseActivo(codigoRolBase);

        UsuarioSistema usuario = new UsuarioSistema();
        usuario.setUsername(username);

        /*
         * La cuenta se crea sin secreto inicial. La contraseña únicamente
         * podrá establecerse mediante el flujo seguro de activación.
         */
        usuario.setPasswordHash(null);
        usuario.setEstadoActivacion(EstadoActivacionCuenta.PENDIENTE);

        usuario.setRol(rol);
        usuario.setActivo(true);

        /*
         * Define cuál es el perfil real activo del usuario sin depender
         * de asesor_id, estudiante_id, monitor_id, administrativo_id
         * o conciliador_id.
         *
         * El tipo se obtiene de la misma identidad estable del rol base,
         * evitando mantener por separado dos valores que podrían ser incompatibles.
         */
        usuario.setTipoPerfilActual(codigoRolBase.getTipoPerfil());

        return usuario;
    }

    /**
     * Obtiene el rol base mediante su identidad estable y verifica
     * su compatibilidad con el tipo de perfil definido por el sistema.
     *
     * El nombre del rol no participa en esta resolución.
     */
    private Rol obtenerRolBaseActivo(CodigoRolBase codigoRolBase) {
        Rol rol = rolRepository.findByCodigoBaseAndActivoTrue(codigoRolBase)
                .orElseThrow(() -> new BusinessException(
                        "Rol base no encontrado o inactivo: " + codigoRolBase));

        if (rol.getTipoPerfil() != codigoRolBase.getTipoPerfil()) {
            throw new BusinessException(
                    "El rol base "
                            + codigoRolBase
                            + " no corresponde al tipo de perfil esperado: "
                            + codigoRolBase.getTipoPerfil());
        }

        return rol;
    }

    private void validarPerfilAsesor(Asesor asesor) {
        if (asesor == null || asesor.getId() == null) {
            throw new BusinessException(
                    "El asesor es obligatorio para crear el usuario del sistema");
        }

        if (asesor.getUsuarioSistema() != null) {
            throw new BusinessException(
                    "Ese asesor ya tiene usuario del sistema");
        }
    }

    private void validarPerfilEstudiante(Estudiante estudiante) {
        if (estudiante == null || estudiante.getId() == null) {
            throw new BusinessException(
                    "El estudiante es obligatorio para crear el usuario del sistema");
        }

        if (estudiante.getUsuarioSistema() != null) {
            throw new BusinessException(
                    "Ese estudiante ya tiene usuario del sistema");
        }
    }

    private void validarPerfilMonitor(Monitor monitor) {
        if (monitor == null || monitor.getId() == null) {
            throw new BusinessException(
                    "El monitor es obligatorio para crear el usuario del sistema");
        }

        if (monitor.getUsuarioSistema() != null) {
            throw new BusinessException(
                    "Ese monitor ya tiene usuario del sistema");
        }
    }

    private void validarPerfilAdministrativo(Administrativo administrativo) {
        if (administrativo == null || administrativo.getId() == null) {
            throw new BusinessException(
                    "El administrativo es obligatorio para crear el usuario del sistema");
        }

        if (administrativo.getUsuarioSistema() != null) {
            throw new BusinessException(
                    "Ese administrativo ya tiene usuario del sistema");
        }
    }

    private void validarPerfilConciliador(Conciliador conciliador) {
        if (conciliador == null || conciliador.getId() == null) {
            throw new BusinessException(
                    "El conciliador es obligatorio para crear el usuario del sistema");
        }

        if (conciliador.getUsuarioSistema() != null) {
            throw new BusinessException(
                    "Ese conciliador ya tiene usuario del sistema");
        }
    }
}