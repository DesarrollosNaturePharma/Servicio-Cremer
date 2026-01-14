package com.rnp.cremer.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuración de WebSocket para comunicación en tiempo real.
 *
 * @author RNP Team
 * @version 1.0
 * @since 2024-11-25
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Habilitar un broker simple en memoria para enviar mensajes a clientes suscritos
        config.enableSimpleBroker("/topic", "/queue");

        // Prefijo para mensajes enviados desde el cliente al servidor
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // ✅ PERMITIR TODOS LOS ORÍGENES PARA WEBSOCKET
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")  // ✅ Ya está correcto
                .withSockJS();
    }
}