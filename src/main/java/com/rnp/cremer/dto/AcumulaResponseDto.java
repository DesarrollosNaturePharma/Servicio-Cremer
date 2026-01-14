package com.rnp.cremer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para datos de acumulación (proceso manual).
 *
 * @author RNP Team
 * @version 1.0
 * @since 2024-11-26
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcumulaResponseDto {

    /**
     * ID del registro de acumulación.
     */
    private Long idAcumula;

    /**
     * ID de la orden asociada.
     */
    private Long idOrder;

    /**
     * Código de la orden (para referencia rápida).
     */
    private String codOrder;

    /**
     * Hora de inicio del proceso manual.
     */
    private LocalDateTime horaInicio;

    /**
     * Hora de fin del proceso manual (null si está en proceso).
     */
    private LocalDateTime horaFin;

    /**
     * Tiempo total del proceso manual en minutos.
     */
    private Float tiempoTotal;

    /**
     * Número de cajas procesadas manualmente.
     */
    private Integer numCajasManual;

    /**
     * Indica si el proceso manual está activo.
     */
    private Boolean enProceso;
}