package com.rnp.cremer.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad que representa el contador de botellas para una orden.
 *
 * <p>Cada orden tiene un único registro de conteo que se actualiza incrementalmente.
 * El contador persiste el valor final cuando la orden se completa.</p>
 *
 * <h3>Características:</h3>
 * <ul>
 *   <li>Relación 1:1 con Order (idOrder UNIQUE)</li>
 *   <li>Contador incremental (no se crean registros duplicados)</li>
 *   <li>Actualización en tiempo real desde GPIO</li>
 * </ul>
 *
 * @author RNP Team
 * @version 1.0
 * @since 2024-12-12
 */
@Entity
@Table(name = "bottle_counter", indexes = {
        @Index(name = "idx_id_order", columnList = "id_order", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BottleCounter {

    /**
     * Identificador único del contador.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * ID de la orden asociada (relación 1:1).
     * Cada orden solo puede tener un contador.
     */
    @Column(name = "id_order", nullable = false, unique = true)
    private Long idOrder;

    /**
     * Cantidad actual de botellas contadas.
     * Se incrementa con cada señal del sensor GPIO.
     */
    @Column(name = "quantity", nullable = false)
    @Builder.Default
    private Integer quantity = 0;

    /**
     * Indica si el contador está activo (recibiendo señales).
     * Se activa cuando la orden está EN_PROCESO o PAUSADA (sin fabricación parcial).
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = false;

    /**
     * Fecha y hora de creación del registro.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Fecha y hora de la última actualización.
     * Se actualiza automáticamente con cada incremento.
     */
    @UpdateTimestamp
    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    /**
     * Fecha y hora del último conteo de botella.
     * Diferente de lastUpdated: este solo cambia cuando se cuenta una botella.
     */
    @Column(name = "last_bottle_counted_at")
    private LocalDateTime lastBottleCountedAt;

    /**
     * Incrementa el contador en 1 y actualiza el timestamp.
     */
    public void increment() {
        this.quantity++;
        this.lastBottleCountedAt = LocalDateTime.now();
    }

    /**
     * Resetea el contador a 0 (para nueva orden).
     */
    public void reset() {
        this.quantity = 0;
        this.lastBottleCountedAt = null;
    }
}