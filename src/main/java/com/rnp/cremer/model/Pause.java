package com.rnp.cremer.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidad que representa una pausa en una orden de producción.
 *
 * @author RNP Team
 * @version 1.0
 * @since 2024-11-25
 */
@Entity
@Table(name = "pauses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pause {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pausa")
    private Long idPausa;

    /**
     * ID de la orden asociada.
     */
    @Column(name = "id_order", nullable = false)
    private Long idOrder;

    /**
     * Tipo de pausa.
     * PUEDE SER NULL al crear (Modo 2), se asigna al finalizar.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", length = 50)  // ⬅️ QUITAR nullable = false
    private TipoPausa tipo;

    /**
     * Descripción o motivo de la pausa (opcional).
     */
    @Column(name = "descripcion", length = 500)
    private String descripcion;

    /**
     * Operario que registra la pausa.
     * PUEDE SER NULL al crear (Modo 2), se asigna al finalizar.
     */
    @Column(name = "operario", length = 100)  // ⬅️ QUITAR nullable = false
    private String operario;

    /**
     * Indica si la pausa computa en las métricas de producción.
     * PUEDE SER NULL al crear (Modo 2), se calcula al finalizar según el tipo.
     */
    @Column(name = "computa")  // ⬅️ QUITAR nullable = false
    private Boolean computa;

    /**
     * Hora de inicio de la pausa.
     */
    @Column(name = "hora_inicio", nullable = false)
    private LocalDateTime horaInicio;

    /**
     * Hora de fin de la pausa (null si está activa).
     */
    @Column(name = "hora_fin")
    private LocalDateTime horaFin;

    /**
     * Tiempo total de la pausa en minutos (calculado al finalizar).
     */
    @Column(name = "tiempo_total_pausa")
    private Float tiempoTotalPausa;
}