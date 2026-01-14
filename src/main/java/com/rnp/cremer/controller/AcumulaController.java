package com.rnp.cremer.controller;

import com.rnp.cremer.dto.AcumulaFinishDto;
import com.rnp.cremer.dto.AcumulaResponseDto;
import com.rnp.cremer.dto.ApiResponse;
import com.rnp.cremer.service.AcumulaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para gestión del proceso manual (acumulación).
 *
 * <p>Expone endpoints para:</p>
 * <ul>
 *   <li>Iniciar proceso manual</li>
 *   <li>Finalizar proceso manual</li>
 *   <li>Consultar estado del proceso manual</li>
 * </ul>
 *
 * <h3>Flujo de Estados:</h3>
 * <pre>
 * ESPERA_MANUAL → PATCH /iniciar-manual → PROCESO_MANUAL → PATCH /finalizar-manual → FINALIZADA
 * </pre>
 *
 * <h3>Nota sobre Métricas:</h3>
 * <p>Las métricas se calculan UNA SOLA VEZ cuando la orden sale de EN_PROCESO
 * (en el endpoint /finalizar). NO se recalculan durante el proceso manual.</p>
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
@Tag(name = "Proceso Manual", description = "API de Gestión del Proceso Manual (Acumulación)")
public class AcumulaController {

    private final AcumulaService acumulaService;

    // ========================================
    // INICIAR PROCESO MANUAL
    // ========================================

    /**
     * Inicia el proceso manual de una orden.
     *
     * <p>Transición: ESPERA_MANUAL → PROCESO_MANUAL</p>
     *
     * @param id ID de la orden
     * @return datos del proceso manual iniciado
     */
    @Operation(
            summary = "Iniciar proceso manual",
            description = "Inicia el proceso manual de una orden que está en ESPERA_MANUAL. " +
                    "Crea un registro de acumulación y cambia el estado a PROCESO_MANUAL."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Proceso manual iniciado exitosamente"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Orden no encontrada"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Estado inválido - La orden debe estar en ESPERA_MANUAL"
            )
    })
    @PatchMapping("/{id}/iniciar-manual")
    public ResponseEntity<ApiResponse<AcumulaResponseDto>> iniciarProcesoManual(
            @Parameter(description = "ID de la orden", required = true)
            @PathVariable @Min(1) Long id) {

        log.info("PATCH /api/v1/orders/{}/iniciar-manual - Iniciar proceso manual", id);

        AcumulaResponseDto result = acumulaService.iniciarProcesoManual(id);

        ApiResponse<AcumulaResponseDto> response = ApiResponse.success(
                "Proceso manual iniciado exitosamente",
                result
        );

        return ResponseEntity.ok(response);
    }

    // ========================================
    // FINALIZAR PROCESO MANUAL
    // ========================================

    /**
     * Finaliza el proceso manual de una orden.
     *
     * <p>Transición: PROCESO_MANUAL → FINALIZADA</p>
     *
     * <p><b>IMPORTANTE:</b> Las métricas NO se recalculan en este punto.
     * Ya fueron calculadas cuando la orden salió de EN_PROCESO.</p>
     *
     * @param id ID de la orden
     * @param dto datos de finalización (número de cajas procesadas)
     * @return datos del proceso manual finalizado
     */
    @Operation(
            summary = "Finalizar proceso manual",
            description = "Finaliza el proceso manual de una orden que está en PROCESO_MANUAL. " +
                    "Registra las cajas procesadas, calcula tiempo total y cambia el estado a FINALIZADA. " +
                    "NOTA: Las métricas NO se recalculan en este punto."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Proceso manual finalizado exitosamente"
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
                    description = "Estado inválido - La orden debe estar en PROCESO_MANUAL"
            )
    })
    @PatchMapping("/{id}/finalizar-manual")
    public ResponseEntity<ApiResponse<AcumulaResponseDto>> finalizarProcesoManual(
            @Parameter(description = "ID de la orden", required = true)
            @PathVariable @Min(1) Long id,
            @Valid @RequestBody AcumulaFinishDto dto) {

        log.info("PATCH /api/v1/orders/{}/finalizar-manual - Cajas: {}", id, dto.getNumCajasManual());

        AcumulaResponseDto result = acumulaService.finalizarProcesoManual(id, dto);

        ApiResponse<AcumulaResponseDto> response = ApiResponse.success(
                "Proceso manual finalizado exitosamente. Orden FINALIZADA.",
                result
        );

        return ResponseEntity.ok(response);
    }

    // ========================================
    // CONSULTAS
    // ========================================

    /**
     * Obtiene los datos de acumulación de una orden.
     *
     * @param id ID de la orden
     * @return datos de acumulación
     */
    @Operation(
            summary = "Obtener datos de acumulación",
            description = "Obtiene los datos del proceso manual de una orden, " +
                    "incluyendo tiempos y cajas procesadas."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Datos obtenidos exitosamente"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Orden no encontrada"
            )
    })
    @GetMapping("/{id}/acumula")
    public ResponseEntity<ApiResponse<AcumulaResponseDto>> getAcumulaByOrder(
            @Parameter(description = "ID de la orden", required = true)
            @PathVariable @Min(1) Long id) {

        log.info("GET /api/v1/orders/{}/acumula - Obtener datos de acumulación", id);

        AcumulaResponseDto result = acumulaService.getAcumulaByOrder(id);

        ApiResponse<AcumulaResponseDto> response = ApiResponse.success(
                "Datos de acumulación obtenidos exitosamente",
                result
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Verifica si una orden tiene proceso manual activo.
     *
     * @param id ID de la orden
     * @return true si tiene proceso activo
     */
    @Operation(
            summary = "Verificar proceso manual activo",
            description = "Verifica si una orden tiene un proceso manual activo (sin finalizar)."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Verificación exitosa"
            )
    })
    @GetMapping("/{id}/acumula/activo")
    public ResponseEntity<ApiResponse<Boolean>> hasActiveProcess(
            @Parameter(description = "ID de la orden", required = true)
            @PathVariable @Min(1) Long id) {

        log.info("GET /api/v1/orders/{}/acumula/activo - Verificar proceso activo", id);

        boolean hasActive = acumulaService.hasActiveProcess(id);

        ApiResponse<Boolean> response = ApiResponse.success(
                hasActive ? "La orden tiene proceso manual activo" : "La orden no tiene proceso manual activo",
                hasActive
        );

        return ResponseEntity.ok(response);
    }
}