package com.rnp.cremer.service;

import com.rnp.cremer.dto.OrderResponseDto;
import com.rnp.cremer.dto.WebSocketEventDto;
import com.rnp.cremer.model.Order;
import com.rnp.cremer.model.TipoPausa;
import com.rnp.cremer.repository.OrderRepository;
import com.rnp.cremer.repository.PauseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestionar notificaciones de órdenes con fabricación parcial.
 *
 * @author RNP Team
 * @version 1.0
 * @since 2025-11-27
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FabricacionParcialService {

    private final PauseRepository pauseRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Notifica la lista actualizada de órdenes con fabricación parcial.
     */
    public void notifyFabricacionParcialUpdate() {
        log.info("Notificando actualización de órdenes con fabricación parcial");

        // Obtener IDs de órdenes con pausas activas de tipo FABRICACION_PARCIAL
        List<Long> orderIds = pauseRepository.findOrderIdsWithActivePauseType(TipoPausa.FABRICACION_PARCIAL);

        // Buscar las órdenes completas
        List<Order> orders = orderRepository.findAllById(orderIds);

        // Convertir a DTOs
        List<OrderResponseDto> orderDtos = orders.stream()
                .map(orderService::mapToResponseDtoPublic)
                .collect(Collectors.toList());

        // Crear evento WebSocket usando el método customEvent
        WebSocketEventDto<List<OrderResponseDto>> event = WebSocketEventDto.customEvent(
                "FABRICACION_PARCIAL_UPDATE",
                "Lista de órdenes con fabricación parcial actualizada",
                orderDtos
        );

        // Enviar notificación al topic específico
        messagingTemplate.convertAndSend("/topic/fabricacion-parcial", event);

        log.info("Notificación enviada - {} órdenes con fabricación parcial", orderDtos.size());
    }

    /**
     * Verifica si una orden tiene pausa activa de fabricación parcial.
     * 
     * @param idOrder ID de la orden a verificar
     * @return true si tiene fabricación parcial activa, false en caso contrario
     */
    public boolean hasActiveFabricacionParcial(Long idOrder) {
        List<Long> orderIds = pauseRepository.findOrderIdsWithActivePauseType(TipoPausa.FABRICACION_PARCIAL);
        return orderIds.contains(idOrder);
    }
}