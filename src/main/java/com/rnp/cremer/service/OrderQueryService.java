package com.rnp.cremer.service;

import com.rnp.cremer.dto.OrderResponseDto;
import com.rnp.cremer.dto.WebSocketEventDto;
import com.rnp.cremer.mappers.OrderMapperService;
import com.rnp.cremer.model.EstadoOrder;
import com.rnp.cremer.model.Order;
import com.rnp.cremer.model.Pause;
import com.rnp.cremer.model.TipoPausa;
import com.rnp.cremer.repository.OrderRepository;
import com.rnp.cremer.repository.PauseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderQueryService {

    private final OrderRepository orderRepository;
    private final PauseRepository pauseRepository;
    private final OrderMapperService orderMapperService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Obtiene la orden activa visible como DTO (para frontend).
     * Excluye órdenes en fabricación parcial.
     */
    @Transactional(readOnly = true)
    public OrderResponseDto getActiveVisibleOrder() {
        log.info("Obteniendo orden activa visible (excluyendo fabricación parcial)");

        Optional<Order> orderOpt = getActiveVisibleOrderEntity();

        return orderOpt
                .map(orderMapperService::mapToResponseDto)
                .orElse(null);
    }

    /**
     * ✅ NUEVO: Obtiene la entidad Order activa visible (para uso interno).
     * Excluye órdenes en fabricación parcial.
     *
     * @return Optional con la orden activa o vacío si no hay ninguna
     */
    @Transactional(readOnly = true)
    public Optional<Order> getActiveVisibleOrderEntity() {
        log.debug("Obteniendo entidad Order activa visible");

        List<Order> activeOrders = orderRepository.findByEstadoIn(
                List.of(EstadoOrder.EN_PROCESO, EstadoOrder.PAUSADA)
        );

        if (activeOrders.isEmpty()) {
            log.debug("No hay órdenes activas");
            return Optional.empty();
        }

        // Ordenar por hora de inicio (más reciente primero)
        activeOrders.sort((o1, o2) -> {
            if (o1.getHoraInicio() == null) return 1;
            if (o2.getHoraInicio() == null) return -1;
            return o2.getHoraInicio().compareTo(o1.getHoraInicio());
        });

        for (Order order : activeOrders) {
            // Caso 1: Orden EN_PROCESO (siempre visible)
            if (order.getEstado() == EstadoOrder.EN_PROCESO) {
                log.debug("Orden activa visible encontrada: {} (EN_PROCESO)", order.getCodOrder());
                return Optional.of(order);
            }

            // Caso 2: Orden PAUSADA (verificar tipo de pausa)
            if (order.getEstado() == EstadoOrder.PAUSADA) {
                Optional<Pause> pausaActivaOpt = pauseRepository.findActivePauseByOrder(order.getIdOrder());

                if (pausaActivaOpt.isEmpty()) {
                    log.warn("Orden {} está PAUSADA pero no tiene pausa activa", order.getCodOrder());
                    return Optional.of(order);
                }

                Pause pausaActiva = pausaActivaOpt.get();

                // Si es FABRICACION_PARCIAL, no es visible
                if (pausaActiva.getTipo() == TipoPausa.FABRICACION_PARCIAL) {
                    log.debug("Orden {} pausada con FABRICACION_PARCIAL - NO visible", order.getCodOrder());
                    continue;
                }

                // Cualquier otro tipo de pausa es visible
                log.debug("Orden activa visible encontrada: {} (PAUSADA - {})",
                        order.getCodOrder(), pausaActiva.getTipo());
                return Optional.of(order);
            }
        }

        log.debug("No hay órdenes visibles (todas están en fabricación parcial)");
        return Optional.empty();
    }

    /**
     * Notifica cambios en la orden activa visible vía WebSocket.
     */
    public void notifyActiveVisibleOrderChange() {
        OrderResponseDto visibleOrder = getActiveVisibleOrder();

        WebSocketEventDto<OrderResponseDto> event = WebSocketEventDto.customEvent(
                "ACTIVE_ORDER_CHANGED",
                visibleOrder != null
                        ? "Orden activa visible: " + visibleOrder.getCodOrder()
                        : "No hay órdenes activas visibles",
                visibleOrder
        );

        messagingTemplate.convertAndSend("/topic/cremer/active-order", event);

        log.info("📡 WS Notificación enviada - Orden visible: {}",
                visibleOrder != null ? visibleOrder.getCodOrder() : "ninguna");
    }
}