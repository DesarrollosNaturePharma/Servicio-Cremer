package com.rnp.cremer.service;

import com.rnp.cremer.dto.PauseResponseDto;
import com.rnp.cremer.model.Order;
import com.rnp.cremer.model.Pause;
import com.rnp.cremer.model.TipoPausa;
import com.rnp.cremer.repository.OrderRepository;
import com.rnp.cremer.repository.PauseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para consultas de pausas (sin lógica de negocio).
 * Evita dependencias circulares.
 *
 * @author RNP Team
 * @version 1.0
 * @since 2025-11-27
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PauseQueryService {

    private final PauseRepository pauseRepository;
    private final OrderRepository orderRepository;

/**
 * Obtiene todas las pausas activas excepto las de tipo FABRICACION_PARCIAL.
 */
@Transactional(readOnly = true)
public List<PauseResponseDto> getActivePausesExcludingFabricacionParcial() {
    log.info("Obteniendo pausas activas (sin fabricación parcial)");
    
    // ⬅️ CAMBIO: Ya no pasamos parámetro
    List<Pause> pauses = pauseRepository.findActivePausesExcludingFabricacionParcial();
    
    // ⬅️ AGREGAR LOG TEMPORAL PARA DEBUG
    log.info("✅ Total pausas encontradas: {}", pauses.size());
    pauses.forEach(p -> log.info("   - Pausa ID: {}, Tipo: {}, Order: {}, Activa: {}", 
        p.getIdPausa(), p.getTipo(), p.getIdOrder(), p.getHoraFin() == null));
    
    return pauses.stream()
            .map(pause -> {
                Order order = orderRepository.findById(pause.getIdOrder())
                        .orElseThrow(() -> new IllegalArgumentException(
                            "Orden no encontrada con ID: " + pause.getIdOrder()
                        ));
                return mapToResponseDto(pause, order.getCodOrder());
            })
            .collect(Collectors.toList());
}

    /**
     * Mapea una entidad Pause a DTO.
     */
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