package com.rnp.cremer.service;

import com.rnp.cremer.dto.OrderDeleteAuditResponseDto;
import com.rnp.cremer.dto.OrderDeleteRequestDto;
import com.rnp.cremer.dto.WebSocketEventDto;
import com.rnp.cremer.exception.InvalidOrderStateException;
import com.rnp.cremer.exception.OrderNotFoundException;
import com.rnp.cremer.model.EstadoOrder;
import com.rnp.cremer.model.Order;
import com.rnp.cremer.model.OrderDeleteAudit;
import com.rnp.cremer.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio para eliminación de órdenes con auditoría.
 *
 * <p>Gestiona la eliminación segura de órdenes, registrando información
 * completa de auditoría incluyendo quién eliminó, cuándo y por qué motivo.</p>
 *
 * <p><b>Importante:</b> La eliminación borra la orden y todos sus datos
 * relacionados (pausas, métricas, acumulación, extra data, contadores).</p>
 *
 * @author RNP Team
 * @version 1.0
 * @since 2024-11-26
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderDeleteService {

    private final OrderRepository orderRepository;
    private final OrderDeleteAuditRepository auditRepository;
    private final PauseRepository pauseRepository;
    private final MetricasRepository metricasRepository;
    private final AcumulaRepository acumulaRepository;
    private final ExtraDataRepository extraDataRepository;
    private final BottleCounterRepository bottleCounterRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private static final String ORDER_NOT_FOUND_MSG = "Orden no encontrada con ID: %d";
    private static final String WS_TOPIC_ORDERS = "/topic/orders";

    // ========================================
    // ELIMINACIÓN DE ORDEN CON AUDITORÍA
    // ========================================

    /**
     * Elimina una orden registrando auditoría completa.
     *
     * <p>Proceso:</p>
     * <ol>
     *   <li>Valida que la orden existe</li>
     *   <li>Valida que la orden puede ser eliminada (no en proceso activo)</li>
     *   <li>Crea registro de auditoría con todos los datos de la orden</li>
     *   <li>Elimina datos relacionados (pausas, métricas, etc.)</li>
     *   <li>Elimina la orden</li>
     *   <li>Notifica vía WebSocket</li>
     * </ol>
     *
     * @param idOrder ID de la orden a eliminar
     * @param dto Datos de la solicitud (usuario y motivo)
     * @param ipAddress Dirección IP del solicitante (opcional)
     * @return DTO con información del registro de auditoría
     * @throws OrderNotFoundException si la orden no existe
     * @throws InvalidOrderStateException si la orden no puede ser eliminada
     */
    @Transactional
    public OrderDeleteAuditResponseDto deleteOrder(Long idOrder, OrderDeleteRequestDto dto, String ipAddress) {
        log.info("Solicitud de eliminación de orden ID: {} por usuario: {}", idOrder, dto.getDeletedBy());

        // 1. Obtener y validar orden
        Order order = findOrderById(idOrder);
        validateOrderCanBeDeleted(order);

        // 2. Crear registro de auditoría ANTES de eliminar
        OrderDeleteAudit audit = createAuditRecord(order, dto, ipAddress);
        OrderDeleteAudit savedAudit = auditRepository.save(audit);

        log.info("Registro de auditoría creado - ID: {} para orden: {}",
                savedAudit.getIdAudit(), order.getCodOrder());

        // 3. Eliminar datos relacionados
        deleteRelatedData(idOrder);

        // 4. Eliminar la orden
        orderRepository.delete(order);

        log.info("Orden {} (ID: {}) eliminada por {} - Motivo: {}",
                order.getCodOrder(), idOrder, dto.getDeletedBy(), dto.getMotivo());

        // 5. Notificar vía WebSocket
        notifyOrderDeleted(order, dto.getDeletedBy());

        // 6. Retornar DTO de auditoría
        return mapToResponseDto(savedAudit);
    }

    /**
     * Elimina múltiples órdenes con auditoría.
     *
     * @param orderIds Lista de IDs de órdenes a eliminar
     * @param dto Datos de la solicitud
     * @param ipAddress Dirección IP del solicitante
     * @return Lista de DTOs de auditoría
     */
    @Transactional
    public List<OrderDeleteAuditResponseDto> deleteMultipleOrders(
            List<Long> orderIds,
            OrderDeleteRequestDto dto,
            String ipAddress) {

        log.info("Solicitud de eliminación múltiple de {} órdenes por usuario: {}",
                orderIds.size(), dto.getDeletedBy());

        return orderIds.stream()
                .map(id -> {
                    try {
                        return deleteOrder(id, dto, ipAddress);
                    } catch (OrderNotFoundException | InvalidOrderStateException e) {
                        log.warn("No se pudo eliminar orden ID: {} - {}", id, e.getMessage());
                        return null;
                    }
                })
                .filter(audit -> audit != null)
                .toList();
    }

    // ========================================
    // VALIDACIONES
    // ========================================

    /**
     * Valida que la orden puede ser eliminada.
     *
     * <p>No se pueden eliminar órdenes que estén actualmente en proceso
     * o en proceso manual.</p>
     */
    private void validateOrderCanBeDeleted(Order order) {
        if (order.getEstado() == EstadoOrder.EN_PROCESO ||
            order.getEstado() == EstadoOrder.PROCESO_MANUAL) {
            String message = String.format(
                    "No se puede eliminar una orden en estado %s. " +
                    "Finalice o cancele la orden primero.",
                    order.getEstado()
            );
            log.error(message);
            throw new InvalidOrderStateException(message);
        }
    }

    // ========================================
    // CREACIÓN DE REGISTRO DE AUDITORÍA
    // ========================================

    /**
     * Crea el registro de auditoría con todos los datos de la orden.
     */
    private OrderDeleteAudit createAuditRecord(Order order, OrderDeleteRequestDto dto, String ipAddress) {
        return OrderDeleteAudit.builder()
                .idOrderDeleted(order.getIdOrder())
                .codOrder(order.getCodOrder())
                .operario(order.getOperario())
                .lote(order.getLote())
                .articulo(order.getArticulo())
                .estadoAlEliminar(order.getEstado())
                .fechaCreacionOrder(order.getHoraCreacion())
                .cantidad(order.getCantidad())
                .botesBuenos(order.getBotesBuenos())
                .botesMalos(order.getBotesMalos())
                .deletedBy(dto.getDeletedBy())
                .motivo(dto.getMotivo())
                .ipAddress(ipAddress)
                .build();
    }

    // ========================================
    // ELIMINACIÓN DE DATOS RELACIONADOS
    // ========================================

    /**
     * Elimina todos los datos relacionados con la orden.
     */
    private void deleteRelatedData(Long idOrder) {
        log.debug("Eliminando datos relacionados para orden ID: {}", idOrder);

        // Eliminar pausas
        pauseRepository.deleteByIdOrder(idOrder);
        log.debug("Pausas eliminadas para orden ID: {}", idOrder);

        // Eliminar métricas
        metricasRepository.deleteByIdOrder(idOrder);
        log.debug("Métricas eliminadas para orden ID: {}", idOrder);

        // Eliminar acumulación
        acumulaRepository.deleteByIdOrder(idOrder);
        log.debug("Acumulación eliminada para orden ID: {}", idOrder);

        // Eliminar extra data
        extraDataRepository.deleteByIdOrder(idOrder);
        log.debug("ExtraData eliminado para orden ID: {}", idOrder);

        // Eliminar contador de botellas
        bottleCounterRepository.deleteByIdOrder(idOrder);
        log.debug("BottleCounter eliminado para orden ID: {}", idOrder);
    }

    // ========================================
    // CONSULTAS DE AUDITORÍA
    // ========================================

    /**
     * Obtiene todos los registros de auditoría paginados.
     */
    @Transactional(readOnly = true)
    public Page<OrderDeleteAuditResponseDto> getAllAuditRecords(Pageable pageable) {
        log.info("Obteniendo registros de auditoría de eliminación");
        return auditRepository.findAllByOrderByDeletedAtDesc(pageable)
                .map(this::mapToResponseDto);
    }

    /**
     * Obtiene registros de auditoría por filtros.
     */
    @Transactional(readOnly = true)
    public Page<OrderDeleteAuditResponseDto> getAuditRecordsByFilters(
            String deletedBy,
            String codOrder,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin,
            Pageable pageable) {

        log.info("Obteniendo registros de auditoría - Filtros: user={}, cod={}, desde={}, hasta={}",
                deletedBy, codOrder, fechaInicio, fechaFin);

        return auditRepository.findByFilters(deletedBy, codOrder, fechaInicio, fechaFin, pageable)
                .map(this::mapToResponseDto);
    }

    /**
     * Obtiene un registro de auditoría por su ID.
     */
    @Transactional(readOnly = true)
    public OrderDeleteAuditResponseDto getAuditRecordById(Long idAudit) {
        log.info("Obteniendo registro de auditoría ID: {}", idAudit);

        OrderDeleteAudit audit = auditRepository.findById(idAudit)
                .orElseThrow(() -> {
                    String message = "Registro de auditoría no encontrado con ID: " + idAudit;
                    log.error(message);
                    return new RuntimeException(message);
                });

        return mapToResponseDto(audit);
    }

    /**
     * Obtiene registros de auditoría por usuario que eliminó.
     */
    @Transactional(readOnly = true)
    public Page<OrderDeleteAuditResponseDto> getAuditRecordsByUser(String deletedBy, Pageable pageable) {
        log.info("Obteniendo registros de auditoría para usuario: {}", deletedBy);
        return auditRepository.findByDeletedByContainingIgnoreCase(deletedBy, pageable)
                .map(this::mapToResponseDto);
    }

    // ========================================
    // MÉTODOS AUXILIARES
    // ========================================

    /**
     * Busca una orden por ID o lanza excepción si no existe.
     */
    private Order findOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> {
                    String message = String.format(ORDER_NOT_FOUND_MSG, id);
                    log.error(message);
                    return new OrderNotFoundException(message);
                });
    }

    /**
     * Convierte entidad de auditoría a DTO de respuesta.
     */
    private OrderDeleteAuditResponseDto mapToResponseDto(OrderDeleteAudit audit) {
        return OrderDeleteAuditResponseDto.builder()
                .idAudit(audit.getIdAudit())
                .idOrderDeleted(audit.getIdOrderDeleted())
                .codOrder(audit.getCodOrder())
                .operario(audit.getOperario())
                .lote(audit.getLote())
                .articulo(audit.getArticulo())
                .estadoAlEliminar(audit.getEstadoAlEliminar())
                .fechaCreacionOrder(audit.getFechaCreacionOrder())
                .cantidad(audit.getCantidad())
                .botesBuenos(audit.getBotesBuenos())
                .botesMalos(audit.getBotesMalos())
                .deletedBy(audit.getDeletedBy())
                .motivo(audit.getMotivo())
                .deletedAt(audit.getDeletedAt())
                .ipAddress(audit.getIpAddress())
                .build();
    }

    // ========================================
    // NOTIFICACIONES WEBSOCKET
    // ========================================

    /**
     * Notifica la eliminación de una orden vía WebSocket.
     */
    private void notifyOrderDeleted(Order order, String deletedBy) {
        String message = String.format(
                "Orden %s eliminada por %s",
                order.getCodOrder(), deletedBy
        );

        WebSocketEventDto<String> event = WebSocketEventDto.customEvent(
                "ORDER_DELETED",
                message,
                order.getCodOrder()
        );

        messagingTemplate.convertAndSend(WS_TOPIC_ORDERS, event);

        log.debug("Notificación WebSocket enviada - Orden eliminada: {}", order.getCodOrder());
    }
}
