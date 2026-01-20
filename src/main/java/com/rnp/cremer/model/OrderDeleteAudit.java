package com.rnp.cremer.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad que registra la auditoría de eliminación de órdenes.
 *
 * <p>Cada vez que se elimina una orden, se crea un registro con información
 * completa sobre quién realizó la eliminación, cuándo y qué datos contenía
 * la orden eliminada.</p>
 *
 * @author RNP Team
 * @version 1.0
 * @since 2024-11-26
 */
@Entity
@Table(name = "order_delete_audit")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDeleteAudit {

    /**
     * Identificador único del registro de auditoría.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_audit")
    private Long idAudit;

    /**
     * ID original de la orden eliminada.
     */
    @Column(name = "id_order_deleted", nullable = false)
    private Long idOrderDeleted;

    /**
     * Código de la orden eliminada.
     */
    @Column(name = "cod_order", nullable = false, length = 50)
    private String codOrder;

    /**
     * Operario asignado a la orden eliminada.
     */
    @Column(name = "operario", length = 100)
    private String operario;

    /**
     * Lote de la orden eliminada.
     */
    @Column(name = "lote", length = 50)
    private String lote;

    /**
     * Artículo de la orden eliminada.
     */
    @Column(name = "articulo", length = 100)
    private String articulo;

    /**
     * Estado de la orden al momento de la eliminación.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_al_eliminar", nullable = false, length = 20)
    private EstadoOrder estadoAlEliminar;

    /**
     * Fecha de creación original de la orden.
     */
    @Column(name = "fecha_creacion_order")
    private LocalDateTime fechaCreacionOrder;

    /**
     * Usuario que realizó la eliminación.
     */
    @Column(name = "deleted_by", nullable = false, length = 100)
    private String deletedBy;

    /**
     * Motivo de la eliminación.
     */
    @Column(name = "motivo", length = 500)
    private String motivo;

    /**
     * Fecha y hora de la eliminación.
     */
    @CreationTimestamp
    @Column(name = "deleted_at", nullable = false, updatable = false)
    private LocalDateTime deletedAt;

    /**
     * Dirección IP desde donde se realizó la eliminación (opcional).
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * Cantidad de la orden eliminada.
     */
    @Column(name = "cantidad")
    private Integer cantidad;

    /**
     * Botes buenos al momento de eliminar.
     */
    @Column(name = "botes_buenos")
    private Integer botesBuenos;

    /**
     * Botes malos al momento de eliminar.
     */
    @Column(name = "botes_malos")
    private Integer botesMalos;
}
