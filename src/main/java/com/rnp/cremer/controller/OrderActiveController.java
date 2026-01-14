package com.rnp.cremer.controller;

import com.rnp.cremer.dto.ApiResponse;
import com.rnp.cremer.dto.OrderResponseDto;
import com.rnp.cremer.service.OrderQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador para consultas de órdenes activas en tiempo real.
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Orders - Active", description = "API de Órdenes Activas en Tiempo Real")
public class OrderActiveController {

    private final OrderQueryService orderQueryService;

    /**
     * Obtiene la orden activa visible para mostrar en tiempo real.
     *
     * <p>Devuelve la orden EN_PROCESO o PAUSADA que esté visible,
     * excluyendo órdenes pausadas con FABRICACION_PARCIAL.</p>
     *
     * @return orden activa visible, o null si no hay
     */
    @GetMapping("/active/visible")
    @Operation(
            summary = "Obtener orden activa visible",
            description = "Devuelve la orden activa (EN_PROCESO o PAUSADA) visible en tiempo real. " +
                    "Excluye órdenes pausadas con FABRICACION_PARCIAL."
    )
    public ResponseEntity<ApiResponse<OrderResponseDto>> getActiveVisibleOrder() {
        log.info("GET /api/v1/orders/active/visible");

        OrderResponseDto order = orderQueryService.getActiveVisibleOrder();

        if (order == null) {
            ApiResponse<OrderResponseDto> response = ApiResponse.success(
                    "No hay órdenes activas visibles",
                    null
            );
            return ResponseEntity.ok(response);
        }

        ApiResponse<OrderResponseDto> response = ApiResponse.success(
                "Orden activa visible obtenida exitosamente",
                order
        );

        return ResponseEntity.ok(response);
    }
}