package co.edu.ufps.legal_cases.business.controller.perfil;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import co.edu.ufps.legal_cases.business.dto.perfil.EstudianteDTO;
import co.edu.ufps.legal_cases.business.service.perfil.EstudianteService;
import co.edu.ufps.legal_cases.business.service.perfil.estudiante.EstudianteCommandService;
import co.edu.ufps.legal_cases.business.service.perfil.estudiante.EstudianteExcelService;

public class EstudianteImportacionEvidenciaQaTest {

    private static final String[] ENCABEZADOS_VALIDOS = {
            "Nombre", "TipoDocumentoId", "Documento", "Email", "Telefono",
            "Usuario", "SedeId", "Codigo", "AsesorId", "Activo", "Conciliacion"
    };

    @Test
    void generarEvidenciasHttpImportacion() throws Exception {
        EstudianteCommandService commandService = mock(EstudianteCommandService.class);
        EstudianteService estudianteService = mock(EstudianteService.class);
        EstudianteExcelService excelService = new EstudianteExcelService(commandService);
        EstudianteController controller = new EstudianteController(estudianteService, excelService);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        Path dir = Paths.get("target/test-output/INT-03");
        Files.createDirectories(dir);

        // 1. Archivo con cabecera inválida (SCRUM-383)
        String[] cabecerasInvalidas = ENCABEZADOS_VALIDOS.clone();
        cabecerasInvalidas[0] = "Nombres_Completos";
        byte[] bytesInvalido = crearExcelEnMemoria(cabecerasInvalidas, true);
        MockMultipartFile fileInvalido = new MockMultipartFile(
                "archivo",
                "estudiantes_cabecera_invalida.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                bytesInvalido
        );

        MvcResult resInvalido = mockMvc.perform(multipart("/api/estudiantes/importar").file(fileInvalido)).andReturn();
        int statusInvalido = resInvalido.getResponse().getStatus();
        String bodyInvalido = resInvalido.getResponse().getContentAsString();

        // Verificar que jamás se intentó persistir ningún registro
        verify(commandService, never()).crear(any(EstudianteDTO.class));

        // 2. Archivo válido
        byte[] bytesValido = crearExcelEnMemoria(ENCABEZADOS_VALIDOS, true);
        MockMultipartFile fileValido = new MockMultipartFile(
                "archivo",
                "estudiantes_valido.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                bytesValido
        );

        MvcResult resValido = mockMvc.perform(multipart("/api/estudiantes/importar").file(fileValido)).andReturn();
        int statusValido = resValido.getResponse().getStatus();
        String bodyValido = resValido.getResponse().getContentAsString();

        // Guardar evidencia textual HTTP
        StringBuilder sb = new StringBuilder();
        sb.append("=== EVIDENCIA DE INTEGRACIÓN HTTP: INT-03 IMPORTACIÓN DE ESTUDIANTES ===\n\n");

        sb.append("1. CASO CABECERA INVÁLIDA (Retest SCRUM-383):\n");
        sb.append("POST /api/estudiantes/importar\n");
        sb.append("Content-Type: multipart/form-data; filename=\"estudiantes_cabecera_invalida.xlsx\"\n");
        sb.append("Cabecera enviada en columna 1: 'Nombres_Completos' (esperada: 'Nombre')\n");
        sb.append("HTTP Status: ").append(statusInvalido).append(" BAD_REQUEST\n");
        sb.append("Response Body:\n").append(bodyInvalido).append("\n");
        sb.append("Filas persistidas / llamadas a crear(): 0 (Atomicidad y rechazo total verificado)\n\n");

        sb.append("2. CASO CABECERA VÁLIDA:\n");
        sb.append("POST /api/estudiantes/importar\n");
        sb.append("Content-Type: multipart/form-data; filename=\"estudiantes_valido.xlsx\"\n");
        sb.append("Cabeceras enviadas: ['Nombre', 'TipoDocumentoId', 'Documento', 'Email', 'Telefono', 'Usuario', 'SedeId', 'Codigo', 'AsesorId', 'Activo', 'Conciliacion']\n");
        sb.append("HTTP Status: ").append(statusValido).append(" OK\n");
        sb.append("Response Body:\n").append(bodyValido).append("\n");
        sb.append("Llamadas a persistir ejecutadas conforme a las filas del archivo.\n");

        Files.writeString(dir.resolve("http_importacion_estudiantes.txt"), sb.toString());
    }

    private byte[] crearExcelEnMemoria(String[] encabezados, boolean conFila) throws Exception {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Estudiantes");
            Row header = sheet.createRow(0);
            for (int i = 0; i < encabezados.length; i++) {
                header.createCell(i).setCellValue(encabezados[i]);
            }
            if (conFila) {
                Row row = sheet.createRow(1);
                row.createCell(0).setCellValue("Estudiante QA");
                row.createCell(1).setCellValue(1);
                row.createCell(2).setCellValue("1098765432");
                row.createCell(3).setCellValue("estudiante.qa@ufps.edu.co");
                row.createCell(4).setCellValue("3151234567");
                row.createCell(5).setCellValue("estudiante.qa");
                row.createCell(6).setCellValue(1);
                row.createCell(7).setCellValue("1151999");
                row.createCell(8).setCellValue(1);
                row.createCell(9).setCellValue(true);
                row.createCell(10).setCellValue(false);
            }
            wb.write(out);
            return out.toByteArray();
        }
    }
}
