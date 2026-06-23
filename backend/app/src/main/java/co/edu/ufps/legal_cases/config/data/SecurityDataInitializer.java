package co.edu.ufps.legal_cases.config.data;

import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarTexto;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import co.edu.ufps.legal_cases.security.constant.PermisoNombre;
import co.edu.ufps.legal_cases.security.model.access.Permiso;
import co.edu.ufps.legal_cases.security.model.access.Rol;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.repository.access.PermisoRepository;
import co.edu.ufps.legal_cases.security.repository.access.RolRepository;

@Configuration
public class SecurityDataInitializer {

    private static final Logger log = LoggerFactory.getLogger(SecurityDataInitializer.class);

    @Bean
    // Debe ejecutarse antes de inicializadores que dependan de roles o permisos.
    @Order(1)
    CommandLineRunner initSecurityData(
            PermisoRepository permisoRepository,
            RolRepository rolRepository) {
        return args -> inicializarDatosMinimos(permisoRepository, rolRepository);
    }

    /**
     * Inicializa datos mínimos de seguridad sin sobrescribir la matriz real de permisos.
     *
     * Importante:
     * - No borra permisos.
     * - No reemplaza relaciones rol_permiso.
     * - No reasigna permisos a roles existentes.
     * - Solo crea permisos o roles base cuando no existen.
     *
     * La matriz rol-permiso se administra desde BD o desde el módulo de roles.
     */
    private void inicializarDatosMinimos(
            PermisoRepository permisoRepository,
            RolRepository rolRepository) {

        crearPermisosDeclaradosEnCodigo(permisoRepository);
        crearRolesBaseSiNoExisten(rolRepository);
    }

    private void crearPermisosDeclaradosEnCodigo(PermisoRepository permisoRepository) {
        obtenerNombresPermisosDeclarados()
                .forEach(nombre -> crearPermisoSiNoExiste(permisoRepository, nombre));
    }

    private List<String> obtenerNombresPermisosDeclarados() {
        return Arrays.stream(PermisoNombre.class.getDeclaredFields())
                .filter(field -> Modifier.isPublic(field.getModifiers()))
                .filter(field -> Modifier.isStatic(field.getModifiers()))
                .filter(field -> Modifier.isFinal(field.getModifiers()))
                .filter(field -> field.getType().equals(String.class))
                .map(field -> {
                    try {
                        return (String) field.get(null);
                    } catch (IllegalAccessException e) {
                        throw new IllegalStateException("No se pudo leer un permiso declarado en PermisoNombre", e);
                    }
                })
                .distinct()
                .sorted()
                .toList();
    }

    private Permiso crearPermisoSiNoExiste(
            PermisoRepository permisoRepository,
            String nombre) {

        String nombreNormalizado = normalizarTexto(nombre);

        return permisoRepository.findByNombreIgnoreCase(nombreNormalizado)
                .orElseGet(() -> {
                    Permiso permiso = new Permiso();
                    permiso.setNombre(nombreNormalizado);
                    permiso.setDescripcion("Permiso del sistema: " + nombreNormalizado);
                    permiso.setActivo(true);

                    log.info("Creando permiso faltante: {}", nombreNormalizado);
                    return permisoRepository.save(permiso);
                });
    }

    private void crearRolesBaseSiNoExisten(RolRepository rolRepository) {
        crearRolSiNoExiste(
                rolRepository,
                "Administrador",
                "Rol administrador del sistema",
                TipoPerfilUsuario.ADMINISTRATIVO);

        crearRolSiNoExiste(
                rolRepository,
                "Asesor",
                "Rol asesor del consultorio juridico",
                TipoPerfilUsuario.ASESOR);

        crearRolSiNoExiste(
                rolRepository,
                "Estudiante",
                "Rol estudiante del consultorio juridico",
                TipoPerfilUsuario.ESTUDIANTE);

        crearRolSiNoExiste(
                rolRepository,
                "Monitor",
                "Rol monitor del consultorio juridico",
                TipoPerfilUsuario.MONITOR);

        crearRolSiNoExiste(
                rolRepository,
                "Conciliador",
                "Rol conciliador del consultorio juridico",
                TipoPerfilUsuario.CONCILIADOR);
    }

    private Rol crearRolSiNoExiste(
            RolRepository rolRepository,
            String nombre,
            String descripcion,
            TipoPerfilUsuario tipoPerfil) {

        String nombreNormalizado = normalizarTexto(nombre);
        String descripcionNormalizada = normalizarTexto(descripcion);

        return rolRepository.findByNombreIgnoreCase(nombreNormalizado)
                .map(rolExistente -> completarTipoPerfilSiFalta(rolRepository, rolExistente, tipoPerfil))
                .orElseGet(() -> {
                    Rol rol = new Rol();
                    rol.setNombre(nombreNormalizado);
                    rol.setDescripcion(descripcionNormalizada);
                    rol.setTipoPerfil(tipoPerfil);
                    rol.setActivo(true);

                    log.info("Creando rol base faltante: {}", nombreNormalizado);
                    return rolRepository.save(rol);
                });
    }

    private Rol completarTipoPerfilSiFalta(
            RolRepository rolRepository,
            Rol rol,
            TipoPerfilUsuario tipoPerfil) {

        if (rol.getTipoPerfil() != null) {
            return rol;
        }

        rol.setTipoPerfil(tipoPerfil);
        log.info("Completando tipoPerfil faltante para rol: {}", rol.getNombre());
        return rolRepository.save(rol);
    }
}