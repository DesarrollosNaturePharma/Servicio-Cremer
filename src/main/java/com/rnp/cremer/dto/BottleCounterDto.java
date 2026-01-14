package com.rnp.cremer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para respuestas del contador de botellas.
 *
 * @author RNP Team
 * @version 1.0
 * @since 2024-12-12
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BottleCounterDto {

    /**
     * ID del contador.
     */
    private Long id;

    /**
     * ID de la orden asociada.
     */
    private Long idOrder;

    /**
     * Código de la orden (para mostrar en UI).
     */
    private String codOrder;

    /**
     * Cantidad actual de botellas contadas.
     */
    private Integer quantity;

    /**
     * Indica si el contador está activo.
     */
    private Boolean isActive;

    /**
     * Fecha y hora de la última actualización.
     */
    private LocalDateTime lastUpdated;

    /**
     * Fecha y hora del último conteo de botella.
     */
    private LocalDateTime lastBottleCountedAt;
}