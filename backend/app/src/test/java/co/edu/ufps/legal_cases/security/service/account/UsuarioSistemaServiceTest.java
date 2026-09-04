package co.edu.ufps.legal_cases.security.service.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import co.edu.ufps.legal_cases.common.dto.PageResponseDTO;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.dto.account.UsuarioSistemaResumenDTO;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.repository.account.UsuarioSistemaRepository;
import co.edu.ufps.legal_cases.security.repository.account.UsuarioSistemaResumenProjection;
import co.edu.ufps.legal_cases.security.service.account.perfil.PerfilUsuarioResolverService;
import co.edu.ufps.legal_cases.security.service.account.usuario.UsuarioSistemaMapper;
import co.edu.ufps.legal_cases.security.service.account.usuario.UsuarioSistemaValidator;
import co.edu.ufps.legal_cases.security.service.invariant.administracion.AdministracionInvariantService;

class UsuarioSistemaServiceTest {

    private UsuarioSistemaRepository usuarioSistemaRepository;
    private UsuarioSistemaMapper usuarioSistemaMapper;
    private UsuarioSistemaService usuarioSistemaService;

    @BeforeEach
    void setUp() {
        usuarioSistemaRepository = mock(UsuarioSistemaRepository.class);
        usuarioSistemaMapper = mock(UsuarioSistemaMapper.class);
        UsuarioSistemaValidator usuarioSistemaValidator = mock(UsuarioSistemaValidator.class);
        AdministracionInvariantService administracionInvariantService = mock(AdministracionInvariantService.class);

        usuarioSistemaService = new UsuarioSistemaService(
                usuarioSistemaRepository,
                usuarioSistemaMapper,
                usuarioSistemaValidator,
                administracionInvariantService);
    }

