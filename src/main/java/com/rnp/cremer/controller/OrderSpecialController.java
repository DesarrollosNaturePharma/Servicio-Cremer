package com.rnp.cremer.controller;

import com.rnp.cremer.dto.ApiResponse;
import com.rnp.cremer.dto.OrderResponseDto;
import com.rnp.cremer.model.Order;
import com.rnp.cremer.model.TipoPausa;
import com.rnp.cremer.repository.OrderRepository;
import com.rnp.cremer.repository.PauseRepository;
import com.rnp.cremer.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controlador REST para consultas especiales de órdenes.
 *
 * @author RNP Team
 * @version 1.0
 * @since 2025-11-27
 */
@RestController
@RequestMapping("/api/v1/orders/special")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Orders - Special", description = "API de Consultas Especiales de Órdenes")
public class OrderSpecialController {

    private final PauseRepository pauseRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;

    /**
     * Obtiene órdenes con pausas activas de tipo FABRICACION_PARCIAL.
     */
    @GetMapping("/fabricacion-parcial")
    @Operation(
            summary = "Listar órdenes con fabricación parcial",
            description = "Obtiene todas las órdenes que tienen pausas activas de tipo FABRICACION_PARCIAL"
    )
    public ResponseEntity<ApiResponse<Page<OrderResponseDto>>> getOrdenesConFabricacionParcial(
            @Parameter(description = "Número de página") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Campo para ordenar") @RequestParam(defaultValue = "horaCreacion") String sort,
            @Parameter(description = "Dirección de orden") @RequestParam(defaultValue = "DESC") Sort.Direction direction) {

        log.info("GET /orders/special/fabricacion-parcial");

        // Obtener IDs de órdenes con pausas activas de tipo FABRICACION_PARCIAL
        List<Long> orderIds = pauseRepository.findOrderIdsWithActivePauseType(TipoPausa.FABRICACION_PARCIAL);

        if (orderIds.isEmpty()) {
            Page<OrderResponseDto> emptyPage = Page.empty(PageRequest.of(page, size));
            ApiResponse<Page<OrderResponseDto>> response = ApiResponse.success(
                    "No hay órdenes con fabricación parcial activa",
                    emptyPage
            );
            return ResponseEntity.ok(response);
        }

        // Buscar las órdenes por sus IDs
        List<Order> orders = orderRepository.findAllById(orderIds);

        // Convertir a DTOs
        List<OrderResponseDto> orderDtos = orders.stream()
                .map(orderService::mapToResponseDtoPublic)
                .collect(Collectors.toList());

        // Aplicar paginación manual
        int start = Math.min(page * size, orderDtos.size());
        int end = Math.min(start + size, orderDtos.size());
        List<OrderResponseDto> pagedOrders = orderDtos.subList(start, end);

        Page<OrderResponseDto> orderPage = new PageImpl<>(
                pagedOrders,
                PageRequest.of(page, size, Sort.by(direction, sort)),
                orderDtos.size()
        );

        ApiResponse<Page<OrderResponseDto>> response = ApiResponse.success(
                "Órdenes con fabricación parcial obtenidas exitosamente",
                orderPage
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Recalcula los tiempos estimados de todas las órdenes.
     * Útil para corregir datos históricos cuando se modifica la fórmula de cálculo.
     */
    @PostMapping("/recalcular-tiempos")
    @Operation(
            summary = "Recalcular tiempos estimados",
            description = "Recalcula los tiempos estimados y cajas previstas de todas las órdenes usando la fórmula actual"
    )
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> recalcularTiemposEstimados() {
        log.info("POST /orders/special/recalcular-tiempos");

        java.util.Map<String, Object> resultado = orderService.recalcularTiemposEstimados();

        ApiResponse<java.util.Map<String, Object>> response = ApiResponse.success(
                "Tiempos estimados recalculados exitosamente",
                resultado
        );

        return ResponseEntity.ok(response);
    }
}
