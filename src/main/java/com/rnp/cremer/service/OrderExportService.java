package com.rnp.cremer.service;

import com.rnp.cremer.dto.OrderExportDto;
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
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para exportar datos de órdenes a Excel.
 *
 * @author RNP Team
 * @version 1.0
 * @since 2024-12-03
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderExportService {

    private final OrderRepository orderRepository;
    private final MetricasRepository metricasRepository;
    private final ExtraDataRepository extraDataRepository;
    private final PauseRepository pauseRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /**
     * Obtiene los datos completos de una orden para exportación.
     *
     * @param idOrder ID de la orden
     * @return DTO con todos los datos combinados
     */
    @Transactional(readOnly = true)
    public OrderExportDto getOrderExportData(Long idOrder) {
        log.info("Obteniendo datos de exportación para orden ID: {}", idOrder);

        // 1. Obtener Order
        Order order = orderRepository.findById(idOrder)
                .orElseThrow(() -> new IllegalArgumentException("Orden no encontrada con ID: " + idOrder));

        // 2. Obtener Metricas (puede ser null si no se han calculado)
        Metricas metricas = metricasRepository.findByIdOrder(idOrder).orElse(null);

        // 3. Obtener ExtraData (puede ser null)
        ExtraData extraData = extraDataRepository.findByIdOrder(idOrder).orElse(null);

        // 4. Obtener Pausas
        List<Pause> pausas = pauseRepository.findByIdOrderOrderByHoraInicioDesc(idOrder);

        // 5. Calcular porcentaje de pausas y stdRealVsStdRef
        Float porcentajePausas = null;
        Float stdRealVsStdRef = null;

        if (metricas != null) {
            if (metricas.getTiempoTotal() != null && metricas.getTiempoTotal() > 0) {
                porcentajePausas = metricas.getTiempoPausado() / metricas.getTiempoTotal();
            }

           if (metricas != null && metricas.getStdReal() != null
        && order.getStdReferencia() != null && order.getStdReferencia() > 0) {

    // Ratio: STD Real / STD Referencia (ej: 0.58 = 58%)
    stdRealVsStdRef = metricas.getStdReal() / order.getStdReferencia();
} else {
    stdRealVsStdRef = null;
}

        }

        // 6. Mapear pausas a DTO
        List<OrderExportDto.PauseExportDto> pausasDto = pausas.stream()
                .map(this::mapPauseToExportDto)
                .collect(Collectors.toList());

        // 7. Construir DTO completo
        return OrderExportDto.builder()
                // Datos básicos
                .idOrder(order.getIdOrder())
                .codOrder(order.getCodOrder())
                .lote(order.getLote())
                .articulo(order.getArticulo())
                .descripcion(order.getDescripcion())
                .cantidad(order.getCantidad())
                .botesCaja(order.getBotesCaja())
                .cajasPrevistas(order.getCajasPrevistas())
                .repercap(order.getRepercap())
                .horaInicio(order.getHoraInicio())
                .horaFin(order.getHoraFin())
                // Datos de producción
                .botesBuenos(order.getBotesBuenos())
                .botesMalos(order.getBotesMalos())
                .totalCajasCierre(order.getTotalCajasCierre())
                // Métricas de tiempo
                .tiempoActivo(metricas != null ? metricas.getTiempoActivo() : null)
                .tiempoPausado(metricas != null ? metricas.getTiempoPausado() : null)
                .tiempoTotal(metricas != null ? metricas.getTiempoTotal() : null)
                // Métricas de rendimiento
                .disponibilidad(metricas != null ? metricas.getDisponibilidad() : null)
                .rendimiento(metricas != null ? metricas.getRendimiento() : null)
                .calidad(metricas != null ? metricas.getCalidad() : null)
                .oee(metricas != null ? metricas.getOee() : null)
                .porcentajePausas(porcentajePausas)
                // Métricas de estándares
                .stdReal(metricas != null ? metricas.getStdReal() : null)
                .stdReferencia(order.getStdReferencia())
                .stdRealVsStdRef(stdRealVsStdRef)
                // Datos extra
                .formatoBote(extraData != null ? extraData.getFormatoBote() : null)
                .tipo(extraData != null ? extraData.getTipo() : null)
                .udsBote(extraData != null ? extraData.getUdsBote() : null)
                // Pausas
                .pausas(pausasDto)
                .build();
    }

    /**
     * Mapea una Pause a PauseExportDto.
     */
    private OrderExportDto.PauseExportDto mapPauseToExportDto(Pause pause) {
        return OrderExportDto.PauseExportDto.builder()
                .idPausa(pause.getIdPausa())
                .tipo(pause.getTipo() != null ? pause.getTipo().name() : null)
                .descripcion(pause.getDescripcion())
                .operario(pause.getOperario())
                .computa(pause.getComputa())
                .horaInicio(pause.getHoraInicio())
                .horaFin(pause.getHoraFin())
                .tiempoTotalPausa(pause.getTiempoTotalPausa())
                .build();
    }

    /**
     * Genera un archivo Excel con los datos de exportación de una orden.
     *
     * @param idOrder ID de la orden
     * @return Bytes del archivo Excel
     */
    public byte[] exportOrderToExcel(Long idOrder) throws IOException {
        log.info("Generando archivo Excel para orden ID: {}", idOrder);

        OrderExportDto data = getOrderExportData(idOrder);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // ========================================
            // HOJA 1: DATOS GENERALES DE LA ORDEN
            // ========================================
            Sheet sheetGeneral = workbook.createSheet("Datos Generales");
            createGeneralDataSheet(workbook, sheetGeneral, data);

            // ========================================
            // HOJA 2: PAUSAS
            // ========================================
            Sheet sheetPausas = workbook.createSheet("Pausas");
            createPausasSheet(workbook, sheetPausas, data);

            // Ajustar ancho de columnas
            for (int i = 0; i < 2; i++) {
                sheetGeneral.autoSizeColumn(i);
            }
            for (int i = 0; i < 8; i++) {
                sheetPausas.autoSizeColumn(i);
            }

            workbook.write(out);
            log.info("Archivo Excel generado exitosamente para orden {}", data.getCodOrder());
            return out.toByteArray();
        }
    }

    /**
     * Crea la hoja de datos generales de la orden.
     */
    private void createGeneralDataSheet(Workbook workbook, Sheet sheet, OrderExportDto data) {
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);

        int rowNum = 0;

        // Título
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("REPORTE DE ORDEN DE PRODUCCIÓN");
        CellStyle titleStyle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 16);
        titleStyle.setFont(titleFont);
        titleCell.setCellStyle(titleStyle);

        rowNum++; // Línea vacía

        // SECCIÓN 1: DATOS BÁSICOS
        createSectionHeader(sheet, rowNum++, "DATOS BÁSICOS", headerStyle);
        rowNum = addDataRow(sheet, rowNum, "Código Orden", data.getCodOrder(), dataStyle);
        rowNum = addDataRow(sheet, rowNum, "Lote", data.getLote(), dataStyle);
        rowNum = addDataRow(sheet, rowNum, "Artículo", data.getArticulo(), dataStyle);
        rowNum = addDataRow(sheet, rowNum, "Descripción", data.getDescripcion(), dataStyle);
        rowNum = addDataRow(sheet, rowNum, "Cantidad", data.getCantidad(), dataStyle);
        rowNum = addDataRow(sheet, rowNum, "Botes/Caja", data.getBotesCaja(), dataStyle);
        rowNum = addDataRow(sheet, rowNum, "Cajas Estimadas", formatDecimal(data.getCajasPrevistas()), dataStyle);
        rowNum = addDataRow(sheet, rowNum, "Repercap", data.getRepercap() != null && data.getRepercap() ? "Sí" : "No", dataStyle);
        rowNum = addDataRow(sheet, rowNum, "Formato Bote", data.getFormatoBote(), dataStyle);
        rowNum = addDataRow(sheet, rowNum, "Tipo", data.getTipo(), dataStyle);
        rowNum = addDataRow(sheet, rowNum, "Unidades/Bote", data.getUdsBote(), dataStyle);

        rowNum++; // Línea vacía

        // SECCIÓN 2: TIEMPOS
        createSectionHeader(sheet, rowNum++, "TIEMPOS", headerStyle);
        rowNum = addDataRow(sheet, rowNum, "Hora Inicio", formatDateTime(data.getHoraInicio()), dataStyle);
        rowNum = addDataRow(sheet, rowNum, "Hora Fin", formatDateTime(data.getHoraFin()), dataStyle);
        rowNum = addDataRow(sheet, rowNum, "Tiempo Activo (min)", formatDecimal(data.getTiempoActivo()), dataStyle);
        rowNum = addDataRow(sheet, rowNum, "Tiempo Pausado (min)", formatDecimal(data.getTiempoPausado()), dataStyle);
        rowNum = addDataRow(sheet, rowNum, "Tiempo Total (min)", formatDecimal(data.getTiempoTotal()), dataStyle);

        rowNum++; // Línea vacía

        // SECCIÓN 3: PRODUCCIÓN
        createSectionHeader(sheet, rowNum++, "PRODUCCIÓN", headerStyle);
        rowNum = addDataRow(sheet, rowNum, "Botes Buenos", data.getBotesBuenos(), dataStyle);
        rowNum = addDataRow(sheet, rowNum, "Botes Malos", data.getBotesMalos(), dataStyle);
        rowNum = addDataRow(sheet, rowNum, "Total Cajas Cierre", data.getTotalCajasCierre(), dataStyle);

        rowNum++; // Línea vacía

        // SECCIÓN 4: MÉTRICAS
        createSectionHeader(sheet, rowNum++, "MÉTRICAS DE RENDIMIENTO", headerStyle);
        rowNum = addDataRow(sheet, rowNum, "Disponibilidad", formatPercentage(data.getDisponibilidad()), dataStyle);
        rowNum = addDataRow(sheet, rowNum, "Rendimiento", formatPercentage(data.getRendimiento()), dataStyle);
        rowNum = addDataRow(sheet, rowNum, "Calidad", formatPercentage(data.getCalidad()), dataStyle);
        rowNum = addDataRow(sheet, rowNum, "OEE", formatPercentage(data.getOee()), dataStyle);
        rowNum = addDataRow(sheet, rowNum, "% Pausas", formatPercentage(data.getPorcentajePausas()), dataStyle);

        rowNum++; // Línea vacía

        // SECCIÓN 5: ESTÁNDARES
        createSectionHeader(sheet, rowNum++, "ESTÁNDARES", headerStyle);
       rowNum = addDataRow(sheet, rowNum, "STD Real (uds/min)", formatDecimal(data.getStdReal()), dataStyle);
