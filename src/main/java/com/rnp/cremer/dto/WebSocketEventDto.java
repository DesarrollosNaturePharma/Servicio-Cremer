package com.rnp.cremer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para eventos enviados por WebSocket.
 *
 * @author RNP Team
 * @version 1.0
 * @since 2024-11-25
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebSocketEventDto<T> {

    /**
     * Tipo de evento.
     * <p>Valores: ORDER_CREATED, ORDER_UPDATED, ORDER_STATE_CHANGED, PAUSE_CREATED, PAUSE_FINISHED, etc.</p>
     */
    private String eventType;

    /**
     * Mensaje descriptivo del evento.
     */
    private String message;

    /**
     * Datos del evento (puede ser Order, Pause, etc.).
     */
    private T data;

    /**
     * Timestamp del evento.
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    // ========================================
    // EVENTOS DE ÓRDENES
    // ========================================

    /**
     * Crea un evento de orden creada.
     */
    public static <T> WebSocketEventDto<T> orderCreated(String message, T data) {
        return WebSocketEventDto.<T>builder()
                .eventType("ORDER_CREATED")
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Crea un evento de orden actualizada.
     */
    public static <T> WebSocketEventDto<T> orderUpdated(String message, T data) {
        return WebSocketEventDto.<T>builder()
                .eventType("ORDER_UPDATED")
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Crea un evento de cambio de estado de orden.
     */
    public static <T> WebSocketEventDto<T> orderStateChanged(String message, T data) {
        return WebSocketEventDto.<T>builder()
                .eventType("ORDER_STATE_CHANGED")
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // ========================================
    // EVENTOS DE PAUSAS
    // ========================================

    /**
     * Crea un evento de pausa creada.
     */
    public static <T> WebSocketEventDto<T> pauseCreated(String message, T data) {
        return WebSocketEventDto.<T>builder()
                .eventType("PAUSE_CREATED")
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Crea un evento de pausa finalizada.
     */
    public static <T> WebSocketEventDto<T> pauseFinished(String message, T data) {
        return WebSocketEventDto.<T>builder()
                .eventType("PAUSE_FINISHED")
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Crea un evento de pausa actualizada.
     */
    public static <T> WebSocketEventDto<T> pauseUpdated(String message, T data) {
        return WebSocketEventDto.<T>builder()
                .eventType("PAUSE_UPDATED")
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // ========================================
    // EVENTOS GENÉRICOS
    // ========================================

    /**
     * Crea un evento genérico con tipo personalizado.
     */
    public static <T> WebSocketEventDto<T> customEvent(String eventType, String message, T data) {
        return WebSocketEventDto.<T>builder()
                .eventType(eventType)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }
}