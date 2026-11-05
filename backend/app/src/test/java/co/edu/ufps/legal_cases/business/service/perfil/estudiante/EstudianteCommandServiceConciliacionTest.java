package co.edu.ufps.legal_cases.business.service.perfil.estudiante;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import co.edu.ufps.legal_cases.business.dto.perfil.EstudianteDTO;
import co.edu.ufps.legal_cases.business.model.catalogo.Sede;
import co.edu.ufps.legal_cases.business.model.catalogo.TipoDocumento;
import co.edu.ufps.legal_cases.business.model.perfil.Asesor;
import co.edu.ufps.legal_cases.business.model.perfil.Estudiante;
import co.edu.ufps.legal_cases.business.repository.catalogo.SedeRepository;
import co.edu.ufps.legal_cases.business.repository.catalogo.TipoDocumentoRepository;
import co.edu.ufps.legal_cases.business.repository.perfil.AsesorRepository;
import co.edu.ufps.legal_cases.business.repository.perfil.EstudianteRepository;
import co.edu.ufps.legal_cases.business.service.acceso.perfil.EstudianteAccessService;
import co.edu.ufps.legal_cases.business.service.consulta.consulta.ConsultaResponsableOperacionService;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.repository.account.UsuarioSistemaRepository;
import co.edu.ufps.legal_cases.security.service.account.perfil.PerfilOperacionEstadoValidator;
import co.edu.ufps.legal_cases.security.service.account.usuario.UsuarioSistemaRegistroService;

class EstudianteCommandServiceConciliacionTest {

    private EstudianteRepository estudianteRepository;
    private TipoDocumentoRepository tipoDocumentoRepository;
    private SedeRepository sedeRepository;
    private AsesorRepository asesorRepository;
    private UsuarioSistemaRegistroService usuarioSistemaRegistroService;
    private EstudianteAccessService estudianteAccessService;

    private EstudianteCommandService service;

    @BeforeEach
    void setUp() {
        estudianteRepository = mock(EstudianteRepository.class);
        tipoDocumentoRepository = mock(TipoDocumentoRepository.class);
        sedeRepository = mock(SedeRepository.class);
        asesorRepository = mock(AsesorRepository.class);
        usuarioSistemaRegistroService = mock(UsuarioSistemaRegistroService.class);
        estudianteAccessService = mock(EstudianteAccessService.class);

        EstudianteValidator estudianteValidator =
                mock(EstudianteValidator.class);

        TipoDocumento tipoDocumento = new TipoDocumento();
        tipoDocumento.setId(1L);

        Sede sede = new Sede();
        sede.setId(1L);

        Asesor asesor = new Asesor();
        asesor.setId(1L);

        when(tipoDocumentoRepository.findByIdAndActivoTrue(1L))
                .thenReturn(Optional.of(tipoDocumento));

        when(sedeRepository.findByIdAndActivoTrue(1L))
                .thenReturn(Optional.of(sede));

        when(asesorRepository.findByIdAndActivoTrue(1L))
                .thenReturn(Optional.of(asesor));

        when(estudianteRepository.save(any(Estudiante.class)))
                .thenAnswer(invocation -> {
                    Estudiante estudiante = invocation.getArgument(0);
                    estudiante.setId(1L);
                    return estudiante;
                });

        when(usuarioSistemaRegistroService.crearParaEstudiante(any(Estudiante.class)))
                .thenReturn(new UsuarioSistema());

        service = new EstudianteCommandService(
                estudianteRepository,
                tipoDocumentoRepository,
                sedeRepository,
                asesorRepository,
                usuarioSistemaRegistroService,
                mock(UsuarioSistemaRepository.class),
                mock(PerfilOperacionEstadoValidator.class),
                estudianteAccessService,
                estudianteValidator,
                new EstudianteMapper(),
                mock(ConsultaResponsableOperacionService.class));
    }

    @Test
    void crearDebeConservarConciliacionTrueSolicitada() {
        EstudianteDTO dto = dtoBase();
        dto.setConciliacion(true);

        EstudianteDTO resultado = service.crear(dto);

        assertTrue(resultado.getConciliacion());
    }

    @Test
    void crearDebeConservarConciliacionFalseSolicitada() {
        EstudianteDTO dto = dtoBase();
        dto.setConciliacion(false);

        EstudianteDTO resultado = service.crear(dto);

        assertFalse(resultado.getConciliacion());
    }

    @Test
    void crearConConciliacionNullDebeUsarFalsePorDefecto() {
        EstudianteDTO dto = dtoBase();
        dto.setConciliacion(null);

        EstudianteDTO resultado = service.crear(dto);

        assertFalse(resultado.getConciliacion());
    }

    @Test
    void crearConConciliacionTrueDebeRechazarseSinPermiso() {
        doThrow(new AccessDeniedException("denegado"))
                .when(estudianteAccessService)
                .validarPuedeGestionarEstudiantes();

        EstudianteDTO dto = dtoBase();
        dto.setConciliacion(true);

        assertThrows(
                AccessDeniedException.class,
                () -> service.crear(dto));

        verify(estudianteRepository, never())
                .save(any(Estudiante.class));
    }

    @Test
    void actualizarDatosGeneralesNoDebeCambiarConciliacion() {
        Estudiante existente = new Estudiante();
        existente.setId(1L);
        existente.setActivo(true);
        existente.setConciliacion(true);

        when(estudianteRepository.findById(1L))
                .thenReturn(Optional.of(existente));

        EstudianteDTO dto = dtoBase();
        dto.setId(1L);

        // La actualización general no debe modificar la habilitación
        // de conciliación existente.
        dto.setConciliacion(false);

        EstudianteDTO resultado = service.actualizar(1L, dto);

        assertTrue(resultado.getConciliacion());
    }

    private EstudianteDTO dtoBase() {
        EstudianteDTO dto = new EstudianteDTO();
        dto.setNombre("Estudiante Prueba");
        dto.setTipoDocumentoId(1L);
        dto.setDocumento("999999999");
        dto.setEmail("estudiante.prueba@ufps.edu.co");
        dto.setTelefono("3000000000");
        dto.setUsuario("estudiante.prueba");
        dto.setSedeId(1L);
        dto.setCodigo("1152999");
        dto.setAsesorId(1L);
        return dto;
    }
}
