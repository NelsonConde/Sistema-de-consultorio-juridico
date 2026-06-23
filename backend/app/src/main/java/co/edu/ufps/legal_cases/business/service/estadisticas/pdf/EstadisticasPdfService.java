package co.edu.ufps.legal_cases.business.service.estadisticas.pdf;

import java.io.ByteArrayOutputStream;
import java.util.List;

import org.springframework.stereotype.Service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import co.edu.ufps.legal_cases.business.dto.estadisticas.ConteoDTO;
import co.edu.ufps.legal_cases.business.dto.estadisticas.EstadisticasSemestreDTO;

@Service
public class EstadisticasPdfService {

    private static final DeviceRgb COLOR_HEADER = new DeviceRgb(45, 74, 122);
    private static final DeviceRgb COLOR_FILA_PAR = new DeviceRgb(240, 244, 248);

    public byte[] generarReporteSemestral(EstadisticasSemestreDTO stats) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document doc = new Document(pdf);
        doc.setMargins(40, 50, 40, 50);

        doc.add(new Paragraph("CONSULTORIO JURÍDICO UFPS")
                .setFontSize(16).setBold().setTextAlignment(TextAlignment.CENTER)
                .setFontColor(COLOR_HEADER));
        doc.add(new Paragraph("Reporte de Estadísticas Semestrales")
                .setFontSize(12).setTextAlignment(TextAlignment.CENTER)
                .setFontColor(ColorConstants.DARK_GRAY));
        doc.add(new Paragraph("Período: " + stats.getPeriodoInicio() + " — " + stats.getPeriodoFin())
                .setFontSize(10).setTextAlignment(TextAlignment.CENTER)
                .setFontColor(ColorConstants.DARK_GRAY));
        // Si semestre y año son null es un reporte de rango libre, no de semestre predefinido.
        String subtituloSemestre = (stats.getSemestre() != null && stats.getAño() != null)
                ? "Semestre " + stats.getSemestre() + " — " + stats.getAño()
                : "Reporte personalizado";
        doc.add(new Paragraph(subtituloSemestre)
                .setFontSize(10).setBold().setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20));

        doc.add(seccionTitulo("1. Resumen General"));
        Table resumen = tablaDoble();
        agregarFilaResumen(resumen, "Total de consultas atendidas", stats.getTotalConsultas(), false);
        agregarFilaResumen(resumen, "Consultas finalizadas", stats.getConsultasFinalizadas(), true);
        agregarFilaResumen(resumen, "Consultas pendientes", stats.getConsultasPendientes(), false);
        agregarFilaResumen(resumen, "Total de personas atendidas", stats.getTotalPersonasAtendidas(), true);
        agregarFilaResumen(resumen, "Total de conciliaciones", stats.getTotalConciliaciones(), false);
        agregarFilaResumen(resumen, "Total de seguimientos", stats.getTotalSeguimientos(), true);
        agregarFilaResumen(resumen, "Estudiantes activos", stats.getTotalEstudiantesActivos(), false);
        agregarFilaResumen(resumen, "Estudiantes habilitados para conciliación",
                stats.getTotalEstudiantesHabilitadosConciliacion(), true);
        doc.add(resumen);
        espacio(doc);

        agregarSeccionConteo(doc, "2. Consultas por Estado", stats.getConsultasPorEstado(), "Estado", "Cantidad");
        agregarSeccionConteo(doc, "3. Consultas por Área Jurídica", stats.getConsultasPorArea(), "Área", "Cantidad");
        agregarSeccionConteo(doc, "4. Consultas por Tipo de Violencia", stats.getConsultasPorTipoViolencia(), "Tipo de Violencia", "Cantidad");
        agregarSeccionConteo(doc, "5. Personas Atendidas por Género", stats.getPersonasPorGenero(), "Género", "Personas");
        agregarSeccionConteo(doc, "6. Personas por Estrato Socioeconómico", stats.getPersonasPorEstrato(), "Estrato", "Personas");
        agregarSeccionConteo(doc, "7. Personas por Zona", stats.getPersonasPorZona(), "Zona", "Personas");
        agregarSeccionConteo(doc, "8. Personas por Grupo Étnico", stats.getPersonasPorGrupoEtnico(), "Grupo Étnico", "Personas");
        agregarSeccionConteo(doc, "9. Personas por Municipio", stats.getPersonasPorMunicipio(), "Municipio", "Personas");
        agregarSeccionConteo(doc, "10. Personas por Condición", stats.getPersonasPorCondicion(), "Condición", "Personas");
        agregarSeccionConteo(doc, "11. Procesos por Estado", stats.getProcesosPorEstado(), "Estado", "Cantidad");
        agregarSeccionConteo(doc, "12. Conciliaciones por Estado", stats.getConciliacionesPorEstado(), "Estado", "Cantidad");
        agregarSeccionConteo(doc, "13. Seguimientos por Estado", stats.getSeguimientosPorEstado(), "Estado", "Cantidad");

        doc.add(new Paragraph("").setMarginBottom(20));
        doc.add(new Paragraph("Documento generado el " + java.time.LocalDate.now() +
                " — Sistema de Gestión de Casos Jurídicos UFPS")
                .setFontSize(8).setTextAlignment(TextAlignment.CENTER)
                .setFontColor(ColorConstants.GRAY));

        doc.close();
        return baos.toByteArray();
    }

    private void agregarSeccionConteo(Document doc, String titulo, List<ConteoDTO> datos,
                                      String col1, String col2) {
        if (datos == null || datos.isEmpty()) return;
        doc.add(seccionTitulo(titulo));
        Table t = tablaDoble();
        agregarCabecera(t, col1, col2);
        boolean par = false;
        for (ConteoDTO item : datos) {
            agregarFilaConteo(t, item.getNombre(), String.valueOf(item.getCantidad()), par);
            par = !par;
        }
        doc.add(t);
        espacio(doc);
    }

    private Paragraph seccionTitulo(String texto) {
        return new Paragraph(texto).setFontSize(11).setBold()
                .setFontColor(COLOR_HEADER).setMarginBottom(5);
    }

    private Table tablaDoble() {
        return new Table(UnitValue.createPercentArray(new float[]{70, 30}))
                .setWidth(UnitValue.createPercentValue(100));
    }

    private void agregarCabecera(Table table, String col1, String col2) {
        table.addHeaderCell(new Cell()
                .add(new Paragraph(col1).setBold().setFontColor(ColorConstants.WHITE))
                .setBackgroundColor(COLOR_HEADER).setPadding(5));
        table.addHeaderCell(new Cell()
                .add(new Paragraph(col2).setBold().setFontColor(ColorConstants.WHITE)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(COLOR_HEADER).setPadding(5));
    }

    private void agregarFilaResumen(Table table, String etiqueta, long valor, boolean par) {
        DeviceRgb bg = par ? COLOR_FILA_PAR : null;
        Cell c1 = new Cell().add(new Paragraph(etiqueta).setFontSize(9)).setPadding(5);
        Cell c2 = new Cell().add(new Paragraph(String.valueOf(valor)).setFontSize(9).setBold()
                .setTextAlignment(TextAlignment.CENTER)).setPadding(5);
        if (bg != null) { c1.setBackgroundColor(bg); c2.setBackgroundColor(bg); }
        table.addCell(c1);
        table.addCell(c2);
    }

    private void agregarFilaConteo(Table table, String nombre, String cantidad, boolean par) {
        DeviceRgb bg = par ? COLOR_FILA_PAR : null;
        Cell c1 = new Cell().add(new Paragraph(nombre).setFontSize(9)).setPadding(5);
        Cell c2 = new Cell().add(new Paragraph(cantidad).setFontSize(9)
                .setTextAlignment(TextAlignment.CENTER)).setPadding(5);
        if (bg != null) { c1.setBackgroundColor(bg); c2.setBackgroundColor(bg); }
        table.addCell(c1);
        table.addCell(c2);
    }

    private void espacio(Document doc) {
        doc.add(new Paragraph("").setMarginBottom(12));
    }
}