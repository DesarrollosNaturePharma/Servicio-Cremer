package com.rnp.cremer.controller;

import com.rnp.cremer.dto.ApiResponse;
import com.rnp.cremer.dto.OrderDeleteAuditResponseDto;
import com.rnp.cremer.dto.OrderDeleteRequestDto;
import com.rnp.cremer.service.OrderDeleteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Controlador REST para eliminación de órdenes con auditoría.
 *
 * <p>Proporciona endpoints para:</p>
 * <ul>
 *   <li>Eliminar órdenes con registro de auditoría</li>
 *   <li>Eliminar múltiples órdenes</li>
 *   <li>Consultar historial de eliminaciones</li>
 * </ul>
 *
 * @author RNP Team
 * @version 1.0
 * @since 2024-11-26
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Order Delete", description = "API de Eliminación de Órdenes con Auditoría")
public class OrderDeleteController {

    private final OrderDeleteService orderDeleteService;

    // ========================================
    // ELIMINAR ORDEN
    // ========================================

    /**
     * Elimina una orden con registro de auditoría.
     *
     * @param id ID de la orden a eliminar
     * @param dto Datos de la solicitud (usuario y motivo)
     * @param request HttpServletRequest para obtener IP
     * @return Registro de auditoría de la eliminación
     */
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Eliminar orden",
            description = "Elimina una orden registrando auditoría completa. " +
                    "No se pueden eliminar órdenes en estado EN_PROCESO o PROCESO_MANUAL."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Orden eliminada exitosamente"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Orden no encontrada"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "No se puede eliminar orden en estado activo"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Datos de solicitud inválidos"
            )
    })
    public ResponseEntity<ApiResponse<OrderDeleteAuditResponseDto>> deleteOrder(
            @Parameter(description = "ID de la orden a eliminar")
            @PathVariable @Min(1) Long id,
            @Valid @RequestBody OrderDeleteRequestDto dto,
            HttpServletRequest request) {

        log.info("DELETE /api/v1/orders/{} - Eliminar orden por usuario: {}", id, dto.getDeletedBy());

        String ipAddress = getClientIpAddress(request);
        OrderDeleteAuditResponseDto audit = orderDeleteService.deleteOrder(id, dto, ipAddress);

        ApiResponse<OrderDeleteAuditResponseDto> response = ApiResponse.success(
                "Orden eliminada exitosamente. Registro de auditoría creado.",
                audit
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Elimina múltiples órdenes con registro de auditoría.
     *
     * @param orderIds Lista de IDs de órdenes a eliminar
     * @param dto Datos de la solicitud
     * @param request HttpServletRequest para obtener IP
     * @return Lista de registros de auditoría
     */
    @DeleteMapping("/batch")
    @Operation(
            summary = "Eliminar múltiples órdenes",
            description = "Elimina múltiples órdenes en una sola operación. " +
                    "Las órdenes que no se puedan eliminar se omiten sin detener el proceso."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Órdenes procesadas"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Lista vacía o datos inválidos"
            )
    })
    public ResponseEntity<ApiResponse<List<OrderDeleteAuditResponseDto>>> deleteMultipleOrders(
            @Parameter(description = "Lista de IDs de órdenes a eliminar")
            @RequestParam List<Long> orderIds,
            @Valid @RequestBody OrderDeleteRequestDto dto,
            HttpServletRequest request) {

        log.info("DELETE /api/v1/orders/batch - Eliminar {} órdenes por usuario: {}",
                orderIds.size(), dto.getDeletedBy());

        if (orderIds == null || orderIds.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("La lista de IDs no puede estar vacía"));
        }

        String ipAddress = getClientIpAddress(request);
        List<OrderDeleteAuditResponseDto> audits = orderDeleteService.deleteMultipleOrders(
                orderIds, dto, ipAddress
        );

        String mensaje = String.format(
                "%d de %d órdenes eliminadas exitosamente",
                audits.size(), orderIds.size()
        );

        ApiResponse<List<OrderDeleteAuditResponseDto>> response = ApiResponse.success(mensaje, audits);
        return ResponseEntity.ok(response);
    }

    // ========================================
    // CONSULTAR AUDITORÍA
    // ========================================

    /**
     * Obtiene el historial de eliminaciones con paginación.
     */
    @GetMapping("/audit/delete")
    @Operation(
            summary = "Listar auditoría de eliminaciones",
            description = "Obtiene el historial de órdenes eliminadas con filtros opcionales"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Registros obtenidos exitosamente"
            )
    })
    public ResponseEntity<ApiResponse<Page<OrderDeleteAuditResponseDto>>> getDeleteAuditRecords(
            @Parameter(description = "Usuario que eliminó")
            @RequestParam(required = false) String deletedBy,

            @Parameter(description = "Código de orden")
            @RequestParam(required = false) String codOrder,

            @Parameter(description = "Fecha inicio (ISO format)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,

            @Parameter(description = "Fecha fin (ISO format)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin,

            @Parameter(description = "Número de página")
            @RequestParam(defaultValue = "0") @Min(0) int page,

            @Parameter(description = "Tamaño de página")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        log.info("GET /api/v1/orders/audit/delete - Consultar auditoría");

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "deletedAt"));

        Page<OrderDeleteAuditResponseDto> audits;
        if (deletedBy != null || codOrder != null || fechaInicio != null || fechaFin != null) {
            audits = orderDeleteService.getAuditRecordsByFilters(
                    deletedBy, codOrder, fechaInicio, fechaFin, pageable
            );
        } else {
            audits = orderDeleteService.getAllAuditRecords(pageable);
        }

        ApiResponse<Page<OrderDeleteAuditResponseDto>> response = ApiResponse.success(
                "Registros de auditoría obtenidos exitosamente",
                audits
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Obtiene un registro de auditoría por su ID.
     */
    @GetMapping("/audit/delete/{idAudit}")
    @Operation(
            summary = "Obtener registro de auditoría",
            description = "Obtiene un registro específico de auditoría de eliminación"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Registro encontrado"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Registro no encontrado"
            )
    })
    public ResponseEntity<ApiResponse<OrderDeleteAuditResponseDto>> getAuditRecordById(
            @Parameter(description = "ID del registro de auditoría")
            @PathVariable @Min(1) Long idAudit) {

        log.info("GET /api/v1/orders/audit/delete/{} - Obtener registro", idAudit);

        OrderDeleteAuditResponseDto audit = orderDeleteService.getAuditRecordById(idAudit);

        ApiResponse<OrderDeleteAuditResponseDto> response = ApiResponse.success(
                "Registro de auditoría obtenido exitosamente",
                audit
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Obtiene registros de auditoría por usuario.
     */
    @GetMapping("/audit/delete/user/{deletedBy}")
    @Operation(
            summary = "Obtener auditoría por usuario",
            description = "Obtiene todos los registros de eliminación realizados por un usuario"
    )
    public ResponseEntity<ApiResponse<Page<OrderDeleteAuditResponseDto>>> getAuditRecordsByUser(
            @Parameter(description = "Nombre del usuario")
            @PathVariable String deletedBy,

            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        log.info("GET /api/v1/orders/audit/delete/user/{} - Auditoría por usuario", deletedBy);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "deletedAt"));
        Page<OrderDeleteAuditResponseDto> audits = orderDeleteService.getAuditRecordsByUser(
                deletedBy, pageable
        );

        ApiResponse<Page<OrderDeleteAuditResponseDto>> response = ApiResponse.success(
                "Registros de auditoría obtenidos exitosamente",
                audits
        );
        return ResponseEntity.ok(response);
    }

    // ========================================
    // MÉTODOS AUXILIARES
    // ========================================

    /**
     * Obtiene la dirección IP del cliente.
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
