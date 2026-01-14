package com.rnp.cremer.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidad que representa datos acumulados del proceso manual de una orden.
 *
 * <p>Almacena información sobre el tiempo y cantidad de cajas procesadas
 * manualmente después de que la orden finaliza su proceso automático.</p>
 *
 * <h3>Relaciones:</h3>
 * <ul>
 *   <li>Una orden tiene un registro de acumulación (1:1)</li>
 * </ul>
 *
 * @author RNP Team
 * @version 1.0
 * @since 2024-11-25
 * @see Order
 */
@Entity
@Table(name = "acumula")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Acumula {

    /**
     * Identificador único del registro de acumulación.
     * <p>Generado automáticamente por la base de datos como auto-increment.</p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_acumula")
    private Long idAcumula;

    /**
     * Identificador de la orden asociada.
     * <p>Clave foránea que relaciona este registro con una orden específica.
     * Debe ser única ya que existe una relación 1:1.</p>
     */
    @NotNull(message = "La orden es obligatoria")
    @Column(name = "id_order", nullable = false, unique = true)
    private Long idOrder;

    /**
     * Fecha y hora de inicio del proceso manual.
     * <p>Marca cuando el operario comienza el trabajo manual sobre la orden.</p>
     */
    @NotNull(message = "La hora de inicio es obligatoria")
    @Column(name = "hora_inicio", nullable = false)
    private LocalDateTime horaInicio;

    /**
     * Fecha y hora de fin del proceso manual.
     * <p>Marca cuando el operario completa el trabajo manual.
     * Permanece NULL mientras el proceso manual está en curso.</p>
     */
    @Column(name = "hora_fin")
    private LocalDateTime horaFin;

    /**
     * Tiempo total del proceso manual en minutos.
     * <p>Calculado como: (horaFin - horaInicio) en minutos.
     * Se calcula automáticamente cuando se establece horaFin.</p>
     * <p>NULL mientras el proceso está activo (horaFin es NULL).</p>
     */
    @Column(name = "tiempo_total")
    private Float tiempoTotal;

    /**
     * Número de cajas procesadas manualmente.
     * <p>Cantidad de cajas que han pasado por el proceso manual
     * (etiquetado, inspección, retrabajo, etc.).</p>
     * <p>Valor inicial: 0</p>
     */
    @Min(value = 0, message = "El número de cajas no puede ser negativo")
    @Column(name = "num_cajas_manual", nullable = false)
    private Integer numCajasManual = 0;
}