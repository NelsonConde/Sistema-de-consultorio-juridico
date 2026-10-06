package co.edu.ufps.legal_cases.business.service.perfil.asesor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import co.edu.ufps.legal_cases.business.dto.perfil.AsesorResumenDTO;
import co.edu.ufps.legal_cases.business.repository.perfil.AsesorRepository;
import co.edu.ufps.legal_cases.business.repository.perfil.AsesorResumenProjection;
import co.edu.ufps.legal_cases.business.service.acceso.perfil.AsesorMonitorAccessService;
import co.edu.ufps.legal_cases.common.dto.PageResponseDTO;

class AsesorSelectorPaginadoTest {

    private AsesorRepository asesorRepository;
    private AsesorMapper asesorMapper;
    private AsesorMonitorAccessService accessService;
    private AsesorQueryService queryService;

    @BeforeEach
    void setUp() {
        asesorRepository = mock(AsesorRepository.class);
        asesorMapper = mock(AsesorMapper.class);
        accessService = mock(AsesorMonitorAccessService.class);
        queryService = new AsesorQueryService(asesorRepository, asesorMapper, accessService);
    }

    @Test
    void debeBuscarYPaginarSoloAsesoresActivos() {
        AsesorResumenProjection projection = mock(AsesorResumenProjection.class);
        AsesorResumenDTO dto = mock(AsesorResumenDTO.class);
        PageRequest interno = PageRequest.of(
                1,
                2,
                Sort.by(Sort.Order.asc("a.nombre").ignoreCase(), Sort.Order.asc("a.id")));
        when(asesorRepository.buscarResumenPaginado("Ana", true, interno))
                .thenReturn(new PageImpl<>(List.of(projection), interno, 5));
        when(asesorMapper.convertirAResumenDTO(projection)).thenReturn(dto);

        PageResponseDTO<AsesorResumenDTO> resultado =
                queryService.listarActivosPaginados(" Ana ", 2, 2, "nombre", "asc");

        assertEquals(List.of(dto), resultado.content());
        assertEquals(2, resultado.page());
        assertEquals(2, resultado.size());
        assertEquals(5, resultado.totalElements());
        assertEquals(3, resultado.totalPages());
        verify(accessService).validarPuedeListarAsesoresYMonitoresActivos();
        verify(asesorRepository).buscarResumenPaginado("Ana", true, interno);
    }

    @Test
    void debeDevolverRespuestaVacia() {
        PageRequest interno = PageRequest.of(
                0,
                10,
                Sort.by(Sort.Order.asc("a.nombre").ignoreCase(), Sort.Order.asc("a.id")));
        when(asesorRepository.buscarResumenPaginado(null, true, interno))
                .thenReturn(new PageImpl<>(List.of(), interno, 0));

        PageResponseDTO<AsesorResumenDTO> resultado =
                queryService.listarActivosPaginados(" ", 1, 10, "nombre", "asc");

        assertEquals(List.of(), resultado.content());
        assertEquals(0, resultado.totalElements());
        assertEquals(0, resultado.totalPages());
    }

    @Test
    void noDebeConsultarSinAutorizacion() {
        doThrow(new AccessDeniedException("denegado"))
                .when(accessService)
                .validarPuedeListarAsesoresYMonitoresActivos();

        assertThrows(
                AccessDeniedException.class,
                () -> queryService.listarActivosPaginados(null, 1, 10, "nombre", "asc"));

        verify(asesorRepository, never()).buscarResumenPaginado(any(), eq(true), any());
    }
}
