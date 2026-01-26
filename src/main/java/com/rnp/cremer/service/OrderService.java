package com.rnp.cremer.service;

import com.rnp.cremer.dto.*;
import com.rnp.cremer.exception.OrderNotFoundException;
import com.rnp.cremer.exception.OrderAlreadyExistsException;
import com.rnp.cremer.exception.InvalidOrderStateException;
import com.rnp.cremer.model.*;
import com.rnp.cremer.repository.ExtraDataRepository;
import com.rnp.cremer.repository.MetricasRepository;
import com.rnp.cremer.repository.OrderRepository;
import com.rnp.cremer.repository.PauseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio de gestión de órdenes de producción.
 *
 * <p>Proporciona operaciones completas del ciclo de vida de órdenes:</p>
 * <ul>
 *   <li>Creación de órdenes con datos iniciales</li>
 *   <li>Inicio de producción (cambio a EN_PROCESO)</li>
 *   <li>Finalización con registro de resultados</li>
 *   <li>Soporte para proceso manual (acumulación)</li>
 *   <li>Consultas y filtrado de órdenes</li>
 *   <li>Notificaciones en tiempo real vía WebSocket</li>
 * </ul>
 *
 * <h3>Flujo de Estados con Acumulación:</h3>
 * <pre>
 * CREADA → EN_PROCESO ↔ PAUSADA
 *              ↓
 *    finalizarOrder(acumula=false) → FINALIZADA
 *    finalizarOrder(acumula=true)  → ESPERA_MANUAL → PROCESO_MANUAL → FINALIZADA
 * </pre>
 *
 * <p><b>Importante:</b> Las métricas se calculan UNA SOLA VEZ cuando la orden
 * sale de EN_PROCESO, independientemente de si acumula o no.</p>
 *
 * @author RNP Team
 * @version 1.1
 * @since 2024-11-25
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ExtraDataRepository extraDataRepository;
    private final PauseRepository pauseRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final MetricasService metricasService;
    private final MetricasRepository metricasRepository;
    private final OrderQueryService orderQueryService;
    private final BottleCounterService bottleCounterService;
    // Constantes
    private static final String ORDER_NOT_FOUND_MSG = "Orden no encontrada con ID: %d";
    private static final String ORDER_NOT_FOUND_CODE_MSG = "Orden no encontrada con código: %s";
    private static final String ORDER_EXISTS_MSG = "Ya existe una orden con el código: %s";
    private static final String INVALID_STATE_MSG = "Estado inválido para esta operación. Estado actual: %s";

    private static final String WS_TOPIC_ORDERS = "/topic/orders";
    private static final String WS_TOPIC_ORDER_DETAIL = "/topic/orders/%d";

    // ========================================
    // CREACIÓN DE ÓRDENES
    // ========================================

    /**
     * Crea una nueva orden de producción con estado CREADA.
     *
     * @param dto datos de creación de la orden
     * @return orden creada con todos sus datos
     * @throws OrderAlreadyExistsException si ya existe una orden con el mismo código
     */
    @Transactional
    public OrderResponseDto createOrder(OrderCreateDto dto) {
        log.info("Creando orden con código: {}", dto.getCodOrder());

        validateOrderDoesNotExist(dto.getCodOrder());

        Order order = buildOrderFromDto(dto);
        Order savedOrder = orderRepository.save(order);

        ExtraData extraData = buildExtraDataFromDto(dto, savedOrder.getIdOrder());
        ExtraData savedExtraData = extraDataRepository.save(extraData);

        log.info("Orden creada exitosamente - ID: {}, Código: {}",
                savedOrder.getIdOrder(), savedOrder.getCodOrder());

        OrderResponseDto responseDto = mapToResponseDto(savedOrder, savedExtraData);
        notifyOrderCreated(responseDto);

        return responseDto;
    }

    /**
     * Valida que no exista una orden con el código dado.
     */
    private void validateOrderDoesNotExist(String codOrder) {
        if (orderRepository.existsByCodOrder(codOrder)) {
            String message = String.format(ORDER_EXISTS_MSG, codOrder);
            log.error(message);
            throw new OrderAlreadyExistsException(message);
        }
    }

    /**
     * Construye una entidad Order a partir del DTO de creación.
     */
    private Order buildOrderFromDto(OrderCreateDto dto) {
        Float cajasPrevistas = calculateCajasPrevistas(dto.getCantidad(), dto.getBotesCaja());
        Float tiempoEstimado = calculateTiempoEstimado(dto.getCantidad(), dto.getStdReferencia());

        return Order.builder()
                .operario(dto.getOperario())
                .codOrder(dto.getCodOrder())
                .lote(dto.getLote())
                .articulo(dto.getArticulo())
                .descripcion(dto.getDescripcion())
                .cantidad(dto.getCantidad())
                .botesCaja(dto.getBotesCaja())
                .stdReferencia(dto.getStdReferencia())
                .estado(EstadoOrder.CREADA)
                .horaCreacion(LocalDateTime.now())
                .cajasPrevistas(cajasPrevistas)
                .tiempoEstimado(tiempoEstimado)
                .repercap(false)
                .acumula(false)
                .build();
    }

    /**
     * Construye una entidad ExtraData a partir del DTO.
     */
    private ExtraData buildExtraDataFromDto(OrderCreateDto dto, Long orderId) {
        return ExtraData.builder()
                .idOrder(orderId)
                .formatoBote(dto.getFormatoBote())
                .tipo(dto.getTipo())
                .udsBote(dto.getUdsBote())
                .build();
    }

    /**
     * Calcula las cajas previstas según la cantidad y botes por caja.
     */
    private Float calculateCajasPrevistas(Integer cantidad, Integer botesCaja) {
        if (botesCaja == null || botesCaja == 0) {
            return 0.0f;
        }
        return (float) cantidad / botesCaja;
    }

