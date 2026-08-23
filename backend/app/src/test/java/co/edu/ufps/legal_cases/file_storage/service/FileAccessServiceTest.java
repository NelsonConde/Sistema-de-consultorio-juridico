package co.edu.ufps.legal_cases.file_storage.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import co.edu.ufps.legal_cases.business.service.acceso.conciliacion.ConciliacionAccessService;
import co.edu.ufps.legal_cases.business.service.acceso.consulta.ConsultaAccessService;
import co.edu.ufps.legal_cases.business.service.acceso.seguimiento.SeguimientoAccessService;
import co.edu.ufps.legal_cases.business.service.acceso.seguimiento.SeguimientoRespuestaAccessService;

@ExtendWith(MockitoExtension.class)
class FileAccessServiceTest {

    @Mock
    private ConsultaAccessService consultaAccessService;

    @Mock
    private SeguimientoAccessService seguimientoAccessService;

    @Mock
    private SeguimientoRespuestaAccessService respuestaAccessService;

    @Mock
    private ConciliacionAccessService conciliacionAccessService;

    @Test
    void usaAlcanceDeConsultaParaLeerArchivo() {
        FileAccessService service = new FileAccessService(
                consultaAccessService,
                seguimientoAccessService,
                respuestaAccessService,
                conciliacionAccessService);

        service.authorizeRead("12/documento.pdf");

        verify(consultaAccessService).validarPuedeVerConsulta(12L);
    }

    @Test
    void usaAlcanceDeRespuestaParaCargarArchivo() {
        FileAccessService service = new FileAccessService(
                consultaAccessService,
                seguimientoAccessService,
                respuestaAccessService,
                conciliacionAccessService);

        service.authorizeUpload("tareas-8-respuestas-21/soporte.pdf");

        verify(respuestaAccessService).validarPuedeResponderSeguimiento(8L);
    }

    @Test
    void rechazaRutaSinRecurso() {
        FileAccessService service = new FileAccessService(
                consultaAccessService,
                seguimientoAccessService,
                respuestaAccessService,
                conciliacionAccessService);

        assertThrows(AccessDeniedException.class, () -> service.authorizeRead("../secreto.pdf"));
    }
}
