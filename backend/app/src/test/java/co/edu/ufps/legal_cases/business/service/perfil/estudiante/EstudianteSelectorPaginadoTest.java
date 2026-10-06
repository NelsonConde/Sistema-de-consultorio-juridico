package co.edu.ufps.legal_cases.business.service.perfil.estudiante;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;

import co.edu.ufps.legal_cases.business.dto.perfil.EstudianteResumenDTO;
import co.edu.ufps.legal_cases.business.repository.perfil.EstudianteRepository;
import co.edu.ufps.legal_cases.business.repository.perfil.EstudianteResumenProjection;
import co.edu.ufps.legal_cases.business.service.acceso.perfil.EstudianteAccessService;
import co.edu.ufps.legal_cases.common.dto.PageResponseDTO;

class EstudianteSelectorPaginadoTest {

    private EstudianteRepository estudianteRepository;
    private EstudianteAccessService accessService;
    private EstudianteQueryService queryService;

    @BeforeEach
    void setUp() {
        estudianteRepository = mock(EstudianteRepository.class);
        accessService = mock(EstudianteAccessService.class);
        queryService = new EstudianteQueryService(
                estudianteRepository,
                accessService,
                mock(EstudianteMapper.class));
    }

    @Test
    void selectorActivosDebeBuscarPaginarYRestringirAActivos() {
        when(accessService.puedeVerTodosLosEstudiantes()).thenReturn(true);
        PageRequest interno = PageRequest.of(
                1,
                4,
                Sort.by(Sort.Order.asc("e.nombre").ignoreCase(), Sort.Order.asc("e.id")));
        when(estudianteRepository.buscarSelectorPaginado("Laura", null, null, interno))
                .thenReturn(new PageImpl<>(List.of(), interno, 7));

        PageResponseDTO<EstudianteResumenDTO> resultado =
                queryService.listarActivosPaginados(" Laura ", 2, 4, "nombre", "asc");

        assertEquals(2, resultado.page());
        assertEquals(4, resultado.size());
        assertEquals(7, resultado.totalElements());
        assertEquals(2, resultado.totalPages());
        verify(estudianteRepository).buscarSelectorPaginado("Laura", null, null, interno);
    }

    @Test
    void selectorConciliacionDebeAplicarBanderaYAlcanceDelAsesor() {
        when(accessService.puedeVerTodosLosEstudiantes()).thenReturn(false);
        when(accessService.usuarioEsAsesor()).thenReturn(true);
        when(accessService.obtenerAsesorActualId()).thenReturn(9L);
        EstudianteResumenProjection projection = mock(EstudianteResumenProjection.class);
        EstudianteResumenDTO dto = mock(EstudianteResumenDTO.class);
        EstudianteMapper mapper = mock(EstudianteMapper.class);
        queryService = new EstudianteQueryService(estudianteRepository, accessService, mapper);
        PageRequest interno = PageRequest.of(
                0,
                10,
                Sort.by(Sort.Order.asc("e.nombre").ignoreCase(), Sort.Order.asc("e.id")));
        when(estudianteRepository.buscarSelectorPaginado(null, true, 9L, interno))
                .thenReturn(new PageImpl<>(List.of(projection), interno, 1));
        when(mapper.convertirAResumenDTO(projection)).thenReturn(dto);

        PageResponseDTO<EstudianteResumenDTO> resultado =
                queryService.listarConConciliacionPaginados(null, 1, 10, "nombre", "asc");

        assertEquals(List.of(dto), resultado.content());
        assertEquals(1, resultado.totalElements());
        verify(estudianteRepository).buscarSelectorPaginado(null, true, 9L, interno);
    }

    @Test
    void debeDevolverVacioSinConsultarCuandoElAlcanceNoPermiteEstudiantes() {
        PageResponseDTO<EstudianteResumenDTO> resultado =
                queryService.listarActivosPaginados(null, 1, 10, "nombre", "asc");

        assertEquals(List.of(), resultado.content());
        assertEquals(0, resultado.totalElements());
        verify(estudianteRepository, never()).buscarSelectorPaginado(any(), any(), any(), any());
    }

    @Test
    void noDebeConsultarSinAutorizacion() {
        doThrow(new AccessDeniedException("denegado"))
                .when(accessService)
                .validarPuedeListarEstudiantes();

        assertThrows(
                AccessDeniedException.class,
                () -> queryService.listarConConciliacionPaginados(null, 1, 10, "nombre", "asc"));

        verify(estudianteRepository, never()).buscarSelectorPaginado(any(), any(), any(), any());
    }
}
