package com.rnp.cremer.controller;

import com.rnp.cremer.dto.*;
import com.rnp.cremer.model.EstadoOrder;
import com.rnp.cremer.service.OrderBulkExportService;
import com.rnp.cremer.service.OrderExportService;
import com.rnp.cremer.service.OrderPdfExportService;
import com.rnp.cremer.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para gestión de órdenes de producción.
 *
 * <p>Proporciona endpoints para CRUD completo y gestión del ciclo de vida de órdenes.</p>
 *
 * <h3>Flujo de Estados:</h3>
 * <pre>
 * CREADA → EN_PROCESO ↔ PAUSADA
 *              ↓
 *    finalizar(acumula=false) → FINALIZADA
 *    finalizar(acumula=true)  → ESPERA_MANUAL → PROCESO_MANUAL → FINALIZADA
 * </pre>
 *
 * <h3>Cálculo de Métricas:</h3>
 * <p>Las métricas (OEE, disponibilidad, rendimiento, calidad) se calculan UNA SOLA VEZ
 * cuando la orden sale de EN_PROCESO, independientemente de si acumula o no.
 * NO se recalculan durante el proceso manual.</p>
 *
 * @author RNP Team
 * @version 1.1
 * @since 2024-11-25
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Orders", description = "API de Gestión de Órdenes de Producción")
public class OrderController {

    private final OrderService orderService;
    private final OrderExportService orderExportService;
    private final OrderBulkExportService orderBulkExportService;
    private final OrderPdfExportService orderPdfExportService;
    // ========================================
    // CREAR ORDEN
    // ========================================

