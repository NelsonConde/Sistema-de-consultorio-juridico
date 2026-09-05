package co.edu.ufps.legal_cases.file_storage.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.file_storage.model.FileAsset;
import co.edu.ufps.legal_cases.file_storage.model.FileAssetStatus;
import co.edu.ufps.legal_cases.file_storage.model.FileResourceType;
import co.edu.ufps.legal_cases.file_storage.repository.FileAssetRepository;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.service.context.UsuarioActualService;

class FileAssetVersioningTest {

    private FileAssetRepository repository;
    private UsuarioActualService usuarioActualService;
    private FileAssetService service;
    private UsuarioSistema usuario;

    @BeforeEach
    void setUp() {
        repository = mock(FileAssetRepository.class);
        usuarioActualService = mock(UsuarioActualService.class);
        service = new FileAssetService(repository, usuarioActualService, "test-bucket");

        usuario = new UsuarioSistema();
        usuario.setId(10L);
        usuario.setUsername("abogado@test.com");
        when(usuarioActualService.obtenerUsuarioActual()).thenReturn(usuario);
        when(repository.save(any(FileAsset.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void primeraVersionEsUnoYGeneraDocumentoLogicoNuevo() {
        FileAsset asset = service.startUpload(
                FileResourceType.CONSULTA,
                100L,
                "anexo.pdf",
                "application/pdf",
                1024L,
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                null,
                "CONSULTA_ANEXO");

        assertNotNull(asset.getDocumentoLogico());
        assertEquals(1, asset.getVersion());
        assertEquals("CONSULTA_ANEXO", asset.getTipoDocumental());
        assertEquals("CARGA_USUARIO", asset.getOrigen());
        assertNull(asset.getReferenciaAnterior());
        assertEquals(FileAssetStatus.UPLOADING, asset.getStatus());
        assertFalse(asset.getActive());
        assertEquals(usuario, asset.getUploadedBy());
    }

    @Test
    void siguienteVersionEsNMasUnoYEnlazaReferenciaAnterior() {
        UUID docLogico = UUID.randomUUID();

        FileAsset v1 = new FileAsset();
        v1.setId(1L);
        v1.setDocumentoLogico(docLogico);
        v1.setVersion(1);
        v1.setResourceType("CONSULTA");
        v1.setResourceId(100L);
        v1.setStatus(FileAssetStatus.VIGENTE);
        v1.setActive(true);

        when(repository.findVigenteForUpdate(docLogico, FileAssetStatus.VIGENTE))
                .thenReturn(Optional.of(v1));

        FileAsset v2 = service.startUpload(
                FileResourceType.CONSULTA,
                100L,
                "anexo_v2.pdf",
                "application/pdf",
                2048L,
                "a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3",
                docLogico,
                "CONSULTA_ANEXO");

        assertEquals(docLogico, v2.getDocumentoLogico());
        assertEquals(2, v2.getVersion());
        assertEquals(v1, v2.getReferenciaAnterior());
        assertEquals("CARGA_USUARIO", v2.getOrigen());
        assertEquals(FileAssetStatus.UPLOADING, v2.getStatus());
    }

    @Test
    void rechazaNuevaVersionSiDocumentoLogicoPerteneceAOtroRecurso() {
        UUID docLogico = UUID.randomUUID();

        FileAsset otroRecurso = new FileAsset();
        otroRecurso.setId(5L);
        otroRecurso.setDocumentoLogico(docLogico);
        otroRecurso.setVersion(1);
        otroRecurso.setResourceType("CONSULTA");
        otroRecurso.setResourceId(999L); // Recurso distinto a 100L
        otroRecurso.setStatus(FileAssetStatus.VIGENTE);

        when(repository.findVigenteForUpdate(docLogico, FileAssetStatus.VIGENTE))
                .thenReturn(Optional.of(otroRecurso));

        assertThrows(BusinessException.class, () -> service.startUpload(
                FileResourceType.CONSULTA,
                100L,
                "intento_fraude.pdf",
                "application/pdf",
                1024L,
                "a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3",
                docLogico,
                "CONSULTA_ANEXO"));
    }

    @Test
    void completarCargaTransicionaAnteriorAHistoricoYMarcaNuevaVigente() {
        UUID docLogico = UUID.randomUUID();
        UUID uploadId = UUID.randomUUID();

        FileAsset v1 = new FileAsset();
        v1.setId(1L);
        v1.setDocumentoLogico(docLogico);
        v1.setVersion(1);
        v1.setStatus(FileAssetStatus.VIGENTE);
        v1.setActive(true);

        FileAsset v2 = new FileAsset();
        v2.setId(2L);
        v2.setUploadId(uploadId);
        v2.setDocumentoLogico(docLogico);
        v2.setVersion(2);
        v2.setReferenciaAnterior(v1);
        v2.setStatus(FileAssetStatus.UPLOADING);
        v2.setSize(2048L);

        when(repository.findByUploadId(uploadId)).thenReturn(Optional.of(v2));
        when(repository.findByDocumentoLogicoAndStatus(docLogico, FileAssetStatus.VIGENTE))
                .thenReturn(Optional.of(v1));

        FileAsset ready = service.markReady(uploadId, 2048L, "application/pdf");

        // v1 pasa a HISTORICO e inactivo
        assertEquals(FileAssetStatus.HISTORICO, v1.getStatus());
        assertFalse(v1.getActive());

        // v2 pasa a VIGENTE y activo
        assertEquals(FileAssetStatus.VIGENTE, ready.getStatus());
        assertTrue(ready.getActive());
    }

    @Test
    void versionesHistoricasSonInmutablesYNoPuedenEliminarse() {
        FileAsset historico = new FileAsset();
        historico.setId(1L);
        historico.setStatus(FileAssetStatus.HISTORICO);

        when(repository.findById(1L)).thenReturn(Optional.of(historico));

        assertThrows(BusinessException.class, () -> service.markDeleted(1L));
        assertThrows(BusinessException.class, () -> service.markDeletePending(1L));

        verify(repository, never()).delete(any());
    }

    @Test
    void bajaLogicaMarcaDeletedSinEliminarFisicamenteElRegistro() {
        FileAsset vigente = new FileAsset();
        vigente.setId(10L);
        vigente.setStatus(FileAssetStatus.VIGENTE);
        vigente.setActive(true);

        when(repository.findById(10L)).thenReturn(Optional.of(vigente));

        FileAsset eliminado = service.markDeleted(10L);

        assertEquals(FileAssetStatus.DELETED, eliminado.getStatus());
        assertFalse(eliminado.getActive());
        verify(repository, never()).delete(any());
    }

    @Test
    void listVersionsRetornaTodasLasVersionesOrdenadasDescendente() {
        UUID docLogico = UUID.randomUUID();
        FileAsset v1 = new FileAsset();
        v1.setVersion(1);
        FileAsset v2 = new FileAsset();
        v2.setVersion(2);

        when(repository.findByDocumentoLogicoOrderByVersionDesc(docLogico))
                .thenReturn(List.of(v2, v1));

        List<FileAsset> versiones = service.listVersions(docLogico);

        assertEquals(2, versiones.size());
        assertEquals(2, versiones.get(0).getVersion());
        assertEquals(1, versiones.get(1).getVersion());
    }

    @Test
    void findExpedienteFilesPropagaParametrosYFiltraEstadosVisibles() {
        LocalDateTime desde = LocalDateTime.of(2026, 9, 1, 0, 0);
        LocalDateTime hasta = LocalDateTime.of(2026, 9, 5, 23, 59);

        FileAsset asset = new FileAsset();
        asset.setId(10L);
        asset.setResourceType("PROCESO");

        when(repository.findExpedienteFiles(
                eq(100L),
                eq(List.of(FileAssetStatus.VIGENTE, FileAssetStatus.READY, FileAssetStatus.ACTIVE)),
                eq("PROCESO_DOCUMENTO"),
                eq("PROCESO"),
                eq("CARGA_USUARIO"),
                eq("7"),
                eq(7L),
                eq(desde),
                eq(hasta)))
                .thenReturn(List.of(asset));

        List<FileAsset> result = service.findExpedienteFiles(
                100L,
                "PROCESO_DOCUMENTO",
                "PROCESO",
                "CARGA_USUARIO",
                "7",
                desde,
                hasta);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getId());
    }

    @Test
    void findExpedienteFilesValidaConsultaIdObligatorio() {
        assertThrows(BusinessException.class, () -> service.findExpedienteFiles(
                null, null, null, null, null, null, null));
        assertThrows(BusinessException.class, () -> service.findExpedienteFiles(
                0L, null, null, null, null, null, null));
    }

    @Test
    void resolveTipoDocumentalParaProcesoRetornaProcesoDocumento() {
        FileAsset asset = service.startUpload(
                FileResourceType.PROCESO,
                50L,
                "demanda.pdf",
                "application/pdf",
                1024L,
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                null,
                null);

        assertEquals("PROCESO_DOCUMENTO", asset.getTipoDocumental());
        assertEquals("PROCESO", asset.getResourceType());
        assertEquals(50L, asset.getResourceId());
    }
}
