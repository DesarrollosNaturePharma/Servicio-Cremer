package com.rnp.cremer.controller;

import com.rnp.cremer.dto.ApiResponse;
import com.rnp.cremer.dto.PauseCreateDto;
import com.rnp.cremer.dto.PauseFinishDto;
import com.rnp.cremer.dto.PauseResponseDto;
import com.rnp.cremer.service.PauseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para gestión de pausas.
 *
 * @author RNP Team
 * @version 1.0
 * @since 2024-11-25
 */
@RestController
@RequestMapping("/api/v1/orders/{idOrder}/pauses")  
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Pauses", description = "API de Gestión de Pausas")
public class PauseController {

    private final PauseService pauseService;

    /**
     * Crea una nueva pausa para una orden.
     */
    @PostMapping
    @Operation(summary = "Crear pausa", description = "Inicia una nueva pausa en una orden EN_PROCESO")
    public ResponseEntity<ApiResponse<PauseResponseDto>> createPause(
            @Parameter(description = "ID de la orden") @PathVariable Long idOrder,
            @RequestBody(required = false) @Valid PauseCreateDto dto) {  // ⬅️ CAMBIO: required = false

        log.info("POST /orders/{}/pauses - Crear pausa", idOrder);

        // Si no se envía body, crear DTO vacío
        if (dto == null) {
            dto = new PauseCreateDto();
        }

        PauseResponseDto pause = pauseService.createPause(idOrder, dto);

        ApiResponse<PauseResponseDto> response = ApiResponse.success(
                "Pausa creada exitosamente",
                pause
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Finaliza una pausa activa.
     */
    @PatchMapping("/{idPausa}/finalizar")
    @Operation(summary = "Finalizar pausa", description = "Finaliza una pausa activa y calcula su duración")
    public ResponseEntity<ApiResponse<PauseResponseDto>> finishPause(
            @Parameter(description = "ID de la orden") @PathVariable Long idOrder,
            @Parameter(description = "ID de la pausa") @PathVariable Long idPausa,
            @Valid @RequestBody(required = false) PauseFinishDto dto) {

        log.info("PATCH /orders/{}/pauses/{}/finalizar", idOrder, idPausa);

        PauseFinishDto finishDto = dto != null ? dto : new PauseFinishDto();
        PauseResponseDto pause = pauseService.finishPause(idOrder, idPausa, finishDto);

        ApiResponse<PauseResponseDto> response = ApiResponse.success(
                "Pausa finalizada exitosamente",
                pause
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Obtiene todas las pausas de una orden.
     */
    @GetMapping
    @Operation(summary = "Listar pausas", description = "Obtiene todas las pausas de una orden")
    public ResponseEntity<ApiResponse<List<PauseResponseDto>>> getPausesByOrder(
            @Parameter(description = "ID de la orden") @PathVariable Long idOrder) {

        log.info("GET /orders/{}/pauses", idOrder);

        List<PauseResponseDto> pauses = pauseService.getPausesByOrder(idOrder);

        ApiResponse<List<PauseResponseDto>> response = ApiResponse.success(
                "Pausas obtenidas exitosamente",
                pauses
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Obtiene la pausa activa de una orden.
     */
    @GetMapping("/active")
    @Operation(summary = "Obtener pausa activa", description = "Obtiene la pausa activa de una orden (si existe)")
    public ResponseEntity<ApiResponse<PauseResponseDto>> getActivePause(
            @Parameter(description = "ID de la orden") @PathVariable Long idOrder) {

        log.info("GET /orders/{}/pauses/active", idOrder);

        PauseResponseDto pause = pauseService.getActivePause(idOrder);

        ApiResponse<PauseResponseDto> response = ApiResponse.success(
                "Pausa activa obtenida exitosamente",
                pause
        );

        return ResponseEntity.ok(response);
    }


}