    /**
     * Crea una nueva orden de producción.
     *
     * @param dto datos de la orden a crear
     * @return orden creada con estado CREADA
     */
    @PostMapping
    @Operation(
            summary = "Crear orden",
            description = "Crea una nueva orden de producción en estado CREADA"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Orden creada exitosamente"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Datos inválidos"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "La orden ya existe"
            )
    })
    public ResponseEntity<ApiResponse<OrderResponseDto>> createOrder(
            @Valid @RequestBody OrderCreateDto dto) {

        log.info("POST /api/v1/orders - Crear orden: {}", dto.getCodOrder());
        OrderResponseDto createdOrder = orderService.createOrder(dto);
        ApiResponse<OrderResponseDto> response = ApiResponse.success(
                "Orden creada exitosamente",
                createdOrder
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ========================================
    // LISTAR ÓRDENES
    // ========================================

    /**
     * Lista todas las órdenes con filtros opcionales.
     *
     * @param estado    filtro por estado de la orden
     * @param operario  filtro por nombre del operario
     * @param lote      filtro por número de lote
     * @param articulo  filtro por código del artículo
     * @param page      número de página (base 0)
     * @param size      tamaño de página
     * @param sort      campo para ordenar
     * @param direction dirección de ordenamiento
     * @return página de órdenes que cumplen los criterios
     */
    @GetMapping
    @Operation(
            summary = "Listar órdenes",
            description = "Obtiene todas las órdenes con filtros opcionales y paginación"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Órdenes obtenidas exitosamente"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Parámetros inválidos"
            )
    })
    public ResponseEntity<ApiResponse<Page<OrderResponseDto>>> getAllOrders(
            @Parameter(description = "Estado de la orden")
            @RequestParam(required = false) EstadoOrder estado,

            @Parameter(description = "Nombre del operario")
            @RequestParam(required = false) String operario,

            @Parameter(description = "Número de lote")
            @RequestParam(required = false) String lote,

            @Parameter(description = "Código del artículo")
            @RequestParam(required = false) String articulo,

            @Parameter(description = "Número de página (base 0)")
            @RequestParam(defaultValue = "0") @Min(0) int page,

            @Parameter(description = "Tamaño de página (1-100)")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,

            @Parameter(description = "Campo para ordenar")
            @RequestParam(defaultValue = "horaCreacion") String sort,

            @Parameter(description = "Dirección de orden (ASC/DESC)")
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {

        log.info("GET /api/v1/orders - Listar órdenes (page: {}, size: {}, estado: {})",
                page, size, estado);

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort));
        Page<OrderResponseDto> orders = orderService.getAllOrders(
                estado, operario, lote, articulo, pageable
        );

        ApiResponse<Page<OrderResponseDto>> response = ApiResponse.success(
                "Órdenes obtenidas exitosamente",
                orders
        );
        return ResponseEntity.ok(response);
    }

    // ========================================
    // OBTENER ORDEN POR ID
    // ========================================

    /**
     * Obtiene una orden por su ID.
     *
     * @param id identificador único de la orden
     * @return orden encontrada
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "Obtener orden por ID",
            description = "Obtiene una orden específica por su ID"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Orden encontrada"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Orden no encontrada"
            )
    })
    public ResponseEntity<ApiResponse<OrderResponseDto>> getOrderById(
            @Parameter(description = "ID de la orden")
            @PathVariable @Min(1) Long id) {

        log.info("GET /api/v1/orders/{} - Obtener orden por ID", id);
        OrderResponseDto order = orderService.getOrderById(id);
        ApiResponse<OrderResponseDto> response = ApiResponse.success(
                "Orden obtenida exitosamente",
                order
        );
        return ResponseEntity.ok(response);
    }

    // ========================================
    // OBTENER ORDEN POR CÓDIGO
    // ========================================

    /**
     * Obtiene una orden por su código único.
     *
     * @param codOrder código único de la orden
     * @return orden encontrada
     */
    @GetMapping("/codigo/{codOrder}")
    @Operation(
            summary = "Obtener orden por código",
            description = "Obtiene una orden específica por su código único"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Orden encontrada"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Orden no encontrada"
            )
    })
    public ResponseEntity<ApiResponse<OrderResponseDto>> getOrderByCodOrder(
            @Parameter(description = "Código de la orden")
            @PathVariable String codOrder) {

        log.info("GET /api/v1/orders/codigo/{} - Obtener orden por código", codOrder);
        OrderResponseDto order = orderService.getOrderByCodOrder(codOrder);
        ApiResponse<OrderResponseDto> response = ApiResponse.success(
                "Orden obtenida exitosamente",
                order
        );
        return ResponseEntity.ok(response);
    }

    // ========================================
    // ESTADÍSTICAS
    // ========================================

    /**
     * Obtiene estadísticas de órdenes agrupadas por estado.
     *
     * @return mapa con conteo de órdenes por estado
     */
    @GetMapping("/statistics")
    @Operation(
            summary = "Obtener estadísticas",
            description = "Obtiene el conteo de órdenes por estado"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Estadísticas obtenidas"
            )
    })
    public ResponseEntity<ApiResponse<Map<String, Long>>> getStatistics() {

        log.info("GET /api/v1/orders/statistics - Obtener estadísticas");
        Map<String, Long> stats = orderService.getOrdersStatistics();
        ApiResponse<Map<String, Long>> response = ApiResponse.success(
                "Estadísticas obtenidas exitosamente",
                stats
        );
        return ResponseEntity.ok(response);
    }

    // ========================================
    // INICIAR ORDEN
    // ========================================

    /**
     * Inicia una orden de producción.
     * Cambia el estado de CREADA a EN_PROCESO y registra la hora de inicio.
     *
     * @param id identificador de la orden a iniciar
     * @return orden actualizada con estado EN_PROCESO
     */
    @PatchMapping("/{id}/iniciar")
    @Operation(
            summary = "Iniciar orden",
            description = "Cambia el estado de una orden de CREADA a EN_PROCESO y registra la hora de inicio"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Orden iniciada exitosamente"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Orden no encontrada"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Estado inválido para iniciar"
            )
    })
    public ResponseEntity<ApiResponse<OrderResponseDto>> iniciarOrder(
            @Parameter(description = "ID de la orden a iniciar")
            @PathVariable @Min(1) Long id) {

        log.info("PATCH /api/v1/orders/{}/iniciar - Iniciar orden", id);
        OrderResponseDto order = orderService.iniciarOrder(id);
        ApiResponse<OrderResponseDto> response = ApiResponse.success(
                "Orden iniciada exitosamente",
                order
        );
        return ResponseEntity.ok(response);
    }

    // ========================================
    // FINALIZAR ORDEN
    // ========================================

    /**
     * Finaliza una orden de producción.
     *
     * <p>Comportamiento según el campo {@code acumula}:</p>
     * <ul>
     *   <li><b>acumula=false (default):</b> Estado pasa a FINALIZADA</li>
     *   <li><b>acumula=true:</b> Estado pasa a ESPERA_MANUAL (requiere proceso manual posterior)</li>
     * </ul>
     *
     * <p><b>IMPORTANTE:</b> Las métricas (OEE, disponibilidad, rendimiento, calidad)
     * se calculan EN ESTE PUNTO, independientemente del valor de acumula.
     * NO se recalculan cuando finalice el proceso manual.</p>
     *
     * @param id  identificador de la orden a finalizar
     * @param dto datos de finalización (incluye campo acumula)
     * @return orden actualizada
     */
    @PatchMapping("/{id}/finalizar")
    @Operation(
            summary = "Finalizar orden",
            description = "Finaliza una orden de producción. Si acumula=true, pasa a ESPERA_MANUAL; " +
                    "si acumula=false, pasa directamente a FINALIZADA. " +
                    "Las métricas se calculan SOLO en este punto, no se recalculan después."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Orden finalizada/en espera manual exitosamente"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Orden no encontrada"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Datos de finalización inválidos"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Estado inválido para finalizar"
            )
    })
    public ResponseEntity<ApiResponse<OrderResponseDto>> finalizarOrder(
            @Parameter(description = "ID de la orden a finalizar")
            @PathVariable @Min(1) Long id,
            @Valid @RequestBody OrderFinishDto dto) {

        log.info("PATCH /api/v1/orders/{}/finalizar - Finalizar orden (acumula: {})",
                id, dto.getAcumula());

        OrderResponseDto order = orderService.finalizarOrder(id, dto);

        String mensaje = Boolean.TRUE.equals(dto.getAcumula())
                ? "Orden en ESPERA_MANUAL. Métricas calculadas. Pendiente proceso manual."
                : "Orden finalizada exitosamente. Métricas calculadas.";

        ApiResponse<OrderResponseDto> response = ApiResponse.success(mensaje, order);
        return ResponseEntity.ok(response);
    }

    // ========================================
    // ENDPOINTS POR ESTADO
    // ========================================

    /**
     * Lista órdenes por estado específico.
     */
    private ResponseEntity<ApiResponse<Page<OrderResponseDto>>> getOrdersByEstado(
            EstadoOrder estado,
            int page,
            int size,
            String sort,
            Sort.Direction direction,
            String mensaje) {

        log.info("GET /api/v1/orders/estado/{} - Listar órdenes en estado {}",
                estado.name().toLowerCase(), estado);

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort));
        Page<OrderResponseDto> orders = orderService.getAllOrders(
                estado, null, null, null, pageable
        );

        ApiResponse<Page<OrderResponseDto>> response = ApiResponse.success(mensaje, orders);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/estado/creadas")
    @Operation(
            summary = "Listar órdenes creadas",
            description = "Obtiene todas las órdenes en estado CREADA con paginación"
    )
    public ResponseEntity<ApiResponse<Page<OrderResponseDto>>> getOrdenesCreadas(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "horaCreacion") String sort,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {

        return getOrdersByEstado(
                EstadoOrder.CREADA, page, size, sort, direction,
                "Órdenes creadas obtenidas exitosamente"
        );
    }

    @GetMapping("/estado/en-proceso")
    @Operation(
            summary = "Listar órdenes en proceso",
            description = "Obtiene todas las órdenes en estado EN_PROCESO con paginación"
    )
    public ResponseEntity<ApiResponse<Page<OrderResponseDto>>> getOrdenesEnProceso(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "horaInicio") String sort,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {

        return getOrdersByEstado(
                EstadoOrder.EN_PROCESO, page, size, sort, direction,
                "Órdenes en proceso obtenidas exitosamente"
        );
    }

    @GetMapping("/estado/pausadas")
    @Operation(
            summary = "Listar órdenes pausadas",
            description = "Obtiene todas las órdenes en estado PAUSADA con paginación"
    )
    public ResponseEntity<ApiResponse<Page<OrderResponseDto>>> getOrdenesPausadas(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "horaCreacion") String sort,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {

        return getOrdersByEstado(
                EstadoOrder.PAUSADA, page, size, sort, direction,
                "Órdenes pausadas obtenidas exitosamente"
        );
    }

    @GetMapping("/estado/finalizadas")
    @Operation(
            summary = "Listar órdenes finalizadas",
            description = "Obtiene todas las órdenes en estado FINALIZADA con paginación"
    )
    public ResponseEntity<ApiResponse<Page<OrderResponseDto>>> getOrdenesFinalizadas(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "horaCreacion") String sort,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {

        return getOrdersByEstado(
                EstadoOrder.FINALIZADA, page, size, sort, direction,
                "Órdenes finalizadas obtenidas exitosamente"
        );
    }

    @GetMapping("/estado/espera-manual")
    @Operation(
            summary = "Listar órdenes en espera manual",
            description = "Obtiene todas las órdenes en estado ESPERA_MANUAL con paginación. " +
                    "Estas órdenes han finalizado producción pero requieren proceso manual adicional."
    )
    public ResponseEntity<ApiResponse<Page<OrderResponseDto>>> getOrdenesEsperaManual(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "horaFin") String sort,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {

        return getOrdersByEstado(
                EstadoOrder.ESPERA_MANUAL, page, size, sort, direction,
                "Órdenes en espera manual obtenidas exitosamente"
        );
    }

    @GetMapping("/estado/proceso-manual")
    @Operation(
            summary = "Listar órdenes en proceso manual",
            description = "Obtiene todas las órdenes en estado PROCESO_MANUAL con paginación. " +
                    "Estas órdenes están siendo procesadas manualmente por un operario."
    )
    public ResponseEntity<ApiResponse<Page<OrderResponseDto>>> getOrdenesProcesoManual(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "horaCreacion") String sort,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {

        return getOrdersByEstado(
                EstadoOrder.PROCESO_MANUAL, page, size, sort, direction,
                "Órdenes en proceso manual obtenidas exitosamente"
        );
    }

    // ========================================
    // ENDPOINTS ESPECIALES
    // ========================================

    @GetMapping("/completa/{id}")
    @Operation(
            summary = "Obtener orden completa",
            description = "Obtiene una orden con todos sus datos relacionados: métricas, extra data, etc."
    )
    public ResponseEntity<OrderCompletaDto> getOrderCompleta(
            @PathVariable @Min(1) Long id) {

        log.info("GET /api/v1/orders/completa/{} - Obtener orden completa", id);
        OrderCompletaDto dto = orderService.obtenerOrdenCompleta(id);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/table/{id}")
    @Operation(
            summary = "Obtener orden para tabla",
            description = "Obtiene datos resumidos de una orden para visualización en tabla"
    )
    public ResponseEntity<OrderTableResponse> getOrderTable(
            @PathVariable @Min(1) Long id) {

        log.info("GET /api/v1/orders/table/{} - Obtener orden para tabla", id);
        return ResponseEntity.ok(orderService.obtenerOrderTable(id));
    }

    @GetMapping("/table")
    @Operation(
            summary = "Obtener todas las órdenes para tabla",
            description = "Obtiene datos resumidos de todas las órdenes para visualización en tabla"
    )
    public ResponseEntity<List<OrderTableResponse>> getAllOrdersTable() {

        log.info("GET /api/v1/orders/table - Obtener todas las órdenes para tabla");
        return ResponseEntity.ok(orderService.obtenerTodasLasOrdersTable());
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> exportOrderToExcel(@PathVariable Long id) {
        log.info("Solicitud de exportación Excel para orden ID: {}", id);

        try {
            byte[] excelFile = orderExportService.exportOrderToExcel(id);

            OrderResponseDto order = orderService.getOrderById(id);
            String filename = String.format("Orden_%s_%s.xlsx",
                    order.getCodOrder().replace("/", "-"),
                    java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);

            return ResponseEntity.ok().headers(headers).body(excelFile);

        } catch (Exception e) {
            log.error("Error al exportar orden {} a Excel", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}/export-data")
    public ResponseEntity<OrderExportDto> getExportData(@PathVariable Long id) {
        try {
            OrderExportDto exportData = orderExportService.getOrderExportData(id);
            return ResponseEntity.ok(exportData);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Exporta múltiples órdenes en un solo archivo Excel (formato horizontal).
     * POST /api/v1/orders/export-multiple
     *
     * @param orderIds Lista de IDs de órdenes a exportar
     * @return Archivo Excel con todas las órdenes en formato horizontal
     */
    @PostMapping("/export-multiple")
    public ResponseEntity<byte[]> exportMultipleOrders(@RequestBody List<Long> orderIds) {
        log.info("Solicitud de exportación múltiple para {} órdenes", orderIds.size());

        if (orderIds == null || orderIds.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            byte[] excelFile = orderBulkExportService.exportMultipleOrdersToExcel(orderIds);

            String filename = String.format("Ordenes_Cremer_%s.xlsx",
                    java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            log.info("Archivo Excel múltiple generado: {} ({} órdenes)", filename, orderIds.size());

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelFile);

        } catch (Exception e) {
            log.error("Error al exportar múltiples órdenes a Excel", e);
            return ResponseEntity.internalServerError().build();
        }


    }

    /**
     * Exporta múltiples órdenes en un solo archivo PDF (formato horizontal).
     * POST /api/v1/orders/export-pdf-multiple
     *
     * @param orderIds Lista de IDs de órdenes a exportar
     * @return Archivo PDF con todas las órdenes en formato horizontal
     */
    @PostMapping("/export-pdf-multiple") // Nuevo endpoint
    @Operation(
            summary = "Exportar múltiples órdenes a PDF",
            description = "Genera un único archivo PDF con los datos de múltiples órdenes en formato horizontal, incluyendo logo."
    )
    public ResponseEntity<byte[]> exportMultipleOrdersToPdf(@RequestBody List<Long> orderIds) {
        log.info("Solicitud de exportación múltiple a PDF para {} órdenes", orderIds.size());

        if (orderIds == null || orderIds.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            // Llama al nuevo servicio de exportación a PDF
            byte[] pdfFile = orderPdfExportService.exportMultipleOrdersToPdf(orderIds);

            // Generación del nombre de archivo
            String filename = String.format("Ordenes_Cremer_%s.pdf", // Cambiado a .pdf
                    java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF); // Cambiado a APPLICATION_PDF
            headers.setContentDispositionFormData("attachment", filename);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            log.info("Archivo PDF múltiple generado: {} ({} órdenes)", filename, orderIds.size());

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfFile);

        } catch (Exception e) {
            log.error("Error al exportar múltiples órdenes a PDF", e);
            return ResponseEntity.internalServerError().build();
        }
    }
















}