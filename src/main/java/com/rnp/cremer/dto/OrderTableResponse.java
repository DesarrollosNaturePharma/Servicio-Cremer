package com.rnp.cremer.dto;

import com.rnp.cremer.model.EstadoOrder;

import java.time.LocalDateTime;

public class OrderTableResponse {

    // -- ORDER --
    private Long idOrder;
    private String codOrder;
    private String descripcion;
    private Integer cantidad;
    private EstadoOrder estado;
    private LocalDateTime horaInicio;
    private LocalDateTime horaFin;

    // -- METRICAS --
    private Float oee;
    private Float porCumpPedido;

    public OrderTableResponse() {}

    public OrderTableResponse(Long idOrder, String codOrder, String descripcion, Integer cantidad,
                              EstadoOrder estado, LocalDateTime horaInicio, LocalDateTime horaFin,
                              Float oee, Float porCumpPedido) {

        this.idOrder = idOrder;
        this.codOrder = codOrder;
        this.descripcion = descripcion;
        this.cantidad = cantidad;
        this.estado = estado;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.oee = oee;
        this.porCumpPedido = porCumpPedido;
    }

    // getters & setters...

    public Long getIdOrder() { return idOrder; }
    public void setIdOrder(Long idOrder) { this.idOrder = idOrder; }

    public String getCodOrder() { return codOrder; }
    public void setCodOrder(String codOrder) { this.codOrder = codOrder; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }

    public EstadoOrder getEstado() { return estado; }
    public void setEstado(EstadoOrder estado) { this.estado = estado; }

    public LocalDateTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalDateTime horaInicio) { this.horaInicio = horaInicio; }

    public LocalDateTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalDateTime horaFin) { this.horaFin = horaFin; }

    public Float getOee() { return oee; }
    public void setOee(Float oee) { this.oee = oee; }

    public Float getPorCumpPedido() { return porCumpPedido; }
    public void setPorCumpPedido(Float porCumpPedido) { this.porCumpPedido = porCumpPedido; }
}
