package com.rnp.cremer.model;

/**
 * Enumeración que representa los tipos de pausas que pueden ocurrir durante una orden de producción.
 */
public enum TipoPausa {

    // ============================================
    // INCIDENCIAS DE MÁQUINAS
    // ============================================
    INCIDENCIA_MAQUINA_CONTADORA,
    INCIDENCIA_MAQUINA_PESADORA,
    INCIDENCIA_MAQUINA_ETIQUETADORA,
    INCIDENCIA_MAQUINA_REPERCAP,
    INCIDENCIA_MAQUINA_TAPONADORA,
    INCIDENCIA_MAQUINA_POSICIONADORA,
    INCIDENCIA_MAQUINA_ENVASADORA,
    INCIDENCIA_MAQUINA_OTROS,

    // ============================================
    // PROBLEMAS DE MATERIAL
    // ============================================
    FALTA_MATERIAL,
    MATERIAL_DEFECTUOSO,

    // ============================================
    // MANTENIMIENTO Y LIMPIEZA
    // ============================================
    MANTENIMIENTO_EN_PROCESO,
    LIMPIEZA_EN_PROCESO,

    // ============================================
    // CALIDAD
    // ============================================
    PARADA_CALIDAD,

    // ============================================
    // PAUSAS NO COMPUTABLES
    // ============================================
    CAMBIO_TURNO,
    FABRICACION_PARCIAL,
    PARADA;

    // ============================================
    // MÉTODOS DE UTILIDAD
    // ============================================

    /**
     * Determina si este tipo de pausa computa en las métricas de productividad.
     */
    public boolean computa() {
        return this != CAMBIO_TURNO &&
                this != FABRICACION_PARCIAL &&
                this != PARADA;
    }

    /**
     * Indica si es una incidencia directamente relacionada con la máquina.
     */
    public boolean esIncidenciaMaquina() {
        return this.name().startsWith("INCIDENCIA_MAQUINA_");
    }

    /**
     * Indica si está relacionada con problemas de material.
     */
    public boolean esProblemaMaterial() {
        return this == FALTA_MATERIAL ||
                this == MATERIAL_DEFECTUOSO;
    }

    /**
     * Pausa operativa que NO computa.
     */
    public boolean esPausaOperativa() {
        return !computa();
    }
}
