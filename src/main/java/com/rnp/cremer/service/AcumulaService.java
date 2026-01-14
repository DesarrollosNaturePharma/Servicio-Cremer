package com.rnp.cremer.service;

import com.rnp.cremer.dto.AcumulaFinishDto;
import com.rnp.cremer.dto.AcumulaResponseDto;
import com.rnp.cremer.dto.OrderResponseDto;
import com.rnp.cremer.dto.WebSocketEventDto;
import com.rnp.cremer.exception.InvalidOrderStateException;
import com.rnp.cremer.exception.OrderNotFoundException;
import com.rnp.cremer.model.Acumula;
import com.rnp.cremer.model.EstadoOrder;
import com.rnp.cremer.model.Order;
import com.rnp.cremer.repository.AcumulaRepository;
import com.rnp.cremer.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Servicio para gestión del proceso manual (acumulación) de órdenes.
 *
 * <p>Gestiona el ciclo de vida del proceso manual:</p>
 * <ul>
 *   <li>Iniciar proceso manual (ESPERA_MANUAL → PROCESO_MANUAL)</li>
 *   <li>Finalizar proceso manual (PROCESO_MANUAL → FINALIZADA)</li>
 *   <li>Consultar estado del proceso manual</li>
 * </ul>
 *
 * <p><b>Importante:</b> Las métricas NO se recalculan durante el proceso manual.
 * Las métricas se calculan únicamente cuando la orden sale de EN_PROCESO.</p>
 *
 * @author RNP Team
 * @version 1.0
 * @since 2024-11-26
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AcumulaService {

    private final AcumulaRepository acumulaRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final SimpMessagingTemplate messagingTemplate;

    // Constantes
    private static final String ORDER_NOT_FOUND_MSG = "Orden no encontrada con ID: %d";
    private static final String INVALID_STATE_MSG = "Estado inválido para esta operación. Estado actual: %s";

    private static final String WS_TOPIC_ORDERS = "/topic/orders";
    private static final String WS_TOPIC_ORDER_DETAIL = "/topic/orders/%d";

    // ========================================
    // INICIAR PROCESO MANUAL
    // ========================================

    /**
     * Inicia el proceso manual de una orden.
     *
     * <p>Transición: ESPERA_MANUAL → PROCESO_MANUAL</p>
     *
     * <p>Crea un registro en la tabla Acumula con la hora de inicio
     * y cambia el estado de la orden a PROCESO_MANUAL.</p>
     *
     * @param idOrder ID de la orden
     * @return DTO con los datos del proceso manual iniciado
     * @throws OrderNotFoundException si la orden no existe
     * @throws InvalidOrderStateException si la orden no está en ESPERA_MANUAL
     */
    @Transactional
    public AcumulaResponseDto iniciarProcesoManual(Long idOrder) {
        log.info("Iniciando proceso manual para orden ID: {}", idOrder);

        // 1. Obtener y validar orden
        Order order = findOrderById(idOrder);
        validateOrderStateForManualStart(order);

        // 2. Verificar que no exista ya un proceso manual activo
        if (acumulaRepository.hasActiveProcess(idOrder)) {
            String message = "Ya existe un proceso manual activo para esta orden";
            log.error(message);
            throw new InvalidOrderStateException(message);
        }

        // 3. Crear registro de acumulación
        Acumula acumula = Acumula.builder()
                .idOrder(idOrder)
                .horaInicio(LocalDateTime.now())
                .numCajasManual(0)
                .build();

        Acumula savedAcumula = acumulaRepository.save(acumula);

        // 4. Cambiar estado de la orden
        EstadoOrder estadoAnterior = order.getEstado();
        order.setEstado(EstadoOrder.PROCESO_MANUAL);
        order.setAcumula(true);
        Order updatedOrder = orderRepository.save(order);

        log.info("Proceso manual iniciado para orden {} a las {}",
                order.getCodOrder(), savedAcumula.getHoraInicio());

        // 5. Notificar vía WebSocket
        notifyManualProcessStarted(updatedOrder, estadoAnterior);

        // 6. Retornar DTO
        return mapToResponseDto(savedAcumula, order.getCodOrder());
    }

    /**
     * Valida que la orden esté en estado ESPERA_MANUAL.
     */
    private void validateOrderStateForManualStart(Order order) {
        if (order.getEstado() != EstadoOrder.ESPERA_MANUAL) {
            String message = String.format(
                    "Solo se puede iniciar proceso manual en órdenes con estado ESPERA_MANUAL. %s",
                    String.format(INVALID_STATE_MSG, order.getEstado())
            );
            log.error(message);
            throw new InvalidOrderStateException(message);
        }
    }

    // ========================================
    // FINALIZAR PROCESO MANUAL
    // ========================================

    /**
     * Finaliza el proceso manual de una orden.
     *
     * <p>Transición: PROCESO_MANUAL → FINALIZADA</p>
     *
     * <p>Actualiza el registro de Acumula con la hora de fin, calcula
     * el tiempo total y registra las cajas procesadas. Cambia el estado
     * de la orden a FINALIZADA.</p>
     *
     * <p><b>Importante:</b> Las métricas NO se recalculan. Ya fueron
     * calculadas cuando la orden salió de EN_PROCESO.</p>
     *
     * @param idOrder ID de la orden
     * @param dto Datos de finalización (número de cajas)
     * @return DTO con los datos del proceso manual finalizado
     * @throws OrderNotFoundException si la orden no existe
     * @throws InvalidOrderStateException si la orden no está en PROCESO_MANUAL
     */
    @Transactional
    public AcumulaResponseDto finalizarProcesoManual(Long idOrder, AcumulaFinishDto dto) {
        log.info("Finalizando proceso manual para orden ID: {}", idOrder);

        // 1. Obtener y validar orden
        Order order = findOrderById(idOrder);
        validateOrderStateForManualFinish(order);

        // 2. Obtener registro de acumulación activo
        Acumula acumula = acumulaRepository.findActiveByIdOrder(idOrder)
                .orElseThrow(() -> {
                    String message = "No se encontró proceso manual activo para la orden: " + idOrder;
                    log.error(message);
                    return new InvalidOrderStateException(message);
                });

        // 3. Actualizar registro de acumulación
        LocalDateTime horaFin = LocalDateTime.now();
        acumula.setHoraFin(horaFin);
        acumula.setNumCajasManual(dto.getNumCajasManual());

        // Calcular tiempo total en minutos
        Duration duration = Duration.between(acumula.getHoraInicio(), horaFin);
        float tiempoTotal = duration.toSeconds() / 60.0f;
        acumula.setTiempoTotal(tiempoTotal);

        Acumula savedAcumula = acumulaRepository.save(acumula);

        // 4. Cambiar estado de la orden a FINALIZADA
        EstadoOrder estadoAnterior = order.getEstado();
        order.setEstado(EstadoOrder.FINALIZADA);
        Order updatedOrder = orderRepository.save(order);

        log.info("Proceso manual finalizado para orden {} - Tiempo: {:.2f} min - Cajas: {}",
                order.getCodOrder(), tiempoTotal, dto.getNumCajasManual());

        // 5. Notificar vía WebSocket
        notifyManualProcessFinished(updatedOrder, estadoAnterior);

        // 6. Retornar DTO
        return mapToResponseDto(savedAcumula, order.getCodOrder());
    }

    /**
     * Valida que la orden esté en estado PROCESO_MANUAL.
     */
    private void validateOrderStateForManualFinish(Order order) {
        if (order.getEstado() != EstadoOrder.PROCESO_MANUAL) {
            String message = String.format(
                    "Solo se puede finalizar proceso manual en órdenes con estado PROCESO_MANUAL. %s",
                    String.format(INVALID_STATE_MSG, order.getEstado())
            );
            log.error(message);
            throw new InvalidOrderStateException(message);
        }
    }

    // ========================================
    // CONSULTAS
    // ========================================

    /**
     * Obtiene los datos de acumulación de una orden.
     *
     * @param idOrder ID de la orden
     * @return DTO con los datos de acumulación
     * @throws OrderNotFoundException si la orden no existe
     */
    @Transactional(readOnly = true)
    public AcumulaResponseDto getAcumulaByOrder(Long idOrder) {
        log.info("Obteniendo datos de acumulación para orden ID: {}", idOrder);

        Order order = findOrderById(idOrder);

        Acumula acumula = acumulaRepository.findByIdOrder(idOrder)
                .orElse(null);

        if (acumula == null) {
            return AcumulaResponseDto.builder()
                    .idOrder(idOrder)
                    .codOrder(order.getCodOrder())
                    .enProceso(false)
                    .build();
        }

        return mapToResponseDto(acumula, order.getCodOrder());
    }

    /**
     * Verifica si una orden tiene proceso manual activo.
     *
     * @param idOrder ID de la orden
     * @return true si tiene proceso activo, false en caso contrario
     */
    @Transactional(readOnly = true)
    public boolean hasActiveProcess(Long idOrder) {
        return acumulaRepository.hasActiveProcess(idOrder);
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
     * Convierte entidad Acumula a DTO de respuesta.
     */
    private AcumulaResponseDto mapToResponseDto(Acumula acumula, String codOrder) {
        return AcumulaResponseDto.builder()
                .idAcumula(acumula.getIdAcumula())
                .idOrder(acumula.getIdOrder())
                .codOrder(codOrder)
                .horaInicio(acumula.getHoraInicio())
                .horaFin(acumula.getHoraFin())
                .tiempoTotal(acumula.getTiempoTotal())
                .numCajasManual(acumula.getNumCajasManual())
                .enProceso(acumula.getHoraFin() == null)
                .build();
    }

    // ========================================
    // NOTIFICACIONES WEBSOCKET
    // ========================================

    /**
     * Notifica el inicio del proceso manual vía WebSocket.
     */
    private void notifyManualProcessStarted(Order order, EstadoOrder estadoAnterior) {
        String message = String.format(
                "Proceso manual iniciado para orden %s",
                order.getCodOrder()
        );

        OrderResponseDto orderDto = orderService.mapToResponseDtoPublic(order);
        WebSocketEventDto<OrderResponseDto> event = WebSocketEventDto.orderStateChanged(message, orderDto);

        // Notificación general
        messagingTemplate.convertAndSend(WS_TOPIC_ORDERS, event);

        // Notificación específica de la orden
        String topicOrderDetail = String.format(WS_TOPIC_ORDER_DETAIL, order.getIdOrder());
        messagingTemplate.convertAndSend(topicOrderDetail, event);

        log.debug("Notificación WebSocket enviada - Proceso manual iniciado: {}", order.getCodOrder());
    }

    /**
     * Notifica la finalización del proceso manual vía WebSocket.
     */
    private void notifyManualProcessFinished(Order order, EstadoOrder estadoAnterior) {
        String message = String.format(
                "Proceso manual finalizado para orden %s. Orden FINALIZADA.",
                order.getCodOrder()
        );

        OrderResponseDto orderDto = orderService.mapToResponseDtoPublic(order);
        WebSocketEventDto<OrderResponseDto> event = WebSocketEventDto.orderStateChanged(message, orderDto);

        // Notificación general
        messagingTemplate.convertAndSend(WS_TOPIC_ORDERS, event);

        // Notificación específica de la orden
        String topicOrderDetail = String.format(WS_TOPIC_ORDER_DETAIL, order.getIdOrder());
        messagingTemplate.convertAndSend(topicOrderDetail, event);

        log.debug("Notificación WebSocket enviada - Proceso manual finalizado: {}", order.getCodOrder());
    }
}