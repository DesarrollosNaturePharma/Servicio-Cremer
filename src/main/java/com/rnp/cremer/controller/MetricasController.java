package com.rnp.cremer.controller;

import com.rnp.cremer.dto.ApiResponse;
import com.rnp.cremer.model.Metricas;
import com.rnp.cremer.service.MetricasService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controlador REST para métricas de producción.
 *
 * @author RNP Team
 * @version 1.1
 * @since 2024-11-26
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Metricas", description = "API de Métricas de Producción (OEE, Disponibilidad, Rendimiento, Calidad)")
public class MetricasController {

    private final MetricasService metricasService;

    /**
     * Obtiene las métricas de una orden.
     */
    @GetMapping("/{idOrder}/metricas")
    @Operation(summary = "Obtener métricas", description = "Obtiene las métricas calculadas de una orden")
    public ResponseEntity<ApiResponse<Metricas>> getMetricas(
            @Parameter(description = "ID de la orden") @PathVariable Long idOrder) {

        log.info("GET /api/v1/orders/{}/metricas", idOrder);

        Metricas metricas = metricasService.getMetricasByOrder(idOrder);

        if (metricas == null) {
            return ResponseEntity.ok(ApiResponse.success(
                    "No existen métricas para esta orden",
                    null
            ));
        }

        ApiResponse<Metricas> response = ApiResponse.success(
                "Métricas obtenidas exitosamente",
                metricas
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Recalcula las métricas de una orden.
     */
    @PostMapping("/{idOrder}/metricas/recalcular")
    @Operation(
            summary = "Recalcular métricas",
            description = "Elimina y recalcula las métricas de una orden. Útil cuando se modifican datos manualmente en BD."
    )
    public ResponseEntity<ApiResponse<Metricas>> recalcularMetricas(
            @Parameter(description = "ID de la orden") @PathVariable Long idOrder) {

        log.info("POST /api/v1/orders/{}/metricas/recalcular", idOrder);

        Metricas metricas = metricasService.recalcularMetricas(idOrder);

        ApiResponse<Metricas> response = ApiResponse.success(
                "Métricas recalculadas exitosamente",
                metricas
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Recalcula las métricas de TODAS las órdenes cerradas.
     *
     * Recalcula únicamente órdenes en estados:
     * FINALIZADA, ESPERA_MANUAL, PROCESO_MANUAL
     */
    @PostMapping("/metricas/recalcular-todas")
    @Operation(
            summary = "Recalcular métricas de todas las órdenes",
            description = "Recalcula (elimina y vuelve a calcular) las métricas de todas las órdenes en estados FINALIZADA, ESPERA_MANUAL y PROCESO_MANUAL."
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> recalcularTodasMetricas() {

        log.info("POST /api/v1/orders/metricas/recalcular-todas");

        Map<String, Object> result = metricasService.recalcularTodasMetricas();

        return ResponseEntity.ok(
                ApiResponse.success("Recalculo masivo de métricas completado", result)
        );
    }
}
