package com.rnp.cremer.model;

/**
 * Enumeración que representa los estados del ciclo de vida de una orden de producción.
 *
 * <p>Una orden pasa por diferentes estados desde su creación hasta su finalización,
 * pudiendo requerir o no intervención manual según el tipo de producción.</p>
 *
 * <h3>Flujo de Estados:</h3>
 * <pre>
 * CREADA → EN_PROCESO ↔ PAUSADA
 *              ↓
 *         FINALIZADA (si no requiere manual)
 *              ó
 *         ESPERA_MANUAL → PROCESO_MANUAL → FINALIZADA (si requiere manual)
 * </pre>
 *
 * @author RNP Team
 * @version 1.0
 * @since 2024-11-25
 */
public enum EstadoOrder {

    /**
     * Estado inicial de una orden.
     * <p>La orden ha sido creada en el sistema pero aún no ha comenzado la producción.</p>
     * <p><b>Transiciones válidas:</b> EN_PROCESO</p>
     */
    CREADA,

    /**
     * La orden está en proceso de producción automática.
     * <p>La máquina está trabajando activamente en la producción de la orden.</p>
     * <p><b>Transiciones válidas:</b> PAUSADA, FINALIZADA, ESPERA_MANUAL</p>
     */
    EN_PROCESO,

    /**
     * La orden ha sido pausada temporalmente.
     * <p>Utilizado para descansos, cambios de turno, ajustes menores, etc.</p>
     * <p><b>Transiciones válidas:</b> EN_PROCESO</p>
     */
    PAUSADA,

    /**
     * La orden ha sido completada en su totalidad.
     * <p>Estado final del ciclo de vida de una orden. No permite más transiciones.</p>
     * <p><b>Transiciones válidas:</b> Ninguna (estado terminal)</p>
     */
    FINALIZADA,

    /**
     * La producción automática ha finalizado y la orden espera proceso manual.
     * <p>Utilizado cuando la orden requiere intervención humana adicional
     * (etiquetado, inspección, retrabajo, etc.) después del proceso automático.</p>
     * <p><b>Transiciones válidas:</b> PROCESO_MANUAL</p>
     */
    ESPERA_MANUAL,

    /**
     * Un operario está realizando trabajo manual en la orden.
     * <p>Indica que se está llevando a cabo un proceso manual activo
     * (etiquetado manual, control de calidad, ajustes, etc.).</p>
     * <p><b>Transiciones válidas:</b> FINALIZADA</p>
     */
    PROCESO_MANUAL
}