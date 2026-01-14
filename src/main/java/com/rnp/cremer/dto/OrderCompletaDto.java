package com.rnp.cremer.dto;

import com.rnp.cremer.model.ExtraData;
import com.rnp.cremer.model.Metricas;
import com.rnp.cremer.model.Order;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;


@Data

@Builder
public class OrderCompletaDto {

    private Order order;
    private Metricas metricas;
    private ExtraData extraData;

    public OrderCompletaDto() {}

    public OrderCompletaDto(Order order, Metricas metricas, ExtraData extraData) {
        this.order = order;
        this.metricas = metricas;
        this.extraData = extraData;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Metricas getMetricas() {
        return metricas;
    }

    public void setMetricas(Metricas metricas) {
        this.metricas = metricas;
    }

    public ExtraData getExtraData() {
        return extraData;
    }

    public void setExtraData(ExtraData extraData) {
        this.extraData = extraData;
    }
}