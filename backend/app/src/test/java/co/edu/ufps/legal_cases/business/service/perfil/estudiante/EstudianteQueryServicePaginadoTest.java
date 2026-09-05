package co.edu.ufps.legal_cases.business.service.perfil.estudiante;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;

import co.edu.ufps.legal_cases.business.dto.perfil.EstudianteDTO;
import co.edu.ufps.legal_cases.business.dto.perfil.EstudianteResumenDTO;
import co.edu.ufps.legal_cases.business.model.perfil.Estudiante;
import co.edu.ufps.legal_cases.business.repository.perfil.EstudianteRepository;
import co.edu.ufps.legal_cases.business.repository.perfil.EstudianteResumenProjection;
import co.edu.ufps.legal_cases.business.service.acceso.perfil.EstudianteAccessService;
import co.edu.ufps.legal_cases.common.dto.PageResponseDTO;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

class EstudianteQueryServicePaginadoTest {

    private EstudianteRepository estudianteRepository;
    private EstudianteAccessService estudianteAccessService;
    private EstudianteMapper estudianteMapper;
    private EstudianteQueryService estudianteQueryService;

    @BeforeEach
    void setUp() {
        estudianteRepository = mock(EstudianteRepository.class);
        estudianteAccessService = mock(EstudianteAccessService.class);
        estudianteMapper = mock(EstudianteMapper.class);

        estudianteQueryService = new EstudianteQueryService(
                estudianteRepository,
                estudianteAccessService,
                estudianteMapper);
    }

