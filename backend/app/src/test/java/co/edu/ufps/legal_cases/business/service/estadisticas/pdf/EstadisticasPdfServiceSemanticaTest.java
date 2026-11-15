package co.edu.ufps.legal_cases.business.service.estadisticas.pdf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;

import co.edu.ufps.legal_cases.business.dto.estadisticas.ConteoDTO;
import co.edu.ufps.legal_cases.business.dto.estadisticas.EstadisticasSemestreDTO;

class EstadisticasPdfServiceSemanticaTest {

    private final EstadisticasPdfService service = new EstadisticasPdfService();

    @Test
    void comunicaEstadoActualDeEstudiantesYPeriodoDeProcesosSinAlterarValores() throws Exception {
        EstadisticasSemestreDTO stats = EstadisticasSemestreDTO.builder()
                .año(2026)
                .semestre(2)
                .periodoInicio("2026-06-01")
                .periodoFin("2026-12-31")
                .totalEstudiantesActivos(23)
                .procesosPorEstado(List.of(new ConteoDTO("EN_TRAMITE", 6L)))
                .build();

        byte[] result = service.generarReporteSemestral(stats);

        assertEquals("%PDF-", new String(result, 0, 5, java.nio.charset.StandardCharsets.US_ASCII));
        String text = extractText(result);
        assertTrue(text.contains("Estudiantes activos (estado actual, sin filtro de período)"));
        assertTrue(text.contains("Procesos por Estado (período del reporte)"));
        assertTrue(text.contains("23"));
        assertTrue(text.contains("EN_TRAMITE"));
        assertTrue(text.contains("6"));
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
