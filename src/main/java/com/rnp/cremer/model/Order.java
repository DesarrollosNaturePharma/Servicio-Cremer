package com.rnp.cremer.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad que representa una orden de producción en el sistema de gestión de manufactura.
 *
 * <p>Una orden contiene toda la información necesaria para el proceso de producción,
 * incluyendo datos del operario, artículo, cantidades, tiempos y métricas de calidad.</p>
 *
 * <h3>Ciclo de Vida:</h3>
 * <ul>
 *   <li>Creación: La orden se crea con estado CREADA</li>
 *   <li>Inicio: Cambia a EN_PROCESO cuando el operario inicia la producción</li>
 *   <li>Producción: Puede pausarse (PAUSADA) y reanudarse múltiples veces</li>
 *   <li>Finalización: Puede ir directo a FINALIZADA o pasar por proceso manual</li>
 * </ul>
 *
 * @author RNP Team
 * @version 1.0
 * @since 2024-11-25
 * @see EstadoOrder
 */
@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    /**
     * Identificador único de la orden.
     * <p>Generado automáticamente por la base de datos como auto-increment.</p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_order")
    private Long idOrder;

    /**
     * Fecha y hora de creación de la orden.
     * <p>Se establece automáticamente al momento de persistir la entidad.
     * Este campo es inmutable una vez creado.</p>
     */
    @CreationTimestamp
    @Column(name = "hora_creacion", nullable = false, updatable = false)
    private LocalDateTime horaCreacion;

    /**
     * Fecha y hora de inicio de la producción.
     * <p>Se establece cuando la orden cambia de estado CREADA a EN_PROCESO.
     * Permanece null hasta que el operario inicie la orden.</p>
     */
    @Column(name = "hora_inicio")
    private LocalDateTime horaInicio;

    /**
     * Fecha y hora de finalización de la producción.
     * <p>Se establece cuando la orden cambia de estado a FINALIZADA.</p>
     */
    @Column(name = "hora_fin")
    private LocalDateTime horaFin;

    /**
     * Nombre o identificador del operario asignado a la orden.
     * <p>Campo obligatorio que indica quién es responsable de la producción.</p>
     */
    @NotBlank(message = "El operario es obligatorio")
    @Column(nullable = false, length = 100)
    private String operario;

    /**
     * Código único identificador de la orden.
     * <p>Este código debe ser único en todo el sistema y sirve como
     * identificador de negocio (diferente del ID técnico).</p>
     *
     * <p><b>Ejemplo:</b> "OF-2024-001", "ORD-202411-0123"</p>
     */
    @NotBlank(message = "El código de orden es obligatorio")
    @Column(name = "cod_order", unique = true, nullable = false, length = 50)
    private String codOrder;

    /**
     * Número de lote de producción.
     * <p>Identifica el lote al que pertenece esta orden para trazabilidad.</p>
     *
     * <p><b>Ejemplo:</b> "LOTE-A-123", "L20241125-01"</p>
     */
    @NotBlank(message = "El lote es obligatorio")
    @Column(nullable = false, length = 50)
    private String lote;

    /**
     * Código o nombre del artículo a producir.
     * <p>Identifica el tipo de producto que se está fabricando.</p>
     *
     * <p><b>Ejemplo:</b> "ART-500", "BOTE-1L-BLANCO"</p>
     */
    @NotBlank(message = "El artículo es obligatorio")
    @Column(nullable = false, length = 100)
    private String articulo;

    /**
     * Descripción detallada del artículo o especificaciones de la orden.
     * <p>Campo opcional para información adicional sobre el producto o proceso.</p>
     *
     * <p><b>Ejemplo:</b> "Botes de conserva 500ml con tapa roja"</p>
     */
    @Column(length = 255)
    private String descripcion;

    /**
     * Estado actual de la orden en su ciclo de vida.
     * <p>Controla el flujo de trabajo y determina las acciones disponibles.</p>
     *
     * @see EstadoOrder
     */
    @NotNull(message = "El estado es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoOrder estado;

    /**
     * Cantidad total de unidades (botes) a producir en esta orden.
     * <p>Representa el objetivo de producción. Debe ser mayor o igual a cero.</p>
     */
    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 0, message = "La cantidad no puede ser negativa")
    @Column(nullable = false)
    private Integer cantidad;

    /**
     * Número de botes que caben en cada caja.
     * <p>Utilizado para calcular el número de cajas previstas y producidas.
     * Debe ser al menos 1.</p>
     *
     * <p><b>Ejemplo:</b> Si botesCaja = 24, una caja contiene 24 botes.</p>
     */
    @NotNull(message = "Los botes por caja son obligatorios")
    @Min(value = 1, message = "Debe haber al menos 1 bote por caja")
    @Column(name = "botes_caja", nullable = false)
    private Integer botesCaja;

    /**
     * Indicador de repercap (reproceso de tapas).
     * <p>true: La orden incluye reproceso de tapas defectuosas</p>
     * <p>false: Producción normal sin reproceso</p>
     * <p>Valor por defecto: false</p>
     */
    @Column(nullable = false)
    private Boolean repercap = false;

    /**
     * Cantidad de botes producidos correctamente (sin defectos).
     * <p>Se incrementa durante el proceso de producción.
     * Utilizado para calcular rendimiento y eficiencia.</p>
     * <p>Campo nullable - se actualiza durante la producción.</p>
     */
    @Column(name = "botes_buenos")
    private Integer botesBuenos;

    /**
     * Cantidad de botes defectuosos o rechazados.
     * <p>Se incrementa cuando se detectan botes con problemas de calidad.
     * Utilizado para métricas de calidad y análisis de mejora.</p>
     * <p>Campo nullable - se actualiza durante la producción.</p>
     */
    @Column(name = "botes_malos")
    private Integer botesMalos;

    /**
     * Número estimado de cajas que se producirán.
     * <p>Calculado típicamente como: cantidad / botesCaja</p>
     * <p>Puede ser establecido manualmente o calculado automáticamente.</p>
     *
     * <p><b>Ejemplo:</b> Si cantidad=1000 y botesCaja=24, cajasPrevistas ≈ 41.67</p>
     */
    @Column(name = "cajas_previstas", nullable = false)
    private Float cajasPrevistas;

    /**
     * Total de cajas realmente producidas al cierre de la orden.
     * <p>Se calcula al finalizar la orden como: botesBuenos / botesCaja</p>
     * <p>Solo tiene valor cuando la orden está en estado FINALIZADA.</p>
     */
    @Column(name = "total_cajas_cierre")
    private Integer totalCajasCierre;

    /**
     * Indicador de acumulación de producción.
     * <p>Puede indicar si la orden acumula datos de múltiples procesos
     * o si requiere proceso manual posterior.</p>
     * <p>Valor por defecto: false</p>
     *
     * <p><b>Nota:</b> El uso exacto de este campo se define en la lógica de negocio.</p>
     */
    @Column(nullable = true)
    private Boolean acumula = false;

    /**
     * Estándar de referencia para la producción.
     * <p>Valor de referencia utilizado para comparar el rendimiento real
     * con el esperado. La unidad depende del contexto del negocio.</p>
     *
     * <p><b>Ejemplo:</b> Tiempo estándar por unidad, tasa de producción esperada, etc.</p>
     */
    @Column(name = "std_referencia", nullable = false)
    private Float stdReferencia;

    /**
     * Tiempo estimado para completar la orden (en minutos).
     * <p>Utilizado para planificación y programación de producción.
     * Puede ser calculado automáticamente o ingresado manualmente.</p>
     *
     * <p><b>Ejemplo:</b> 120.0 = 2 horas, 90.5 = 1 hora y 30 minutos</p>
     */
    @Column(name = "tiempo_estimado", nullable = false)
    private Float tiempoEstimado;
}