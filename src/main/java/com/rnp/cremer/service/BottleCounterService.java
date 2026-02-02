package com.rnp.cremer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rnp.cremer.dto.BottleCounterDto;
import com.rnp.cremer.dto.WebSocketEventDto;
import com.rnp.cremer.model.BottleCounter;
import com.rnp.cremer.model.EstadoOrder;
import com.rnp.cremer.model.Order;
import com.rnp.cremer.repository.BottleCounterRepository;
import com.rnp.cremer.repository.OrderRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Servicio para gestión del contador de botellas con integración GPIO.
 *
 * <p>Funcionalidades principales:</p>
 * <ul>
 *   <li>Conexión WebSocket a Raspberry Pi (192.168.20.30:8765)</li>
 *   <li>Escucha señales del pin GPIO 23</li>
 *   <li>Cuenta botellas en flanco de bajada (1→0)</li>
 *   <li>Solo cuenta si hay orden activa</li>
 *   <li>Actualización incremental (no duplica registros)</li>
 *   <li>Notificaciones en tiempo real al frontend</li>
 * </ul>
 *
 * @author RNP Team
 * @version 1.0
 * @since 2024-12-12
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BottleCounterService {

    private final BottleCounterRepository bottleCounterRepository;
    private final OrderRepository orderRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    // WebSocket Client
    private WebSocketClient wsClient;
    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private final String RASPBERRY_WS_URL = "ws://192.168.20.30:8765";

    // Estado del pin GPIO 23
    private final Map<Integer, Integer> pinStates = new HashMap<>();
    private static final int TARGET_PIN = 23;

    // Flag para indicar si ya se inicializó el estado del pin
    private final AtomicBoolean pinInitialized = new AtomicBoolean(false);

    // ========================================
    // INICIALIZACIÓN Y CONEXIÓN
    // ========================================

    /**
     * Inicializa la conexión WebSocket al arrancar el servicio.
     */
    @PostConstruct
    public void init() {
        log.info("🚀 Inicializando servicio de contador de botellas");
        connectToRaspberryPi();
    }

    /**
     * Cierra la conexión WebSocket al detener el servicio.
     */
    @PreDestroy
    public void cleanup() {
        log.info("🛑 Deteniendo servicio de contador de botellas");
        if (wsClient != null && wsClient.isOpen()) {
            wsClient.close();
        }
    }

    /**
     * Conecta al WebSocket de la Raspberry Pi.
     */
    private void connectToRaspberryPi() {
        try {
            log.info("🔌 Conectando a Raspberry Pi: {}", RASPBERRY_WS_URL);

            wsClient = new WebSocketClient(new URI(RASPBERRY_WS_URL)) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    isConnected.set(true);
                    pinInitialized.set(false); // Resetear flag al reconectar
                    log.info("✅ Conexión WebSocket establecida con Raspberry Pi");
                }

                @Override
                public void onMessage(String message) {
                    handleGpioMessage(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    isConnected.set(false);
                    pinInitialized.set(false);
                    log.warn("❌ Conexión WebSocket cerrada - Código: {}, Razón: {}", code, reason);
                }

                @Override
                public void onError(Exception ex) {
                    isConnected.set(false);
                    log.error("⚠️ Error en WebSocket", ex);
                }
            };

            wsClient.connect();

        } catch (Exception e) {
            log.error("❌ Error al conectar con Raspberry Pi", e);
        }
    }

    /**
     * Reconecta automáticamente cada 30 segundos si la conexión se pierde.
     */
    @Scheduled(fixedDelay = 30000)
    public void checkConnection() {
        if (!isConnected.get() || wsClient == null || !wsClient.isOpen()) {
            log.warn("🔄 Reconectando a Raspberry Pi...");
            connectToRaspberryPi();
        }
    }

    // ========================================
    // PROCESAMIENTO DE SEÑALES GPIO
    // ========================================

    /**
     * Procesa mensajes del WebSocket GPIO.
     * Formato esperado: {"pin": 23, "value": 0|1}
     */
    private void handleGpioMessage(String message) {
        try {
            JsonNode json = objectMapper.readTree(message);

            // Puede ser un array de estados iniciales o un solo objeto
            if (json.isArray()) {
                // Estados iniciales al conectar
                log.info("🔧 Recibiendo estados iniciales de GPIO");
                for (JsonNode node : json) {
                    int pin = node.get("pin").asInt();
                    int value = node.get("value").asInt();

                    // Inicializar estado sin detectar flancos
                    if (pin == TARGET_PIN) {
                        pinStates.put(pin, value);
                        log.info("🎯 Estado inicial del pin {} configurado a: {}", pin, value);
                    }
                }
                // Marcar como inicializado
                pinInitialized.set(true);
                log.info("✅ Estados de GPIO inicializados correctamente");
            } else {
                // Cambio de estado individual
                processGpioState(json);
            }

        } catch (Exception e) {
            log.error("❌ Error al procesar mensaje GPIO: {}", message, e);
        }
    }

    /**
     * Procesa un cambio de estado de un pin GPIO.
     */
    private void processGpioState(JsonNode node) {
        int pin = node.get("pin").asInt();
        int value = node.get("value").asInt();

        // Solo procesamos el pin 23
        if (pin != TARGET_PIN) {
            return;
        }

        Integer previousValue = pinStates.get(pin);
        pinStates.put(pin, value);

        // Si el pin no está inicializado, inicializarlo con el primer mensaje
        // Esto soluciona el problema de reinicio donde la Raspberry no envía array inicial
        if (!pinInitialized.get()) {
            if (previousValue == null) {
                // Primera vez que vemos este pin - solo guardamos el estado inicial
                log.info("🎯 Pin {} inicializado con valor: {} (primer mensaje individual)", pin, value);
                pinInitialized.set(true);
                return;
            }
        }

        // Detectar flanco de bajada (1 → 0)
        if (previousValue != null && previousValue == 1 && value == 0) {
            log.info("📦 Botella detectada - Pin {} cambió de 1 → 0", pin);
            incrementBottleCount();
        } else {
            log.debug("📊 Estado GPIO - Pin {}: {} → {} (no es flanco de bajada)",
                    pin, previousValue, value);
        }
    }

    /**
     * Incrementa el contador de botellas si hay orden EN_PROCESO.
     *
     * <p>Usa TransactionTemplate en lugar de @Transactional porque este método
     * se llama desde el callback del WebSocket (self-invocation + hilo no gestionado
     * por Spring), lo que hace que @Transactional no se aplique.</p>
     */
    public void incrementBottleCount() {
        try {
            transactionTemplate.execute(status -> {
                // 1. Obtener orden EN_PROCESO (solo EN_PROCESO, no PAUSADA)
                List<Order> enProcesoOrders = orderRepository.findByEstadoIn(
                        List.of(EstadoOrder.EN_PROCESO)
                );

                if (enProcesoOrders.isEmpty()) {
                    log.debug("⏸️ No hay orden EN_PROCESO, botella no contada");
                    return null;
                }

                // Tomar la más reciente por hora de inicio
                Order activeOrder = enProcesoOrders.stream()
                        .filter(o -> o.getHoraInicio() != null)
                        .max((o1, o2) -> o1.getHoraInicio().compareTo(o2.getHoraInicio()))
                        .orElse(enProcesoOrders.get(0));

                Long orderId = activeOrder.getIdOrder();

                // 2. Buscar o crear contador para esta orden
                BottleCounter counter = bottleCounterRepository.findByIdOrder(orderId)
                        .orElseGet(() -> {
                            BottleCounter newCounter = BottleCounter.builder()
                                    .idOrder(orderId)
                                    .quantity(0)
                                    .isActive(true)
                                    .build();
                            return bottleCounterRepository.save(newCounter);
                        });

                // 3. Asegurarse de que está activo
                if (!counter.getIsActive()) {
                    counter.setIsActive(true);
                }

                // 4. Incrementar contador
                counter.increment();
                BottleCounter savedCounter = bottleCounterRepository.save(counter);

                log.info("✅ Contador actualizado - Orden: {} | Cantidad: {}",
                        activeOrder.getCodOrder(), savedCounter.getQuantity());

                // 5. Notificar al frontend
                notifyCounterUpdate(savedCounter, activeOrder.getCodOrder());

                return null;
            });
        } catch (Exception e) {
            log.error("❌ Error al incrementar contador de botellas", e);
        }
    }

    // ========================================
    // GESTIÓN DE CONTADORES
    // ========================================

    /**
     * Crea un contador para una orden.
     */
    @Transactional
    public BottleCounter createCounterForOrder(Long orderId) {
        log.info("🆕 Creando contador para orden ID: {}", orderId);

        BottleCounter counter = BottleCounter.builder()
                .idOrder(orderId)
                .quantity(0)
                .isActive(true)
                .build();

        return bottleCounterRepository.save(counter);
    }

    /**
     * Activa el contador de una orden.
     * Se llama cuando una orden pasa a EN_PROCESO.
     */
    @Transactional
    public void activateCounterForOrder(Long orderId) {
        log.info("▶️ Activando contador para orden ID: {}", orderId);

        // Desactivar todos los contadores previos con una sola query
        bottleCounterRepository.deactivateAllCounters();

        // Activar o crear contador para esta orden
        BottleCounter counter = bottleCounterRepository.findByIdOrder(orderId)
                .orElseGet(() -> createCounterForOrder(orderId));

        counter.setIsActive(true);
        bottleCounterRepository.save(counter);

        log.info("✅ Contador activado para orden ID: {}", orderId);
    }

    /**
     * Desactiva el contador de una orden.
     * Se llama cuando una orden pasa a FINALIZADA.
     */
    @Transactional
    public void deactivateCounterForOrder(Long orderId) {
        log.info("⏸️ Desactivando contador para orden ID: {}", orderId);

        bottleCounterRepository.findByIdOrder(orderId).ifPresent(counter -> {
            counter.setIsActive(false);
            bottleCounterRepository.save(counter);
            log.info("✅ Contador desactivado - Valor final: {}", counter.getQuantity());
        });
    }

    /**
     * Obtiene el contador de una orden.
     */
    @Transactional(readOnly = true)
    public Optional<BottleCounterDto> getCounterByOrderId(Long orderId) {
        return bottleCounterRepository.findByIdOrder(orderId)
                .map(this::mapToDto);
    }

    /**
     * Obtiene el contador activo actual.
     */
    @Transactional(readOnly = true)
    public Optional<BottleCounterDto> getActiveCounter() {
        return bottleCounterRepository.findActiveCounter()
                .map(this::mapToDto);
    }

    /**
     * Resetea el contador de una orden a 0.
     * USAR CON PRECAUCIÓN: esto borra el conteo.
     */
    @Transactional
    public void resetCounter(Long orderId) {
        log.warn("🔄 Reseteando contador para orden ID: {}", orderId);

        bottleCounterRepository.findByIdOrder(orderId).ifPresent(counter -> {
            counter.reset();
            bottleCounterRepository.save(counter);
            log.info("✅ Contador reseteado a 0");
        });
    }

    // ========================================
    // NOTIFICACIONES WEBSOCKET
    // ========================================

    /**
     * Notifica actualización del contador al frontend.
     */
    private void notifyCounterUpdate(BottleCounter counter, String codOrder) {
        BottleCounterDto dto = mapToDto(counter, codOrder);

        WebSocketEventDto<BottleCounterDto> event = WebSocketEventDto.<BottleCounterDto>builder()
                .eventType("BOTTLE_COUNTER_UPDATE")
                .message(String.format("Contador actualizado: %d botellas", counter.getQuantity()))
                .data(dto)
                .timestamp(LocalDateTime.now())
                .build();

        // Notificación general
        messagingTemplate.convertAndSend("/topic/bottle-counter", event);

        // Notificación específica de la orden
        messagingTemplate.convertAndSend(
                "/topic/bottle-counter/" + counter.getIdOrder(),
                event
        );

        log.debug("📡 Notificación WebSocket enviada - Cantidad: {}", counter.getQuantity());
    }

    // ========================================
    // MAPPERS
    // ========================================

    /**
     * Convierte entidad a DTO.
     */
    private BottleCounterDto mapToDto(BottleCounter counter) {
        String codOrder = orderRepository.findById(counter.getIdOrder())
                .map(Order::getCodOrder)
                .orElse(null);

        return mapToDto(counter, codOrder);
    }

    /**
     * Convierte entidad a DTO con código de orden.
     */
    private BottleCounterDto mapToDto(BottleCounter counter, String codOrder) {
        return BottleCounterDto.builder()
                .id(counter.getId())
                .idOrder(counter.getIdOrder())
                .codOrder(codOrder)
                .quantity(counter.getQuantity())
                .isActive(counter.getIsActive())
                .lastUpdated(counter.getLastUpdated())
                .lastBottleCountedAt(counter.getLastBottleCountedAt())
                .build();
    }

    /**
     * Obtiene el estado de la conexión WebSocket.
     */
    public boolean isConnected() {
        return isConnected.get();
    }
}