    @Test
    void buscarGlobalDebeValidarAccesoYUsarScopeNullConPaginaInternaCero() {
        PageRequest esperado = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("id")));
        when(estudianteAccessService.puedeVerTodosLosEstudiantes()).thenReturn(true);
        when(estudianteRepository.buscarResumenPaginado(isNull(), isNull(), isNull(), eq(esperado)))
                .thenReturn(new PageImpl<>(List.of(), esperado, 0));

        estudianteQueryService.buscar(null, 1, 10, "id", "desc", null);

        verify(estudianteAccessService).validarPuedeListarEstudiantes();
        verify(estudianteRepository).buscarResumenPaginado(isNull(), isNull(), isNull(), eq(esperado));
    }

    @Test
    void buscarAsesorDebeEnviarElIdDelPerfilRealComoScope() {
        PageRequest esperado = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("id")));
        when(estudianteAccessService.puedeVerTodosLosEstudiantes()).thenReturn(false);
        when(estudianteAccessService.usuarioEsAsesor()).thenReturn(true);
        when(estudianteAccessService.obtenerAsesorActualId()).thenReturn(99L);
        when(estudianteRepository.buscarResumenPaginado(isNull(), isNull(), eq(99L), eq(esperado)))
                .thenReturn(new PageImpl<>(List.of(), esperado, 0));

        estudianteQueryService.buscar(null, 1, 10, "id", "desc", null);

        verify(estudianteAccessService).validarPuedeListarEstudiantes();
        verify(estudianteAccessService).obtenerAsesorActualId();
        verify(estudianteRepository).buscarResumenPaginado(isNull(), isNull(), eq(99L), eq(esperado));
    }

    @Test
    void otroPerfilAutorizadoNoDebeRecibirAccesoNiConsultarRepository() {
        when(estudianteAccessService.puedeVerTodosLosEstudiantes()).thenReturn(false);
        when(estudianteAccessService.usuarioEsAsesor()).thenReturn(false);

        PageResponseDTO<EstudianteResumenDTO> resultado =
                estudianteQueryService.buscar(null, 2, 20, "id", "desc", null);

        assertTrue(resultado.content().isEmpty());
        assertEquals(2, resultado.page());
        assertEquals(20, resultado.size());
        assertEquals(0, resultado.totalElements());
        assertEquals(0, resultado.totalPages());
        verify(estudianteRepository, never()).buscarResumenPaginado(any(), any(), any(), any());
    }

    @Test
    void idDeAsesorNoResueltoDebeFallarCerradoSinScopeGlobalAccidental() {
        when(estudianteAccessService.puedeVerTodosLosEstudiantes()).thenReturn(false);
        when(estudianteAccessService.usuarioEsAsesor()).thenReturn(true);
        when(estudianteAccessService.obtenerAsesorActualId()).thenReturn(null);

        assertThrows(
                AccessDeniedException.class,
                () -> estudianteQueryService.buscar(null, 1, 10, "id", "desc", null));

        verify(estudianteRepository, never()).buscarResumenPaginado(any(), any(), any(), any());
    }

    @Test
    void autorizacionDebeOcurrirAntesDeConsultarRepository() {
        doThrow(new AccessDeniedException("denegado"))
                .when(estudianteAccessService).validarPuedeListarEstudiantes();

        assertThrows(
                AccessDeniedException.class,
                () -> estudianteQueryService.buscar(null, 1, 10, "id", "desc", null));

        verify(estudianteRepository, never()).buscarResumenPaginado(any(), any(), any(), any());
    }

    @Test
    void debeRechazarPaginacionSearchSortYDirectionInvalidos() {
        assertThrows(BusinessException.class,
                () -> estudianteQueryService.buscar(null, 0, 10, "id", "desc", null));
        assertThrows(BusinessException.class,
                () -> estudianteQueryService.buscar(null, 1, 0, "id", "desc", null));
        assertThrows(BusinessException.class,
                () -> estudianteQueryService.buscar(null, 1, 51, "id", "desc", null));
        assertThrows(BusinessException.class,
                () -> estudianteQueryService.buscar("x".repeat(101), 1, 10, "id", "desc", null));
        assertThrows(BusinessException.class,
                () -> estudianteQueryService.buscar(null, 1, 10, "telefono", "desc", null));
        assertThrows(BusinessException.class,
                () -> estudianteQueryService.buscar(null, 1, 10, null, "desc", null));
        assertThrows(BusinessException.class,
                () -> estudianteQueryService.buscar(null, 1, 10, "   ", "desc", null));
        assertThrows(BusinessException.class,
                () -> estudianteQueryService.buscar(null, 1, 10, "id", "lateral", null));
        assertThrows(BusinessException.class,
                () -> estudianteQueryService.buscar(null, 1, 10, "id", null, null));
        assertThrows(BusinessException.class,
                () -> estudianteQueryService.buscar(null, 1, 10, "id", "   ", null));

        verify(estudianteRepository, never()).buscarResumenPaginado(
                any(), any(), any(), any(Pageable.class));
    }

    @Test
    void parametrosInvalidosDebenRechazarseAntesDeResolverScopeSinVisibilidad() {
        when(estudianteAccessService.puedeVerTodosLosEstudiantes()).thenReturn(false);
        when(estudianteAccessService.usuarioEsAsesor()).thenReturn(false);

        assertThrows(BusinessException.class,
                () -> estudianteQueryService.buscar(null, 0, 10, "id", "desc", null));

        verify(estudianteAccessService).validarPuedeListarEstudiantes();
        verify(estudianteAccessService, never()).puedeVerTodosLosEstudiantes();
        verify(estudianteAccessService, never()).usuarioEsAsesor();
        verify(estudianteRepository, never()).buscarResumenPaginado(any(), any(), any(), any());
    }

    @Test
    void debeNormalizarSearchYTratarBlankComoNull() {
        stubGlobalVacio();

        estudianteQueryService.buscar("  juan   estudiante  ", 1, 10, "id", "desc", null);
        estudianteQueryService.buscar("   ", 1, 10, "id", "desc", null);

        verify(estudianteRepository).buscarResumenPaginado(
                eq("juan estudiante"), isNull(), isNull(), any(Pageable.class));
        verify(estudianteRepository).buscarResumenPaginado(
                isNull(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void sortTextoDebeMapearRelacionAplicarIgnoreCaseYDesempatarPorId() {
        stubGlobalVacio();

        estudianteQueryService.buscar(null, 1, 10, "  nombre  ", " ASC ", null);
        estudianteQueryService.buscar(null, 1, 10, "asesorNombre", "desc", null);

        List<PageRequest> pageables = capturarPageables(2);
        List<Sort.Order> nombreAsc = pageables.get(0).getSort().stream().toList();
        assertEquals("nombre", nombreAsc.get(0).getProperty());
        assertEquals(Sort.Direction.ASC, nombreAsc.get(0).getDirection());
        assertTrue(nombreAsc.get(0).isIgnoreCase());
        assertEquals(Sort.Order.asc("id"), nombreAsc.get(1));

        List<Sort.Order> asesorDesc = pageables.get(1).getSort().stream().toList();
        assertEquals("asesor.nombre", asesorDesc.get(0).getProperty());
        assertEquals(Sort.Direction.DESC, asesorDesc.get(0).getDirection());
        assertTrue(asesorDesc.get(0).isIgnoreCase());
        assertEquals(Sort.Order.asc("id"), asesorDesc.get(1));
    }

    @Test
    void sortNoTextoNuncaDebeAplicarIgnoreCase() {
        stubGlobalVacio();

        estudianteQueryService.buscar(null, 1, 10, "id", "asc", null);
        estudianteQueryService.buscar(null, 1, 10, "activo", "desc", null);
        estudianteQueryService.buscar(null, 1, 10, "conciliacion", "asc", null);

        List<PageRequest> pageables = capturarPageables(3);
        List<Sort.Order> id = pageables.get(0).getSort().stream().toList();
        assertEquals(1, id.size());
        assertFalse(id.get(0).isIgnoreCase());

        List<Sort.Order> activo = pageables.get(1).getSort().stream().toList();
        assertFalse(activo.get(0).isIgnoreCase());
        assertEquals(Sort.Order.asc("id"), activo.get(1));

        List<Sort.Order> conciliacion = pageables.get(2).getSort().stream().toList();
        assertFalse(conciliacion.get(0).isIgnoreCase());
        assertEquals(Sort.Order.asc("id"), conciliacion.get(1));
    }

    @Test
    void debePropagarActivoTrueYFalse() {
        stubGlobalVacio();

        estudianteQueryService.buscar(null, 1, 10, "id", "desc", true);
        estudianteQueryService.buscar(null, 1, 10, "id", "desc", false);

        verify(estudianteRepository).buscarResumenPaginado(isNull(), eq(true), isNull(), any(Pageable.class));
        verify(estudianteRepository).buscarResumenPaginado(isNull(), eq(false), isNull(), any(Pageable.class));
    }

    @Test
    void debeConservarMetadataYMapearCadaProjectionAResumen() {
        when(estudianteAccessService.puedeVerTodosLosEstudiantes()).thenReturn(true);
        EstudianteResumenProjection projection = mock(EstudianteResumenProjection.class);
        EstudianteResumenDTO dto = new EstudianteResumenDTO();
        PageRequest pageable = PageRequest.of(1, 10, Sort.by(Sort.Order.desc("id")));
        when(estudianteRepository.buscarResumenPaginado(isNull(), isNull(), isNull(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(projection), pageable, 21));
        when(estudianteMapper.convertirAResumenDTO(projection)).thenReturn(dto);

        PageResponseDTO<EstudianteResumenDTO> resultado =
                estudianteQueryService.buscar(null, 2, 10, "id", "desc", null);

        assertEquals(List.of(dto), resultado.content());
        assertEquals(2, resultado.page());
        assertEquals(10, resultado.size());
        assertEquals(21, resultado.totalElements());
        assertEquals(3, resultado.totalPages());
        verify(estudianteMapper).convertirAResumenDTO(projection);
    }

    @Test
    void mapperDebeConvertirTodosLosCamposDeProjectionAResumen() {
        EstudianteMapper mapper = new EstudianteMapper();
        EstudianteResumenProjection projection = mock(EstudianteResumenProjection.class);
        when(projection.getId()).thenReturn(1L);
        when(projection.getNombre()).thenReturn("Estudiante A");
        when(projection.getDocumento()).thenReturn("DOC-1");
        when(projection.getEmail()).thenReturn("estudiante@example.test");
        when(projection.getUsuario()).thenReturn("estudiante.a");
        when(projection.getCodigo()).thenReturn("EST-1");
        when(projection.getActivo()).thenReturn(true);
        when(projection.getSedeId()).thenReturn(2L);
        when(projection.getSedeNombre()).thenReturn("Principal");
        when(projection.getAsesorId()).thenReturn(3L);
        when(projection.getAsesorNombre()).thenReturn("Asesor A");
        when(projection.getConciliacion()).thenReturn(true);

        EstudianteResumenDTO dto = mapper.convertirAResumenDTO(projection);

        assertEquals(1L, dto.getId());
        assertEquals("Estudiante A", dto.getNombre());
        assertEquals("DOC-1", dto.getDocumento());
        assertEquals("estudiante@example.test", dto.getEmail());
        assertEquals("estudiante.a", dto.getUsuario());
        assertEquals("EST-1", dto.getCodigo());
        assertEquals(true, dto.getActivo());
        assertEquals(2L, dto.getSedeId());
        assertEquals("Principal", dto.getSedeNombre());
        assertEquals(3L, dto.getAsesorId());
        assertEquals("Asesor A", dto.getAsesorNombre());
        assertEquals(true, dto.getConciliacion());
    }

    @Test
    void listarLegadoDebePreservarVistaGlobal() {
        Estudiante estudiante = mock(Estudiante.class);
        EstudianteDTO dto = new EstudianteDTO();
        when(estudianteAccessService.puedeVerTodosLosEstudiantes()).thenReturn(true);
        when(estudianteRepository.findAll()).thenReturn(List.of(estudiante));
        when(estudianteMapper.convertirADTO(estudiante)).thenReturn(dto);

        assertEquals(List.of(dto), estudianteQueryService.listar());

        verify(estudianteAccessService).validarPuedeListarEstudiantes();
        verify(estudianteRepository).findAll();
    }

    @Test
    void listarLegadoDebePreservarScopeDelAsesor() {
        Estudiante estudiante = mock(Estudiante.class);
        EstudianteDTO dto = new EstudianteDTO();
        when(estudianteAccessService.puedeVerTodosLosEstudiantes()).thenReturn(false);
        when(estudianteAccessService.usuarioEsAsesor()).thenReturn(true);
        when(estudianteAccessService.obtenerAsesorActualId()).thenReturn(99L);
        when(estudianteRepository.findByAsesorIdAndActivoTrue(99L)).thenReturn(List.of(estudiante));
        when(estudianteMapper.convertirADTO(estudiante)).thenReturn(dto);

        assertEquals(List.of(dto), estudianteQueryService.listar());

        verify(estudianteRepository).findByAsesorIdAndActivoTrue(99L);
        verify(estudianteRepository, never()).findAll();
    }

    private void stubGlobalVacio() {
        when(estudianteAccessService.puedeVerTodosLosEstudiantes()).thenReturn(true);
        when(estudianteRepository.buscarResumenPaginado(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));
    }

    private List<PageRequest> capturarPageables(int cantidad) {
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(estudianteRepository, times(cantidad))
                .buscarResumenPaginado(any(), any(), any(), captor.capture());
        return captor.getAllValues().stream()
                .map(PageRequest.class::cast)
                .toList();
    }
}
