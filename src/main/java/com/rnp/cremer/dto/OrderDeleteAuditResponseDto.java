package com.rnp.cremer.dto;

import com.rnp.cremer.model.EstadoOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta con información del registro de auditoría de eliminación.
 *
 * @author RNP Team
 * @version 1.0
 * @since 2024-11-26
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDeleteAuditResponseDto {

    /**
     * ID del registro de auditoría.
     */
    private Long idAudit;

    /**
     * ID original de la orden eliminada.
     */
    private Long idOrderDeleted;

    /**
     * Código de la orden eliminada.
     */
    private String codOrder;

    /**
     * Operario de la orden eliminada.
     */
    private String operario;

    /**
     * Lote de la orden eliminada.
     */
    private String lote;

    /**
     * Artículo de la orden eliminada.
     */
    private String articulo;

    /**
     * Estado de la orden al momento de eliminar.
     */
    private EstadoOrder estadoAlEliminar;

    /**
     * Fecha de creación original de la orden.
     */
    private LocalDateTime fechaCreacionOrder;

    /**
     * Cantidad de la orden.
     */
    private Integer cantidad;

    /**
     * Botes buenos al eliminar.
     */
    private Integer botesBuenos;

    /**
     * Botes malos al eliminar.
     */
    private Integer botesMalos;

    /**
     * Usuario que eliminó la orden.
     */
    private String deletedBy;

    /**
     * Motivo de la eliminación.
     */
    private String motivo;

    /**
     * Fecha y hora de la eliminación.
     */
    private LocalDateTime deletedAt;

    /**
     * IP desde donde se realizó la eliminación.
     */
    private String ipAddress;
}
