package com.rnp.cremer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para exportación horizontal de órdenes (una fila por orden).
 *
 * @author RNP Team
 * @version 1.0
 * @since 2024-12-03
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderHorizontalExportDto {

    // ===================================
    // DATOS INICIO
    // ===================================
    private String codOrder;
    private String lote;
    private String articulo;
    private String descripcion;
    private Float stdReferencia; // <<-- AÑADIDO
    private Integer cantidad;
    private Integer botesCaja;
    private Float cajasPrevistas;
    private Boolean repercap;

    // ===================================
    // TIEMPOS EJECUCIÓN
    // ===================================
    private Float tiempoActivo;      // minutos
    private Float tiempoPausado;     // minutos
    private Float tiempoTotal;       // minutos

    // ===================================
    // PRODUCCIÓN AUTO + MANUAL
    // ===================================
    private Integer botesBuenos;
    private Integer botesMalos;
    private Integer totalCajas;

    // ===================================
    // OEE
    // ===================================
    private Float disponibilidad;    // 0-1 (convertir a %)
    private Float rendimiento;       // 0-1 (convertir a %)
    private Float calidad;           // 0-1 (convertir a %)
    private Float oee;               // 0-1 (convertir a %)

    // ===================================
    // CÁLCULOS
    // ===================================
    private Float porcentajePausas;  // tiempoPausado / tiempoTotal
    private Float stdReal;           // minutos/unidad
    private Float stdRealVsStdRef;   // stdReal - stdReferencia
    private Float porCumpPedido;     // botesBuenos / cantidad

    // ===================================
    // DATOS EXTRA (info Manual)
    // ===================================
    private String formatoBote;
    private String tipo;
    private Integer udsBote;

    // ===================================
    // FECHAS
    // ===================================
    private LocalDateTime horaInicio;
    private LocalDateTime horaFin;
}