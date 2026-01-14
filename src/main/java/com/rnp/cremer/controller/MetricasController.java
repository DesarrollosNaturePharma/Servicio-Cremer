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

/**
 * Controlador REST para métricas de producción.
 *
 * @author RNP Team
 * @version 1.0
 * @since 2024-11-26
 */
@RestController
@RequestMapping("/api/v1//orders/{idOrder}/metricas")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Metricas", description = "API de Métricas de Producción (OEE, Disponibilidad, Rendimiento, Calidad)")
public class MetricasController {

    private final MetricasService metricasService;

    /**
     * Obtiene las métricas de una orden.
     */
    @GetMapping
    @Operation(summary = "Obtener métricas", description = "Obtiene las métricas calculadas de una orden")
    public ResponseEntity<ApiResponse<Metricas>> getMetricas(
            @Parameter(description = "ID de la orden") @PathVariable Long idOrder) {

        log.info("GET /orders/{}/metricas", idOrder);

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
    @PostMapping("/recalcular")
    @Operation(
            summary = "Recalcular métricas",
            description = "Elimina y recalcula las métricas de una orden. Útil cuando se modifican datos manualmente en BD."
    )
    public ResponseEntity<ApiResponse<Metricas>> recalcularMetricas(
            @Parameter(description = "ID de la orden") @PathVariable Long idOrder) {

        log.info("POST /orders/{}/metricas/recalcular", idOrder);

        Metricas metricas = metricasService.recalcularMetricas(idOrder);

        ApiResponse<Metricas> response = ApiResponse.success(
                "Métricas recalculadas exitosamente",
                metricas
        );

        return ResponseEntity.ok(response);
    }
}