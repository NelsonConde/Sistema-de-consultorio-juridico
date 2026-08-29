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
import co.edu.ufps.legal_cases.security.model.access.CodigoRolBase;
import co.edu.ufps.legal_cases.security.model.access.Permiso;
import co.edu.ufps.legal_cases.security.model.access.Rol;
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
     * Inicializa datos mínimos de seguridad sin sobrescribir la matriz real de
     * permisos.
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
                        throw new IllegalStateException(
                                "No se pudo leer un permiso declarado en PermisoNombre",
                                e);
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
        crearRolBaseSiNoExiste(
                rolRepository,
                CodigoRolBase.ADMINISTRADOR,
                "Administrador",
                "Rol administrador del sistema");

        crearRolBaseSiNoExiste(
                rolRepository,
                CodigoRolBase.ASESOR,
                "Asesor",
                "Rol asesor del consultorio juridico");

        crearRolBaseSiNoExiste(
                rolRepository,
                CodigoRolBase.ESTUDIANTE,
                "Estudiante",
                "Rol estudiante del consultorio juridico");

        crearRolBaseSiNoExiste(
                rolRepository,
                CodigoRolBase.MONITOR,
                "Monitor",
                "Rol monitor del consultorio juridico");

        crearRolBaseSiNoExiste(
                rolRepository,
                CodigoRolBase.CONCILIADOR,
                "Conciliador",
                "Rol conciliador del consultorio juridico");
    }

    /**
     * Resuelve los roles base mediante su código estable.
     *
     * El nombre solo se utiliza como valor inicial al crear el rol en una base
     * nueva. Una vez creado, el rol base se identifica únicamente mediante
     * CodigoRolBase, por lo que puede renombrarse sin perder su identidad.
     *
     * Una base existente que todavía no haya adoptado codigo_base no se corrige
     * automáticamente por nombre. En ese caso se falla de forma explícita para
     * evitar adoptar o duplicar una fila incorrecta.
     */
    private Rol crearRolBaseSiNoExiste(
            RolRepository rolRepository,
            CodigoRolBase codigoBase,
            String nombre,
            String descripcion) {

        String nombreNormalizado = normalizarTexto(nombre);
        String descripcionNormalizada = normalizarTexto(descripcion);

        return rolRepository.findByCodigoBase(codigoBase)
                .map(rolExistente -> validarRolBaseExistente(
                        rolRepository,
                        rolExistente,
                        codigoBase))
                .orElseGet(() -> {

                    /*
                     * Si el nombre esperado ya existe pero no tiene el código base,
                     * la BD requiere la adopción explícita de SEC-10.
                     *
                     * No se adopta automáticamente por nombre porque el nombre es
                     * una etiqueta editable y no una identidad funcional.
                     */
                    rolRepository.findByNombreIgnoreCase(nombreNormalizado)
                            .ifPresent(rolExistente -> {
                                throw new IllegalStateException(
                                        "SEC-10: existe el rol '"
                                                + nombreNormalizado
                                                + "' pero no está identificado como "
                                                + codigoBase
                                                + ". Debe ejecutarse la adopción de roles base "
                                                + "antes de iniciar la aplicación.");
                            });

                    Rol rol = new Rol();
                    rol.setNombre(nombreNormalizado);
                    rol.setDescripcion(descripcionNormalizada);
                    rol.setTipoPerfil(codigoBase.getTipoPerfil());
                    rol.setCodigoBase(codigoBase);
                    rol.setActivo(true);

                    log.info(
                            "Creando rol base faltante: codigo={}, tipoPerfil={}",
                            codigoBase,
                            codigoBase.getTipoPerfil());

                    return rolRepository.save(rol);
                });
    }

    /**
     * Verifica que la identidad estable del rol base sea coherente con el
     * tipo de perfil definido por el sistema.
     *
     * No modifica nombre, descripción ni permisos del rol existente.
     */
    private Rol validarRolBaseExistente(
            RolRepository rolRepository,
            Rol rol,
            CodigoRolBase codigoBase) {

        if (rol.getTipoPerfil() != codigoBase.getTipoPerfil()) {
            throw new IllegalStateException(
                    "SEC-10: el rol base "
                            + codigoBase
                            + " tiene un tipoPerfil incompatible. Esperado: "
                            + codigoBase.getTipoPerfil()
                            + ", encontrado: "
                            + rol.getTipoPerfil());
        }

        /*
         * Un rol base forma parte de la configuración mínima requerida
         * por el sistema y debe permanecer activo.
         */
        if (Boolean.FALSE.equals(rol.getActivo())) {
            rol.setActivo(true);

            log.warn(
                    "Reactivando rol base requerido por el sistema: {}",
                    codigoBase);

            return rolRepository.save(rol);
        }

        return rol;
    }
}