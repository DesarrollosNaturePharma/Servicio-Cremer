package com.rnp.cremer.controller;

import com.rnp.cremer.dto.ApiResponse;
import com.rnp.cremer.dto.BottleCounterDto;
import com.rnp.cremer.service.BottleCounterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controlador REST para gestión del contador de botellas.
 *
 * <p>Proporciona endpoints para consultar y gestionar el conteo de botellas
 * integrado con sensores GPIO de Raspberry Pi.</p>
 *
 * @author RNP Team
 * @version 1.0
 * @since 2024-12-12
 */
@RestController
@RequestMapping("/api/v1/bottle-counter")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Bottle Counter", description = "API de Contador de Botellas con GPIO")
public class BottleCounterController {

    private final BottleCounterService bottleCounterService;

    // ========================================
    // CONSULTAS
    // ========================================

    /**
     * Obtiene el contador activo actual.
     *
     * @return contador de la orden activa o 404 si no hay ninguna
     */
    @GetMapping("/active")
    @Operation(
            summary = "Obtener contador activo",
            description = "Obtiene el contador de la orden actualmente activa (EN_PROCESO o PAUSADA)"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Contador obtenido exitosamente"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "No hay contador activo"
            )
    })
    public ResponseEntity<ApiResponse<BottleCounterDto>> getActiveCounter() {
        log.info("GET /api/v1/bottle-counter/active - Obtener contador activo");

        return bottleCounterService.getActiveCounter()
                .map(counter -> {
                    ApiResponse<BottleCounterDto> response = ApiResponse.success(
                            "Contador activo obtenido exitosamente",
                            counter
                    );
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    ApiResponse<BottleCounterDto> response = ApiResponse.error( "No hay contador activo en este momento" );
                    return ResponseEntity.status(404).body(response); });
    }

    /**
     * Obtiene el contador de una orden específica.
     *
     * @param orderId ID de la orden
     * @return contador de la orden o 404 si no existe
     */
    @GetMapping("/order/{orderId}")
    @Operation(
            summary = "Obtener contador por orden",
            description = "Obtiene el contador de botellas de una orden específica"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Contador obtenido exitosamente"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Contador no encontrado para esta orden"
            )
    })
    public ResponseEntity<ApiResponse<BottleCounterDto>> getCounterByOrder(
            @Parameter(description = "ID de la orden")
            @PathVariable Long orderId) {

        log.info("GET /api/v1/bottle-counter/order/{} - Obtener contador por orden", orderId);

        return bottleCounterService.getCounterByOrderId(orderId)
                .map(counter -> {
                    ApiResponse<BottleCounterDto> response = ApiResponse.success(
                            "Contador obtenido exitosamente",
                            counter
                    );
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    ApiResponse<BottleCounterDto> response = ApiResponse.error( "No se encontró contador para esta orden" );
                    return ResponseEntity.status(404).body(response); });
    }

    // ========================================
    // ACCIONES
    // ========================================

    /**
     * Activa el contador para una orden.
     * Se llama automáticamente cuando una orden pasa a EN_PROCESO.
     *
     * @param orderId ID de la orden
     * @return confirmación de activación
     */
    @PostMapping("/activate/{orderId}")
    @Operation(
            summary = "Activar contador",
            description = "Activa el contador de botellas para una orden específica"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Contador activado exitosamente"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Error al activar contador"
            )
    })
    public ResponseEntity<ApiResponse<Void>> activateCounter(
            @Parameter(description = "ID de la orden")
            @PathVariable Long orderId) {

        log.info("POST /api/v1/bottle-counter/activate/{} - Activar contador", orderId);

        try {
            bottleCounterService.activateCounterForOrder(orderId);
            ApiResponse<Void> response = ApiResponse.success(
                    "Contador activado exitosamente",
                    null
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error al activar contador", e);
            ApiResponse<Void> response = ApiResponse.error( "Error al activar contador: " + e.getMessage() );
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Desactiva el contador de una orden.
     * Se llama automáticamente cuando una orden pasa a FINALIZADA.
     *
     * @param orderId ID de la orden
     * @return confirmación de desactivación
     */
    @PostMapping("/deactivate/{orderId}")
    @Operation(
            summary = "Desactivar contador",
            description = "Desactiva el contador de botellas para una orden específica"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Contador desactivado exitosamente"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Error al desactivar contador"
            )
    })
    public ResponseEntity<ApiResponse<Void>> deactivateCounter(
            @Parameter(description = "ID de la orden")
            @PathVariable Long orderId) {

        log.info("POST /api/v1/bottle-counter/deactivate/{} - Desactivar contador", orderId);

        try {
            bottleCounterService.deactivateCounterForOrder(orderId);
            ApiResponse<Void> response = ApiResponse.success(
                    "Contador desactivado exitosamente",
                    null
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error al desactivar contador", e);
            ApiResponse<Void> response = ApiResponse.error( "Error al desactivar contador: " + e.getMessage() );
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Resetea el contador de una orden a 0.
     * USAR CON PRECAUCIÓN: esto borra el conteo actual.
     *
     * @param orderId ID de la orden
     * @return confirmación de reseteo
     */
    @PostMapping("/reset/{orderId}")
    @Operation(
            summary = "Resetear contador",
            description = "Resetea el contador de botellas a 0. USAR CON PRECAUCIÓN."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Contador reseteado exitosamente"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Error al resetear contador"
            )
    })
    public ResponseEntity<ApiResponse<Void>> resetCounter(
            @Parameter(description = "ID de la orden")
            @PathVariable Long orderId) {

        log.warn("POST /api/v1/bottle-counter/reset/{} - Resetear contador (PRECAUCIÓN)", orderId);

        try {
            bottleCounterService.resetCounter(orderId);
            ApiResponse<Void> response = ApiResponse.success(
                    "Contador reseteado a 0 exitosamente",
                    null
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error al resetear contador", e);
            ApiResponse<Void> response = ApiResponse.error( "Error al resetear contador: " + e.getMessage() );
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ========================================
    // ESTADO DEL SISTEMA
    // ========================================

    /**
     * Obtiene el estado de la conexión con la Raspberry Pi.
     *
     * @return estado de conexión WebSocket
     */
    @GetMapping("/status")
    @Operation(
            summary = "Estado del sistema",
            description = "Obtiene el estado de la conexión WebSocket con la Raspberry Pi"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Estado obtenido exitosamente"
            )
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemStatus() {
        log.info("GET /api/v1/bottle-counter/status - Obtener estado del sistema");

        Map<String, Object> status = new HashMap<>();
        status.put("raspberryPiConnected", bottleCounterService.isConnected());
        status.put("raspberryPiUrl", "192.168.20.30:8765");
        status.put("targetPin", 23);

        ApiResponse<Map<String, Object>> response = ApiResponse.success(
                "Estado del sistema obtenido exitosamente",
                status
        );

        return ResponseEntity.ok(response);
    }
}