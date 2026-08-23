package co.edu.ufps.legal_cases.business.service.persona.persona;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import co.edu.ufps.legal_cases.business.dto.persona.PersonaDTO;
import co.edu.ufps.legal_cases.business.dto.persona.PersonaPageResponseDTO;
import co.edu.ufps.legal_cases.business.dto.persona.PersonaResumenDTO;
import co.edu.ufps.legal_cases.business.model.persona.Persona;
import co.edu.ufps.legal_cases.business.repository.persona.PersonaRepository;
import co.edu.ufps.legal_cases.business.repository.persona.PersonaResumenProjection;
import co.edu.ufps.legal_cases.business.service.acceso.persona.PersonaAccessService;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.common.exception.ResourceNotFoundException;

class PersonaQueryServiceTest {

    private PersonaRepository personaRepository;
    private PersonaAccessService personaAccessService;
    private PersonaMapper personaMapper;
    private PersonaResumenMapper personaResumenMapper;
    private PersonaQueryService personaQueryService;

    @BeforeEach
    void setUp() {
        personaRepository = mock(PersonaRepository.class);
        personaAccessService = mock(PersonaAccessService.class);
        personaMapper = mock(PersonaMapper.class);
        personaResumenMapper = mock(PersonaResumenMapper.class);
        personaQueryService = new PersonaQueryService(
                personaRepository,
                personaAccessService,
                personaMapper,
                personaResumenMapper);
    }

    @Test
    void debeBuscarEnRepositorioYPaginarDesdeUno() {
        PersonaResumenProjection projection = mock(PersonaResumenProjection.class);
        PersonaResumenDTO resumen = new PersonaResumenDTO(
                5L,
                "Ana",
                "Perez",
                "CC",
                "******3456",
                "Solicitante",
                true);
        PageRequest interno = PageRequest.of(1, 10);

        when(personaRepository.buscarResumen("Ana Perez", null, interno))
                .thenReturn(new PageImpl<>(List.of(projection), interno, 21));
        when(personaResumenMapper.convertirAResumen(projection)).thenReturn(resumen);

        PersonaPageResponseDTO resultado = personaQueryService.listar("  Ana   Perez ", 2, 10);

        assertEquals(List.of(resumen), resultado.content());
        assertEquals(2, resultado.page());
        assertEquals(10, resultado.size());
        assertEquals(21, resultado.totalElements());
        assertEquals(3, resultado.totalPages());
        verify(personaAccessService).validarPuedeBuscarPersonas();
        verify(personaRepository, never()).findAll();
    }

    @Test
    void debeTratarBusquedaVaciaComoAusenciaDeFiltro() {
        PageRequest interno = PageRequest.of(0, 10);
        when(personaRepository.buscarResumen(isNull(), isNull(), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(), interno, 0));

        personaQueryService.listar("   ", 1, 10);

        verify(personaRepository).buscarResumen(null, null, interno);
    }

    @Test
    void debeAplicarFiltroActivoEnEndpointDeActivos() {
        PageRequest interno = PageRequest.of(0, 50);
        when(personaRepository.buscarResumen(null, true, interno))
                .thenReturn(new PageImpl<>(List.of(), interno, 0));

        personaQueryService.listarActivos(null, 1, 50);

        verify(personaRepository).buscarResumen(null, true, interno);
    }

    @Test
    void debeRechazarPaginaMenorAUno() {
        BusinessException error = assertThrows(
                BusinessException.class,
                () -> personaQueryService.listar(null, 0, 10));

        assertEquals("La página debe ser mayor o igual a 1", error.getMessage());
        verify(personaRepository, never()).buscarResumen(any(), any(), any());
    }

    @Test
    void debeRechazarTamanoFueraDelLimite() {
        assertThrows(BusinessException.class, () -> personaQueryService.listar(null, 1, 0));
        assertThrows(BusinessException.class, () -> personaQueryService.listar(null, 1, 51));

        verify(personaRepository, never()).buscarResumen(any(), any(), any());
    }

    @Test
    void debeRechazarBusquedaExcesivamenteLarga() {
        assertThrows(
                BusinessException.class,
                () -> personaQueryService.listar("x".repeat(101), 1, 10));

        verify(personaRepository, never()).buscarResumen(any(), any(), any());
    }

    @Test
    void debeConsultarDetalleSoloDespuesDeValidarAlcance() {
        Persona persona = new Persona();
        PersonaDTO detalle = PersonaDTO.builder().id(8L).correo("sensible@example.com").build();
        when(personaRepository.findById(8L)).thenReturn(Optional.of(persona));
        when(personaMapper.convertirADTO(persona)).thenReturn(detalle);

        PersonaDTO resultado = personaQueryService.obtenerPorId(8L);

        assertSame(detalle, resultado);
        verify(personaAccessService).validarPuedeVerDetallePersona(8L);
        verify(personaRepository).findById(8L);
    }

    @Test
    void noDebeConsultarPersonaCuandoElAlcanceEsDenegado() {
        doThrow(new ResourceNotFoundException("Persona no encontrada"))
                .when(personaAccessService)
                .validarPuedeVerDetallePersona(40L);

        assertThrows(
                ResourceNotFoundException.class,
                () -> personaQueryService.obtenerPorId(40L));

        verify(personaRepository, never()).findById(40L);
    }

    @Test
    void idInexistenteYFueraDeAlcanceDebenCompartirMensajeGenerico() {
        when(personaRepository.findById(41L)).thenReturn(Optional.empty());

        ResourceNotFoundException inexistente = assertThrows(
                ResourceNotFoundException.class,
                () -> personaQueryService.obtenerPorId(41L));

        assertEquals("Persona no encontrada", inexistente.getMessage());
    }

    @Test
    void noDebeBuscarCuandoFaltaPermisoFuncional() {
        doThrow(new AccessDeniedException("denegado"))
                .when(personaAccessService)
                .validarPuedeBuscarPersonas();

        assertThrows(
                AccessDeniedException.class,
                () -> personaQueryService.listar("Ana", 1, 10));

        verify(personaRepository, never()).buscarResumen(any(), any(), any());
    }

    @Test
    void debePropagarPageableSinOrdenClientePorqueLaQueryDefineOrdenDeterminista() {
        PageRequest interno = PageRequest.of(2, 25);

        when(personaRepository.buscarResumen("1090", null, interno))
                .thenReturn(new PageImpl<>(List.of(), interno, 60));

        personaQueryService.listar("1090", 3, 25);

        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);

        verify(personaRepository).buscarResumen(
                eq("1090"),
                isNull(),
                captor.capture());

        assertEquals(2, captor.getValue().getPageNumber());
        assertEquals(25, captor.getValue().getPageSize());
        assertEquals(false, captor.getValue().getSort().isSorted());
    }
}