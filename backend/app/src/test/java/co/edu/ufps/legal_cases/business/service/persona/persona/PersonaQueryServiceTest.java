package co.edu.ufps.legal_cases.business.service.persona.persona;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
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
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;

import co.edu.ufps.legal_cases.business.dto.persona.PersonaDTO;
import co.edu.ufps.legal_cases.business.dto.persona.PersonaResumenDTO;
import co.edu.ufps.legal_cases.business.model.consulta.EstadoConsulta;
import co.edu.ufps.legal_cases.business.model.persona.Persona;
import co.edu.ufps.legal_cases.business.repository.persona.PersonaRepository;
import co.edu.ufps.legal_cases.business.repository.persona.PersonaResumenProjection;
import co.edu.ufps.legal_cases.business.service.acceso.persona.AlcanceLecturaPersonas;
import co.edu.ufps.legal_cases.business.service.acceso.persona.PersonaAccessService;
import co.edu.ufps.legal_cases.common.dto.PageResponseDTO;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.common.exception.ResourceNotFoundException;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;

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

        // Alcance global por defecto para todos los tests históricos
        when(personaAccessService.obtenerAlcanceLecturaPersonas())
                .thenReturn(AlcanceLecturaPersonas.global());
    }

    // =========================================================
    // Tests históricos — conservan el mismo significado.
    // El stub de obtenerAlcanceLecturaPersonas() retorna global
    // en el setUp para no romper las verificaciones anteriores.
    // =========================================================

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
        Sort sortEsperado = Sort.by(
                Sort.Order.asc("nombres").ignoreCase(),
                Sort.Order.asc("id"));
        PageRequest interno = PageRequest.of(1, 10, sortEsperado);

        when(personaRepository.buscarResumen(
                eq("Ana Perez"), isNull(),
                eq(true), isNull(), isNull(), eq(EstadoConsulta.ARCHIVADO),
                eq(interno)))
                .thenReturn(new PageImpl<>(List.of(projection), interno, 21));
        when(personaResumenMapper.convertirAResumen(projection)).thenReturn(resumen);

        PageResponseDTO<PersonaResumenDTO> resultado =
                personaQueryService.listar("  Ana   Perez ", 2, 10, "nombres", "asc");

        assertEquals(List.of(resumen), resultado.content());
        assertEquals(2, resultado.page());
        assertEquals(10, resultado.size());
        assertEquals(21, resultado.totalElements());
        assertEquals(3, resultado.totalPages());
        verify(personaAccessService).obtenerAlcanceLecturaPersonas();
        verify(personaRepository, never()).findAll();
    }

    @Test
    void debeTratarBusquedaVaciaComoAusenciaDeFiltro() {
        Sort sortEsperado = Sort.by(
                Sort.Order.asc("nombres").ignoreCase(),
                Sort.Order.asc("id"));
        PageRequest interno = PageRequest.of(0, 10, sortEsperado);
        when(personaRepository.buscarResumen(
                isNull(), isNull(),
                eq(true), isNull(), isNull(), eq(EstadoConsulta.ARCHIVADO),
                any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(), interno, 0));

        personaQueryService.listar("   ", 1, 10, "nombres", "asc");

        verify(personaRepository).buscarResumen(
                isNull(), isNull(),
                eq(true), isNull(), isNull(), eq(EstadoConsulta.ARCHIVADO),
                eq(interno));
    }

    @Test
    void debeAplicarFiltroActivoEnEndpointDeActivos() {
        Sort sortEsperado = Sort.by(
                Sort.Order.asc("nombres").ignoreCase(),
                Sort.Order.asc("id"));
        PageRequest interno = PageRequest.of(0, 50, sortEsperado);
        when(personaRepository.buscarResumen(
                isNull(), eq(true),
                eq(true), isNull(), isNull(), eq(EstadoConsulta.ARCHIVADO),
                any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(), interno, 0));

        personaQueryService.listarActivos(null, 1, 50, "nombres", "asc");

        verify(personaRepository).buscarResumen(
                isNull(), eq(true),
                eq(true), isNull(), isNull(), eq(EstadoConsulta.ARCHIVADO),
                eq(interno));
    }

    @Test
    void debeRechazarPaginaMenorAUno() {
        BusinessException error = assertThrows(
                BusinessException.class,
                () -> personaQueryService.listar(null, 0, 10, "nombres", "asc"));

        assertEquals("La página debe ser mayor o igual a 1", error.getMessage());
        verify(personaRepository, never()).buscarResumen(
                any(), any(), anyBoolean(), any(), any(), any(), any());
    }

    @Test
    void debeRechazarTamanoFueraDelLimite() {
        assertThrows(BusinessException.class,
                () -> personaQueryService.listar(null, 1, 0, "nombres", "asc"));
        assertThrows(BusinessException.class,
                () -> personaQueryService.listar(null, 1, 51, "nombres", "asc"));

        verify(personaRepository, never()).buscarResumen(
                any(), any(), anyBoolean(), any(), any(), any(), any());
    }

    @Test
    void debeRechazarBusquedaExcesivamenteLarga() {
        assertThrows(
                BusinessException.class,
                () -> personaQueryService.listar("x".repeat(101), 1, 10, "nombres", "asc"));

        verify(personaRepository, never()).buscarResumen(
                any(), any(), anyBoolean(), any(), any(), any(), any());
    }

    @Test
    void debeConstruirSortConNombresAscYDesempateIdAsc() {
        when(personaRepository.buscarResumen(
                isNull(), isNull(),
                eq(true), isNull(), isNull(), eq(EstadoConsulta.ARCHIVADO),
                any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        personaQueryService.listar(null, 1, 10, "nombres", "asc");

        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(personaRepository).buscarResumen(
                isNull(), isNull(),
                eq(true), isNull(), isNull(), eq(EstadoConsulta.ARCHIVADO),
                captor.capture());
        Sort sort = captor.getValue().getSort();
        List<Sort.Order> ordenes = sort.stream().toList();
        assertEquals(2, ordenes.size());
        assertEquals("nombres", ordenes.get(0).getProperty());
        assertEquals(Sort.Direction.ASC, ordenes.get(0).getDirection());
        assertTrue(ordenes.get(0).isIgnoreCase());
        assertEquals("id", ordenes.get(1).getProperty());
        assertEquals(Sort.Direction.ASC, ordenes.get(1).getDirection());
    }

    @Test
    void debeConstruirSortDescendenteConDesempateIdAsc() {
        when(personaRepository.buscarResumen(
                isNull(), isNull(),
                eq(true), isNull(), isNull(), eq(EstadoConsulta.ARCHIVADO),
                any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        personaQueryService.listar(null, 1, 10, "apellidos", "desc");

        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(personaRepository).buscarResumen(
                isNull(), isNull(),
                eq(true), isNull(), isNull(), eq(EstadoConsulta.ARCHIVADO),
                captor.capture());
        Sort sort = captor.getValue().getSort();
        List<Sort.Order> ordenes = sort.stream().toList();
        assertEquals(2, ordenes.size());
        assertEquals("apellidos", ordenes.get(0).getProperty());
        assertEquals(Sort.Direction.DESC, ordenes.get(0).getDirection());
        assertTrue(ordenes.get(0).isIgnoreCase());
        assertEquals("id", ordenes.get(1).getProperty());
        assertEquals(Sort.Direction.ASC, ordenes.get(1).getDirection());
    }

    @Test
    void debePermitirOtrosCamposValidosDeLaWhitelistComoTipoPersonaYActivo() {
        when(personaRepository.buscarResumen(
                isNull(), isNull(),
                eq(true), isNull(), isNull(), eq(EstadoConsulta.ARCHIVADO),
                any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        personaQueryService.listar(null, 1, 10, "tipoPersona", "asc");

        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(personaRepository, org.mockito.Mockito.atLeastOnce()).buscarResumen(
                isNull(), isNull(),
                eq(true), isNull(), isNull(), eq(EstadoConsulta.ARCHIVADO),
                captor.capture());
        Sort.Order ordenTipoPersona = captor.getValue().getSort().getOrderFor("tipoPersona.nombre");
        assertNotNull(ordenTipoPersona);
        assertEquals(Sort.Direction.ASC, ordenTipoPersona.getDirection());

        personaQueryService.listar(null, 1, 10, "activo", "desc");
        verify(personaRepository, org.mockito.Mockito.atLeastOnce()).buscarResumen(
                isNull(), isNull(),
                eq(true), isNull(), isNull(), eq(EstadoConsulta.ARCHIVADO),
                captor.capture());
        Sort.Order ordenActivo = captor.getValue().getSort().getOrderFor("activo");
        assertNotNull(ordenActivo);
        assertEquals(Sort.Direction.DESC, ordenActivo.getDirection());
        assertFalse(ordenActivo.isIgnoreCase());
    }

    @Test
    void debeRechazarSortByInvalidoOIdDirecto() {
        BusinessException errorInvalido = assertThrows(
                BusinessException.class,
                () -> personaQueryService.listar(null, 1, 10, "invalido", "asc"));
        assertEquals("El campo de ordenamiento 'invalido' no es válido", errorInvalido.getMessage());

        BusinessException errorId = assertThrows(
                BusinessException.class,
                () -> personaQueryService.listar(null, 1, 10, "id", "asc"));
        assertEquals("El campo de ordenamiento 'id' no es válido", errorId.getMessage());

        verify(personaRepository, never()).buscarResumen(
                any(), any(), anyBoolean(), any(), any(), any(), any());
    }

    @Test
    void debeRechazarSortByVacioONulo() {
        BusinessException errorVacio = assertThrows(
                BusinessException.class,
                () -> personaQueryService.listar(null, 1, 10, "   ", "asc"));
        assertEquals("El campo de ordenamiento no puede estar vacío", errorVacio.getMessage());

        BusinessException errorNulo = assertThrows(
                BusinessException.class,
                () -> personaQueryService.listar(null, 1, 10, null, "asc"));
        assertEquals("El campo de ordenamiento no puede estar vacío", errorNulo.getMessage());

        verify(personaRepository, never()).buscarResumen(
                any(), any(), anyBoolean(), any(), any(), any(), any());
    }

    @Test
    void debeRechazarDirectionInvalidaOVacia() {
        BusinessException errorInvalido = assertThrows(
                BusinessException.class,
                () -> personaQueryService.listar(null, 1, 10, "nombres", "lateral"));
        assertEquals("La dirección de ordenamiento debe ser 'asc' o 'desc'", errorInvalido.getMessage());

        BusinessException errorVacio = assertThrows(
                BusinessException.class,
                () -> personaQueryService.listar(null, 1, 10, "nombres", "   "));
        assertEquals("La dirección de ordenamiento no puede estar vacía", errorVacio.getMessage());

        verify(personaRepository, never()).buscarResumen(
                any(), any(), anyBoolean(), any(), any(), any(), any());
    }

    @Test
    void debeNormalizarMayusculasYEspaciosEnDirectionYSortBy() {
        when(personaRepository.buscarResumen(
                isNull(), isNull(),
                eq(true), isNull(), isNull(), eq(EstadoConsulta.ARCHIVADO),
                any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        personaQueryService.listar(null, 1, 10, "  nombres  ", "  ASC  ");

        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(personaRepository, org.mockito.Mockito.atLeastOnce()).buscarResumen(
                isNull(), isNull(),
                eq(true), isNull(), isNull(), eq(EstadoConsulta.ARCHIVADO),
                captor.capture());
        Sort.Order orden = captor.getValue().getSort().getOrderFor("nombres");
        assertNotNull(orden);
        assertEquals(Sort.Direction.ASC, orden.getDirection());

        personaQueryService.listar(null, 1, 10, "apellidos", "DeSc");
        verify(personaRepository, org.mockito.Mockito.atLeastOnce()).buscarResumen(
                isNull(), isNull(),
                eq(true), isNull(), isNull(), eq(EstadoConsulta.ARCHIVADO),
                captor.capture());
        Sort.Order ordenDesc = captor.getValue().getSort().getOrderFor("apellidos");
        assertNotNull(ordenDesc);
        assertEquals(Sort.Direction.DESC, ordenDesc.getDirection());
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
                .obtenerAlcanceLecturaPersonas();

        assertThrows(
                AccessDeniedException.class,
                () -> personaQueryService.listar("Ana", 1, 10, "nombres", "asc"));

        verify(personaRepository, never()).buscarResumen(
                any(), any(), anyBoolean(), any(), any(), any(), any());
    }

    @Test
    void debePropagarPageableSinOrdenClientePorqueLaQueryDefineOrdenDeterminista() {
        Sort sortEsperado = Sort.by(
                Sort.Order.asc("nombres").ignoreCase(),
                Sort.Order.asc("id"));
        PageRequest interno = PageRequest.of(2, 25, sortEsperado);

        when(personaRepository.buscarResumen(
                eq("1090"), isNull(),
                eq(true), isNull(), isNull(), eq(EstadoConsulta.ARCHIVADO),
                eq(interno)))
                .thenReturn(new PageImpl<>(List.of(), interno, 60));

        personaQueryService.listar("1090", 3, 25, "nombres", "asc");

        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(personaRepository).buscarResumen(
                eq("1090"), isNull(),
                eq(true), isNull(), isNull(), eq(EstadoConsulta.ARCHIVADO),
                captor.capture());

        assertEquals(2, captor.getValue().getPageNumber());
        assertEquals(25, captor.getValue().getPageSize());
        assertEquals(sortEsperado, captor.getValue().getSort());
    }

    // =========================================================
    // Test nuevo obligatorio — propagación exacta del scope (7.3)
    // =========================================================

    @Test
    void debePropagarAlcanceRestringidoResueltoPorAccessService() {
        when(personaAccessService.obtenerAlcanceLecturaPersonas())
                .thenReturn(AlcanceLecturaPersonas.restringido(TipoPerfilUsuario.ESTUDIANTE, 11L));

        Sort sortEsperado = Sort.by(
                Sort.Order.asc("nombres").ignoreCase(),
                Sort.Order.asc("id"));
        PageRequest internoEsperado = PageRequest.of(0, 10, sortEsperado);

        when(personaRepository.buscarResumen(
                isNull(), isNull(),
                eq(false), eq("ESTUDIANTE"), eq(11L), eq(EstadoConsulta.ARCHIVADO),
                eq(internoEsperado)))
                .thenReturn(new PageImpl<>(List.of(), internoEsperado, 0));

        personaQueryService.listar(null, 1, 10, "nombres", "asc");

        verify(personaRepository).buscarResumen(
                isNull(),
                isNull(),
                eq(false),
                eq("ESTUDIANTE"),
                eq(11L),
                eq(EstadoConsulta.ARCHIVADO),
                eq(internoEsperado));
    }
}