    @Test
    void buscarDebeUsarDefaultsValidosYConvertirPaginaPublicaUnoAPageRequestCero() {
        PageRequest esperado = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("id")));
        when(usuarioSistemaRepository.buscarResumenPaginado(
                isNull(), isNull(), isNull(), eq(esperado)))
                .thenReturn(new PageImpl<>(List.of(), esperado, 0));

        usuarioSistemaService.buscar(null, 1, 10, "id", "desc", null, null);

        verify(usuarioSistemaRepository).buscarResumenPaginado(
                isNull(), isNull(), isNull(), eq(esperado));
    }

    @Test
    void debeConservarMetadatosDePageResponseDTOYMapearContenido() {
        UsuarioSistemaResumenProjection projection = mock(UsuarioSistemaResumenProjection.class);
        UsuarioSistemaResumenDTO dto = new UsuarioSistemaResumenDTO();
        PageRequest pageable = PageRequest.of(1, 10, Sort.by(Sort.Order.desc("id")));

        when(usuarioSistemaRepository.buscarResumenPaginado(
                isNull(), isNull(), isNull(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(projection), pageable, 21));
        when(usuarioSistemaMapper.convertirAResumenDTO(projection)).thenReturn(dto);

        PageResponseDTO<UsuarioSistemaResumenDTO> resultado =
                usuarioSistemaService.buscar(null, 2, 10, "id", "desc", null, null);

        assertEquals(List.of(dto), resultado.content());
        assertEquals(2, resultado.page());
        assertEquals(10, resultado.size());
        assertEquals(21, resultado.totalElements());
        assertEquals(3, resultado.totalPages());
    }

    @Test
    void debeRechazarPageMenorAUno() {
        assertThrows(BusinessException.class,
                () -> usuarioSistemaService.buscar(null, 0, 10, "id", "desc", null, null));

        verify(usuarioSistemaRepository, never())
                .buscarResumenPaginado(any(), any(), any(), any(Pageable.class));
    }

    @Test
    void debeRechazarSizeMenorAUno() {
        assertThrows(BusinessException.class,
                () -> usuarioSistemaService.buscar(null, 1, 0, "id", "desc", null, null));

        verify(usuarioSistemaRepository, never())
                .buscarResumenPaginado(any(), any(), any(), any(Pageable.class));
    }

    @Test
    void debeRechazarSizeMayorACincuenta() {
        assertThrows(BusinessException.class,
                () -> usuarioSistemaService.buscar(null, 1, 51, "id", "desc", null, null));

        verify(usuarioSistemaRepository, never())
                .buscarResumenPaginado(any(), any(), any(), any(Pageable.class));
    }

    @Test
    void debeNormalizarSearchYTratarBlankComoNull() {
        stubEmpty();

        usuarioSistemaService.buscar("  admin   juridico  ", 1, 10, "id", "desc", null, null);

        verify(usuarioSistemaRepository).buscarResumenPaginado(
                eq("admin juridico"), isNull(), isNull(), any(Pageable.class));

        usuarioSistemaService.buscar("   ", 1, 10, "id", "desc", null, null);

        verify(usuarioSistemaRepository).buscarResumenPaginado(
                isNull(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void debeRechazarSearchMayorACienCaracteres() {
        assertThrows(BusinessException.class,
                () -> usuarioSistemaService.buscar("x".repeat(101), 1, 10, "id", "desc", null, null));

        verify(usuarioSistemaRepository, never())
                .buscarResumenPaginado(any(), any(), any(), any(Pageable.class));
    }

    @Test
    void sortByPermitidoDebeMapearPropiedadJpaYAgregarDesempate() {
        stubEmpty();

        usuarioSistemaService.buscar(null, 1, 10, "rolNombre", "asc", null, null);

        List<Sort.Order> ordenes = capturarPageable().getSort().stream().toList();
        assertEquals(2, ordenes.size());
        assertEquals("rol.nombre", ordenes.get(0).getProperty());
        assertEquals(Sort.Direction.ASC, ordenes.get(0).getDirection());
        assertTrue(ordenes.get(0).isIgnoreCase());
        assertEquals("id", ordenes.get(1).getProperty());
        assertEquals(Sort.Direction.ASC, ordenes.get(1).getDirection());
    }

    @Test
    void sortByInvalidoDebeLanzarBusinessException() {
        assertThrows(BusinessException.class,
                () -> usuarioSistemaService.buscar(null, 1, 10, "passwordHash", "desc", null, null));
        assertThrows(BusinessException.class,
                () -> usuarioSistemaService.buscar(null, 1, 10, null, "desc", null, null));
        assertThrows(BusinessException.class,
                () -> usuarioSistemaService.buscar(null, 1, 10, "   ", "desc", null, null));

        verify(usuarioSistemaRepository, never())
                .buscarResumenPaginado(any(), any(), any(), any(Pageable.class));
    }

    @Test
    void directionAscYDescDebenSerValidas() {
        stubEmpty();

        usuarioSistemaService.buscar(null, 1, 10, "username", "ASC", null, null);
        usuarioSistemaService.buscar(null, 1, 10, "username", "desc", null, null);

        List<PageRequest> pageables = capturarDosPageables();
        List<Sort.Order> asc = pageables.get(0).getSort().stream().toList();
        assertEquals(Sort.Direction.ASC, asc.get(0).getDirection());

        List<Sort.Order> desc = pageables.get(1).getSort().stream().toList();
        assertEquals(Sort.Direction.DESC, desc.get(0).getDirection());
    }

    @Test
    void directionInvalidaDebeLanzarBusinessException() {
        assertThrows(BusinessException.class,
                () -> usuarioSistemaService.buscar(null, 1, 10, "id", "lateral", null, null));
        assertThrows(BusinessException.class,
                () -> usuarioSistemaService.buscar(null, 1, 10, "id", null, null, null));
        assertThrows(BusinessException.class,
                () -> usuarioSistemaService.buscar(null, 1, 10, "id", "   ", null, null));

        verify(usuarioSistemaRepository, never())
                .buscarResumenPaginado(any(), any(), any(), any(Pageable.class));
    }

    @Test
    void debePropagarFiltroActivo() {
        stubEmpty();

        usuarioSistemaService.buscar(null, 1, 10, "id", "desc", true, null);

        verify(usuarioSistemaRepository).buscarResumenPaginado(
                isNull(), eq(true), isNull(), any(Pageable.class));
    }

    @Test
    void debePropagarFiltroTipoPerfil() {
        stubEmpty();

        usuarioSistemaService.buscar(null, 1, 10, "id", "desc", null, TipoPerfilUsuario.ASESOR);

        verify(usuarioSistemaRepository).buscarResumenPaginado(
                isNull(), isNull(), eq(TipoPerfilUsuario.ASESOR), any(Pageable.class));
    }

    @Test
    void projectionDebeMapearADtoResumenSinResolverPerfilNiPermisos() {
        PerfilUsuarioResolverService perfilUsuarioResolverService = mock(PerfilUsuarioResolverService.class);
        UsuarioSistemaMapper mapper = new UsuarioSistemaMapper(perfilUsuarioResolverService);
        UsuarioSistemaResumenProjection projection = mock(UsuarioSistemaResumenProjection.class);
        when(projection.getId()).thenReturn(10L);
        when(projection.getUsername()).thenReturn("admin@example.test");
        when(projection.getActivo()).thenReturn(true);
        when(projection.getRolId()).thenReturn(20L);
        when(projection.getRolNombre()).thenReturn("Administrador");
        when(projection.getTipoPerfil()).thenReturn(TipoPerfilUsuario.ADMINISTRATIVO);

        UsuarioSistemaResumenDTO dto = mapper.convertirAResumenDTO(projection);

        assertEquals(10L, dto.getId());
        assertEquals("admin@example.test", dto.getUsername());
        assertEquals(true, dto.getActivo());
        assertEquals(20L, dto.getRolId());
        assertEquals("Administrador", dto.getRolNombre());
        assertEquals(TipoPerfilUsuario.ADMINISTRATIVO, dto.getTipoPerfil());
        verifyNoInteractions(perfilUsuarioResolverService);
    }

    private void stubEmpty() {
        when(usuarioSistemaRepository.buscarResumenPaginado(
                any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));
    }

    private PageRequest capturarPageable() {
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(usuarioSistemaRepository).buscarResumenPaginado(
                any(), any(), any(), captor.capture());
        return (PageRequest) captor.getValue();
    }

    private List<PageRequest> capturarDosPageables() {
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(usuarioSistemaRepository, times(2)).buscarResumenPaginado(
                any(), any(), any(), captor.capture());
        return captor.getAllValues().stream()
                .map(PageRequest.class::cast)
                .toList();
    }
}
