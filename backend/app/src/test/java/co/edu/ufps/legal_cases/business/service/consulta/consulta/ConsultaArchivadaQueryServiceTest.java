package co.edu.ufps.legal_cases.business.service.consulta.consulta;

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
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;

import co.edu.ufps.legal_cases.business.dto.consulta.ConsultaBusquedaDTO;
import co.edu.ufps.legal_cases.business.model.consulta.EstadoConsulta;
import co.edu.ufps.legal_cases.business.repository.consulta.ConsultaRepository;
import co.edu.ufps.legal_cases.business.repository.consulta.ConsultaResumenProjection;
import co.edu.ufps.legal_cases.business.service.acceso.consulta.ConsultaAccessService;
import co.edu.ufps.legal_cases.common.dto.PageResponseDTO;

class ConsultaArchivadaQueryServiceTest {

    private ConsultaRepository consultaRepository;
    private ConsultaAccessService consultaAccessService;
    private ConsultaMapper consultaMapper;
    private ConsultaQueryService consultaQueryService;

    @BeforeEach
    void setUp() {
        consultaRepository = mock(ConsultaRepository.class);
        consultaAccessService = mock(ConsultaAccessService.class);
        consultaMapper = mock(ConsultaMapper.class);
        consultaQueryService = new ConsultaQueryService(
                consultaRepository,
                consultaAccessService,
                consultaMapper);
    }

    @Test
    void debeDevolverPrimeraPaginaConTamanoYTotales() {
        ConsultaResumenProjection projection = mock(ConsultaResumenProjection.class);
        ConsultaBusquedaDTO dto = mock(ConsultaBusquedaDTO.class);
        PageRequest interno = PageRequest.of(
                0,
                2,
                Sort.by(Sort.Order.desc("fecha"), Sort.Order.asc("id")));
        when(consultaRepository.buscarArchivadasResumenPaginado(
                null,
                EstadoConsulta.ARCHIVADO,
                interno))
                .thenReturn(new PageImpl<>(List.of(projection), interno, 5));
        when(consultaMapper.convertirABusquedaDTO(projection)).thenReturn(dto);

        PageResponseDTO<ConsultaBusquedaDTO> resultado =
                consultaQueryService.listarArchivadas(null, 1, 2, "fecha", "desc");

        assertEquals(List.of(dto), resultado.content());
        assertEquals(1, resultado.page());
        assertEquals(2, resultado.size());
        assertEquals(5, resultado.totalElements());
        assertEquals(3, resultado.totalPages());
        verify(consultaAccessService).validarPuedeListarConsultasArchivadas();
    }

    @Test
    void debeAplicarBusquedaSegundaPaginaYOrdenEstable() {
        PageRequest interno = PageRequest.of(
                1,
                3,
                Sort.by(Sort.Order.asc("persona.nombres").ignoreCase(), Sort.Order.asc("id")));
        when(consultaRepository.buscarArchivadasResumenPaginado(
                "Ana Perez",
                EstadoConsulta.ARCHIVADO,
                interno))
                .thenReturn(new PageImpl<>(List.of(), interno, 4));

        PageResponseDTO<ConsultaBusquedaDTO> resultado =
                consultaQueryService.listarArchivadas("  Ana   Perez ", 2, 3, "nombre", "asc");

        assertEquals(List.of(), resultado.content());
        assertEquals(2, resultado.page());
        assertEquals(3, resultado.size());
        assertEquals(4, resultado.totalElements());
        assertEquals(2, resultado.totalPages());

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(consultaRepository).buscarArchivadasResumenPaginado(
                eq("Ana Perez"),
                eq(EstadoConsulta.ARCHIVADO),
                pageable.capture());
        assertEquals(1, pageable.getValue().getPageNumber());
        assertEquals("persona.nombres: ASC, ignoring case,id: ASC", pageable.getValue().getSort().toString());
    }

    @Test
    void debeDevolverPaginaVaciaFueraDelRango() {
        PageRequest interno = PageRequest.of(
                4,
                10,
                Sort.by(Sort.Order.desc("fecha"), Sort.Order.asc("id")));
        when(consultaRepository.buscarArchivadasResumenPaginado(
                null,
                EstadoConsulta.ARCHIVADO,
                interno))
                .thenReturn(new PageImpl<>(List.of(), interno, 12));

        PageResponseDTO<ConsultaBusquedaDTO> resultado =
                consultaQueryService.listarArchivadas(null, 5, 10, "fecha", "desc");

        assertEquals(List.of(), resultado.content());
        assertEquals(12, resultado.totalElements());
        assertEquals(2, resultado.totalPages());
    }

    @Test
    void noDebeConsultarRepositorioSinAutorizacion() {
        doThrow(new AccessDeniedException("denegado"))
                .when(consultaAccessService)
                .validarPuedeListarConsultasArchivadas();

        assertThrows(
                AccessDeniedException.class,
                () -> consultaQueryService.listarArchivadas(null, 1, 10, "fecha", "desc"));

        verify(consultaRepository, never()).buscarArchivadasResumenPaginado(any(), any(), any());
    }
}
