package com.rnp.cremer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rnp.cremer.dto.PauseCreateDto;
import com.rnp.cremer.dto.PauseFinishDto;
import com.rnp.cremer.dto.PauseResponseDto;
import com.rnp.cremer.model.EstadoOrder;
import com.rnp.cremer.model.Order;
import com.rnp.cremer.model.Pause;
import com.rnp.cremer.model.TipoPausa;
import com.rnp.cremer.repository.OrderRepository;
import com.rnp.cremer.repository.PauseRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Servicio para gestión de pausas automáticas basadas en señales GPIO.
 *
 * <p>Monitorea las siguientes señales GPIO desde el servidor WebSocket:</p>
 * <ul>
 *   <li>GPIO 22: Avería Ponderal - Cuando está en false por 20s, crea pausa automática</li>
 *   <li>GPIO 19: Avería Etiqueta - Cuando está en false por 20s, crea pausa automática</li>
 * </ul>
 *
 * <p>Lógica de funcionamiento:</p>
 * <ul>
 *   <li>Señal en TRUE = máquina funcionando correctamente</li>
 *   <li>Señal en FALSE durante 20 segundos = crear pausa automática</li>
 *   <li>Señal en TRUE durante 5 segundos = finalizar pausa automática</li>
 *   <li>Solo una pausa activa a la vez (exclusividad)</li>
 *   <li>Cooldown de 30 segundos después de finalizar una pausa</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AutomaticPauseService {

    private final PauseService pauseService;
    private final OrderRepository orderRepository;
    private final PauseRepository pauseRepository;
    private final OrderQueryService orderQueryService;
    private final ObjectMapper objectMapper;

    // WebSocket Client
    private WebSocketClient wsClient;
    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private final String RASPBERRY_WS_URL = "ws://192.168.20.30:8765";

    // Configuración de pines GPIO
    private static final int GPIO_PONDERAL = 22;   // Avería Ponderal
    private static final int GPIO_ETIQUETA = 19;   // Avería Etiqueta

    // Configuración de tiempos (en segundos)
    private static final int TIEMPO_CREAR_PAUSA = 20;      // Tiempo en false para crear pausa
    private static final int TIEMPO_FINALIZAR_PAUSA = 5;   // Tiempo en true para finalizar pausa
    private static final int TIEMPO_COOLDOWN = 30;          // Tiempo de espera después de finalizar

    // Estado de los pines
    private final Map<Integer, Boolean> pinStates = new ConcurrentHashMap<>();
    private final AtomicBoolean pinsInitialized = new AtomicBoolean(false);

    // Estado de pausa activa gestionada por este servicio
    private final AtomicReference<ActiveAutoPause> activePause = new AtomicReference<>(null);

    // Scheduler para temporizadores
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    // Futures para los temporizadores de cada pin
    private final Map<Integer, ScheduledFuture<?>> createPauseTimers = new ConcurrentHashMap<>();
    private final Map<Integer, ScheduledFuture<?>> finishPauseTimers = new ConcurrentHashMap<>();

    // Cooldown activo
    private final AtomicBoolean inCooldown = new AtomicBoolean(false);
    private ScheduledFuture<?> cooldownTimer;

    // Tracking de tiempo en estado false para cada pin
    private final Map<Integer, Long> falseStateStartTime = new ConcurrentHashMap<>();
    private final Map<Integer, Long> trueStateStartTime = new ConcurrentHashMap<>();

    /**
     * Clase interna para trackear la pausa automática activa.
     */
    private static class ActiveAutoPause {
        final int gpioPin;
        final Long orderId;
        final Long pauseId;
        final TipoPausa tipo;

        ActiveAutoPause(int gpioPin, Long orderId, Long pauseId, TipoPausa tipo) {
            this.gpioPin = gpioPin;
            this.orderId = orderId;
            this.pauseId = pauseId;
            this.tipo = tipo;
        }
    }

    // ========================================
    // INICIALIZACIÓN Y CONEXIÓN
    // ========================================

    @PostConstruct
    public void init() {
        log.info("Inicializando servicio de pausas automáticas GPIO");
        log.info("Monitoreando GPIO {} (Ponderal) y GPIO {} (Etiqueta)", GPIO_PONDERAL, GPIO_ETIQUETA);
        log.info("Tiempos configurados - Crear: {}s, Finalizar: {}s, Cooldown: {}s",
                TIEMPO_CREAR_PAUSA, TIEMPO_FINALIZAR_PAUSA, TIEMPO_COOLDOWN);
        connectToRaspberryPi();
    }

    @PreDestroy
    public void cleanup() {
        log.info("Deteniendo servicio de pausas automáticas");

        // Cancelar todos los timers
        createPauseTimers.values().forEach(f -> f.cancel(true));
        finishPauseTimers.values().forEach(f -> f.cancel(true));
        if (cooldownTimer != null) {
            cooldownTimer.cancel(true);
        }

        scheduler.shutdown();

        if (wsClient != null && wsClient.isOpen()) {
            wsClient.close();
        }
    }

    private void connectToRaspberryPi() {
        try {
            log.info("Conectando a servidor GPIO: {}", RASPBERRY_WS_URL);

            wsClient = new WebSocketClient(new URI(RASPBERRY_WS_URL)) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    isConnected.set(true);
                    pinsInitialized.set(false);
                    log.info("Conexión WebSocket establecida para pausas automáticas");
                }

                @Override
                public void onMessage(String message) {
                    handleGpioMessage(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    isConnected.set(false);
                    pinsInitialized.set(false);
                    cancelAllTimers();
                    log.warn("Conexión WebSocket cerrada - Código: {}, Razón: {}", code, reason);
                }

                @Override
                public void onError(Exception ex) {
                    isConnected.set(false);
                    log.error("Error en WebSocket de pausas automáticas", ex);
                }
            };

            wsClient.connect();

        } catch (Exception e) {
            log.error("Error al conectar con servidor GPIO", e);
        }
    }

    @Scheduled(fixedDelay = 30000)
    public void checkConnection() {
        if (!isConnected.get() || wsClient == null || !wsClient.isOpen()) {
            log.warn("Reconectando al servidor GPIO...");
            connectToRaspberryPi();
        }
    }

    /**
     * Verifica periódicamente si la pausa activa fue finalizada manualmente.
     * Esto evita que el sistema quede en estado inconsistente si alguien
     * finaliza la pausa manualmente mientras el GPIO sigue en false.
     */
    @Scheduled(fixedDelay = 5000)
    public void checkActivePauseStatus() {
        ActiveAutoPause current = activePause.get();

        if (current == null) {
            return;
        }

        try {
            Optional<Pause> pauseOpt = pauseRepository.findById(current.pauseId);

            if (pauseOpt.isEmpty() || pauseOpt.get().getHoraFin() != null) {
                log.info("Pausa ID {} fue finalizada manualmente, limpiando estado del servicio", current.pauseId);

                // Cancelar timer de finalización si existe
                cancelFinishPauseTimer(current.gpioPin);

                // Limpiar estado
                activePause.set(null);

                // Iniciar cooldown
                startCooldown();
            }
        } catch (Exception e) {
            log.error("Error al verificar estado de pausa activa", e);
        }
    }

    // Estado anterior de la orden para detectar cambios
    private final AtomicBoolean lastOrderEnProceso = new AtomicBoolean(false);

    /**
     * Verifica periódicamente si la orden volvió a EN_PROCESO después de una pausa manual.
     * Cuando esto ocurre, evalúa los estados GPIO para iniciar detección si hay fallos.
     */
    @Scheduled(fixedDelay = 3000)
    public void checkOrderStateForGpioMonitoring() {
        // Solo verificar si no hay pausa automática activa y no estamos en cooldown
        if (activePause.get() != null || inCooldown.get() || !pinsInitialized.get()) {
            return;
        }

        boolean currentEnProceso = isOrderEnProceso();
        boolean wasEnProceso = lastOrderEnProceso.getAndSet(currentEnProceso);

        // Detectar transición a EN_PROCESO (orden reanudada después de pausa manual)
        if (currentEnProceso && !wasEnProceso) {
            log.info("Orden volvió a EN_PROCESO, evaluando estados GPIO para detección automática");
            evaluateGpioStatesForNewDetection();
        }
    }

    /**
     * Evalúa los estados GPIO cuando la orden vuelve a EN_PROCESO.
     * Si algún pin está en false, inicia el timer de creación de pausa.
     */
    private void evaluateGpioStatesForNewDetection() {
        for (int pin : new int[]{GPIO_PONDERAL, GPIO_ETIQUETA}) {
            Boolean state = pinStates.get(pin);
            if (state != null && !state) {
                log.info("GPIO {} ({}) está en FALLO, iniciando timer de {} segundos",
                        pin, getPinName(pin), TIEMPO_CREAR_PAUSA);
                if (canStartCreateTimer(pin)) {
                    // Actualizar tiempo de inicio de fallo
                    falseStateStartTime.put(pin, System.currentTimeMillis());
                    startCreatePauseTimer(pin);
                    break; // Solo un timer a la vez
                }
            }
        }
    }

    // ========================================
    // PROCESAMIENTO DE SEÑALES GPIO
    // ========================================

    private void handleGpioMessage(String message) {
        try {
            JsonNode json = objectMapper.readTree(message);

            if (json.isArray()) {
                // Estados iniciales al conectar
                log.info("Recibiendo estados iniciales de GPIO para pausas automáticas");
                for (JsonNode node : json) {
                    int pin = node.get("pin").asInt();
                    int value = node.get("value").asInt();

                    if (pin == GPIO_PONDERAL || pin == GPIO_ETIQUETA) {
                        boolean state = value == 1;
                        pinStates.put(pin, state);
                        log.info("Estado inicial GPIO {}: {} ({})",
                                pin, state ? "OK" : "FALLO", getPinName(pin));

                        // Inicializar tracking de tiempo
                        if (state) {
                            trueStateStartTime.put(pin, System.currentTimeMillis());
                            falseStateStartTime.remove(pin);
                        } else {
                            falseStateStartTime.put(pin, System.currentTimeMillis());
                            trueStateStartTime.remove(pin);
                        }
                    }
                }
                pinsInitialized.set(true);
                log.info("Estados GPIO inicializados - Ponderal: {}, Etiqueta: {}",
                        pinStates.get(GPIO_PONDERAL), pinStates.get(GPIO_ETIQUETA));

                // Evaluar estados iniciales después de inicializar
                evaluateInitialStates();

            } else {
                // Cambio de estado individual
                processGpioStateChange(json);
            }

        } catch (Exception e) {
            log.error("Error al procesar mensaje GPIO: {}", message, e);
        }
    }

    private void evaluateInitialStates() {
        // Si algún pin ya está en false, iniciar timer para crear pausa
        for (int pin : new int[]{GPIO_PONDERAL, GPIO_ETIQUETA}) {
            Boolean state = pinStates.get(pin);
            if (state != null && !state) {
                log.info("GPIO {} detectado en estado FALLO al inicializar", pin);
                startCreatePauseTimer(pin);
            }
        }
    }

    private void processGpioStateChange(JsonNode node) {
        int pin = node.get("pin").asInt();
        int value = node.get("value").asInt();

        // Solo procesamos los pines de interés
        if (pin != GPIO_PONDERAL && pin != GPIO_ETIQUETA) {
            return;
        }

        if (!pinsInitialized.get()) {
            log.debug("Esperando inicialización de pines antes de procesar cambios");
            return;
        }

        boolean newState = value == 1;
        Boolean previousState = pinStates.get(pin);
        pinStates.put(pin, newState);

        if (previousState == null || previousState == newState) {
            return; // Sin cambio real
        }

        log.info("GPIO {} ({}) cambió: {} -> {}",
                pin, getPinName(pin),
                previousState ? "OK" : "FALLO",
                newState ? "OK" : "FALLO");

        if (newState) {
            // Cambio a TRUE (máquina OK)
            onSignalRecovered(pin);
        } else {
            // Cambio a FALSE (máquina FALLO)
            onSignalFailed(pin);
        }
    }

    // ========================================
    // LÓGICA DE PAUSAS AUTOMÁTICAS
    // ========================================

    /**
     * Se llama cuando una señal pasa a FALSE (fallo).
     */
    private void onSignalFailed(int pin) {
        log.info("Señal FALLO detectada en GPIO {} ({})", pin, getPinName(pin));

        // Registrar tiempo de inicio de fallo
        falseStateStartTime.put(pin, System.currentTimeMillis());
        trueStateStartTime.remove(pin);

        // Cancelar timer de finalización si existía
        cancelFinishPauseTimer(pin);

        // Verificar si podemos iniciar timer de creación
        if (canStartCreateTimer(pin)) {
            startCreatePauseTimer(pin);
        } else {
            log.info("No se puede iniciar timer de creación para GPIO {} - Pausa activa o cooldown", pin);
        }
    }

    /**
     * Se llama cuando una señal pasa a TRUE (recuperación).
     */
    private void onSignalRecovered(int pin) {
        log.info("Señal OK detectada en GPIO {} ({})", pin, getPinName(pin));

        // Registrar tiempo de inicio de OK
        trueStateStartTime.put(pin, System.currentTimeMillis());
        falseStateStartTime.remove(pin);

        // Cancelar timer de creación si existía
        cancelCreatePauseTimer(pin);

        // Si hay una pausa activa de este pin, iniciar timer de finalización
        ActiveAutoPause current = activePause.get();
        if (current != null && current.gpioPin == pin) {
            startFinishPauseTimer(pin);
        }
    }

    private boolean canStartCreateTimer(int pin) {
        // No crear si ya hay pausa activa gestionada por este servicio
        if (activePause.get() != null) {
            log.debug("Ya existe pausa automática activa - GPIO {}", activePause.get().gpioPin);
            return false;
        }

        // No crear si estamos en cooldown
        if (inCooldown.get()) {
            log.debug("En período de cooldown");
            return false;
        }

        // No crear si ya hay timer activo para otro pin
        for (Map.Entry<Integer, ScheduledFuture<?>> entry : createPauseTimers.entrySet()) {
            if (entry.getKey() != pin && !entry.getValue().isDone()) {
                log.debug("Ya existe timer de creación activo para GPIO {}", entry.getKey());
                return false;
            }
        }

        // No crear si la orden no está EN_PROCESO (puede haber pausa manual activa)
        if (!isOrderEnProceso()) {
            log.debug("Orden no está EN_PROCESO, ignorando señales GPIO");
            return false;
        }

        return true;
    }

    /**
     * Verifica si hay una orden activa en estado EN_PROCESO.
     */
    private boolean isOrderEnProceso() {
        Optional<Order> activeOrderOpt = orderQueryService.getActiveVisibleOrderEntity();
        if (activeOrderOpt.isEmpty()) {
            return false;
        }
        return activeOrderOpt.get().getEstado() == EstadoOrder.EN_PROCESO;
    }

    // ========================================
    // TIMERS DE CREACIÓN DE PAUSA
    // ========================================

    private void startCreatePauseTimer(int pin) {
        // Cancelar timer previo si existe
        cancelCreatePauseTimer(pin);

        log.info("Iniciando timer de {} segundos para crear pausa en GPIO {} ({})",
                TIEMPO_CREAR_PAUSA, pin, getPinName(pin));

        ScheduledFuture<?> timer = scheduler.schedule(() -> {
            try {
                // Verificar que el pin sigue en false
                Boolean currentState = pinStates.get(pin);
                if (currentState != null && !currentState) {
                    createAutomaticPause(pin);
                } else {
                    log.info("GPIO {} ya no está en fallo, cancelando creación de pausa", pin);
                }
            } catch (Exception e) {
                log.error("Error al crear pausa automática para GPIO {}", pin, e);
            }
        }, TIEMPO_CREAR_PAUSA, TimeUnit.SECONDS);

        createPauseTimers.put(pin, timer);
    }

    private void cancelCreatePauseTimer(int pin) {
        ScheduledFuture<?> timer = createPauseTimers.remove(pin);
        if (timer != null && !timer.isDone()) {
            timer.cancel(false);
            log.debug("Timer de creación cancelado para GPIO {}", pin);
        }
    }

    // ========================================
    // TIMERS DE FINALIZACIÓN DE PAUSA
    // ========================================

    private void startFinishPauseTimer(int pin) {
        // Cancelar timer previo si existe
        cancelFinishPauseTimer(pin);

        log.info("Iniciando timer de {} segundos para finalizar pausa en GPIO {} ({})",
                TIEMPO_FINALIZAR_PAUSA, pin, getPinName(pin));

        ScheduledFuture<?> timer = scheduler.schedule(() -> {
            try {
                // Verificar que el pin sigue en true
                Boolean currentState = pinStates.get(pin);
                if (currentState != null && currentState) {
                    finishAutomaticPause(pin);
                } else {
                    log.info("GPIO {} volvió a fallo, cancelando finalización de pausa", pin);
                }
            } catch (Exception e) {
                log.error("Error al finalizar pausa automática para GPIO {}", pin, e);
            }
        }, TIEMPO_FINALIZAR_PAUSA, TimeUnit.SECONDS);

        finishPauseTimers.put(pin, timer);
    }

    private void cancelFinishPauseTimer(int pin) {
        ScheduledFuture<?> timer = finishPauseTimers.remove(pin);
        if (timer != null && !timer.isDone()) {
            timer.cancel(false);
            log.debug("Timer de finalización cancelado para GPIO {}", pin);
        }
    }

    // ========================================
    // CREACIÓN Y FINALIZACIÓN DE PAUSAS
    // ========================================

    private void createAutomaticPause(int pin) {
        log.info("=== CREANDO PAUSA AUTOMÁTICA - GPIO {} ({}) ===", pin, getPinName(pin));

        // Verificar nuevamente condiciones
        if (activePause.get() != null) {
            log.warn("Ya existe pausa automática activa, no se puede crear otra");
            return;
        }

        if (inCooldown.get()) {
            log.warn("En período de cooldown, no se puede crear pausa");
            return;
        }

        // Obtener orden activa
        Optional<Order> activeOrderOpt = orderQueryService.getActiveVisibleOrderEntity();

        if (activeOrderOpt.isEmpty()) {
            log.warn("No hay orden activa, no se puede crear pausa automática");
            return;
        }

        Order activeOrder = activeOrderOpt.get();

        // Verificar que la orden está EN_PROCESO
        if (activeOrder.getEstado() != EstadoOrder.EN_PROCESO) {
            log.warn("Orden {} no está EN_PROCESO (estado: {}), ignorando",
                    activeOrder.getCodOrder(), activeOrder.getEstado());
            return;
        }

        TipoPausa tipo = getTipoPausaForPin(pin);

        try {
            // Crear la pausa
            PauseCreateDto createDto = PauseCreateDto.builder()
                    .tipo(tipo)
                    .descripcion("Pausa automática detectada por señal GPIO " + pin)
                    .operario("SISTEMA AUTOMATICO")
                    .build();

            PauseResponseDto response = pauseService.createPause(activeOrder.getIdOrder(), createDto);

            // Registrar pausa activa
            activePause.set(new ActiveAutoPause(
                    pin,
                    activeOrder.getIdOrder(),
                    response.getIdPausa(),
                    tipo
            ));

            log.info("Pausa automática CREADA - ID: {}, Orden: {}, Tipo: {}",
                    response.getIdPausa(), activeOrder.getCodOrder(), tipo);

        } catch (Exception e) {
            log.error("Error al crear pausa automática para orden {}", activeOrder.getCodOrder(), e);
        }
    }

    private void finishAutomaticPause(int pin) {
        log.info("=== FINALIZANDO PAUSA AUTOMÁTICA - GPIO {} ({}) ===", pin, getPinName(pin));

        ActiveAutoPause current = activePause.get();

        if (current == null) {
            log.warn("No hay pausa automática activa para finalizar");
            return;
        }

        if (current.gpioPin != pin) {
            log.warn("La pausa activa es de GPIO {} ({}), no de GPIO {} ({})",
                    current.gpioPin, getPinName(current.gpioPin), pin, getPinName(pin));
            return;
        }

        try {
            // Verificar si la pausa ya fue finalizada manualmente
            Optional<Pause> pauseOpt = pauseRepository.findById(current.pauseId);

            if (pauseOpt.isEmpty()) {
                log.info("La pausa ID {} ya no existe, limpiando estado", current.pauseId);
                activePause.set(null);
                startCooldown();
                return;
            }

            Pause pause = pauseOpt.get();

            if (pause.getHoraFin() != null) {
                // La pausa ya fue finalizada manualmente
                log.info("La pausa ID {} ya fue finalizada manualmente, limpiando estado", current.pauseId);
                activePause.set(null);
                startCooldown();
                return;
            }

            // Finalizar la pausa automáticamente
            PauseFinishDto finishDto = PauseFinishDto.builder()
                    .descripcion("Finalizada automáticamente - Señal GPIO " + pin + " recuperada")
                    .build();

            PauseResponseDto response = pauseService.finishPause(
                    current.orderId,
                    current.pauseId,
                    finishDto
            );

            log.info("Pausa automática FINALIZADA - ID: {}, Duración: {} minutos",
                    response.getIdPausa(), response.getTiempoTotalPausa());

            // Limpiar pausa activa
            activePause.set(null);

            // Iniciar cooldown
            startCooldown();

        } catch (Exception e) {
            log.error("Error al finalizar pausa automática ID {}", current.pauseId, e);
            // Limpiar de todas formas para evitar bloqueos
            activePause.set(null);
            startCooldown();
        }
    }

    // ========================================
    // COOLDOWN
    // ========================================

    private void startCooldown() {
        log.info("Iniciando período de cooldown de {} segundos", TIEMPO_COOLDOWN);

        inCooldown.set(true);

        if (cooldownTimer != null && !cooldownTimer.isDone()) {
            cooldownTimer.cancel(false);
        }

        cooldownTimer = scheduler.schedule(() -> {
            inCooldown.set(false);
            log.info("Cooldown finalizado - Sistema listo para detectar nuevas averías");

            // Evaluar estados actuales por si algún pin está en fallo
            checkCurrentStatesAfterCooldown();

        }, TIEMPO_COOLDOWN, TimeUnit.SECONDS);
    }

    private void checkCurrentStatesAfterCooldown() {
        for (int pin : new int[]{GPIO_PONDERAL, GPIO_ETIQUETA}) {
            Boolean state = pinStates.get(pin);
            if (state != null && !state) {
                log.info("GPIO {} detectado en FALLO después de cooldown", pin);
                if (canStartCreateTimer(pin)) {
                    startCreatePauseTimer(pin);
                    break; // Solo un timer a la vez
                }
            }
        }
    }

    // ========================================
    // UTILIDADES
    // ========================================

    private void cancelAllTimers() {
        createPauseTimers.values().forEach(f -> f.cancel(true));
        createPauseTimers.clear();

        finishPauseTimers.values().forEach(f -> f.cancel(true));
        finishPauseTimers.clear();

        if (cooldownTimer != null) {
            cooldownTimer.cancel(true);
        }

        log.debug("Todos los timers cancelados");
    }

    private TipoPausa getTipoPausaForPin(int pin) {
        return switch (pin) {
            case GPIO_PONDERAL -> TipoPausa.AVERIA_PONDERAL;
            case GPIO_ETIQUETA -> TipoPausa.AVERIA_ETIQUETA;
            default -> throw new IllegalArgumentException("Pin GPIO no soportado: " + pin);
        };
    }

    private String getPinName(int pin) {
        return switch (pin) {
            case GPIO_PONDERAL -> "Ponderal";
            case GPIO_ETIQUETA -> "Etiqueta";
            default -> "Desconocido";
        };
    }

    // ========================================
    // MÉTODOS PÚBLICOS DE ESTADO
    // ========================================

    /**
     * Indica si hay conexión activa con el servidor GPIO.
     */
    public boolean isConnected() {
        return isConnected.get();
    }

    /**
     * Indica si hay una pausa automática activa.
     */
    public boolean hasActivePause() {
        return activePause.get() != null;
    }

    /**
     * Indica si el sistema está en cooldown.
     */
    public boolean isInCooldown() {
        return inCooldown.get();
    }

    /**
     * Obtiene el estado actual del GPIO Ponderal.
     * @return true si OK, false si FALLO, null si no inicializado
     */
    public Boolean getPonderalState() {
        return pinStates.get(GPIO_PONDERAL);
    }

    /**
     * Obtiene el estado actual del GPIO Etiqueta.
     * @return true si OK, false si FALLO, null si no inicializado
     */
    public Boolean getEtiquetaState() {
        return pinStates.get(GPIO_ETIQUETA);
    }
}
