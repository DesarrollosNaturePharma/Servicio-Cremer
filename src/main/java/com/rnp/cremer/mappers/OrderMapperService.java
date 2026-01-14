package com.rnp.cremer.mappers;

import com.rnp.cremer.dto.OrderResponseDto;
import com.rnp.cremer.model.ExtraData;
import com.rnp.cremer.model.Order;
import com.rnp.cremer.repository.ExtraDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio dedicado a mapear entidades Order a DTOs.
 * Evita dependencias circulares.
 */
@Service
@RequiredArgsConstructor
public class OrderMapperService {

    private final ExtraDataRepository extraDataRepository;

    /**
     * Convierte una entidad Order a DTO cargando ExtraData si existe.
     */
    @Transactional(readOnly = true)
    public OrderResponseDto mapToResponseDto(Order order) {
        ExtraData extraData = extraDataRepository.findByIdOrder(order.getIdOrder())
                .orElse(null);

        return OrderResponseDto.builder()
                .idOrder(order.getIdOrder())
                .operario(order.getOperario())
                .codOrder(order.getCodOrder())
                .lote(order.getLote())
                .articulo(order.getArticulo())
                .descripcion(order.getDescripcion())
                .cantidad(order.getCantidad())
                .botesCaja(order.getBotesCaja())
                .stdReferencia(order.getStdReferencia())
                .estado(order.getEstado())
                .horaCreacion(order.getHoraCreacion())
                .horaInicio(order.getHoraInicio())
                .horaFin(order.getHoraFin())
                .cajasPrevistas(order.getCajasPrevistas())
                .tiempoEstimado(order.getTiempoEstimado())
                .botesBuenos(order.getBotesBuenos())
                .botesMalos(order.getBotesMalos())
                .totalCajasCierre(order.getTotalCajasCierre())
                .acumula(order.getAcumula())
                .formatoBote(extraData != null ? extraData.getFormatoBote() : null)
                .tipo(extraData != null ? extraData.getTipo() : null)
                .udsBote(extraData != null ? extraData.getUdsBote() : null)
                .build();
    }
}