package co.edu.ufps.legal_cases.business.service.conciliacion.conciliacion;

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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import co.edu.ufps.legal_cases.business.model.conciliacion.Conciliacion;
import co.edu.ufps.legal_cases.business.model.conciliacion.EstadoConciliacion;
import co.edu.ufps.legal_cases.business.model.consulta.Consulta;
import co.edu.ufps.legal_cases.business.repository.conciliacion.ConciliacionRepository;
import co.edu.ufps.legal_cases.business.service.acceso.conciliacion.ConciliacionAccessService;
import co.edu.ufps.legal_cases.business.service.conciliacion.reunion.notificacion.ReunionConciliacionNotificacionService;
import co.edu.ufps.legal_cases.common.concurrency.ConcurrenciaOptimistaValidator;
import co.edu.ufps.legal_cases.file_storage.model.FileAsset;
import co.edu.ufps.legal_cases.file_storage.model.FileResourceType;
import co.edu.ufps.legal_cases.file_storage.service.FileResourceService;
import co.edu.ufps.legal_cases.security.repository.account.UsuarioSistemaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;

class ConciliacionCommandServiceDb03Test {

    private ConciliacionRepository conciliacionRepository;
    private ConciliacionRelacionService relacionService;
    private FileResourceService fileResourceService;
    private ConciliacionValidator conciliacionValidator;
    private ReunionConciliacionNotificacionService notificacionService;
    private EntityManager entityManager;
    private ConciliacionCommandService service;

    @BeforeEach
    void setUp() {
        conciliacionRepository = mock(ConciliacionRepository.class);
        relacionService = mock(ConciliacionRelacionService.class);
        fileResourceService = mock(FileResourceService.class);
        conciliacionValidator = mock(ConciliacionValidator.class);
        notificacionService = mock(ReunionConciliacionNotificacionService.class);
        entityManager = mock(EntityManager.class);

        service = new ConciliacionCommandService(
                conciliacionRepository,
                mock(UsuarioSistemaRepository.class),
                mock(ConciliacionAccessService.class),
                relacionService,
                mock(ConciliacionAsignacionService.class),
                fileResourceService,
                conciliacionValidator,
                mock(ConciliacionMapper.class),
                notificacionService,
                new ConcurrenciaOptimistaValidator(),
                entityManager);
    }

    @Test
    void finalizarDescartaActaNuevaSiFlushVersionadoFalla() {
        Conciliacion conciliacion = conciliacion(30L, 3L);
        EstadoConciliacion estadoFinal = new EstadoConciliacion();
        FileAsset actaNueva = archivo(100L);
        MultipartFile acta = mock(MultipartFile.class);
        OptimisticLockException conflicto = new OptimisticLockException("conflicto");

        when(relacionService.obtenerConciliacionActiva(30L)).thenReturn(conciliacion);
        when(relacionService.obtenerEstadoActivoPorCodigo("COMPLETO_CONCILIADO"))
                .thenReturn(estadoFinal);
        when(fileResourceService.storeMultipartAfterAuthorization(
                eq(FileResourceType.CONCILIACION),
                eq(30L),
                isNull(),
                eq(acta)))
                .thenReturn(actaNueva);
        when(conciliacionRepository.save(conciliacion)).thenReturn(conciliacion);
        doThrow(conflicto).when(entityManager).flush();

        OptimisticLockException lanzada = assertThrows(
                OptimisticLockException.class,
                () -> service.finalizar(30L, "COMPLETO_CONCILIADO", acta, 3L));

        assertSame(conflicto, lanzada);
        verify(fileResourceService).discardStoredAsset(actaNueva);
        verify(notificacionService, never()).cancelarPendientesPorConciliacion(any());
    }

    @Test
    void reemplazarSolicitudDescartaSolicitudNuevaSiFlushVersionadoFalla() {
        Conciliacion conciliacion = conciliacion(31L, 3L);
        FileAsset solicitudAnterior = archivo(200L);
        FileAsset solicitudNueva = archivo(201L);
        MultipartFile solicitud = mock(MultipartFile.class);
        OptimisticLockException conflicto = new OptimisticLockException("conflicto");

        conciliacion.setDocumentoSolicitud(solicitudAnterior);

        when(relacionService.obtenerConciliacionActiva(31L)).thenReturn(conciliacion);
        when(fileResourceService.storeMultipartAfterAuthorization(
                eq(FileResourceType.CONCILIACION),
                eq(31L),
                isNull(),
                eq(solicitud)))
                .thenReturn(solicitudNueva);
        when(conciliacionRepository.save(conciliacion)).thenReturn(conciliacion);
        doThrow(conflicto).when(entityManager).flush();

        OptimisticLockException lanzada = assertThrows(
                OptimisticLockException.class,
                () -> service.reemplazarSolicitud(31L, solicitud, 3L));

        assertSame(conflicto, lanzada);
        verify(fileResourceService).discardStoredAsset(solicitudNueva);
        verify(fileResourceService, never()).discardStoredAsset(solicitudAnterior);
    }

    private Conciliacion conciliacion(Long id, Long version) {
        Conciliacion conciliacion = new Conciliacion();
        conciliacion.setId(id);
        conciliacion.setVersion(version);
        conciliacion.setConsulta(new Consulta());
        return conciliacion;
    }

    private FileAsset archivo(Long id) {
        FileAsset asset = new FileAsset();
        asset.setId(id);
        asset.setObjectKey("conciliacion/" + id + "/archivo.pdf");
        return asset;
    }
}
