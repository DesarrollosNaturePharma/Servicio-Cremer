package com.rnp.cremer.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad que representa las métricas de producción de una orden.
 *
 * @author RNP Team
 * @version 1.0
 * @since 2024-11-26
 */
@Entity
@Table(name = "metricas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Metricas {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_metricas")
    private Long idMetricas;

    /**
     * ID de la orden asociada.
     */
    @Column(name = "id_order", nullable = false, unique = true)
    private Long idOrder;

    /**
     * Tiempo total en minutos (hora inicio - hora fin).
     */
    @Column(name = "tiempo_total")
    private Float tiempoTotal;

    /**
     * Tiempo pausado en minutos (suma de pausas computables).
     */
    @Column(name = "tiempo_pausado")
    private Float tiempoPausado;

    /**
     * Tiempo activo en minutos (tiempo total - tiempo pausado).
     */
    @Column(name = "tiempo_activo")
    private Float tiempoActivo;

    /**
     * Disponibilidad (0-1): tiempo activo / tiempo total.
     */
    @Column(name = "disponibilidad")
    private Float disponibilidad;

    /**
     * Rendimiento (0-1+): producción real / producción esperada.
     */
    @Column(name = "rendimiento")
    private Float rendimiento;

    /**
     * Calidad (0-1): botes buenos / total producido.
     */
    @Column(name = "calidad")
    private Float calidad;

    /**
     * OEE (0-1): disponibilidad × rendimiento × calidad.
     */
    @Column(name = "oee")
    private Float oee;

    /**
     * STD real (minutos/unidad): tiempo activo / total producido.
     */
    @Column(name = "std_real")
    private Float stdReal;

    /**
     * Porcentaje de cumplimiento del pedido (0-1+): botes buenos / cantidad solicitada.
     * <p>Indica qué porcentaje del pedido original se completó exitosamente.</p>
     * <p>Valores > 1.0 indican sobreproducción.</p>
     * <p><b>Ejemplo:</b> 0.95 = 95% del pedido completado, 1.05 = 5% de sobreproducción</p>
     */
    @Column(name = "por_cump_pedido")
    private Float porCumpPedido;
}