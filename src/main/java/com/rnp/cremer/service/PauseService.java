package com.rnp.cremer.service;

import com.rnp.cremer.dto.PauseCreateDto;
import com.rnp.cremer.dto.PauseFinishDto;
import com.rnp.cremer.dto.PauseResponseDto;
import com.rnp.cremer.dto.WebSocketEventDto;
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

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PauseService {

    private final PauseRepository pauseRepository;
    private final OrderRepository orderRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final OrderService orderService;
    private final FabricacionParcialService fabricacionParcialService;
    private final PauseNonPartialService pauseNonPartialService;
    private final OrderQueryService orderQueryService;

    @Transactional
    public PauseResponseDto createPause(Long idOrder, PauseCreateDto dto) {
        log.info("Creando pausa para orden ID: {} - Tipo: {}", idOrder, dto.getTipo());

        Order order = orderRepository.findById(idOrder)
                .orElseThrow(() -> new IllegalArgumentException("Orden no encontrada con ID: " + idOrder));

        if (order.getEstado() != EstadoOrder.EN_PROCESO) {
            throw new IllegalArgumentException(
                    "Solo se pueden crear pausas para órdenes EN_PROCESO. Estado actual: " + order.getEstado());
        }

        if (pauseRepository.hasActivePause(idOrder)) {
            throw new IllegalArgumentException("Ya existe una pausa activa para esta orden");
        }

        Boolean computa = null;
        if (dto.getTipo() != null) {
            computa = computaSegunTipo(dto.getTipo());
        }

        Pause pause = Pause.builder()
                .idOrder(idOrder)
                .tipo(dto.getTipo())
                .descripcion(dto.getDescripcion())
                .operario(dto.getOperario())
                .computa(computa)
                .horaInicio(LocalDateTime.now())
                .horaFin(null)
                .tiempoTotalPausa(null)
                .build();

        Pause savedPause = pauseRepository.save(pause);
        EstadoOrder estadoAnterior = order.getEstado();
        order.setEstado(EstadoOrder.PAUSADA);
        Order updatedOrder = orderRepository.save(order);

        log.info("Pausa creada con ID: {} - Orden {} cambiada a PAUSADA",
                savedPause.getIdPausa(), order.getCodOrder());

        PauseResponseDto responseDto = mapToResponseDto(savedPause, order.getCodOrder());

        notifyPauseCreated(responseDto);
        orderService.notifyOrderStateChangedPublic(
                orderService.mapToResponseDtoPublic(updatedOrder),
                estadoAnterior,
                EstadoOrder.PAUSADA
        );

        if (savedPause.getTipo() == TipoPausa.FABRICACION_PARCIAL) {
            log.info("Notificando actualización de fabricación parcial (pausa creada)");
            fabricacionParcialService.notifyFabricacionParcialUpdate();
        } else {
            log.info("Notificando actualización de pausas activas (sin fabricación parcial)");
            pauseNonPartialService.notifyPausesNonPartialUpdate();
        }

        // ✅ NUEVO: Notificar cambio de orden visible
        orderQueryService.notifyActiveVisibleOrderChange();

        return responseDto;
    }
    @Transactional
    public PauseResponseDto finishPause(Long idOrder, Long idPausa, PauseFinishDto dto) {
        log.info("Finalizando pausa ID: {} de orden ID: {}", idPausa, idOrder);

        Pause pause = pauseRepository.findById(idPausa)
                .orElseThrow(() -> new IllegalArgumentException("Pausa no encontrada con ID: " + idPausa));

        if (!pause.getIdOrder().equals(idOrder)) {
            throw new IllegalArgumentException("La pausa no pertenece a la orden especificada");
        }

        if (pause.getHoraFin() != null) {
            throw new IllegalArgumentException("La pausa ya está finalizada");
        }

        if (pause.getTipo() == null) {
            if (dto.getTipo() == null) {
                throw new IllegalArgumentException(
                        "La pausa se creó sin tipo. Debe proporcionar el tipo al finalizar");
            }
            pause.setTipo(dto.getTipo());
            pause.setComputa(computaSegunTipo(dto.getTipo()));
            log.info("Tipo de pausa asignado al finalizar: {}", dto.getTipo());
        } else {
            if (dto.getTipo() != null && dto.getTipo() != pause.getTipo()) {
                log.info("Actualizando tipo de pausa de {} a {}", pause.getTipo(), dto.getTipo());
                pause.setTipo(dto.getTipo());
                pause.setComputa(computaSegunTipo(dto.getTipo()));
            }
        }

        if (dto.getOperario() != null && !dto.getOperario().isBlank()) {
            pause.setOperario(dto.getOperario());
        }

        if (dto.getDescripcion() != null && !dto.getDescripcion().isBlank()) {
            if (pause.getDescripcion() != null && !pause.getDescripcion().isBlank()) {
                pause.setDescripcion(pause.getDescripcion() + " | " + dto.getDescripcion());
            } else {
                pause.setDescripcion(dto.getDescripcion());
            }
        }

        LocalDateTime horaFin = LocalDateTime.now();
        pause.setHoraFin(horaFin);

        Duration duration = Duration.between(pause.getHoraInicio(), horaFin);
        float tiempoMinutos = duration.toSeconds() / 60.0f;
        pause.setTiempoTotalPausa(tiempoMinutos);

        Pause savedPause = pauseRepository.save(pause);

        Order order = orderRepository.findById(idOrder)
                .orElseThrow(() -> new IllegalArgumentException("Orden no encontrada"));

        EstadoOrder estadoAnterior = order.getEstado();
        order.setEstado(EstadoOrder.EN_PROCESO);
        Order updatedOrder = orderRepository.save(order);

        log.info("Pausa {} finalizada - Tipo: {} - Computa: {} - Duración: {:.2f} minutos",
                idPausa, savedPause.getTipo(), savedPause.getComputa(), tiempoMinutos);

        PauseResponseDto responseDto = mapToResponseDto(savedPause, order.getCodOrder());

        notifyPauseFinished(responseDto);
        orderService.notifyOrderStateChangedPublic(
                orderService.mapToResponseDtoPublic(updatedOrder),
                estadoAnterior,
                EstadoOrder.EN_PROCESO
        );

        if (savedPause.getTipo() == TipoPausa.FABRICACION_PARCIAL) {
            log.info("Notificando actualización de fabricación parcial (pausa finalizada)");
            fabricacionParcialService.notifyFabricacionParcialUpdate();
        } else {
            log.info("Notificando actualización de pausas activas (sin fabricación parcial)");
            pauseNonPartialService.notifyPausesNonPartialUpdate();
        }

        // ✅ NUEVO: Notificar cambio de orden visible
        orderQueryService.notifyActiveVisibleOrderChange();

        return responseDto;
    }

    @Transactional(readOnly = true)
    public List<PauseResponseDto> getPausesByOrder(Long idOrder) {
        log.info("Obteniendo pausas de orden ID: {}", idOrder);

        if (!orderRepository.existsById(idOrder)) {
            throw new IllegalArgumentException("Orden no encontrada con ID: " + idOrder);
        }

        List<Pause> pauses = pauseRepository.findByIdOrderOrderByHoraInicioDesc(idOrder);
        Order order = orderRepository.findById(idOrder).orElseThrow();
        String codOrder = order.getCodOrder();

        return pauses.stream()
                .map(pause -> mapToResponseDto(pause, codOrder))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PauseResponseDto getActivePause(Long idOrder) {
        log.info("Obteniendo pausa activa de orden ID: {}", idOrder);

        if (!orderRepository.existsById(idOrder)) {
            throw new IllegalArgumentException("Orden no encontrada con ID: " + idOrder);
        }

        Pause activePause = pauseRepository.findActivePauseByOrder(idOrder).orElse(null);

        if (activePause == null) {
            return null;
        }

        Order order = orderRepository.findById(idOrder).orElseThrow();
        return mapToResponseDto(activePause, order.getCodOrder());
    }

    private Boolean computaSegunTipo(TipoPausa tipo) {
        if (tipo == null) {
            return null;
        }
        return tipo.computa();
    }


    private void notifyPauseCreated(PauseResponseDto pause) {
        WebSocketEventDto<PauseResponseDto> event = WebSocketEventDto.pauseCreated(
                "Nueva pausa creada en orden: " + pause.getCodOrder(),
                pause
        );

        messagingTemplate.convertAndSend("/topic/orders", event);
        messagingTemplate.convertAndSend("/topic/orders/" + pause.getIdOrder(), event);

        log.info("Notificación WebSocket enviada - Pausa creada: ID {}", pause.getIdPausa());
    }

    private void notifyPauseFinished(PauseResponseDto pause) {
        WebSocketEventDto<PauseResponseDto> event = WebSocketEventDto.pauseFinished(
                "Pausa finalizada en orden: " + pause.getCodOrder(),
                pause
        );

        messagingTemplate.convertAndSend("/topic/orders", event);
        messagingTemplate.convertAndSend("/topic/orders/" + pause.getIdOrder(), event);

        log.info("Notificación WebSocket enviada - Pausa finalizada: ID {}", pause.getIdPausa());
    }

    private PauseResponseDto mapToResponseDto(Pause pause, String codOrder) {
        return PauseResponseDto.builder()
                .idPausa(pause.getIdPausa())
                .idOrder(pause.getIdOrder())
                .codOrder(codOrder)
                .tipo(pause.getTipo())
                .descripcion(pause.getDescripcion())
                .operario(pause.getOperario())
                .computa(pause.getComputa())
                .horaInicio(pause.getHoraInicio())
                .horaFin(pause.getHoraFin())
                .tiempoTotalPausa(pause.getTiempoTotalPausa())
                .build();
    }
}