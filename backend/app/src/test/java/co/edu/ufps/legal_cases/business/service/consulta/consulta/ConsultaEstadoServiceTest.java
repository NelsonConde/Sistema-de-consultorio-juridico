package co.edu.ufps.legal_cases.business.service.consulta.consulta;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import co.edu.ufps.legal_cases.business.model.consulta.Consulta;
import co.edu.ufps.legal_cases.business.model.consulta.EstadoConsulta;
import co.edu.ufps.legal_cases.business.model.proceso.EstadoProceso;
import co.edu.ufps.legal_cases.business.model.seguimiento.EstadoSeguimiento;
import co.edu.ufps.legal_cases.business.model.seguimiento.respuesta.EstadoRespuestaSeguimiento;
import co.edu.ufps.legal_cases.business.repository.conciliacion.ConciliacionRepository;
import co.edu.ufps.legal_cases.business.repository.proceso.ProcesoRepository;
import co.edu.ufps.legal_cases.business.repository.seguimiento.SeguimientoRepository;
import co.edu.ufps.legal_cases.business.repository.seguimiento.notificacion.SeguimientoNotificacionRepository;
import co.edu.ufps.legal_cases.business.repository.seguimiento.respuesta.SeguimientoRespuestaRepository;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

class ConsultaEstadoServiceTest {

    private ProcesoRepository procesoRepository;
    private SeguimientoRepository seguimientoRepository;
    private SeguimientoRespuestaRepository seguimientoRespuestaRepository;
    private SeguimientoNotificacionRepository seguimientoNotificacionRepository;
    private ConciliacionRepository conciliacionRepository;
    private ConsultaEstadoService service;

    @BeforeEach
    void setUp() {
        procesoRepository = mock(ProcesoRepository.class);
        seguimientoRepository = mock(SeguimientoRepository.class);
        seguimientoRespuestaRepository = mock(SeguimientoRespuestaRepository.class);
        seguimientoNotificacionRepository = mock(SeguimientoNotificacionRepository.class);
        conciliacionRepository = mock(ConciliacionRepository.class);

        service = new ConsultaEstadoService(
                procesoRepository,
                seguimientoRepository,
                seguimientoRespuestaRepository,
                seguimientoNotificacionRepository,
                conciliacionRepository);
    }

    @Test
    void debeRechazarCierreSinResultado() {
        Consulta consulta = consultaOperativa();
        consulta.setResultado("   ");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.validarCambioEstado(consulta, EstadoConsulta.CERRADO));

