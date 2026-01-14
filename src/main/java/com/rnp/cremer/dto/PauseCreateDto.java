package com.rnp.cremer.dto;

import com.rnp.cremer.model.TipoPausa;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para crear una nueva pausa.
 *
 * <p><b>Modo 1 - Completo al inicio:</b> Se proporciona tipo, descripción y operario.
 * Al finalizar solo se cierra la pausa.</p>
 *
 * <p><b>Modo 2 - Rápido:</b> Solo se proporciona operario (o nada).
 * Al finalizar se proporciona tipo, descripción, etc.</p>
 *
 * @author RNP Team
 * @version 1.0
 * @since 2024-11-25
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PauseCreateDto {

    /**
     * Tipo de pausa (OPCIONAL al crear).
     * Si no se proporciona, debe proporcionarse al finalizar.
     */
    private TipoPausa tipo;

    /**
     * Descripción/motivo de la pausa (OPCIONAL al crear).
     */
    @Size(max = 500, message = "La descripción no puede superar 500 caracteres")
    private String descripcion;

    /**
     * Operario que registra la pausa (OPCIONAL al crear).
     * Si no se proporciona, se puede agregar al finalizar.
     */
    @Size(max = 100, message = "El operario no puede superar 100 caracteres")
    private String operario;
}