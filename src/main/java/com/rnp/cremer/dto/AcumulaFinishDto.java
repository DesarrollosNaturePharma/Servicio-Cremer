package com.rnp.cremer.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para finalizar el proceso manual de una orden.
 *
 * @author RNP Team
 * @version 1.0
 * @since 2024-11-26
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcumulaFinishDto {

    /**
     * Número de cajas procesadas manualmente.
     */
    @NotNull(message = "El número de cajas es obligatorio")
    @Min(value = 0, message = "El número de cajas no puede ser negativo")
    private Integer numCajasManual;
}