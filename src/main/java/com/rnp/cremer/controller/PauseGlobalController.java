package com.rnp.cremer.controller;

import com.rnp.cremer.dto.ApiResponse;
import com.rnp.cremer.dto.PauseResponseDto;
import com.rnp.cremer.service.PauseQueryService;  // ⬅️ CAMBIO
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para consultas globales de pausas.
 *
 * @author RNP Team
 * @version 1.0
 * @since 2024-11-27
 */
@RestController
@RequestMapping("/api/v1/pauses")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Pauses - Global", description = "API de Consultas Globales de Pausas")
public class PauseGlobalController {

    private final PauseQueryService pauseQueryService;  // ⬅️ CAMBIO

    /**
     * Obtiene todas las pausas activas excepto las de tipo FABRICACION_PARCIAL.
     */
    @GetMapping("/active/non-partial")
    @Operation(
        summary = "Listar pausas activas (sin fabricación parcial)", 
        description = "Obtiene todas las pausas activas excepto las de tipo FABRICACION_PARCIAL"
    )
    public ResponseEntity<ApiResponse<List<PauseResponseDto>>> getActivePausesNonPartial() {
        
        log.info("GET /api/v1/pauses/active/non-partial");
        
        List<PauseResponseDto> pauses = pauseQueryService.getActivePausesExcludingFabricacionParcial();
        
        ApiResponse<List<PauseResponseDto>> response = ApiResponse.success(
                "Pausas activas obtenidas exitosamente",
                pauses
        );
        
        return ResponseEntity.ok(response);
    }
}