package co.edu.ufps.legal_cases.security.service.account.usuario;

import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarEmail;
import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarNumeroDocumento;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.model.perfil.Administrativo;
import co.edu.ufps.legal_cases.business.model.perfil.Asesor;
import co.edu.ufps.legal_cases.business.model.perfil.Conciliador;
import co.edu.ufps.legal_cases.business.model.perfil.Estudiante;
import co.edu.ufps.legal_cases.business.model.perfil.Monitor;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.model.access.Rol;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.repository.access.RolRepository;
import co.edu.ufps.legal_cases.security.repository.account.UsuarioSistemaRepository;

// Service interno para crear usuarios del sistema a partir de perfiles reales del negocio.
//Por esto no uso dto, sino directamente las entidades de negocio. 
//Lo uso en los servicios de cada perfil (AsesorService, EstudianteService, etc) para crear el usuario del sistema justo después de crear el perfil real.
@Service
@Transactional
public class UsuarioSistemaRegistroService {

    private static final String ROL_ASESOR = "Asesor";
    private static final String ROL_ESTUDIANTE = "Estudiante";
    private static final String ROL_MONITOR = "Monitor";
    private static final String ROL_ADMINISTRADOR = "Administrador";
    private static final String ROL_CONCILIADOR = "Conciliador";

    private final UsuarioSistemaRepository usuarioSistemaRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioSistemaRegistroService(
            UsuarioSistemaRepository usuarioSistemaRepository,
            RolRepository rolRepository,
            PasswordEncoder passwordEncoder) {
        this.usuarioSistemaRepository = usuarioSistemaRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UsuarioSistema crearParaAsesor(Asesor asesor) {
        validarPerfilAsesor(asesor);

        return crearYGuardarUsuarioBase(
                asesor.getEmail(),
                asesor.getDocumento(),
                ROL_ASESOR,
                TipoPerfilUsuario.ASESOR
        );
    }

    public UsuarioSistema crearParaEstudiante(Estudiante estudiante) {
        validarPerfilEstudiante(estudiante);

        return crearYGuardarUsuarioBase(
                estudiante.getEmail(),
                estudiante.getDocumento(),
                ROL_ESTUDIANTE,
                TipoPerfilUsuario.ESTUDIANTE
        );
    }

    public UsuarioSistema crearParaMonitor(Monitor monitor) {
        validarPerfilMonitor(monitor);

        return crearYGuardarUsuarioBase(
                monitor.getEmail(),
                monitor.getDocumento(),
                ROL_MONITOR,
                TipoPerfilUsuario.MONITOR
        );
    }

    public UsuarioSistema crearParaAdministrativo(Administrativo administrativo) {
        validarPerfilAdministrativo(administrativo);

        return crearYGuardarUsuarioBase(
                administrativo.getEmail(),
                administrativo.getDocumento(),
                ROL_ADMINISTRADOR,
                TipoPerfilUsuario.ADMINISTRATIVO
        );
    }

    public UsuarioSistema crearParaConciliador(Conciliador conciliador) {
        validarPerfilConciliador(conciliador);

        return crearYGuardarUsuarioBase(
                conciliador.getEmail(),
                conciliador.getDocumento(),
                ROL_CONCILIADOR,
                TipoPerfilUsuario.CONCILIADOR
        );
    }

    private UsuarioSistema crearYGuardarUsuarioBase(
            String email,
            String documento,
            String nombreRol,
            TipoPerfilUsuario tipoPerfilActual) {

        UsuarioSistema usuario = crearUsuarioBase(
                email,
                documento,
                nombreRol,
                tipoPerfilActual
        );

        // Arriba se creo pero aqui es que se guarda
        return usuarioSistemaRepository.save(usuario);
    }

    // Aqui se crea el objeto pero no se guarda
    private UsuarioSistema crearUsuarioBase(
            String email,
            String documento,
            String nombreRol,
            TipoPerfilUsuario tipoPerfilActual) {

        String username = normalizarEmail(email);
        String passwordInicial = normalizarNumeroDocumento(documento);
        Rol rol = obtenerRolBaseActivo(nombreRol);

        if (username == null) {
            throw new BusinessException("El correo es obligatorio para crear el usuario del sistema");
        }

        if (passwordInicial == null) {
            throw new BusinessException("El documento es obligatorio para crear la contraseña inicial");
        }

        if (tipoPerfilActual == null) {
            throw new BusinessException("El tipo de perfil actual es obligatorio para crear el usuario del sistema");
        }

        if (usuarioSistemaRepository.existsByUsernameIgnoreCase(username)) {
            throw new BusinessException("Ya existe un usuario del sistema con ese correo");
        }

        UsuarioSistema usuario = new UsuarioSistema();
        usuario.setUsername(username);

        // La contraseña inicial es el documento, pero se guarda cifrada.
        usuario.setPasswordHash(passwordEncoder.encode(passwordInicial));

        usuario.setRol(rol);
        usuario.setActivo(true);

        // Nuevo modelo de normalización.
        // Define cuál es el perfil real activo del usuario sin depender
        // de asesor_id, estudiante_id, monitor_id, administrativo_id o conciliador_id.
        usuario.setTipoPerfilActual(tipoPerfilActual);

        return usuario;
    }

    private Rol obtenerRolBaseActivo(String nombreRol) {
        return rolRepository.findByNombreIgnoreCaseAndActivoTrue(nombreRol)
                .orElseThrow(() -> new BusinessException(
                        "Rol base no encontrado o inactivo: " + nombreRol));
    }

    private void validarPerfilAsesor(Asesor asesor) {
        if (asesor == null || asesor.getId() == null) {
            throw new BusinessException("El asesor es obligatorio para crear el usuario del sistema");
        }

        if (asesor.getUsuarioSistema() != null) {
            throw new BusinessException("Ese asesor ya tiene usuario del sistema");
        }
    }

    private void validarPerfilEstudiante(Estudiante estudiante) {
        if (estudiante == null || estudiante.getId() == null) {
            throw new BusinessException("El estudiante es obligatorio para crear el usuario del sistema");
        }

        if (estudiante.getUsuarioSistema() != null) {
            throw new BusinessException("Ese estudiante ya tiene usuario del sistema");
        }
    }

    private void validarPerfilMonitor(Monitor monitor) {
        if (monitor == null || monitor.getId() == null) {
            throw new BusinessException("El monitor es obligatorio para crear el usuario del sistema");
        }

        if (monitor.getUsuarioSistema() != null) {
            throw new BusinessException("Ese monitor ya tiene usuario del sistema");
        }
    }

    private void validarPerfilAdministrativo(Administrativo administrativo) {
        if (administrativo == null || administrativo.getId() == null) {
            throw new BusinessException("El administrativo es obligatorio para crear el usuario del sistema");
        }

        if (administrativo.getUsuarioSistema() != null) {
            throw new BusinessException("Ese administrativo ya tiene usuario del sistema");
        }
    }

    private void validarPerfilConciliador(Conciliador conciliador) {
        if (conciliador == null || conciliador.getId() == null) {
            throw new BusinessException("El conciliador es obligatorio para crear el usuario del sistema");
        }

        if (conciliador.getUsuarioSistema() != null) {
            throw new BusinessException("Ese conciliador ya tiene usuario del sistema");
        }
    }
}