package com.rnp.cremer.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad que representa datos adicionales de una orden.
 *
 * <p>Almacena información complementaria que no forma parte
 * de la orden principal:</p>
 * <ul>
 *   <li>Formato del bote (ej: "500ml")</li>
 *   <li>Tipo de producto (ej: "Conserva")</li>
 *   <li>Unidades por bote</li>
 *   <li>Referencia a métricas (opcional, se gestiona después)</li>
 * </ul>
 *
 * @author RNP Team
 * @version 1.0
 * @since 2024-11-25
 */
@Entity
@Table(name = "extra_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExtraData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_extra_data")
    private Long idExtraData;

    /**
     * ID de la orden asociada.
     */
    @Column(name = "id_order", nullable = false)
    private Long idOrder;

    /**
     * Formato del bote (ej: "500ml", "1L", "250ml").
     */
    @Column(name = "formato_bote", length = 100)
    private String formatoBote;

    /**
     * Tipo de producto (ej: "Conserva", "Mermelada", "Salsa").
     */
    @Column(name = "tipo", length = 100)
    private String tipo;

    /**
     * Unidades por bote.
     */
    @Column(name = "uds_bote")
    private Integer udsBote;

    /**
     * ID de métricas asociadas (opcional, se gestiona posteriormente).
     * PUEDE SER NULL - Las métricas se calculan/asignan después.
     */
    @Column(name = "id_metricas")  // ⬅️ QUITAR nullable = false y @NotNull
    private Long idMetricas;
}