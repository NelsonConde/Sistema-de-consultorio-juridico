package co.edu.ufps.legal_cases.business.service.estadisticas.pdf;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;

import co.edu.ufps.legal_cases.business.dto.estadisticas.ConteoDTO;
import co.edu.ufps.legal_cases.business.dto.estadisticas.EstadisticasSemestreDTO;
import tools.jackson.databind.json.JsonMapper;

public class EstadisticasIntegracionPdfQaTest {

    private final EstadisticasPdfService service = new EstadisticasPdfService();
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void verificarIntegracionEstadisticasYGeneracionPdf() throws Exception {
        EstadisticasSemestreDTO stats = EstadisticasSemestreDTO.builder()
                .año(2026)
                .semestre(2)
                .periodoInicio("2026-06-01")
                .periodoFin("2026-12-31")
                .totalConsultas(45)
                .consultasFinalizadas(30)
                .consultasPendientes(15)
                .totalPersonasAtendidas(42)
                .totalConciliaciones(12)
                .totalSeguimientos(68)
                .totalEstudiantesActivos(23)
                .totalEstudiantesHabilitadosConciliacion(18)
                .consultasPorEstado(List.of(
                        new ConteoDTO("FINALIZADA", 30L),
                        new ConteoDTO("EN_PROCESO", 15L)
                ))
                .procesosPorEstado(List.of(
                        new ConteoDTO("EN_TRAMITE", 6L),
                        new ConteoDTO("FALLADO", 4L)
                ))
                .build();

        byte[] pdfBytes = service.generarReporteSemestral(stats);

        String pdfText = extractText(pdfBytes);

        // Validaciones semánticas y cuantitativas
        assertTrue(pdfText.contains("Período: 2026-06-01 — 2026-12-31"));
        assertTrue(pdfText.contains("Semestre 2 — 2026"));
        assertTrue(pdfText.contains("45")); // Total consultas
        assertTrue(pdfText.contains("30")); // Finalizadas
        assertTrue(pdfText.contains("15")); // Pendientes
        assertTrue(pdfText.contains("42")); // Personas atendidas
        assertTrue(pdfText.contains("12")); // Conciliaciones
        assertTrue(pdfText.contains("68")); // Seguimientos
        assertTrue(pdfText.contains("23")); // Estudiantes activos
        assertTrue(pdfText.contains("EN_TRAMITE"));
        assertTrue(pdfText.contains("FALLADO"));

        // Guardar evidencias en directorio de salida de pruebas
        Path dir = Paths.get("target/test-output/INT-02");
        Files.createDirectories(dir);

        Files.write(dir.resolve("estadisticas_descargadas.pdf"), pdfBytes);
        Files.writeString(dir.resolve("inspeccion_texto_pdf.txt"), pdfText);
        Files.writeString(dir.resolve("reporte_estadisticas_api.json"), jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(stats));
    }

    private static String extractText(byte[] pdfBytes) throws Exception {
        try (PdfDocument pdf = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdfBytes)))) {
            StringBuilder text = new StringBuilder();
            for (int page = 1; page <= pdf.getNumberOfPages(); page++) {
                text.append(PdfTextExtractor.getTextFromPage(pdf.getPage(page)));
            }
            return text.toString();
        }
    }
}
