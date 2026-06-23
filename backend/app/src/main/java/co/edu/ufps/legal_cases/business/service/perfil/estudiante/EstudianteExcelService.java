package co.edu.ufps.legal_cases.business.service.perfil.estudiante;

import java.io.IOException;
import java.lang.IllegalArgumentException;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import co.edu.ufps.legal_cases.business.dto.perfil.EstudianteDTO;
import co.edu.ufps.legal_cases.business.dto.perfil.ImportacionEstudiantesDTO;

@Service
public class EstudianteExcelService {

    private final EstudianteCommandService estudianteCommandService;

    public EstudianteExcelService(
            EstudianteCommandService estudianteCommandService
    ) {
        this.estudianteCommandService = estudianteCommandService;
    }


    public ImportacionEstudiantesDTO importar(
            MultipartFile archivo
    ) {

        ImportacionEstudiantesDTO resultado =
                new ImportacionEstudiantesDTO();

        try (
                Workbook workbook =
                        new XSSFWorkbook(
                                archivo.getInputStream()
                        )
        ) {

            Sheet sheet = workbook.getSheetAt(0);

            validarFormato(sheet);

            resultado.setTotalFilas(
                    sheet.getLastRowNum()
            );

            for (
                    int i = 1;
                    i <= sheet.getLastRowNum();
                    i++
            ) {

                try {

                    Row row = sheet.getRow(i);

                    if (row == null) {
                        continue;
                    }

                    EstudianteDTO dto =
                            convertirFila(row);

                    estudianteCommandService
                            .crear(dto);

                    resultado.setExitosos(
                            resultado.getExitosos() + 1
                    );

                } catch (Exception e) {

                    resultado.setFallidos(
                            resultado.getFallidos() + 1
                    );

                    resultado.getErrores().add(
                            "Fila "
                                    + (i + 1)
                                    + ": "
                                    + e.getMessage()
                    );
                }
            }

            return resultado;

        } catch (IOException e) {

            throw new RuntimeException(
                    "Error leyendo archivo Excel",
                    e
            );
        }
    }

    private void validarFormato(Sheet sheet) {

        Row header = sheet.getRow(0);

        if (header == null) {
            throw new IllegalArgumentException(
                    "El archivo no contiene encabezados"
            );
        }

        String[] esperados = {
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

        for (int i = 0; i < esperados.length; i++) {

            Cell cell = header.getCell(i);

            String valor =
                    cell == null
                            ? ""
                            : cell.getStringCellValue().trim();

            if (!esperados[i].equalsIgnoreCase(valor)) {

                throw new RuntimeException(
                        "Formato inválido. Se esperaba la columna '"
                                + esperados[i]
                                + "' en la posición "
                                + (i + 1)
                );
            }
        }
    }

    private EstudianteDTO convertirFila(
            Row row
    ) {

        EstudianteDTO dto =
                new EstudianteDTO();

        dto.setNombre(
                getString(row.getCell(0))
        );

        dto.setTipoDocumentoId(
                getLong(row.getCell(1))
        );

        dto.setDocumento(
                getString(row.getCell(2))
        );

        dto.setEmail(
                getString(row.getCell(3))
        );

        dto.setTelefono(
                getString(row.getCell(4))
        );

        dto.setUsuario(
                getString(row.getCell(5))
        );

        dto.setSedeId(
                getLong(row.getCell(6))
        );

        dto.setCodigo(
                getString(row.getCell(7))
        );

        dto.setAsesorId(
                getLong(row.getCell(8))
        );

        dto.setActivo(
                getBoolean(row.getCell(9), true)
        );

        dto.setConciliacion(
                getBoolean(row.getCell(10), false)
        );

        return dto;
    }

    private String getString(Cell cell) {

        if (cell == null) {
            return null;
        }

        cell.setCellType(CellType.STRING);

        return cell
                .getStringCellValue()
                .trim();
    }

    private Long getLong(Cell cell) {

        if (cell == null) {
            return null;
        }

        return (long)
                cell.getNumericCellValue();
    }

    private Boolean getBoolean(
            Cell cell,
            boolean defaultValue
    ) {

        if (cell == null) {
            return defaultValue;
        }

        return switch (cell.getCellType()) {

            case BOOLEAN ->
                    cell.getBooleanCellValue();

            case STRING ->
                    Boolean.parseBoolean(
                            cell.getStringCellValue()
                    );

            default ->
                    defaultValue;
        };
    }
}