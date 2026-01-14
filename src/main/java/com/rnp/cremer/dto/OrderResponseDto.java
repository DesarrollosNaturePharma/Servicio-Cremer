package com.rnp.cremer.dto;

import com.rnp.cremer.model.EstadoOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para una orden de producción.
 *
 * @author RNP Team
 * @version 1.0
 * @since 2024-11-25
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponseDto {

    private Long idOrder;
    private String operario;
    private String codOrder;
    private String lote;
    private String articulo;
    private String descripcion;
    private Integer cantidad;
    private Integer botesCaja;
    private Float stdReferencia;
    private EstadoOrder estado;
    private LocalDateTime horaCreacion;
    private LocalDateTime horaInicio;
    private LocalDateTime horaFin;
    private Float cajasPrevistas;
    private Float tiempoEstimado;
    private String formatoBote;
    private String tipo;
    private Integer udsBote;
    private Integer botesBuenos;
    private Integer botesMalos;
    private Integer totalCajasCierre;
    private Boolean acumula;
}