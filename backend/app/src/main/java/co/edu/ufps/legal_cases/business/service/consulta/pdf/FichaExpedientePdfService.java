package co.edu.ufps.legal_cases.business.service.consulta.pdf;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import co.edu.ufps.legal_cases.business.dto.consulta.ficha.FichaExpedienteDTO;
import co.edu.ufps.legal_cases.business.dto.consulta.ficha.FichaExpedienteDTO.ConciliacionFichaDTO;
import co.edu.ufps.legal_cases.business.dto.consulta.ficha.FichaExpedienteDTO.DocumentoIndiceDTO;
import co.edu.ufps.legal_cases.business.dto.consulta.ficha.FichaExpedienteDTO.PersonaDetalleDTO;
import co.edu.ufps.legal_cases.business.dto.consulta.ficha.FichaExpedienteDTO.ProcesoFichaDTO;
import co.edu.ufps.legal_cases.business.dto.consulta.ficha.FichaExpedienteDTO.ResponsableDetalleDTO;
import co.edu.ufps.legal_cases.business.dto.consulta.ficha.FichaExpedienteDTO.SeguimientoFichaDTO;

/**
 * Servicio encargado de la renderización del expediente integral en formato PDF bajo demanda (PB-35).
 * Genera el documento completamente en memoria (sin almacenar copias en disco ni bucket),
 * estructurando la información vigente y garantizando que no se expongan rutas internas de almacenamiento.
 */
@Service
public class FichaExpedientePdfService {