        assertEquals(
                "No se puede cerrar la consulta sin resultado o conclusión final",
                exception.getMessage());
    }

    @Test
    void debeRechazarCierreConProcesoPendiente() {
        Consulta consulta = consultaOperativaConResultado();

        when(procesoRepository.existsByConsulta_IdAndActivoTrueAndEstado(
                consulta.getId(),
                EstadoProceso.PENDIENTE))
                .thenReturn(true);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.validarCambioEstado(consulta, EstadoConsulta.CERRADO));

        assertEquals(
                "No se puede cerrar la consulta porque tiene procesos pendientes",
                exception.getMessage());
    }

    @Test
    void debeRechazarCierreConSeguimientoPendiente() {
        Consulta consulta = consultaOperativaConResultado();

        when(seguimientoRepository.existsByConsulta_IdAndActivoTrueAndEstado(
                consulta.getId(),
                EstadoSeguimiento.PENDIENTE))
                .thenReturn(true);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.validarCambioEstado(consulta, EstadoConsulta.CERRADO));

        assertEquals(
                "No se puede cerrar la consulta porque tiene seguimientos pendientes",
                exception.getMessage());
    }

    @Test
    void debeRechazarCierreConRespuestaPendiente() {
        Consulta consulta = consultaOperativaConResultado();

        when(seguimientoRespuestaRepository
                .existsBySeguimiento_Consulta_IdAndSeguimiento_ActivoTrueAndActivoTrueAndEstado(
                        consulta.getId(),
                        EstadoRespuestaSeguimiento.PENDIENTE))
                .thenReturn(true);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.validarCambioEstado(consulta, EstadoConsulta.CERRADO));

        assertEquals(
                "No se puede cerrar la consulta porque tiene respuestas pendientes",
                exception.getMessage());
    }

    @Test
    void debeRechazarCierreConNotificacionPendiente() {
        Consulta consulta = consultaOperativaConResultado();

        when(seguimientoNotificacionRepository
                .existsBySeguimiento_Consulta_IdAndSeguimiento_ActivoTrueAndActivoTrueAndEnviadaFalse(
                        consulta.getId()))
                .thenReturn(true);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.validarCambioEstado(consulta, EstadoConsulta.CERRADO));

        assertEquals(
                "No se puede cerrar la consulta porque tiene notificaciones pendientes",
                exception.getMessage());
    }

    @Test
    void debeRechazarCierreConConciliacionPendiente() {
        Consulta consulta = consultaOperativaConResultado();

        when(conciliacionRepository.existsByConsulta_IdAndActivoTrueAndEstado_CodigoIn(
                eq(consulta.getId()),
                any()))
                .thenReturn(true);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.validarCambioEstado(consulta, EstadoConsulta.CERRADO));

        assertEquals(
                "No se puede cerrar la consulta porque tiene conciliaciones pendientes",
                exception.getMessage());
    }

    @Test
    void debePermitirCierreConResultadoSinPendientes() {
        Consulta consulta = consultaOperativaConResultado();

        assertDoesNotThrow(() -> service.validarCambioEstado(consulta, EstadoConsulta.CERRADO));
    }

    @Test
    void debeRechazarCambioDeConsultaCerradaAEstadoOperativo() {
        Consulta consulta = consultaCerrada();

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.validarCambioEstado(consulta, EstadoConsulta.ACTIVO));

        assertEquals("Una consulta cerrada solo puede archivarse", exception.getMessage());
    }

    @Test
    void debeRechazarOperacionSobreConsultaCerrada() {
        Consulta consulta = consultaCerrada();

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.validarPermiteOperacionOperativa(consulta));

        assertEquals(
                "No se pueden realizar operaciones sobre una consulta cerrada",
                exception.getMessage());
    }

    @Test
    void debeRechazarOperacionSobreConsultaArchivada() {
        Consulta consulta = consultaArchivada();

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.validarPermiteOperacionOperativa(consulta));

        assertEquals(
                "No se pueden realizar operaciones sobre una consulta archivada",
                exception.getMessage());
    }

    @Test
    void debePermitirOperacionSobreConsultaOperativa() {
        Consulta consulta = consultaOperativa();

        assertDoesNotThrow(() -> service.validarPermiteOperacionOperativa(consulta));
    }

    @Test
    void debeRechazarArchivarConsultaNoCerrada() {
        Consulta consulta = consultaOperativa();

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.validarPuedeArchivar(consulta));

        assertEquals("Solo se pueden archivar consultas cerradas", exception.getMessage());
    }

    @Test
    void debePermitirArchivarConsultaCerradaSinPendientes() {
        Consulta consulta = consultaCerrada();

        assertDoesNotThrow(() -> service.validarPuedeArchivar(consulta));
    }

    @Test
    void debeRechazarDesarchivarConsultaNoArchivada() {
        Consulta consulta = consultaCerrada();

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.validarPuedeDesarchivar(consulta));

        assertEquals("Solo se pueden desarchivar consultas archivadas", exception.getMessage());
    }

    @Test
    void debePermitirDesarchivarConsultaArchivadaSinPendientes() {
        Consulta consulta = consultaArchivada();

        assertDoesNotThrow(() -> service.validarPuedeDesarchivar(consulta));
    }

    private Consulta consultaOperativa() {
        Consulta consulta = new Consulta();
        consulta.setId(10L);
        consulta.setEstado(EstadoConsulta.EN_PROCESO);
        return consulta;
    }

    private Consulta consultaOperativaConResultado() {
        Consulta consulta = consultaOperativa();
        consulta.setResultado("Se brindó orientación jurídica y se finalizó la atención.");
        return consulta;
    }

    private Consulta consultaCerrada() {
        Consulta consulta = new Consulta();
        consulta.setId(10L);
        consulta.setEstado(EstadoConsulta.CERRADO);
        consulta.setResultado("Consulta cerrada con resultado registrado.");
        return consulta;
    }

    private Consulta consultaArchivada() {
        Consulta consulta = new Consulta();
        consulta.setId(10L);
        consulta.setEstado(EstadoConsulta.ARCHIVADO);
        consulta.setResultado("Consulta archivada con resultado registrado.");
        return consulta;
    }
}