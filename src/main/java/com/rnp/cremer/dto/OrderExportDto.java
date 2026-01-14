package com.rnp.cremer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO para exportación completa de una orden con todas sus métricas y pausas.
 *
 * @author RNP Team
 * @version 1.0
 * @since 2024-12-03
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderExportDto {

    // ===================================
    // DATOS BÁSICOS DE LA ORDEN
    // ===================================
    private Long idOrder;
    private String codOrder;
    private String lote;
    private String articulo;
    private String descripcion;
    private Integer cantidad;
    private Integer botesCaja;
    private Float cajasPrevistas;
    private Boolean repercap;
    private LocalDateTime horaInicio;
    private LocalDateTime horaFin;

    // ===================================
    // DATOS DE PRODUCCIÓN
    // ===================================
    private Integer botesBuenos;
    private Integer botesMalos;
    private Integer totalCajasCierre;

    // ===================================
    // MÉTRICAS DE TIEMPO
    // ===================================
    private Float tiempoActivo;      // minutos
    private Float tiempoPausado;     // minutos
    private Float tiempoTotal;       // minutos

    // ===================================
    // MÉTRICAS DE RENDIMIENTO
    // ===================================
    private Float disponibilidad;    // 0-1
    private Float rendimiento;       // 0-1+
    private Float calidad;           // 0-1
    private Float oee;               // 0-1
    private Float porcentajePausas;  // 0-1 (calculado: tiempoPausado / tiempoTotal)

    // ===================================
    // MÉTRICAS DE ESTÁNDARES
    // ===================================
    private Float stdReal;           // minutos/unidad
    private Float stdReferencia;     // minutos/unidad (de Order)
    private Float stdRealVsStdRef;   // diferencia o ratio

    // ===================================
    // DATOS EXTRA
    // ===================================
    private String formatoBote;      // de ExtraData
    private String tipo;             // de ExtraData
    private Integer udsBote;         // de ExtraData

    // ===================================
    // PAUSAS ASOCIADAS
    // ===================================
    private List<PauseExportDto> pausas;

    /**
     * DTO simplificado para exportar información de pausas.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PauseExportDto {
        private Long idPausa;
        private String tipo;
        private String descripcion;
        private String operario;
        private Boolean computa;
        private LocalDateTime horaInicio;
        private LocalDateTime horaFin;
        private Float tiempoTotalPausa;  // minutos
    }
}