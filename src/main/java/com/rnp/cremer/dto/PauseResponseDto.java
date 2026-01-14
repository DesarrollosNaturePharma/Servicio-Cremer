package com.rnp.cremer.dto;

import com.rnp.cremer.model.TipoPausa;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para una pausa.
 *
 * @author RNP Team
 * @version 1.0
 * @since 2024-11-25
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PauseResponseDto {

    private Long idPausa;
    private Long idOrder;
    private String codOrder;
    private TipoPausa tipo;
    private String descripcion;
    private String operario;
    private Boolean computa;
    private LocalDateTime horaInicio;
    private LocalDateTime horaFin;
    private Float tiempoTotalPausa;  // En minutos
    private Boolean activa;  // true si horaFin es null
}