/**
 * Calcula el tiempo estimado según la cantidad y el estándar de referencia.
 * @param cantidad número de botes a producir
 * @param stdReferencia tasa de producción en botes/minuto
 * @return tiempo estimado en minutos
 */
private Float calculateTiempoEstimado(Integer cantidad, Float stdReferencia) {
    if (stdReferencia == null || stdReferencia == 0) {
        return 0.0f;
    }
    return cantidad / stdReferencia;  // ← DIVISIÓN (no multiplicación)
}

    // ========================================
    // INICIO DE PRODUCCIÓN
    // ========================================

    /**
     * Inicia una orden de producción cambiando su estado a EN_PROCESO.
     *
     * @param id identificador de la orden
     * @return orden actualizada con estado EN_PROCESO
     * @throws OrderNotFoundException si la orden no existe
     * @throws InvalidOrderStateException si la orden no está en estado CREADA
     */
    @Transactional
    public OrderResponseDto iniciarOrder(Long id) {
        log.info("Iniciando orden con ID: {}", id);

        Order order = findOrderById(id);
        validateOrderStateForStart(order);

        EstadoOrder estadoAnterior = order.getEstado();
        order.setEstado(EstadoOrder.EN_PROCESO);
        order.setHoraInicio(LocalDateTime.now());

        Order updatedOrder = orderRepository.save(order);

        bottleCounterService.activateCounterForOrder(id);


        log.info("Orden {} iniciada exitosamente a las {}",
                updatedOrder.getCodOrder(), updatedOrder.getHoraInicio());

        OrderResponseDto responseDto = mapToResponseDtoSimple(updatedOrder);
        notifyOrderStateChanged(responseDto, estadoAnterior, EstadoOrder.EN_PROCESO);
        orderQueryService.notifyActiveVisibleOrderChange();

        return responseDto;
    }

    /**
     * Valida que la orden esté en estado CREADA para poder iniciarla.
     */
    private void validateOrderStateForStart(Order order) {
        if (order.getEstado() != EstadoOrder.CREADA) {
            String message = String.format(
                    "Solo se pueden iniciar órdenes en estado CREADA. %s",
                    String.format(INVALID_STATE_MSG, order.getEstado())
            );
            log.error(message);
            throw new InvalidOrderStateException(message);
        }
    }

    // ========================================
    // FINALIZACIÓN DE ÓRDENES
    // ========================================

    /**
     * Finaliza una orden de producción registrando los resultados finales.
     *
     * <p>Si la orden está pausada, finaliza automáticamente la pausa activa.</p>
     *
     * <p><b>Flujo según parámetro acumula:</b></p>
     * <ul>
     *   <li><b>acumula=false:</b> Estado pasa a FINALIZADA</li>
     *   <li><b>acumula=true:</b> Estado pasa a ESPERA_MANUAL (requiere proceso manual)</li>
     * </ul>
     *
     * <p><b>Importante:</b> Las métricas se calculan SIEMPRE en este punto,
     * independientemente del valor de acumula. NO se recalculan después.</p>
     *
     * @param id identificador de la orden
     * @param dto datos de finalización (incluye campo acumula)
     * @return orden finalizada o en espera manual
     * @throws OrderNotFoundException si la orden no existe
     * @throws InvalidOrderStateException si la orden no está EN_PROCESO ni PAUSADA
     */
    @Transactional
    public OrderResponseDto finalizarOrder(Long id, OrderFinishDto dto) {
        log.info("Finalizando orden con ID: {} - Acumula: {}", id, dto.getAcumula());

        Order order = findOrderById(id);
        validateOrderStateForFinish(order);

        // Si está pausada, finalizar pausa activa automáticamente
        if (order.getEstado() == EstadoOrder.PAUSADA) {
            finalizarPausaActivaAutomaticamente(order);
            order.setEstado(EstadoOrder.EN_PROCESO);
        }

        // Validación adicional de seguridad
        validateNoPausasActivas(id);

        // Aplicar datos de finalización
        applyFinishData(order, dto);

        // Establecer hora de fin (siempre se registra aquí)
        order.setHoraFin(LocalDateTime.now());

        // Determinar estado final según acumula
        Boolean acumula = dto.getAcumula() != null ? dto.getAcumula() : false;
        EstadoOrder estadoAnterior = order.getEstado();
        EstadoOrder nuevoEstado;

        if (acumula) {
            nuevoEstado = EstadoOrder.ESPERA_MANUAL;
            order.setAcumula(true);
            log.info("Orden {} requiere proceso manual. Estado: ESPERA_MANUAL", order.getCodOrder());
        } else {
            nuevoEstado = EstadoOrder.FINALIZADA;
            order.setAcumula(false);
            log.info("Orden {} no requiere proceso manual. Estado: FINALIZADA", order.getCodOrder());
        }

        order.setEstado(nuevoEstado);
        Order updatedOrder = orderRepository.save(order);
        if (nuevoEstado == EstadoOrder.FINALIZADA) {
            bottleCounterService.deactivateCounterForOrder(id);
        }
        log.info("Orden {} procesada - Buenos: {}, Malos: {}, Cajas: {}, Estado: {}",
                updatedOrder.getCodOrder(), dto.getBotesBuenos(),
                dto.getBotesMalos(), dto.getTotalCajasCierre(), nuevoEstado);

        // =====================================================
        // CALCULAR MÉTRICAS (UNA SOLA VEZ, AQUÍ)
        // =====================================================
        // Las métricas se calculan siempre cuando la orden sale de EN_PROCESO,
        // independientemente de si va a FINALIZADA o ESPERA_MANUAL.
        // NO se recalculan cuando termine el proceso manual.
        metricasService.calcularYGuardarMetricas(updatedOrder);

        OrderResponseDto responseDto = mapToResponseDtoSimple(updatedOrder);

        // Notificar según el estado final
        if (acumula) {
            notifyOrderWaitingManual(responseDto);
        } else {
            notifyOrderFinished(responseDto);
        }
        orderQueryService.notifyActiveVisibleOrderChange();
        return responseDto;
    }

    /**
     * Valida que la orden esté en estado válido para finalizar.
     */
    private void validateOrderStateForFinish(Order order) {
        if (order.getEstado() != EstadoOrder.EN_PROCESO &&
                order.getEstado() != EstadoOrder.PAUSADA) {
            String message = String.format(
                    "Solo se pueden finalizar órdenes EN_PROCESO o PAUSADAS. %s",
                    String.format(INVALID_STATE_MSG, order.getEstado())
            );
            log.error(message);
            throw new InvalidOrderStateException(message);
        }
    }

    /**
     * Finaliza automáticamente la pausa activa de una orden.
     */
    private void finalizarPausaActivaAutomaticamente(Order order) {
        log.info("Orden está PAUSADA. Finalizando pausa activa automáticamente...");

        Optional<Pause> pausaActivaOpt = pauseRepository.findActivePauseByOrder(order.getIdOrder());

        if (pausaActivaOpt.isPresent()) {
            Pause pausaActiva = pausaActivaOpt.get();
            LocalDateTime horaFin = LocalDateTime.now();

            pausaActiva.setHoraFin(horaFin);

            Duration duration = Duration.between(pausaActiva.getHoraInicio(), horaFin);
            pausaActiva.setTiempoTotalPausa(duration.toSeconds() / 60.0f);

            pauseRepository.save(pausaActiva);
            log.info("Pausa {} finalizada automáticamente", pausaActiva.getIdPausa());
        }
    }

    /**
     * Valida que no existan pausas activas antes de finalizar.
     */
    private void validateNoPausasActivas(Long orderId) {
        if (pauseRepository.hasActivePause(orderId)) {
            String message = "No se puede finalizar la orden. Hay una pausa activa. Primero finalice la pausa.";
            log.error(message);
            throw new InvalidOrderStateException(message);
        }
    }

    /**
     * Aplica los datos de finalización a la orden.
     */
    private void applyFinishData(Order order, OrderFinishDto dto) {
        order.setBotesBuenos(dto.getBotesBuenos());
        order.setBotesMalos(dto.getBotesMalos());
        order.setTotalCajasCierre(dto.getTotalCajasCierre());
    }

    // ========================================
    // CONSULTAS Y FILTRADO
    // ========================================

    /**
     * Obtiene una página de órdenes aplicando filtros opcionales.
     *
     * @param estado filtro por estado (opcional)
     * @param operario filtro por operario (opcional)
     * @param lote filtro por lote (opcional)
     * @param articulo filtro por artículo (opcional)
     * @param pageable configuración de paginación
     * @return página de órdenes que cumplen los criterios
     */
    @Transactional(readOnly = true)
    public Page<OrderResponseDto> getAllOrders(
            EstadoOrder estado,
            String operario,
            String lote,
            String articulo,
            Pageable pageable) {

        log.info("Obteniendo órdenes - Filtros: Estado={}, Operario={}, Lote={}, Articulo={}",
                estado, operario, lote, articulo);

        String operarioLower = (operario != null) ? operario.toLowerCase() : null;
        String loteLower = (lote != null) ? lote.toLowerCase() : null;
        String articuloLower = (articulo != null) ? articulo.toLowerCase() : null;

        Page<Order> orders = hasAnyFilter(estado, operario, lote, articulo)
                ? orderRepository.findByFilters(estado, operarioLower, loteLower, articuloLower, pageable)
                : orderRepository.findAllByOrderByHoraCreacionDesc(pageable);

        return orders.map(this::mapToResponseDtoSimple);
    }

    /**
     * Verifica si se aplicó algún filtro.
     */
    private boolean hasAnyFilter(EstadoOrder estado, String operario, String lote, String articulo) {
        return estado != null
                || (operario != null && !operario.isBlank())
                || (lote != null && !lote.isBlank())
                || (articulo != null && !articulo.isBlank());
    }


    /**
     * Obtiene una orden por su ID.
     *
     * @param id identificador de la orden
     * @return orden encontrada
     * @throws OrderNotFoundException si la orden no existe
     */
    @Transactional(readOnly = true)
    public OrderResponseDto getOrderById(Long id) {
        log.info("Obteniendo orden con ID: {}", id);
        Order order = findOrderById(id);
        return mapToResponseDtoSimple(order);
    }

    /**
     * Obtiene una orden por su código único.
     *
     * @param codOrder código de la orden
     * @return orden encontrada
     * @throws OrderNotFoundException si la orden no existe
     */
    @Transactional(readOnly = true)
    public OrderResponseDto getOrderByCodOrder(String codOrder) {
        log.info("Obteniendo orden con código: {}", codOrder);

        Order order = orderRepository.findByCodOrder(codOrder)
                .orElseThrow(() -> {
                    String message = String.format(ORDER_NOT_FOUND_CODE_MSG, codOrder);
                    log.error(message);
                    return new OrderNotFoundException(message);
                });

        return mapToResponseDtoSimple(order);
    }

    /**
     * Obtiene estadísticas de órdenes agrupadas por estado.
     *
     * @return mapa con el conteo de órdenes por cada estado
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getOrdersStatistics() {
        log.info("Obteniendo estadísticas de órdenes");

        List<Order> allOrders = orderRepository.findAll();

        Map<String, Long> stats = Arrays.stream(EstadoOrder.values())
                .collect(Collectors.toMap(
                        EstadoOrder::name,
                        estado -> allOrders.stream()
                                .filter(order -> order.getEstado() == estado)
                                .count()
                ));

        log.debug("Estadísticas calculadas: {}", stats);
        return stats;
    }

    // ========================================
    // MÉTODOS AUXILIARES DE BÚSQUEDA
    // ========================================

    /**
     * Busca una orden por ID o lanza excepción si no existe.
     *
     * @param id identificador de la orden
     * @return orden encontrada
     * @throws OrderNotFoundException si la orden no existe
     */
    private Order findOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> {
                    String message = String.format(ORDER_NOT_FOUND_MSG, id);
                    log.error(message);
                    return new OrderNotFoundException(message);
                });
    }

    // ========================================
    // NOTIFICACIONES WEBSOCKET
    // ========================================

    /**
     * Notifica la creación de una nueva orden vía WebSocket.
     */
    private void notifyOrderCreated(OrderResponseDto order) {
        WebSocketEventDto<OrderResponseDto> event = WebSocketEventDto.orderCreated(
                "Nueva orden creada: " + order.getCodOrder(),
                order
        );

        messagingTemplate.convertAndSend(WS_TOPIC_ORDERS, event);

        log.debug("Notificación WebSocket enviada - Orden creada: {}", order.getCodOrder());
    }

    /**
     * Notifica un cambio de estado de orden vía WebSocket.
     */
    private void notifyOrderStateChanged(
            OrderResponseDto order,
            EstadoOrder estadoAnterior,
            EstadoOrder estadoNuevo) {

        String message = String.format(
                "Orden %s cambió de %s a %s",
                order.getCodOrder(), estadoAnterior, estadoNuevo
        );

        WebSocketEventDto<OrderResponseDto> event = WebSocketEventDto.orderStateChanged(message, order);

        // Notificación general
        messagingTemplate.convertAndSend(WS_TOPIC_ORDERS, event);

        // Notificación específica de la orden
        String topicOrderDetail = String.format(WS_TOPIC_ORDER_DETAIL, order.getIdOrder());
        messagingTemplate.convertAndSend(topicOrderDetail, event);

        log.debug("Notificación WebSocket enviada - Cambio de estado: {} -> {}",
                estadoAnterior, estadoNuevo);
    }

    /**
     * Notifica la finalización de una orden vía WebSocket.
     */
    private void notifyOrderFinished(OrderResponseDto order) {
        String message = String.format("Orden %s finalizada", order.getCodOrder());

        WebSocketEventDto<OrderResponseDto> event = WebSocketEventDto.orderStateChanged(message, order);

        messagingTemplate.convertAndSend(WS_TOPIC_ORDERS, event);

        String topicOrderDetail = String.format(WS_TOPIC_ORDER_DETAIL, order.getIdOrder());
        messagingTemplate.convertAndSend(topicOrderDetail, event);

        log.debug("Notificación WebSocket enviada - Orden finalizada: {}", order.getCodOrder());
    }

    /**
     * Notifica que una orden espera proceso manual vía WebSocket.
     */
    private void notifyOrderWaitingManual(OrderResponseDto order) {
        String message = String.format(
                "Orden %s en espera de proceso manual",
                order.getCodOrder()
        );

        WebSocketEventDto<OrderResponseDto> event = WebSocketEventDto.orderStateChanged(message, order);

        messagingTemplate.convertAndSend(WS_TOPIC_ORDERS, event);

        String topicOrderDetail = String.format(WS_TOPIC_ORDER_DETAIL, order.getIdOrder());
        messagingTemplate.convertAndSend(topicOrderDetail, event);

        log.debug("Notificación WebSocket enviada - Orden en espera manual: {}", order.getCodOrder());
    }

    // ========================================
    // MAPPERS (CONVERSIÓN ENTIDAD → DTO)
    // ========================================

    /**
     * Convierte una entidad Order y ExtraData a DTO de respuesta.
     */
    private OrderResponseDto mapToResponseDto(Order order, ExtraData extraData) {
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

    /**
     * Convierte una entidad Order a DTO cargando ExtraData si existe.
     */
    private OrderResponseDto mapToResponseDtoSimple(Order order) {
        ExtraData extraData = extraDataRepository.findByIdOrder(order.getIdOrder())
                .orElse(null);

        return mapToResponseDto(order, extraData);
    }

    // ========================================
    // API PÚBLICA PARA OTROS SERVICIOS
    // ========================================

    /**
     * Notifica cambios de estado de orden (uso público para PauseService).
     *
     * @param order orden actualizada
     * @param estadoAnterior estado previo
     * @param estadoNuevo estado actual
     */
    public void notifyOrderStateChangedPublic(
            OrderResponseDto order,
            EstadoOrder estadoAnterior,
            EstadoOrder estadoNuevo) {
        notifyOrderStateChanged(order, estadoAnterior, estadoNuevo);
    }

    /**
     * Convierte Order a DTO (uso público para PauseService y AcumulaService).
     *
     * @param order entidad Order
     * @return OrderResponseDto con todos los datos
     */
    public OrderResponseDto mapToResponseDtoPublic(Order order) {
        return mapToResponseDtoSimple(order);
    }

    /**
     * Obtiene una orden completa con métricas y datos extra.
     *
     * @param idOrden ID de la orden
     * @return DTO combinado con todos los datos
     */
    public OrderCompletaDto obtenerOrdenCompleta(Long idOrden) {

        // 1. Obtener Order
        Order order = orderRepository.findById(idOrden)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada"));

        // 2. Obtener Metricas (USANDO Optional)
        Metricas metricas = metricasRepository.findByIdOrder(idOrden)
                .orElse(null);

        // 3. Obtener ExtraData (USANDO Optional)
        ExtraData extraData = extraDataRepository.findByIdOrder(idOrden)
                .orElse(null);

        // 4. Devolver DTO combinado
        return new OrderCompletaDto(order, metricas, extraData);
    }

    /**
     * Obtiene datos de una orden para tabla.
     *
     * @param idOrden ID de la orden
     * @return DTO para visualización en tabla
     */
    public OrderTableResponse obtenerOrderTable(Long idOrden) {

        // 1. Obtener Order
        Order order = orderRepository.findById(idOrden)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada"));

        // 2. Obtener Metricas
        Metricas metricas = metricasRepository.findByIdOrder(idOrden)
                .orElse(null);

        // 3. Construir el DTO
        return new OrderTableResponse(
                order.getIdOrder(),
                order.getCodOrder(),
                order.getDescripcion(),
                order.getCantidad(),
                order.getEstado(),
                order.getHoraInicio(),
                order.getHoraFin(),
                metricas != null ? metricas.getOee() : null,
                metricas != null ? metricas.getPorCumpPedido() : null
        );
    }

    /**
     * Obtiene todas las órdenes para tabla.
     *
     * @return Lista de DTOs para visualización en tabla
     */
    public List<OrderTableResponse> obtenerTodasLasOrdersTable() {

        List<Order> orders = orderRepository.findAll();

        List<OrderTableResponse> responseList = new ArrayList<>();

        for (Order order : orders) {

            // traer métricas por idOrder
            Metricas metricas = metricasRepository.findByIdOrder(order.getIdOrder())
                    .orElse(null);

            OrderTableResponse dto = new OrderTableResponse(
                    order.getIdOrder(),
                    order.getCodOrder(),
                    order.getDescripcion(),
                    order.getCantidad(),
                    order.getEstado(),
                    order.getHoraInicio(),
                    order.getHoraFin(),
                    metricas != null ? metricas.getOee() : null,
                    metricas != null ? metricas.getPorCumpPedido() : null
            );

            responseList.add(dto);
        }

        return responseList;
    }

    /**
     * Recalcula los tiempos estimados de todas las órdenes.
     * Útil cuando se corrige la fórmula de cálculo y se necesita actualizar datos históricos.
     *
     * @return mapa con estadísticas del recálculo (total, actualizadas, sin cambios)
     */
    @Transactional
    public Map<String, Object> recalcularTiemposEstimados() {
        log.info("Iniciando recálculo de tiempos estimados para todas las órdenes");

        List<Order> orders = orderRepository.findAll();
        int totalOrders = orders.size();
        int actualizadas = 0;
        int sinCambios = 0;
        List<Map<String, Object>> detalles = new ArrayList<>();

        for (Order order : orders) {
            Float tiempoAnterior = order.getTiempoEstimado();
            Float tiempoNuevo = calculateTiempoEstimado(order.getCantidad(), order.getStdReferencia());

            // También recalcular cajas previstas por si acaso
            Float cajasAnterior = order.getCajasPrevistas();
            Float cajasNuevo = calculateCajasPrevistas(order.getCantidad(), order.getBotesCaja());

            boolean cambioTiempo = !Objects.equals(tiempoAnterior, tiempoNuevo);
            boolean cambioCajas = !Objects.equals(cajasAnterior, cajasNuevo);

            if (cambioTiempo || cambioCajas) {
                order.setTiempoEstimado(tiempoNuevo);
                order.setCajasPrevistas(cajasNuevo);
                orderRepository.save(order);
                actualizadas++;

                Map<String, Object> detalle = new HashMap<>();
                detalle.put("idOrder", order.getIdOrder());
                detalle.put("codOrder", order.getCodOrder());
                detalle.put("tiempoAnterior", tiempoAnterior);
                detalle.put("tiempoNuevo", tiempoNuevo);
                detalle.put("cajasAnterior", cajasAnterior);
                detalle.put("cajasNuevo", cajasNuevo);
                detalles.add(detalle);

                log.debug("Orden {} actualizada: tiempo {} -> {}, cajas {} -> {}",
                        order.getCodOrder(), tiempoAnterior, tiempoNuevo, cajasAnterior, cajasNuevo);
            } else {
                sinCambios++;
            }
        }

        log.info("Recálculo completado - Total: {}, Actualizadas: {}, Sin cambios: {}",
                totalOrders, actualizadas, sinCambios);

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("totalOrdenes", totalOrders);
        resultado.put("actualizadas", actualizadas);
        resultado.put("sinCambios", sinCambios);
        resultado.put("detalles", detalles);

        return resultado;
    }
}