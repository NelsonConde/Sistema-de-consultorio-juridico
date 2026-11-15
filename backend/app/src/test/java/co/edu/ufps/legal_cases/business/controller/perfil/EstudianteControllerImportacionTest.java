package co.edu.ufps.legal_cases.business.controller.perfil;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayOutputStream;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;

import co.edu.ufps.legal_cases.business.dto.perfil.EstudianteDTO;
import co.edu.ufps.legal_cases.business.service.perfil.EstudianteService;
import co.edu.ufps.legal_cases.business.service.perfil.estudiante.EstudianteCommandService;
import co.edu.ufps.legal_cases.business.service.perfil.estudiante.EstudianteExcelService;

class EstudianteControllerImportacionTest {

    private static final String[] ENCABEZADOS = {
            "Nombre",
            "TipoDocumentoId",
            "Documento",
            "Email",
            "Telefono",
            "Usuario",
            "SedeId",
            "Codigo",
            "AsesorId",
            "Activo",
            "Conciliacion"
    };

    private EstudianteCommandService estudianteCommandService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        estudianteCommandService = mock(EstudianteCommandService.class);
        EstudianteExcelService excelService = new EstudianteExcelService(
                estudianteCommandService);
        mockMvc = crearMockMvc(excelService);
    }

    @Test
    void importarConPrimeraColumnaIncorrectaDebeResponder400SinCrearEstudiantes()
            throws Exception {
        String[] encabezadosInvalidos = ENCABEZADOS.clone();
        encabezadosInvalidos[0] = "Nombres";

        MockMultipartFile archivo = crearArchivo(
                encabezadosInvalidos,
                true);

        mockMvc.perform(multipart("/api/estudiantes/importar")
                        .file(archivo))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Nombre")))
                .andExpect(content().string(containsString("posición 1")));

        verify(estudianteCommandService, never())
                .crear(any(EstudianteDTO.class));
    }

    @Test
    void importarConPrimeraColumnaNumericaDebeResponder400SinCrearEstudiantes()
            throws Exception {
        MockMultipartFile archivo = crearArchivo(
                ENCABEZADOS,
                true,
                true);

        mockMvc.perform(multipart("/api/estudiantes/importar")
                        .file(archivo))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Nombre")))
                .andExpect(content().string(containsString("posición 1")));

        verify(estudianteCommandService, never())
                .crear(any(EstudianteDTO.class));
    }

    @Test
    void importarConCabeceraValidaDebeConservarElProcesamientoActual()
            throws Exception {
        MockMultipartFile archivo = crearArchivo(ENCABEZADOS, true);

        mockMvc.perform(multipart("/api/estudiantes/importar")
                        .file(archivo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFilas").value(1))
                .andExpect(jsonPath("$.exitosos").value(1))
                .andExpect(jsonPath("$.fallidos").value(0));

        verify(estudianteCommandService)
                .crear(any(EstudianteDTO.class));
    }

    @Test
    void importarSinCabeceraDebeResponder400SinCrearEstudiantes()
            throws Exception {
        MockMultipartFile archivo = crearArchivo(null, false);

        mockMvc.perform(multipart("/api/estudiantes/importar")
                        .file(archivo))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("El archivo no contiene encabezados"));

        verify(estudianteCommandService, never())
                .crear(any(EstudianteDTO.class));
    }

    @Test
    void importarConErrorInternoDebeConservarRespuesta500()
            throws Exception {
        EstudianteExcelService excelService = mock(EstudianteExcelService.class);
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo",
                "estudiantes.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[] { 1 });
        when(excelService.importar(any(MultipartFile.class)))
                .thenThrow(new RuntimeException("fallo de almacenamiento"));

        MockMvc mockMvcConFalloInterno = crearMockMvc(excelService);

        mockMvcConFalloInterno.perform(multipart("/api/estudiantes/importar")
                        .file(archivo))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(
                        "Error interno: fallo de almacenamiento"));
    }

    private MockMvc crearMockMvc(EstudianteExcelService excelService) {
        EstudianteController controller = new EstudianteController(
                mock(EstudianteService.class),
                excelService);
        return MockMvcBuilders.standaloneSetup(controller).build();
    }

    private MockMultipartFile crearArchivo(
            String[] encabezados,
            boolean incluirEstudiante
    ) throws Exception {
        return crearArchivo(encabezados, incluirEstudiante, false);
    }

    private MockMultipartFile crearArchivo(
            String[] encabezados,
            boolean incluirEstudiante,
            boolean primerEncabezadoNumerico
    ) throws Exception {
        try (
                Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream output = new ByteArrayOutputStream()
        ) {
            Sheet sheet = workbook.createSheet("Estudiantes");

            if (encabezados != null) {
                Row header = sheet.createRow(0);
                for (int i = 0; i < encabezados.length; i++) {
                    if (i == 0 && primerEncabezadoNumerico) {
                        header.createCell(i).setCellValue(123);
                    } else {
                        header.createCell(i).setCellValue(encabezados[i]);
                    }
                }
            }

            if (incluirEstudiante) {
                agregarEstudiante(sheet.createRow(1));
            }

            workbook.write(output);
            return new MockMultipartFile(
                    "archivo",
                    "estudiantes.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    output.toByteArray());
        }
    }

    private void agregarEstudiante(Row row) {
        row.createCell(0).setCellValue("Estudiante Prueba");
        row.createCell(1).setCellValue(1);
        row.createCell(2).setCellValue("100000001");
        row.createCell(3).setCellValue("estudiante@ufps.edu.co");
        row.createCell(4).setCellValue("3000000001");
        row.createCell(5).setCellValue("estudiante.prueba");
        row.createCell(6).setCellValue(1);
        row.createCell(7).setCellValue("1150001");
        row.createCell(8).setCellValue(1);
        row.createCell(9).setCellValue(true);
        row.createCell(10).setCellValue(false);
    }
}
