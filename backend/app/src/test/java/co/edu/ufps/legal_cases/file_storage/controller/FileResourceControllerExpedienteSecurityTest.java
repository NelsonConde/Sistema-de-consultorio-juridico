package co.edu.ufps.legal_cases.file_storage.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import co.edu.ufps.legal_cases.business.model.consulta.Consulta;
import co.edu.ufps.legal_cases.business.model.perfil.Asesor;
import co.edu.ufps.legal_cases.business.model.perfil.Estudiante;
import co.edu.ufps.legal_cases.business.model.perfil.Monitor;
import co.edu.ufps.legal_cases.business.repository.consulta.ConsultaRepository;
import co.edu.ufps.legal_cases.business.service.acceso.consulta.ConsultaAccessService;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.common.exception.handler.GlobalExceptionHandler;
import co.edu.ufps.legal_cases.file_storage.dto.ExpedienteDocumentoResponse;
import co.edu.ufps.legal_cases.file_storage.dto.FileResponse;
import co.edu.ufps.legal_cases.file_storage.dto.FileUploadRequest;
import co.edu.ufps.legal_cases.file_storage.dto.FileUploadResponse;
import co.edu.ufps.legal_cases.file_storage.model.FileAsset;
import co.edu.ufps.legal_cases.file_storage.model.FileAssetStatus;
import co.edu.ufps.legal_cases.file_storage.model.FileResourceType;
import co.edu.ufps.legal_cases.file_storage.service.FileAssetService;
import co.edu.ufps.legal_cases.file_storage.service.FileResourceAuthorizationService;
import co.edu.ufps.legal_cases.file_storage.service.FileResourceService;
import co.edu.ufps.legal_cases.file_storage.service.FileValidationService;
import co.edu.ufps.legal_cases.file_storage.service.StorageProvider;
import co.edu.ufps.legal_cases.security.dto.account.PerfilUsuarioActual;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.service.context.UsuarioActualService;

class FileResourceControllerExpedienteSecurityTest {

    private MockMvc mockMvc;
    private FileResourceService fileResourceService;
    private FileAssetService fileAssetService;
    private ConsultaAccessService consultaAccessService;
    private ConsultaRepository consultaRepository;
    private UsuarioActualService usuarioActualService;

