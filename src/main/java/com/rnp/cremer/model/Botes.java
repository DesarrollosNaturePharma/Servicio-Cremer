package com.rnp.cremer.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad que representa el contador acumulado de botes de una orden.
 *
 * <p>Esta tabla mantiene UN SOLO registro por orden, con un contador que se incrementa
 * cada vez que el sensor detecta un bote. El sensor genera pulsos que actualizan
 * el campo cantidad de forma incremental (1+1+1...).</p>
 *
 * <h3>Funcionamiento:</h3>
 * <ul>
 *   <li>Se crea un registro cuando inicia la orden (cantidad = 0)</li>
 *   <li>Cada pulso del sensor incrementa cantidad en 1</li>
 *   <li>El campo ultima_actualizacion se actualiza con cada pulso</li>
 *   <li>Un solo registro por orden (relación 1:1)</li>
 * </ul>
 *
 * <h3>Ejemplo de actualizaciones:</h3>
 * <pre>
 * Inicial:  idOrder=1, cantidad=0
 * Pulso 1:  idOrder=1, cantidad=1  (0+1)
 * Pulso 2:  idOrder=1, cantidad=2  (1+1)
 * Pulso 3:  idOrder=1, cantidad=3  (2+1)
 * ...
 * Pulso N:  idOrder=1, cantidad=N
 * </pre>
 *
 * <h3>Relaciones:</h3>
 * <ul>
 *   <li>Una orden tiene un contador de botes (relación 1:1)</li>
 * </ul>
 *
 * @author RNP Team
 * @version 1.0
 * @since 2024-11-25
 * @see Order
 */
@Entity
@Table(name = "botes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Botes {

    /**
     * Identificador único del contador.
     * <p>Generado automáticamente por la base de datos como auto-increment.</p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_botes")
    private Long idBotes;

    /**
     * Identificador de la orden asociada.
     * <p>Clave foránea única que relaciona este contador con una orden específica.
     * Debe ser única ya que existe una relación 1:1.</p>
     */
    @NotNull(message = "La orden es obligatoria")
    @Column(name = "id_order", nullable = false, unique = true)
    private Long idOrder;

    /**
     * Contador acumulado de botes detectados.
     * <p>Se incrementa en 1 con cada pulso del sensor.</p>
     * <p><b>Comportamiento:</b></p>
     * <ul>
     *   <li>Valor inicial: 0 (al crear la orden)</li>
     *   <li>Actualización: cantidad = cantidad + 1</li>
     *   <li>Refleja el total de botes contados en tiempo real</li>
     * </ul>
     */
    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 0, message = "La cantidad no puede ser negativa")
    @Column(nullable = false)
    private Integer cantidad = 0;

    /**
     * Fecha y hora de la última actualización del contador.
     * <p>Se actualiza automáticamente cada vez que el sensor incrementa la cantidad.
     * Permite monitorear el último pulso recibido y detectar inactividad.</p>
     */
    @UpdateTimestamp
    @Column(name = "ultima_actualizacion", nullable = false)
    private LocalDateTime ultimaActualizacion;
}