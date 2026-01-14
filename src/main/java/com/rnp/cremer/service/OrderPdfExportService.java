package com.rnp.cremer.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.rnp.cremer.dto.OrderHorizontalExportDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Servicio de exportación profesional de órdenes a PDF VERTICAL.
 * Implementa diseño modular adaptado a formato A4 portrait.
 *
 * @author RNP Team
 * @version 2.0 (Vertical)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderPdfExportService {

    private final ResourceLoader resourceLoader;
    private final OrderBulkExportService orderBulkExportService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    // === PALETA DE COLORES PROFESIONAL ===
    private static final Color PRIMARY_BLUE = new Color(37, 99, 235);
    private static final Color HEADER_BG = new Color(241, 245, 249);
    private static final Color SECTION_BG = new Color(226, 232, 240);
    private static final Color ACCENT_GREEN = new Color(34, 197, 94);
    private static final Color ACCENT_AMBER = new Color(245, 158, 11);
    private static final Color TEXT_PRIMARY = new Color(15, 23, 42);
    private static final Color TEXT_SECONDARY = new Color(100, 116, 139);
    private static final Color WHITE = Color.WHITE;

    // === FUENTES OPTIMIZADAS ===
    private static final Font FONT_TITLE = new Font(Font.HELVETICA, 18, Font.BOLD, PRIMARY_BLUE);
    private static final Font FONT_SUBTITLE = new Font(Font.HELVETICA, 9, Font.NORMAL, TEXT_SECONDARY);
    private static final Font FONT_SECTION = new Font(Font.HELVETICA, 10, Font.BOLD, WHITE);
    private static final Font FONT_HEADER = new Font(Font.HELVETICA, 8, Font.BOLD, TEXT_PRIMARY);
    private static final Font FONT_DATA = new Font(Font.HELVETICA, 8, Font.NORMAL, TEXT_PRIMARY);
    private static final Font FONT_DATA_BOLD = new Font(Font.HELVETICA, 9, Font.BOLD, TEXT_PRIMARY);
    private static final Font FONT_FOOTER = new Font(Font.HELVETICA, 8, Font.ITALIC, TEXT_SECONDARY);
    private static final Font FONT_LABEL = new Font(Font.HELVETICA, 7, Font.BOLD, TEXT_SECONDARY);

    /**
     * Exporta múltiples órdenes en formato VERTICAL a PDF profesional.
     * Cada orden ocupa una página completa.
     */
    @Transactional(readOnly = true)
    public byte[] exportMultipleOrdersToPdf(List<Long> orderIds) throws IOException {
        log.info("🚀 Generando PDF VERTICAL con {} órdenes", orderIds.size());

        List<OrderHorizontalExportDto> ordersData = collectOrdersData(orderIds);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // ✅ SIN .rotate() = VERTICAL (Portrait)
            Document document = new Document(PageSize.A4);
            document.setMargins(30, 30, 60, 50);

            PdfWriter writer = PdfWriter.getInstance(document, baos);

            HeaderFooterPageEvent event = new HeaderFooterPageEvent(resourceLoader, ordersData.size());
            writer.setPageEvent(event);

            document.open();

            // Construir contenido - UNA PÁGINA POR ORDEN
            for (int i = 0; i < ordersData.size(); i++) {
                addOrderPage(document, ordersData.get(i), i + 1, ordersData.size());

                // Nueva página si no es la última orden
                if (i < ordersData.size() - 1) {
                    document.newPage();
                }
            }

            document.close();
            log.info("✅ PDF VERTICAL generado con {} órdenes ({} páginas)", ordersData.size(), ordersData.size());
            return baos.toByteArray();

        } catch (DocumentException e) {
            throw new IOException("Error al construir PDF: " + e.getMessage(), e);
        }
    }

    private List<OrderHorizontalExportDto> collectOrdersData(List<Long> orderIds) {
        List<OrderHorizontalExportDto> ordersData = new java.util.ArrayList<>();
        for (Long id : orderIds) {
            try {
                ordersData.add(orderBulkExportService.getOrderHorizontalData(id));
            } catch (Exception e) {
                log.error("❌ Error al obtener orden {}: {}", id, e.getMessage());
            }
        }
        return ordersData;
    }

    /**
     * Añade una página completa para una orden.
     */
    private void addOrderPage(Document document, OrderHorizontalExportDto order, int pageNum, int totalPages)
            throws DocumentException {

        // Espaciado para header
        document.add(new Paragraph("\n", FONT_DATA));

        // 1. IDENTIFICACIÓN
        addSectionIdentificacion(document, order);
        document.add(Chunk.NEWLINE);

        // 2. ESPECIFICACIONES
        addSectionEspecificaciones(document, order);
        document.add(Chunk.NEWLINE);

        // 3. TIEMPOS Y PRODUCCIÓN
        addSectionTiemposProduccion(document, order);
        document.add(Chunk.NEWLINE);

        // 4. MÉTRICAS OEE (Destacado)
        addSectionOEE(document, order);
        document.add(Chunk.NEWLINE);

        // 5. ANÁLISIS
        addSectionAnalisis(document, order);
        document.add(Chunk.NEWLINE);

        // 6. INFO ADICIONAL
        addSectionInfoAdicional(document, order);
    }

    // === SECCIONES DEL DOCUMENTO ===

    private void addSectionIdentificacion(Document document, OrderHorizontalExportDto order)
            throws DocumentException {

        // Header de sección
        PdfPTable headerTable = new PdfPTable(1);
        headerTable.setWidthPercentage(100);

        PdfPCell headerCell = new PdfPCell(new Phrase("IDENTIFICACIÓN", FONT_SECTION));
        headerCell.setBackgroundColor(PRIMARY_BLUE);
        headerCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        headerCell.setPadding(8f);
        headerCell.setBorder(Rectangle.NO_BORDER);
        headerTable.addCell(headerCell);
        document.add(headerTable);

        // Contenido en 2 columnas
        float[] widths = {1f, 1f};
        PdfPTable contentTable = new PdfPTable(widths);
        contentTable.setWidthPercentage(100);
        contentTable.setSpacingBefore(5f);

        addDataField(contentTable, "Código Orden", order.getCodOrder(), true);
        addDataField(contentTable, "Lote", order.getLote());
        addDataField(contentTable, "Artículo", order.getArticulo());
        addDataField(contentTable, "Tipo", order.getTipo());

        // Descripción ocupa 2 columnas
        PdfPCell descLabel = createLabelCell("Descripción");
        descLabel.setColspan(2);
        contentTable.addCell(descLabel);

        PdfPCell descValue = createValueCell(order.getDescripcion());
        descValue.setColspan(2);
        contentTable.addCell(descValue);

        document.add(contentTable);
    }

    private void addSectionEspecificaciones(Document document, OrderHorizontalExportDto order)
            throws DocumentException {

        PdfPTable headerTable = new PdfPTable(1);
        headerTable.setWidthPercentage(100);

        PdfPCell headerCell = new PdfPCell(new Phrase("ESPECIFICACIONES", FONT_SECTION));
        headerCell.setBackgroundColor(new Color(99, 102, 241)); // Indigo
        headerCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        headerCell.setPadding(8f);
        headerCell.setBorder(Rectangle.NO_BORDER);
        headerTable.addCell(headerCell);
        document.add(headerTable);

        float[] widths = {1f, 1f};
        PdfPTable contentTable = new PdfPTable(widths);
        contentTable.setWidthPercentage(100);
        contentTable.setSpacingBefore(5f);

        addDataField(contentTable, "STD Referencia", order.getStdReferencia());
        addDataField(contentTable, "Cantidad Total", order.getCantidad());
        addDataField(contentTable, "Botes por Caja", order.getBotesCaja());
        addDataField(contentTable, "Cajas Previstas", order.getCajasPrevistas());
        addDataField(contentTable, "Formato Bote", order.getFormatoBote());
        addDataField(contentTable, "Unidades/Bote", order.getUdsBote());
        addDataField(contentTable, "Repercap", order.getRepercap() != null && order.getRepercap() ? "Sí" : "No");

        document.add(contentTable);
    }

    private void addSectionTiemposProduccion(Document document, OrderHorizontalExportDto order)
            throws DocumentException {

        // Header TIEMPOS
        PdfPTable headerTable = new PdfPTable(1);
        headerTable.setWidthPercentage(100);

        PdfPCell headerCell = new PdfPCell(new Phrase("TIEMPOS Y PRODUCCIÓN", FONT_SECTION));
        headerCell.setBackgroundColor(new Color(168, 85, 247)); // Purple
        headerCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        headerCell.setPadding(8f);
        headerCell.setBorder(Rectangle.NO_BORDER);
        headerTable.addCell(headerCell);
        document.add(headerTable);

        float[] widths = {1f, 1f};
        PdfPTable contentTable = new PdfPTable(widths);
        contentTable.setWidthPercentage(100);
        contentTable.setSpacingBefore(5f);

        // Tiempos
        addDataField(contentTable, "Tiempo Activo", formatMinutes(order.getTiempoActivo()));
        addDataField(contentTable, "Tiempo Pausado", formatMinutes(order.getTiempoPausado()));
        addDataField(contentTable, "Tiempo Total", formatMinutes(order.getTiempoTotal()));

        // Producción
        addDataField(contentTable, "Botes Buenos", order.getBotesBuenos());
        addDataField(contentTable, "Botes Malos", order.getBotesMalos());
        addDataField(contentTable, "Total Cajas", order.getTotalCajas());

        // Fechas
        addDataField(contentTable, "Hora Inicio",
                order.getHoraInicio() != null ? order.getHoraInicio().format(DATE_FORMATTER) : "—");
        addDataField(contentTable, "Hora Fin",
                order.getHoraFin() != null ? order.getHoraFin().format(DATE_FORMATTER) : "—");

        document.add(contentTable);
    }

    private void addSectionOEE(Document document, OrderHorizontalExportDto order)
            throws DocumentException {

        // Header destacado
        PdfPTable headerTable = new PdfPTable(1);
        headerTable.setWidthPercentage(100);

        PdfPCell headerCell = new PdfPCell(new Phrase("MÉTRICAS OEE", FONT_SECTION));
        headerCell.setBackgroundColor(new Color(14, 165, 233)); // Sky
        headerCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        headerCell.setPadding(8f);
        headerCell.setBorder(Rectangle.NO_BORDER);
        headerTable.addCell(headerCell);
        document.add(headerTable);

        // Tabla 2x2 para métricas OEE
        float[] widths = {1f, 1f};
        PdfPTable contentTable = new PdfPTable(widths);
        contentTable.setWidthPercentage(100);
        contentTable.setSpacingBefore(5f);

        DecimalFormat df = new DecimalFormat("0.00");

        addMetricField(contentTable, "Disponibilidad", order.getDisponibilidad(), df);
        addMetricField(contentTable, "Rendimiento", order.getRendimiento(), df);
        addMetricField(contentTable, "Calidad", order.getCalidad(), df);
        addMetricField(contentTable, "OEE", order.getOee(), df);

        document.add(contentTable);
    }

    private void addSectionAnalisis(Document document, OrderHorizontalExportDto order)
            throws DocumentException {

        PdfPTable headerTable = new PdfPTable(1);
        headerTable.setWidthPercentage(100);

        PdfPCell headerCell = new PdfPCell(new Phrase("ANÁLISIS", FONT_SECTION));
        headerCell.setBackgroundColor(ACCENT_AMBER);
        headerCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        headerCell.setPadding(8f);
        headerCell.setBorder(Rectangle.NO_BORDER);
        headerTable.addCell(headerCell);
        document.add(headerTable);

        float[] widths = {1f, 1f};
        PdfPTable contentTable = new PdfPTable(widths);
        contentTable.setWidthPercentage(100);
        contentTable.setSpacingBefore(5f);

        DecimalFormat dfP = new DecimalFormat("0.00");

        addDataField(contentTable, "% Pausas",
                order.getPorcentajePausas() != null ? dfP.format(order.getPorcentajePausas() * 100) + "%" : "—");
        addDataField(contentTable, "STD Real", order.getStdReal());
        addDataField(contentTable, "STD Real vs Ref", order.getStdRealVsStdRef());
        addDataField(contentTable, "% Cumplimiento Pedido",
                order.getPorCumpPedido() != null ? dfP.format(order.getPorCumpPedido() * 100) + "%" : "—");

        document.add(contentTable);
    }

    private void addSectionInfoAdicional(Document document, OrderHorizontalExportDto order)
            throws DocumentException {

        PdfPTable headerTable = new PdfPTable(1);
        headerTable.setWidthPercentage(100);

        PdfPCell headerCell = new PdfPCell(new Phrase("INFORMACIÓN ADICIONAL", FONT_SECTION));
        headerCell.setBackgroundColor(new Color(236, 72, 153)); // Pink
        headerCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        headerCell.setPadding(8f);
        headerCell.setBorder(Rectangle.NO_BORDER);
        headerTable.addCell(headerCell);
        document.add(headerTable);

        float[] widths = {1f, 1f};
        PdfPTable contentTable = new PdfPTable(widths);
        contentTable.setWidthPercentage(100);
        contentTable.setSpacingBefore(5f);

        addDataField(contentTable, "Formato Bote", order.getFormatoBote());
        addDataField(contentTable, "Tipo Producto", order.getTipo());
        addDataField(contentTable, "Unidades por Bote", order.getUdsBote());

        document.add(contentTable);
    }

    // === HELPERS ===

    private void addDataField(PdfPTable table, String label, Object value) {
        addDataField(table, label, value, false);
    }

    private void addDataField(PdfPTable table, String label, Object value, boolean highlight) {
        table.addCell(createLabelCell(label));

        String text = (value != null) ? String.valueOf(value) : "—";
        Font font = highlight ? FONT_DATA_BOLD : FONT_DATA;

        PdfPCell valueCell = new PdfPCell(new Phrase(text, font));
        valueCell.setBackgroundColor(WHITE);
        valueCell.setPadding(6f);
        valueCell.setBorderWidth(0.5f);
        valueCell.setBorderColor(SECTION_BG);
        table.addCell(valueCell);
    }

    private void addMetricField(PdfPTable table, String label, Float value, DecimalFormat df) {
        table.addCell(createLabelCell(label));

        if (value == null) {
            table.addCell(createValueCell("—"));
            return;
        }

        String text = df.format(value);

        // Color según rendimiento
        Color textColor = TEXT_PRIMARY;
        Color bgColor = WHITE;

        if (value >= 0.85f) {
            textColor = ACCENT_GREEN;
            bgColor = new Color(240, 253, 244); // Green-50
        } else if (value >= 0.70f) {
            textColor = ACCENT_AMBER;
            bgColor = new Color(254, 252, 232); // Yellow-50
        } else if (value < 0.70f) {
            textColor = new Color(239, 68, 68); // Red-500
            bgColor = new Color(254, 242, 242); // Red-50
        }

        Font metricFont = new Font(Font.HELVETICA, 11, Font.BOLD, textColor);
        PdfPCell valueCell = new PdfPCell(new Phrase(text, metricFont));
        valueCell.setBackgroundColor(bgColor);
        valueCell.setPadding(8f);
        valueCell.setBorderWidth(0.5f);
        valueCell.setBorderColor(SECTION_BG);
        valueCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(valueCell);
    }

    private PdfPCell createLabelCell(String label) {
        PdfPCell cell = new PdfPCell(new Phrase(label, FONT_LABEL));
        cell.setBackgroundColor(HEADER_BG);
        cell.setPadding(6f);
        cell.setBorderWidth(0.5f);
        cell.setBorderColor(SECTION_BG);
        return cell;
    }

    private PdfPCell createValueCell(String value) {
        PdfPCell cell = new PdfPCell(new Phrase(value != null ? value : "—", FONT_DATA));
        cell.setBackgroundColor(WHITE);
        cell.setPadding(6f);
        cell.setBorderWidth(0.5f);
        cell.setBorderColor(SECTION_BG);
        return cell;
    }

    private String formatMinutes(Float minutes) {
        if (minutes == null || minutes == 0) return "—";

        int totalMinutes = Math.round(minutes);
        if (totalMinutes < 60) {
            return totalMinutes + "m";
        }

        int hours = totalMinutes / 60;
        int mins = totalMinutes % 60;
        return String.format("%dh %dm", hours, mins);
    }

    // === HEADER Y FOOTER ===

    static class HeaderFooterPageEvent extends PdfPageEventHelper {
        private final ResourceLoader resourceLoader;
        private final int totalOrders;
        private Image logo;

        public HeaderFooterPageEvent(ResourceLoader resourceLoader, int totalOrders) {
            this.resourceLoader = resourceLoader;
            this.totalOrders = totalOrders;
            loadLogo();
        }

        private void loadLogo() {
            try {
                // ✅ El logo se busca AQUÍ en el BACKEND
                String imagePath = "classpath:images/logo.png";
                logo = Image.getInstance(resourceLoader.getResource(imagePath).getURL());
                logo.scaleToFit(100, 50); // Escalado para vertical
            } catch (Exception e) {
                log.warn("⚠️ Logo no encontrado: {}", e.getMessage());
                logo = null; // Si no hay logo, se muestra texto
            }
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();

            // === HEADER ===
            try {
                float pageWidth = document.getPageSize().getWidth();
                float topMargin = document.getPageSize().getTop() - 35;

                // Logo (izquierda)
                if (logo != null) {
                    logo.setAbsolutePosition(30, topMargin - 15);
                    cb.addImage(logo);
                }

                // Título (derecha del logo o centrado si no hay logo)
                float titleX = logo != null ? 150 : pageWidth / 2;
                int alignment = logo != null ? Element.ALIGN_LEFT : Element.ALIGN_CENTER;

                ColumnText.showTextAligned(
                        cb,
                        alignment,
                        new Phrase("REPORTE DE ORDEN DE PRODUCCIÓN", FONT_TITLE),
                        titleX,
                        topMargin,
                        0
                );

                // Línea divisoria
                cb.setLineWidth(0.5f);
                cb.setColorStroke(new Color(203, 213, 225));
                cb.moveTo(30, topMargin - 25);
                cb.lineTo(pageWidth - 30, topMargin - 25);
                cb.stroke();

            } catch (Exception e) {
                log.error("Error en header", e);
            }

            // === FOOTER ===
            try {
                float bottomMargin = document.getPageSize().getBottom() + 25;
                float pageWidth = document.getPageSize().getWidth();

                // Línea superior
                cb.setLineWidth(0.5f);
                cb.setColorStroke(new Color(203, 213, 225));
                cb.moveTo(30, bottomMargin + 15);
                cb.lineTo(pageWidth - 30, bottomMargin + 15);
                cb.stroke();

                // Texto izquierda
                ColumnText.showTextAligned(
                        cb,
                        Element.ALIGN_LEFT,
                        new Phrase("Sistema RNP Cremer", FONT_FOOTER),
                        30,
                        bottomMargin,
                        0
                );

                // Paginación centro
                String pageText = String.format("Orden %d de %d",
                        writer.getPageNumber(), totalOrders);
                ColumnText.showTextAligned(
                        cb,
                        Element.ALIGN_CENTER,
                        new Phrase(pageText, FONT_FOOTER),
                        pageWidth / 2,
                        bottomMargin,
                        0
                );

                // Timestamp derecha
                ColumnText.showTextAligned(
                        cb,
                        Element.ALIGN_RIGHT,
                        new Phrase(java.time.LocalDateTime.now().format(TIME_FORMATTER), FONT_FOOTER),
                        pageWidth - 30,
                        bottomMargin,
                        0
                );

            } catch (Exception e) {
                log.error("Error en footer", e);
            }
        }
    }
}