package co.edu.ufps.legal_cases.business.service.consulta.reasignacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import co.edu.ufps.legal_cases.business.dto.consulta.ConsultaReasignacionHistorialDTO;
import co.edu.ufps.legal_cases.business.model.consulta.TipoResponsableConsulta;
import co.edu.ufps.legal_cases.business.repository.consulta.ConsultaReasignacionHistorialRepository;
import co.edu.ufps.legal_cases.business.repository.consulta.ConsultaReasignacionHistorialResumenProjection;
import co.edu.ufps.legal_cases.business.service.acceso.consulta.ConsultaAccessService;
import co.edu.ufps.legal_cases.common.dto.PageResponseDTO;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

class ConsultaReasignacionHistorialQueryServiceTest {

    private ConsultaReasignacionHistorialRepository historialRepository;
    private ConsultaAccessService consultaAccessService;
    private ConsultaReasignacionHistorialQueryService queryService;

    @BeforeEach
    void setUp() {
        historialRepository = mock(ConsultaReasignacionHistorialRepository.class);
        consultaAccessService = mock(ConsultaAccessService.class);
        queryService = new ConsultaReasignacionHistorialQueryService(
                historialRepository,
                consultaAccessService);
    }

    @Test
    void debeAutorizarYPaginarEnBaseDeDatosConDatosLegibles() {
        Instant ocurridoEn = Instant.parse("2026-10-25T15:30:00Z");
        ConsultaReasignacionHistorialResumenProjection evento = evento(
                19L,
                "ASESOR",
                "Asesor anterior",
                "Asesor nuevo",
                "Distribución de carga",
                "Administradora principal",
                ocurridoEn);
        PageRequest pageable = PageRequest.of(1, 7);

        when(historialRepository.buscarResumenPorConsultaId(7L, pageable))
                .thenReturn(new PageImpl<>(List.of(evento), pageable, 8));

        PageResponseDTO<ConsultaReasignacionHistorialDTO> resultado =
                queryService.listar(7L, 2, 7);

        assertEquals(2, resultado.page());
        assertEquals(7, resultado.size());
        assertEquals(8, resultado.totalElements());
        assertEquals(2, resultado.totalPages());
        assertEquals(1, resultado.content().size());

        ConsultaReasignacionHistorialDTO dto = resultado.content().getFirst();
        assertEquals(19L, dto.id());
        assertEquals(TipoResponsableConsulta.ASESOR, dto.tipoResponsable());
        assertEquals("Asesor anterior", dto.responsableAnteriorNombre());
        assertEquals("Asesor nuevo", dto.responsableNuevoNombre());
        assertEquals("Distribución de carga", dto.motivo());
        assertEquals("Administradora principal", dto.actorNombre());
        assertEquals(ocurridoEn, dto.ocurridoEn());

        InOrder orden = inOrder(consultaAccessService, historialRepository);
        orden.verify(consultaAccessService).validarPuedeVerConsulta(7L);
        orden.verify(historialRepository).buscarResumenPorConsultaId(7L, pageable);
    }

    @Test
    void debeConservarAnteriorNuloYDevolverPaginaVaciaFueraDeRango() {
        PageRequest primeraPagina = PageRequest.of(0, 10);
        ConsultaReasignacionHistorialResumenProjection evento = evento(
                1L,
                "MONITOR",
                null,
                "Monitor asignado",
                "Asignación inicial",
                "Coordinadora",
                Instant.parse("2026-10-25T14:00:00Z"));
        when(historialRepository.buscarResumenPorConsultaId(3L, primeraPagina))
                .thenReturn(new PageImpl<>(List.of(evento), primeraPagina, 1));

        PageResponseDTO<ConsultaReasignacionHistorialDTO> primera =
                queryService.listar(3L, 1, 10);

        assertNull(primera.content().getFirst().responsableAnteriorNombre());

        PageRequest paginaFueraDeRango = PageRequest.of(2, 10);
        when(historialRepository.buscarResumenPorConsultaId(3L, paginaFueraDeRango))
                .thenReturn(new PageImpl<>(List.of(), paginaFueraDeRango, 1));

        PageResponseDTO<ConsultaReasignacionHistorialDTO> vacia =
                queryService.listar(3L, 3, 10);

        assertEquals(List.of(), vacia.content());
        assertEquals(1, vacia.totalElements());
        assertEquals(1, vacia.totalPages());
    }

    @Test
    void noDebeConsultarHistorialCuandoFallaLaAutorizacion() {
        doThrow(new AccessDeniedException("denegado"))
                .when(consultaAccessService)
                .validarPuedeVerConsulta(12L);

        assertThrows(
                AccessDeniedException.class,
                () -> queryService.listar(12L, 1, 10));

        verify(historialRepository, never())
                .buscarResumenPorConsultaId(eq(12L), any(Pageable.class));
    }

    @Test
    void debeRechazarPaginacionInvalidaSinConsultarHistorial() {
        assertThrows(BusinessException.class, () -> queryService.listar(4L, 0, 10));
        assertThrows(BusinessException.class, () -> queryService.listar(4L, 1, 0));
        assertThrows(BusinessException.class, () -> queryService.listar(4L, 1, 51));

        verify(consultaAccessService, times(3)).validarPuedeVerConsulta(4L);
        verify(historialRepository, never())
                .buscarResumenPorConsultaId(eq(4L), any(Pageable.class));
    }

    private ConsultaReasignacionHistorialResumenProjection evento(
            Long id,
            String tipoResponsable,
            String responsableAnteriorNombre,
            String responsableNuevoNombre,
            String motivo,
            String actorNombre,
            Instant ocurridoEn) {
        ConsultaReasignacionHistorialResumenProjection evento =
                mock(ConsultaReasignacionHistorialResumenProjection.class);
        when(evento.getId()).thenReturn(id);
        when(evento.getTipoResponsable()).thenReturn(tipoResponsable);
        when(evento.getResponsableAnteriorNombre()).thenReturn(responsableAnteriorNombre);
        when(evento.getResponsableNuevoNombre()).thenReturn(responsableNuevoNombre);
        when(evento.getMotivo()).thenReturn(motivo);
        when(evento.getActorNombre()).thenReturn(actorNombre);
        when(evento.getOcurridoEn()).thenReturn(ocurridoEn);
        return evento;
    }
}
