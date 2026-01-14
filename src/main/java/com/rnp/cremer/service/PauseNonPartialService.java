package com.rnp.cremer.service;

import com.rnp.cremer.dto.PauseResponseDto;
import com.rnp.cremer.dto.WebSocketEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio para gestionar notificaciones de pausas activas (sin fabricación parcial).
 *
 * @author RNP Team
 * @version 1.0
 * @since 2025-11-27
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PauseNonPartialService {

    private final PauseQueryService pauseQueryService;  // ⬅️ CAMBIO: Usar PauseQueryService
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Notifica la lista actualizada de pausas activas (sin fabricación parcial).
     */
    public void notifyPausesNonPartialUpdate() {
        log.info("Notificando actualización de pausas activas (sin fabricación parcial)");

        List<PauseResponseDto> pauses = pauseQueryService.getActivePausesExcludingFabricacionParcial();

        WebSocketEventDto<List<PauseResponseDto>> event = WebSocketEventDto.<List<PauseResponseDto>>builder()
                .eventType("PAUSES_NON_PARTIAL_UPDATE")
                .message("Lista de pausas activas (sin fabricación parcial) actualizada")
                .data(pauses)
                .timestamp(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSend("/topic/pauses-non-partial", event);

        log.info("Notificación enviada - {} pausas activas", pauses.size());
    }
}