    @BeforeEach
    void setUp() {
        fileAssetService = mock(FileAssetService.class);
        consultaRepository = mock(ConsultaRepository.class);
        usuarioActualService = mock(UsuarioActualService.class);
        consultaAccessService = new ConsultaAccessService(usuarioActualService, consultaRepository);

        fileResourceService = new FileResourceService(
                fileAssetService,
                mock(FileValidationService.class),
                mock(FileResourceAuthorizationService.class),
                mock(StorageProvider.class),
                usuarioActualService,
                consultaAccessService,
                java.time.Duration.ofMinutes(10),
                java.time.Duration.ofMinutes(5));

        FileResourceController controller = new FileResourceController(fileResourceService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void usuarioSinPermisoRecibe403Forbidden() throws Exception {
        when(usuarioActualService.tieneAlgunPermiso(any())).thenReturn(false);

        mockMvc.perform(get("/api/consultas/10/expediente/archivos"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("No autorizado"));
    }

    @Test
    void consultaInexistenteRecibe404NotFound() throws Exception {
        permitirLecturaConsultas();
        when(consultaRepository.findById(10L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/consultas/10/expediente/archivos"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Consulta no encontrada"));
    }

    @Test
    void perfilAdministradorAccedeACualquierExpediente200Ok() throws Exception {
        permitirLecturaConsultas();
        when(usuarioActualService.esRolAdministrador()).thenReturn(true);
        when(consultaRepository.findById(10L)).thenReturn(Optional.of(new Consulta()));

        when(fileAssetService.findExpedienteFiles(eq(10L), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(crearAsset(1L, "CONSULTA", 10L, "radicado.pdf")));

        mockMvc.perform(get("/api/consultas/10/expediente/archivos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].fileName").value("radicado.pdf"))
                .andExpect(jsonPath("$[0].resourceType").value("CONSULTA"))
                .andExpect(jsonPath("$[0].resourceId").value(10));
    }

    @Test
    void perfilEstudianteConExpedientePropioAccede200Ok() throws Exception {
        permitirLecturaConsultas();
        Consulta consulta = new Consulta();
        Estudiante est = new Estudiante();
        est.setId(15L);
        consulta.setEstudiante(est);

        when(consultaRepository.findById(10L)).thenReturn(Optional.of(consulta));
        when(usuarioActualService.obtenerPerfilActual())
                .thenReturn(new PerfilUsuarioActual(15L, TipoPerfilUsuario.ESTUDIANTE));

        when(fileAssetService.findExpedienteFiles(eq(10L), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(crearAsset(2L, "SEGUIMIENTO", 100L, "informe.pdf")));

        mockMvc.perform(get("/api/consultas/10/expediente/archivos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].fileName").value("informe.pdf"));
    }

    @Test
    void perfilEstudianteConExpedienteAjenoRecibe404NotFound() throws Exception {
        permitirLecturaConsultas();
        Consulta consulta = new Consulta();
        Estudiante est = new Estudiante();
        est.setId(99L); // Otro estudiante
        consulta.setEstudiante(est);

        when(consultaRepository.findById(10L)).thenReturn(Optional.of(consulta));
        when(usuarioActualService.obtenerPerfilActual())
                .thenReturn(new PerfilUsuarioActual(15L, TipoPerfilUsuario.ESTUDIANTE));

        mockMvc.perform(get("/api/consultas/10/expediente/archivos"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Consulta no encontrada"));
    }

    @Test
    void perfilAsesorConExpedienteAsignadoAccede200Ok() throws Exception {
        permitirLecturaConsultas();
        Consulta consulta = new Consulta();
        Asesor asesor = new Asesor();
        asesor.setId(20L);
        consulta.setAsesor(asesor);

        when(consultaRepository.findById(10L)).thenReturn(Optional.of(consulta));
        when(usuarioActualService.obtenerPerfilActual())
                .thenReturn(new PerfilUsuarioActual(20L, TipoPerfilUsuario.ASESOR));

        when(fileAssetService.findExpedienteFiles(eq(10L), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(crearAsset(3L, "PROCESO", 50L, "demanda.pdf")));

        mockMvc.perform(get("/api/consultas/10/expediente/archivos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(3))
                .andExpect(jsonPath("$[0].resourceType").value("PROCESO"));
    }

    @Test
    void perfilAsesorConExpedienteAjenoRecibe404NotFound() throws Exception {
        permitirLecturaConsultas();
        Consulta consulta = new Consulta();
        Asesor asesor = new Asesor();
        asesor.setId(25L);
        consulta.setAsesor(asesor);

        when(consultaRepository.findById(10L)).thenReturn(Optional.of(consulta));
        when(usuarioActualService.obtenerPerfilActual())
                .thenReturn(new PerfilUsuarioActual(20L, TipoPerfilUsuario.ASESOR));

        mockMvc.perform(get("/api/consultas/10/expediente/archivos"))
                .andExpect(status().isNotFound());
    }

    @Test
    void perfilMonitorConExpedienteAsignadoAccede200Ok() throws Exception {
        permitirLecturaConsultas();
        Consulta consulta = new Consulta();
        Monitor monitor = new Monitor();
        monitor.setId(30L);
        consulta.setMonitor(monitor);

        when(consultaRepository.findById(10L)).thenReturn(Optional.of(consulta));
        when(usuarioActualService.obtenerPerfilActual())
                .thenReturn(new PerfilUsuarioActual(30L, TipoPerfilUsuario.MONITOR));

        when(fileAssetService.findExpedienteFiles(eq(10L), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(crearAsset(4L, "CONCILIACION", 70L, "acta.pdf")));

        mockMvc.perform(get("/api/consultas/10/expediente/archivos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(4));
    }

    @Test
    void perfilMonitorConExpedienteAjenoRecibe404NotFound() throws Exception {
        permitirLecturaConsultas();
        Consulta consulta = new Consulta();
        Monitor monitor = new Monitor();
        monitor.setId(35L);
        consulta.setMonitor(monitor);

        when(consultaRepository.findById(10L)).thenReturn(Optional.of(consulta));
        when(usuarioActualService.obtenerPerfilActual())
                .thenReturn(new PerfilUsuarioActual(30L, TipoPerfilUsuario.MONITOR));

        mockMvc.perform(get("/api/consultas/10/expediente/archivos"))
                .andExpect(status().isNotFound());
    }

    @Test
    void perfilConciliadorRecibe404NotFoundEnExpedienteGeneral() throws Exception {
        permitirLecturaConsultas();
        Consulta consulta = new Consulta();

        when(consultaRepository.findById(10L)).thenReturn(Optional.of(consulta));
        when(usuarioActualService.obtenerPerfilActual())
                .thenReturn(new PerfilUsuarioActual(40L, TipoPerfilUsuario.CONCILIADOR));

        mockMvc.perform(get("/api/consultas/10/expediente/archivos"))
                .andExpect(status().isNotFound());
    }

    @Test
    void rangoFechasInvalidoRetorna400BadRequest() throws Exception {
        permitirLecturaConsultas();
        when(usuarioActualService.esRolAdministrador()).thenReturn(true);
        when(consultaRepository.findById(10L)).thenReturn(Optional.of(new Consulta()));

        mockMvc.perform(get("/api/consultas/10/expediente/archivos")
                        .param("fechaDesde", "2026-09-10")
                        .param("fechaHasta", "2026-09-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("La fecha desde no puede ser posterior a la fecha hasta"));
    }

    @Test
    void filtrosPropagadosCorrectamenteAServicio() throws Exception {
        permitirLecturaConsultas();
        when(usuarioActualService.esRolAdministrador()).thenReturn(true);
        when(consultaRepository.findById(10L)).thenReturn(Optional.of(new Consulta()));

        mockMvc.perform(get("/api/consultas/10/expediente/archivos")
                        .param("tipoDocumental", "PROCESO_DOCUMENTO")
                        .param("resourceType", "PROCESO")
                        .param("origen", "CARGA_USUARIO")
                        .param("autor", "abogado")
                        .param("fechaDesde", "2026-09-01")
                        .param("fechaHasta", "2026-09-05"))
                .andExpect(status().isOk());

        verify(fileAssetService).findExpedienteFiles(
                eq(10L),
                eq("PROCESO_DOCUMENTO"),
                eq("PROCESO"),
                eq("CARGA_USUARIO"),
                eq("abogado"),
                eq(LocalDate.of(2026, 9, 1).atStartOfDay()),
                eq(LocalDate.of(2026, 9, 5).atTime(23, 59, 59, 999999999)));
    }

    private void permitirLecturaConsultas() {
        when(usuarioActualService.tieneAlgunPermiso(
                co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_CONSULTAS,
                co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_CONSULTAS))
                .thenReturn(true);
    }

    private FileAsset crearAsset(Long id, String resourceType, Long resourceId, String fileName) {
        FileAsset asset = new FileAsset();
        asset.setId(id);
        asset.setDocumentoLogico(UUID.randomUUID());
        asset.setVersion(1);
        asset.setTipoDocumental("GENERAL");
        asset.setOrigen("CARGA_USUARIO");
        asset.setResourceType(resourceType);
        asset.setResourceId(resourceId);
        asset.setOriginalFileName(fileName);
        asset.setSize(1024L);
        asset.setContentType("application/pdf");
        asset.setStatus(FileAssetStatus.VIGENTE);
        UsuarioSistema user = new UsuarioSistema();
        user.setId(1L);
        user.setUsername("test@ufps.edu.co");
        asset.setUploadedBy(user);
        asset.setCreatedAt(LocalDateTime.now());
        return asset;
    }
}
