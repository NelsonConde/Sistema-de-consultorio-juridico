package co.edu.ufps.legal_cases.security.service.access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.dto.access.RolDTO;
import co.edu.ufps.legal_cases.security.model.access.CodigoRolBase;
import co.edu.ufps.legal_cases.security.model.access.Rol;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.repository.access.PermisoRepository;
import co.edu.ufps.legal_cases.security.repository.access.RolRepository;
import co.edu.ufps.legal_cases.security.service.access.permiso.PermisoMapper;
import co.edu.ufps.legal_cases.security.service.access.rol.RolMapper;
import co.edu.ufps.legal_cases.security.service.access.rol.RolValidator;
import co.edu.ufps.legal_cases.security.service.invariant.administracion.AdministracionInvariantService;

class RolServiceTipoPerfilTest {

    private RolRepository rolRepository;
    private PermisoRepository permisoRepository;
    private AdministracionInvariantService invariantService;

    private RolService service;

    @BeforeEach
    void setUp() {
        rolRepository = mock(RolRepository.class);
        permisoRepository = mock(PermisoRepository.class);
        invariantService = mock(AdministracionInvariantService.class);

        PermisoMapper permisoMapper = mock(PermisoMapper.class);
        RolMapper rolMapper = new RolMapper(permisoMapper);
        RolValidator rolValidator = new RolValidator();

        service = new RolService(
                rolRepository,
                permisoRepository,
                rolMapper,
                rolValidator,
                invariantService);

        when(rolRepository.save(any(Rol.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void creaRolesConCadaTipoPerfilValido() {
        for (TipoPerfilUsuario tipoPerfil : TipoPerfilUsuario.values()) {
            RolDTO dto = new RolDTO();
            dto.setNombre("Rol " + tipoPerfil.name());
            dto.setDescripcion("Rol de prueba " + tipoPerfil.name());
            dto.setActivo(true);
            dto.setTipoPerfil(tipoPerfil);
            dto.setPermisoIds(Set.of());

            RolDTO creado = service.crear(dto);

            assertEquals(
                    tipoPerfil,
                    creado.getTipoPerfil());

            assertNull(
                    creado.getCodigoBase());

            assertTrue(
                    creado.getActivo());
        }
    }

    @Test
    void rechazaCreacionSinTipoPerfil() {
        RolDTO dto = new RolDTO();
        dto.setNombre("Rol sin tipo");
        dto.setDescripcion("Rol de prueba");
        dto.setActivo(true);
        dto.setPermisoIds(Set.of());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.crear(dto));

        assertTrue(
                exception.getMessage()
                        .toLowerCase()
                        .contains("tipo de perfil"));

        verifyNoInteractions(
                rolRepository,
                permisoRepository,
                invariantService);
    }

    @Test
    void rechazaCambioDeTipoPerfil() {
        Rol existente = new Rol();
        existente.setId(10L);
        existente.setNombre("Asesor institucional");
        existente.setDescripcion("Rol asesor");
        existente.setActivo(true);
        existente.setTipoPerfil(TipoPerfilUsuario.ASESOR);
        existente.setPermisos(Set.of());

        when(rolRepository.findWithPermisosById(10L))
                .thenReturn(Optional.of(existente));

        RolDTO dto = new RolDTO();
        dto.setId(10L);
        dto.setNombre("Asesor institucional");
        dto.setDescripcion("Rol asesor");
        dto.setActivo(true);
        dto.setTipoPerfil(TipoPerfilUsuario.ESTUDIANTE);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.actualizar(10L, dto));

        assertTrue(
                exception.getMessage()
                        .toLowerCase()
                        .contains("tipo de perfil"));

        assertEquals(
                TipoPerfilUsuario.ASESOR,
                existente.getTipoPerfil());

        verify(rolRepository, never())
                .save(any(Rol.class));
    }

    @Test
    void permiteRenombrarRolBaseConservandoIdentidadYTipoPerfil() {
        Rol administrador = new Rol();
        administrador.setId(1L);
        administrador.setNombre("Administrador");
        administrador.setDescripcion("Rol administrador del sistema");
        administrador.setActivo(true);
        administrador.setTipoPerfil(TipoPerfilUsuario.ADMINISTRATIVO);
        administrador.setCodigoBase(CodigoRolBase.ADMINISTRADOR);
        administrador.setPermisos(Set.of());

        when(rolRepository.findWithPermisosById(1L))
                .thenReturn(Optional.of(administrador));

        RolDTO dto = new RolDTO();
        dto.setId(1L);
        dto.setNombre("Gestor institucional");
        dto.setDescripcion("Rol administrador del sistema");
        dto.setActivo(true);
        dto.setTipoPerfil(TipoPerfilUsuario.ADMINISTRATIVO);

        RolDTO actualizado = service.actualizar(
                1L,
                dto);

        assertEquals(
                "Gestor institucional",
                actualizado.getNombre());

        assertEquals(
                TipoPerfilUsuario.ADMINISTRATIVO,
                actualizado.getTipoPerfil());

        assertEquals(
                CodigoRolBase.ADMINISTRADOR,
                actualizado.getCodigoBase());

        assertEquals(
                CodigoRolBase.ADMINISTRADOR,
                administrador.getCodigoBase());

        assertEquals(
                TipoPerfilUsuario.ADMINISTRATIVO,
                administrador.getTipoPerfil());

        verify(rolRepository)
                .save(administrador);
    }

    @Test
    void rechazaDesactivacionDeRolBase() {
        Rol administrador = new Rol();
        administrador.setId(1L);
        administrador.setNombre("Gestor institucional");
        administrador.setDescripcion("Rol administrador");
        administrador.setActivo(true);
        administrador.setTipoPerfil(TipoPerfilUsuario.ADMINISTRATIVO);
        administrador.setCodigoBase(CodigoRolBase.ADMINISTRADOR);
        administrador.setPermisos(Set.of());

        when(rolRepository.findWithPermisosById(1L))
                .thenReturn(Optional.of(administrador));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.cambiarEstado(1L, false));

        assertTrue(
                exception.getMessage()
                        .toLowerCase()
                        .contains("rol base"));

        assertTrue(
                administrador.getActivo());

        verify(rolRepository, never())
                .save(any(Rol.class));
    }

    @Test
    void ignoraCodigoBaseEnDatosRecibidosDelCliente() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        String json = """
                {
                    "nombre": "Rol personalizado",
                    "tipoPerfil": "ASESOR",
                    "codigoBase": "ADMINISTRADOR"
                }
                """;

        RolDTO dto = objectMapper.readValue(
                json,
                RolDTO.class);

        assertEquals(
                TipoPerfilUsuario.ASESOR,
                dto.getTipoPerfil());

        assertNull(
                dto.getCodigoBase());
    }
}