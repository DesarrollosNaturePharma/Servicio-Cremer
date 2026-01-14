package com.rnp.cremer.service;

import com.rnp.cremer.dto.OrderHorizontalExportDto;
import com.rnp.cremer.model.*;
import com.rnp.cremer.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio para exportación horizontal de múltiples órdenes en un solo Excel.
 *
 * @author RNP Team
 * @version 1.0
 * @since 2024-12-03
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderBulkExportService {

    private final OrderRepository orderRepository;
    private final MetricasRepository metricasRepository;
    private final ExtraDataRepository extraDataRepository;
    private final PauseRepository pauseRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /**
     * Exporta múltiples órdenes en formato horizontal (una fila por orden).
     *
     * @param orderIds Lista de IDs de órdenes a exportar
     * @return Bytes del archivo Excel
     */
    @Transactional(readOnly = true)
    public byte[] exportMultipleOrdersToExcel(List<Long> orderIds) throws IOException {
        log.info("Generando archivo Excel con {} órdenes", orderIds.size());

        List<OrderHorizontalExportDto> ordersData = new ArrayList<>();

        for (Long idOrder : orderIds) {
            try {
                OrderHorizontalExportDto data = getOrderHorizontalData(idOrder);
                ordersData.add(data);
            } catch (Exception e) {
                log.error("Error al obtener datos de orden {}: {}", idOrder, e.getMessage());
                // Continuar con las demás órdenes
            }
        }

        return createHorizontalExcel(ordersData);
    }

    /**
     * Obtiene los datos de una orden en formato horizontal.
     */
    OrderHorizontalExportDto getOrderHorizontalData(Long idOrder) {
        // 1. Obtener Order
        Order order = orderRepository.findById(idOrder)
                .orElseThrow(() -> new IllegalArgumentException("Orden no encontrada con ID: " + idOrder));

        // 2. Obtener Metricas
        Metricas metricas = metricasRepository.findByIdOrder(idOrder).orElse(null);

        // 3. Obtener ExtraData
        ExtraData extraData = extraDataRepository.findByIdOrder(idOrder).orElse(null);

        // 4. Calcular campos adicionales
        Float porcentajePausas = null;
        Float stdRealVsStdRef = null;

        if (metricas != null) {
            if (metricas.getTiempoTotal() != null && metricas.getTiempoTotal() > 0) {
                porcentajePausas = metricas.getTiempoPausado() / metricas.getTiempoTotal();
            }
        }

        if (metricas != null && metricas.getStdReal() != null && order.getStdReferencia() != null) {
            stdRealVsStdRef = metricas.getStdReal() - order.getStdReferencia();
        }


        // 5. Construir DTO
        return OrderHorizontalExportDto.builder()
                // Datos inicio
                .codOrder(order.getCodOrder())
                .lote(order.getLote())
                .articulo(order.getArticulo())
                .descripcion(order.getDescripcion())
                .stdReferencia(order.getStdReferencia()) // <<-- AÑADIDO
                .cantidad(order.getCantidad())
                .botesCaja(order.getBotesCaja())
                .cajasPrevistas(order.getCajasPrevistas())
                .repercap(order.getRepercap())
                // Tiempos
                .tiempoActivo(metricas != null ? metricas.getTiempoActivo() : null)
                .tiempoPausado(metricas != null ? metricas.getTiempoPausado() : null)
                .tiempoTotal(metricas != null ? metricas.getTiempoTotal() : null)
                // Producción
                .botesBuenos(order.getBotesBuenos())
                .botesMalos(order.getBotesMalos())
                .totalCajas(order.getTotalCajasCierre())
                // OEE
                .disponibilidad(metricas != null ? metricas.getDisponibilidad() : null)
                .rendimiento(metricas != null ? metricas.getRendimiento() : null)
                .calidad(metricas != null ? metricas.getCalidad() : null)
                .oee(metricas != null ? metricas.getOee() : null)
                // Cálculos
                .porcentajePausas(porcentajePausas)
                .stdReal(metricas != null ? metricas.getStdReal() : null)
                .stdRealVsStdRef(stdRealVsStdRef)
                .porCumpPedido(metricas != null ? metricas.getPorCumpPedido() : null)
                // Extra data
                .formatoBote(extraData != null ? extraData.getFormatoBote() : null)
                .tipo(extraData != null ? extraData.getTipo() : null)
                .udsBote(extraData != null ? extraData.getUdsBote() : null)
                // Fechas
                .horaInicio(order.getHoraInicio())
                .horaFin(order.getHoraFin())
                .build();
    }

    /**
     * Crea el archivo Excel con formato horizontal.
     */
    private byte[] createHorizontalExcel(List<OrderHorizontalExportDto> orders) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // ========================================
            // HOJA 1: ÓRDENES (FORMATO HORIZONTAL)
            // ========================================
            Sheet sheetOrdenes = workbook.createSheet("Órdenes");

            // Estilos
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook); // Para campos numéricos/texto sin formato especial
            CellStyle simpleDataStyle = createSimpleDataStyle(workbook); // <<-- Nuevo para OEE
            CellStyle percentageStyle = createPercentageStyle(workbook); // Para campos que SÍ son porcentajes (0.00%)
            CellStyle dateStyle = createDateStyle(workbook);

            int rowNum = 0;



            // FILA 2: ENCABEZADOS DE COLUMNAS
            Row headerRow = sheetOrdenes.createRow(rowNum++);
            createColumnHeaders(headerRow, headerStyle);

            // FILAS DE DATOS
            for (OrderHorizontalExportDto order : orders) {
                Row dataRow = sheetOrdenes.createRow(rowNum++);
                fillDataRow(dataRow, order, simpleDataStyle, percentageStyle, dateStyle);
            }

            // Auto-ajustar columnas
            for (int i = 0; i < 30; i++) {
                sheetOrdenes.autoSizeColumn(i);
            }

            // ========================================
            // HOJA 2: PAUSAS DE TODAS LAS ÓRDENES
            // ========================================
            Sheet sheetPausas = workbook.createSheet("Pausas");
            createPausasSheet(workbook, sheetPausas, orders);

            workbook.write(out);
            log.info("Archivo Excel horizontal generado con {} órdenes y sus pausas", orders.size());
            return out.toByteArray();
        }
    }

    /**
     * Crea la hoja de pausas con todas las pausas de todas las órdenes.
     */
    private void createPausasSheet(Workbook workbook, Sheet sheet, List<OrderHorizontalExportDto> orders) {
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        CellStyle dateStyle = createDateStyle(workbook);

        int rowNum = 0;

        // ========================================
        // ENCABEZADOS
        // ========================================
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {
                "Código Orden",
                "ID Pausa",
                "Tipo",
                "Descripción",
                "Operario",
                "Computa",
                "Hora Inicio",
                "Hora Fin",
                "Tiempo (min)"
        };

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // ========================================
        // DATOS DE PAUSAS
        // ========================================
        // Recopilar todas las pausas de todas las órdenes
        for (OrderHorizontalExportDto orderDto : orders) {
            // Obtener el ID de la orden
            Order order = orderRepository.findByCodOrder(orderDto.getCodOrder()).orElse(null);

            if (order == null) continue;

            // Obtener todas las pausas de esta orden
            List<Pause> pausas = pauseRepository.findByIdOrderOrderByHoraInicioDesc(order.getIdOrder());

            // Añadir cada pausa como una fila
            for (Pause pause : pausas) {
                Row row = sheet.createRow(rowNum++);
                int col = 0;

                // Código Orden
                Cell cellCodOrder = row.createCell(col++);
                cellCodOrder.setCellValue(orderDto.getCodOrder());
                cellCodOrder.setCellStyle(dataStyle);

                // ID Pausa
                Cell cellIdPausa = row.createCell(col++);
                if (pause.getIdPausa() != null) {
                    cellIdPausa.setCellValue(pause.getIdPausa());
                }
                cellIdPausa.setCellStyle(dataStyle);

                // Tipo
                Cell cellTipo = row.createCell(col++);
                if (pause.getTipo() != null) {
                    cellTipo.setCellValue(pause.getTipo().name());
                }
                cellTipo.setCellStyle(dataStyle);

                // Descripción
                Cell cellDesc = row.createCell(col++);
                if (pause.getDescripcion() != null) {
                    cellDesc.setCellValue(pause.getDescripcion());
                }
                cellDesc.setCellStyle(dataStyle);

                // Operario
                Cell cellOp = row.createCell(col++);
                if (pause.getOperario() != null) {
                    cellOp.setCellValue(pause.getOperario());
                }
                cellOp.setCellStyle(dataStyle);

                // Computa
                Cell cellComputa = row.createCell(col++);
                if (pause.getComputa() != null) {
                    cellComputa.setCellValue(pause.getComputa() ? "Sí" : "No");
                }
                cellComputa.setCellStyle(dataStyle);

                // Hora Inicio
                Cell cellInicio = row.createCell(col++);
                if (pause.getHoraInicio() != null) {
                    cellInicio.setCellValue(pause.getHoraInicio().format(DATE_FORMATTER));
                }
                cellInicio.setCellStyle(dateStyle);

                // Hora Fin
                Cell cellFin = row.createCell(col++);
                if (pause.getHoraFin() != null) {
                    cellFin.setCellValue(pause.getHoraFin().format(DATE_FORMATTER));
                }
                cellFin.setCellStyle(dateStyle);

                // Tiempo Total Pausa (minutos)
                Cell cellTiempo = row.createCell(col++);
                if (pause.getTiempoTotalPausa() != null) {
                    cellTiempo.setCellValue(pause.getTiempoTotalPausa());
                }
                cellTiempo.setCellStyle(dataStyle);
            }
        }

        // Auto-ajustar columnas
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // Si no hay pausas, añadir mensaje
        if (rowNum == 1) {
            Row emptyRow = sheet.createRow(1);
            Cell cell = emptyRow.createCell(0);
            cell.setCellValue("No hay pausas registradas para las órdenes seleccionadas");
            CellStyle italicStyle = workbook.createCellStyle();
            Font italicFont = workbook.createFont();
            italicFont.setItalic(true);
            italicStyle.setFont(italicFont);
            cell.setCellStyle(italicStyle);
        }
    }

    /**
     * Crea los encabezados de secciones (DATOS INICIO, TIEMPOS EJECUCIÓN, etc.)
     */

    private void createMergedSectionHeader(Row row, int startCol, int endCol, String value, CellStyle style) {
        for (int i = startCol; i <= endCol; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(value);
            cell.setCellStyle(style);
        }
    }

    /**
     * Crea los encabezados de columnas.
     */
    private void createColumnHeaders(Row row, CellStyle style) {
        int col = 0;

        // DATOS INICIO
        createHeaderCell(row, col++, "Código Orden", style);
        createHeaderCell(row, col++, "Lote", style);
        createHeaderCell(row, col++, "Artículo", style);
        createHeaderCell(row, col++, "Descripción", style);
        createHeaderCell(row, col++, "STD Referencia", style); // <<-- AÑADIDO
        createHeaderCell(row, col++, "Cantidad", style);
        createHeaderCell(row, col++, "Botes/Caja", style);
        createHeaderCell(row, col++, "Cajas TH", style);
        createHeaderCell(row, col++, "Repercap", style);

        // TIEMPOS EJECUCIÓN
        createHeaderCell(row, col++, "Tiempo Activo (min)", style);
        createHeaderCell(row, col++, "Tiempo Pausado (min)", style);
        createHeaderCell(row, col++, "Tiempo Total (min)", style);

        // PRODUCCIÓN
        createHeaderCell(row, col++, "Botes Buenos", style);
        createHeaderCell(row, col++, "Botes Malos", style);
        createHeaderCell(row, col++, "Total Cajas", style);

        // OEE
        createHeaderCell(row, col++, "Disponibilidad", style);
        createHeaderCell(row, col++, "Rendimiento", style);
        createHeaderCell(row, col++, "Calidad", style);
        createHeaderCell(row, col++, "OEE", style);

        // CÁLCULOS
        createHeaderCell(row, col++, "% Pausas", style);
        createHeaderCell(row, col++, "STD Real", style); // <<-- RENOMBRADO
        createHeaderCell(row, col++, "STD Real Vs STD Ref", style); // <<-- RENOMBRADO
        createHeaderCell(row, col++, "% Cump Pedido", style);

        // INFO MANUAL
        createHeaderCell(row, col++, "formato", style);
        createHeaderCell(row, col++, "tipo", style);
        createHeaderCell(row, col++, "udsBote", style);

        // FECHAS
        createHeaderCell(row, col++, "Hora Inicio", style);
        createHeaderCell(row, col++, "Hora Fin", style);
    }

    private void createHeaderCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    /**
     * Rellena una fila con los datos de una orden.
     */
    public void fillDataRow(Row row, OrderHorizontalExportDto order, CellStyle dataStyle, CellStyle percentageStyle, CellStyle dateStyle) {
        int col = 0;

        // DATOS INICIO
        setCellValue(row, col++, order.getCodOrder(), dataStyle);
        setCellValue(row, col++, order.getLote(), dataStyle);
        setCellValue(row, col++, order.getArticulo(), dataStyle);
        setCellValue(row, col++, order.getDescripcion(), dataStyle);
        setCellValue(row, col++, order.getStdReferencia(), dataStyle); // <<-- AÑADIDO
        setCellValue(row, col++, order.getCantidad(), dataStyle);
        setCellValue(row, col++, order.getBotesCaja(), dataStyle);
        setCellValue(row, col++, order.getCajasPrevistas(), dataStyle);
        setCellValue(row, col++, order.getRepercap() != null && order.getRepercap() ? "Sí" : "No", dataStyle);

        // TIEMPOS EJECUCIÓN
        setCellValue(row, col++, order.getTiempoActivo(), dataStyle);
        setCellValue(row, col++, order.getTiempoPausado(), dataStyle);
        setCellValue(row, col++, order.getTiempoTotal(), dataStyle);

        // PRODUCCIÓN
        setCellValue(row, col++, order.getBotesBuenos(), dataStyle);
        setCellValue(row, col++, order.getBotesMalos(), dataStyle);
        setCellValue(row, col++, order.getTotalCajas(), dataStyle);

        // OEE (SIN porcentajes, como decimal 0.00)
        setCellValue(row, col++, order.getDisponibilidad(), dataStyle);
        setCellValue(row, col++, order.getRendimiento(), dataStyle);
        setCellValue(row, col++, order.getCalidad(), dataStyle);
        setCellValue(row, col++, order.getOee(), dataStyle);

        // CÁLCULOS
        setCellPercentage(row, col++, order.getPorcentajePausas(), percentageStyle);
        setCellValue(row, col++, order.getStdReal(), dataStyle);
        setCellValue(row, col++, order.getStdRealVsStdRef(), dataStyle);
        setCellPercentage(row, col++, order.getPorCumpPedido(), percentageStyle);

        // INFO MANUAL
        setCellValue(row, col++, order.getFormatoBote(), dataStyle);
        setCellValue(row, col++, order.getTipo(), dataStyle);
        setCellValue(row, col++, order.getUdsBote(), dataStyle);

        // FECHAS
        setCellDate(row, col++, order.getHoraInicio(), dateStyle);
        setCellDate(row, col++, order.getHoraFin(), dateStyle);
    }

    // Métodos auxiliares para establecer valores
    private void setCellValue(Row row, int col, Object value, CellStyle style) {
        Cell cell = row.createCell(col);
        if (value != null) {
            if (value instanceof String) {
                cell.setCellValue((String) value);
            } else if (value instanceof Integer) {
                cell.setCellValue((Integer) value);
            } else if (value instanceof Float) {
                cell.setCellValue((Float) value);
            } else if (value instanceof Double) {
                cell.setCellValue((Double) value);
            }
        }
        cell.setCellStyle(style);
    }

    private void setCellPercentage(Row row, int col, Float value, CellStyle style) {
        Cell cell = row.createCell(col);
        if (value != null) {
            cell.setCellValue(value); // Ya viene como decimal (0.85 = 85%)
        }
        cell.setCellStyle(style);
    }

    private void setCellDate(Row row, int col, java.time.LocalDateTime date, CellStyle style) {
        Cell cell = row.createCell(col);
        if (date != null) {
            cell.setCellValue(date.format(DATE_FORMATTER));
        }
        cell.setCellStyle(style);
    }

    // Estilos
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setWrapText(true);
        return style;
    }

    // Estilo para datos genéricos (enteros, texto)
    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    // Estilo para datos numéricos con 2 decimales (OEE, STD, etc.)
    private CellStyle createSimpleDataStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        DataFormat format = workbook.createDataFormat();
        // Formato para mostrar 2 decimales sin el símbolo de porcentaje
        style.setDataFormat(format.getFormat("0.00"));
        return style;
    }

    // Estilo para datos que deben ser porcentajes (0.00%)
    private CellStyle createPercentageStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("0.00%"));
        return style;
    }

    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        style.setAlignment(HorizontalAlignment.LEFT);
        return style;
    }
}