    private static final DeviceRgb COLOR_PRIMARIO = new DeviceRgb(30, 58, 95);      // Azul institucional profundo
    private static final DeviceRgb COLOR_SECUNDARIO = new DeviceRgb(163, 0, 0);     // Rojo UFPS
    private static final DeviceRgb COLOR_CABECERA = new DeviceRgb(45, 74, 122);      // Azul cabecera
    private static final DeviceRgb COLOR_FONDO_SECCION = new DeviceRgb(240, 244, 250);
    private static final DeviceRgb COLOR_FILA_PAR = new DeviceRgb(248, 250, 252);
    private static final DeviceRgb COLOR_BORDE = new DeviceRgb(218, 224, 233);

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public byte[] generarPdf(FichaExpedienteDTO ficha) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);

        // Agrega pie de página con número de página y aviso de confidencialidad
        pdf.addEventHandler(PdfDocumentEvent.END_PAGE, new PiePaginaEventHandler());

        Document doc = new Document(pdf);
        doc.setMargins(36, 40, 40, 40);

        // Encabezado institucional
        agregarEncabezado(doc, ficha);

        // 1. Identificación y Estado
        agregarSeccionIdentificacionYEstado(doc, ficha);

        // 2. Clasificación del Asunto
        agregarSeccionClasificacion(doc, ficha);

        // 3. Partes Intervinientes
        agregarSeccionPartes(doc, ficha);

        // 4. Responsables Asignados
        agregarSeccionResponsables(doc, ficha);

        // 5. Seguimientos y Tareas
        agregarSeccionSeguimientos(doc, ficha);

        // 6. Proceso Judicial y Conciliación
        agregarSeccionProcesoYConciliacion(doc, ficha);

        // 7. Índice Documental
        agregarSeccionIndiceDocumental(doc, ficha);

        // Cierre
        doc.close();
        return baos.toByteArray();
    }

    private void agregarEncabezado(Document doc, FichaExpedienteDTO ficha) {
        Table headerTable = new Table(UnitValue.createPercentArray(new float[]{65, 35}))
                .setWidth(UnitValue.createPercentValue(100));

        Cell celdaIzq = new Cell()
                .setBorder(null)
                .add(new Paragraph("UNIVERSIDAD FRANCISCO DE PAULA SANTANDER")
                        .setFontSize(12).setBold().setFontColor(COLOR_SECUNDARIO).setMargin(0))
                .add(new Paragraph("FACULTAD DE CIENCIAS JURÍDICAS Y POLÍTICAS\nCONSULTORIO JURÍDICO")
                        .setFontSize(9).setBold().setFontColor(COLOR_PRIMARIO).setMargin(0))
                .add(new Paragraph("Ficha Integral del Expediente Jurídico")
                        .setFontSize(11).setBold().setFontColor(ColorConstants.BLACK).setMarginTop(3));

        Long idConsulta = ficha.getIdentificacion() != null ? ficha.getIdentificacion().getConsultaId() : null;
        String idTexto = idConsulta != null ? "#" + idConsulta : "N/A";

        Cell celdaDer = new Cell()
                .setBorder(null)
                .setTextAlignment(TextAlignment.RIGHT)
                .add(new Paragraph("RADICADO / EXPEDIENTE")
                        .setFontSize(9).setBold().setFontColor(COLOR_CABECERA).setMargin(0))
                .add(new Paragraph(idTexto)
                        .setFontSize(16).setBold().setFontColor(COLOR_PRIMARIO).setMargin(0))
                .add(new Paragraph("Emisión: " + java.time.LocalDate.now().format(FORMATO_FECHA))
                        .setFontSize(8).setFontColor(ColorConstants.DARK_GRAY).setMarginTop(2));

        headerTable.addCell(celdaIzq);
        headerTable.addCell(celdaDer);
        doc.add(headerTable);

        // Línea divisoria decorativa
        Table linea = new Table(1).setWidth(UnitValue.createPercentValue(100)).setMarginTop(5).setMarginBottom(10);
        linea.addCell(new Cell().setHeight(2).setBackgroundColor(COLOR_PRIMARIO).setBorder(null));
        doc.add(linea);
    }

    private void agregarSeccionIdentificacionYEstado(Document doc, FichaExpedienteDTO ficha) {
        doc.add(tituloSeccion("1. IDENTIFICACIÓN Y ESTADO DEL EXPEDIENTE"));

        var iden = ficha.getIdentificacion();
        var est = ficha.getEstadoResultado();

        Table tabla = tablaGrid(4, new float[]{22, 28, 22, 28});
        agregarParClaveValor(tabla, "Fecha Radicación:", iden != null && iden.getFechaRadicacion() != null ? iden.getFechaRadicacion().format(FORMATO_FECHA) : "N/A");
        agregarParClaveValor(tabla, "Trámite:", iden != null ? texto(iden.getTramite()) : "N/A");
        agregarParClaveValor(tabla, "Estado Actual:", est != null ? texto(est.getEstado()) : "N/A");
        agregarParClaveValor(tabla, "Última Actualización:", est != null && est.getUltimaActualizacion() != null ? est.getUltimaActualizacion().format(FORMATO_FECHA) : "N/A");
        agregarParClaveValor(tabla, "Resultado:", est != null ? texto(est.getResultado(), "Pendiente") : "Pendiente", 4);
        doc.add(tabla);

        if (iden != null) {
            doc.add(bloqueTexto("Descripción:", iden.getDescripcion()));
            doc.add(bloqueTexto("Hechos:", iden.getHechos()));
            doc.add(bloqueTexto("Pretensiones:", iden.getPretensiones()));
            doc.add(bloqueTexto("Concepto Jurídico:", iden.getConceptoJuridico()));
            if (iden.getObservaciones() != null && !iden.getObservaciones().isBlank()) {
                doc.add(bloqueTexto("Observaciones:", iden.getObservaciones()));
            }
        }
        doc.add(espacio());
    }

    private void agregarSeccionClasificacion(Document doc, FichaExpedienteDTO ficha) {
        doc.add(tituloSeccion("2. CLASIFICACIÓN DEL ASUNTO"));
        var clas = ficha.getClasificacion();

        Table tabla = tablaGrid(4, new float[]{20, 30, 20, 30});
        agregarParClaveValor(tabla, "Sede:", clas != null ? texto(clas.getSede()) : "N/A");
        agregarParClaveValor(tabla, "Área Jurídica:", clas != null ? texto(clas.getArea()) : "N/A");
        agregarParClaveValor(tabla, "Tema:", clas != null ? texto(clas.getTema()) : "N/A");
        agregarParClaveValor(tabla, "Tipo de Caso:", clas != null ? texto(clas.getTipo()) : "N/A");
        agregarParClaveValor(tabla, "Tipo de Violencia:", clas != null ? texto(clas.getTipoViolencia(), "No registra") : "No registra", 4);
        doc.add(tabla);
        doc.add(espacio());
    }

    private void agregarSeccionPartes(Document doc, FichaExpedienteDTO ficha) {
        doc.add(tituloSeccion("3. PARTES INTERVINIENTES"));
        var partes = ficha.getPartes();

        // Solicitante Principal
        PersonaDetalleDTO sol = partes != null ? partes.getSolicitantePrincipal() : null;
        Paragraph subSol = new Paragraph("Solicitante Principal")
                .setFontSize(9).setBold().setFontColor(COLOR_PRIMARIO).setMarginBottom(3);
        doc.add(subSol);

        if (sol != null) {
            Table tablaSol = tablaGrid(4, new float[]{20, 30, 20, 30});
            agregarParClaveValor(tablaSol, "Nombre Completo:", texto(sol.getNombreCompleto()));
            agregarParClaveValor(tablaSol, "Documento:", texto(sol.getTipoDocumento()) + " " + texto(sol.getNumeroDocumento()));
            agregarParClaveValor(tablaSol, "Teléfono:", texto(sol.getTelefono()));
            agregarParClaveValor(tablaSol, "Correo Electrónico:", texto(sol.getCorreo()));
            agregarParClaveValor(tablaSol, "Dirección / Ciudad:", texto(sol.getDireccion()) + " (" + texto(sol.getMunicipio()) + ")");
            agregarParClaveValor(tablaSol, "Estrato Socioeconómico:", sol.getEstrato() != null ? String.valueOf(sol.getEstrato()) : "N/A");
            doc.add(tablaSol);
        } else {
            doc.add(parrafoVacio("No registra solicitante principal."));
        }

        // Partes adicionales
        List<PersonaDetalleDTO> adicionales = partes != null ? partes.getPartesAdicionales() : List.of();
        doc.add(new Paragraph("Partes Adicionales (" + adicionales.size() + ")")
                .setFontSize(9).setBold().setFontColor(COLOR_CABECERA).setMarginTop(5).setMarginBottom(3));
        if (adicionales.isEmpty()) {
            doc.add(parrafoVacio("No registra partes adicionales."));
        } else {
            Table t = tablaLista(new float[]{35, 25, 20, 20});
            agregarCabeceraTabla(t, "Nombre", "Documento", "Teléfono", "Correo");
            boolean par = false;
            for (PersonaDetalleDTO p : adicionales) {
                agregarFilaTabla(t, par, texto(p.getNombreCompleto()), texto(p.getTipoDocumento()) + " " + texto(p.getNumeroDocumento()), texto(p.getTelefono()), texto(p.getCorreo()));
                par = !par;
            }
            doc.add(t);
        }

        // Contrapartes
        List<PersonaDetalleDTO> contrapartes = partes != null ? partes.getContrapartes() : List.of();
        doc.add(new Paragraph("Contrapartes (" + contrapartes.size() + ")")
                .setFontSize(9).setBold().setFontColor(COLOR_CABECERA).setMarginTop(5).setMarginBottom(3));
        if (contrapartes.isEmpty()) {
            doc.add(parrafoVacio("No registra contrapartes."));
        } else {
            Table t = tablaLista(new float[]{35, 25, 20, 20});
            agregarCabeceraTabla(t, "Nombre", "Documento", "Teléfono", "Correo");
            boolean par = false;
            for (PersonaDetalleDTO p : contrapartes) {
                agregarFilaTabla(t, par, texto(p.getNombreCompleto()), texto(p.getTipoDocumento()) + " " + texto(p.getNumeroDocumento()), texto(p.getTelefono()), texto(p.getCorreo()));
                par = !par;
            }
            doc.add(t);
        }

        doc.add(espacio());
    }

    private void agregarSeccionResponsables(Document doc, FichaExpedienteDTO ficha) {
        doc.add(tituloSeccion("4. RESPONSABLES ASIGNADOS"));
        var resp = ficha.getResponsables();

        Table tabla = tablaGrid(3, new float[]{33.3f, 33.3f, 33.4f});
        agregarCeldaResponsable(tabla, "Estudiante a Cargo", resp != null ? resp.getEstudiante() : null, true);
        agregarCeldaResponsable(tabla, "Docente Asesor", resp != null ? resp.getAsesor() : null, false);
        agregarCeldaResponsable(tabla, "Monitor", resp != null ? resp.getMonitor() : null, false);
        doc.add(tabla);
        doc.add(espacio());
    }

    private void agregarSeccionSeguimientos(Document doc, FichaExpedienteDTO ficha) {
        doc.add(tituloSeccion("5. HISTORIAL DE SEGUIMIENTOS Y TAREAS"));
        List<SeguimientoFichaDTO> lista = ficha.getSeguimientos();

        if (lista == null || lista.isEmpty()) {
            doc.add(parrafoVacio("No se registran seguimientos activos en este expediente."));
        } else {
            Table t = tablaLista(new float[]{14, 14, 18, 14, 16, 24});
            agregarCabeceraTabla(t, "F. Creación", "F. Entrega", "Categoría", "Estado", "Autor", "Descripción");
            boolean par = false;
            for (SeguimientoFichaDTO s : lista) {
                String fCrea = s.getFechaCreacion() != null ? s.getFechaCreacion().format(FORMATO_FECHA) : "N/A";
                String fEntrega = s.getFechaEntrega() != null ? s.getFechaEntrega().format(FORMATO_FECHA) : "Sin fecha";
                agregarFilaTabla(t, par, fCrea, fEntrega, texto(s.getCategoria()), texto(s.getEstado()), texto(s.getAutor()), texto(s.getDescripcion()));
                par = !par;
            }
            doc.add(t);
        }
        doc.add(espacio());
    }

    private void agregarSeccionProcesoYConciliacion(Document doc, FichaExpedienteDTO ficha) {
        doc.add(tituloSeccion("6. VINCULACIÓN A PROCESO JUDICIAL Y CONCILIACIÓN"));

        // Proceso Judicial
        List<ProcesoFichaDTO> procesos = ficha.getProcesos();
        doc.add(new Paragraph("Procesos Judiciales Vinculados")
                .setFontSize(9).setBold().setFontColor(COLOR_PRIMARIO).setMarginBottom(3));
        if (procesos == null || procesos.isEmpty()) {
            doc.add(parrafoVacio("No registra proceso judicial vinculado."));
        } else {
            Table t = tablaLista(new float[]{25, 20, 20, 20, 15});
            agregarCabeceraTabla(t, "Radicado", "Órgano Control", "Especialidad", "Departamento", "Estado");
            boolean par = false;
            for (ProcesoFichaDTO p : procesos) {
                agregarFilaTabla(t, par, texto(p.getNumeroRadicado()), texto(p.getOrganoControl()), texto(p.getEspecialidad()), texto(p.getDepartamento()), texto(p.getEstado()));
                par = !par;
            }
            doc.add(t);
        }

        // Conciliación
        List<ConciliacionFichaDTO> conciliaciones = ficha.getConciliaciones();
        doc.add(new Paragraph("Trámites de Conciliación Vinculados")
                .setFontSize(9).setBold().setFontColor(COLOR_PRIMARIO).setMarginTop(5).setMarginBottom(3));
        if (conciliaciones == null || conciliaciones.isEmpty()) {
            doc.add(parrafoVacio("No registra trámite de conciliación vinculado."));
        } else {
            Table t = tablaLista(new float[]{15, 20, 25, 20, 20});
            agregarCabeceraTabla(t, "ID", "Estado", "F. Conciliación", "Conciliador", "Estudiante");
            boolean par = false;
            for (ConciliacionFichaDTO c : conciliaciones) {
                String fConc = c.getFechaConciliacion() != null ? c.getFechaConciliacion().format(FORMATO_FECHA) : "Por programar";
                agregarFilaTabla(t, par, "#" + c.getId(), texto(c.getEstado()), fConc, texto(c.getConciliadorNombre()), texto(c.getEstudianteNombre()));
                par = !par;
            }
            doc.add(t);
        }

        doc.add(espacio());
    }

    private void agregarSeccionIndiceDocumental(Document doc, FichaExpedienteDTO ficha) {
        doc.add(tituloSeccion("7. ÍNDICE DOCUMENTAL (METADATOS DE ARCHIVOS VIGENTES)"));
        List<DocumentoIndiceDTO> docs = ficha.getIndiceDocumental();

        if (docs == null || docs.isEmpty()) {
            doc.add(parrafoVacio("No se registran documentos vigentes vinculados al expediente."));
        } else {
            Table t = tablaLista(new float[]{10, 30, 15, 15, 15, 15});
            agregarCabeceraTabla(t, "ID", "Nombre de Archivo", "Origen", "Tamaño", "Fecha Carga", "Cargado Por");
            boolean par = false;
            for (DocumentoIndiceDTO d : docs) {
                String fCarga = d.getFechaCarga() != null ? d.getFechaCarga().format(FORMATO_FECHA_HORA) : "N/A";
                agregarFilaTabla(t, par, "#" + d.getId(), texto(d.getNombreArchivo()), texto(d.getRecursoOrigen()), texto(d.getTamañoFormateado()), fCarga, texto(d.getCargadoPor()));
                par = !par;
            }
            doc.add(t);
        }

        // Nota de seguridad y confidencialidad
        Paragraph nota = new Paragraph("Aviso de Seguridad: Este documento contiene información confidencial protegida por secreto profesional y reserva legal. No almacena copia física en el servidor ni divulga claves o rutas internas de almacenamiento.")
                .setFontSize(7).setItalic().setFontColor(ColorConstants.GRAY).setTextAlignment(TextAlignment.CENTER).setMarginTop(8);
        doc.add(nota);
    }

    // --- Helpers de maquetación iText 7 ---

    private Paragraph tituloSeccion(String titulo) {
        return new Paragraph(titulo)
                .setFontSize(10).setBold()
                .setFontColor(COLOR_PRIMARIO)
                .setBackgroundColor(COLOR_FONDO_SECCION)
                .setPadding(4)
                .setMarginTop(4)
                .setMarginBottom(4);
    }

    private Table tablaGrid(int cols, float[] porcentajes) {
        return new Table(UnitValue.createPercentArray(porcentajes))
                .setWidth(UnitValue.createPercentValue(100))
                .setBorder(new SolidBorder(COLOR_BORDE, 0.5f));
    }

    private Table tablaLista(float[] porcentajes) {
        return new Table(UnitValue.createPercentArray(porcentajes))
                .setWidth(UnitValue.createPercentValue(100))
                .setBorder(new SolidBorder(COLOR_BORDE, 0.5f));
    }

    private void agregarCabeceraTabla(Table t, String... headers) {
        for (String h : headers) {
            t.addHeaderCell(new Cell()
                    .add(new Paragraph(h).setFontSize(8).setBold().setFontColor(ColorConstants.WHITE))
                    .setBackgroundColor(COLOR_CABECERA)
                    .setPadding(3)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setBorder(new SolidBorder(COLOR_BORDE, 0.5f)));
        }
    }

    private void agregarFilaTabla(Table t, boolean par, String... valores) {
        DeviceRgb bg = par ? COLOR_FILA_PAR : null;
        for (String v : valores) {
            Cell c = new Cell()
                    .add(new Paragraph(v).setFontSize(7.5f))
                    .setPadding(3)
                    .setBorder(new SolidBorder(COLOR_BORDE, 0.5f));
            if (bg != null) c.setBackgroundColor(bg);
            t.addCell(c);
        }
    }

    private void agregarParClaveValor(Table t, String clave, String valor) {
        t.addCell(new Cell().add(new Paragraph(clave).setFontSize(8).setBold().setFontColor(COLOR_CABECERA))
                .setPadding(3).setBorder(new SolidBorder(COLOR_BORDE, 0.5f)));
        t.addCell(new Cell().add(new Paragraph(valor).setFontSize(8))
                .setPadding(3).setBorder(new SolidBorder(COLOR_BORDE, 0.5f)));
    }

    private void agregarParClaveValor(Table t, String clave, String valor, int colSpan) {
        t.addCell(new Cell().add(new Paragraph(clave).setFontSize(8).setBold().setFontColor(COLOR_CABECERA))
                .setPadding(3).setBorder(new SolidBorder(COLOR_BORDE, 0.5f)));
        t.addCell(new Cell(1, colSpan - 1).add(new Paragraph(valor).setFontSize(8))
                .setPadding(3).setBorder(new SolidBorder(COLOR_BORDE, 0.5f)));
    }

    private Paragraph bloqueTexto(String etiqueta, String contenido) {
        Paragraph p = new Paragraph().setMarginBottom(3);
        p.add(new Paragraph(etiqueta + " ").setFontSize(8).setBold().setFontColor(COLOR_CABECERA));
        p.add(new Paragraph(texto(contenido, "Sin información registrada")).setFontSize(8));
        return p;
    }

    private void agregarCeldaResponsable(Table t, String cargo, ResponsableDetalleDTO resp, boolean incluirCodigo) {
        Cell c = new Cell().setPadding(4).setBorder(new SolidBorder(COLOR_BORDE, 0.5f));
        c.add(new Paragraph(cargo).setFontSize(8).setBold().setFontColor(COLOR_CABECERA).setMargin(0));
        if (resp != null && resp.getNombre() != null && !resp.getNombre().isBlank()) {
            c.add(new Paragraph(resp.getNombre()).setFontSize(8).setBold().setMargin(0));
            if (incluirCodigo && resp.getCodigo() != null) {
                c.add(new Paragraph("Cód: " + resp.getCodigo()).setFontSize(7.5f).setFontColor(ColorConstants.DARK_GRAY).setMargin(0));
            }
            if (resp.getEmail() != null) {
                c.add(new Paragraph(resp.getEmail()).setFontSize(7).setFontColor(ColorConstants.GRAY).setMargin(0));
            }
            if (resp.getTelefono() != null) {
                c.add(new Paragraph("Tel: " + resp.getTelefono()).setFontSize(7).setFontColor(ColorConstants.GRAY).setMargin(0));
            }
        } else {
            c.add(new Paragraph("Sin asignar").setFontSize(7.5f).setItalic().setFontColor(ColorConstants.DARK_GRAY));
        }
        t.addCell(c);
    }

    private Paragraph parrafoVacio(String mensaje) {
        return new Paragraph(mensaje)
                .setFontSize(8).setItalic().setFontColor(ColorConstants.DARK_GRAY).setMarginBottom(3);
    }

    private Paragraph espacio() {
        return new Paragraph("").setMarginBottom(5);
    }

    private String texto(String val) {
        return texto(val, "N/A");
    }

    private String texto(String val, String fallback) {
        return (val == null || val.isBlank()) ? fallback : val.trim();
    }

    /**
     * Manejador de evento de página para numeración y nota confidencial inferior.
     */
    private static class PiePaginaEventHandler implements IEventHandler {
        @Override
        public void handleEvent(Event event) {
            PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
            PdfDocument pdfDoc = docEvent.getDocument();
            PdfPage page = docEvent.getPage();
            int numPagina = pdfDoc.getPageNumber(page);

            PdfCanvas canvas = new PdfCanvas(page.newContentStreamAfter(), page.getResources(), pdfDoc);
            try (Canvas pageCanvas = new Canvas(canvas, page.getPageSize())) {
                pageCanvas.showTextAligned(
                        new Paragraph(String.format("Página %d — Consultorio Jurídico UFPS — Expediente Confidencial", numPagina))
                                .setFontSize(7.5f).setFontColor(ColorConstants.GRAY),
                        page.getPageSize().getWidth() / 2, 18, TextAlignment.CENTER);
            }
        }
    }
}
