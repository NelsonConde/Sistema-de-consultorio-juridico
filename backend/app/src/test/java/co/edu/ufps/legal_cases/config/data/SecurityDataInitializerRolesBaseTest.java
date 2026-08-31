package co.edu.ufps.legal_cases.config.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.CommandLineRunner;

import co.edu.ufps.legal_cases.security.model.access.CodigoRolBase;
import co.edu.ufps.legal_cases.security.model.access.Permiso;
import co.edu.ufps.legal_cases.security.model.access.Rol;
import co.edu.ufps.legal_cases.security.repository.access.PermisoRepository;
import co.edu.ufps.legal_cases.security.repository.access.RolRepository;

class SecurityDataInitializerRolesBaseTest {

    private PermisoRepository permisoRepository;
    private RolRepository rolRepository;

    private SecurityDataInitializer initializer;

    @BeforeEach
    void setUp() {
        permisoRepository =
                mock(PermisoRepository.class);

        rolRepository =
                mock(RolRepository.class);

        initializer =
                new SecurityDataInitializer();

        /*
         * Los permisos no forman parte de estas pruebas.
         * Se simulan como existentes para aislar exclusivamente
         * la inicialización de los roles base.
         */
        when(permisoRepository.findByNombreIgnoreCase(anyString()))
                .thenReturn(Optional.of(new Permiso()));
    }

    @Test
    void reinicioNoDuplicaRolesBaseRenombrados()
            throws Exception {

        Rol administrador = crearRolBase(
                CodigoRolBase.ADMINISTRADOR,
                "Gestor institucional");

        Rol asesor = crearRolBase(
                CodigoRolBase.ASESOR,
                "Asesor jurídico");

        Rol estudiante = crearRolBase(
                CodigoRolBase.ESTUDIANTE,
                "Estudiante");

        Rol monitor = crearRolBase(
                CodigoRolBase.MONITOR,
                "Monitor");

        Rol conciliador = crearRolBase(
                CodigoRolBase.CONCILIADOR,
                "Conciliador");

        when(rolRepository.findByCodigoBase(
                CodigoRolBase.ADMINISTRADOR))
                .thenReturn(Optional.of(administrador));

        when(rolRepository.findByCodigoBase(
                CodigoRolBase.ASESOR))
                .thenReturn(Optional.of(asesor));

        when(rolRepository.findByCodigoBase(
                CodigoRolBase.ESTUDIANTE))
                .thenReturn(Optional.of(estudiante));

        when(rolRepository.findByCodigoBase(
                CodigoRolBase.MONITOR))
                .thenReturn(Optional.of(monitor));

        when(rolRepository.findByCodigoBase(
                CodigoRolBase.CONCILIADOR))
                .thenReturn(Optional.of(conciliador));

        CommandLineRunner runner =
                initializer.initSecurityData(
                        permisoRepository,
                        rolRepository);

        runner.run();

        verify(rolRepository)
                .findByCodigoBase(
                        CodigoRolBase.ADMINISTRADOR);

        verify(rolRepository)
                .findByCodigoBase(
                        CodigoRolBase.ASESOR);

        verify(rolRepository)
                .findByCodigoBase(
                        CodigoRolBase.ESTUDIANTE);

        verify(rolRepository)
                .findByCodigoBase(
                        CodigoRolBase.MONITOR);

        verify(rolRepository)
                .findByCodigoBase(
                        CodigoRolBase.CONCILIADOR);

        /*
         * Cuando los códigos base existen, ningún nombre se utiliza
         * para resolver la identidad de los roles.
         */
        verify(rolRepository, never())
                .findByNombreIgnoreCase(anyString());

        /*
         * Los roles existentes tampoco deben recrearse ni modificarse.
         */
        verify(rolRepository, never())
                .save(any(Rol.class));
    }

