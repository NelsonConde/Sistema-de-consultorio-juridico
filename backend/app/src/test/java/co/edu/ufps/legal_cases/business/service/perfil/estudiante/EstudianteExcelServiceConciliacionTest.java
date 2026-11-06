package co.edu.ufps.legal_cases.business.service.perfil.estudiante;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayOutputStream;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import co.edu.ufps.legal_cases.business.dto.perfil.EstudianteDTO;
import co.edu.ufps.legal_cases.business.dto.perfil.ImportacionEstudiantesDTO;

class EstudianteExcelServiceConciliacionTest {

    @Test
    void importarDebeConservarBanderaConciliacionDeCadaFila() throws Exception {
        EstudianteCommandService commandService = mock(EstudianteCommandService.class);
        EstudianteExcelService excelService = new EstudianteExcelService(commandService);

        byte[] archivoExcel = crearExcelPrueba();

        MockMultipartFile archivo = new MockMultipartFile(
                "archivo",
                "estudiantes-prueba.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                archivoExcel);

        ImportacionEstudiantesDTO resultado = excelService.importar(archivo);

        assertEquals(2, resultado.getTotalFilas());
        assertEquals(2, resultado.getExitosos());
        assertEquals(0, resultado.getFallidos());

        ArgumentCaptor<EstudianteDTO> captor =
                ArgumentCaptor.forClass(EstudianteDTO.class);

        verify(commandService, org.mockito.Mockito.times(2))
                .crear(captor.capture());

        EstudianteDTO primero = captor.getAllValues().get(0);
        EstudianteDTO segundo = captor.getAllValues().get(1);

        assertEquals(Boolean.TRUE, primero.getConciliacion());
        assertEquals(Boolean.FALSE, segundo.getConciliacion());
    }

    private byte[] crearExcelPrueba() throws Exception {
        try (
                Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream output = new ByteArrayOutputStream()
        ) {
            Sheet sheet = workbook.createSheet("Estudiantes");

            String[] encabezados = {
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

            Row header = sheet.createRow(0);

            for (int i = 0; i < encabezados.length; i++) {
                header.createCell(i).setCellValue(encabezados[i]);
            }

            Row filaTrue = sheet.createRow(1);
            filaTrue.createCell(0).setCellValue("Estudiante Excel True");
            filaTrue.createCell(1).setCellValue(1);
            filaTrue.createCell(2).setCellValue("900000001");
            filaTrue.createCell(3).setCellValue("excel.true@ufps.edu.co");
            filaTrue.createCell(4).setCellValue("3000000001");
            filaTrue.createCell(5).setCellValue("excel.true");
            filaTrue.createCell(6).setCellValue(1);
            filaTrue.createCell(7).setCellValue("1152901");
            filaTrue.createCell(8).setCellValue(1);
            filaTrue.createCell(9).setCellValue(true);
            filaTrue.createCell(10).setCellValue(true);

            Row filaFalse = sheet.createRow(2);
            filaFalse.createCell(0).setCellValue("Estudiante Excel False");
            filaFalse.createCell(1).setCellValue(1);
            filaFalse.createCell(2).setCellValue("900000002");
            filaFalse.createCell(3).setCellValue("excel.false@ufps.edu.co");
            filaFalse.createCell(4).setCellValue("3000000002");
            filaFalse.createCell(5).setCellValue("excel.false");
            filaFalse.createCell(6).setCellValue(1);
            filaFalse.createCell(7).setCellValue("1152902");
            filaFalse.createCell(8).setCellValue(1);
            filaFalse.createCell(9).setCellValue(true);
            filaFalse.createCell(10).setCellValue(false);

            workbook.write(output);

            return output.toByteArray();
        }
    }
}