rowNum = addDataRow(sheet, rowNum, "STD Referencia (uds/min)", formatDecimal(data.getStdReferencia()), dataStyle);

rowNum = addDataRow(sheet, rowNum, "STD Real vs STD Ref", formatPercentage(data.getStdRealVsStdRef()), dataStyle);
    }

    /**
     * Crea la hoja de pausas.
     */
    private void createPausasSheet(Workbook workbook, Sheet sheet, OrderExportDto data) {
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);

        int rowNum = 0;

        // Encabezados
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"ID Pausa", "Tipo", "Descripción", "Operario", "Computa", "Hora Inicio", "Hora Fin", "Tiempo (min)"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Datos de pausas
        for (OrderExportDto.PauseExportDto pause : data.getPausas()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(pause.getIdPausa());
            row.createCell(1).setCellValue(pause.getTipo());
            row.createCell(2).setCellValue(pause.getDescripcion());
            row.createCell(3).setCellValue(pause.getOperario());
            row.createCell(4).setCellValue(pause.getComputa() != null && pause.getComputa() ? "Sí" : "No");
            row.createCell(5).setCellValue(formatDateTime(pause.getHoraInicio()));
            row.createCell(6).setCellValue(formatDateTime(pause.getHoraFin()));
            row.createCell(7).setCellValue(formatDecimal(pause.getTiempoTotalPausa()));

            for (int i = 0; i < headers.length; i++) {
                row.getCell(i).setCellStyle(dataStyle);
            }
        }
    }

    /**
     * Crea un encabezado de sección.
     */
    private void createSectionHeader(Sheet sheet, int rowNum, String title, CellStyle headerStyle) {
        Row row = sheet.createRow(rowNum);
        Cell cell = row.createCell(0);
        cell.setCellValue(title);
        cell.setCellStyle(headerStyle);
    }

    /**
     * Añade una fila de datos clave-valor.
     */
    private int addDataRow(Sheet sheet, int rowNum, String label, Object value, CellStyle dataStyle) {
        Row row = sheet.createRow(rowNum);
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(dataStyle);

        Cell valueCell = row.createCell(1);
        if (value != null) {
            valueCell.setCellValue(value.toString());
        }
        valueCell.setCellStyle(dataStyle);

        return rowNum + 1;
    }

    /**
     * Estilo para encabezados.
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * Estilo para datos.
     */
    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * Formatea un decimal a 2 decimales.
     */
    private String formatDecimal(Float value) {
        return value != null ? String.format("%.2f", value) : "-";
    }

    /**
     * Formatea un porcentaje.
     */
    private String formatPercentage(Float value) {
        return value != null ? String.format("%.2f%%", value * 100) : "-";
    }

    /**
     * Formatea una fecha.
     */
    private String formatDateTime(java.time.LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_FORMATTER) : "-";
    }
}