    @Test
    void baseNuevaCreaLosCincoRolesBase()
            throws Exception {

        when(rolRepository.findByCodigoBase(
                any(CodigoRolBase.class)))
                .thenReturn(Optional.empty());

        when(rolRepository.findByNombreIgnoreCase(
                anyString()))
                .thenReturn(Optional.empty());

        when(rolRepository.save(any(Rol.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CommandLineRunner runner =
                initializer.initSecurityData(
                        permisoRepository,
                        rolRepository);

        runner.run();

        ArgumentCaptor<Rol> captor =
                ArgumentCaptor.forClass(Rol.class);

        verify(
                rolRepository,
                times(5))
                .save(captor.capture());

        List<Rol> rolesCreados =
                captor.getAllValues();

        assertEquals(
                5,
                rolesCreados.size());

        for (Rol rol : rolesCreados) {
            assertTrue(
                    rol.getActivo());

            assertEquals(
                    rol.getCodigoBase().getTipoPerfil(),
                    rol.getTipoPerfil());
        }

        assertTrue(
                rolesCreados.stream()
                        .anyMatch(rol ->
                                rol.getCodigoBase()
                                        == CodigoRolBase.ADMINISTRADOR));

        assertTrue(
                rolesCreados.stream()
                        .anyMatch(rol ->
                                rol.getCodigoBase()
                                        == CodigoRolBase.ASESOR));

        assertTrue(
                rolesCreados.stream()
                        .anyMatch(rol ->
                                rol.getCodigoBase()
                                        == CodigoRolBase.ESTUDIANTE));

        assertTrue(
                rolesCreados.stream()
                        .anyMatch(rol ->
                                rol.getCodigoBase()
                                        == CodigoRolBase.MONITOR));

        assertTrue(
                rolesCreados.stream()
                        .anyMatch(rol ->
                                rol.getCodigoBase()
                                        == CodigoRolBase.CONCILIADOR));
    }

    @Test
    void rechazaRolLegacySinCodigoBase()
            throws Exception {

        when(rolRepository.findByCodigoBase(
                CodigoRolBase.ADMINISTRADOR))
                .thenReturn(Optional.empty());

        Rol rolLegacy = new Rol();
        rolLegacy.setId(1L);
        rolLegacy.setNombre("Administrador");
        rolLegacy.setTipoPerfil(
                CodigoRolBase.ADMINISTRADOR.getTipoPerfil());
        rolLegacy.setActivo(true);

        when(rolRepository.findByNombreIgnoreCase(
                "Administrador"))
                .thenReturn(Optional.of(rolLegacy));

        CommandLineRunner runner =
                initializer.initSecurityData(
                        permisoRepository,
                        rolRepository);

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        runner::run);

        assertTrue(
                exception.getMessage()
                        .contains("adopción"));

        verify(rolRepository, never())
                .save(any(Rol.class));
    }

    @Test
    void rechazaRolBaseConTipoPerfilIncompatible()
            throws Exception {

        Rol administradorInconsistente =
                new Rol();

        administradorInconsistente.setNombre(
                "Gestor institucional");

        administradorInconsistente.setCodigoBase(
                CodigoRolBase.ADMINISTRADOR);

        administradorInconsistente.setTipoPerfil(
                CodigoRolBase.ASESOR.getTipoPerfil());

        administradorInconsistente.setActivo(true);

        when(rolRepository.findByCodigoBase(
                CodigoRolBase.ADMINISTRADOR))
                .thenReturn(
                        Optional.of(
                                administradorInconsistente));

        CommandLineRunner runner =
                initializer.initSecurityData(
                        permisoRepository,
                        rolRepository);

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        runner::run);

        assertTrue(
                exception.getMessage()
                        .contains("tipoPerfil incompatible"));

        verify(rolRepository, never())
                .save(any(Rol.class));
    }

    private Rol crearRolBase(
            CodigoRolBase codigoBase,
            String nombre) {

        Rol rol = new Rol();

        rol.setNombre(nombre);
        rol.setDescripcion("Rol base de prueba");
        rol.setActivo(true);
        rol.setCodigoBase(codigoBase);
        rol.setTipoPerfil(
                codigoBase.getTipoPerfil());

        return rol;
    }
}