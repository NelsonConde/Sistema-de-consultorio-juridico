package co.edu.ufps.legal_cases.business.service.seguimiento.seguimiento;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import co.edu.ufps.legal_cases.business.model.consulta.Consulta;
import co.edu.ufps.legal_cases.business.model.seguimiento.Seguimiento;
import co.edu.ufps.legal_cases.business.repository.consulta.ConsultaRepository;
import co.edu.ufps.legal_cases.business.repository.seguimiento.CategoriaSeguimientoRepository;
import co.edu.ufps.legal_cases.business.repository.seguimiento.SeguimientoRepository;
import co.edu.ufps.legal_cases.business.service.acceso.seguimiento.SeguimientoAccessService;
import co.edu.ufps.legal_cases.business.service.consulta.consulta.ConsultaEstadoService;
import co.edu.ufps.legal_cases.business.service.seguimiento.SeguimientoNotificacionService;
import co.edu.ufps.legal_cases.common.concurrency.ConcurrenciaOptimistaValidator;
import co.edu.ufps.legal_cases.security.repository.account.UsuarioSistemaRepository;
import jakarta.persistence.EntityManager;

class SeguimientoCommandServiceDb03Test {

    @Test
    void eliminarCancelaNotificacionesSoloDespuesDeFlushVersionado() {
        SeguimientoRepository seguimientoRepository = mock(SeguimientoRepository.class);
        SeguimientoNotificacionService notificacionService = mock(SeguimientoNotificacionService.class);
        EntityManager entityManager = mock(EntityManager.class);

        SeguimientoCommandService service = new SeguimientoCommandService(
                seguimientoRepository,
                mock(CategoriaSeguimientoRepository.class),
                mock(ConsultaRepository.class),
                mock(UsuarioSistemaRepository.class),
                notificacionService,
                mock(SeguimientoAccessService.class),
                mock(SeguimientoEstadoService.class),
                mock(SeguimientoMapper.class),
                mock(SeguimientoValidator.class),
                mock(ConsultaEstadoService.class),
                new ConcurrenciaOptimistaValidator(),
                entityManager);

        Seguimiento seguimiento = new Seguimiento();
        seguimiento.setId(20L);
        seguimiento.setVersion(3L);
        seguimiento.setConsulta(new Consulta());

        when(seguimientoRepository.findByIdAndActivoTrue(20L))
                .thenReturn(Optional.of(seguimiento));
        when(seguimientoRepository.save(seguimiento))
                .thenReturn(seguimiento);

        service.eliminar(20L, 3L);

        InOrder orden = inOrder(
                seguimientoRepository,
                entityManager,
                notificacionService);
        orden.verify(seguimientoRepository).save(seguimiento);
        orden.verify(entityManager).flush();
        orden.verify(notificacionService).cancelarNotificacionesPendientes(20L);
    }
}
